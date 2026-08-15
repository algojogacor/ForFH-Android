package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences as CorePreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionManager
import com.aryariap.forfh.network.ApiClient
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.PersistentCookieJar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(private val app: ForfhApp) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(app) }

    private val dataStore: DataStore<CorePreferences> by lazy {
        androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            scope = applicationScope,
            produceFile = { app.preferencesDataStoreFile("forfh_prefs") },
        )
    }

    val prefs: Preferences by lazy { Preferences(dataStore) }
    val secureCookieStore: SecureCookieStore by lazy { SecureCookieStore(dataStore, applicationScope) }
    val sessionManager: SessionManager by lazy { SessionManager(secureCookieStore) }

    private val cookieJar: PersistentCookieJar by lazy {
        PersistentCookieJar(secureCookieStore, applicationScope)
    }

    val apiService: ForfhApiService by lazy {
        ApiClient.retrofit(ApiClient.build(cookieJar, sessionManager))
    }

    val planner: com.aryariap.forfh.alarm.AlarmPlanner by lazy { com.aryariap.forfh.alarm.AlarmPlanner() }
    val scheduler: com.aryariap.forfh.alarm.AlarmScheduler by lazy {
        com.aryariap.forfh.alarm.AlarmScheduler(com.aryariap.forfh.alarm.AndroidAlarmApi(app))
    }
    val rescheduler: com.aryariap.forfh.sync.AlarmRescheduler by lazy {
        com.aryariap.forfh.sync.AlarmRescheduler(planner, scheduler, database.scheduledAlarmsDao(), database.schedulesDao(), prefs)
    }
    val notifications: com.aryariap.forfh.alarm.ForfhNotifications by lazy { com.aryariap.forfh.alarm.ForfhNotifications(app) }
    val alarmFlow: com.aryariap.forfh.alarm.AlarmFlowHandler by lazy {
        com.aryariap.forfh.alarm.AlarmFlowHandler(
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
}
