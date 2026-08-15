package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.prefs.Preferences
import java.time.ZonedDateTime

class AlarmRescheduler(
    private val planner: AlarmPlanner,
    private val scheduler: AlarmScheduler,
    private val alarmsDao: ScheduledAlarmsDao,
    private val schedulesDao: SchedulesDao,
    private val prefs: Preferences,
) {
    /** Cancel + hapus row identity (dipakai guard SkipCancel & slot tugas selesai). */
    suspend fun cancelAlarm(identity: String) {
        alarmsDao.getByIdOnce(identity)?.let { scheduler.cancel(it) }
        alarmsDao.deleteById(identity)
    }

    /** Pasang satu row (snooze, reschedule) — tidak menyentuh row lain. */
    suspend fun scheduleRow(row: ScheduledAlarmEntity) {
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }

    /** Pola one-shot tugas: hapus row hari ini, pasang row besok (§8.7). */
    suspend fun replaceTaskSlotRow(slotHour: Int, now: ZonedDateTime) {
        val (date, trigger) = planner.nextTaskSlot(slotHour, now)
        val identity = AlarmPlanner.taskIdentity(slotHour, date)
        val row = ScheduledAlarmEntity(
            id = identity,
            kind = "TASK_REMINDER",
            scheduleId = null,
            offsetMinutes = 0,
            occurrenceDate = date.toString(),
            triggerAtMillis = trigger,
            snoozeCount = 0,
        )
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }
}
