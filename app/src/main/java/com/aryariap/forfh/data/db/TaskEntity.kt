package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabel mirror GET /api/tasks — wipe-and-replace saat sync sukses.
 * Kolom mengikuti spec §7; courseId & courseColor ditambah untuk filter/warna di UI (REQ-18).
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val courseId: String?,
    val courseName: String?,
    val courseCode: String?,
    val title: String,
    val description: String?,
    val dueAt: Long?,                    // epoch ms | null
    val status: String,                  // NOT_STARTED|IN_PROGRESS|REVISION|DONE|OVERDUE
    val computedStatus: String?,         // "OVERDUE" | null
    val priority: String,                // low|medium|high|urgent
    val courseColor: String?,            // dari course row API, utk badge warna
    val subtasksJson: String?,           // JSON encode List<SubtaskDto> — spec §7: detail tugas WAJIB tampilkan subtasks
)
