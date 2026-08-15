package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SchedulesDao {
    @Query("SELECT * FROM schedules ORDER BY dayOfWeek, startTime")
    fun getAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    fun getAllOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE enabled = 1")
    fun getEnabledOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getByIdOnce(id: String): ScheduleEntity?

    @Query("DELETE FROM schedules")
    fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ScheduleEntity>)

    /** wipe-and-replace — HANYA tabel mirror, HANYA saat sync sukses (invariant spec). */
    @Transaction
    suspend fun replaceAll(items: List<ScheduleEntity>) {
        clearAll()
        insertAll(items)
    }
}
