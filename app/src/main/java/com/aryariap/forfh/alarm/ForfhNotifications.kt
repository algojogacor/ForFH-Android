package com.aryariap.forfh.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aryariap.forfh.R
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity

/**
 * Semua tampilan notifikasi. App tidak pernah bergantung pada FSI:
 * dapatUseFullScreenIntent == false → notif biasa heads-up (HIGH + sound + vibration).
 * Sound/vibration tetap subject ke setelan user; app tidak pernah mem-bypass DND (§8.5).
 */
class ForfhNotifications(private val context: Context) {

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CLASS,
                    context.getString(R.string.channel_alarm_kuliah),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alarm bangun kuliah"
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                    enableVibration(true)
                    // CATEGORY_ALARM dibawa notifikasi (builder), bukan channel — setCategory channel API tersembunyi
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TASK,
                    context.getString(R.string.channel_reminder_tugas),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Rekap tugas harian"
                },
            )
        }
    }

    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 || NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canUseFullScreenIntent(): Boolean =
        Build.VERSION.SDK_INT < 34 || nm.canUseFullScreenIntent()

    /** Alarm kuliah: FSI bila tersedia, kalau tidak heads-up. SnoozeAction sebagai aksi notif bila tersedia. */
    fun showClassAlarm(schedule: ScheduleEntity, row: ScheduledAlarmEntity, snoozeAvailable: Boolean) {
        ensureChannels()
        val requestCode = StableHash.of(row.id)
        val fsiIntent = Intent(context, FullScreenAlarmActivity::class.java)
            .putExtra("identity", row.id)
            .putExtra("scheduleId", row.scheduleId ?: "")
            .putExtra("offsetMinutes", row.offsetMinutes)
            .putExtra("occurrenceDate", row.occurrenceDate)
            .putExtra("triggerAtMillis", row.triggerAtMillis)
        val fsiPi = PendingIntent.getActivity(
            context, requestCode, fsiIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_CLASS)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(ClassAlarmText.title(schedule))
            .setContentText(ClassAlarmText.body(schedule))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(fsiPi)
            .setDeleteIntent(null)

        if (canUseFullScreenIntent()) {
            builder.setFullScreenIntent(fsiPi, true)
        }
        if (snoozeAvailable) {
            val snoozePi = PendingIntent.getBroadcast(
                context, requestCode + 1,
                Intent(context, AlarmReceiver::class.java)
                    .setAction(AlarmReceiver.ACTION_SNOOZE)
                    .putExtra("identity", row.id),
                PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "Tidur lagi 3 menit", snoozePi)
        }

        NotificationManagerCompat.from(context).notify(StableHash.of(row.id), builder.build())
    }

    /** Reminder tugas: tap → halaman Tugas (MainActivity extra open_tasks). */
    fun showTaskReminder(text: String, slotHour: Int, date: String) {
        ensureChannels()
        val intent = Intent(context, com.aryariap.forfh.MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open_tasks", true)
        val pi = PendingIntent.getActivity(
            context, StableHash.of("task|$slotHour|$date"), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Tugas")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(StableHash.of("task|$slotHour|$date"), notif)
    }

    companion object {
        const val CHANNEL_CLASS = "alarm_kuliah"
        const val CHANNEL_TASK = "reminder_tugas"
    }
}
