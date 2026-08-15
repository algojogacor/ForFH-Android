package com.aryariap.forfh.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiFormat {
    private val deadlineFmt = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("id", "ID"))
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    /** dueAt epoch ms → tanggal WIB. Null → "Tanpa deadline". */
    fun deadline(dueAtEpochMillis: Long?, zone: ZoneId): String {
        if (dueAtEpochMillis == null) return "Tanpa deadline"
        return Instant.ofEpochMilli(dueAtEpochMillis).atZone(zone).format(deadlineFmt)
    }

    fun range(start: String, end: String): String = "$start–$end"

    fun statusLabel(status: String): String = when (status) {
        "DONE" -> "Selesai"
        "OVERDUE" -> "Terlambat"
        "IN_PROGRESS" -> "Proses"
        "REVISION" -> "Revisi"
        else -> "Belum"
    }

    fun dayName(dayOfWeek: Int): String = when (dayOfWeek) { // 0=Sunday..6=Saturday
        0 -> "Minggu"; 1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"
        4 -> "Kamis"; 5 -> "Jumat"; else -> "Sabtu"
    }

    fun timeText(t: String): String = t.take(5)

    /** "berhasil HH:mm" / "gagal — coba lagi" / "belum pernah" utk label sinkronisasi. */
    fun syncInfo(status: String, lastSyncAt: Long): String = when (status) {
        "ok" -> "berhasil ${Instant.ofEpochMilli(lastSyncAt).atZone(ZoneId.of("Asia/Jakarta")).format(timeFmt)}"
        "error" -> "gagal — coba lagi"
        else -> "belum pernah"
    }
}
