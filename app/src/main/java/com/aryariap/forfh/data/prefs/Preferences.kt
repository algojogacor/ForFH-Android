package com.aryariap.forfh.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.aryariap.forfh.sync.SyncStateStore

class Preferences(private val dataStore: DataStore<Preferences>) : SyncStateStore {

    private val keyOffset3h = booleanPreferencesKey("alarm_offset_3h")
    private val keyOffset2h = booleanPreferencesKey("alarm_offset_2h")
    private val keyOffset1h = booleanPreferencesKey("alarm_offset_1h")
    private val keyLastSyncAt = longPreferencesKey("last_sync_at")
    private val keyLastSyncStatus = stringPreferencesKey("last_sync_status")

    val offsets: Flow<AlarmOffsets> = dataStore.data.map { p ->
        AlarmOffsets(
            offset3h = p[keyOffset3h] ?: true,
            offset2h = p[keyOffset2h] ?: true,
            offset1h = p[keyOffset1h] ?: true,
        )
    }

    suspend fun setOffsets(o: AlarmOffsets) {
        dataStore.edit { p ->
            p[keyOffset3h] = o.offset3h
            p[keyOffset2h] = o.offset2h
            p[keyOffset1h] = o.offset1h
        }
    }

    val lastSyncAt: Flow<Long> = dataStore.data.map { it[keyLastSyncAt] ?: 0L }
    val lastSyncStatus: Flow<String> = dataStore.data.map { it[keyLastSyncStatus] ?: "" }

    override suspend fun setLastSync(epochMillis: Long, status: String) {
        dataStore.edit { p ->
            p[keyLastSyncAt] = epochMillis
            p[keyLastSyncStatus] = status
        }
    }

    override suspend fun lastSyncAt(): Long = dataStore.data.first()[keyLastSyncAt] ?: 0L
    override suspend fun lastSyncStatus(): String = dataStore.data.first()[keyLastSyncStatus] ?: ""
}
