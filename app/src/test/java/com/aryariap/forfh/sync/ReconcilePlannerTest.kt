package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ReconcilePlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = ReconcilePlanner(AlarmPlanner(zone))
    private val offsetsByDay: Map<Int, List<Int>> = (0..6).associateWith { listOf(180, 120, 60) }

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    private fun sched(id: String = "s1", day: Int = 1, start: String = "08:00", enabled: Boolean = true) =
        ScheduleEntity(
            id = id, courseId = "c1", courseName = "Hukum", courseCode = null,
            courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = day,
            startTime = start, endTime = "09:40", room = "A101", onlineUrl = null, enabled = enabled,
        )

    private fun classRow(id: String, trigger: Long, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = id, kind = "CLASS_ALARM", scheduleId = "s1", offsetMinutes = 120,
        occurrenceDate = "2026-08-17", triggerAtMillis = trigger, snoozeCount = snoozeCount,
    )

    private fun task(id: String, dueAt: Long, status: String = "NOT_STARTED") = TaskEntity(
        id = id, courseId = null, courseName = null, courseCode = null,
        title = "Tugas $id", description = null,
        dueAt = dueAt, status = status, computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    private fun deadlineRow(id: String, occurrenceDate: String, trigger: Long, snoozeCount: Int = 0) =
        ScheduledAlarmEntity(
            id = id, kind = "TASK_DEADLINE", scheduleId = null, offsetMinutes = 0,
            occurrenceDate = occurrenceDate, triggerAtMillis = trigger, snoozeCount = snoozeCount,
        )

    @Test
    fun `rescheduleAll membangun row untuk tiap offset aktif`() {
        val ops = planner.computeOps(
            emptyList(), listOf(sched()), offsetsByDay, wib("2026-08-15T10:00"), fullRebuild = true,
            tasks = emptyList(),
        )
        // slot tugas "selalu ada" (test lain) → filter ke row CLASS_ALARM untuk per-offset
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(3, rows.size)
        assertEquals(setOf(180, 120, 60), rows.map { it.offsetMinutes }.toSet())
        assertEquals(setOf("class|s1|180|2026-08-17", "class|s1|120|2026-08-17", "class|s1|60|2026-08-17"), rows.map { it.id }.toSet())
    }

    @Test
    fun `per hari - tiap schedule memakai daftar offset harinya sendiri`() {
        // Senin (API 1): [90, 60]; Rabu (API 3): [240, 180] — bebas berapa pun menitnya
        val perDay = mapOf(1 to listOf(90, 60), 3 to listOf(240, 180))
        // Senin 08:00 + Rabu 13:00; now Sabtu → occurrence keduanya minggu depan
        val ops = planner.computeOps(
            emptyList(),
            listOf(sched(id = "s1", day = 1, start = "08:00"), sched(id = "s2", day = 3, start = "13:00")),
            perDay,
            wib("2026-08-15T10:00"),
            fullRebuild = true,
            tasks = emptyList(),
        )
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        // s1 (Senin): hanya 90 & 60 → 2 alarm; s2 (Rabu): 240 & 180 → 2 alarm
        assertEquals(4, rows.size)
        assertEquals(setOf("class|s1|90|2026-08-17", "class|s1|60|2026-08-17"), rows.filter { it.scheduleId == "s1" }.map { it.id }.toSet())
        assertEquals(setOf("class|s2|240|2026-08-19", "class|s2|180|2026-08-19"), rows.filter { it.scheduleId == "s2" }.map { it.id }.toSet())
    }

    @Test
    fun `hanya kuliah pertama per hari yang dapat alarm - kuliah kedua tanpa alarm apa pun`() {
        // Senin (API 1): PIH 13:00 + Ilmu Negara 15:00 — HANYA 13:00 (kuliah pertama, user masih di rumah)
        val ops = planner.computeOps(
            emptyList(),
            listOf(sched(id = "s1", day = 1, start = "13:00"), sched(id = "s2", day = 1, start = "15:00")),
            mapOf(1 to listOf(180, 120, 60)),
            wib("2026-08-15T10:00"),
            fullRebuild = true,
            tasks = emptyList(),
        )
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        // 3 offset × hanya kuliah pertama; s2 tidak dapat alarm sama sekali
        assertEquals(3, classRows.size)
        assertTrue(classRows.all { it.scheduleId == "s1" })
        assertTrue(classRows.none { it.scheduleId == "s2" })
    }

    @Test
    fun `dua kuliah mulai jam sama - keduanya kuliah pertama, keduanya dapat alarm`() {
        val ops = planner.computeOps(
            emptyList(),
            listOf(sched(id = "s1", day = 2, start = "11:00"), sched(id = "s2", day = 2, start = "11:00")),
            mapOf(2 to listOf(120)),
            wib("2026-08-15T10:00"),
            fullRebuild = true,
            tasks = emptyList(),
        )
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(2, classRows.size)
        assertEquals(setOf("s1", "s2"), classRows.map { it.scheduleId }.toSet())
    }

    @Test
    fun `kuliah kedua yang sudah terpasang di-cancel saat aturan aktif`() {
        val now = wib("2026-08-15T00:00")
        // Row lama kuliah kedua (Senin 15:00) dari versi sebelumnya — harus di-cancel
        val oldSecondRow = ScheduledAlarmEntity(
            id = "class|s2|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s2",
            offsetMinutes = 120, occurrenceDate = "2026-08-17",
            triggerAtMillis = 1_750_000_000_000L, snoozeCount = 0,
        )
        val ops = planner.computeOps(
            listOf(oldSecondRow),
            listOf(sched(id = "s1", day = 1, start = "13:00"), sched(id = "s2", day = 1, start = "15:00")),
            mapOf(1 to listOf(120)),
            now,
            fullRebuild = true,
            tasks = emptyList(),
        )
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldSecondRow.id })
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(1, classRows.size)
        assertEquals("s1", classRows.single().scheduleId)
    }

    @Test
    fun `matikan alarm hari ini - row kelas hari itu tidak dibuat, task reminder dan TASK_DEADLINE tetap`() {
        val now = wib("2026-08-15T10:00") // Sabtu
        val ops = planner.computeOps(
            emptyList(),
            listOf(sched(day = 1, start = "13:00")),
            mapOf(1 to listOf(120)),
            now,
            fullRebuild = true,
            skipDates = setOf("2026-08-17"), // Senin dimatikan user
            tasks = listOf(task("t1", wibEpoch(2026, 8, 16, 23, 59))), // deadline Minggu 16 (besok) — tdk boleh kena mute
        )
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(0, classRows.size)
        // task reminder TIDAK ikut dimatikan
        assertEquals(3, ops.filterIsInstance<AlarmOp.Schedule>().count { it.row.kind == "TASK_REMINDER" })
        // TASK_DEADLINE juga TIDAK ikut dimatikan — mute hanya menyentuh CLASS_ALARM
        val deadlineRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "TASK_DEADLINE" }.map { it.row }
        assertEquals(1, deadlineRows.size)
        assertEquals("taskdl|t1|2026-08-16", deadlineRows.single().id)
    }

    @Test
    fun `matikan alarm hari ini - hari berikutnya alarm minggu depan normal lagi`() {
        val now = wib("2026-08-18T00:00") // Selasa — mute kemarin (Senin 17) sudah basi
        val ops = planner.computeOps(
            emptyList(),
            listOf(sched(day = 1, start = "13:00")),
            mapOf(1 to listOf(120)),
            now,
            fullRebuild = true,
            skipDates = setOf("2026-08-17"),
            tasks = emptyList(),
        )
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        // occurrence berikutnya Senin 24 Agu — dibuat normal; tanggal basi tidak menghalangi
        assertEquals(1, classRows.size)
        assertEquals("2026-08-24", classRows.single().occurrenceDate)
    }

    @Test
    fun `matikan alarm hari ini - row snooze aktif hari itu ikut di-cancel`() {
        val now = wib("2026-08-15T00:00")
        val snoozed = ScheduledAlarmEntity(
            id = "class|s1|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s1",
            offsetMinutes = 120, occurrenceDate = "2026-08-17",
            triggerAtMillis = 1_750_000_000_000L, snoozeCount = 2,
        )
        val ops = planner.computeOps(
            listOf(snoozed),
            listOf(sched(day = 1, start = "13:00")),
            mapOf(1 to listOf(120)),
            now,
            fullRebuild = true,
            skipDates = setOf("2026-08-17"),
            tasks = emptyList(),
        )
        // pengecualian aturan snooze-Keep: mute eksplisit user → cancel walau snooze aktif
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == snoozed.id })
    }

    @Test
    fun `per hari - hari tanpa daftar offset tidak punya alarm kelas`() {
        val ops = planner.computeOps(
            emptyList(), listOf(sched(day = 2)), emptyMap(), wib("2026-08-15T10:00"), fullRebuild = true,
            tasks = emptyList(),
        )
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(rows.none { it.kind == "CLASS_ALARM" })
        assertEquals(3, rows.size) // hanya slot tugas
    }

    @Test
    fun `slot tugas selalu ada - 3 slot one-shot`() {
        val ops = planner.computeOps(
            emptyList(), emptyList(), emptyMap(), wib("2026-08-15T10:00"), fullRebuild = true,
            tasks = emptyList(),
        )
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.kind == "TASK_REMINDER" })
        assertEquals(setOf("task|09|2026-08-16", "task|15|2026-08-15", "task|20|2026-08-15"), rows.map { it.id }.toSet()) // now 10:00 → slot 09 lewat → besok
    }

    @Test
    fun `deadline besok - row TASK_DEADLINE masuk desired trigger hari ini jam 2000 WIB`() {
        val now = wib("2026-08-17T10:00") // Senin; t1 deadline Selasa 18
        val ops = planner.computeOps(
            emptyList(), emptyList(), emptyMap(), now, fullRebuild = true,
            tasks = listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59))),
        )
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "TASK_DEADLINE" }.map { it.row }
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("taskdl|t1|2026-08-18", row.id)
        assertEquals(null, row.scheduleId)
        assertEquals(0, row.offsetMinutes)
        assertEquals("2026-08-18", row.occurrenceDate) // tanggal DEADLINE, bukan tanggal trigger
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), row.triggerAtMillis)
        assertEquals(0, row.snoozeCount)
    }

    @Test
    fun `deadline hari ini sebelum jam 2000 - row TASK_DEADLINE masuk desired`() {
        val now = wib("2026-08-17T10:00")
        val ops = planner.computeOps(
            emptyList(), emptyList(), emptyMap(), now, fullRebuild = true,
            tasks = listOf(task("t1", wibEpoch(2026, 8, 17, 23, 59))),
        )
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "TASK_DEADLINE" }.map { it.row }
        assertEquals(1, rows.size)
        assertEquals("taskdl|t1|2026-08-17", rows.single().id)
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), rows.single().triggerAtMillis)
    }

    @Test
    fun `R19 - now lewat jam 2000 di hari H-1 - row TASK_DEADLINE trigger masa lalu tidak masuk desired dan di-cancel`() {
        val now = wib("2026-08-17T20:30") // Senin 20:30; deadline t1 besok (18) tapi jam notif sudah lewat
        val stale = deadlineRow("taskdl|t1|2026-08-18", "2026-08-18", wibEpoch(2026, 8, 17, 20, 0))
        val ops = planner.computeOps(
            listOf(stale), emptyList(), emptyMap(), now, fullRebuild = true,
            tasks = listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59))),
        )
        // R19: trigger 20:00 sudah <= now → jangan masuk desired (alarm masa lalu tidak boleh menyala)
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.kind == "TASK_DEADLINE" })
        // pass cancel fullRebuild membersihkan row stale yang tersimpan
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == stale.id })
    }

    @Test
    fun `deadline besok sebelum jam 2000 - row yang sudah terpasang benar di-Keep`() {
        val now = wib("2026-08-17T10:00")
        val row = deadlineRow("taskdl|t1|2026-08-18", "2026-08-18", wibEpoch(2026, 8, 17, 20, 0))
        val ops = planner.computeOps(
            listOf(row), emptyList(), emptyMap(), now, fullRebuild = true,
            tasks = listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59))),
        )
        assertTrue(ops.any { it is AlarmOp.Keep })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.kind == "TASK_DEADLINE" })
        assertTrue(ops.none { it is AlarmOp.Cancel && it.row.id == row.id })
    }

    @Test
    fun `HARDENING - sync selesai setelah snooze - tidak double schedule, trigger snooze dipertahankan`() {
        val now = wib("2026-08-17T00:00") // Senin
        val s = sched()
        // rescheduleAll pertama (sync selesai)
        val ops1 = planner.computeOps(emptyList(), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true, tasks = emptyList())
        val base = (ops1.single { it is AlarmOp.Schedule && it.row.offsetMinutes == 120 } as AlarmOp.Schedule).row
        // user snooze: +3 menit, count 1 — di sela sync
        val snoozed = base.copy(triggerAtMillis = base.triggerAtMillis + 180_000L, snoozeCount = 1)
        // sync selesai → rescheduleAll lagi terhadap state ber-snooze
        val ops2 = planner.computeOps(listOf(snoozed), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true, tasks = emptyList())
        // invariant: snoozeCount tetap 1, trigger tetap nilai snooze, tidak ada double-schedule utk identity ini
        assertTrue(ops2.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == base.id })
        assertTrue(ops2.filterIsInstance<AlarmOp.Cancel>().none { it.row.id == base.id })
        assertTrue(ops2.any { it is AlarmOp.Keep })
    }

    @Test
    fun `HARDENING - jadwal diubah setelah alarm terpasang - alarm lama di-cancel, alarm baru terpasang`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        // sync membawa jadwal pindah: Senin 08:00 → Rabu 10:00
        val moved = sched(day = 3, start = "10:00")
        val ops = planner.computeOps(listOf(oldRow), listOf(moved), mapOf(3 to listOf(120)), now, fullRebuild = true, tasks = emptyList())
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        val newRows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(newRows.any { it.scheduleId == "s1" && it.occurrenceDate == "2026-08-19" })
        // guard receiver utk alarm lama: row identity tak ada → SkipSilent (diuji di ReceiverGuardTest)
    }

    @Test
    fun `jadwal dinonaktifkan - row lama di-cancel tanpa schedule baru`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        val ops = planner.computeOps(listOf(oldRow), listOf(sched(enabled = false)), mapOf(1 to listOf(120)), now, fullRebuild = true, tasks = emptyList())
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().all { it.row.kind == "TASK_REMINDER" })
    }

    @Test
    fun `reconcile idempotent - state benar tidak disentuh`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val ops1 = planner.computeOps(emptyList(), listOf(s), offsetsByDay, now, fullRebuild = true, tasks = emptyList())
        val installed = (ops1.first { it is AlarmOp.Schedule } as AlarmOp.Schedule).row
        // reconcile kedua: hanya 1 dari 6 row benar yang terpasang (ops1 = 3 class + 3 task slot)
        // → 1 Keep (row cocok), 5 Schedule (yang hilang), 0 Cancel
        val ops2 = planner.computeOps(listOf(installed), listOf(s), offsetsByDay, now, fullRebuild = false, tasks = emptyList())
        assertTrue(ops2.none { it is AlarmOp.Cancel })
        assertTrue(ops2.any { it is AlarmOp.Keep })
        assertEquals(5, ops2.filterIsInstance<AlarmOp.Schedule>().size)
        // reconcile menambahkan yang hilang
        val ops3 = planner.computeOps(emptyList(), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = false, tasks = emptyList())
        assertTrue(ops3.any { it is AlarmOp.Schedule })
    }

    @Test
    fun `HARDENING - exact restore - rescheduleAll kembali exact preserve sesi snooze`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val snoozed = ScheduledAlarmEntity(
            id = "class|s1|120|2026-08-17", kind = "CLASS_ALARM", scheduleId = "s1",
            offsetMinutes = 120, occurrenceDate = "2026-08-17",
            triggerAtMillis = 1_787_007_780_000L, // 18 Agu 06:03 WIB (snooze +3 mnt dari trigger 06:00; anchor .NET) — future vs now 17 Agu 00:00 WIB
            snoozeCount = 2,
        )
        val ops = planner.computeOps(listOf(snoozed), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true, tasks = emptyList())
        assertTrue(ops.any { it is AlarmOp.Keep })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == snoozed.id })
        // bila Keep dijalankan → scheduleRow(snoozed) memakai triggerAtMillis tersimpan (snooze), bukan base
        // (assert trigger bernilai snooze di AlarmSchedulerTest.restore exact)
    }
}
