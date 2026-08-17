package com.aryariap.forfh.widget

import com.aryariap.forfh.data.db.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Helper murni widget tugas: filter dan format data untuk TasksWidget.
 * Tidak punya konteks Android, fully deterministic dari parameter.
 */

/**
 * Ambil tugas aktif berikutnya (bukan DONE), paling banyak [limit] buah.
 * DAO sudah mengurutkan dueAt asc nulls last; helper ini tahan input (filter DONE).
 */
fun nextTasks(tasks: List<TaskEntity>, limit: Int = 3): List<TaskEntity> =
    tasks.filter { it.status != "DONE" }.take(limit)

private val WIB: ZoneId = ZoneId.of("Asia/Jakarta")
private val syncTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Baris status sync untuk widget tugas.
 * - lastSyncAt = 0: "Belum pernah sinkron"
 * - lastSyncStatus "ok": "Sinkron HH:mm"
 * - selain itu: "Gagal sinkron HH:mm"
 */
fun syncStatusLine(lastSyncAt: Long, lastSyncStatus: String, nowMs: Long): String {
    if (lastSyncAt == 0L) return "Belum pernah sinkron"
    val time = Instant.ofEpochMilli(lastSyncAt)
        .atZone(WIB)
        .format(syncTimeFmt)
    return when (lastSyncStatus) {
        "ok" -> "Sinkron $time"
        else -> "Gagal sinkron $time"
    }
}
