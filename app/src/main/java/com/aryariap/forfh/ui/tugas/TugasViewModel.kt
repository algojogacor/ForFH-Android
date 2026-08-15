package com.aryariap.forfh.ui.tugas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.network.MarkDoneRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.ZoneId

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
)

data class TugasUiState(
    val items: List<TugasItem> = emptyList(),
    val detail: TugasItem? = null,
    val message: String? = null, // hasil aksi terakhir (mis. error markDone)
)

class TugasViewModel(private val container: AppContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(TugasUiState())
    val state: StateFlow<TugasUiState> = _state

    init {
        viewModelScope.launch {
            container.database.tasksDao().getAll().collect { entities ->
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
            container.database.tasksDao().getById(id).collect { entity ->
                _state.value = _state.value.copy(detail = entity?.toItem())
            }
        }
    }

    fun closeDetail() {
        _state.value = _state.value.copy(detail = null, message = null)
    }

    /**
     * Server sumber kebenaran (invariant §7, REQ-13): PUT sukses → baru update Room.
     * Gagal → tugas tetap utuh, user diberi tahu. (Fix round final review: transport failure
     * retrofit suspend melempar IOException — tanpa try/catch ini jadi uncaught di viewModelScope
     * → process crash; spec §10: tangani dengan pesan, jangan crash.)
     */
    fun markDone(taskId: String) {
        viewModelScope.launch {
            val resp = try {
                container.apiService.markDone(taskId, MarkDoneRequest("DONE"))
            } catch (e: IOException) {
                _state.value = _state.value.copy(message = "Gagal menandai selesai. Cek koneksi, coba lagi.")
                return@launch
            }
            if (resp.isSuccessful) {
                container.database.tasksDao().updateStatus(taskId, "DONE", null)
                _state.value = _state.value.copy(message = "Tugas ditandai selesai.")
            } else {
                _state.value = _state.value.copy(message = "Gagal menandai selesai. Cek koneksi, coba lagi.")
            }
        }
    }

    private fun com.aryariap.forfh.data.db.TaskEntity.toItem() = TugasItem(
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
    )
}
