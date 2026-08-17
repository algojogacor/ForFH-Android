package com.aryariap.forfh.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.aryariap.forfh.sync.SyncStateStore

class Preferences(private val dataStore: DataStore<Preferences>) : SyncStateStore {

    // Per-hari (0=Minggu..6=Sabtu, konvensi API ForFH): stringSet "180,120,60" dst.
    // Key lama alarm_offset_{3h,2h,1h} tetap dibaca SEKALI sebagai migrasi bila key baru belum ada.
    private fun dayKey(day: Int) = stringSetPreferencesKey("offset_day_$day")
    private val keyOffset3h = booleanPreferencesKey("alarm_offset_3h")
    private val keyOffset2h = booleanPreferencesKey("alarm_offset_2h")
    private val keyOffset1h = booleanPreferencesKey("alarm_offset_1h")
    private val keyLastSyncAt = longPreferencesKey("last_sync_at")
    private val keyLastSyncStatus = stringPreferencesKey("last_sync_status")
    private val keyMutedDate = stringPreferencesKey("alarms_muted_date")
    private val keyPendingMarkDone = stringSetPreferencesKey("pending_mark_done")

    val offsets: Flow<AlarmOffsets> = dataStore.data.map { p ->
        val hasNewKeys = (0..6).any { p[dayKey(it)] != null }
        if (!hasNewKeys) {
            // Upgrade pertama dari toggle lama → daftar yang sama untuk semua hari (customisasi lama terjaga)
            AlarmOffsets.fromLegacy(
                offset3h = p[keyOffset3h] ?: true,
                offset2h = p[keyOffset2h] ?: true,
                offset1h = p[keyOffset1h] ?: true,
            )
        } else {
            AlarmOffsets((0..6).associate { day ->
                day to (p[dayKey(day)]?.mapNotNull { it.toIntOrNull() }.orEmpty())
            })
        }
    }

    suspend fun setOffsets(o: AlarmOffsets) {
        dataStore.edit { p ->
            for (day in 0..6) {
                p[dayKey(day)] = o.perDay[day].orEmpty().map { it.toString() }.toSet()
            }
        }
    }

    /** Tanggal "yyyy-MM-dd" saat user mematikan seluruh alarm kuliah hari itu (null = normal). */
    val mutedDate: Flow<String?> = dataStore.data.map { it[keyMutedDate] }

    suspend fun setMutedDate(date: String?) {
        dataStore.edit { p ->
            if (date == null) p.remove(keyMutedDate) else p[keyMutedDate] = date
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

    // ---- Pending mark selesai (Task 10, ruling R25): stringSet id tugas yang PUT-nya belum
    // dikonfirmasi server — sync me-re-apply status ini setelah wipe-and-replace. ----

    override suspend fun pendingMarkDone(): Set<String> =
        dataStore.data.first()[keyPendingMarkDone].orEmpty()

    override suspend fun setPendingMarkDone(ids: Set<String>) {
        dataStore.edit { p ->
            if (ids.isEmpty()) p.remove(keyPendingMarkDone) else p[keyPendingMarkDone] = ids
        }
    }

    override suspend fun addPendingMarkDone(id: String) {
        dataStore.edit { p -> p[keyPendingMarkDone] = (p[keyPendingMarkDone] ?: emptySet()) + id }
    }

    override suspend fun removePendingMarkDone(id: String) {
        dataStore.edit { p ->
            val rest = (p[keyPendingMarkDone] ?: emptySet()) - id
            if (rest.isEmpty()) p.remove(keyPendingMarkDone) else p[keyPendingMarkDone] = rest
        }
    }
}
