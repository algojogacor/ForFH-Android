package com.aryariap.forfh

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.aryariap.forfh.ui.ForfhAppRoot
import com.aryariap.forfh.ui.theme.ForfhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // edge-to-edge wajib (targetSdk 36)
        val app = application as ForfhApp
        requestNotificationsPermissionIfNeeded(app)
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

    /**
     * MANDATORY (Global Constraints, ruling R6): minta izin runtime POST_NOTIFICATIONS
     * otomatis saat app pertama dibuka (Android 13+, API 33+). Dipanggil sekali per launch
     * sampai diberikan — Android hanya menampilkan dialog sekali. Hasilnya tidak mengubah
     * perilaku app: izin ditolak → receiver tetap jalan tapi silent (guard, spec §10) dan
     * Pengaturan menyediakan tombol buka setelan; diberi → notifikasi tampil normal.
     */
    private fun requestNotificationsPermissionIfNeeded(app: ForfhApp) {
        if (Build.VERSION.SDK_INT >= 33 && !app.container.notifications.hasPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // tap notif kedua saat app sudah hidup (launchMode CLEAR_TOP) → extra open_tasks fresh
        recreate()
    }

    private companion object {
        const val REQ_POST_NOTIFICATIONS = 41
    }
}
