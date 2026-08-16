package com.aryariap.forfh.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.NextUpContainer
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * State kartu "Berikutnya" di halaman Jadwal (V1.1 Task 1).
 * nextClass = kuliah enabled berikutnya yang belum mulai (Ruling R5: pakai offset 0 → startDateTime,
 * ambil yang paling awal; skipDates/mute TIDAK diterapkan — murni tampilan).
 * nextAlarm = alarm kelas terpasang berikutnya (Room, trigger masa depan terdekat).
 * mutedToday = prefs.mutedDate == tanggal hari ini (WIB).
 */
data class NextUpUiState(
    val nextClass: Pair<ScheduleEntity, ZonedDateTime>? = null,
    val nextAlarm: ScheduledAlarmEntity? = null,
    val mutedToday: Boolean = false,
)

class NextUpViewModel(
    private val container: NextUpContainer,
    /** Dispatcher untuk baca Room blocking — default Default; test meng-inject test dispatcher. */
    private val background: CoroutineDispatcher = Dispatchers.Default,
    /** Sumber "sekarang" (WIB) — default waktu nyata; test mengontrol. */
    private val nowProvider: () -> ZonedDateTime = { ZonedDateTime.now(ZONE) },
) : ViewModel() {

    private val _state = MutableStateFlow(NextUpUiState())
    val state: StateFlow<NextUpUiState> = _state

    init {
        viewModelScope.launch {
            container.prefs.mutedDate.collect { d ->
                _state.update { it.copy(mutedToday = d == todayWib()) }
            }
        }
        // Ubah konfigurasi alarm (offset) → hitung ulang kartu. Siklus 30 dtk ticker ada di UI (Task 2).
        viewModelScope.launch {
            container.prefs.offsets.collect { refresh() }
        }
    }

    /** Hitung ulang next class + next alarm dari "sekarang" (dipanggil ticker UI tiap 30 dtk). */
    fun refresh() {
        viewModelScope.launch {
            val now = nowProvider()
            val (nextClass, nextAlarm) = withContext(background) {
                val schedules = container.schedulesDao.getEnabledOnce()
                val nc = schedules
                    .map { s ->
                        s to container.planner.nextClassOccurrence(s.id, s.dayOfWeek, s.startTime, 0, now).startDateTime
                    }
                    .minByOrNull { it.second }
                val na = container.alarmsDao.nextClassAlarmOnce(now.toInstant().toEpochMilli())
                nc to na
            }
            _state.update { it.copy(nextClass = nextClass, nextAlarm = nextAlarm) }
        }
    }

    /** "Matikan seluruh alarm hari ini" — pola PengaturanViewModel: simpan tanggal + rescheduleAll. */
    fun muteToday() {
        viewModelScope.launch {
            container.prefs.setMutedDate(todayWib())
            withContext(background) { container.rescheduler.rescheduleAll() }
            // Ruling R12 (review Task 1): rescheduleAll mengubah row alarm → hitung ulang
            // kartu langsung; tanpa ini kartu bisa tampilkan alarm basi sampai ticker 30 dtk.
            refresh()
        }
    }

    /** Batal mute — hapus tanggal + pasang kembali alarm hari ini. */
    fun unmuteToday() {
        viewModelScope.launch {
            container.prefs.setMutedDate(null)
            withContext(background) { container.rescheduler.rescheduleAll() }
            refresh()
        }
    }

    private fun todayWib(): String = nowProvider().toLocalDate().toString()

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    }
}
