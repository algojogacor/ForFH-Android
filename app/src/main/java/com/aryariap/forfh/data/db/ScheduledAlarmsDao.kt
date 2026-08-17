package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAlarmsDao {
    @Query("SELECT * FROM scheduled_alarms")
    fun getAll(): Flow<List<ScheduledAlarmEntity>>

    @Query("SELECT * FROM scheduled_alarms")
    fun getAllOnce(): List<ScheduledAlarmEntity>

    @Query("SELECT * FROM scheduled_alarms WHERE id = :id")
    fun getByIdOnce(id: String): ScheduledAlarmEntity?

    /** Alarm kuliah berikutnya (kartu "Berikutnya" V1.1): CLASS_ALARM dengan trigger masa depan terdekat. */
    @Query(
        "SELECT * FROM scheduled_alarms WHERE kind = 'CLASS_ALARM' AND triggerAtMillis > :nowMs " +
            "ORDER BY triggerAtMillis ASC LIMIT 1"
    )
    fun nextClassAlarmOnce(nowMs: Long): ScheduledAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(row: ScheduledAlarmEntity)

    @Query("DELETE FROM scheduled_alarms WHERE id = :id")
    fun deleteById(id: String)

    @Query("DELETE FROM scheduled_alarms")
    fun clearAll()
}
