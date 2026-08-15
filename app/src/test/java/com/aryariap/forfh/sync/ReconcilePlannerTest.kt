package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
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

    @Test
    fun `rescheduleAll membangun row untuk tiap offset aktif`() {
        val ops = planner.computeOps(emptyList(), listOf(sched()), offsetsByDay, wib("2026-08-15T10:00"), fullRebuild = true)
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
        )
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldSecondRow.id })
        val classRows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(1, classRows.size)
        assertEquals("s1", classRows.single().scheduleId)
    }

    @Test
    fun `per hari - hari tanpa daftar offset tidak punya alarm kelas`() {
        val ops = planner.computeOps(emptyList(), listOf(sched(day = 2)), emptyMap(), wib("2026-08-15T10:00"), fullRebuild = true)
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(rows.none { it.kind == "CLASS_ALARM" })
        assertEquals(3, rows.size) // hanya slot tugas
    }

    @Test
    fun `slot tugas selalu ada - 3 slot one-shot`() {
        val ops = planner.computeOps(emptyList(), emptyList(), emptyMap(), wib("2026-08-15T10:00"), fullRebuild = true)
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertEquals(3, rows.size)
        assertTrue(rows.all { it.kind == "TASK_REMINDER" })
        assertEquals(setOf("task|09|2026-08-16", "task|15|2026-08-15", "task|20|2026-08-15"), rows.map { it.id }.toSet()) // now 10:00 → slot 09 lewat → besok
    }

    @Test
    fun `HARDENING - sync selesai setelah snooze - tidak double schedule, trigger snooze dipertahankan`() {
        val now = wib("2026-08-17T00:00") // Senin
        val s = sched()
        // rescheduleAll pertama (sync selesai)
        val ops1 = planner.computeOps(emptyList(), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true)
        val base = (ops1.single { it is AlarmOp.Schedule && it.row.offsetMinutes == 120 } as AlarmOp.Schedule).row
        // user snooze: +3 menit, count 1 — di sela sync
        val snoozed = base.copy(triggerAtMillis = base.triggerAtMillis + 180_000L, snoozeCount = 1)
        // sync selesai → rescheduleAll lagi terhadap state ber-snooze
        val ops2 = planner.computeOps(listOf(snoozed), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true)
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
        val ops = planner.computeOps(listOf(oldRow), listOf(moved), mapOf(3 to listOf(120)), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        val newRows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(newRows.any { it.scheduleId == "s1" && it.occurrenceDate == "2026-08-19" })
        // guard receiver utk alarm lama: row identity tak ada → SkipSilent (diuji di ReceiverGuardTest)
    }

    @Test
    fun `jadwal dinonaktifkan - row lama di-cancel tanpa schedule baru`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        val ops = planner.computeOps(listOf(oldRow), listOf(sched(enabled = false)), mapOf(1 to listOf(120)), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().all { it.row.kind == "TASK_REMINDER" })
    }

    @Test
    fun `reconcile idempotent - state benar tidak disentuh`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val ops1 = planner.computeOps(emptyList(), listOf(s), offsetsByDay, now, fullRebuild = true)
        val installed = (ops1.first { it is AlarmOp.Schedule } as AlarmOp.Schedule).row
        // reconcile kedua: hanya 1 dari 6 row benar yang terpasang (ops1 = 3 class + 3 task slot)
        // → 1 Keep (row cocok), 5 Schedule (yang hilang), 0 Cancel
        val ops2 = planner.computeOps(listOf(installed), listOf(s), offsetsByDay, now, fullRebuild = false)
        assertTrue(ops2.none { it is AlarmOp.Cancel })
        assertTrue(ops2.any { it is AlarmOp.Keep })
        assertEquals(5, ops2.filterIsInstance<AlarmOp.Schedule>().size)
        // reconcile menambahkan yang hilang
        val ops3 = planner.computeOps(emptyList(), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = false)
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
        val ops = planner.computeOps(listOf(snoozed), listOf(s), mapOf(1 to listOf(120)), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Keep })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == snoozed.id })
        // bila Keep dijalankan → scheduleRow(snoozed) memakai triggerAtMillis tersimpan (snooze), bukan base
        // (assert trigger bernilai snooze di AlarmSchedulerTest.restore exact)
    }
}
