package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.KampusInfoEntity
import com.aryariap.forfh.data.db.KampusInfoSnapshot
import com.aryariap.forfh.data.db.KampusMetaEntity
import com.aryariap.forfh.data.db.PresensiRecapEntity
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.KampusInfoEnvelopeDto
import com.aryariap.forfh.network.KampusInfoItemDto
import com.aryariap.forfh.network.LoginRequest
import com.aryariap.forfh.network.LoginResponse
import com.aryariap.forfh.network.MarkDoneRequest
import com.aryariap.forfh.network.ScheduleDto
import com.aryariap.forfh.network.SchedulesResponse
import com.aryariap.forfh.network.SuccessResponse
import com.aryariap.forfh.network.TaskDto
import com.aryariap.forfh.network.TasksResponse
import com.aryariap.forfh.network.UserDto
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SyncRepositoryTest {

    private class FakeSchedulesDao : SchedulesDao {
        var items = emptyList<ScheduleEntity>()
        override fun getAll(): Flow<List<ScheduleEntity>> = MutableStateFlow(items)
        override fun getAllOnce(): List<ScheduleEntity> = items
        override fun getEnabledOnce(): List<ScheduleEntity> = items.filter { it.enabled }
        override fun getByIdOnce(id: String): ScheduleEntity? = items.firstOrNull { it.id == id }
        override fun clearAll() { items = emptyList() }
        override fun insertAll(items: List<ScheduleEntity>) { this.items = items }
        override suspend fun replaceAll(items: List<ScheduleEntity>) { this.items = items }
    }

    private class FakeTasksDao : TasksDao {
        var items = emptyList<TaskEntity>()
        override fun getAll(): Flow<List<TaskEntity>> = MutableStateFlow(items)
        override fun getById(id: String): Flow<TaskEntity?> = MutableStateFlow(items.firstOrNull { it.id == id })
        override fun getAllOnce(): List<TaskEntity> = items
        override fun getByIdOnce(id: String): TaskEntity? = items.firstOrNull { it.id == id }
        override fun getActiveByDeadline(): List<TaskEntity> = items.filter { it.status != "DONE" }
            .sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        override fun getDueTasksOnce(fromMillis: Long, toMillis: Long): List<TaskEntity> =
            items.filter { it.status != "DONE" && it.dueAt != null && it.dueAt >= fromMillis && it.dueAt < toMillis }
        override suspend fun updateMarked(id: String) {
            items = items.map {
                if (it.id == id) it.copy(status = "DONE", computedStatus = null, syncState = TaskEntity.SyncState.PENDING) else it
            }
        }
        override suspend fun updateUnmarked(id: String) {
            items = items.map {
                if (it.id == id) it.copy(status = "NOT_STARTED", computedStatus = null, syncState = TaskEntity.SyncState.PENDING) else it
            }
        }
        override suspend fun updateSyncState(id: String, state: String) {
            items = items.map { if (it.id == id) it.copy(syncState = state) else it }
        }
        override fun clearAll() { items = emptyList() }
        override fun insertAll(items: List<TaskEntity>) { this.items = items }
        override suspend fun replaceAll(items: List<TaskEntity>) { this.items = items }
    }

    /** Mencatat saveSnapshot — satu-satunya kontrak sync → DAO kampus. */
    private class FakeKampusInfoDao : KampusInfoDao {
        val snapshots = mutableListOf<KampusInfoSnapshot>()
        override fun getPresensiRecap(): Flow<List<PresensiRecapEntity>> = flowOf(emptyList())
        override fun getKampusInfo(): Flow<List<KampusInfoEntity>> = flowOf(emptyList())
        override fun getMeta(): Flow<KampusMetaEntity?> = flowOf(null)
        override suspend fun getMetaOnce(): KampusMetaEntity? = null
        override suspend fun clearPresensiRecap() {}
        override suspend fun clearKampusInfo() {}
        override suspend fun clearKampusMeta() {}
        override suspend fun insertPresensiRecap(items: List<PresensiRecapEntity>) {}
        override suspend fun insertKampusInfo(items: List<KampusInfoEntity>) {}
        override suspend fun insertMeta(meta: KampusMetaEntity) {}
        override suspend fun saveSnapshot(snapshot: KampusInfoSnapshot) { snapshots += snapshot }
    }

    private class FakeApi(
        var scheduleResponse: Response<SchedulesResponse> = Response.success(200, SchedulesResponse(emptyList())),
        var tasksResponse: Response<TasksResponse> = Response.success(200, TasksResponse(emptyList())),
        var campusInfoResponse: Response<KampusInfoEnvelopeDto> = Response.success(200, KampusInfoEnvelopeDto()),
    ) : ForfhApiService {
        var schedulesCalls = 0
        var campusInfoCalls = 0
        /** Bila di-set, campusInfo() melempar (simulasi transport/network error). */
        var campusInfoError: IOException? = null
        override suspend fun login(body: LoginRequest): Response<LoginResponse> =
            Response.success(200, LoginResponse(true, UserDto("u1", "a", "A", "1")))
        override suspend fun schedules(): Response<SchedulesResponse> { schedulesCalls++; return scheduleResponse }
        override suspend fun tasks(): Response<TasksResponse> = tasksResponse
        override suspend fun markDone(id: String, body: MarkDoneRequest): Response<SuccessResponse> =
            Response.success(200, SuccessResponse(true, id))
        override suspend fun campusInfo(): Response<KampusInfoEnvelopeDto> {
            campusInfoCalls++
            campusInfoError?.let { throw it }
            return campusInfoResponse
        }
    }

    private class FakeState : SyncStateStore {
        var lastSync = 0L
        var status = ""
        var pending = emptySet<String>()
        var throwOnPendingRead = false
        override suspend fun setLastSync(epochMillis: Long, status: String) { lastSync = epochMillis; this.status = status }
        override suspend fun lastSyncAt(): Long = lastSync
        override suspend fun lastSyncStatus(): String = status
        override suspend fun pendingMarkDone(): Set<String> {
            if (throwOnPendingRead) throw IOException("prefs rusak")
            return pending
        }
        override suspend fun setPendingMarkDone(ids: Set<String>) { pending = ids }
        override suspend fun addPendingMarkDone(id: String) { pending = pending + id }
        override suspend fun removePendingMarkDone(id: String) { pending = pending - id }
    }

    private fun okSchedules() = Response.success(200, SchedulesResponse(listOf(newSchedule())))
    private fun okTasks() = Response.success(200, TasksResponse(listOf(newTask())))

    private fun repo(api: FakeApi, infoDao: FakeKampusInfoDao, state: FakeState = FakeState()) =
        SyncRepository(api, FakeSchedulesDao(), FakeTasksDao(), state, infoDao)

    @Test
    fun `sync sukses - wipe and replace kedua tabel dan state ok`() = runTest {
        val schedDao = FakeSchedulesDao()
        val taskDao = FakeTasksDao()
        val state = FakeState()
        schedDao.items = listOf(oldSchedule())
        taskDao.items = listOf(oldTask())
        val api = FakeApi(
            scheduleResponse = Response.success(200, SchedulesResponse(listOf(newSchedule()))),
            tasksResponse = Response.success(200, TasksResponse(listOf(newTask()))),
        )
        val repo = SyncRepository(api, schedDao, taskDao, state, FakeKampusInfoDao())
        val out = repo.sync()
        assertTrue(out is SyncOutcome.Success)
        assertEquals(1, schedDao.items.size)
        assertEquals("s-new", schedDao.items.single().id)      // yang lama tergantikan
        assertEquals("t-new", taskDao.items.single().id)
        assertEquals("ok", state.status)
        assertTrue(state.lastSync > 0)
    }

    @Test
    fun `sync gagal offline - Room TIDAK disentuh dan status error`() = runTest {
        val schedDao = FakeSchedulesDao()
        val taskDao = FakeTasksDao()
        val state = FakeState()
        schedDao.items = listOf(oldSchedule())
        taskDao.items = listOf(oldTask())
        val api = FakeApi(scheduleResponse = Response.error(500, okhttp3.ResponseBody.create(null, "{}")))
        val repo = SyncRepository(api, schedDao, taskDao, state, FakeKampusInfoDao())
        val out = repo.sync()
        assertTrue(out is SyncOutcome.Failure)
        assertEquals(SyncFailure.SERVER, (out as SyncOutcome.Failure).reason)
        assertEquals("s-old", schedDao.items.single().id)   // Room lama tetap
        assertEquals("t-old", taskDao.items.single().id)
        assertEquals("error", state.status)
    }

    @Test
    fun `sync sukses - campus info sukses ikut tersimpan sebagai snapshot`() = runTest {
        val infoDao = FakeKampusInfoDao()
        val api = FakeApi(
            scheduleResponse = okSchedules(),
            tasksResponse = okTasks(),
            campusInfoResponse = Response.success(
                200,
                KampusInfoEnvelopeDto(
                    connected = true,
                    lastSyncAt = "2026-08-17T04:05:06.123Z",
                    items = listOf(
                        KampusInfoItemDto(
                            jenis = "presensi",
                            data = Json.parseToJsonElement(
                                """[{"code":"FHK25601032","name":"Hak Asasi Manusia","tm":14,"hadir":13,"persen":93}]""",
                            ),
                            updatedAt = "2026-08-17T04:05:06.123Z",
                        ),
                        KampusInfoItemDto(
                            jenis = "status_mhs",
                            data = Json.parseToJsonElement("""[{"NIM_MHS":"626103051310","STATUS_AKADEMIK":"Aktif"}]"""),
                            updatedAt = "2026-08-17T04:05:06.123Z",
                        ),
                    ),
                ),
            ),
        )
        val out = repo(api, infoDao).sync()

        assertTrue(out is SyncOutcome.Success)
        assertEquals(1, api.campusInfoCalls)
        assertEquals(1, infoDao.snapshots.size)
        val snap = infoDao.snapshots.single()
        assertEquals(true, snap.connected)
        assertEquals("2026-08-17T04:05:06.123Z", snap.lastSyncAt)
        assertEquals(1, snap.presensi.size)
        assertEquals("FHK25601032", snap.presensi.single().kode)
        assertEquals(1, snap.info.size)
        assertEquals("status_mhs", snap.info.single().jenis)
    }

    @Test
    fun `campus info HTTP error - sync tetap Success dan snapshot TIDAK disimpan`() = runTest {
        val infoDao = FakeKampusInfoDao()
        val state = FakeState()
        val api = FakeApi(
            scheduleResponse = okSchedules(),
            tasksResponse = okTasks(),
            campusInfoResponse = Response.error(500, okhttp3.ResponseBody.create(null, "{}")),
        )
        val out = repo(api, infoDao, state).sync()

        assertTrue(out is SyncOutcome.Success)
        assertEquals("ok", state.status) // kegagalan campus info TIDAK menyentuh status sync utama
        assertEquals(1, api.campusInfoCalls)
        assertTrue(infoDao.snapshots.isEmpty())
    }

    @Test
    fun `campus info network error - sync tetap Success dan snapshot TIDAK disimpan`() = runTest {
        val infoDao = FakeKampusInfoDao()
        val state = FakeState()
        val api = FakeApi(scheduleResponse = okSchedules(), tasksResponse = okTasks())
        api.campusInfoError = IOException("no network")
        val out = repo(api, infoDao, state).sync()

        assertTrue(out is SyncOutcome.Success)
        assertEquals("ok", state.status) // kegagalan campus info TIDAK menyentuh status sync utama
        assertEquals(1, api.campusInfoCalls)
        assertTrue(infoDao.snapshots.isEmpty())
    }

    @Test
    fun `sync gagal - campus info TIDAK dipanggil sama sekali`() = runTest {
        val infoDao = FakeKampusInfoDao()
        val api = FakeApi(
            scheduleResponse = Response.error(500, okhttp3.ResponseBody.create(null, "{}")),
            tasksResponse = okTasks(),
        )
        val out = repo(api, infoDao).sync()

        assertTrue(out is SyncOutcome.Failure)
        assertEquals(0, api.campusInfoCalls)
        assertTrue(infoDao.snapshots.isEmpty())
    }

    // ---------- Task 10: re-apply pending markDone setelah replaceAll ----------

    @Test
    fun `sync sukses - pending markDone diterapkan ulang setelah replaceAll`() = runTest {
        val taskDao = FakeTasksDao()
        val state = FakeState()
        taskDao.items = listOf(oldTask())
        state.pending = setOf("t-new")
        val api = FakeApi(tasksResponse = okTasks())
        val repo = SyncRepository(api, FakeSchedulesDao(), taskDao, state, FakeKampusInfoDao())

        val out = repo.sync()

        assertTrue(out is SyncOutcome.Success)
        val t = taskDao.items.single()
        assertEquals("t-new", t.id)
        assertEquals("DONE", t.status) // PUT belum dikonfirmasi server → "Selesai" tidak tertimpa "Belum"
        assertEquals(null, t.computedStatus)
        assertEquals(TaskEntity.SyncState.PENDING, t.syncState)
        assertEquals("ok", state.status) // re-apply tidak mengubah hasil sync utama
        assertEquals(setOf("t-new"), state.pending) // id tetap pending sampai PUT sukses
    }

    @Test
    fun `sync sukses - pending id yang sudah DONE di server tetap status server`() = runTest {
        val taskDao = FakeTasksDao()
        val state = FakeState()
        state.pending = setOf("t-new")
        val api = FakeApi(tasksResponse = Response.success(200, TasksResponse(listOf(newTask().copy(status = "DONE")))))
        val repo = SyncRepository(api, FakeSchedulesDao(), taskDao, state, FakeKampusInfoDao())

        val out = repo.sync()

        assertTrue(out is SyncOutcome.Success)
        val t = taskDao.items.single()
        assertEquals("DONE", t.status)
        assertEquals(TaskEntity.SyncState.SYNCED, t.syncState) // server sudah konfirmasi → bukan PENDING
    }

    @Test
    fun `sync sukses - pending id yang tidak ada di response tidak ditambahkan`() = runTest {
        val taskDao = FakeTasksDao()
        val state = FakeState()
        state.pending = setOf("t-gone") // server tidak punya tugas ini lagi
        val api = FakeApi(tasksResponse = okTasks())
        val repo = SyncRepository(api, FakeSchedulesDao(), taskDao, state, FakeKampusInfoDao())

        val out = repo.sync()

        assertTrue(out is SyncOutcome.Success)
        val t = taskDao.items.single()
        assertEquals("t-new", t.id) // tugas "t-gone" hilang karena wipe-and-replace (aturan server)
        assertEquals("NOT_STARTED", t.status)
    }

    @Test
    fun `sync sukses - pending store bermasalah TIDAK menggagalkan sync`() = runTest {
        val taskDao = FakeTasksDao()
        val state = FakeState()
        state.throwOnPendingRead = true
        val api = FakeApi(tasksResponse = okTasks())
        val repo = SyncRepository(api, FakeSchedulesDao(), taskDao, state, FakeKampusInfoDao())

        val out = repo.sync()

        assertTrue(out is SyncOutcome.Success)
        assertEquals("ok", state.status)
        assertEquals("t-new", taskDao.items.single().id)
    }

    private fun oldSchedule() = ScheduleEntity("s-old", "c1", "Lama", null, "#3b82f6", null, 2, 1, "08:00", "09:40", null, null, true)
    private fun newSchedule() = ScheduleDto("s-new", "c1", "Baru", null, "#3b82f6", null, 2, 3, "10:00", "11:40", null, null, 1)
    private fun oldTask() = TaskEntity("t-old", null, null, null, "Lama", null, 1L, "NOT_STARTED", null, "medium", null, null) // 12 params — subtasksJson terakhir (koreksi controller)
    private fun newTask() = TaskDto(
        "t-new", "u1", "c1", "Baru", null, "assignment", "2026-08-20T03:00:00.000Z",
        null, "medium", 30, "NOT_STARTED", 0, "manual", null, null, 1, null,
        "2026-08-01T03:00:00.000Z", "2026-08-01T03:00:00.000Z", null, null, emptyList(),
    )
}
