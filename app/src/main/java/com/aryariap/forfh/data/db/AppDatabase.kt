package com.aryariap.forfh.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScheduleEntity::class, TaskEntity::class, ScheduledAlarmEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schedulesDao(): SchedulesDao
    abstract fun tasksDao(): TasksDao
    abstract fun scheduledAlarmsDao(): ScheduledAlarmsDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "forfh.db")
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
