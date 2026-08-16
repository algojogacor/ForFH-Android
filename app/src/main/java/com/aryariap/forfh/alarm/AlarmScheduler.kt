package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.sync.TaskDeadlinePlanner

/** Abstraksi AlarmManager agar bisa di-unit-test dengan fake (tanpa Robolectric). */
interface AlarmApi {
    fun canScheduleExact(): Boolean

    /**
     * action = Intent action PendingIntent — bagian dari matching (Intent.filterEquals), jadi
     * schedule/cancel/reschedule WAJIB action sama utk satu identity. null → tanpa action.
     */
    fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>)
    fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>)
    fun cancel(requestCode: Int, action: String?)
}

class AlarmScheduler(
    private val alarmApi: AlarmApi,
    private val stableHash: (String) -> Int = StableHash::of,
) {
    companion object {
        /** Fallback window: jendela mulai DARI triggerAtMillis (bukan "±10 menit" — istilah itu dilarang, §8.3). */
        const val FALLBACK_WINDOW_MS = 10 * 60 * 1000L
    }

    /** Action PendingIntent per kind — AlarmReceiver merouting berdasarkan ini (else → no-op). */
    private fun actionFor(kind: String): String? = when (kind) {
        "CLASS_ALARM" -> AlarmReceiver.ACTION_CLASS_ALARM
        "TASK_REMINDER" -> AlarmReceiver.ACTION_TASK_REMINDER
        "TASK_DEADLINE" -> AlarmReceiver.ACTION_TASK_DEADLINE
        else -> null // kind tak dikenal → receiver no-op (else -> Unit)
    }

    /** Exact bila tersedia, fallback setWindow bila tidak. RTC_WAKEUP di sisi impl (AndroidAlarmApi). */
    fun schedule(row: ScheduledAlarmEntity) {
        val requestCode = stableHash(row.id)
        val extras = mutableMapOf(
            "scheduleId" to (row.scheduleId ?: ""),
            "offsetMinutes" to row.offsetMinutes.toString(),
            "occurrenceDate" to row.occurrenceDate,
            "triggerAtMillis" to row.triggerAtMillis.toString(),
        )
        // TASK_DEADLINE: receiver butuh tahu tugas mana (identity taskdl|{taskId}|{date}) —
        // extra taskId hanya untuk kind ini; kind lain tanpa taskId (kontrak extras ketat).
        TaskDeadlinePlanner.taskIdFromIdentity(row.id)?.let { extras["taskId"] = it }
        val action = actionFor(row.kind)
        if (alarmApi.canScheduleExact()) {
            alarmApi.setExactAndAllowWhileIdle(row.triggerAtMillis, requestCode, action, extras)
        } else {
            alarmApi.setWindow(row.triggerAtMillis, FALLBACK_WINDOW_MS, requestCode, action, extras)
        }
    }

    fun cancel(row: ScheduledAlarmEntity) {
        // action HARUS sama dengan schedule: PendingIntent matching via filterEquals mencakup action
        alarmApi.cancel(stableHash(row.id), actionFor(row.kind))
    }
}
