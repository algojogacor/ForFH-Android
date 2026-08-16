package com.aryariap.forfh.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KampusInfoDao {
    @Query("SELECT * FROM presensi_recap ORDER BY nama COLLATE NOCASE, kode")
    fun getPresensiRecap(): Flow<List<PresensiRecapEntity>>

    @Query("SELECT * FROM kampus_info")
    fun getKampusInfo(): Flow<List<KampusInfoEntity>>

    @Query("SELECT * FROM kampus_meta WHERE id = 1")
    fun getMeta(): Flow<KampusMetaEntity?>

    @Query("SELECT * FROM kampus_meta WHERE id = 1")
    suspend fun getMetaOnce(): KampusMetaEntity?

    @Query("DELETE FROM presensi_recap")
    suspend fun clearPresensiRecap()

    @Query("DELETE FROM kampus_info")
    suspend fun clearKampusInfo()

    @Query("DELETE FROM kampus_meta")
    suspend fun clearKampusMeta()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresensiRecap(items: List<PresensiRecapEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKampusInfo(items: List<KampusInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: KampusMetaEntity)

    /**
     * Simpan snapshot GET /api/campus/info. Presensi wipe-and-replace (tabel mirror,
     * pola SchedulesDao.replaceAll). Info kampus di-upsert per jenis — jenis yang tidak
     * ikut respons (fetch-nya gagal di sisi web) MEMPERTAHANKAN data lama, sama seperti
     * perilaku web (toleran per jenis). connected=false → semua tabel dibersihkan
     * (metadata tetap ditulis connected=false supaya UI tahu belum terhubung).
     */
    @Transaction
    suspend fun saveSnapshot(snapshot: KampusInfoSnapshot) {
        insertMeta(
            KampusMetaEntity(
                id = 1,
                connected = snapshot.connected,
                lastSyncAt = snapshot.lastSyncAt,
            ),
        )
        if (snapshot.connected) {
            clearPresensiRecap()
            insertPresensiRecap(snapshot.presensi)
            insertKampusInfo(snapshot.info)
        } else {
            clearPresensiRecap()
            clearKampusInfo()
        }
    }
}
