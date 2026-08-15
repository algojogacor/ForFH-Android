package com.aryariap.forfh.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aryariap.forfh.ForfhApp
import java.util.concurrent.TimeUnit

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ForfhApp
        return when (inputData.getString(MODE)) {
            MODE_RECONCILE -> {
                app.container.rescheduler.reconcile()
                Result.success()
            }
            else -> when (val out = app.container.syncRepository.sync()) {
                is SyncOutcome.Success -> {
                    app.container.rescheduler.rescheduleAll() // via AlarmRescheduler — tidak pernah langsung
                    Result.success()
                }
                // Kotlin 2.4: pattern `Failure(OFFLINE)`/`Failure(SERVER)` tak dianggap exhaustive —
                // when pada enum reason dipakai supaya ekshaustif (semantik identik dengan brief).
                is SyncOutcome.Failure -> when (out.reason) {
                    SyncFailure.OFFLINE -> Result.retry()
                    SyncFailure.SERVER -> Result.success()
                }
            }
        }
    }

    companion object {
        private const val MODE = "mode"
        private const val MODE_RECONCILE = "reconcile"

        /** Login sukses / tombol "Coba lagi" / pull-to-refresh. */
        fun enqueueOneShot(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(MODE to "sync"))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("sync_once", ExistingWorkPolicy.REPLACE, request)
        }

        /** Safety net: ±6 jam, network-constrained — bukan timing guarantee (§8.7, §9). */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(MODE to "sync"))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync_periodic", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** BOOT_COMPLETED / MY_PACKAGE_REPLACED — reconcile dari Room, tanpa perlu network. */
        fun enqueueReconcile(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(MODE to MODE_RECONCILE))
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("reconcile", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
