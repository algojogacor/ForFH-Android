package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskDeadlineTextTest {

    private fun task(title: String, courseName: String?) = TaskEntity(
        id = "t1", courseId = null, courseName = courseName, courseCode = null, title = title,
        description = null, dueAt = 1L, status = "NOT_STARTED", computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    private val today = LocalDate.of(2026, 8, 17)

    @Test
    fun `deadline besok - hint deadline besok`() {
        assertEquals(
            "📚 Hukum Pidana: Makalah Hukum — deadline besok",
            TaskDeadlineText.build(task("Makalah Hukum", "Hukum Pidana"), LocalDate.of(2026, 8, 18), today),
        )
    }

    @Test
    fun `deadline hari ini - hint deadline hari ini`() {
        assertEquals(
            "📚 Hukum Pidana: Makalah Hukum — deadline hari ini",
            TaskDeadlineText.build(task("Makalah Hukum", "Hukum Pidana"), today, today),
        )
    }

    @Test
    fun `tanpa course - title saja`() {
        assertEquals(
            "📚 Makalah Hukum — deadline besok",
            TaskDeadlineText.build(task("Makalah Hukum", null), LocalDate.of(2026, 8, 18), today),
        )
    }

    @Test
    fun `bukan hari ini atau besok - tanggal ditampilkan`() {
        assertEquals(
            "📚 Makalah Hukum — deadline 20 Agu",
            TaskDeadlineText.build(task("Makalah Hukum", null), LocalDate.of(2026, 8, 20), today),
        )
    }
}
