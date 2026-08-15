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
    private val offsets = listOf(180, 120, 60)

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
        val ops = planner.computeOps(emptyList(), listOf(sched()), offsets, wib("2026-08-15T10:00"), fullRebuild = true)
        // slot tugas "selalu ada" (test lain) → filter ke row CLASS_ALARM untuk per-offset
        val rows = ops.filterIsInstance<AlarmOp.Schedule>().filter { it.row.kind == "CLASS_ALARM" }.map { it.row }
        assertEquals(3, rows.size)
        assertEquals(setOf(180, 120, 60), rows.map { it.offsetMinutes }.toSet())
        assertEquals(setOf("class|s1|180|2026-08-17", "class|s1|120|2026-08-17", "class|s1|60|2026-08-17"), rows.map { it.id }.toSet())
    }

    @Test
    fun `slot tugas selalu ada - 3 slot one-shot`() {
        val ops = planner.computeOps(emptyList(), emptyList(), emptyList(), wib("2026-08-15T10:00"), fullRebuild = true)
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
        val ops1 = planner.computeOps(emptyList(), listOf(s), listOf(120), now, fullRebuild = true)
        val base = (ops1.single { it is AlarmOp.Schedule && it.row.offsetMinutes == 120 } as AlarmOp.Schedule).row
        // user snooze: +3 menit, count 1 — di sela sync
        val snoozed = base.copy(triggerAtMillis = base.triggerAtMillis + 180_000L, snoozeCount = 1)
        // sync selesai → rescheduleAll lagi terhadap state ber-snooze
        val ops2 = planner.computeOps(listOf(snoozed), listOf(s), listOf(120), now, fullRebuild = true)
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
        val ops = planner.computeOps(listOf(oldRow), listOf(moved), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        val newRows = ops.filterIsInstance<AlarmOp.Schedule>().map { it.row }
        assertTrue(newRows.any { it.scheduleId == "s1" && it.occurrenceDate == "2026-08-19" })
        // guard receiver utk alarm lama: row identity tak ada → SkipSilent (diuji di ReceiverGuardTest)
    }

    @Test
    fun `jadwal dinonaktifkan - row lama di-cancel tanpa schedule baru`() {
        val now = wib("2026-08-15T00:00")
        val oldRow = classRow("class|s1|120|2026-08-17", trigger = 1_750_000_000_000L)
        val ops = planner.computeOps(listOf(oldRow), listOf(sched(enabled = false)), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Cancel && it.row.id == oldRow.id })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().all { it.row.kind == "TASK_REMINDER" })
    }

    @Test
    fun `reconcile idempotent - state benar tidak disentuh`() {
        val now = wib("2026-08-17T00:00")
        val s = sched()
        val ops1 = planner.computeOps(emptyList(), listOf(s), offsets, now, fullRebuild = true)
        val installed = (ops1.first { it is AlarmOp.Schedule } as AlarmOp.Schedule).row
        // reconcile kedua: hanya 1 dari 6 row benar yang terpasang (ops1 = 3 class + 3 task slot)
        // → 1 Keep (row cocok), 5 Schedule (yang hilang), 0 Cancel
        val ops2 = planner.computeOps(listOf(installed), listOf(s), offsets, now, fullRebuild = false)
        assertTrue(ops2.none { it is AlarmOp.Cancel })
        assertTrue(ops2.any { it is AlarmOp.Keep })
        assertEquals(5, ops2.filterIsInstance<AlarmOp.Schedule>().size)
        // reconcile menambahkan yang hilang
        val ops3 = planner.computeOps(emptyList(), listOf(s), listOf(120), now, fullRebuild = false)
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
        val ops = planner.computeOps(listOf(snoozed), listOf(s), listOf(120), now, fullRebuild = true)
        assertTrue(ops.any { it is AlarmOp.Keep })
        assertTrue(ops.filterIsInstance<AlarmOp.Schedule>().none { it.row.id == snoozed.id })
        // bila Keep dijalankan → scheduleRow(snoozed) memakai triggerAtMillis tersimpan (snooze), bukan base
        // (assert trigger bernilai snooze di AlarmSchedulerTest.restore exact)
    }
}
