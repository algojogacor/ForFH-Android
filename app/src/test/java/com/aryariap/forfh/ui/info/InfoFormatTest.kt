package com.aryariap.forfh.ui.info

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InfoFormat — helper murni layar Info (Task 8): label Indonesia utk field UPPERCASE_SNAKE
 * Kampus Kita, judul per jenis, parse dataJson mentah → blok label:nilai, format baris
 * presensi, format updatedAt. Semua angka hanya dari data (R-17); string tanpa em dash (R-02).
 */
class InfoFormatTest {

    // ---------- label map ----------

    @Test
    fun `fieldLabel - field dikenal mendapat label Indonesia`() {
        assertEquals("NIM", InfoFormat.fieldLabel("NIM_MHS"))
        assertEquals("Nama", InfoFormat.fieldLabel("NM_PENGGUNA"))
        assertEquals("Prodi", InfoFormat.fieldLabel("NM_PROGRAM_STUDI"))
        assertEquals("Jenis Kelamin", InfoFormat.fieldLabel("JK"))
        assertEquals("Status Akademik", InfoFormat.fieldLabel("STATUS_AKADEMIK"))
        assertEquals("Nama MK", InfoFormat.fieldLabel("NM_MATA_KULIAH"))
        assertEquals("Kode MK", InfoFormat.fieldLabel("KODE_MK"))
        assertEquals("Kelas", InfoFormat.fieldLabel("NAMA_KELAS"))
        assertEquals("Dosen", InfoFormat.fieldLabel("NM_DOSEN"))
        assertEquals("Kegiatan", InfoFormat.fieldLabel("KEGIATAN"))
        assertEquals("Tanggal Mulai", InfoFormat.fieldLabel("TGL_MULAI"))
        assertEquals("Tanggal Selesai", InfoFormat.fieldLabel("TGL_SELESAI"))
    }

    @Test
    fun `fieldLabel - field tak dikenal ditampilkan apa adanya (jujur, pola web)`() {
        assertEquals("ASING_BARU", InfoFormat.fieldLabel("ASING_BARU"))
    }

    @Test
    fun `jenisTitle - jenis dikenal mendapat judul Indonesia`() {
        assertEquals("Status Mahasiswa", InfoFormat.jenisTitle("status_mhs"))
        assertEquals("Kalender Akademik", InfoFormat.jenisTitle("kalender_akademik"))
        assertEquals("Peserta Mata Kuliah", InfoFormat.jenisTitle("peserta_mk"))
        assertEquals("Riwayat HER", InfoFormat.jenisTitle("hist_her"))
    }

    @Test
    fun `jenisTitle - jenis baru tak dikenal ditampilkan apa adanya`() {
        assertEquals("jenis_baru", InfoFormat.jenisTitle("jenis_baru"))
    }

    // ---------- baris presensi ----------

    @Test
    fun `formatPresensi - semua angka tersedia`() {
        assertEquals("Hadir 13 dari 14 pertemuan (93%)", InfoFormat.formatPresensi(14, 13, 93))
    }

    @Test
    fun `formatPresensi - tanpa persen`() {
        assertEquals("Hadir 13 dari 14 pertemuan", InfoFormat.formatPresensi(14, 13, null))
    }

    @Test
    fun `formatPresensi - sebagian tersedia`() {
        assertEquals("Hadir 13", InfoFormat.formatPresensi(null, 13, null))
        assertEquals("14 pertemuan", InfoFormat.formatPresensi(14, null, null))
        assertEquals("93%", InfoFormat.formatPresensi(null, null, 93))
    }

    @Test
    fun `formatPresensi - tidak ada angka - teks jujur, bukan angka palsu`() {
        assertEquals("Kehadiran belum tersedia", InfoFormat.formatPresensi(null, null, null))
    }

    @Test
    fun `formatPresensi - nol tetap ditampilkan (data nyata)`() {
        assertEquals("Hadir 0 dari 14 pertemuan (0%)", InfoFormat.formatPresensi(14, 0, 0))
    }

    // ---------- updatedAt ----------

    @Test
    fun `formatUpdatedAt - ISO Z diparse ke WIB (UTC+7)`() {
        assertEquals("17 Agu 2026, 11:05", InfoFormat.formatUpdatedAt("2026-08-17T04:05:06.123Z"))
    }

    @Test
    fun `formatUpdatedAt - tak bisa diparse - null (jangan ditampilkan)`() {
        assertNull(InfoFormat.formatUpdatedAt(""))
        assertNull(InfoFormat.formatUpdatedAt("bukan-iso"))
        assertNull(InfoFormat.formatUpdatedAt("2026-08-17"))
    }

    // ---------- parse dataJson ----------

    @Test
    fun `kampusRows - status_mhs sample - satu blok, label Indonesia`() {
        val rows = InfoFormat.kampusRows(
            """
            [{"NIM_MHS":"626103051310","NM_PENGGUNA":"Arya Rizky","NM_PROGRAM_STUDI":"Ilmu Hukum",
              "JENJANG":"S1","FAKULTAS":"Fakultas Hukum","ANGKATAN":"2026",
              "STATUS_AKADEMIK":"Aktif","JK":"L","AGAMA":"Islam"}]
            """.trimIndent(),
        )
        assertEquals(1, rows.blocks.size)
        assertEquals(0, rows.skippedRecords)
        val map = rows.blocks.single().rows.toMap()
        assertEquals(9, map.size)
        assertEquals("626103051310", map["NIM"])
        assertEquals("Arya Rizky", map["Nama"])
        assertEquals("Ilmu Hukum", map["Prodi"])
        assertEquals("S1", map["Jenjang"])
        assertEquals("Fakultas Hukum", map["Fakultas"])
        assertEquals("2026", map["Angkatan"])
        assertEquals("Aktif", map["Status Akademik"])
        assertEquals("L", map["Jenis Kelamin"])
        assertEquals("Islam", map["Agama"])
    }

    @Test
    fun `kampusRows - beberapa record - satu blok per record, urutan dipertahankan`() {
        val rows = InfoFormat.kampusRows(
            """
            [{"KODE_MK":"FHK25601032","NM_MATA_KULIAH":"Hak Asasi Manusia","NAMA_KELAS":"A-1","SKS":"3","NM_DOSEN":"Dr. X"},
             {"KODE_MK":"FHK25601033","NM_MATA_KULIAH":"Hukum Acara Pidana","NAMA_KELAS":"A-1","SKS":"3"}]
            """.trimIndent(),
        )
        assertEquals(2, rows.blocks.size)
        assertEquals("FHK25601032", rows.blocks[0].rows.first().second)
        assertEquals("Hak Asasi Manusia", rows.blocks[0].rows[1].second)
        assertEquals("FHK25601033", rows.blocks[1].rows.first().second)
    }

    @Test
    fun `kampusRows - nilai null dan kosong dilewati, key tak dikenal tampil mentah`() {
        val rows = InfoFormat.kampusRows(
            """[{"TGL_SELESAI":null,"KEGIATAN":"","TGL_MULAI":"2026-08-24","ASING":"?"}]""",
        )
        assertEquals(1, rows.blocks.size)
        val map = rows.blocks.single().rows.toMap()
        assertEquals(2, map.size)
        assertEquals("2026-08-24", map["Tanggal Mulai"])
        assertEquals("?", map["ASING"])
    }

    @Test
    fun `kampusRows - record non-object dilewati tanpa crash`() {
        val rows = InfoFormat.kampusRows("""[null,"teks",5,{"A":"B"}]""")
        assertEquals(1, rows.blocks.size)
        assertEquals("B", rows.blocks.single().rows.single().second)
    }

    @Test
    fun `kampusRows - nilai objek ditampilkan sebagai JSON mentah`() {
        val rows = InfoFormat.kampusRows("""[{"DATA":{"a":1,"b":true}}]""")
        val value = rows.blocks.single().rows.single().second
        assertTrue(value.contains("\"a\":1"))
        assertTrue(value.contains("\"b\":true"))
    }

    @Test
    fun `kampusRows - lebih dari maxRecords - dipangkas dengan catatan jumlah`() {
        val json = buildString {
            append('[')
            repeat(30) { i ->
                if (i > 0) append(',')
                append("""{"KODE_MK":"MK$i","NM_MATA_KULIAH":"MK $i"}""")
            }
            append(']')
        }
        val rows = InfoFormat.kampusRows(json, maxRecords = 25)
        assertEquals(25, rows.blocks.size)
        assertEquals(5, rows.skippedRecords)
    }

    @Test
    fun `kampusRows - dataJson kosong, bukan array, atau rusak - hasil kosong tanpa crash`() {
        assertEquals(0, InfoFormat.kampusRows("[]").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("{}").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("bukan-json").blocks.size)
        assertEquals(0, InfoFormat.kampusRows("").blocks.size)
    }
}
