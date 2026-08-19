package com.aryariap.forfh.data.changelog

import android.content.Context
import com.aryariap.forfh.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChangelogEntry(
    val version: String,
    val versionCode: Int,
    val date: String,
    val title: String,
    val highlights: List<String>,
)

object ChangelogCatalog {

    private val json = Json { ignoreUnknownKeys = true }

    // Static fallback list to ensure zero-latency & deterministic data availability
    private val staticEntries = listOf(
        ChangelogEntry(
            version = "2.3.0",
            versionCode = 5,
            date = "19 Agustus 2026",
            title = "Polesan Widget & Fleksibilitas Tugas",
            highlights = listOf(
                "Ikon Baru Minimalis: Logo timbangan hukum geometris elegan dengan latar obsidian black.",
                "Pembaruan Home Widget: Nama mata kuliah kini tampil jelas sebagai judul utama dan format lokasi ruangan diringkas (contoh: 'R. LG02 B') agar tidak terpotong.",
                "Fitur Pembatalan Tugas (Uncheck): Tugas yang sudah selesai kini bisa dibatalkan ceklisnya untuk dikembalikan ke daftar aktif, baik dari daftar maupun detail tugas.",
                "Perbaikan Filter 'Semua': Menampilkan dan menghitung total tugas aktif dan selesai secara akurat.",
                "Badge Prioritas Konsisten: Format badge prioritas (P1 - P4) diseragamkan dengan gaya minimalis Linear.",
            ),
        ),
        ChangelogEntry(
            version = "2.2.0",
            versionCode = 4,
            date = "18 Agustus 2026",
            title = "Kalender Lengkap & Integrasi Agenda",
            highlights = listOf(
                "Tiga Tampilan Kalender: Beralih cepat antara 'Hari Ini', 'Seminggu', dan 'Bulan' (Notion-style Month Grid).",
                "Multi-Dot Indicator: Indikator titik warna pada tanggal kalender bulanan untuk kuliah, tugas, dan kegiatan akademik.",
                "Filter Kategori Interaktif: Saring tampilan kalender berdasarkan Kuliah, Tugas, atau Kalender Akademik.",
            ),
        ),
        ChangelogEntry(
            version = "2.1.0",
            versionCode = 3,
            date = "18 Agustus 2026",
            title = "Redesain Total Linear & Dark Mode",
            highlights = listOf(
                "Desain Baru Dark Mode: Tampilan modern bergaya Linear dengan elevated surfaces, tipografi Plus Jakarta Sans, dan warna tema adaptif.",
                "Kartu Tugas Todoist: Urutan prioritas cerdas (P1 Urgent hingga P4 Normal).",
                "Pintasan HEBAT e-Learning: Tombol akses cepat ke modul tugas e-learning UNAIR.",
                "Info Presensi Akademik: Pantau persentase kehadiran kuliah per mata kuliah secara real-time.",
            ),
        ),
        ChangelogEntry(
            version = "2.0.0",
            versionCode = 2,
            date = "10 Agustus 2026",
            title = "Rilis Perdana ForFH",
            highlights = listOf(
                "Sinkronisasi Otomatis: Impor jadwal kuliah, tugas, dan kalender akademik dari Cybercampus FH UNAIR.",
                "Alarm & Notifikasi Cerdas: Pengingat otomatis sebelum kelas dimulai di latar belakang.",
            ),
        ),
    )

    fun loadAll(context: Context? = null): List<ChangelogEntry> {
        if (context == null) return staticEntries
        return try {
            val jsonString = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
            json.decodeFromString<List<ChangelogEntry>>(jsonString)
        } catch (_: Throwable) {
            staticEntries
        }
    }

    fun getLatest(context: Context? = null): ChangelogEntry {
        return loadAll(context).firstOrNull() ?: staticEntries.first()
    }

    fun getForVersion(versionCode: Int, context: Context? = null): ChangelogEntry? {
        return loadAll(context).firstOrNull { it.versionCode == versionCode }
    }
}
