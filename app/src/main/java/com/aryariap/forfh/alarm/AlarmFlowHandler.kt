package com.aryariap.forfh.alarm

import android.content.Context
import android.content.Intent
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.sync.AlarmRescheduler
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmFlowHandler(
    private val context: Context,
    private val database: AppDatabase,
    private val prefs: Preferences,
    private val sessionManager: SessionManager,
    private val rescheduler: AlarmRescheduler,
    private val notifications: ForfhNotifications,
    private val planner: AlarmPlanner,
    private val scope: CoroutineScope,
) {
    private val alarmsDao get() = database.scheduledAlarmsDao()
    private val schedulesDao get() = database.schedulesDao()
    private val tasksDao get() = database.tasksDao()
    private val zone = ZoneId.of("Asia/Jakarta")

    /** CLASS_ALARM: guard berlapis → tampil (FSI/heads-up). Row dibiarkan utk snooze; basi dihapus. */
    suspend fun handleClassAlarm(intent: Intent) {
        val scheduleId = intent.getStringExtra("scheduleId") ?: return
        val offsetMinutes = intent.getIntExtra("offsetMinutes", -1)
        val occurrenceDate = intent.getStringExtra("occurrenceDate") ?: return
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (offsetMinutes < 0 || trigger < 0) return
        val identity = AlarmPlanner.classIdentity(
            scheduleId, offsetMinutes, LocalDate.parse(occurrenceDate),
        )
        val result = ReceiverGuard.evaluate(
            GuardInput(
                isLoggedIn = sessionManager.isLoggedIn(),
                schedule = schedulesDao.getByIdOnce(scheduleId),
                row = alarmsDao.getByIdOnce(identity),
                extrasTriggerAtMillis = trigger,
                nowEpochMillis = System.currentTimeMillis(),
                hasNotificationPermission = notifications.hasPermission(),
            ),
        )
        when (result) {
            is GuardResult.Show -> {
                notifications.showClassAlarm(
                    result.schedule, result.row,
                    snoozeAvailable = SnoozeCounter.canSnooze(result.row.snoozeCount),
                )
            }
            GuardResult.SkipSilent -> Unit // tidak menampilkan apa pun, tidak crash
            GuardResult.SkipCancel -> {
                rescheduler.cancelAlarm(identity)
            }
        }
    }

    /** TASK_REMINDER: one-shot → query Room → tampil → hapus row hari ini → schedule besok. */
    suspend fun handleTaskReminder(intent: Intent) {
        val slotHour = intent.getIntExtra("slotHour", -1)
        val occurrenceDate = intent.getStringExtra("occurrenceDate") ?: return
        val trigger = intent.getLongExtra("triggerAtMillis", -1L)
        if (slotHour !in TASK_SLOTS || trigger < 0) return
        val identity = AlarmPlanner.taskIdentity(slotHour, LocalDate.parse(occurrenceDate))
        val row = alarmsDao.getByIdOnce(identity) ?: return
        if (row.triggerAtMillis != trigger) return
        if (!sessionManager.isLoggedIn()) { // defense-in-depth pasca-logout
            rescheduler.cancelAlarm(identity)
            return
        }
        val tasks = tasksDao.getActiveByDeadline()
        val text = TaskReminderText.build(tasks, slotHour)
        if (text != null && notifications.hasPermission()) {
            notifications.showTaskReminder(text, slotHour, occurrenceDate)
        }
        // one-shot: selesai tampil → row besok (spec §8.7)
        rescheduler.replaceTaskSlotRow(slotHour, ZonedDateTime.now(zone))
    }

    /** Snooze dari FSI activity atau aksi notif: +3 menit, count++, update Room, reschedule (RTC_WAKEUP). */
    suspend fun snooze(identity: String): Boolean {
        val row = alarmsDao.getByIdOnce(identity) ?: return false
        if (!SnoozeCounter.canSnooze(row.snoozeCount)) return false
        val updated = row.copy(
            triggerAtMillis = SnoozeCounter.nextTrigger(row.triggerAtMillis),
            snoozeCount = SnoozeCounter.nextCount(row.snoozeCount),
        )
        alarmsDao.upsert(updated)
        rescheduler.scheduleRow(updated)
        return true
    }

    companion object {
        val TASK_SLOTS = listOf(9, 15, 20)
    }
}
