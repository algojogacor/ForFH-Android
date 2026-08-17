package com.aryariap.forfh.alarm

import android.content.Context
import android.content.Intent
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.debug.AppLog
import com.aryariap.forfh.sync.AlarmRescheduler
import com.aryariap.forfh.sync.TaskDeadlinePlanner
import com.aryariap.forfh.sync.TomorrowPlanner
import com.aryariap.forfh.sync.TomorrowSummaryText
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
        )
        if (extras == null) {
            AppLog.warn(TAG, "class alarm extras tak valid: scheduleId=${intent.getStringExtra("scheduleId")} trigger=${intent.getStringExtra("triggerAtMillis")}")
            return
        }
        val identity = AlarmPlanner.classIdentity(
            extras.scheduleId, extras.offsetMinutes, LocalDate.parse(extras.occurrenceDate),
        )
        // Faktur guard diangkat ke lokal: dipakai log (alasan skip = info debug paling berharga).
        val isLoggedIn = sessionManager.isLoggedIn()
        val schedule = schedulesDao.getByIdOnce(extras.scheduleId)
        val row = alarmsDao.getByIdOnce(identity)
        val hasNotifPermission = notifications.hasPermission()
        val result = ReceiverGuard.evaluate(
            GuardInput(
                isLoggedIn = isLoggedIn,
                schedule = schedule,
                row = row,
                extrasTriggerAtMillis = extras.triggerAtMillis,
                nowEpochMillis = System.currentTimeMillis(),
                hasNotificationPermission = hasNotifPermission,
            ),
        )
        when (result) {
            is GuardResult.Show -> {
                AppLog.info(TAG, "class alarm show id=$identity sched=${result.schedule.courseCode} date=${extras.occurrenceDate}")
                notifications.showClassAlarm(
                    result.schedule, result.row,
                    snoozeAvailable = SnoozeCounter.canSnooze(result.row.snoozeCount),
                )
            }
            GuardResult.SkipSilent -> AppLog.warn(
                TAG,
                "class alarm skip-silent id=$identity loggedIn=$isLoggedIn sched=${schedule != null} " +
                    "schedOn=${schedule?.enabled} row=${row != null} notif=$hasNotifPermission",
            )
            GuardResult.SkipCancel -> {
                AppLog.warn(TAG, "class alarm skip-cancel id=$identity (logout)")
                rescheduler.cancelAlarm(identity)
            }
        }
    }

    /** TASK_REMINDER: one-shot → query Room → tampil → hapus row hari ini → schedule besok. */
    suspend fun handleTaskReminder(intent: Intent) {
        val occurrenceDate = intent.getStringExtra("occurrenceDate")
        if (occurrenceDate == null) {
            AppLog.warn(TAG, "task reminder extras tak valid: occurrenceDate hilang")
            return
        }
        val trigger = intent.getStringExtra("triggerAtMillis")?.toLongOrNull()
        if (trigger == null) {
            AppLog.warn(TAG, "task reminder extras tak valid: trigger=${intent.getStringExtra("triggerAtMillis")}")
            return
        }
        // slotHour tidak ada di extras (T6) → turunkan deterministik dari trigger + date (AlarmFlowExtras)
        val slotHour = AlarmFlowExtras.resolveTaskSlot(occurrenceDate, trigger, zone)
        if (slotHour == null) {
            AppLog.warn(TAG, "task reminder skip: slot tak cocok date=$occurrenceDate trigger=$trigger")
            return
        }
        val identity = AlarmPlanner.taskIdentity(slotHour, LocalDate.parse(occurrenceDate))
        val row = alarmsDao.getByIdOnce(identity)
        if (row == null) {
            AppLog.warn(TAG, "task reminder skip: row tak ada id=$identity")
            return
        }
        if (row.triggerAtMillis != trigger) {
            AppLog.warn(TAG, "task reminder skip: trigger basi id=$identity row=${row.triggerAtMillis} extras=$trigger")
            return
        }
        if (!sessionManager.isLoggedIn()) { // defense-in-depth pasca-logout
            AppLog.warn(TAG, "task reminder skip-cancel id=$identity (logout)")
            rescheduler.cancelAlarm(identity)
            return
        }
        val tasks = tasksDao.getActiveByDeadline()
        val text = TaskReminderText.build(tasks, slotHour)
        if (text != null && notifications.hasPermission()) {
            notifications.showTaskReminder(text, slotHour, occurrenceDate)
            AppLog.info(TAG, "task reminder show slot=$slotHour date=$occurrenceDate")
        } else {
            AppLog.warn(TAG, "task reminder skip-silent slot=$slotHour text=${text != null} notif=${notifications.hasPermission()}")
        }
        // one-shot: selesai tampil → row besok (spec §8.7)
        rescheduler.replaceTaskSlotRow(slotHour, ZonedDateTime.now(zone))
    }

    /**
     * TASK_DEADLINE: one-shot per tugas → guard berlapis di DeadlineDecision (murni, teruji —
     * pola TASK_REMINDER, tidak lewat ReceiverGuard yang khusus jalur CLASS_ALARM):
     * Fire → notif biasa (bukan full-screen) lalu row DIHAPUS; CancelSilently → row dihapus
     * tanpa tampil (logout / tugas hilang / DONE); Ignore → intent stale, tak disentuh.
     * Reconcile membangun ulang row H-1 utk hari berikutnya.
     */
    suspend fun handleTaskDeadline(intent: Intent) {
        val extras = AlarmFlowExtras.parseDeadlineExtras(
            taskId = intent.getStringExtra("taskId"),
            occurrenceDate = intent.getStringExtra("occurrenceDate"),
            triggerStr = intent.getStringExtra("triggerAtMillis"),
        )
        if (extras == null) {
            AppLog.warn(TAG, "deadline extras tak valid: taskId=${intent.getStringExtra("taskId")} trigger=${intent.getStringExtra("triggerAtMillis")}")
            return
        }
        val identity = TaskDeadlinePlanner.taskDeadlineIdentity(extras.taskId, LocalDate.parse(extras.occurrenceDate))
        when (val action = DeadlineDecision.decide(
            extras = extras,
            row = alarmsDao.getByIdOnce(identity),
            isLoggedIn = sessionManager.isLoggedIn(),
            task = tasksDao.getByIdOnce(extras.taskId),
            today = LocalDate.now(zone),
        )) {
            is DeadlineAction.Fire -> {
                AppLog.info(TAG, "deadline fire task=${extras.taskId} date=${extras.occurrenceDate}")
                if (notifications.hasPermission()) {
                    notifications.showTaskDeadline(action.text, extras.taskId, extras.occurrenceDate)
                } else {
                    AppLog.warn(TAG, "deadline fire tanpa izin notif task=${extras.taskId}")
                }
                // one-shot per plan: fire → row hilang (bukan di-replace seperti slot tugas)
                rescheduler.cancelAlarm(identity)
                AppLog.info(TAG, "deadline row dihapus id=$identity")
            }
            DeadlineAction.CancelSilently -> {
                AppLog.warn(TAG, "deadline cancel-silent task=${extras.taskId} id=$identity (logout/tugas hilang/DONE/basi)")
                rescheduler.cancelAlarm(identity)
            }
            DeadlineAction.Ignore -> AppLog.warn(TAG, "deadline ignore task=${extras.taskId} (row tak ada/trigger basi)")
        }
    }

    /**
     * DAY_PREVIEW: one-shot ringkasan besok jam 20:00 WIB.
     * Guard berlapis: row ada + trigger cocok + logged-in → Fire (notif + hapus row);
     * logout → CancelSilently (hapus row tanpa tampil); row tak ada / trigger basi → Ignore.
     */
    suspend fun handleDayPreview(intent: Intent) {
        val date = intent.getStringExtra("occurrenceDate")
        val trigger = intent.getStringExtra("triggerAtMillis")?.toLongOrNull()
        if (date == null || trigger == null) {
            AppLog.warn(TAG, "day preview extras tak valid date=$date trigger=$trigger")
            return
        }
        val identity = TomorrowPlanner.dayPreviewIdentity(LocalDate.parse(date))
        val row = alarmsDao.getByIdOnce(identity)
        when (DayPreviewDecision.decide(
            extras = DayPreviewExtras(date, trigger),
            row = row,
            isLoggedIn = sessionManager.isLoggedIn(),
        )) {
            DayPreviewAction.Fire -> {
                val tomorrow = LocalDate.parse(date)
                val zone0 = zone
                val from = tomorrow.atStartOfDay(zone0).toInstant().toEpochMilli()
                val to = tomorrow.plusDays(1).atStartOfDay(zone0).toInstant().toEpochMilli()
                val text = TomorrowSummaryText.build(
                    schedulesDao.getAllOnce(), tasksDao.getDueTasksOnce(from, to), tomorrow, zone0,
                )
                if (text != null && notifications.hasPermission()) {
                    notifications.showDayPreview(text, date)
                    AppLog.info(TAG, "day preview show date=$date")
                } else {
                    AppLog.warn(TAG, "day preview skip-silent date=$date text=${text != null}")
                }
                // one-shot: fire/tidak → row dihapus; reconcile membangun ulang utk malam berikutnya
                rescheduler.cancelAlarm(identity)
            }
            DayPreviewAction.CancelSilently -> {
                AppLog.warn(TAG, "day preview cancel-silent id=$identity (logout)")
                rescheduler.cancelAlarm(identity)
            }
            DayPreviewAction.Ignore -> {
                AppLog.warn(TAG, "day preview ignore id=$identity row=${row != null}")
            }
        }
    }

    /** Snooze dari FSI activity atau aksi notif: +3 menit, count++, update Room, reschedule (RTC_WAKEUP). */
    suspend fun snooze(identity: String): Boolean {
        val row = alarmsDao.getByIdOnce(identity)
        if (row == null) {
            AppLog.warn(TAG, "snooze skip: row tak ada id=$identity")
            return false
        }
        if (!SnoozeCounter.canSnooze(row.snoozeCount)) {
            AppLog.warn(TAG, "snooze skip: quota habis id=$identity count=${row.snoozeCount}")
            return false
        }
        val updated = row.copy(
            triggerAtMillis = SnoozeCounter.nextTrigger(row.triggerAtMillis),
            snoozeCount = SnoozeCounter.nextCount(row.snoozeCount),
        )
        alarmsDao.upsert(updated)
        rescheduler.scheduleRow(updated)
        AppLog.info(TAG, "snooze ok id=$identity next=${updated.triggerAtMillis} count=${updated.snoozeCount}")
        return true
    }

    companion object {
        private const val TAG = "AlarmFlowHandler"

        /** Alias source-compat — pemilik sebenarnya AlarmFlowExtras (dipakai resolveTaskSlot). */
        val TASK_SLOTS = AlarmFlowExtras.TASK_SLOTS
    }
}
