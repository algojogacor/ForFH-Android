package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskDeadlinePlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = TaskDeadlinePlanner(zone)

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    private fun task(id: String, dueAt: Long?, status: String = "NOT_STARTED") = TaskEntity(
        id = id, courseId = null, courseName = null, courseCode = null,
        title = "Tugas $id", description = null,
        dueAt = dueAt, status = status, computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    // now tetap: Senin 2026-08-17 10:00 WIB → today = 08-17, tomorrow = 08-18
    private val now = wib("2026-08-17T10:00")

    private fun assertNoRows(tasks: List<TaskEntity>) {
        assertTrue("tidak boleh ada row TASK_DEADLINE", planner.computeTasks(tasks, now).isEmpty())
    }

    @Test
    fun `deadline besok - row H-1 trigger hari ini jam 2000 WIB`() {
        val rows = planner.computeTasks(listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59))), now)
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("taskdl|t1|2026-08-18", row.id)
        assertEquals("TASK_DEADLINE", row.kind)
        assertEquals(null, row.scheduleId)
        assertEquals(0, row.offsetMinutes)
        assertEquals("2026-08-18", row.occurrenceDate) // tanggal DEADLINE, bukan tanggal trigger
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), row.triggerAtMillis) // hari ini 20:00 WIB
        assertEquals(0, row.snoozeCount)
    }

    @Test
    fun `deadline hari ini sebelum jam 2000 - row trigger hari ini 2000 WIB`() {
        val rows = planner.computeTasks(listOf(task("t1", wibEpoch(2026, 8, 17, 23, 59))), now)
        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("taskdl|t1|2026-08-17", row.id)
        assertEquals("2026-08-17", row.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), row.triggerAtMillis)
    }

    @Test
    fun `deadline lebih dari 1 hari - TIDAK ada row`() {
        assertNoRows(listOf(task("t1", wibEpoch(2026, 8, 19, 23, 59))))
    }

    @Test
    fun `deadline sudah lewat kemarin - TIDAK ada row`() {
        assertNoRows(listOf(task("t1", wibEpoch(2026, 8, 16, 23, 59))))
    }

    @Test
    fun `tanpa due date terparse - skip`() {
        assertNoRows(listOf(task("t1", dueAt = null)))
    }

    @Test
    fun `status DONE - skip walau deadline besok`() {
        assertNoRows(listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59), status = "DONE")))
    }

    @Test
    fun `deadline hari ini tapi now sudah lewat jam 2000 - skip`() {
        val rows = planner.computeTasks(
            listOf(task("t1", wibEpoch(2026, 8, 17, 23, 59))),
            wib("2026-08-17T20:30"),
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `deadline hari ini dan now pas 2000 - skip (batas sama dianggap lewat)`() {
        val rows = planner.computeTasks(
            listOf(task("t1", wibEpoch(2026, 8, 17, 23, 59))),
            wib("2026-08-17T20:00"),
        )
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `deadline hari ini sudah lewat jamnya tapi sebelum 2000 - tetap row jam 2000 (semantik literal spec)`() {
        // Spec Task 5: cabang "deadline hari ini" hanya meng-gate now vs 20:00, bukan jam deadline vs now.
        val rows = planner.computeTasks(
            listOf(task("t1", wibEpoch(2026, 8, 17, 9, 0))), // deadline 09:00, now 10:00
            now,
        )
        assertEquals(1, rows.size)
        assertEquals(wibEpoch(2026, 8, 17, 20, 0), rows.single().triggerAtMillis)
    }

    @Test
    fun `campuran - hanya tugas H-1 yang dapat row`() {
        val rows = planner.computeTasks(
            listOf(
                task("h1", wibEpoch(2026, 8, 18, 23, 59)),
                task("done", wibEpoch(2026, 8, 18, 23, 59), status = "DONE"),
                task("nul", null),
                task("jauh", wibEpoch(2026, 8, 30, 23, 59)),
            ),
            now,
        )
        assertEquals(listOf("taskdl|h1|2026-08-18"), rows.map { it.id })
    }

    @Test
    fun `beberapa tugas deadline sama - row terpisah tiap tugas`() {
        val rows = planner.computeTasks(
            listOf(task("t1", wibEpoch(2026, 8, 18, 23, 59)), task("t2", wibEpoch(2026, 8, 18, 9, 0))),
            now,
        )
        assertEquals(2, rows.size)
        assertEquals(setOf("taskdl|t1|2026-08-18", "taskdl|t2|2026-08-18"), rows.map { it.id }.toSet())
        assertTrue(rows.all { it.triggerAtMillis == wibEpoch(2026, 8, 17, 20, 0) })
    }

    @Test
    fun `identity helper - taskdl|taskId|yyyy-MM-dd`() {
        assertEquals("taskdl|t1|2026-08-18", TaskDeadlinePlanner.taskDeadlineIdentity("t1", LocalDate.of(2026, 8, 18)))
    }
}
