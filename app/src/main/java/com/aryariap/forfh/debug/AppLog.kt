package com.aryariap.forfh.debug

import android.content.Context
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tingkat kepentingan baris log — nama enum langsung jadi label di garis log. */
enum class LogLevel { INFO, WARN, ERROR }

/**
 * Penyimpanan log berbasis file, MURNI (tanpa Android, tanpa logcat) — diuji JUnit4
 * (LogFileTest). Kontrak:
 *
 * - Satu file aktif `forfh.log` di direktori yang diberikan + satu arsip `forfh.log.1`.
 * - Append selalu dalam mode append (FileOutputStream APPEND), tidak pernah rewrite —
 *   aman dari masalah renameTo ala DataStore di Windows, dan setiap operasi menutup
 *   file (tidak ada handle tersisa).
 * - Rotasi sederhana: bila ukuran aktif + baris baru melebihi [maxFileBytes] (default
 *   ~512 KB), aktif dipindah ke arsip (menimpa arsip lama — maksimal satu arsip),
 *   lalu baris ditulis ke file aktif yang baru. Disk total maksimal ~1 MB.
 * - Satu baris log yang lebih panjang dari [maxFileBytes] tetap ditulis utuh (satu
 *   garis besar), tidak pernah dipecah.
 * - Thread-safe: append/read/clear disinkronkan (synchronized) — aman dipanggil dari
 *   receiver alarm, worker sync, dan UI secara bersamaan.
 * - Tidak pernah melempar: semua I/O error ditelan dan dikembalikan sebagai false.
 * - Baris dibersihkan: newline → spasi, tag ≤ 40 karakter, pesan ≤ 500 karakter.
 */
class LogFile(
    private val directory: File,
    private val clock: Clock = Clock.systemUTC(),
    private val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
) {
    val activeFile: File = File(directory, FILE_NAME)
    val archiveFile: File = File(directory, ARCHIVE_NAME)

    /** Tambah satu baris log (format WIB). Mengembalikan false bila I/O gagal — tidak pernah melempar. */
    @Synchronized
    fun append(level: LogLevel, tag: String, message: String): Boolean = try {
        if (!directory.exists() && !directory.mkdirs()) return false
        val line = formatLine(level, tag, message)
        if (activeFile.length() + line.toByteArray(Charsets.UTF_8).size > maxFileBytes) {
            rotate()
        }
        activeFile.appendText(line, Charsets.UTF_8)
        true
    } catch (t: Throwable) {
        false
    }

    /** Baris terakhir (arsip + aktif, urutan kronologis), maksimal [maxLines]. Tidak pernah melempar. */
    @Synchronized
    fun readRecent(maxLines: Int): List<String> = try {
        if (maxLines <= 0) return emptyList()
        val all = mutableListOf<String>()
        if (archiveFile.exists()) all += archiveFile.readLines()
        if (activeFile.exists()) all += activeFile.readLines()
        all.takeLast(maxLines)
    } catch (t: Throwable) {
        emptyList()
    }

    /** Hapus file aktif + arsip. Tidak pernah melempar. */
    @Synchronized
    fun clear() {
        try {
            if (activeFile.exists()) activeFile.delete()
            if (archiveFile.exists()) archiveFile.delete()
        } catch (t: Throwable) {
            // abaikan — clear log tidak pernah boleh crash
        }
    }

    /** Aktif dipindah ke arsip (arsip lama ditimpa), file aktif baru menunggu baris berikutnya. */
    private fun rotate() {
        if (archiveFile.exists()) archiveFile.delete()
        activeFile.renameTo(archiveFile)
    }

    private fun formatLine(level: LogLevel, tag: String, message: String): String {
        val wib = Instant.now(clock).atZone(WIB)
        // Baris TUNGGAL: newline/CR digabung jadi satu spasi, tag dan pesan dipangkas.
        val safeTag = tag.replace(NEWLINE_RUN, " ").take(MAX_TAG_CHARS)
        val safeMessage = message.replace(NEWLINE_RUN, " ").take(MAX_MESSAGE_CHARS)
        return "${wib.format(TIMESTAMP_FMT)} WIB | $level | $safeTag | $safeMessage\n"
    }

    companion object {
        /** Batas panjang pesan per baris (karakter) — diuji di LogFileTest. */
        internal const val MAX_MESSAGE_CHARS = 500
        private const val FILE_NAME = "forfh.log"
        private const val ARCHIVE_NAME = "forfh.log.1"
        private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
        // Numerik murni → tidak terpengaruh lokale (aman dari masalah "Turkish i").
        private val TIMESTAMP_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val NEWLINE_RUN: Regex = Regex("[\\r\\n]+")
        private const val DEFAULT_MAX_FILE_BYTES: Long = 512L * 1024
        private const val MAX_TAG_CHARS = 40
    }
}

/**
 * Facade singleton untuk seluruh app (AlarmReceiver, AlarmFlowHandler, SyncRepository,
 * widget, notifikasi, layar Log). Dipanggil hanya dari proses utama; diinisialisasi di
 * ForfhApp.onCreate (selalu berjalan sebelum receiver/worker/activity mana pun).
 *
 * Semua pemanggilan menelan error di lapisan ini DAN di LogFile — logging TIDAK PERNAH
 * boleh crash, memblok, atau mengubah perilaku alur yang di-instrumentasi.
 * Tanpa init (mis. unit test) semua panggilan no-op.
 */
object AppLog {
    private var store: LogFile? = null

    fun init(context: Context) {
        store = LogFile(File(context.filesDir, "logs"))
    }

    fun info(tag: String, message: String) = write(LogLevel.INFO, tag, message)

    fun warn(tag: String, message: String) = write(LogLevel.WARN, tag, message)

    fun error(tag: String, message: String) = write(LogLevel.ERROR, tag, message)

    private fun write(level: LogLevel, tag: String, message: String) {
        try {
            store?.append(level, tag, message)
        } catch (t: Throwable) {
            // logging tidak pernah boleh crash — abaikan semua kegagalan
        }
    }

    /** Baris terakhir untuk layar Log (arsip + aktif). Kosong bila belum init. */
    fun readRecent(maxLines: Int): List<String> = try {
        store?.readRecent(maxLines).orEmpty()
    } catch (t: Throwable) {
        emptyList()
    }

    /** Hapus seluruh log (tombol "Hapus log"). */
    fun clear() {
        try {
            store?.clear()
        } catch (t: Throwable) {
            // abaikan
        }
    }

    /** File log aktif untuk dibagikan via FileProvider; null bila belum init. */
    fun activeFile(): File? = try {
        store?.activeFile
    } catch (t: Throwable) {
        null
    }
}
