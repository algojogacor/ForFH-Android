package com.aryariap.forfh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aryariap.forfh.ui.ForfhAppRoot
import com.aryariap.forfh.ui.theme.ForfhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // edge-to-edge wajib (targetSdk 36)
        val app = application as ForfhApp
        val openTasks = intent.getBooleanExtra("open_tasks", false) // notif tugas (T7)
        // consume flag: recreation berikutnya (rotate, process-death lalu kembali) membawa intent
        // tanpa extra → LaunchedEffect(openTasks) di ForfhAppRoot no-op → tab restore pilihan user.
        intent.removeExtra("open_tasks")
        setContent {
            ForfhTheme {
                ForfhAppRoot(container = app.container, openTasks = openTasks)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // tap notif kedua saat app sudah hidup (launchMode CLEAR_TOP) → extra open_tasks fresh
        recreate()
    }
}
