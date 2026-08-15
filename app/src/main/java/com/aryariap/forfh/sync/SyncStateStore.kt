package com.aryariap.forfh.sync

interface SyncStateStore {
    suspend fun setLastSync(epochMillis: Long, status: String)
    suspend fun lastSyncAt(): Long
    suspend fun lastSyncStatus(): String
}
