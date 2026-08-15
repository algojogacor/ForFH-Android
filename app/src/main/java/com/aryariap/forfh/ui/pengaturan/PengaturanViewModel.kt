package com.aryariap.forfh.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.sync.SyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PengaturanUiState(
    val offsets: AlarmOffsets = AlarmOffsets(true, true, true),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
)

class PengaturanViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PengaturanUiState())
    val state: StateFlow<PengaturanUiState> = _state

    init {
        viewModelScope.launch { container.prefs.offsets.collect { o -> _state.value = _state.value.copy(offsets = o) } }
        viewModelScope.launch { container.prefs.lastSyncStatus.collect { s -> _state.value = _state.value.copy(lastSyncStatus = s) } }
        viewModelScope.launch { container.prefs.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) } }
    }

    /** Ubah offset → rescheduleAll: alarm lama di-cancel, alarm baru dipasang; snooze aktif tetap (ReconcilePlanner). */
    fun setOffset(offsetMinutes: Int, enabled: Boolean) {
        viewModelScope.launch {
            val cur = _state.value.offsets
            val next = AlarmOffsets(
                offset3h = if (offsetMinutes == 180) enabled else cur.offset3h,
                offset2h = if (offsetMinutes == 120) enabled else cur.offset2h,
                offset1h = if (offsetMinutes == 60) enabled else cur.offset1h,
            )
            container.prefs.setOffsets(next)
            container.rescheduler.rescheduleAll()
        }
    }

    fun syncNow() { SyncWorker.enqueueOneShot(container.context) }

    fun logout() { container.logout("Kamu sudah keluar.") }
}
