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

    override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
        cancelLegacy(requestCode)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(requestCode, action, extras))
    }

    override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
        cancelLegacy(requestCode)
        am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, windowLengthMillis, pendingIntent(requestCode, action, extras))
    }

    override fun cancel(requestCode: Int, action: String?) {
        am.cancel(pendingIntent(requestCode, action, emptyMap()))
        cancelLegacy(requestCode)
    }

    /**
     * Migrasi V1: PendingIntent lama dipasang TANPA action (sebelum Task 6), sedangkan sekarang
     * tiap kind memakai action sendiri. PendingIntent matching mencakup action (filterEquals) —
     * token lama tidak akan kena cancel token baru. Sisa token V1 dibersihkan di sini supaya
     * tidak terus berbunyi sebagai broadcast no-op; cancel token yang tidak ada adalah no-op.
     */
    private fun cancelLegacy(requestCode: Int) {
        am.cancel(pendingIntent(requestCode, null, emptyMap()))
    }

    private fun pendingIntent(requestCode: Int, action: String?, extras: Map<String, String>): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        if (action != null) intent.action = action
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
