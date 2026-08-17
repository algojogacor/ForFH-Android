package com.aryariap.forfh.data.db

import androidx.room.ColumnInfo
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
    /**
     * Keadaan sinkron mark selesai (Task 10): PENDING (PUT sedang berjalan / belum dikonfirmasi
     * server), SYNCED (server sudah konfirmasi), FAILED (PUT gagal — retry via ketuk chip di UI).
     * Default 'SYNCED' (migrasi V2→V3, ruling R24: baris lama dianggap tersinkron; tidak ada
     * yang pending sebelum fitur ini ada).
     */
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncState: String = "SYNCED",
) {
    /** Nilai kolom syncState — konstanta (bukan enum) mengikuti gaya kolom lain yang String. */
    object SyncState {
        const val PENDING = "PENDING"
        const val SYNCED = "SYNCED"
        const val FAILED = "FAILED"
    }

    companion object
}
