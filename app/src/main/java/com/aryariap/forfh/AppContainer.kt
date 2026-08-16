package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences as CorePreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.aryariap.forfh.alarm.AlarmFlowHandler
import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.alarm.AndroidAlarmApi
import com.aryariap.forfh.alarm.ForfhNotifications
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionEvent
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.network.ApiClient
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.PersistentCookieJar
import com.aryariap.forfh.sync.AlarmRescheduler
import com.aryariap.forfh.sync.RescheduleAll
import com.aryariap.forfh.sync.SyncRepository
import com.aryariap.forfh.sync.SyncWorker
import com.aryariap.forfh.ui.info.InfoContainer
import com.aryariap.forfh.widget.refreshAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ketergantungan kartu "Berikutnya" (NextUpViewModel) — kontrak komposisi yang dipenuhi
 * AppContainer; test memakai fake. Memakai seam RescheduleAll supaya quick mute bisa di-fake.
 */
interface NextUpContainer {
    val schedulesDao: SchedulesDao
    val alarmsDao: ScheduledAlarmsDao
    val prefs: Preferences
    val rescheduler: RescheduleAll
    val planner: AlarmPlanner
}

class AppContainer(private val app: ForfhApp) : NextUpContainer, InfoContainer {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val context: android.content.Context get() = app

    val database: AppDatabase by lazy { AppDatabase.build(app) }

    override val schedulesDao: SchedulesDao by lazy { database.schedulesDao() }
    override val alarmsDao: ScheduledAlarmsDao by lazy { database.scheduledAlarmsDao() }
    override val kampusInfoDao: KampusInfoDao by lazy { database.kampusInfoDao() }

    private val dataStore: DataStore<CorePreferences> by lazy {
        PreferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { app.preferencesDataStoreFile("forfh_prefs") },
        )
    }

    override val prefs: Preferences by lazy { Preferences(dataStore) }
    val secureCookieStore: SecureCookieStore by lazy { SecureCookieStore(dataStore) }
    val sessionManager: SessionManager by lazy { SessionManager(secureCookieStore) }

    private val cookieJar: PersistentCookieJar by lazy {
        PersistentCookieJar(secureCookieStore, applicationScope)
    }

    val apiService: ForfhApiService by lazy {
        ApiClient.retrofit(ApiClient.build(cookieJar, sessionManager))
    }

    override val planner: AlarmPlanner by lazy { AlarmPlanner() }
    val scheduler: AlarmScheduler by lazy { AlarmScheduler(AndroidAlarmApi(app)) }
    override val rescheduler: AlarmRescheduler by lazy {
        AlarmRescheduler(
            planner, scheduler, database.scheduledAlarmsDao(), database.schedulesDao(), prefs,
            database.tasksDao(),
            // Task 4: setelah reschedule alarm (sync sukses, mute/unmute, offset, exact-restore)
            // widget ikut di-refresh; kegagalan refresh non-fatal (refreshAll menelan sendiri).
            onAlarmsChanged = { refreshAll(app) },
        )
    }
    val notifications: ForfhNotifications by lazy { ForfhNotifications(app) }

    // ---- InfoContainer (layar Info, Task 8) ----
    override val lastSyncStatus: Flow<String> get() = prefs.lastSyncStatus
    override val lastSyncAt: Flow<Long> get() = prefs.lastSyncAt

    /** Sinyal "sync sedang berjalan/menunggu" dari unique work "sync_once" (WorkManager). */
    override val syncRunning: Flow<Boolean> by lazy {
        WorkManager.getInstance(app).getWorkInfosForUniqueWorkFlow(SyncWorker.UNIQUE_SYNC_ONCE)
            .map { infos ->
                infos.any {
                    it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                }
            }
            .distinctUntilChanged()
    }

    override val enqueueSync: () -> Unit = { SyncWorker.enqueueOneShot(app) }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(apiService, database.schedulesDao(), database.tasksDao(), prefs, database.kampusInfoDao())
    }

    val alarmFlow: AlarmFlowHandler by lazy {
        AlarmFlowHandler(
            context = app,
            database = database,
            prefs = prefs,
            sessionManager = sessionManager,
            rescheduler = rescheduler,
            notifications = notifications,
            planner = planner,
            scope = applicationScope,
        )
    }

    /** Logout §8.10: cancel alarm → hapus scheduled_alarms → wipe Room + DataStore + cookie. */
    fun logout(message: String) {
        applicationScope.launch {
            rescheduler.cancelAll()
            database.scheduledAlarmsDao().clearAll()
            database.schedulesDao().clearAll()
            database.tasksDao().clearAll()
            database.kampusInfoDao().clearKampusInfo()
            database.kampusInfoDao().clearPresensiRecap()
            database.kampusInfoDao().clearKampusMeta()
            cookieJar.clear() // T4-M3: evict cookie in-memory — WAJIB sebelum secureCookieStore.clear()
            secureCookieStore.clear()
            prefs.setLastSync(0L, "")
            sessionManager.tryEmitLoggedOut(message)
        }
    }

    /** Dipanggil MainActivity: daftarkan emitter kejadian sesi. */
    fun collectSessionEvents(onEvent: (SessionEvent) -> Unit) {
        applicationScope.launch { sessionManager.events.collect(onEvent) }
    }
}
