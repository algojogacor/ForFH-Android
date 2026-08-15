package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiverGuardTest {

    private val zone = java.time.ZoneId.of("Asia/Jakarta")

    private fun schedule(enabled: Boolean = true, startTime: String = "08:00") = ScheduleEntity(
        id = "s1", courseId = "c1", courseName = "Hukum", courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = 1,
        startTime = startTime, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
    )

    // Senin 2026-08-17 08:00 WIB
    private val startEpoch = java.time.ZonedDateTime.of(2026, 8, 17, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
    private val triggerEpoch = startEpoch - 120 * 60_000L

    private fun row(trigger: Long = triggerEpoch, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = "class|s1|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s1",
        offsetMinutes = 120, occurrenceDate = "2026-08-17", triggerAtMillis = trigger, snoozeCount = snoozeCount,
    )

    private fun input(
        loggedIn: Boolean = true, sched: ScheduleEntity? = schedule(), r: ScheduledAlarmEntity? = row(),
        extraTrigger: Long = triggerEpoch, now: Long = triggerEpoch - 60_000L, notifOk: Boolean = true,
    ) = GuardInput(
        isLoggedIn = loggedIn, schedule = sched, row = r,
        extrasTriggerAtMillis = extraTrigger, nowEpochMillis = now, hasNotificationPermission = notifOk,
    )

    @Test
    fun `guard lengkap lulus - Show dengan startDateTime`() {
        val result = ReceiverGuard.evaluate(input())
        assertEquals(GuardResult.Show(schedule(), startEpoch, row()), result)
    }

    @Test
    fun `tidak login - SkipCancel (cancel alarm ini)`() {
        assertEquals(GuardResult.SkipCancel, ReceiverGuard.evaluate(input(loggedIn = false)))
    }

    @Test
    fun `jadwal hilang atau dinonaktifkan - skip silent`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(sched = null)))
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(sched = schedule(enabled = false))))
    }

    @Test
    fun `row identity hilang - skip silent (jadwal diubah setelah alarm terpasang)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(r = null)))
    }

    @Test
    fun `triggerAtMillis tidak cocok dengan extras - skip silent (stale)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(extraTrigger = triggerEpoch + 180_000L)))
    }

    @Test
    fun `now melampaui startDateTime - skip silent (alarm basi keluar dari Doze)`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(now = startEpoch + 1L)))
    }

    @Test
    fun `POST_NOTIFICATIONS ditolak - silent, tidak crash`() {
        assertEquals(GuardResult.SkipSilent, ReceiverGuard.evaluate(input(notifOk = false)))
    }
}
