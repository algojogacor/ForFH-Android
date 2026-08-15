package com.aryariap.forfh.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Rebuild seluruh alarm dari Room setelah reboot / update APK (§8.9). Non-directBootAware. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (RecoveryPlan.modeFor(intent.action)) {
            RecoveryPlan.Mode.RECONCILE -> SyncWorker.enqueueReconcile(context)
            null -> Unit
        }
    }
}
