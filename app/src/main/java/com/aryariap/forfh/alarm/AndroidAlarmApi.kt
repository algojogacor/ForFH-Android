package com.aryariap.forfh.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class AndroidAlarmApi(private val context: Context) : AlarmApi {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExact(): Boolean = am.canScheduleExactAlarms()

    override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(requestCode, extras))
    }

    override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>) {
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, windowLengthMillis, pendingIntent(requestCode, extras))
    }

    override fun cancel(requestCode: Int) {
        am.cancel(pendingIntent(requestCode, emptyMap()))
    }

    private fun pendingIntent(requestCode: Int, extras: Map<String, String>): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        extras.forEach { (k, v) -> intent.putExtra(k, v) }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
