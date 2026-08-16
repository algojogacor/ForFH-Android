package com.aryariap.forfh.ui.jadwal

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aryariap.forfh.NextUpContainer
import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.prefs.Preferences
import com.aryariap.forfh.sync.RescheduleAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * NextUpViewModel — kartu "Berikutnya" di halaman Jadwal (V1.1 Task 1).
 * Test pertama ViewModel di repo ini: plain JUnit4 + kotlinx-coroutines-test (tanpa Robolectric).
 * Pola: Dispatchers.setMain(StandardTestDispatcher) supaya viewModelScope berjalan di test
 * scheduler; background dispatcher yang sama di-inject ke ViewModel; DataStore asli dibuat dengan
 * scope = backgroundScope (test scheduler) sehingga I/O DataStore DETERMINISTIK — tidak ada
 * resume korutin nyata setelah resetMain / race rename antar thread (Windows).
 *
 * CATATAN Windows: di unit test JVM murni, Build.VERSION.SDK_INT == 0 → FileStorage memakai
 * File.renameTo() yang TIDAK bisa menggantikan file yang sudah ada (tidak seperti
 * Files.move(REPLACE_EXISTING) di Android nyata). Maka TIAP test memakai file DataStore
 * fresh dan maksimal SATU write per file (lihat FileMoves.android.kt di datastore-core).
 */
class NextUpViewModelTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = AlarmPlanner(zone)
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

    private class FakeSchedulesDao(private val schedules: List<ScheduleEntity>) : SchedulesDao {
        override fun getAll(): Flow<List<ScheduleEntity>> = flowOf(schedules)
        override fun getAllOnce(): List<ScheduleEntity> = schedules
        override fun getEnabledOnce(): List<ScheduleEntity> = schedules.filter { it.enabled }
        override fun getByIdOnce(id: String): ScheduleEntity? = schedules.firstOrNull { it.id == id }
        override fun clearAll() = Unit
        override fun insertAll(items: List<ScheduleEntity>) = Unit
        override suspend fun replaceAll(items: List<ScheduleEntity>) = Unit
    }

    private class FakeAlarmsDao(initial: List<ScheduledAlarmEntity> = emptyList()) : ScheduledAlarmsDao {
        val rows = initial.associateBy { it.id }.toMutableMap()
        override fun getAll(): Flow<List<ScheduledAlarmEntity>> = flowOf(rows.values.toList())
        override fun getAllOnce(): List<ScheduledAlarmEntity> = rows.values.toList()
        override fun getByIdOnce(id: String): ScheduledAlarmEntity? = rows[id]
        override fun nextClassAlarmOnce(nowMs: Long): ScheduledAlarmEntity? =
            rows.values.filter { it.kind == "CLASS_ALARM" && it.triggerAtMillis > nowMs }
                .minByOrNull { it.triggerAtMillis }
        override fun upsert(row: ScheduledAlarmEntity) { rows[row.id] = row }
        override fun deleteById(id: String) { rows.remove(id) }
        override fun clearAll() { rows.clear() }
    }

    /** Mencatat panggilan rescheduleAll (satu-satunya operasi yang dipakai ViewModel). */
    private class FakeRescheduler : RescheduleAll {
        var rescheduleAllCalls = 0
        override suspend fun rescheduleAll() { rescheduleAllCalls++ }
    }

    private class FakeContainer(
        override val schedulesDao: SchedulesDao,
        override val alarmsDao: ScheduledAlarmsDao,
        override val prefs: Preferences,
        override val rescheduler: RescheduleAll,
    ) : NextUpContainer {
        override val planner = AlarmPlanner() // WIB, sama seperti AppContainer
    }

    // ---------- fixtures ----------

    /** DataStore asli di temp dir, tapi scope-nya test scheduler → semua I/O deterministik. */
    private fun testPrefs(scope: CoroutineScope): Preferences {
        val dir = Files.createTempDirectory("forfh-nextup-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(dir, "test.preferences_pb")
        }
        return Preferences(dataStore)
    }

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun sched(id: String, day: Int, start: String, enabled: Boolean = true) = ScheduleEntity(
        id = id, courseId = "c$id", courseName = "Kuliah $id", courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = day,
        startTime = start, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
    )

    private fun alarmRow(
        id: String,
        triggerAtMillis: Long,
        kind: String = "CLASS_ALARM",
        scheduleId: String? = null,
    ) = ScheduledAlarmEntity(
        id = id, kind = kind, scheduleId = scheduleId, offsetMinutes = 60,
        occurrenceDate = "2026-08-17", triggerAtMillis = triggerAtMillis, snoozeCount = 0,
    )

    private fun vm(container: NextUpContainer, nowProvider: () -> ZonedDateTime) =
        NextUpViewModel(container, background = dispatcher, nowProvider = nowProvider)

    /** Semua pekerjaan (VM + DataStore) di test scheduler → advanceUntilIdle cukup; loop hanya penjaga. */
    private suspend fun TestScope.awaitUntil(message: String, condition: () -> Boolean) {
        var attempts = 0
        while (attempts++ < 1000) {
            advanceUntilIdle()
            if (condition()) return
            yield()
        }
        fail("Kondisi tidak terpenuhi: $message")
    }

    // ---------- tests ----------

    @Test
    fun `next class - kuliah pertama yang belum lewat (yang lewat dilewati ke minggu depan, disabled tidak dihitung)`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30") // Senin 07:30
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(
                    listOf(
                        sched("s1", day = 1, start = "08:00"), // Senin ini 08:00 — belum lewat → dipilih
                        sched("s2", day = 1, start = "07:00"), // sudah lewat (07:00 < 07:30) → Senin depan
                        sched("s3", day = 2, start = "10:00"), // Selasa 10:00 — lebih lambat dari s1
                        sched("s4", day = 1, start = "06:00", enabled = false), // disabled → diabaikan
                    )
                ),
                alarmsDao = FakeAlarmsDao(),
                prefs = testPrefs(backgroundScope),
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        viewModel.refresh()
        advanceUntilIdle()

        val nextClass = viewModel.state.value.nextClass
        assertEquals("s1", nextClass!!.first.id)
        assertEquals(wib("2026-08-17T08:00"), nextClass.second)
    }

    @Test
    fun `next class - tanpa schedule enabled = null`() = runTest(dispatcher) {
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(
                    listOf(sched("s1", day = 1, start = "08:00", enabled = false))
                ),
                alarmsDao = FakeAlarmsDao(),
                prefs = testPrefs(backgroundScope),
                rescheduler = FakeRescheduler(),
            )
        ) { wib("2026-08-17T07:30") }

        viewModel.refresh()
        advanceUntilIdle()

        assertNull(viewModel.state.value.nextClass)
    }

    @Test
    fun `next class - semua sudah lewat - ambil yang paling awal minggu depan`() = runTest(dispatcher) {
        val now = wib("2026-08-17T08:30") // Senin 08:30 — s1 08:00 dan s2 07:00 hari ini sudah lewat
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(
                    listOf(
                        sched("s1", day = 1, start = "08:00"),
                        sched("s2", day = 1, start = "07:00"),
                    )
                ),
                alarmsDao = FakeAlarmsDao(),
                prefs = testPrefs(backgroundScope),
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        viewModel.refresh()
        advanceUntilIdle()

        val nextClass = viewModel.state.value.nextClass
        assertEquals("s2", nextClass!!.first.id)
        assertEquals(wib("2026-08-24T07:00"), nextClass.second) // Senin depan
    }

    @Test
    fun `next alarm - CLASS_ALARM masa depan terdekat, tugas dan yang sudah lewat dikecualikan`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30")
        val nowMs = now.toInstant().toEpochMilli()
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(emptyList()),
                alarmsDao = FakeAlarmsDao(
                    listOf(
                        alarmRow("class-far", triggerAtMillis = nowMs + 7_200_000),
                        alarmRow("class-near", triggerAtMillis = nowMs + 600_000), // 10 mnt lagi → dipilih
                        alarmRow("class-past", triggerAtMillis = nowMs - 60_000), // lewat → dikecualikan
                        alarmRow("task", kind = "TASK_REMINDER", triggerAtMillis = nowMs + 300_000), // tugas → dikecualikan
                    )
                ),
                prefs = testPrefs(backgroundScope),
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("class-near", viewModel.state.value.nextAlarm!!.id)
    }

    @Test
    fun `mutedToday - true saat prefs mutedDate == hari ini WIB`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30")
        val prefs = testPrefs(backgroundScope)
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(emptyList()),
                alarmsDao = FakeAlarmsDao(),
                prefs = prefs,
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        prefs.setMutedDate("2026-08-17") // hari ini (satu-satunya write ke file ini — lihat CATATAN Windows)
        awaitUntil("mutedToday == true") { viewModel.state.value.mutedToday }
        assertTrue(viewModel.state.value.mutedToday)
    }

    @Test
    fun `mutedToday - false saat prefs mutedDate tanggal lain atau belum pernah di-set`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30")
        val prefs = testPrefs(backgroundScope)
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(emptyList()),
                alarmsDao = FakeAlarmsDao(),
                prefs = prefs,
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        // Belum pernah mute → default false
        advanceUntilIdle()
        assertFalse(viewModel.state.value.mutedToday)

        prefs.setMutedDate("2026-08-16") // kemarin — bukan hari ini (satu-satunya write ke file ini)
        awaitUntil("mutedToday tetap false") { !viewModel.state.value.mutedToday }
        assertFalse(viewModel.state.value.mutedToday)
    }

    @Test
    fun `muteToday - simpan tanggal hari ini + panggil rescheduleAll`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30")
        val prefs = testPrefs(backgroundScope)
        val rescheduler = FakeRescheduler()
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(emptyList()),
                alarmsDao = FakeAlarmsDao(),
                prefs = prefs,
                rescheduler = rescheduler,
            )
        ) { now }

        viewModel.muteToday()
        awaitUntil("rescheduleAll dipanggil saat mute") { rescheduler.rescheduleAllCalls == 1 }
        assertEquals("2026-08-17", prefs.mutedDate.first())
        awaitUntil("mutedToday true setelah mute") { viewModel.state.value.mutedToday }
    }

    @Test
    fun `unmuteToday - hapus tanggal mute + panggil rescheduleAll`() = runTest(dispatcher) {
        val now = wib("2026-08-17T07:30")
        val prefs = testPrefs(backgroundScope)
        val rescheduler = FakeRescheduler()
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(emptyList()),
                alarmsDao = FakeAlarmsDao(),
                prefs = prefs,
                rescheduler = rescheduler,
            )
        ) { now }

        viewModel.unmuteToday()
        awaitUntil("rescheduleAll dipanggil saat unmute") { rescheduler.rescheduleAllCalls == 1 }
        assertEquals(null, prefs.mutedDate.first())
        awaitUntil("mutedToday false setelah unmute") { !viewModel.state.value.mutedToday }
    }

    @Test
    fun `refresh - hitung ulang dari waktu sekarang tiap panggilan (countdown bergerak saat kelas lewat)`() = runTest(dispatcher) {
        var now = wib("2026-08-17T07:30") // Senin 07:30
        val viewModel = vm(
            FakeContainer(
                schedulesDao = FakeSchedulesDao(listOf(sched("s1", day = 1, start = "08:00"))),
                alarmsDao = FakeAlarmsDao(),
                prefs = testPrefs(backgroundScope),
                rescheduler = FakeRescheduler(),
            )
        ) { now }

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(wib("2026-08-17T08:00"), viewModel.state.value.nextClass!!.second)

        now = wib("2026-08-17T08:05") // kelas sudah mulai → next class pindah ke Senin depan
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(wib("2026-08-24T08:00"), viewModel.state.value.nextClass!!.second)
    }
}
