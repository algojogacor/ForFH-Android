package com.aryariap.forfh

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences as CorePreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aryariap.forfh.data.db.AppDatabase
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.data.prefs.SecureCookieStore
import com.aryariap.forfh.data.prefs.SessionManager
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
}
