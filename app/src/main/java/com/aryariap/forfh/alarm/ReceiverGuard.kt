package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import java.time.ZoneId

data class GuardInput(
    val isLoggedIn: Boolean,
    val schedule: ScheduleEntity?,
    val row: ScheduledAlarmEntity?,
    val extrasTriggerAtMillis: Long,
    val nowEpochMillis: Long,
    val hasNotificationPermission: Boolean,
)

sealed interface GuardResult {
    data class Show(
        val schedule: ScheduleEntity,
        val startDateTimeEpochMillis: Long,
        val row: ScheduledAlarmEntity,
    ) : GuardResult

    data object SkipSilent : GuardResult
    data object SkipCancel : GuardResult
}

/** Guard berlapis alarm kuliah — salah satu gagal → skip, tidak menampilkan apa pun (§8.4). */
object ReceiverGuard {
    fun evaluate(input: GuardInput, zone: ZoneId = ZoneId.of("Asia/Jakarta")): GuardResult {
        if (!input.isLoggedIn) return GuardResult.SkipCancel   // defense-in-depth pasca-logout
        val schedule = input.schedule ?: return GuardResult.SkipSilent
        if (!schedule.enabled) return GuardResult.SkipSilent
        val row = input.row ?: return GuardResult.SkipSilent   // identity tak ada → stale
        if (row.triggerAtMillis != input.extrasTriggerAtMillis) return GuardResult.SkipSilent
        val start = AlarmPlanner(zone).startDateTimeFor(row.occurrenceDate, schedule.startTime)
        if (input.nowEpochMillis >= start.toInstant().toEpochMilli()) return GuardResult.SkipSilent // alarm basi
        if (!input.hasNotificationPermission) return GuardResult.SkipSilent // silent, tidak crash (§10)
        return GuardResult.Show(schedule, start.toInstant().toEpochMilli(), row)
    }
}
