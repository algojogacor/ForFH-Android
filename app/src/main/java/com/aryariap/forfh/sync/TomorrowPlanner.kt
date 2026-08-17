package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Murni: notifikasi ringkasan "Besok" (malam sebelum hari kuliah).
 * Row one-shot kind DAY_PREVIEW, trigger HARI INI jam 20:00 WIB -- pola TaskDeadlinePlanner.
 * skipDates/mute TIDAK menyentuh row ini (aturan sama dengan TASK_DEADLINE).
 *
 * occurrenceDate menyimpan tanggal BESOK (yang direview), bukan hari ini --
 * handler memakainya untuk query jadwal besok. Ini beda subtle dari TASK_DEADLINE
 * (yang menyimpan deadlineDay); didokumentasikan di sini.
 */
class TomorrowPlanner(private val zone: ZoneId = ZoneId.of("Asia/Jakarta")) {

    companion object {
        const val PREVIEW_HOUR = 20
        private const val PREFIX = "tmrw"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun dayPreviewIdentity(date: LocalDate): String = "$PREFIX|${date.format(DATE_FMT)}"
    }

    /**
     * Return row HANYA jika besok punya >=1 jadwal enabled dengan dayOfWeek cocok.
     * Trigger: now.toLocalDate().atTime(20, 0).atZone(zone) -- hari INI jam 20:00.
     * R19: trigger masa lalu (now >= 20:00) -> null.
     * skipDates/mute TIDAK menyentuh DAY_PREVIEW.
     *
     * Konvensi ScheduleEntity.dayOfWeek: 0=Minggu..6=Sabtu (sama dengan java.time DayOfWeek % 7).
     */
    fun computeDayPreview(schedules: List<ScheduleEntity>, now: ZonedDateTime): ScheduledAlarmEntity? {
        val tomorrow = now.toLocalDate().plusDays(1)
        // ScheduleEntity.dayOfWeek: 0=Minggu..6=Sabtu; java DayOfWeek: 1=Sen..7=Min -> %7
        val tomorrowDow = tomorrow.dayOfWeek.value % 7
        val hasClass = schedules.any { it.enabled && it.dayOfWeek == tomorrowDow }
        if (!hasClass) return null

        val trigger = now.toLocalDate().atTime(PREVIEW_HOUR, 0).atZone(zone)
        if (trigger.toInstant().toEpochMilli() <= now.toInstant().toEpochMilli()) return null // R19

        return ScheduledAlarmEntity(
            id = dayPreviewIdentity(tomorrow),
            kind = "DAY_PREVIEW",
            scheduleId = null,
            offsetMinutes = 0,
            occurrenceDate = tomorrow.format(DATE_FMT), // tanggal BESOK (direview), bukan trigger
            triggerAtMillis = trigger.toInstant().toEpochMilli(),
            snoozeCount = 0,
        )
    }
}
