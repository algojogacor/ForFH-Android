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
 * Ketergantungan layar Info — dipenuhi AppContainer; test memakai fake (pola NextUpContainer).
 * Hanya yang benar-benar dipakai ViewModel yang diekspos: DAO kampus, status sync (prefs),
 * sinyal "sync sedang berjalan" (WorkManager), dan aksi enqueue sync.
 */
interface InfoContainer {
    val kampusInfoDao: KampusInfoDao
    /** Status sync terakhir "ok"/"error"/"" (DataStore, sama seperti layar lain). */
    val lastSyncStatus: Flow<String>
    /** Waktu sync terakhir (epoch ms). */
    val lastSyncAt: Flow<Long>
    /** true saat worker sync berjalan/menunggu (produksi: unique work "sync_once"). */
    val syncRunning: Flow<Boolean>
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
    val rows: InfoRows,
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
    val lastSyncAt: Long = 0L,
    /** true saat worker sync berjalan/menunggu (WorkManager unique work "sync_once"). */
    val loading: Boolean = false,
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
            container.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) }
        }
        viewModelScope.launch {
            container.syncRunning.collect { running -> _state.value = _state.value.copy(loading = running) }
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
        rows = InfoFormat.kampusRows(dataJson),
        updatedAt = InfoFormat.formatUpdatedAt(updatedAt),
    )
}
