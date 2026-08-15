package com.aryariap.forfh.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.data.prefs.AlarmOffsets
import com.aryariap.forfh.sync.SyncWorker
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PengaturanUiState(
    val offsets: AlarmOffsets = AlarmOffsets.defaults(),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
    /** "yyyy-MM-dd" saat seluruh alarm kuliah hari itu dimatikan user (null = normal). */
    val mutedDate: String? = null,
)

class PengaturanViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PengaturanUiState())
    val state: StateFlow<PengaturanUiState> = _state

    init {
        viewModelScope.launch { container.prefs.offsets.collect { o -> _state.value = _state.value.copy(offsets = o) } }
        viewModelScope.launch { container.prefs.lastSyncStatus.collect { s -> _state.value = _state.value.copy(lastSyncStatus = s) } }
        viewModelScope.launch { container.prefs.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) } }
        viewModelScope.launch { container.prefs.mutedDate.collect { d -> _state.value = _state.value.copy(mutedDate = d) } }
    }

    /**
     * "Matikan seluruh alarm hari ini": simpan tanggal → rescheduleAll.
     * Alarm kelas hari itu di-cancel (termasuk row snooze aktif hari itu), task reminder
     * tetap; besok normal lagi (tanggal basi otomatis).
     */
    fun muteToday() {
        viewModelScope.launch {
            container.prefs.setMutedDate(LocalDate.now().toString())
            withContext(Dispatchers.Default) { container.rescheduler.rescheduleAll() }
        }
    }

    /** Batal: kembalikan semua alarm hari ini seperti semula. */
    fun unmuteToday() {
        viewModelScope.launch {
            container.prefs.setMutedDate(null)
            withContext(Dispatchers.Default) { container.rescheduler.rescheduleAll() }
        }
    }

    /**
     * Tambah offset satu hari (menit bebas, 1..720) → rescheduleAll:
     * alarm lama di-cancel, alarm baru dipasang; snooze aktif tetap (ReconcilePlanner).
     * Duplikat ditolak (set semantics), daftar disimpan sorted desc.
     */
    fun addOffset(dayOfWeek: Int, offsetMinutes: Int) {
        viewModelScope.launch {
            val cur = _state.value.offsets
            val list = (cur.perDay[dayOfWeek].orEmpty() + offsetMinutes).distinct().sortedDescending()
            val next = AlarmOffsets(cur.perDay + (dayOfWeek to list))
            container.prefs.setOffsets(next)
            // Room DAO blocking → wajib background (pola sama dgn ExactAlarmPermissionReceiver)
            withContext(Dispatchers.Default) { container.rescheduler.rescheduleAll() }
        }
    }

    /** Hapus satu offset dari satu hari → rescheduleAll. */
    fun removeOffset(dayOfWeek: Int, offsetMinutes: Int) {
        viewModelScope.launch {
            val cur = _state.value.offsets
            val list = cur.perDay[dayOfWeek].orEmpty().filterNot { it == offsetMinutes }
            val next = AlarmOffsets(cur.perDay + (dayOfWeek to list))
            container.prefs.setOffsets(next)
            // Room DAO blocking → wajib background (pola sama dgn ExactAlarmPermissionReceiver)
            withContext(Dispatchers.Default) { container.rescheduler.rescheduleAll() }
        }
    }

    fun syncNow() { SyncWorker.enqueueOneShot(container.context) }

    fun logout() { container.logout("Kamu sudah keluar.") }
}
