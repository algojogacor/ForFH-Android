package com.aryariap.forfh.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AndroidAlarmApi(private val context: Context) : AlarmApi {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // canScheduleExactAlarms() baru ada sejak API 31 (S) — guard mencegah NoSuchMethodError di API 26-30
    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()

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
        // FLAG_UPDATE_CURRENT: matching PendingIntent mengabaikan extras, jadi tanpa flag ini
        // getBroadcast mengembalikan token lama berisi Intent pemanggilan pertama (extras basi).
        // Wajib saat reschedule/snooze — triggerAtMillis harus segar (kontrak "receiver tidak menebak-nebak").
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
