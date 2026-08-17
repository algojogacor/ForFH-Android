package com.aryariap.forfh.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.info.SyncActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

data class JadwalItem(
    val id: String,
    val courseName: String,
    val courseCode: String?,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val onlineUrl: String?,
    val color: String,
    val enabled: Boolean,
    val dayIndex: Int, // 0=Sunday .. 6=Saturday (konvensi API ForFH)
)

data class JadwalHari(
    val dayIndex: Int,
    val label: String,
    val items: List<JadwalItem>,
)

data class JadwalUiState(
    val today: List<JadwalItem> = emptyList(),
    val week: List<JadwalHari> = emptyList(),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
    /** Aktivitas worker sync, indikator pull-to-refresh (RUNNING). */
    val syncActivity: SyncActivity = SyncActivity.IDLE,
)

class JadwalViewModel(private val container: AppContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(JadwalUiState())
    val state: StateFlow<JadwalUiState> = _state

    init {
        viewModelScope.launch {
            container.database.schedulesDao().getAll().collect { entities ->
                val todayIdx = ZonedDateTime.now(zone).dayOfWeek.value % 7 // Senin=1..Minggu=7 → 0=Sunday
                val items = entities.map {
                    JadwalItem(
                        id = it.id,
                        courseName = it.courseName,
                        courseCode = it.courseCode,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        room = it.room,
                        onlineUrl = it.onlineUrl,
                        color = it.courseColor,
                        enabled = it.enabled,
                        dayIndex = it.dayOfWeek,
                    )
                }
                _state.value = _state.value.copy(
                    today = items.filter { it.dayIndex == todayIdx },
                    week = (0..6).map { d ->
                        JadwalHari(d, UiFormat.dayName(d), items.filter { it.dayIndex == d })
                    },
                )
            }
        }
        viewModelScope.launch {
            container.prefs.lastSyncStatus.collect { s -> _state.value = _state.value.copy(lastSyncStatus = s) }
        }
        viewModelScope.launch {
            container.prefs.lastSyncAt.collect { t -> _state.value = _state.value.copy(lastSyncAt = t) }
        }
        viewModelScope.launch {
            container.syncActivity.collect { a -> _state.value = _state.value.copy(syncActivity = a) }
        }
    }

    /** Sinkron sekarang: fire-and-forget via WorkManager (pola InfoViewModel.syncNow). */
    fun syncNow() {
        container.enqueueSync()
    }
}
