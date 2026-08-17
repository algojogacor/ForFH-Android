package com.aryariap.forfh.widget

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TDD: nextTasks dan syncStatusLine. Pure JVM tests (tanpa Robolectric, tanpa Android).
 */
class NextTasksTest {

    // ---- nextTasks ----

    @Test
    fun `nextTasks DONE dibuang`() {
        val tasks = listOf(
            task("1", "Tugas A", "DONE"),
            task("2", "Tugas B", "NOT_STARTED"),
        )
        assertEquals(listOf("2"), nextTasks(tasks).map { it.id })
    }

    @Test
    fun `nextTasks limit 3`() {
        val tasks = (1..5).map { i -> task("$i", "Tugas $i", "NOT_STARTED") }
        assertEquals(listOf("1", "2", "3"), nextTasks(tasks).map { it.id })
    }

    @Test
    fun `nextTasks urutan terjaga`() {
        val tasks = listOf(
            task("3", "Tugas C", "NOT_STARTED"),
            task("1", "Tugas A", "NOT_STARTED"),
            task("2", "Tugas B", "NOT_STARTED"),
        )
        // Urutan dari DAO: dueAt asc nulls last — di test ini urut sudah dari input
        assertEquals(listOf("3", "1", "2"), nextTasks(tasks).map { it.id })
    }

    @Test
    fun `nextTasks kosong jika semua DONE`() {
        val tasks = listOf(
            task("1", "Tugas A", "DONE"),
            task("2", "Tugas B", "DONE"),
        )
        assertEquals(emptyList<String>(), nextTasks(tasks).map { it.id })
    }

    // ---- syncStatusLine ----

    // "Sinkron 14:02" sukses (status "ok" / "SYNCED")
    @Test
    fun `syncStatusLine sukses`() {
        // 2026-08-17 14:02:00 WIB = epoch ms
        val epoch = java.time.Instant.parse("2026-08-17T07:02:00Z").toEpochMilli()
        val result = syncStatusLine(lastSyncAt = epoch, lastSyncStatus = "ok", nowMs = 0L)
        assertEquals("Sinkron 14:02", result)
    }

    // "Gagal sinkron 14:02" gagal
    @Test
    fun `syncStatusLine gagal`() {
        val epoch = java.time.Instant.parse("2026-08-17T07:05:00Z").toEpochMilli()
        val result = syncStatusLine(lastSyncAt = epoch, lastSyncStatus = "error", nowMs = 0L)
        assertEquals("Gagal sinkron 14:05", result)
    }

    // "Belum pernah sinkron" jika lastSyncAt = 0
    @Test
    fun `syncStatusLine belum pernah`() {
        assertEquals("Belum pernah sinkron", syncStatusLine(lastSyncAt = 0L, lastSyncStatus = "", nowMs = 0L))
    }

    private fun task(id: String, title: String, status: String): TaskEntity = TaskEntity(
        id = id,
        courseId = null,
        courseName = "Course",
        courseCode = null,
        title = title,
        description = null,
        dueAt = null,
        status = status,
        computedStatus = null,
        priority = "medium",
        courseColor = null,
        subtasksJson = null,
    )
}
