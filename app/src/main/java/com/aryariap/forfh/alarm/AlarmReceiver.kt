package com.aryariap.forfh.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryariap.forfh.ForfhApp
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext as ForfhApp
        val handler = app.container.alarmFlow
        app.container.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_CLASS_ALARM -> handler.handleClassAlarm(intent)
                    ACTION_TASK_REMINDER -> handler.handleTaskReminder(intent)
                    ACTION_SNOOZE -> handler.snooze(intent.getStringExtra("identity") ?: "")
                    else -> Unit
                }
            } catch (t: Throwable) {
                android.util.Log.e("AlarmReceiver", "handler gagal", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_CLASS_ALARM = "com.aryariap.forfh.action.CLASS_ALARM"
        const val ACTION_TASK_REMINDER = "com.aryariap.forfh.action.TASK_REMINDER"
        const val ACTION_SNOOZE = "com.aryariap.forfh.action.SNOOZE"
    }
}
