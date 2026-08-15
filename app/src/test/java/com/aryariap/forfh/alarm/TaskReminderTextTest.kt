package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderTextTest {

    private fun task(id: String, title: String, dueAt: Long?) = TaskEntity(
        id = id, courseId = null, courseName = null, courseCode = null, title = title,
        description = null, dueAt = dueAt, status = "NOT_STARTED", computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    @Test
    fun `nol tugas - hanya slot 09 yang menampilkan perayaan`() {
        assertEquals("🎉 Tidak ada tugas hari ini — selamat beraktivitas!", TaskReminderText.build(emptyList(), 9))
        assertNull(TaskReminderText.build(emptyList(), 15))
        assertNull(TaskReminderText.build(emptyList(), 20))
    }

    @Test
    fun `satu tugas`() {
        assertEquals("📚 1 tugas belum selesai: Makalah Hukum", TaskReminderText.build(listOf(task("t1", "Makalah Hukum", null)), 9))
    }

    @Test
    fun `dua tugas`() {
        assertEquals(
            "📚 2 tugas belum selesai: Makalah Hukum, Kuis Bab 2",
            TaskReminderText.build(listOf(task("t1", "Makalah Hukum", 1L), task("t2", "Kuis Bab 2", 2L)), 15),
        )
    }

    @Test
    fun `tiga tugas atau lebih - format K lagi`() {
        val tasks = listOf(
            task("t1", "A", 1L), task("t2", "B", 2L), task("t3", "C", 3L), task("t4", "D", 4L),
        )
        assertEquals("📚 4 tugas belum selesai: A, B, +2 lagi", TaskReminderText.build(tasks, 9))
    }

    @Test
    fun `urutan deadline terdekat - dueAt null paling akhir`() {
        val late = task("late", "Tanpa deadline", null)
        val soon = task("soon", "Deadline besok", 1L)
        assertEquals("📚 2 tugas belum selesai: Deadline besok, Tanpa deadline", TaskReminderText.build(listOf(late, soon), 9))
    }
}
