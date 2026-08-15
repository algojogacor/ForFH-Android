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
        val extras = AlarmFlowExtras.parseClassExtras(
            scheduleId = intent.getStringExtra("scheduleId"),
            offsetStr = intent.getStringExtra("offsetMinutes"),
            occurrenceDate = intent.getStringExtra("occurrenceDate"),
            triggerStr = intent.getStringExtra("triggerAtMillis"),
        ) ?: return
        val identity = AlarmPlanner.classIdentity(
            extras.scheduleId, extras.offsetMinutes, LocalDate.parse(extras.occurrenceDate),
        )
        val result = ReceiverGuard.evaluate(
            GuardInput(
                isLoggedIn = sessionManager.isLoggedIn(),
                schedule = schedulesDao.getByIdOnce(extras.scheduleId),
                row = alarmsDao.getByIdOnce(identity),
                extrasTriggerAtMillis = extras.triggerAtMillis,
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
        val occurrenceDate = intent.getStringExtra("occurrenceDate") ?: return
        val trigger = intent.getStringExtra("triggerAtMillis")?.toLongOrNull() ?: return
        // slotHour tidak ada di extras (T6) → turunkan deterministik dari trigger + date (AlarmFlowExtras)
        val slotHour = AlarmFlowExtras.resolveTaskSlot(occurrenceDate, trigger, zone) ?: return
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
        /** Alias source-compat — pemilik sebenarnya AlarmFlowExtras (dipakai resolveTaskSlot). */
        val TASK_SLOTS = AlarmFlowExtras.TASK_SLOTS
    }
}
