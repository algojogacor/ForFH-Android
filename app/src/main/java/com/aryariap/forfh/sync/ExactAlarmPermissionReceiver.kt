package com.aryariap.forfh.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryariap.forfh.ForfhApp
import kotlinx.coroutines.launch

/**
 * ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (foreground broadcast) —
 * begitu access dikembalikan, rescheduleAll(): semua alarm kembali exact,
 * sesi snooze aktif dipertahankan (spec §8.3).
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ForfhApp
        app.container.applicationScope.launch { app.container.rescheduler.rescheduleAll() }
    }
}
