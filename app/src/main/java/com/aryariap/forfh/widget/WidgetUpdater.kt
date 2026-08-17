package com.aryariap.forfh.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.aryariap.forfh.debug.AppLog

/**
 * Refresh semua instance widget jadwal (Task 4). Dipanggil dari 4 titik update:
 * (1) AlarmRescheduler.execute() setelah selesai, (2) sync sukses (lewat rescheduleAll,
 * lihat SyncWorker), (3) boot app (ForfhApp.onCreate), (4) alarm ditutup/di-snooze
 * (FullScreenAlarmViewModel). Plus updatePeriodMillis 30 mnt di forfh_widget_info.xml.
 *
 * Kegagalan ditelan dan di-log DI SINI: refresh widget tidak pernah boleh menggagalkan
 * alur alarm, sync, atau boot (global constraint Task 4).
 *
 * API note: di Glance 1.1.1 `updateAll` adalah extension `suspend fun GlanceAppWidget
 * .updateAll(context)` (diverifikasi dari AAR); overload `GlanceAppWidgetManager
 * .updateAll(KClass)` baru ada di versi lebih baru.
 */
suspend fun refreshAll(context: Context) {
    try {
        ForfhWidget().updateAll(context)
    } catch (t: Throwable) {
        Log.w(TAG, "Refresh widget gagal (non-fatal)", t)
        AppLog.error(TAG, "refresh widget gagal (non-fatal): ${t.message}")
    }
}

private const val TAG = "WidgetUpdater"
