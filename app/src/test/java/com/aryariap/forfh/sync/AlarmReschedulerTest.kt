package com.aryariap.forfh.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.aryariap.forfh.alarm.AlarmApi
import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.alarm.StableHash
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.data.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Perilaku boot path (§8.9): reconcile = re-arm SEMUA row Room — AlarmManager kosong
 * setelah reboot, jadi "row sudah benar" (Keep) TIDAK CUKUP; setiap row harus di-schedule ulang.
 * Fix round final review: bug lama — reconcile memakai computeOps(fullRebuild=false) yang
 * mengeluarkan Keep untuk row yang cocok → alarm mati diam-diam sampai occurrence bergeser.
 */
class AlarmReschedulerTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = AlarmPlanner(zone)

    /** Fake AlarmApi — mencatat schedule calls (pola sama dengan AlarmSchedulerTest). */
    private class FakeAlarmApi : AlarmApi {
        val calls = mutableListOf<String>()
        override fun canScheduleExact(): Boolean = true
        override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
            calls += "exact:$requestCode:$triggerAtMillis"
        }
        override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
            calls += "window:$requestCode:$triggerAtMillis:$windowLengthMillis"
        }
        override fun cancel(requestCode: Int, action: String?) { calls += "cancel:$requestCode" }
    }

    /** Fake DAO in-memory; writes dicatat utk membuktikan reconcile tidak menyentuh Room. */
    private class FakeAlarmsDao(initial: List<ScheduledAlarmEntity>) : ScheduledAlarmsDao {
        val rows = initial.associateBy { it.id }.toMutableMap()
        val writes = mutableListOf<String>()
        override fun getAll(): Flow<List<ScheduledAlarmEntity>> = flowOf(rows.values.toList())
        override fun getAllOnce(): List<ScheduledAlarmEntity> = rows.values.toList()
        override fun getByIdOnce(id: String): ScheduledAlarmEntity? = rows[id]
        override fun nextClassAlarmOnce(nowMs: Long): ScheduledAlarmEntity? =
            rows.values.filter { it.kind == "CLASS_ALARM" && it.triggerAtMillis > nowMs }
                .minByOrNull { it.triggerAtMillis }
        override fun upsert(row: ScheduledAlarmEntity) { rows[row.id] = row; writes += "upsert:${row.id}" }
        override fun deleteById(id: String) { rows.remove(id); writes += "delete:$id" }
        override fun clearAll() { rows.clear() }
    }

    private class FakeSchedulesDao(private val schedules: List<ScheduleEntity>) : SchedulesDao {
        override fun getAll(): Flow<List<ScheduleEntity>> = flowOf(schedules)
        override fun getAllOnce(): List<ScheduleEntity> = schedules
        override fun getEnabledOnce(): List<ScheduleEntity> = schedules.filter { it.enabled }
        override fun getByIdOnce(id: String): ScheduleEntity? = schedules.firstOrNull { it.id == id }
        override fun clearAll() = Unit
        override fun insertAll(items: List<ScheduleEntity>) = Unit
        override suspend fun replaceAll(items: List<ScheduleEntity>) = Unit
    }

    /** Fake TasksDao — getDueTasksOnce memakai kontrak half-open [from, to) yang sama dengan Room. */
    private class FakeTasksDao(private val tasks: List<TaskEntity>) : TasksDao {
        override fun getAll(): Flow<List<TaskEntity>> = flowOf(tasks)
        override fun getById(id: String): Flow<TaskEntity?> = flowOf(tasks.firstOrNull { it.id == id })
        override fun getAllOnce(): List<TaskEntity> = tasks
        override fun getByIdOnce(id: String): TaskEntity? = tasks.firstOrNull { it.id == id }
        override fun getActiveByDeadline(): List<TaskEntity> = tasks.filter { it.status != "DONE" }
            .sortedWith(compareBy<TaskEntity> { it.dueAt ?: Long.MAX_VALUE })
        override fun getDueTasksOnce(fromMillis: Long, toMillis: Long): List<TaskEntity> =
            tasks.filter { it.status != "DONE" && it.dueAt != null && it.dueAt >= fromMillis && it.dueAt < toMillis }
        override suspend fun updateMarked(id: String) = Unit
        override suspend fun updateSyncState(id: String, state: String) = Unit
        override fun clearAll() = Unit
        override fun insertAll(items: List<TaskEntity>) = Unit
        override suspend fun replaceAll(items: List<TaskEntity>) = Unit
    }

    private fun testPrefs(): Preferences {
        val dir = Files.createTempDirectory("forfh-alarm-test").toFile()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO)) {
            File(dir, "test.preferences_pb")
        }
        return Preferences(dataStore)
    }

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    private fun sched(id: String = "s1", day: Int = 1, start: String = "08:00") = ScheduleEntity(
        id = id, courseId = "c1", courseName = "Hukum", courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = day,
        startTime = start, endTime = "09:40", room = "A101", onlineUrl = null, enabled = true,
    )

    private fun task(id: String, title: String, dueAt: Long) = TaskEntity(
        id = id, courseId = null, courseName = "Hukum", courseCode = null, title = title,
        description = null, dueAt = dueAt, status = "NOT_STARTED", computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    /** Baris "yang benar" (class offset 120 + task slot 15) — dihitung dari planner, sumber kebenaran sama. */
    private fun desiredRows(now: ZonedDateTime): Pair<ScheduledAlarmEntity, ScheduledAlarmEntity> {
        val ops = ReconcilePlanner(planner).computeOps(
            emptyList(), listOf(sched()), mapOf(1 to listOf(120)), now, fullRebuild = true, tasks = emptyList(),
        )
        val classRow = ops.filterIsInstance<AlarmOp.Schedule>().first { it.row.kind == "CLASS_ALARM" }.row
        val taskRow = ops.filterIsInstance<AlarmOp.Schedule>().first { it.row.kind == "TASK_REMINDER" && it.row.id.contains("|15|") }.row
        return classRow to taskRow
    }

    private fun rescheduler(
        dao: FakeAlarmsDao,
        schedulesDao: FakeSchedulesDao,
        api: FakeAlarmApi,
        tasksDao: FakeTasksDao = FakeTasksDao(emptyList()),
        onAlarmsChanged: suspend () -> Unit = {},
    ) = AlarmRescheduler(
        planner = planner,
        scheduler = AlarmScheduler(api),
        alarmsDao = dao,
        schedulesDao = schedulesDao,
        prefs = testPrefs(),
        tasksDao = tasksDao,
        onAlarmsChanged = onAlarmsChanged,
    )

    @Test
    fun `reconcile setelah boot - re-arm SEMUA row Room pakai requestCode identity dan trigger tersimpan`() = runTest {
        val now = wib("2026-08-17T00:00") // Senin
        val (classRow, taskRow) = desiredRows(now)
        val dao = FakeAlarmsDao(listOf(classRow, taskRow))
        val api = FakeAlarmApi()
        val rs = rescheduler(dao, FakeSchedulesDao(listOf(sched())), api)

        rs.reconcile()

        // Row yang SUDAH "benar" pun di-schedule ulang (AlarmManager kosong pasca-reboot):
        // satu setExact per row, requestCode = StableHash(identity), trigger = tersimpan di Room.
        assertEquals(
            listOf(
                "exact:${StableHash.of(classRow.id)}:${classRow.triggerAtMillis}",
                "exact:${StableHash.of(taskRow.id)}:${taskRow.triggerAtMillis}",
            ),
            api.calls,
        )
        // Room tidak disentuh (re-arm murni AlarmManager).
        assertTrue(dao.writes.isEmpty())
    }

    @Test
    fun `reconcile setelah boot - row snooze dipertahankan, re-arm dgn trigger tersimpan dan snoozeCount tidak direset`() = runTest {
        val now = wib("2026-08-17T00:00")
        val (classRow, _) = desiredRows(now)
        val snoozed = classRow.copy(triggerAtMillis = classRow.triggerAtMillis + 180_000L, snoozeCount = 2)
        val dao = FakeAlarmsDao(listOf(snoozed))
        val api = FakeAlarmApi()
        val rs = rescheduler(dao, FakeSchedulesDao(listOf(sched())), api)

        rs.reconcile()

        // Yang dipasang = trigger snooze TERSIMPAN (bukan trigger occurrence base) — §8.1 invariant.
        assertEquals(listOf("exact:${StableHash.of(snoozed.id)}:${snoozed.triggerAtMillis}"), api.calls)
        // State snooze utuh di Room.
        assertEquals(2, dao.getByIdOnce(snoozed.id)!!.snoozeCount)
        assertEquals(snoozed.triggerAtMillis, dao.getByIdOnce(snoozed.id)!!.triggerAtMillis)
    }

    @Test
    fun `reconcile idempotent - dua run, requestCode sama, satu setExact per row per run (tanpa double)`() = runTest {
        val now = wib("2026-08-17T00:00")
        val (classRow, taskRow) = desiredRows(now)
        val dao = FakeAlarmsDao(listOf(classRow, taskRow))
        val api = FakeAlarmApi()
        val rs = rescheduler(dao, FakeSchedulesDao(listOf(sched())), api)

        rs.reconcile()
        rs.reconcile()

        // Identity sama → requestCode sama → setExact mengganti, tidak ada double registration.
        assertEquals(
            listOf(
                "exact:${StableHash.of(classRow.id)}:${classRow.triggerAtMillis}",
                "exact:${StableHash.of(taskRow.id)}:${taskRow.triggerAtMillis}",
                "exact:${StableHash.of(classRow.id)}:${classRow.triggerAtMillis}",
                "exact:${StableHash.of(taskRow.id)}:${taskRow.triggerAtMillis}",
            ),
            api.calls,
        )
    }

    @Test
    fun `rescheduleAll - deadline tugas besok - row TASK_DEADLINE dihitung dari tasksDao dan dipasang`() = runTest {
        val now = wib("2026-08-17T10:00") // Senin; t1 deadline Selasa 18
        val dao = FakeAlarmsDao(emptyList())
        val api = FakeAlarmApi()
        val rs = rescheduler(
            dao, FakeSchedulesDao(emptyList()), api,
            tasksDao = FakeTasksDao(listOf(task("t1", "Makalah Hukum", wibEpoch(2026, 8, 18, 23, 59)))),
        )

        rs.rescheduleAll()

        // compute() mengambil kandidat deadline via getDueTasksOnce dan memasang row TASK_DEADLINE
        val expectedId = "taskdl|t1|2026-08-18"
        assertTrue(api.calls.contains("exact:${StableHash.of(expectedId)}:${wibEpoch(2026, 8, 17, 20, 0)}"))
        val stored = dao.getByIdOnce(expectedId)
        assertEquals("TASK_DEADLINE", stored?.kind)
        assertEquals("2026-08-18", stored?.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), stored?.triggerAtMillis)
    }

    @Test
    fun `rescheduleAll - onAlarmsChanged dipanggil setelah execute (hook refresh widget Task 4)`() = runTest {
        val now = wib("2026-08-17T00:00")
        val (classRow, taskRow) = desiredRows(now)
        val dao = FakeAlarmsDao(listOf(classRow, taskRow))
        var refreshed = 0
        val rs = rescheduler(dao, FakeSchedulesDao(listOf(sched())), FakeAlarmApi()) { refreshed++ }

        rs.rescheduleAll()

        // Hook eksekusi penuh: sync sukses / mute / ubah offset / exact-restore semua melewati
        // rescheduleAll → execute → onAlarmsChanged, walau tidak ada row yang berubah (Keep).
        assertEquals(1, refreshed)
        // reconcile (boot) TIDAK memicu hook (row tidak berubah, widget konten sama).
        rs.reconcile()
        assertEquals(1, refreshed)
    }
}
