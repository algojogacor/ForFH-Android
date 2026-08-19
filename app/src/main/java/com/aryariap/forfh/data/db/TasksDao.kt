package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TasksDao {
    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'DONE' THEN 1 ELSE 0 END, (dueAt IS NULL), dueAt ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getByIdOnce(id: String): TaskEntity?

    @Query("SELECT * FROM tasks")
    fun getAllOnce(): List<TaskEntity>

    /** Reminder tugas: status != DONE, urut deadline terdekat (dueAt ASC NULLS LAST). */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY (dueAt IS NULL), dueAt ASC")
    fun getActiveByDeadline(): List<TaskEntity>

    /**
     * Kandidat notifikasi deadline H-1 (TaskDeadlinePlanner): tugas aktif (status != DONE) dengan
     * dueAt terparse dalam rentang [fromMillis, toMillis) — caller menghitung batas hari WIB
     * (today 00:00 .. lusa 00:00). Planner murni yang memutuskan H-1; query hanya mempersempit
     * kandidat supaya tidak menarik seluruh tabel. Batas ms (bukan epoch day) agar konversi WIB
     * tetap di Kotlin (ZonedDateTime), tidak tersembunyi di SQL.
     */
    @Query(
        "SELECT * FROM tasks WHERE status != 'DONE' AND dueAt IS NOT NULL " +
            "AND dueAt >= :fromMillis AND dueAt < :toMillis"
    )
    fun getDueTasksOnce(fromMillis: Long, toMillis: Long): List<TaskEntity>

    /**
     * Mark selesai optimistik (Task 10): status DONE + computedStatus NULL + syncState PENDING
     * SEKETIKA, sebelum PUT /api/tasks/{id} selesai — UI tidak menunggu network round-trip.
     * suspend → Room jalankan di query executor; NON-suspend di sini akan crash
     * "Cannot access database on the main thread" saat dipanggil dari viewModelScope (Main)
     * — insiden 2026-08-16 (markDone) — padahal update dipanggil dari UI, bukan context Default.
     */
    @Query("UPDATE tasks SET status = 'DONE', computedStatus = NULL, syncState = 'PENDING' WHERE id = :id")
    suspend fun updateMarked(id: String)

    /**
     * Batalkan selesai optimistik: status NOT_STARTED + computedStatus NULL + syncState PENDING.
     */
    @Query("UPDATE tasks SET status = 'NOT_STARTED', computedStatus = NULL, syncState = 'PENDING' WHERE id = :id")
    suspend fun updateUnmarked(id: String)

    /**
     * Hasil PUT markDone / unmarkDone (Task 10): syncState = SYNCED (sukses) / FAILED (gagal).
     * Status tugas tidak disentuh lagi — sudah DONE atau NOT_STARTED dari updateMarked/updateUnmarked.
     */
    @Query("UPDATE tasks SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String)

    @Query("DELETE FROM tasks")
    fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<TaskEntity>)

    @Transaction
    suspend fun replaceAll(items: List<TaskEntity>) {
        clearAll()
        insertAll(items)
    }
}
