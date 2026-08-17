package com.aryariap.forfh.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bentuk respons nyata GET /api/campus/info (ForFH web, terverifikasi 2026-08-17 dari
 * src/app/api/campus/info/route.ts + src/lib/campus/sync.ts + src/tests/campus.test.ts):
 *
 *   { "connected": bool, "lastSyncAt": ISO|null,
 *     "items": [ { "jenis": string, "data": [...], "updatedAt": ISO }, ... ] }
 *
 * - jenis "presensi" → data TERNORMALISASI server-side (presensiToRecap web):
 *     [ { "code", "name", "tm", "hadir", "persen" } ]  (camelCase!)
 * - jenis lain (status_mhs, peserta_mk, pembayaran, dosen_wali, masa_studi, sks_aktif,
 *   hist_her, penyerahan_ktm, kalender_akademik, instruksi_tugas, ...) → baris MENTAH
 *   UPPERCASE_SNAKE dari libapp.so, disimpan verbatim (pola web: campusData.dataJson +
 *   rendering label-value generik).
 */
class KampusInfoMappersTest {

    private val json = ApiClient.forfhJson // ignoreUnknownKeys + coerceInputValues (produksi)

    private val sampleLengkap = """
        {
          "connected": true,
          "lastSyncAt": "2026-08-17T04:05:06.123Z",
          "items": [
            {
              "jenis": "presensi",
              "data": [
                { "code": "FHK25601032", "name": "Hak Asasi Manusia", "tm": 14, "hadir": 13, "persen": 93 },
                { "code": "FHK25601033", "name": "Hukum Acara Pidana", "tm": 14, "hadir": 7, "persen": 50 }
              ],
              "updatedAt": "2026-08-17T04:05:06.123Z"
            },
            {
              "jenis": "status_mhs",
              "data": [
                {
                  "NIM_MHS": "626103051310", "NM_PENGGUNA": "Arya Rizky",
                  "NM_PROGRAM_STUDI": "Ilmu Hukum", "JENJANG": "S1",
                  "FAKULTAS": "Fakultas Hukum", "ANGKATAN": "2026",
                  "STATUS_AKADEMIK": "Aktif", "JK": "L", "AGAMA": "Islam"
                }
              ],
              "updatedAt": "2026-08-17T04:05:06.123Z"
            },
            {
              "jenis": "peserta_mk",
              "data": [
                { "KODE_MK": "FHK25601032", "NM_MATA_KULIAH": "Hak Asasi Manusia", "NAMA_KELAS": "A-1", "SKS": "3", "NM_DOSEN": "Dr. X" }
              ],
              "updatedAt": "2026-08-17T04:05:06.123Z"
            },
            {
              "jenis": "kalender_akademik",
              "data": [
                { "KEGIATAN": "KPRS", "TGL_MULAI": "2026-08-24", "TGL_SELESAI": "2026-08-28" }
              ],
              "updatedAt": "2026-08-17T04:05:06.123Z"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `envelope lengkap - presensi typed, info raw UPPERCASE_SNAKE round-trip`() {
        val snapshot = json.decodeFromString<KampusInfoEnvelopeDto>(sampleLengkap).toSnapshot()

        assertTrue(snapshot.connected)
        assertEquals("2026-08-17T04:05:06.123Z", snapshot.lastSyncAt)

        // Rekap presensi per MK — kolom typed dari data ternormalisasi server
        assertEquals(2, snapshot.presensi.size)
        val ham = snapshot.presensi.first { it.kode == "FHK25601032" }
        assertEquals("Hak Asasi Manusia", ham.nama)
        assertEquals(14, ham.tm)
        assertEquals(13, ham.hadir)
        assertEquals(93, ham.persen)
        val pidana = snapshot.presensi.first { it.kode == "FHK25601033" }
        assertEquals(7, pidana.hadir)
        assertEquals(50, pidana.persen)

        // Info kampus — baris mentah UPPERCASE_SNAKE dipertahankan verbatim
        assertEquals(3, snapshot.info.size)
        val status = snapshot.info.first { it.jenis == "status_mhs" }
        assertTrue(status.dataJson.contains("\"NIM_MHS\":\"626103051310\""))
        assertTrue(status.dataJson.contains("\"NM_PENGGUNA\":\"Arya Rizky\""))
        assertTrue(status.dataJson.contains("\"STATUS_AKADEMIK\":\"Aktif\""))
        assertEquals("2026-08-17T04:05:06.123Z", status.updatedAt)
        assertTrue(snapshot.info.first { it.jenis == "peserta_mk" }.dataJson.contains("\"KODE_MK\":\"FHK25601032\""))
        assertTrue(snapshot.info.first { it.jenis == "kalender_akademik" }.dataJson.contains("\"KEGIATAN\":\"KPRS\""))

        // round-trip: dataJson ter-parse kembali jadi JSON valid dengan field persis sama
        val row = Json.parseToJsonElement(status.dataJson).jsonArray[0].jsonObject
        assertEquals("626103051310", row["NIM_MHS"]?.jsonPrimitive?.content)
        assertEquals("Ilmu Hukum", row["NM_PROGRAM_STUDI"]?.jsonPrimitive?.content)
        assertEquals("Aktif", row["STATUS_AKADEMIK"]?.jsonPrimitive?.content)
    }

    @Test
    fun `connected false - snapshot kosong tanpa crash`() {
        val snapshot = json.decodeFromString<KampusInfoEnvelopeDto>("""{"connected":false}""").toSnapshot()

        assertFalse(snapshot.connected)
        assertNull(snapshot.lastSyncAt)
        assertTrue(snapshot.presensi.isEmpty())
        assertTrue(snapshot.info.isEmpty())
    }

    @Test
    fun `field hilang atau tidak dikenal - ignoreUnknownKeys dan coerce, tidak crash`() {
        val snapshot = json.decodeFromString<KampusInfoEnvelopeDto>(
            """
            {"items":[
                {"jenis":"presensi","data":[
                    {"code":"FHK25601040","name":"Hukum Teknologi","tm":null,"hadir":null,"persen":null,"fieldAsing":"x"}
                ]},
                {"jenis":"status_mhs","data":[{"NIM_MHS":"123","ASING":"?"}]},
                {"jenis":"jenis_baru_tak_dikenal","data":[]},
                {"jenis":"jenis_baru_tanpa_data"}
            ]}
            """.trimIndent(),
        ).toSnapshot()

        assertEquals(1, snapshot.presensi.size)
        assertNull(snapshot.presensi.single().hadir) // null dari server → null (coerce)
        assertEquals(3, snapshot.info.size) // jenis tak dikenal tetap disimpan raw
        assertTrue(snapshot.info.first { it.jenis == "status_mhs" }.dataJson.contains("NIM_MHS"))
        assertEquals("[]", snapshot.info.first { it.jenis == "jenis_baru_tanpa_data" }.dataJson)
    }

    @Test
    fun `data presensi rusak - dilewati toleran seperti web, tidak crash`() {
        val snapshot = json.decodeFromString<KampusInfoEnvelopeDto>(
            """
            {"items":[
                {"jenis":"presensi","data":"bukan-array"},
                {"jenis":"presensi","data":[{"code":"FHK25601041","name":"Hukum Perdata","tm":7,"hadir":7,"persen":100}]}
            ]}
            """.trimIndent(),
        ).toSnapshot()

        assertEquals(1, snapshot.presensi.size)
        assertEquals("FHK25601041", snapshot.presensi.single().kode)
        assertEquals(100, snapshot.presensi.single().persen)
    }
}
