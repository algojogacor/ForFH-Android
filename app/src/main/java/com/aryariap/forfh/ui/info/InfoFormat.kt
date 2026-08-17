package com.aryariap.forfh.ui.info

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
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
 * Label Indonesia untuk field Kampus Kita — salinan penuh CAMPUS_FIELD_LABELS web ForFH
 * (src/components/campus/campusMeta.ts, data, bukan perilaku) + varian lowercase yang
 * benar-benar muncul di respons (device: nm_mata_kuliah/kuota/peserta/hari/ruang;
 * kk_lite & mappings.ts web: kode_mk/nama_kelas/dll) + kunci status_mhs asli
 * (THN_ANGKATAN_MHS, NM_JENJANG, NM_STATUS_PENGGUNA). Field tak dikenal → humanize
 * (Title Case), TIDAK pernah key mentah lowercase snake tampil ke user (fix review).
 */
internal val KAMPUS_FIELD_LABELS: Map<String, String> = buildMap {
    // add(key, label): key + varian lowercase-nya — API Kampus Kita mencampur kasus
    // (UPPERCASE_SNAKE di status/sks-aktif, lowercase snake di peserta_mk).
    fun add(key: String, label: String) {
        put(key, label)
        put(key.lowercase(), label)
    }
    // — salinan penuh CAMPUS_FIELD_LABELS web (campusMeta.ts) —
    add("NIM", "NIM")
    add("NIM_MHS", "NIM")
    add("NAMA", "Nama")
    add("NAMA_MHS", "Nama")
    add("NM_PENGGUNA", "Nama")
    add("NM_MATA_KULIAH", "Nama MK")
    add("NAMA_MK", "Nama MK")
    add("KD_MATA_KULIAH", "Kode MK")
    add("KODE", "Kode")
    add("KODE_MK", "Kode MK")
    add("SKS", "SKS")
    add("KREDIT_SEMESTER", "SKS")
    add("NAMA_KELAS", "Kelas")
    add("KELAS", "Kelas")
    add("NM_DOSEN", "Dosen")
    add("DOSEN", "Dosen")
    add("DOSEN_WALI", "Dosen Wali")
    add("NILAI_HURUF", "Nilai")
    add("NILAI", "Skor")
    add("NILAI_ANGKA", "Skor")
    add("ID_SEMESTER", "Semester")
    add("SEMESTER", "Semester")
    add("TAHUN_AJARAN", "Tahun Ajaran")
    add("NM_SEMESTER", "Periode")
    add("NM_PROGRAM_STUDI", "Prodi")
    add("PRODI", "Prodi")
    add("JENJANG", "Jenjang")
    add("FAKULTAS", "Fakultas")
    add("ANGKATAN", "Angkatan")
    add("STATUS_AKADEMIK", "Status Akademik")
    add("JK", "Jenis Kelamin")
    add("AGAMA", "Agama")
    add("JUM_MK", "Jumlah MK")
    add("SKS_TEMPUH", "SKS Tempuh")
    add("TANGGAL", "Tanggal")
    add("TGL", "Tanggal")
    add("TANGGAL_AWAL", "Tanggal Awal")
    add("TANGGAL_AKHIR", "Tanggal Akhir")
    add("MULAI", "Mulai")
    add("SELESAI", "Selesai")
    add("AWAL", "Awal")
    add("AKHIR", "Akhir")
    add("BERLAKU", "Berlaku")
    add("HARI", "Hari")
    add("JAM", "Jam")
    add("RUANGAN", "Ruangan")
    add("NM_RUANGAN", "Ruangan")
    add("STATUS", "Status")
    add("KET", "Keterangan")
    add("KETERANGAN", "Keterangan")
    add("URAIAN", "Uraian")
    add("NOMINAL", "Nominal")
    add("JUMLAH", "Jumlah")
    add("PERIODE", "Periode")
    add("BULAN", "Bulan")
    add("TAHUN", "Tahun")
    add("KEGIATAN", "Kegiatan")
    add("AGENDA", "Agenda")
    add("NM_KEGIATAN", "Kegiatan")
    add("TGL_MULAI", "Tanggal Mulai")
    add("TGL_MULAI_JSF", "Tanggal Mulai")
    add("TGL_SELESAI", "Tanggal Selesai")
    add("TGL_SELESAI_JSF", "Tanggal Selesai")
    add("TGL_BAYAR", "Tanggal Bayar")
    add("NOMINAL_BAYAR", "Nominal Dibayar")
    add("NAMA_STATUS", "Status")
    add("NM_BANK", "Bank")
    add("LAMA_STUDI", "Lama Studi")
    add("NO_UJIAN", "No. Ujian")
    add("TGL_VERIFIKASI_PENDIDIKAN", "Tgl Verifikasi")
    add("TGL_UPDATE_HER", "Tgl Update HER")
    add("NAMA_DOSEN", "Nama Dosen")
    add("NIP_DOSEN", "NIP")
    add("NIDN_DOSEN", "NIDN")
    add("EMAIL", "Email")
    add("BIOGRAFI", "Biografi")
    add("ID_SINTA", "ID SINTA")
    add("ID_SCOPUS", "ID Scopus")
    add("ID_FACEBOOK", "ID Facebook")
    add("ID_TWITTER", "ID Twitter")
    add("JML_HADIR", "Hadir")
    add("TOTAL_TM", "Total TM")
    add("PERSEN", "Persen")
    // — kunci yang benar-benar muncul di respons asli (device + kk_lite P map) —
    add("THN_ANGKATAN_MHS", "Angkatan")
    add("NM_JENJANG", "Jenjang")
    add("ID_JENJANG", "Jenjang")
    add("NM_STATUS_PENGGUNA", "Status Akademik")
    add("PROGRAM_STUDI", "Prodi")
    add("EMAIL_PENGGUNA", "Email")
    add("ID_MHS", "ID")
    add("IPK_MHS", "IPK")
    add("SKS_LULUS", "SKS Lulus")
    add("NM_IBU_MHS", "Nama Ibu")
    add("NM_AYAH_MHS", "Nama Ayah")
    add("RUANG", "Ruangan")
    add("KUOTA", "Kuota")
    add("PESERTA", "Peserta")
    add("WAKTU", "Waktu")
}

/**
 * Judul Indonesia per jenis info kampus — padanan CAMPUS_JENIS_META web (title per jenis,
 * tanpa instruksi_tugas di meta web tapi ada di CAMPUS_DATA_JENIS). Jenis baru → humanize,
 * jujur, bukan key mentah.
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
    "instruksi_tugas" to "Instruksi Tugas",
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
    private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
    private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale("id", "ID"))
    private val wib = ZoneId.of("Asia/Jakarta")

    /** Label Indonesia; field tak dikenal → humanize Title Case (bukan key mentah, fix review). */
    fun fieldLabel(key: String): String = KAMPUS_FIELD_LABELS[key] ?: humanizeField(key)

    /** Judul Indonesia per jenis; jenis baru → humanize (bukan key mentah). */
    fun jenisTitle(jenis: String): String = KAMPUS_JENIS_TITLES[jenis] ?: humanizeField(jenis)

    /**
     * Humanisasi fallback: snake_case / UPPERCASE_SNAKE / camelCase → "Title Case".
     * Hanya dipakai untuk field/jenis yang TIDAK ada di map — label mentah lowercase
     * snake tidak pernah tampil ke user.
     */
    fun humanizeField(key: String): String {
        if (key.isBlank()) return key
        val words = key
            .split('_', ' ')
            // camelCase → batas kata; lookbehind huruf kecil/angka supaya "ASING" (huruf
            // besar beruntun) tidak terpecah jadi "A S I N G"
            .flatMap { it.split(Regex("(?<=[a-z0-9])(?=[A-Z])")) }
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return key
        return words.joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
    }

    /**
     * Nominal → "Rp 1.234.567" (pemisah ribuan titik, tanpa desimal). Toleran:
     * "1500000", "1500000.5" (dibulatkan), "1.500.000" (format grup Indonesia) diterima;
     * bukan angka → null (baris dilewati, tidak menampilkan nilai asal) — R-17.
     */
    fun formatRupiah(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        val grouped = Regex("""^\d{1,3}([.,]\d{3})+$""").matches(s)
        val decimal = Regex("""^\d+([.,]\d+)?$""").matches(s)
        val value: Long = when {
            grouped -> s.filter { it.isDigit() }.toLongOrNull() ?: return null
            decimal -> {
                val d = s.replace(',', '.').toDoubleOrNull() ?: return null
                Math.round(d)
            }
            else -> return null
        }
        val groupedText = String.format(Locale.ROOT, "%,d", value).replace(',', '.')
        return "Rp $groupedText"
    }

    /**
     * "2026-08-24" (ISO lokal) → "24 Agu 2026"; "2026-08-03T08:00:00+07:00" (ISO lokal + offset,
     * detik opsional — format TGL_MULAI/TGL_SELESAI kalender akademik di device) →
     * "3 Agu 2026 08:00" (jam di offset aslinya, +07:00 = WIB utk data Kampus Kita);
     * tak bisa diparse → apa adanya (jujur).
     */
    fun formatIsoDate(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        val local = runCatching { LocalDate.parse(s) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(s).toLocalDate() }.getOrNull()
        if (local != null) return local.format(dateFmt)
        return runCatching { OffsetDateTime.parse(s).format(dateTimeFmt) }.getOrNull() ?: s
    }

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
     * dataJson (array baris mentah) → blok label:nilai per record (fallback kartu generic).
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
