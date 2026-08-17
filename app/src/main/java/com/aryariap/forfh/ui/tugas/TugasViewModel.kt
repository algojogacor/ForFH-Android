package com.aryariap.forfh.ui.tugas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.data.db.TaskEntity
import com.aryariap.forfh.data.db.TasksDao
import com.aryariap.forfh.network.ForfhApiService
import com.aryariap.forfh.network.MarkDoneRequest
import com.aryariap.forfh.sync.SyncStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.ZoneId

/**
 * Ketergantungan layar Tugas — dipenuhi AppContainer; test memakai fake (pola NextUpContainer /
 * InfoContainer). Hanya yang dipakai TugasViewModel yang diekspos: api (markDone), DAO tugas,
 * dan store sync (pending mark selesai yang belum dikonfirmasi server, Task 10).
 */
interface TugasContainer {
    val apiService: ForfhApiService
    val tasksDao: TasksDao
    /** Pending mark selesai (Preferences): id tugas yang PUT-nya belum dikonfirmasi server. */
    val syncState: SyncStateStore
}

data class TugasItem(
    val id: String,
    val title: String,
    val courseName: String?,
    val courseCode: String?,
    val courseColor: String?,
    val dueAt: Long?,
    val status: String,
    val computedStatus: String?,
    val priority: String,
    val description: String?,
    val subtasksJson: String?, // dipakai TugasDetailScreen (koreksi controller #1)
    val syncState: String,     // PENDING/SYNCED/FAILED (Task 10) — chip status di list
)

data class TugasUiState(
    val items: List<TugasItem> = emptyList(),
    val detail: TugasItem? = null,
    val message: String? = null, // hasil aksi terakhir (mis. error markDone)
)

class TugasViewModel(private val container: TugasContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(TugasUiState())
    val state: StateFlow<TugasUiState> = _state

    /** Id yang PUT-nya sedang berjalan — guard double-tap (Task 10). */
    private val inFlight = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            container.tasksDao.getAll().collect { entities ->
                val detailId = _state.value.detail?.id
                _state.value = _state.value.copy(
                    items = entities.map { it.toItem() },
                    detail = entities.firstOrNull { it.id == detailId }?.toItem()
                        ?: _state.value.detail,
                )
            }
        }
    }

    fun openDetail(id: String) {
        viewModelScope.launch {
            container.tasksDao.getById(id).collect { entity ->
                _state.value = _state.value.copy(detail = entity?.toItem())
            }
        }
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detail = null, message = null)
    }

    /**
     * Mark selesai OPTIMISTIK (Task 10): update Room + UI SEKETIKA (DONE + syncState PENDING,
     * id masuk pending_mark_done) → PUT berjalan background (viewModelScope). Sukses → SYNCED
     * (tanpa sentuh UI lagi); gagal (HTTP/network) → FAILED + pesan sekali-pakai. Retry gagal
     * via ketuk chip "Gagal" di list — markDone dipanggil lagi (di-silent re-PUT? tidak: FAILED
     * ikut aturan server saat sync, retry selalu user-initiated).
     */
    fun markDone(taskId: String) {
        if (!inFlight.add(taskId)) return // double-tap saat PUT sedang berjalan → diabaikan
        viewModelScope.launch {
            try {
                container.tasksDao.updateMarked(taskId)
                container.syncState.addPendingMarkDone(taskId)
                val resp = try {
                    container.apiService.markDone(taskId, MarkDoneRequest("DONE"))
                } catch (e: IOException) {
                    failPending(taskId)
                    return@launch
                }
                if (resp.isSuccessful) {
                    container.tasksDao.updateSyncState(taskId, TaskEntity.SyncState.SYNCED)
                    container.syncState.removePendingMarkDone(taskId)
                    _state.value = _state.value.copy(message = "Tugas ditandai selesai.")
                } else {
                    failPending(taskId)
                }
            } finally {
                inFlight.remove(taskId)
            }
        }
    }

    private suspend fun failPending(taskId: String) {
        container.tasksDao.updateSyncState(taskId, TaskEntity.SyncState.FAILED)
        container.syncState.removePendingMarkDone(taskId)
        _state.value = _state.value.copy(message = "Gagal menandai selesai. Cek koneksi, coba lagi.")
    }

    private fun TaskEntity.toItem() = TugasItem(
        id = id,
        title = title,
        courseName = courseName,
        courseCode = courseCode,
        courseColor = courseColor,
        dueAt = dueAt,
        status = status,
        computedStatus = computedStatus,
        priority = priority,
        description = description,
        subtasksJson = subtasksJson,
        syncState = syncState,
    )
}
