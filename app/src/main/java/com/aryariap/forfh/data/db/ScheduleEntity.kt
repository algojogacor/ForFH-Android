package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Tabel mirror GET /api/schedules — wipe-and-replace saat sync sukses. */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val courseName: String,
    val courseCode: String?,
    val courseColor: String,   // default "#3b82f6"
    val lecturer: String?,
    val credits: Int,          // default 2
    val dayOfWeek: Int,        // 0=Sunday .. 6=Saturday (konvensi API)
    val startTime: String,     // "HH:MM"
    val endTime: String,       // "HH:MM"
    val room: String?,
    val onlineUrl: String?,
    val enabled: Boolean,
)
