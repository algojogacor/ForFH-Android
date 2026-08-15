package com.aryariap.forfh.alarm

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class ClassOccurrence(
    val identity: String,
    val occurrenceDate: LocalDate,
    val startDateTime: ZonedDateTime,
    val triggerAtMillis: Long,
)

/** Matematika next occurrence — murni, tanpa Android, bisa unit-test. Semua perhitungan WIB eksplisit. */
class AlarmPlanner(private val zone: ZoneId = ZoneId.of("Asia/Jakarta")) {

    companion object {
        const val CLASS_PREFIX = "class"
        const val TASK_PREFIX = "task"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun classIdentity(scheduleId: String, offsetMinutes: Int, occurrenceDate: LocalDate): String =
            "$CLASS_PREFIX|$scheduleId|$offsetMinutes|${occurrenceDate.format(DATE_FMT)}"

        fun taskIdentity(slotHour: Int, date: LocalDate): String =
            "$TASK_PREFIX|${slotHour.toString().padStart(2, '0')}|${date.format(DATE_FMT)}"
    }

    /**
     * Next occurrence kuliah: LocalDate + DayOfWeek(dayOfWeek) + LocalTime(startTime) → ZonedDateTime WIB.
     * Jika trigger (start − offset) <= now → lompat ke minggu berikutnya (spec §8.2).
     */
    fun nextClassOccurrence(
        scheduleId: String,
        dayOfWeek: Int,      // 0=Sunday .. 6=Saturday (konvensi API ForFH)
        startTime: String,   // "HH:MM"
        offsetMinutes: Int,
        now: ZonedDateTime,
    ): ClassOccurrence {
        require(dayOfWeek in 0..6) { "dayOfWeek harus 0..6" }
        val start = LocalTime.parse(startTime)
        val javaDay = DayOfWeek.of((dayOfWeek + 6) % 7 + 1)
        var date = now.toLocalDate().plusDays(((javaDay.value - now.dayOfWeek.value + 7) % 7).toLong())
        var trigger = date.atTime(start).atZone(zone).toInstant().toEpochMilli() - offsetMinutes * 60_000L
        if (trigger <= now.toInstant().toEpochMilli()) {
            date = date.plusWeeks(1)
            trigger = date.atTime(start).atZone(zone).toInstant().toEpochMilli() - offsetMinutes * 60_000L
        }
        return ClassOccurrence(
            identity = classIdentity(scheduleId, offsetMinutes, date),
            occurrenceDate = date,
            startDateTime = date.atTime(start).atZone(zone),
            triggerAtMillis = trigger,
        )
    }

    /** Slot tugas one-shot: hari ini bila trigger masih future, kalau tidak besok. */
    fun nextTaskSlot(slotHour: Int, now: ZonedDateTime): Pair<LocalDate, Long> {
        var date = now.toLocalDate()
        var trigger = date.atTime(slotHour, 0).atZone(zone).toInstant().toEpochMilli()
        if (trigger <= now.toInstant().toEpochMilli()) {
            date = date.plusDays(1)
            trigger = date.atTime(slotHour, 0).atZone(zone).toInstant().toEpochMilli()
        }
        return date to trigger
    }

    /** Rekonstruksi waktu mulai dari Room + occurrenceDate (WIB) — dipakai guard receiver. */
    fun startDateTimeFor(occurrenceDate: String, startTime: String): ZonedDateTime =
        LocalDate.parse(occurrenceDate, DATE_FMT).atTime(LocalTime.parse(startTime)).atZone(zone)
}
