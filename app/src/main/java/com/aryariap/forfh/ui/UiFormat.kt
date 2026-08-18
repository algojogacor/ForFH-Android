package com.aryariap.forfh.ui

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiFormat {
    private val localeId = Locale("id", "ID")
    private val deadlineFmt = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", localeId)
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

    /** "HH:mm" (WIB) dari ZonedDateTime. */
    fun timeOf(t: ZonedDateTime): String = t.format(timeFmt)

    /** "HH:mm" WIB dari epoch millis. */
    fun timeOf(epochMillis: Long, zone: ZoneId): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFmt)

    /**
     * Countdown "dalam 2 j 47 m" (kartu "Berikutnya"): durasi now → start, floor ke menit.
     * Lebih dari sehari memakai "X hari" (kelas berikutnya bisa 3-7 hari lagi, mis. Jumat
     * sore → Senin pagi); "0 m" bila ≤ 1 menit atau start sudah lewat (defensif).
     */
    fun countdownTo(now: ZonedDateTime, start: ZonedDateTime): String {
        val totalMinutes = Duration.between(now, start).toMinutes().coerceAtLeast(0)
        val d = totalMinutes / 1440
        val h = (totalMinutes % 1440) / 60
        val m = totalMinutes % 60
        return buildList {
            if (d > 0L) add("$d hari")
            if (h > 0L) add("$h j")
            if (m > 0L || isEmpty()) add("$m m")
        }.joinToString(" ")
    }

    /** "berhasil HH:mm" / "gagal — coba lagi" / "belum pernah" utk label sinkronisasi. */
    fun syncInfo(status: String, lastSyncAt: Long): String = when (status) {
        "ok" -> "berhasil ${Instant.ofEpochMilli(lastSyncAt).atZone(ZoneId.of("Asia/Jakarta")).format(timeFmt)}"
        "error" -> "gagal — coba lagi"
        else -> "belum pernah"
    }

    private val monthYearFmt = DateTimeFormatter.ofPattern("MMMM yyyy", localeId)
    private val fullDateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", localeId)
    private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", localeId)

    fun monthYear(yearMonth: java.time.YearMonth): String = yearMonth.format(monthYearFmt)
    fun fullDateIndonesian(localDate: LocalDate): String = localDate.format(fullDateFmt)
    fun shortDateIndonesian(localDate: LocalDate): String = localDate.format(shortDateFmt)

    private val MONTH_NAMES = arrayOf(
        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val SHORT_MONTH_NAMES = arrayOf(
        "", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Ags", "Sep", "Okt", "Nov", "Des"
    )

    fun parseDateRobust(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()
        return runCatching {
            if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
                LocalDate.parse(clean.take(10))
            } else if (clean.contains("/")) {
                val parts = clean.split("/")
                if (parts.size == 3) {
                    if (parts[2].length == 4) { // dd/MM/yyyy
                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    } else { // yyyy/MM/dd
                        LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    }
                } else null
            } else if (clean.contains("-")) {
                val parts = clean.split("-")
                if (parts.size == 3 && parts[2].length == 4) { // dd-MM-yyyy
                    LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else null
            } else {
                LocalDate.parse(clean)
            }
        }.getOrNull()
    }

    fun parseTimeRobust(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()
        if (clean.contains("T")) {
            val afterT = clean.substringAfter("T")
            if (afterT.length >= 5 && afterT[2] == ':') {
                return afterT.take(5)
            }
        } else if (clean.contains(" ")) {
            val afterSpace = clean.substringAfter(" ")
            if (afterSpace.length >= 5 && afterSpace[2] == ':') {
                return afterSpace.take(5)
            }
        }
        return null
    }

    /**
     * Format rentang kalender akademik ke Bahasa Indonesia yang manusiawi dan elegan.
     * Contoh:
     * - "3 – 19 Agustus 2026 · 08:00 – 16:00 WIB"
     * - "13 Juli – 29 Agustus 2026"
     * - "18 Agustus 2026 · 08:00 WIB"
     */
    fun formatAcademicRange(rawStart: String?, rawEnd: String?): String {
        val sDate = parseDateRobust(rawStart)
        val eDate = parseDateRobust(rawEnd) ?: sDate
        val sTime = parseTimeRobust(rawStart)
        val eTime = parseTimeRobust(rawEnd)

        if (sDate == null) {
            return listOfNotNull(rawStart, rawEnd).filter { it.isNotBlank() }.joinToString(" – ")
        }

        val datePart = when {
            eDate == null || eDate == sDate -> {
                "${sDate.dayOfMonth} ${MONTH_NAMES.getOrElse(sDate.monthValue) { "" }} ${sDate.year}"
            }
            sDate.year == eDate.year && sDate.monthValue == eDate.monthValue -> {
                "${sDate.dayOfMonth} – ${eDate.dayOfMonth} ${MONTH_NAMES.getOrElse(sDate.monthValue) { "" }} ${sDate.year}"
            }
            sDate.year == eDate.year -> {
                "${sDate.dayOfMonth} ${SHORT_MONTH_NAMES.getOrElse(sDate.monthValue) { "" }} – ${eDate.dayOfMonth} ${SHORT_MONTH_NAMES.getOrElse(eDate.monthValue) { "" }} ${sDate.year}"
            }
            else -> {
                "${sDate.dayOfMonth} ${SHORT_MONTH_NAMES.getOrElse(sDate.monthValue) { "" }} ${sDate.year} – ${eDate.dayOfMonth} ${SHORT_MONTH_NAMES.getOrElse(eDate.monthValue) { "" }} ${eDate.year}"
            }
        }

        val hasMeaningfulTime = (sTime != null && sTime != "00:00") || (eTime != null && eTime != "00:00")
        val timePart = if (hasMeaningfulTime) {
            when {
                sTime != null && eTime != null && sTime != eTime -> " · $sTime – $eTime WIB"
                sTime != null -> " · $sTime WIB"
                eTime != null -> " · s.d. $eTime WIB"
                else -> ""
            }
        } else ""

        return datePart + timePart
    }
}
