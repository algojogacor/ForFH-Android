package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity

/** Abstraksi AlarmManager agar bisa di-unit-test dengan fake (tanpa Robolectric). */
interface AlarmApi {
    fun canScheduleExact(): Boolean
    fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>)
    fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>)
    fun cancel(requestCode: Int)
}

class AlarmScheduler(
    private val alarmApi: AlarmApi,
    private val stableHash: (String) -> Int = StableHash::of,
) {
    companion object {
        /** Fallback window: jendela mulai DARI triggerAtMillis (bukan "±10 menit" — istilah itu dilarang, §8.3). */
        const val FALLBACK_WINDOW_MS = 10 * 60 * 1000L
    }

    /** Exact bila tersedia, fallback setWindow bila tidak. RTC_WAKEUP di sisi impl (AndroidAlarmApi). */
    fun schedule(row: ScheduledAlarmEntity) {
        val requestCode = stableHash(row.id)
        val extras = mapOf(
            "scheduleId" to (row.scheduleId ?: ""),
            "offsetMinutes" to row.offsetMinutes.toString(),
            "occurrenceDate" to row.occurrenceDate,
            "triggerAtMillis" to row.triggerAtMillis.toString(),
        )
        if (alarmApi.canScheduleExact()) {
            alarmApi.setExactAndAllowWhileIdle(row.triggerAtMillis, requestCode, extras)
        } else {
            alarmApi.setWindow(row.triggerAtMillis, FALLBACK_WINDOW_MS, requestCode, extras)
        }
    }

    fun cancel(row: ScheduledAlarmEntity) {
        alarmApi.cancel(stableHash(row.id))
    }
}
