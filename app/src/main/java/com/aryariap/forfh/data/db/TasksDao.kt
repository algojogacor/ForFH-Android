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

    @Query("SELECT * FROM tasks")
    fun getAllOnce(): List<TaskEntity>

    /** Reminder tugas: status != DONE, urut deadline terdekat (dueAt ASC NULLS LAST). */
    @Query("SELECT * FROM tasks WHERE status != 'DONE' ORDER BY (dueAt IS NULL), dueAt ASC")
    fun getActiveByDeadline(): List<TaskEntity>

    /** Dipanggil HANYA setelah PUT /api/tasks/{id} sukses (invariant: server sumber kebenaran). */
    @Query("UPDATE tasks SET status = :status, computedStatus = :computedStatus WHERE id = :id")
    fun updateStatus(id: String, status: String, computedStatus: String?)

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
