package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.SchedulesDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
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
        override suspend fun updateStatus(id: String, status: String, computedStatus: String?) {
            items = items.map { if (it.id == id) it.copy(status = status, computedStatus = computedStatus) else it }
        }
        override fun clearAll() { items = emptyList() }
        override fun insertAll(items: List<TaskEntity>) { this.items = items }
        override suspend fun replaceAll(items: List<TaskEntity>) { this.items = items }
    }

    private class FakeApi(
        var scheduleResponse: Response<SchedulesResponse> = Response.success(200, SchedulesResponse(emptyList())),
        var tasksResponse: Response<TasksResponse> = Response.success(200, TasksResponse(emptyList())),
    ) : ForfhApiService {
        var schedulesCalls = 0
        override suspend fun login(body: LoginRequest): Response<LoginResponse> =
            Response.success(200, LoginResponse(true, UserDto("u1", "a", "A", "1")))
        override suspend fun schedules(): Response<SchedulesResponse> { schedulesCalls++; return scheduleResponse }
        override suspend fun tasks(): Response<TasksResponse> = tasksResponse
        override suspend fun markDone(id: String, body: MarkDoneRequest): Response<SuccessResponse> =
            Response.success(200, SuccessResponse(true, id))
        override suspend fun campusInfo(): Response<KampusInfoEnvelopeDto> =
            Response.success(200, KampusInfoEnvelopeDto())
    }

    private class FakeState : SyncStateStore {
        var lastSync = 0L
        var status = ""
        override suspend fun setLastSync(epochMillis: Long, status: String) { lastSync = epochMillis; this.status = status }
        override suspend fun lastSyncAt(): Long = lastSync
        override suspend fun lastSyncStatus(): String = status
    }

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
        val repo = SyncRepository(api, schedDao, taskDao, state)
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
        val repo = SyncRepository(api, schedDao, taskDao, state)
        val out = repo.sync()
        assertTrue(out is SyncOutcome.Failure)
        assertEquals(SyncFailure.SERVER, (out as SyncOutcome.Failure).reason)
        assertEquals("s-old", schedDao.items.single().id)   // Room lama tetap
        assertEquals("t-old", taskDao.items.single().id)
        assertEquals("error", state.status)
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
