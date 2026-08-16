package com.aryariap.forfh

import android.app.Application
import com.aryariap.forfh.sync.SyncWorker
import com.aryariap.forfh.widget.refreshAll
import kotlinx.coroutines.launch

class ForfhApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.enqueuePeriodic(this) // safety net ±6 jam; KEEP bila sudah ada
        // Task 4: refresh widget saat app boot, konten terisi tanpa menunggu trigger lain.
        // Non-blocking (launch di applicationScope) dan non-fatal (refreshAll menelan kegagalan).
        container.applicationScope.launch { refreshAll(this@ForfhApp) }
    }
}
