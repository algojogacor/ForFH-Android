package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.TaskEntity

/**
 * Terapkan ulang mark selesai yang belum dikonfirmasi server ke daftar hasil wipe-and-replace
 * dari server (Task 10, ruling R25). Sync menggantikan tabel tasks dari server; tanpa ini,
 * tugas yang user tandai selesai saat PUT belum dikonfirmasi (syncState=PENDING) akan tertimpa
 * kembali menjadi "Belum".
 *
 * Semantik: HANYA id di pendingIds yang diterapkan ulang (status DONE + syncState PENDING).
 * Tugas lain (SYNCED/FAILED) ikut aturan server — FAILED tidak di-silent re-PUT saat sync,
 * retry-nya lewat ketukan chip di UI. Id pending yang SUDAH berstatus DONE di response server
 * dipertahankan apa adanya (server sudah mengonfirmasi — tidak perlu re-mark).
 *
 * Murni: tidak menyentuh DB/DataStore — pemanggil (SyncRepository) yang menulis hasilnya.
 */
fun applyPendingStatuses(tasks: List<TaskEntity>, pendingIds: Set<String>): List<TaskEntity> {
    if (pendingIds.isEmpty()) return tasks
    return tasks.map { t ->
        if (t.id in pendingIds && t.status != "DONE") {
            t.copy(
                status = "DONE",
                computedStatus = null,
                syncState = TaskEntity.SyncState.PENDING,
            )
        } else {
            t
        }
    }
}
