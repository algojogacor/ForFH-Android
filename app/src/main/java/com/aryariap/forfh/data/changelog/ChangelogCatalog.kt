package com.aryariap.forfh.data.changelog

import android.content.Context
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
            version = "2.5.0",
            versionCode = 7,
            date = "19 Agustus 2026",
            title = "Pembaruan Layar Masuk & Perapihan Pengaturan",
            highlights = listOf(
                "Desain Baru Layar Masuk: Tampilan modern dengan logo resmi ForFH, kartu login terangkat, dan tombol intip password.",
                "Perbaikan Tampilan Pengaturan: Perapihan status pembaruan dan tombol cek update agar tampil rapi dan nyaman dibaca.",
                "Optimalisasi Komponen Tombol: Fleksibilitas tata letak tombol di seluruh layar aplikasi.",
            ),
        ),
        ChangelogEntry(
            version = "2.4.0",
            versionCode = 6,
            date = "19 Agustus 2026",
            title = "Pratinjau Pembaruan Online & Halaman Unduh Resmi",
            highlights = listOf(
                "Pratinjau Catatan Rilis Online: Intip langsung daftar poin perubahan versi baru dari GitHub Releases di layar Pengaturan sebelum memutuskan update.",
                "Halaman Unduh Resmi (/unduh): Portal unduhan APK dengan tampilan Paper & Ink yang selaras dengan web ForFH, link unduh langsung, dan panduan instalasi.",
                "Perbaikan Distribusi APK: Penataan aset file instalasi rilis GitHub untuk memastikan tautan unduhan langsung aktif tanpa kendala 404.",
            ),
        ),
        ChangelogEntry(
            version = "2.3.0",
            versionCode = 5,
            date = "19 Agustus 2026",
            title = "Polesan Widget, Ikon Baru & Fleksibilitas Tugas",
            highlights = listOf(
                "Ikon Baru Minimalis: Logo timbangan hukum geometris abstrak yang elegan dengan latar obsidian black.",
                "Pembaruan Home Widget: Nama mata kuliah kini tampil jelas sebagai judul utama dan format lokasi ruangan diringkas cerdas (contoh: 'R. LG02 B') agar tidak terpotong.",
                "Fitur Pembatalan Tugas (Uncheck): Tugas yang selesai kini bisa dibatalkan ceklisnya untuk dikembalikan ke daftar aktif, baik dari daftar maupun detail tugas.",
                "Perbaikan Filter 'Semua': Menampilkan dan menghitung total tugas aktif dan selesai secara akurat.",
                "Badge Prioritas Konsisten: Format badge prioritas (P1 - P4) diseragamkan dengan gaya minimalis Linear.",
                "Catatan Perubahan & Cek Update: Riwayat pembaruan lengkap in-app, info versi dinamis, dialog 'Ada yang baru!', dan deteksi rilis baru.",
            ),
        ),
        ChangelogEntry(
            version = "2.2.0",
            versionCode = 4,
            date = "18 Agustus 2026",
            title = "Kalender Lengkap & Integrasi Agenda",
            highlights = listOf(
                "Tiga Tampilan Kalender: Beralih cepat antara tab 'Hari Ini', 'Seminggu', dan 'Bulan' (Notion-style Month Grid).",
                "Multi-Dot Indicator: Indikator titik warna pada tanggal kalender bulanan untuk kuliah, tugas, dan kegiatan akademik.",
                "Filter Kategori Interaktif: Saring tampilan kalender berdasarkan Kuliah, Tugas, atau Kalender Akademik.",
                "Badge Kalender Akademik Cerdas: Format tanggal ramah baca untuk rentang jadwal akademik semester.",
            ),
        ),
        ChangelogEntry(
            version = "2.1.0",
            versionCode = 4,
            date = "18 Agustus 2026",
            title = "Redesain Total Linear & Dark Mode",
            highlights = listOf(
                "Desain Baru Dark Mode: Tampilan modern bergaya Linear dengan elevated surfaces, tipografi Plus Jakarta Sans, dan warna tema adaptif.",
                "Kartu Tugas Todoist: Urutan prioritas cerdas (P1 Urgent hingga P4 Normal) dengan indikator visual yang jelas.",
                "Pintasan HEBAT e-Learning: Tombol akses cepat membuka halaman mata kuliah dan modul tugas langsung di browser e-learning UNAIR.",
                "Pembersih Format Tugas: Teks instruksi HTML dari Cybercampus otomatis dibersihkan dan link otomatis dapat diklik.",
                "Info Presensi Akademik: Pantau persentase kehadiran kuliah per mata kuliah secara real-time.",
            ),
        ),
        ChangelogEntry(
            version = "2.0.0",
            versionCode = 4,
            date = "18 Agustus 2026",
            title = "Evolusi Desain Konstitusi Bold",
            highlights = listOf(
                "Rombak Desain Fase 1: Eksplorasi gaya visual bertema hukum dengan tipografi tegas dan palet warna khas FH UNAIR.",
                "Peningkatan Komponen UI: Penyesuaian tata letak kartu jadwal dan tugas agar lebih proporsional di layar ponsel.",
            ),
        ),
        ChangelogEntry(
            version = "1.2.0",
            versionCode = 3,
            date = "17 Agustus 2026",
            title = "Notifikasi Ringkasan Malam & Widget Tugas",
            highlights = listOf(
                "Notifikasi Ringkasan Besok: Notifikasi harian setiap pukul 20:00 WIB yang merangkum jadwal kuliah dan deadline tugas esok hari.",
                "Widget Home Screen Tugas: Widget baru untuk memantau 3 tugas terdekat langsung dari layar utama HP.",
                "Deep Linking Widget: Mengetuk widget langsung membuka tab yang relevan di dalam aplikasi (Jadwal atau Tugas).",
                "Keamanan Sesi: Pencegahan sinkronisasi data saat akun telah keluar.",
            ),
        ),
        ChangelogEntry(
            version = "1.1.0",
            versionCode = 2,
            date = "17 Agustus 2026",
            title = "Widget Jadwal, Notifikasi Deadline & Info Kampus",
            highlights = listOf(
                "Widget Home Screen Jadwal: Kartu widget responsif yang menampilkan mata kuliah berikutnya dan alarm aktif.",
                "Kartu 'Berikutnya' (NextUp): Penghitung waktu mundur (countdown) realtime dan tombol cepat matikan alarm hari ini.",
                "Notifikasi Deadline H-1: Pengingat otomatis satu hari sebelum tenggat waktu pengumpulan tugas.",
                "Layar Info Kampus & Presensi: Pantau kehadiran kuliah, status akademik, dan informasi registrasi semester.",
                "Status Sinkronisasi Tugas: Indikator status pengiriman tugas (Pending, Synced, Failed) yang responsif dan optimistik.",
                "Log Diagnostik In-App: Halaman log diagnostik untuk memeriksa aktivitas alarm, notifikasi, dan jaringan.",
            ),
        ),
        ChangelogEntry(
            version = "1.0.0",
            versionCode = 1,
            date = "16 Agustus 2026",
            title = "Rilis Perdana ForFH Android",
            highlights = listOf(
                "Sinkronisasi Jadwal & Tugas: Impor otomatis jadwal kuliah dan tugas mahasiswa dari Cybercampus FH UNAIR.",
                "Fullscreen Alarm Cerdas: Alarm layar penuh sebelum kuliah pertama dimulai dengan nada alarm kustom.",
                "Kustomisasi Alarm per Hari: Atur menit pengingat alarm berbeda untuk tiap hari kuliah atau matikan khusus hari ini.",
                "Penyimpanan Aman Terenkripsi: Data kredensial dan sesi login disimpan menggunakan enkripsi AES-GCM Android Keystore.",
                "Mode Offline: Jadwal dan tugas tetap dapat diakses meski tanpa sambungan internet.",
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

    fun getForVersionName(version: String, context: Context? = null): ChangelogEntry? {
        val clean = version.removePrefix("v").trim()
        return loadAll(context).firstOrNull { it.version == clean }
    }
}
