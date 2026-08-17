package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * applyPendingStatuses — logika murni re-apply mark selesai yang belum dikonfirmasi server
 * (Task 10). Sync REPLACES tabel tasks dari server; tanpa ini, tugas yang user tandai selesai
 * (PUT masih berjalan / belum konfirmasi, syncState=PENDING) akan kembali tampil "Belum".
 *
 * Semantik (ruling Task 10): HANYA yang PENDING yang diterapkan ulang; yang SYNCED/FAILED
 * ikut aturan server (retry FAILED via ketuk UI, bukan di-silent re-PUT saat sync).
 * Pengecualian: id pending yang statusnya SUDAH DONE di response server dipertahankan apa
 * adanya — server sudah mengonfirmasi, tidak perlu re-mark.
 */
class PendingStatusTest {

    private fun task(id: String, status: String = "NOT_STARTED", syncState: String = TaskEntity.SyncState.SYNCED) =
        TaskEntity(
            id = id, courseId = null, courseName = null, courseCode = null, title = "Tugas $id",
            description = null, dueAt = null, status = status, computedStatus = null,
            priority = "medium", courseColor = null, subtasksJson = null, syncState = syncState,
        )

    @Test
    fun `pending id - status jadi DONE dan syncState PENDING`() {
        val out = applyPendingStatuses(listOf(task("t1")), setOf("t1"))
        assertEquals("DONE", out.single().status)
        assertEquals(null, out.single().computedStatus)
        assertEquals(TaskEntity.SyncState.PENDING, out.single().syncState)
    }

    @Test
    fun `non-pending - status server dan SYNCED dipertahankan`() {
        val out = applyPendingStatuses(listOf(task("t1")), setOf("t2"))
        assertEquals("NOT_STARTED", out.single().status)
        assertEquals(TaskEntity.SyncState.SYNCED, out.single().syncState)
    }

    @Test
    fun `pending set kosong - daftar dikembalikan tanpa perubahan`() {
        val tasks = listOf(task("t1"), task("t2"))
        val out = applyPendingStatuses(tasks, emptySet())
        assertSame(tasks, out) // referensi sama — tidak ada kerja dilakukan
    }

    @Test
    fun `pending id yang sudah DONE di server - status server dipertahankan`() {
        val out = applyPendingStatuses(listOf(task("t1", status = "DONE")), setOf("t1"))
        assertEquals("DONE", out.single().status)
        assertEquals(TaskEntity.SyncState.SYNCED, out.single().syncState) // sudah dikonfirmasi server
    }

    @Test
    fun `campuran pending dan non-pending - hanya pending yang diubah`() {
        val out = applyPendingStatuses(
            listOf(task("t1"), task("t2", status = "IN_PROGRESS")),
            setOf("t1"),
        )
        assertEquals("DONE", out[0].status)
        assertEquals(TaskEntity.SyncState.PENDING, out[0].syncState)
        assertEquals("IN_PROGRESS", out[1].status)
        assertEquals(TaskEntity.SyncState.SYNCED, out[1].syncState)
    }
}
