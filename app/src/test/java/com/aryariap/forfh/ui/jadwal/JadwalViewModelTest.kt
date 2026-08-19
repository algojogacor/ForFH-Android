package com.aryariap.forfh.ui.jadwal

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aryariap.forfh.JadwalContainer
import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.KampusInfoEntity
import com.aryariap.forfh.data.db.KampusMetaEntity
import com.aryariap.forfh.data.db.PresensiRecapEntity
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.ui.info.SyncActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class JadwalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testPrefs(scope: CoroutineScope): Preferences {
        val dir = Files.createTempDirectory("forfh-jadwal-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(dir, "test.preferences_pb")
        }
        return Preferences(dataStore)
    }

    // ---------- Fake DAOs ----------

    private class FakeSchedulesDao(val list: List<ScheduleEntity>) : SchedulesDao {
        override fun getAll(): Flow<List<ScheduleEntity>> = flowOf(list)
        override fun getAllOnce(): List<ScheduleEntity> = list
        override fun getEnabledOnce(): List<ScheduleEntity> = list.filter { it.enabled }
        override fun getByIdOnce(id: String): ScheduleEntity? = list.firstOrNull { it.id == id }
        override fun clearAll() = Unit
        override fun insertAll(items: List<ScheduleEntity>) = Unit
        override suspend fun replaceAll(items: List<ScheduleEntity>) = Unit
    }

    private class FakeTasksDao(val list: List<TaskEntity>) : TasksDao {
        override fun getAll(): Flow<List<TaskEntity>> = flowOf(list)
        override fun getById(id: String): Flow<TaskEntity?> = flowOf(list.firstOrNull { it.id == id })
        override fun getByIdOnce(id: String): TaskEntity? = list.firstOrNull { it.id == id }
        override fun getAllOnce(): List<TaskEntity> = list
        override fun getActiveByDeadline(): List<TaskEntity> = list.filter { it.status != "DONE" }
        override fun getDueTasksOnce(fromMillis: Long, toMillis: Long): List<TaskEntity> =
            list.filter { it.status != "DONE" && it.dueAt != null && it.dueAt in fromMillis until toMillis }
        override suspend fun updateMarked(id: String) = Unit
        override suspend fun updateUnmarked(id: String) = Unit
        override suspend fun updateSyncState(id: String, state: String) = Unit
        override fun clearAll() = Unit
        override fun insertAll(items: List<TaskEntity>) = Unit
        override suspend fun replaceAll(items: List<TaskEntity>) = Unit
    }

    private class FakeKampusInfoDao(val list: List<KampusInfoEntity>) : KampusInfoDao {
        override fun getPresensiRecap(): Flow<List<PresensiRecapEntity>> = flowOf(emptyList())
        override fun getKampusInfo(): Flow<List<KampusInfoEntity>> = flowOf(list)
        override fun getMeta(): Flow<KampusMetaEntity?> = flowOf(null)
        override suspend fun getMetaOnce(): KampusMetaEntity? = null
        override suspend fun clearPresensiRecap() = Unit
        override suspend fun clearKampusInfo() = Unit
        override suspend fun clearKampusMeta() = Unit
        override suspend fun insertPresensiRecap(items: List<PresensiRecapEntity>) = Unit
        override suspend fun insertKampusInfo(items: List<KampusInfoEntity>) = Unit
        override suspend fun insertMeta(meta: KampusMetaEntity) = Unit
    }

    private class FakeJadwalContainer(
        override val schedulesDao: SchedulesDao,
        override val tasksDao: TasksDao,
        override val kampusInfoDao: KampusInfoDao,
        override val prefs: Preferences,
        override val syncActivity: Flow<SyncActivity> = flowOf(SyncActivity.IDLE),
    ) : JadwalContainer {
        override val enqueueSync: () -> Unit = {}
    }

    @Test
    fun testUnifiedCalendarStateInitialization() = runTest {
        val testSchedules = listOf(
            ScheduleEntity(
                id = "sched-1",
                courseId = "c-1",
                courseName = "Pengantar Ilmu Hukum",
                courseCode = "FHK101",
                courseColor = "#38BDF8",
                lecturer = "Prof. Dr.",
                credits = 3,
                dayOfWeek = 1, // Senin
                startTime = "07:00",
                endTime = "09:30",
                room = "Ruang 301",
                onlineUrl = null,
                enabled = true
            )
        )

        val targetDate = LocalDate.of(2026, 8, 30)
        val targetEpochMs = targetDate.atStartOfDay(java.time.ZoneId.of("Asia/Jakarta")).plusHours(14).toInstant().toEpochMilli()

        val testTasks = listOf(
            TaskEntity(
                id = "task-1",
                courseId = "c-1",
                courseName = "Pengantar Ilmu Hukum",
                courseCode = "FHK101",
                title = "Tugas Makalah Hukum",
                description = "Kerjakan bab 1",
                dueAt = targetEpochMs,
                priority = "high",
                status = "NOT_STARTED",
                computedStatus = "NOT_STARTED",
                courseColor = "#38BDF8",
                subtasksJson = null
            )
        )

        val academicJson = """[{"KEGIATAN":"Ujian Tengah Semester (UTS)","TGL_MULAI":"2026-08-25","TGL_SELESAI":"2026-08-31"}]"""
        val testInfo = listOf(
            KampusInfoEntity(
                jenis = "kalender_akademik",
                dataJson = academicJson,
                updatedAt = "2026-08-18T00:00:00.000Z"
            )
        )

        val container = FakeJadwalContainer(
            schedulesDao = FakeSchedulesDao(testSchedules),
            tasksDao = FakeTasksDao(testTasks),
            kampusInfoDao = FakeKampusInfoDao(testInfo),
            prefs = testPrefs(backgroundScope)
        )

        val viewModel = JadwalViewModel(container)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value

        assertEquals(1, state.allSchedules.size)
        assertEquals("Pengantar Ilmu Hukum", state.allSchedules[0].courseName)
        assertEquals(1, state.allTasks.size)
        assertEquals("Tugas Makalah Hukum", state.allTasks[0].title)
        assertEquals(1, state.allAcademicEvents.size)
        assertEquals("Ujian Tengah Semester (UTS)", state.allAcademicEvents[0].title)

        // Test filter date matching
        val eventsOnDate = state.getEventsForDate(targetDate)

        assertEquals(1, eventsOnDate.tasks.size)
        assertEquals("Tugas Makalah Hukum", eventsOnDate.tasks[0].title)
        assertEquals(1, eventsOnDate.academic.size)
        assertEquals("Ujian Tengah Semester (UTS)", eventsOnDate.academic[0].title)
    }

    @Test
    fun testTabAndMonthNavigation() = runTest {
        val container = FakeJadwalContainer(
            schedulesDao = FakeSchedulesDao(emptyList()),
            tasksDao = FakeTasksDao(emptyList()),
            kampusInfoDao = FakeKampusInfoDao(emptyList()),
            prefs = testPrefs(backgroundScope)
        )

        val viewModel = JadwalViewModel(container)
        testDispatcher.scheduler.advanceUntilIdle()

        // Select Tab
        viewModel.selectTab(CalendarTab.BULAN)
        assertEquals(CalendarTab.BULAN, viewModel.state.value.selectedTab)

        // Month Navigation
        val initialMonth = viewModel.state.value.selectedMonth
        viewModel.nextMonth()
        assertEquals(initialMonth.plusMonths(1), viewModel.state.value.selectedMonth)

        viewModel.prevMonth()
        assertEquals(initialMonth, viewModel.state.value.selectedMonth)

        // Filter Toggle
        viewModel.toggleFilter(CalendarFilter.TUGAS)
        assertFalse(viewModel.state.value.activeFilters.contains(CalendarFilter.TUGAS))

        viewModel.toggleFilter(CalendarFilter.TUGAS)
        assertTrue(viewModel.state.value.activeFilters.contains(CalendarFilter.TUGAS))
    }
}
