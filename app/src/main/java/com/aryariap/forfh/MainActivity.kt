package com.aryariap.forfh

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aryariap.forfh.ui.ForfhAppRoot
import com.aryariap.forfh.ui.StartTab
import com.aryariap.forfh.ui.theme.ForfhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // edge-to-edge wajib (targetSdk 36)
        val app = application as ForfhApp
        requestNotificationsPermissionIfNeeded()
        val openTab = StartTab.fromIntent(
            openTasks = intent.getBooleanExtra("open_tasks", false),
            openTab = intent.getIntExtra("open_tab", -1),
        )
        // consume extras: recreation berikutnya (rotate, process-death lalu kembali) membawa intent
        // tanpa extra → LaunchedEffect(startTab) di ForfhAppRoot no-op → tab restore pilihan user.
        intent.removeExtra("open_tasks")
        intent.removeExtra("open_tab")
        setContent {
            ForfhTheme {
                ForfhAppRoot(container = app.container, startTab = openTab)
            }
        }
    }

    /**
     * MANDATORY (Global Constraints, ruling R6): minta izin runtime POST_NOTIFICATIONS
     * otomatis saat app pertama dibuka (Android 13+, API 33+). Dipanggil sekali per launch
     * sampai diberikan : Android hanya menampilkan dialog sekali. Guard memakai
     * checkSelfPermission (granted, bukan areNotificationsEnabled : fix review): izin yang
     * pernah diberikan tapi dimatikan di setelan tidak perlu diminta lagi. Hasilnya tidak
     * mengubah perilaku app: izin ditolak → receiver tetap jalan tapi silent (guard, spec
     * §10) dan Pengaturan menyediakan tombol buka setelan; diberi → notifikasi tampil normal.
     */
    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // tap notif kedua saat app sudah hidup (launchMode CLEAR_TOP) → extra open_tab/open_tasks fresh
        recreate()
    }

    private companion object {
        const val REQ_POST_NOTIFICATIONS = 41
    }
}
