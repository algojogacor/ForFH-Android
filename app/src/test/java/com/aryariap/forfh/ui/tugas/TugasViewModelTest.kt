package com.aryariap.forfh.ui.tugas

import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.KampusInfoEnvelopeDto
import com.aryariap.forfh.network.LoginRequest
import com.aryariap.forfh.network.LoginResponse
import com.aryariap.forfh.network.MarkDoneRequest
import com.aryariap.forfh.network.ScheduleDto
import com.aryariap.forfh.network.SchedulesResponse
import com.aryariap.forfh.network.SuccessResponse
import com.aryariap.forfh.network.TaskDto
import com.aryariap.forfh.network.TasksResponse
import com.aryariap.forfh.network.UserDto
import com.aryariap.forfh.sync.SyncStateStore
import com.aryariap.forfh.ui.info.SyncActivity
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path

/**
 * TugasViewModel — markDone optimistik (Task 10): UI jadi DONE + syncState PENDING
 * SEKETIKA (sebelum PUT selesai), PUT berjalan di background, hasilnya SYNCED/FAILED.
 * Pola InfoViewModelTest: Dispatchers.setMain(StandardTestDispatcher) + runTest(dispatcher)
 * → advanceUntilIdle deterministik; PUT di-fake dengan CompletableDeferred sebagai gerbang
 * supaya bisa menguji kondisi "PUT belum selesai" secara eksplisit.
 */
class TugasViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------- fakes ----------

    private class FakeTasksDao(initial: List<TaskEntity> = emptyList()) : TasksDao {
        val items = MutableStateFlow(initial)
        override fun getAll(): Flow<List<TaskEntity>> = items
        override fun getById(id: String): Flow<TaskEntity?> =
            MutableStateFlow(items.value.firstOrNull { it.id == id })
        override fun getAllOnce(): List<TaskEntity> = items.value
        override fun getByIdOnce(id: String): TaskEntity? = items.value.firstOrNull { it.id == id }
        override fun getActiveByDeadline(): List<TaskEntity> = items.value.filter { it.status != "DONE" }
            .sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        override fun getDueTasksOnce(fromMillis: Long, toMillis: Long): List<TaskEntity> =
            items.value.filter { it.status != "DONE" && it.dueAt != null && it.dueAt >= fromMillis && it.dueAt < toMillis }
        override suspend fun updateMarked(id: String) {
            items.value = items.value.map {
                if (it.id == id) it.copy(status = "DONE", computedStatus = null, syncState = TaskEntity.SyncState.PENDING) else it
            }
        }
        override suspend fun updateUnmarked(id: String) {
            items.value = items.value.map {
                if (it.id == id) it.copy(status = "NOT_STARTED", computedStatus = null, syncState = TaskEntity.SyncState.PENDING) else it
            }
        }
        override suspend fun updateSyncState(id: String, state: String) {
            items.value = items.value.map { if (it.id == id) it.copy(syncState = state) else it }
        }
        override fun clearAll() { items.value = emptyList() }
        override fun insertAll(items: List<TaskEntity>) { this.items.value = items }
        override suspend fun replaceAll(items: List<TaskEntity>) { this.items.value = items }
    }

    /**
     * markDone di-gerbang CompletableDeferred: panggilan terkumpul, hasil ditentukan test
     * (completeSuccess/completeError) — supaya "PUT belum selesai" bisa diamati.
     */
    private class FakeApi : ForfhApiService {
        val markDoneIds = mutableListOf<String>()
        private val gate = CompletableDeferred<Response<SuccessResponse>>()
        var failWith: IOException? = null

        override suspend fun markDone(@Path("id") id: String, @Body body: MarkDoneRequest): Response<SuccessResponse> {
            markDoneIds += id
            failWith?.let { throw it }
            return gate.await()
        }

        fun completeSuccess() = gate.complete(Response.success(200, SuccessResponse(true, "t1")))
        fun completeError() = gate.complete(
            Response.error(500, okhttp3.ResponseBody.create(null, "{}")),
        )
        override suspend fun login(body: LoginRequest): Response<LoginResponse> =
            Response.success(200, LoginResponse(true, UserDto("u1", "a", "A", "1")))
        override suspend fun schedules(): Response<SchedulesResponse> = Response.success(200, SchedulesResponse(emptyList()))
        override suspend fun tasks(): Response<TasksResponse> = Response.success(200, TasksResponse(emptyList()))
        override suspend fun campusInfo(): Response<KampusInfoEnvelopeDto> = Response.success(200, KampusInfoEnvelopeDto())
    }

    private class FakeSyncState : SyncStateStore {
        var pending = emptySet<String>()
        override suspend fun setLastSync(epochMillis: Long, status: String) {}
        override suspend fun lastSyncAt(): Long = 0L
        override suspend fun lastSyncStatus(): String = ""
        override suspend fun pendingMarkDone(): Set<String> = pending
        override suspend fun setPendingMarkDone(ids: Set<String>) { pending = ids }
        override suspend fun addPendingMarkDone(id: String) { pending = pending + id }
        override suspend fun removePendingMarkDone(id: String) { pending = pending - id }
    }

    private class FakeTugasContainer(
        override val tasksDao: TasksDao,
        override val apiService: ForfhApiService,
        override val syncState: SyncStateStore,
    ) : TugasContainer {
        /** Aktivitas sync (WorkManager di-fake) — indikator pull-to-refresh. */
        override val syncActivity = MutableStateFlow(SyncActivity.IDLE)
        /** Status/waktu sync terakhir — footer "Terakhir sinkron". */
        override val lastSyncStatus = MutableStateFlow("")
        override val lastSyncAt = MutableStateFlow(0L)
        /** Jumlah panggilan enqueueSync (pull-to-refresh / tombol sync). */
        var syncNowCount = 0

        override val enqueueSync: () -> Unit = { syncNowCount++ }
    }

    private fun task(id: String = "t1") = TaskEntity(
        id = id, courseId = null, courseName = null, courseCode = null, title = "Tugas $id",
        description = null, dueAt = null, status = "NOT_STARTED", computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null, syncState = TaskEntity.SyncState.SYNCED,
    )

    private fun vm(dao: FakeTasksDao, api: FakeApi, state: FakeSyncState) =
        TugasViewModel(FakeTugasContainer(dao, api, state))

    // ---------- tests ----------

    @Test
    fun `markDone optimistik - DONE dan PENDING muncul sebelum PUT selesai`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val state = FakeSyncState()
        val viewModel = vm(dao, api, state)

        viewModel.markDone("t1")
        advanceUntilIdle() // PUT belum dikirim hasilnya (gerbang belum dibuka)

        // UI + Room sudah DONE + PENDING, dan id tercatat pending
        assertEquals("DONE", dao.items.value.single().status)
        assertEquals(TaskEntity.SyncState.PENDING, dao.items.value.single().syncState)
        assertEquals("DONE", viewModel.state.value.items.single().status)
        assertEquals(TaskEntity.SyncState.PENDING, viewModel.state.value.items.single().syncState)
        assertEquals(setOf("t1"), state.pending)
        assertEquals(1, api.markDoneIds.size) // PUT benar-benar berjalan di background

        // PUT sukses → SYNCED, pending dibersihkan; TANPA pesan sukses (Task 11: chip status
        // sudah jadi "Selesai" — banner sukses hanya laporan ganda; message = null saja).
        api.completeSuccess()
        advanceUntilIdle()
        assertEquals(TaskEntity.SyncState.SYNCED, dao.items.value.single().syncState)
        assertTrue(state.pending.isEmpty())
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `markDone - PUT HTTP error jadi FAILED dan pesan gagal`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val state = FakeSyncState()
        val viewModel = vm(dao, api, state)

        viewModel.markDone("t1")
        advanceUntilIdle()
        api.completeError()
        advanceUntilIdle()

        assertEquals("DONE", dao.items.value.single().status) // status lokal tetap DONE
        assertEquals(TaskEntity.SyncState.FAILED, dao.items.value.single().syncState)
        assertTrue(state.pending.isEmpty()) // bukan PENDING lagi — retry via ketuk UI
        assertEquals("Gagal menandai selesai. Cek koneksi, coba lagi.", viewModel.state.value.message)
    }

    @Test
    fun `markDone - network error jadi FAILED dan pesan gagal`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val state = FakeSyncState()
        val viewModel = vm(dao, api, state)

        api.failWith = IOException("no network")
        viewModel.markDone("t1")
        advanceUntilIdle()

        assertEquals(TaskEntity.SyncState.FAILED, dao.items.value.single().syncState)
        assertTrue(state.pending.isEmpty())
        assertEquals("Gagal menandai selesai. Cek koneksi, coba lagi.", viewModel.state.value.message)
    }

    @Test
    fun `markDone - double-tap diabaikan - satu PUT`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val viewModel = vm(dao, api, FakeSyncState())

        viewModel.markDone("t1")
        viewModel.markDone("t1") // tap kedua saat masih dikirim
        advanceUntilIdle()

        assertEquals(1, api.markDoneIds.size) // hanya satu PUT
        assertEquals(TaskEntity.SyncState.PENDING, dao.items.value.single().syncState)

        api.completeSuccess()
        advanceUntilIdle()
        assertEquals(TaskEntity.SyncState.SYNCED, dao.items.value.single().syncState)
    }

    @Test
    fun `markDone - retry setelah FAILED mengirim PUT lagi`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val state = FakeSyncState()
        val viewModel = vm(dao, api, state)

        // gagal dulu
        api.failWith = IOException("no network")
        viewModel.markDone("t1")
        advanceUntilIdle()
        assertEquals(TaskEntity.SyncState.FAILED, dao.items.value.single().syncState)

        // retry via ketuk chip → PUT dikirim ulang
        api.failWith = null
        viewModel.markDone("t1")
        advanceUntilIdle()
        assertEquals(2, api.markDoneIds.size)
        assertEquals(TaskEntity.SyncState.PENDING, dao.items.value.single().syncState)

        api.completeSuccess()
        advanceUntilIdle()
        assertEquals(TaskEntity.SyncState.SYNCED, dao.items.value.single().syncState)
        assertTrue(state.pending.isEmpty())
    }

    @Test
    fun `syncNow - memanggil enqueueSync (pull-to-refresh)`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val container = FakeTugasContainer(dao, FakeApi(), FakeSyncState())
        val viewModel = TugasViewModel(container)
        advanceUntilIdle() // koleksi Room flow pertama

        viewModel.syncNow()

        assertEquals(1, container.syncNowCount)
        // syncNow tidak menyentuh data — list tetap render dari Room (cache-first)
        assertEquals(1, viewModel.state.value.items.size)
    }

    @Test
    fun `syncActivity RUNNING sampai ke state - indikator pull-to-refresh`() = runTest(dispatcher) {
        val container = FakeTugasContainer(FakeTasksDao(initial = listOf(task())), FakeApi(), FakeSyncState())
        val viewModel = TugasViewModel(container)

        assertEquals(SyncActivity.IDLE, viewModel.state.value.syncActivity)

        container.syncActivity.value = SyncActivity.RUNNING
        advanceUntilIdle()
        assertEquals(SyncActivity.RUNNING, viewModel.state.value.syncActivity)

        container.syncActivity.value = SyncActivity.IDLE
        advanceUntilIdle()
        assertEquals(SyncActivity.IDLE, viewModel.state.value.syncActivity)
    }

    @Test
    fun `consumeMessage - pesan sekali-pakai dibersihkan setelah banner tampil`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val viewModel = vm(dao, api, FakeSyncState())

        api.failWith = IOException("no network")
        viewModel.markDone("t1")
        advanceUntilIdle()
        assertEquals("Gagal menandai selesai. Cek koneksi, coba lagi.", viewModel.state.value.message)

        viewModel.consumeMessage()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `retry sukses membersihkan pesan gagal sebelumnya`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task()))
        val api = FakeApi()
        val viewModel = vm(dao, api, FakeSyncState())

        // gagal dulu → banner gagal
        api.failWith = IOException("no network")
        viewModel.markDone("t1")
        advanceUntilIdle()
        assertEquals("Gagal menandai selesai. Cek koneksi, coba lagi.", viewModel.state.value.message)

        // retry sukses → banner gagal TIDAK boleh tersisa (tanpa pesan sukses baru)
        api.failWith = null
        viewModel.markDone("t1")
        advanceUntilIdle()
        api.completeSuccess()
        advanceUntilIdle()
        assertNull(viewModel.state.value.message)
        assertEquals(TaskEntity.SyncState.SYNCED, dao.items.value.single().syncState)
    }

    @Test
    fun `unmarkDone optimistik - NOT_STARTED dan PENDING muncul sebelum PUT selesai`() = runTest(dispatcher) {
        val completedTask = task().copy(status = "DONE")
        val dao = FakeTasksDao(initial = listOf(completedTask))
        val api = FakeApi()
        val viewModel = vm(dao, api, FakeSyncState())

        viewModel.unmarkDone("t1")
        advanceUntilIdle()

        // UI + Room sudah NOT_STARTED + PENDING
        assertEquals("NOT_STARTED", dao.items.value.single().status)
        assertEquals(TaskEntity.SyncState.PENDING, dao.items.value.single().syncState)

        // Setelah PUT sukses -> SYNCED
        api.completeSuccess()
        advanceUntilIdle()
        assertEquals("NOT_STARTED", dao.items.value.single().status)
        assertEquals(TaskEntity.SyncState.SYNCED, dao.items.value.single().syncState)
    }

    @Test
    fun `toggleDone otomatis unmark jika DONE dan markDone jika NOT_STARTED`() = runTest(dispatcher) {
        val dao = FakeTasksDao(initial = listOf(task("t1").copy(status = "DONE"), task("t2").copy(status = "NOT_STARTED")))
        val api = FakeApi()
        val viewModel = vm(dao, api, FakeSyncState())
        advanceUntilIdle()

        // t1 (DONE) -> di-toggle jadi NOT_STARTED
        viewModel.toggleDone("t1")
        advanceUntilIdle()
        assertEquals("NOT_STARTED", dao.items.value.first { it.id == "t1" }.status)

        // t2 (NOT_STARTED) -> di-toggle jadi DONE
        viewModel.toggleDone("t2")
        advanceUntilIdle()
        assertEquals("DONE", dao.items.value.first { it.id == "t2" }.status)
    }
}
