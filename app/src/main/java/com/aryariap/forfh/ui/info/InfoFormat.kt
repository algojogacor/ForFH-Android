package com.aryariap.forfh.ui.info

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Label Indonesia untuk field UPPERCASE_SNAKE dari Kampus Kita — padanan CAMPUS_FIELD_LABELS
 * web ForFH (src/components/campus/campusMeta.ts), untuk field yang benar-benar muncul di
 * sample data (ruling R22/R23). Field tak dikenal ditampilkan apa adanya (label = key mentah,
 * jujur — pola web "CAMPUS_FIELD_LABELS[c] || c").
 */
internal val KAMPUS_FIELD_LABELS: Map<String, String> = mapOf(
    "NIM_MHS" to "NIM",
    "NM_PENGGUNA" to "Nama",
    "NM_PROGRAM_STUDI" to "Prodi",
    "JENJANG" to "Jenjang",
    "FAKULTAS" to "Fakultas",
    "ANGKATAN" to "Angkatan",
    "STATUS_AKADEMIK" to "Status Akademik",
    "JK" to "Jenis Kelamin",
    "AGAMA" to "Agama",
    "KODE_MK" to "Kode MK",
    "NM_MATA_KULIAH" to "Nama MK",
    "NAMA_KELAS" to "Kelas",
    "SKS" to "SKS",
    "NM_DOSEN" to "Dosen",
    "KEGIATAN" to "Kegiatan",
    "TGL_MULAI" to "Tanggal Mulai",
    "TGL_SELESAI" to "Tanggal Selesai",
)

/**
 * Judul Indonesia per jenis info kampus — padanan campusMeta web (title per jenis).
 * Jenis baru (server menambahkan) tampil dengan key mentah — jujur, bukan terjemahan palsu.
 */
internal val KAMPUS_JENIS_TITLES: Map<String, String> = mapOf(
    "status_mhs" to "Status Mahasiswa",
    "kalender_akademik" to "Kalender Akademik",
    "dosen_wali" to "Dosen Wali",
    "masa_studi" to "Masa Studi",
    "sks_aktif" to "SKS Aktif",
    "peserta_mk" to "Peserta Mata Kuliah",
    "hist_her" to "Riwayat HER",
    "penyerahan_ktm" to "Penyerahan KTM",
    "pembayaran" to "Pembayaran",
)

/** Satu record (satu baris JSON) hasil parse dataJson — pasangan label: nilai. */
data class InfoRowBlock(
    val rows: List<Pair<String, String>>,
)

/** Hasil parse satu dataJson: blok per record + berapa record dipangkas (batas tampil). */
data class InfoRows(
    val blocks: List<InfoRowBlock>,
    val skippedRecords: Int,
)

/** Helper murni layar Info — semua angka hanya dari data server (R-17), tanpa em dash (R-02). */
object InfoFormat {

    private val updatedFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("id", "ID"))
    private val wib = ZoneId.of("Asia/Jakarta")

    /** Label Indonesia; field tak dikenal → key mentah (jujur, pola web). */
    fun fieldLabel(key: String): String = KAMPUS_FIELD_LABELS[key] ?: key

    /** Judul Indonesia per jenis; jenis baru → key mentah. */
    fun jenisTitle(jenis: String): String = KAMPUS_JENIS_TITLES[jenis] ?: jenis

    /**
     * Baris rekap presensi per MK: "Hadir 13 dari 14 pertemuan (93%)".
     * Hanya angka yang tersedia yang ditampilkan; tanpa satu angka pun → teks jujur,
     * bukan angka palsu (R-17).
     */
    fun formatPresensi(tm: Int?, hadir: Int?, persen: Int?): String {
        val hadirText = when {
            hadir != null && tm != null -> "Hadir $hadir dari $tm pertemuan"
            hadir != null -> "Hadir $hadir"
            tm != null -> "$tm pertemuan"
            else -> null
        }
        return when {
            hadirText != null && persen != null -> "$hadirText ($persen%)"
            hadirText != null -> hadirText
            persen != null -> "$persen%"
            else -> "Kehadiran belum tersedia"
        }
    }

    /** ISO-8601 server → "17 Agu 2026, 04:05" WIB; null saat tak bisa diparse (jangan ditampilkan). */
    fun formatUpdatedAt(iso: String): String? = runCatching {
        Instant.parse(iso).atZone(wib).format(updatedFmt)
    }.getOrNull()

    /**
     * Footer layar Info: umur data kampus dari meta.lastSyncAt (bukan waktu sync jadwal/tugas,
     * dan tanpa em dash — fix review). Tanggal tak bisa diparse / belum pernah → teks jujur.
     */
    fun kampusUpdatedText(lastSyncAt: String?): String {
        val formatted = lastSyncAt?.let { formatUpdatedAt(it) }
        return if (formatted != null) "Info terakhir diperbarui $formatted" else "Info kampus belum pernah diperbarui"
    }

    /**
     * dataJson (array baris mentah UPPERCASE_SNAKE) → blok label:nilai per record.
     * Nilai null/"" dilewati (kosong lebih jujur daripada placeholder), record non-object
     * atau tanpa satu pun nilai dilewati. Batas maxRecords per jenis (pola web: cap 50 baris)
     * dengan catatan "+N lainnya" — data dipangkas TAPI diakui, tidak disembunyikan.
     */
    fun kampusRows(dataJson: String, maxRecords: Int = 25): InfoRows {
        val element = runCatching { Json.parseToJsonElement(dataJson) }.getOrNull()
            ?: return InfoRows(emptyList(), 0)
        if (element !is JsonArray) return InfoRows(emptyList(), 0)
        val blocks = ArrayList<InfoRowBlock>()
        var skipped = 0
        for (item in element) {
            if (blocks.size >= maxRecords) {
                skipped++
                continue
            }
            if (item !is JsonObject) continue
            val rows = ArrayList<Pair<String, String>>()
            for ((key, value) in item) {
                val text = valueText(value) ?: continue
                rows += fieldLabel(key) to text
            }
            if (rows.isNotEmpty()) blocks += InfoRowBlock(rows)
        }
        return InfoRows(blocks, skipped)
    }

    /** Nilai JSON → teks tampil; null dan "" → null (dilewati); objek/array → JSON mentah. */
    private fun valueText(value: JsonElement): String? = when (value) {
        is JsonNull -> null
        is JsonPrimitive -> value.content.ifBlank { null }
        else -> value.toString()
    }
}
