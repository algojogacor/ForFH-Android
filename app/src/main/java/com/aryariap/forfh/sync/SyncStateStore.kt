package com.aryariap.forfh.sync

/**
 * State sinkronisasi (implementasi: Preferences/DataStore) — dipakai SyncRepository dan
 * TugasViewModel (via TugasContainer). Pending mark selesai (Task 10): id tugas yang PUT
 * markDone-nya belum dikonfirmasi server, supaya wipe-and-replace saat sync tidak menimpa
 * "Selesai" lokal menjadi "Belum".
 */
interface SyncStateStore {
    suspend fun setLastSync(epochMillis: Long, status: String)
    suspend fun lastSyncAt(): Long
    suspend fun lastSyncStatus(): String

    /** Id tugas yang mark selesainya menunggu konfirmasi PUT (optimistic markDone, Task 10). */
    suspend fun pendingMarkDone(): Set<String>

    /** Ganti seluruh set pending (stringSet "pending_mark_done"). */
    suspend fun setPendingMarkDone(ids: Set<String>)

    /**
     * Tambah/hapus satu id secara ATOMIS dalam satu transaksi DataStore. Dipakai ViewModel:
     * baca-lalu-tulis dari dua coroutine (dua tugas ditekan berdekatan) bisa saling menimpa,
     * edit{} menjalankan transform terhadap preferences terkini sehingga tidak ada id yang hilang.
     */
    suspend fun addPendingMarkDone(id: String)
    suspend fun removePendingMarkDone(id: String)
}
