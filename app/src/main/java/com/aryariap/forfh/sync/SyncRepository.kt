package com.aryariap.forfh.sync

import android.util.Log
import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.toEntity
import com.aryariap.forfh.network.toSnapshot
import java.io.IOException
import java.time.Clock

sealed interface SyncOutcome {
    data class Success(val schedules: Int, val tasks: Int) : SyncOutcome
    data class Failure(val reason: SyncFailure) : SyncOutcome
}

enum class SyncFailure { OFFLINE, SERVER }

/**
 * Wipe-and-replace HANYA saat response sukses & valid, dan HANYA tabel mirror
 * schedules & tasks — scheduled_alarms tidak pernah disentuh di sini (invariant spec §7, §9).
 * Sync Worker / UI tidak pernah menyentuh alarm langsung — AlarmRescheduler yang urus.
 */
class SyncRepository(
    private val api: ForfhApiService,
    private val schedulesDao: SchedulesDao,
    private val tasksDao: TasksDao,
    private val syncState: SyncStateStore,
    private val kampusInfoDao: KampusInfoDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun sync(): SyncOutcome {
        val schedResp = try {
            api.schedules()
        } catch (e: IOException) {
            return markFailure(SyncFailure.OFFLINE)
        } catch (e: Exception) {
            return markFailure(SyncFailure.SERVER)
        }

        val tasksResp = try {
            api.tasks()
        } catch (e: IOException) {
            return markFailure(SyncFailure.OFFLINE)
        } catch (e: Exception) {
            return markFailure(SyncFailure.SERVER)
        }

        if (!schedResp.isSuccessful || !tasksResp.isSuccessful) {
            return markFailure(SyncFailure.SERVER) // 401 ditangani SessionExpiryInterceptor (auto-logout)
        }

        val schedBody = schedResp.body()
        val tasksBody = tasksResp.body()
        if (schedBody == null || tasksBody == null) {
            return markFailure(SyncFailure.SERVER) // body null (mis. 204 tanpa konten) — bukan crash
        }
        val schedules = schedBody.schedules.map { it.toEntity() }
        val tasks = tasksBody.tasks.map { it.toEntity(nowMs = clock.millis()) }

        schedulesDao.replaceAll(schedules)
        tasksDao.replaceAll(tasks)
        syncState.setLastSync(clock.millis(), "ok")
        syncKampusInfo() // R4: setelah sukses, terpisah — campus info TIDAK pernah menggagalkan sync
        return SyncOutcome.Success(schedules.size, tasks.size)
    }

    /**
     * Fetch info kampus (ruling R4/R23) SETELAH sync utama sukses, sekuensial dalam fungsi
     * yang sama (tidak ada lock di SyncRepository). Route web mengembalikan SEMUA baris
     * campusData setiap kali → saveSnapshot wipe-and-replace (delete-if-absent), dan
     * connected=false + items kosong → tabel kampus dibersihkan (meta tetap ditulis).
     *
     * Kegagalan apa pun (network/HTTP/parse/simpan) hanya di-log dan dilewati: campus info
     * adalah data bonus — jadwal/tugas & alarm tetap sumber utama. Sesuai plan: "jangan
     * tambah titik kegagalan ke sync utama".
     */
    private suspend fun syncKampusInfo() {
        val resp = try {
            api.campusInfo()
        } catch (e: IOException) {
            Log.w(TAG, "campus info gagal (offline), dilewati: ${e.message}")
            return
        } catch (e: Exception) {
            Log.w(TAG, "campus info gagal (server), dilewati: ${e.message}")
            return
        }
        if (!resp.isSuccessful) {
            Log.w(TAG, "campus info HTTP ${resp.code()}, dilewati")
            return
        }
        val body = resp.body() ?: run {
            Log.w(TAG, "campus info body null, dilewati")
            return
        }
        try {
            kampusInfoDao.saveSnapshot(body.toSnapshot())
        } catch (e: Exception) {
            Log.w(TAG, "campus info simpan gagal, dilewati: ${e.message}")
        }
    }

    private suspend fun markFailure(reason: SyncFailure): SyncOutcome {
        syncState.setLastSync(clock.millis(), "error") // Room tidak disentuh — alarm tetap jalan dari data lokal
        return SyncOutcome.Failure(reason)
    }

    private companion object {
        const val TAG = "SyncRepository"
    }
}
