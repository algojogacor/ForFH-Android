package com.aryariap.forfh.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rekap presensi per MK — mirror item jenis "presensi" dari GET /api/campus/info.
 * Server web mengirim data TERNORMALISASI (presensiToRecap): {code, name, tm, hadir, persen}.
 * Kolom tm/hadir/persen NULL saat server kirim null (angka tidak tersedia).
 */
@Entity(tableName = "presensi_recap", primaryKeys = ["kode", "nama"])
data class PresensiRecapEntity(
    val kode: String,
    val nama: String,
    val tm: Int?,    // total pertemuan (minggu)
    val hadir: Int?, // jumlah hadir
    val persen: Int?, // persentase hadir 0-100 (server atau hasil hitung web)
)

/**
 * Info kampus per jenis (status_mhs, peserta_mk, pembayaran, dosen_wali, masa_studi,
 * sks_aktif, hist_her, penyerahan_ktm, kalender_akademik, ...) — mirror campusData web.
 * dataJson = JSON array baris MENTAH UPPERCASE_SNAKE (libapp.so), disimpan verbatim
 * (pola web: campusData.dataJson + rendering label-value generik). Jenis "presensi"
 * TIDAK masuk tabel ini — dia dipetakan typed ke PresensiRecapEntity.
 */
@Entity(tableName = "kampus_info")
data class KampusInfoEntity(
    @PrimaryKey val jenis: String,
    val dataJson: String,  // JSON array baris mentah; "[]" saat kosong/null
    val updatedAt: String, // ISO-8601 dari server
)

/** Baris tunggal metadata sinkronisasi kampus (id selalu 1). */
@Entity(tableName = "kampus_meta")
data class KampusMetaEntity(
    @PrimaryKey val id: Int = 1,
    val connected: Boolean,
    val lastSyncAt: String?, // ISO-8601 | null
)

/** Hasil pemetaan satu envelope GET /api/campus/info — siap disimpan (DAO.saveSnapshot). */
data class KampusInfoSnapshot(
    val connected: Boolean,
    val lastSyncAt: String?,
    val presensi: List<PresensiRecapEntity>,
    val info: List<KampusInfoEntity>,
)
