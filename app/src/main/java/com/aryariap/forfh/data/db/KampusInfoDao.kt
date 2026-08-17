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
     * Simpan snapshot GET /api/campus/info. Kedua tabel kampus WIPE-AND-REPLACE (ruling R23,
     * terverifikasi dari route.ts web): route mengembalikan SEMUA baris campusData setiap
     * kali → jenis yang absen dari respons memang sudah tidak ada di sisi web → dihapus
     * (delete-if-absent), bukan dipertahankan. connected=false → data dibersihkan semua
     * (metadata tetap ditulis connected=false supaya UI tahu belum terhubung, R23: layar
     * menampilkan state putus, tidak pernah kartu basi).
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
            clearKampusInfo() // R23: wipe-and-replace — delete jenis yang tidak ikut respons
            insertKampusInfo(snapshot.info)
        } else {
            clearPresensiRecap()
            clearKampusInfo()
        }
    }
}
