package com.aryariap.forfh

import android.app.Application
import com.aryariap.forfh.sync.SyncWorker

class ForfhApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncWorker.enqueuePeriodic(this) // safety net ±6 jam; KEEP bila sudah ada
    }
}
