package com.aryariap.forfh.network

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `schedule dto ke entity - enabled int ke boolean`() {
        val dto = ScheduleDto(
            id = "s1", courseId = "c1", courseName = "Hukum", courseCode = null,
            courseColor = "", lecturer = null, credits = 2, dayOfWeek = 1,
            startTime = "08:00", endTime = "09:40", room = null, onlineUrl = null, enabled = 1,
        )
        val e: ScheduleEntity = dto.toEntity()
        assertTrue(e.enabled)
        assertEquals("#3b82f6", e.courseColor) // blank → default
    }

    @Test
    fun `task dto ke entity - computedStatus dari server, dueAt epoch`() {
        val dto = TaskDto(
            id = "t1", userId = "u1", courseId = "c1", title = "Makalah", description = null,
            type = "assignment", dueAt = "2026-08-20T03:00:00.000Z", internalTargetAt = null,
            priority = "high", estimatedMinutes = 60, status = "NOT_STARTED", progress = 0,
            source = "manual", completedAt = null, deletedAt = null, version = 1,
            externalId = null, createdAt = "2026-08-01T03:00:00.000Z",
            updatedAt = "2026-08-01T03:00:00.000Z", computedStatus = "OVERDUE",
            course = null,
            subtasks = listOf(
                SubtaskDto(
                    id = "st1", userId = "u1", taskId = "t1", title = "Bab 1",
                    createdAt = "2026-08-01T03:00:00.000Z", updatedAt = "2026-08-01T03:00:00.000Z",
                ),
            ),
        )
        val e: TaskEntity = dto.toEntity(nowMs = 1_000_000_000_000L)
        assertEquals("OVERDUE", e.computedStatus)
        assertEquals(1_787_194_800_000L, e.dueAt) // "2026-08-20T03:00:00.000Z" = 1_787_194_800_000 (terverifikasi .NET)
        // spec §7: subtasks tidak boleh hilang — disimpan JSON utk detail tugas
        assertNotNull(e.subtasksJson)
        assertTrue(e.subtasksJson!!.contains("Bab 1"))
    }

    @Test
    fun `computedStatus dihitung ulang bila server tak kirim - overdue hanya bila lewat`() {
        assertNull(computeComputedStatus("NOT_STARTED", "2026-08-20T03:00:00.000Z", nowMs = 1_786_000_000_000L))
        assertEquals("OVERDUE", computeComputedStatus("NOT_STARTED", "2026-08-20T03:00:00.000Z", nowMs = 1_788_000_000_000L))
        assertNull(computeComputedStatus("DONE", "2026-08-01T03:00:00.000Z", nowMs = 1_788_000_000_000L))
        assertNull(computeComputedStatus("NOT_STARTED", null, nowMs = 1_788_000_000_000L))
    }
}
