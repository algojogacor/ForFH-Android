package com.aryariap.forfh.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.data.db.KampusInfoDao
import com.aryariap.forfh.data.db.KampusInfoEntity
import com.aryariap.forfh.data.db.PresensiRecapEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Aktivitas worker sync (unique work "sync_once"). Layar Info membedakan QUEUED
 * (ENQUEUED — menunggu jaringan/constraint, bisa menunggu tanpa batas) dari RUNNING
 * (benar-benar berjalan): yang pertama adalah banner kecil, bukan spinner layar penuh.
 */
enum class SyncActivity { IDLE, QUEUED, RUNNING }

/**
 * Ketergantungan layar Info — dipenuhi AppContainer; test memakai fake (pola NextUpContainer).
 * Hanya yang benar-benar dipakai ViewModel yang diekspos: DAO kampus, status sync terakhir
 * (prefs, untuk flag error), aktivitas worker sync (WorkManager), dan aksi enqueue sync.
 */
interface InfoContainer {
    val kampusInfoDao: KampusInfoDao
    /** Status sync terakhir "ok"/"error"/"" (DataStore, sama seperti layar lain). */
    val lastSyncStatus: Flow<String>
    /** Aktivitas worker sync: QUEUED = menunggu jaringan, RUNNING = berjalan. */
    val syncActivity: Flow<SyncActivity>
    /** Sinkron sekarang: enqueue one-shot (pola PengaturanViewModel.syncNow). */
    val enqueueSync: () -> Unit
}

data class PresensiRow(
    val kode: String,
    val nama: String,
    val tm: Int?,
    val hadir: Int?,
    val persen: Int?,
)

data class InfoCard(
    val jenis: String,
    val title: String,
    /** Model tampil per jenis (kartu identitas, daftar MK, HER, dst.) — bukan dump mentah. */
    val model: InfoCardModel,
    val updatedAt: String?,
)

data class InfoUiState(
    val presensi: List<PresensiRow> = emptyList(),
    val cards: List<InfoCard> = emptyList(),
    /** null = belum pernah sinkron kampus; false = akun putus (data sudah dibersihkan R23). */
    val connected: Boolean? = null,
    /** lastSyncAt kampus dari server (ISO-8601, null saat belum pernah). */
    val kampusLastSyncAt: String? = null,
    val lastSyncStatus: String = "",
    /**
     * false = emisi Room pertama belum datang (meta belum terbaca). Guard frame pertama:
     * sebelum meta ter-emisi, null belum bisa dibedakan "belum pernah sync" vs "belum
     * terbaca" — render state terminal (kosong/putus) hanya setelah loaded (fix review).
     */
    val loaded: Boolean = false,
    /** Aktivitas worker sync (IDLE = tidak ada work yang berjalan/menunggu). */
    val syncActivity: SyncActivity = SyncActivity.IDLE,
    /** lastSyncStatus == "error" — sync terakhir gagal. */
    val error: Boolean = false,
)

class InfoViewModel(
    private val container: InfoContainer,
    /** Dispatcher untuk parse dataJson (bisa berat) — test meng-inject test dispatcher. */
    private val background: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(InfoUiState())
    val state: StateFlow<InfoUiState> = _state

    init {
        // Room Flow → state (DAO Flow aman dikoleksi di Main; parse + map dipindah ke
        // background supaya dataJson besar (hist_her/pembayaran) tidak menyumbat main thread).
        viewModelScope.launch {
            combine(
                container.kampusInfoDao.getPresensiRecap(),
                container.kampusInfoDao.getKampusInfo(),
                container.kampusInfoDao.getMeta(),
            ) { presensi, info, meta ->
                val (rows, cards) = withContext(background) {
                    presensi.map { it.toRow() } to info.map { it.toCard() }
                }
                _state.value = _state.value.copy(
                    loaded = true,
                    presensi = rows,
                    cards = cards,
                    connected = meta?.connected,
                    kampusLastSyncAt = meta?.lastSyncAt,
                )
            }.collect { }
        }
        viewModelScope.launch {
            container.lastSyncStatus.collect { s ->
                _state.value = _state.value.copy(lastSyncStatus = s, error = s == "error")
            }
        }
        viewModelScope.launch {
            container.syncActivity.collect { a -> _state.value = _state.value.copy(syncActivity = a) }
        }
    }

    /** Sinkron sekarang — fire-and-forget via WorkManager (pola PengaturanViewModel.syncNow). */
    fun syncNow() {
        container.enqueueSync()
    }

    private fun PresensiRecapEntity.toRow() = PresensiRow(kode, nama, tm, hadir, persen)

    private fun KampusInfoEntity.toCard() = InfoCard(
        jenis = jenis,
        title = InfoFormat.jenisTitle(jenis),
        model = InfoCardModels.buildInfoCardModel(jenis, dataJson),
        updatedAt = InfoFormat.formatUpdatedAt(updatedAt),
    )
}
