package com.aryariap.forfh.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aryariap.forfh.R
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.debug.AppLog

/**
 * Semua tampilan notifikasi. App tidak pernah bergantung pada FSI:
 * dapatUseFullScreenIntent == false → notif biasa heads-up (HIGH + sound + vibration).
 * Sound/vibration tetap subject ke setelan user; app tidak pernah mem-bypass DND (§8.5).
 */
class ForfhNotifications(private val context: Context) {

    // Suara alarm: Extreme.mp3 milik user, di-bundle ke res/raw (pilihan user 2026-08-15).
    // Resource URI — selalu ada, tanpa izin baca file, berfungsi di background (FSI & heads-up).
    private val alarmSoundUri: Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.extreme}")

    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            // Migrasi suara alarm → Extreme.mp3 (v2) + audio attributes USAGE_ALARM (v3):
            // sound & audio attributes channel TIDAK bisa diubah setelah create
            // (API 26+ mengunci keduanya saat channel dibuat). Channel lama dari
            // versi sebelumnya harus di-delete lalu di-recreate — sekali, dipicu flag.
            // Tanpa ini, APK baru tetap bunyi suara sistem di channel yang sudah ada.
            val prefs = context.getSharedPreferences("forfh_notif", Context.MODE_PRIVATE)
            if (prefs.getInt("channel_sound_version", 0) < CHANNEL_SOUND_VERSION) {
                nm.deleteNotificationChannel(CHANNEL_CLASS)
                prefs.edit().putInt("channel_sound_version", CHANNEL_SOUND_VERSION).apply()
            }
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CLASS,
                    context.getString(R.string.channel_alarm_kuliah),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alarm bangun kuliah"
                    // USAGE_ALARM → bunyi diputar di stream ALARM, bukan NOTIFICATION.
                    // Tanpa ini dumpsys menunjukkan mAudioAttributes=null → sistem pakai
                    // default USAGE_NOTIFICATION → volume ikut stream notif (~50%),
                    // bukan stream alarm (87%) → bunyi "kecil banget".
                    // setAudioAttributes() sudah tidak ada di compileSdk 37 (android.jar
                    // platform 36/37 hanya punya getAudioAttributes) — jalan satu-satunya:
                    // setSound(uri, attrs) yang menetapkan sound + attributes sekaligus.
                    setSound(
                        alarmSoundUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
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
            .setSound(alarmSoundUri)
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
        AppLog.info(TAG, "notif kuliah posted id=${row.id} channel=$CHANNEL_CLASS fsi=${canUseFullScreenIntent()} snooze=$snoozeAvailable")
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
        AppLog.info(TAG, "notif tugas posted slot=$slotHour date=$date channel=$CHANNEL_TASK")
    }

    /**
     * Deadline tugas H-1: notif biasa (bukan full-screen), tap → halaman Tugas (open_tab=1).
     * R2: pakai channel tugas existing CHANNEL_TASK — tanpa channel baru.
     */
    fun showTaskDeadline(text: String, taskId: String, date: String) {
        ensureChannels()
        val intent = Intent(context, com.aryariap.forfh.MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open_tab", 1)
        val pi = PendingIntent.getActivity(
            context, StableHash.of("taskdl|$taskId|$date"), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Deadline tugas")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(StableHash.of("taskdl|$taskId|$date"), notif)
        AppLog.info(TAG, "notif deadline posted task=$taskId date=$date channel=$CHANNEL_TASK")
    }

    /**
     * DAY_PREVIEW: ringkasan besok jam 20:00 WIB, tap → tab Jadwal (open_tab=0).
     * Satu-shot: row dihapus setelah fire.
     */
    fun showDayPreview(text: String, date: String) {
        ensureChannels()
        val intent = Intent(context, com.aryariap.forfh.MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("open_tab", 0)
        val pi = PendingIntent.getActivity(
            context, StableHash.of("tmrw|$date"), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Besok")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(StableHash.of("tmrw|$date"), notif)
        AppLog.info(TAG, "notif day preview posted date=$date channel=$CHANNEL_TASK")
    }

    companion object {
        private const val TAG = "ForfhNotifications"
        const val CHANNEL_CLASS = "alarm_kuliah"
        const val CHANNEL_TASK = "reminder_tugas"
        // Bump tiap kali sound/vibration/audio-attributes channel berubah — memicu delete+recreate sekali
        private const val CHANNEL_SOUND_VERSION = 3
    }
}
