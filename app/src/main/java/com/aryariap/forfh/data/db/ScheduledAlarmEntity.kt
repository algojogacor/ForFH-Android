package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * State alarm eksplisit — identity deterministic:
 *   "class|scheduleId|offsetMinutes|occurrenceDate"  (kuliah)
 *   "task|slot|date"                                  (tugas, slot = "09"|"15"|"20")
 * TIDAK pernah di-wipe; perubahannya hanya lewat AlarmRescheduler.
 */
@Entity(tableName = "scheduled_alarms")
data class ScheduledAlarmEntity(
    @PrimaryKey val id: String,
    val kind: String,              // "CLASS_ALARM" | "TASK_REMINDER"
    val scheduleId: String?,       // null utk task slot
    val offsetMinutes: Int,        // 0 utk task slot
    val occurrenceDate: String,    // "2026-08-17" (LocalDate WIB)
    val triggerAtMillis: Long,     // berubah saat snooze
    val snoozeCount: Int,          // reset saat row baru
)
