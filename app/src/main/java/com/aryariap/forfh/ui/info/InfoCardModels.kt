package com.aryariap.forfh.ui.info

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Model tampil per jenis info kampus (V1.1 Task 9): dataJson baris mentah → model
 * berdesain per jenis, bukan dump label:nilai generik. Murni (tanpa Compose) — diuji
 * JUnit murni. Semua nilai hanya dari data (R-17); jenis tak dikenal → kartu generic
 * (label map penuh + humanize, InfoFormat.kampusRows); field tak dikenal pada baris
 * typed → extras berlabel (data tetap terlihat, tidak dibuang).
 */
sealed interface InfoCardModel {
    /** true = tidak ada satu pun konten untuk ditampilkan (kartu menampilkan teks kosong). */
    val isEmpty: Boolean
}

/** status_mhs — kartu identitas: NIM + badge status akademik + fakta terpilih. */
data class IdentityCard(
    val nim: String?,
    val nama: String?,
    val status: String?,
    val prodi: String?,
    val jenjang: String?,
    val fakultas: String?,
    val angkatan: String?,
    val jk: String?,
    val agama: String?,
    /** Sisa field baris yang tidak masuk slot di atas — label map penuh. */
    val extras: List<Pair<String, String>>,
) : InfoCardModel {
    override val isEmpty: Boolean
        get() = nim == null && nama == null && status == null && prodi == null && jenjang == null &&
            fakultas == null && angkatan == null && jk == null && agama == null && extras.isEmpty()
}

/** peserta_mk — daftar mata kuliah: kode/nama, chip SKS, kelas/dosen/hari/ruang. */
data class CourseListModel(
    val courses: List<CourseRow>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = courses.isEmpty()
}

data class CourseRow(
    val kode: String?,
    val nama: String?,
    val kelas: String?,
    val sks: String?,
    val dosen: String?,
    val hari: String?,
    val ruang: String?,
    /** Sisa field baris (kuota, peserta, dll.) — label map penuh. */
    val extras: List<Pair<String, String>>,
)

/** hist_her — daftar riwayat HER: nama MK, no ujian, nilai huruf sebagai fokus. */
data class HerListModel(
    val rows: List<HerRow>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = rows.isEmpty()
}

data class HerRow(
    val nama: String?,
    val noUjian: String?,
    val periode: String?,
    val nilai: String?,
    val nilaiHuruf: String?,
    val sks: String?,
    /** Sisa field baris — label map penuh. */
    val extras: List<Pair<String, String>>,
) {
    /**
     * Nilai yang ditampilkan sebagai fokus: huruf bila ada, skor numerik bila tidak
     * (fix review: baris dengan NILAI saja tidak boleh kehilangan nilainya).
     */
    val grade: String? get() = nilaiHuruf ?: nilai
}

/** pembayaran — daftar tagihan: kegiatan/semester, nominal Rupiah sebagai fokus. */
data class PaymentListModel(
    val rows: List<PaymentRow>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = rows.isEmpty()
}

data class PaymentRow(
    val kegiatan: String?,
    val semester: String?,
    /** Nominal mentah dari server — diformat Rupiah di tampilan via InfoFormat.formatRupiah. */
    val nominal: String?,
    val tglBayar: String?,
    val status: String?,
    /** Sisa field baris — label map penuh. */
    val extras: List<Pair<String, String>>,
)

/** kalender_akademik — daftar kegiatan + rentang tanggal. */
data class CalendarListModel(
    val rows: List<CalendarRow>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = rows.isEmpty()
}

data class CalendarRow(
    val kegiatan: String?,
    val mulai: String?,
    val selesai: String?,
    /** Sisa field baris — label map penuh. */
    val extras: List<Pair<String, String>>,
)

/** masa_studi / sks_aktif / penyerahan_ktm — kartu rekap: fakta kunci + sisa field. */
data class SummaryCardModel(
    val headline: List<Pair<String, String>>,
    val rows: List<Pair<String, String>>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = headline.isEmpty() && rows.isEmpty()
}

/** dosen_wali — satu kartu ringkas per dosen. */
data class DosenWaliModel(
    val dosen: List<DosenFacts>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = dosen.isEmpty()
}

data class DosenFacts(
    val nama: String?,
    /** Fakta lain (NIDN, NIP, email, dll.) — label map penuh. */
    val facts: List<Pair<String, String>>,
)

/** instruksi_tugas — blok teks terbaca per kursus: kode/nama + section berisi teks. */
data class InstructionBlockModel(
    val courses: List<CourseInstruction>,
) : InfoCardModel {
    override val isEmpty: Boolean get() = courses.isEmpty()
}

data class CourseInstruction(
    val kode: String?,
    val nama: String?,
    val sections: List<SectionInstruction>,
)

data class SectionInstruction(
    val nama: String?,
    val teks: String?,
    /** Nama aktivitas dalam section (data-activityname web) — konten, bukan kunci matching. */
    val assignments: List<String> = emptyList(),
)

/** Fallback jenis tak dikenal (atau baris tak beraturan) — kartu generic label map penuh. */
data class GenericRowModel(
    val rows: InfoRows,
) : InfoCardModel {
    override val isEmpty: Boolean get() = rows.blocks.isEmpty()
}

/** Tone badge status akademik: Aktif = positif (hijau), lainnya = netral. */
enum class StatusTone { POSITIVE, NEUTRAL }

internal const val JENIS_STATUS_MHS = "status_mhs"
internal const val JENIS_PESERTA_MK = "peserta_mk"
internal const val JENIS_HIST_HER = "hist_her"
internal const val JENIS_PEMBAYARAN = "pembayaran"
internal const val JENIS_KALENDER = "kalender_akademik"
internal const val JENIS_DOSEN_WALI = "dosen_wali"
internal const val JENIS_MASA_STUDI = "masa_studi"
internal const val JENIS_SKS_AKTIF = "sks_aktif"
internal const val JENIS_PENYERAHAN_KTM = "penyerahan_ktm"
internal const val JENIS_INSTRUKSI_TUGAS = "instruksi_tugas"

object InfoCardModels {

    /**
     * Klasifikasi tone badge — SELALU dari nilai asli STATUS_AKADEMIK (R-17, R-38):
     * nilai yang diawali "aktif" (case-insensitive) → POSITIVE, lainnya → NEUTRAL.
     * Teks badge tetap nilai aslinya; tidak ada teks yang dikarang.
     */
    fun statusTone(status: String?): StatusTone {
        val s = status?.trim().orEmpty()
        if (s.isEmpty()) return StatusTone.NEUTRAL
        return if (s.lowercase().startsWith("aktif")) StatusTone.POSITIVE else StatusTone.NEUTRAL
    }

    private typealias CardBuilder = (jenis: String, rows: List<JsonObject>) -> InfoCardModel

    private val CARD_BUILDERS: Map<String, CardBuilder> = mapOf(
        JENIS_STATUS_MHS to { _, rows -> identityCard(rows) },
        JENIS_PESERTA_MK to { _, rows -> courseList(rows) },
        JENIS_HIST_HER to { _, rows -> herList(rows) },
        JENIS_PEMBAYARAN to { _, rows -> paymentList(rows) },
        JENIS_KALENDER to { _, rows -> calendarList(rows) },
        JENIS_DOSEN_WALI to { _, rows -> dosenWali(rows) },
        JENIS_MASA_STUDI to ::summaryCard,
        JENIS_SKS_AKTIF to ::summaryCard,
        JENIS_PENYERAHAN_KTM to ::summaryCard,
        JENIS_INSTRUKSI_TUGAS to { _, rows -> instructions(rows) },
    )

    /** dataJson baris mentah → model per jenis; jenis tak dikenal → generic (label penuh). */
    fun buildInfoCardModel(jenis: String, dataJson: String): InfoCardModel {
        val rows = parseRows(dataJson)
        val builder = CARD_BUILDERS[jenis]
        return if (builder != null) {
            builder(jenis, rows)
        } else {
            GenericRowModel(InfoFormat.kampusRows(dataJson))
        }
    }

    // ---------- builder per jenis ----------

    /** Slot field utama kartu identitas. */
    private val STATUS_KEYS = arrayOf(
        "NIM_MHS", "NIM",                      // 0-1: nim
        "NM_PENGGUNA", "NAMA_MHS", "NAMA",     // 2-4: nama
        "NM_PROGRAM_STUDI", "PROGRAM_STUDI", "PRODI", // 5-7: prodi
        "JENJANG", "NM_JENJANG", "ID_JENJANG", // 8-10: jenjang
        "FAKULTAS",                            // 11: fakultas
        "ANGKATAN", "THN_ANGKATAN_MHS",        // 12-13: angkatan
        "STATUS_AKADEMIK", "NM_STATUS_PENGGUNA", // 14-15: status
        "JK",                                  // 16
        "AGAMA",                               // 17
    )

    /** Kartu identitas dipakai untuk baris tunggal (profil); lebih dari itu → generic. */
    private fun identityCard(rows: List<JsonObject>): InfoCardModel {
        if (rows.size != 1) return GenericRowModel(InfoFormat.kampusRows(rowsToJson(rows)))
        val obj = rows.single()
        return IdentityCard(
            nim = pick(obj, STATUS_KEYS[0], STATUS_KEYS[1]),
            nama = pick(obj, STATUS_KEYS[2], STATUS_KEYS[3], STATUS_KEYS[4]),
            status = pick(obj, STATUS_KEYS[14], STATUS_KEYS[15]),
            prodi = pick(obj, STATUS_KEYS[5], STATUS_KEYS[6], STATUS_KEYS[7]),
            jenjang = pick(obj, STATUS_KEYS[8], STATUS_KEYS[9], STATUS_KEYS[10]),
            fakultas = pick(obj, STATUS_KEYS[11]),
            angkatan = pick(obj, STATUS_KEYS[12], STATUS_KEYS[13]),
            jk = pick(obj, STATUS_KEYS[16]),
            agama = pick(obj, STATUS_KEYS[17]),
            extras = extras(obj, consumedOf(*STATUS_KEYS)),
        )
    }

    /** Slot field utama baris peserta MK. */
    private val COURSE_KEYS = arrayOf(
        "KODE_MK", "KD_MATA_KULIAH", "KODE",
        "NM_MATA_KULIAH", "NAMA_MK", "NAMA",
        "NAMA_KELAS", "KELAS",
        "SKS", "KREDIT_SEMESTER",
        "NM_DOSEN", "DOSEN", "NAMA_DOSEN",
        "HARI",
        "RUANG", "RUANGAN", "NM_RUANGAN",
    )

    private fun courseList(rows: List<JsonObject>): CourseListModel {
        val courses = rows.map { obj ->
            CourseRow(
                kode = pick(obj, COURSE_KEYS[0], COURSE_KEYS[1], COURSE_KEYS[2]),
                nama = pick(obj, COURSE_KEYS[3], COURSE_KEYS[4], COURSE_KEYS[5]),
                kelas = pick(obj, COURSE_KEYS[6], COURSE_KEYS[7]),
                sks = pick(obj, COURSE_KEYS[8], COURSE_KEYS[9]),
                dosen = pick(obj, COURSE_KEYS[10], COURSE_KEYS[11], COURSE_KEYS[12]),
                hari = pick(obj, COURSE_KEYS[13]),
                ruang = pick(obj, COURSE_KEYS[14], COURSE_KEYS[15], COURSE_KEYS[16]),
                extras = extras(obj, consumedOf(*COURSE_KEYS)),
            )
        }
        return CourseListModel(courses)
    }

    /** Slot field utama baris HER. */
    private val HER_KEYS = arrayOf(
        "NM_MATA_KULIAH", "NAMA_MK", "NAMA",
        "NO_UJIAN",
        "NM_SEMESTER", "ID_SEMESTER", "SEMESTER",
        "NILAI",
        "NILAI_HURUF",
        "SKS", "KREDIT_SEMESTER",
    )

    private fun herList(rows: List<JsonObject>): HerListModel {
        val herRows = rows.map { obj ->
            HerRow(
                nama = pick(obj, HER_KEYS[0], HER_KEYS[1], HER_KEYS[2]),
                noUjian = pick(obj, HER_KEYS[3]),
                periode = pick(obj, HER_KEYS[4], HER_KEYS[5], HER_KEYS[6]),
                nilai = pick(obj, HER_KEYS[7]),
                nilaiHuruf = pick(obj, HER_KEYS[8]),
                sks = pick(obj, HER_KEYS[9], HER_KEYS[10]),
                extras = extras(obj, consumedOf(*HER_KEYS)),
            )
        }
        return HerListModel(herRows)
    }

    /** Slot field utama baris pembayaran. */
    private val PAYMENT_KEYS = arrayOf(
        "KEGIATAN", "NM_KEGIATAN", "AGENDA",
        "TAHUN_AJARAN",
        "NM_SEMESTER", "PERIODE",
        "NOMINAL", "NOMINAL_BAYAR", "JUMLAH",
        "TGL_BAYAR", "TANGGAL", "TGL",
        "NAMA_STATUS", "STATUS",
    )

    private fun paymentList(rows: List<JsonObject>): PaymentListModel {
        val payments = rows.map { obj ->
            val tahunAjaran = pick(obj, PAYMENT_KEYS[3])
            val periode = pick(obj, PAYMENT_KEYS[4], PAYMENT_KEYS[5])
            PaymentRow(
                kegiatan = pick(obj, PAYMENT_KEYS[0], PAYMENT_KEYS[1], PAYMENT_KEYS[2]),
                semester = listOfNotNull(tahunAjaran, periode).joinToString(" "),
                nominal = pick(obj, PAYMENT_KEYS[6], PAYMENT_KEYS[7], PAYMENT_KEYS[8]),
                tglBayar = pick(obj, PAYMENT_KEYS[9], PAYMENT_KEYS[10], PAYMENT_KEYS[11]),
                status = pick(obj, PAYMENT_KEYS[12], PAYMENT_KEYS[13]),
                extras = extras(obj, consumedOf(*PAYMENT_KEYS)),
            )
        }
        return PaymentListModel(payments)
    }

    /** Slot field utama baris kalender akademik. */
    private val CALENDAR_KEYS = arrayOf(
        "KEGIATAN", "NM_KEGIATAN", "AGENDA",
        "TGL_MULAI", "TGL_MULAI_JSF", "MULAI", "AWAL", "TANGGAL_AWAL",
        "TGL_SELESAI", "TGL_SELESAI_JSF", "SELESAI", "AKHIR", "TANGGAL_AKHIR",
    )

    private fun calendarList(rows: List<JsonObject>): CalendarListModel {
        val calendar = rows.map { obj ->
            CalendarRow(
                kegiatan = pick(obj, CALENDAR_KEYS[0], CALENDAR_KEYS[1], CALENDAR_KEYS[2]),
                mulai = pick(obj, CALENDAR_KEYS[3], CALENDAR_KEYS[4], CALENDAR_KEYS[5], CALENDAR_KEYS[6], CALENDAR_KEYS[7]),
                selesai = pick(obj, CALENDAR_KEYS[8], CALENDAR_KEYS[9], CALENDAR_KEYS[10], CALENDAR_KEYS[11], CALENDAR_KEYS[12]),
                extras = extras(obj, consumedOf(*CALENDAR_KEYS)),
            )
        }
        return CalendarListModel(calendar)
    }

    private fun dosenWali(rows: List<JsonObject>): DosenWaliModel {
        val dosen = rows.map { obj ->
            val nama = pick(obj, "NM_DOSEN", "DOSEN_WALI", "NAMA_DOSEN")
            val consumed = consumedOf("NM_DOSEN", "DOSEN_WALI", "NAMA_DOSEN")
            DosenFacts(nama = nama, facts = extras(obj, consumed))
        }
        return DosenWaliModel(dosen)
    }

    /** Fakta kunci per jenis rekap (masa_studi / sks_aktif / penyerahan_ktm). */
    private val SUMMARY_HEADLINE: Map<String, List<String>> = mapOf(
        JENIS_MASA_STUDI to listOf("LAMA_STUDI", "TGL_MULAI", "TGL_SELESAI"),
        JENIS_SKS_AKTIF to listOf("SKS_TEMPUH", "JUM_MK"),
        JENIS_PENYERAHAN_KTM to listOf("TGL_VERIFIKASI_PENDIDIKAN", "TANGGAL", "TGL", "STATUS", "NAMA_STATUS"),
    )

    /** Kartu rekap untuk baris tunggal (masa studi/SKS aktif/KTM satu record); lebih → generic. */
    private fun summaryCard(jenis: String, rows: List<JsonObject>): InfoCardModel {
        if (rows.size != 1) return GenericRowModel(InfoFormat.kampusRows(rowsToJson(rows)))
        val obj = rows.single()
        val keys = SUMMARY_HEADLINE.getValue(jenis)
        val headline = ArrayList<Pair<String, String>>()
        val seen = HashSet<String>()
        for (key in keys) {
            val value = pick(obj, key) ?: continue
            val label = InfoFormat.fieldLabel(key)
            if (!seen.add(label)) continue // TANGGAL dan TGL punya label sama → sekali saja
            headline += label to value
        }
        val rest = extras(obj, consumedOf(*keys.toTypedArray()))
        return SummaryCardModel(headline, rest)
    }

    /**
     * Baris instruksi tugas HE-BAT berbentuk objek camelCase (HebatCourseInstructions web):
     * {courseId, shortname, fullname, sections:[{sectionName, summary, assignments}]}.
     * Teks instruksi = section summary; assignments = daftar nama aktivitas (data-activityname,
     * mis. "Pengumpulan Tugas Resume Buku PIH Prof Peter Bab I") — konten yang dicari user,
     * BUKAN kunci matching (fix review). Keduanya dipertahankan; section dengan salah satu
     * konten tetap tampil; yang benar-benar kosong dilewati.
     */
    private fun instructions(rows: List<JsonObject>): InstructionBlockModel {
        val courses = rows.mapNotNull { obj ->
            val sections = (obj["sections"] as? JsonArray).orNull().mapNotNull { item ->
                val section = item as? JsonObject ?: return@mapNotNull null
                val teks = (section["summary"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                val nama = (section["sectionName"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                val assignments = (section["assignments"] as? JsonArray).orNull()
                    .mapNotNull { (it as? JsonPrimitive)?.content?.takeIf { a -> a.isNotBlank() } }
                if (teks == null && assignments.isEmpty()) return@mapNotNull null
                SectionInstruction(nama, teks, assignments)
            }
            if (sections.isEmpty()) return@mapNotNull null
            CourseInstruction(
                kode = (obj["shortname"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
                nama = (obj["fullname"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() },
                sections = sections,
            )
        }
        return InstructionBlockModel(courses)
    }

    // ---------- helper parse ----------

    /** dataJson → daftar objek baris; rusak/bukan array → kosong (toleran, pola web). */
    private fun parseRows(dataJson: String): List<JsonObject> {
        val element = runCatching { Json.parseToJsonElement(dataJson) }.getOrNull()
            ?: return emptyList()
        if (element !is JsonArray) return emptyList()
        return element.mapNotNull { it as? JsonObject }
    }

    /** Baris JsonObject → JSON string lagi (fallback generic saat baris tak beraturan). */
    private fun rowsToJson(rows: List<JsonObject>): String = buildString {
        append('[')
        rows.forEachIndexed { i, obj ->
            if (i > 0) append(',')
            append(obj.toString())
        }
        append(']')
    }

    /**
     * Nilai scalar pertama yang ada dari daftar kunci — tiap kunci dicoba versi aslinya
     * DAN lowercase-nya (API mencampur kasus: UPPERCASE_SNAKE vs lowercase snake).
     * null/""/null JSON dilewati; objek/array bukan nilai fakta → dilewati.
     */
    private fun pick(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            scalar(obj, key)?.let { return it }
            if (key != key.lowercase()) scalar(obj, key.lowercase())?.let { return it }
        }
        return null
    }

    private fun scalar(obj: JsonObject, key: String): String? {
        val value = obj[key] ?: return null
        if (value is JsonNull) return null
        val s = (value as? JsonPrimitive)?.content ?: return null
        return s.takeIf { it.isNotBlank() }
    }

    /** Key konsumsi (kunci + lowercase) untuk slot typed — sisa field jadi extras. */
    private fun consumedOf(vararg keys: String): Set<String> = buildSet {
        for (key in keys) {
            add(key)
            add(key.lowercase())
        }
    }

    /** Field yang tidak masuk slot typed → pasangan label map penuh (data tidak dibuang). */
    private fun extras(obj: JsonObject, consumed: Set<String>): List<Pair<String, String>> =
        obj.entries.mapNotNull { (key, value) ->
            if (key in consumed) null
            else jsonValueText(value)?.let { InfoFormat.fieldLabel(key) to it }
        }

    /** Nilai JSON → teks; null/"" → null; objek/array → JSON mentah (sama InfoFormat). */
    private fun jsonValueText(value: JsonElement): String? = when (value) {
        is JsonNull -> null
        is JsonPrimitive -> value.content.ifBlank { null }
        else -> value.toString()
    }

    private fun JsonArray?.orNull(): JsonArray = this ?: JsonArray(emptyList())
}
