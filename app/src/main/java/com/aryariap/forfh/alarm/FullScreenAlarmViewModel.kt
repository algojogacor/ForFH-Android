package com.aryariap.forfh.alarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.ForfhApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AlarmUiState(
    val valid: Boolean = false,
    val title: String = "",
    val body: String = "",
    val snoozeAvailable: Boolean = false,
    val snoozeCount: Int = 0,
)

class FullScreenAlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ForfhApp).container
    private val alarmsDao get() = container.database.scheduledAlarmsDao()
    private val schedulesDao get() = container.database.schedulesDao()

    private var identity: String? = null
    private var snoozedThisSession = false

    private val _state = MutableStateFlow(AlarmUiState())
    val state: StateFlow<AlarmUiState> = _state

    fun bind(identity: String, extrasTriggerAtMillis: Long) {
        this.identity = identity
        viewModelScope.launch {
            val row = alarmsDao.getByIdOnce(identity)
            val schedule = row?.scheduleId?.let { schedulesDao.getByIdOnce(it) }
            val valid = row != null && schedule != null && row.triggerAtMillis == extrasTriggerAtMillis
            _state.value = if (valid) {
                AlarmUiState(
                    valid = true,
                    title = ClassAlarmText.title(schedule!!),
                    body = ClassAlarmText.body(schedule),
                    snoozeAvailable = SnoozeCounter.canSnooze(row!!.snoozeCount),
                    snoozeCount = row.snoozeCount,
                )
            } else {
                AlarmUiState(valid = false)
            }
        }
    }

    fun snooze() {
        val id = identity ?: return
        viewModelScope.launch {
            if (container.alarmFlow.snooze(id)) {
                snoozedThisSession = true
                val row = alarmsDao.getByIdOnce(id)
                _state.value = _state.value.copy(
                    snoozeCount = row?.snoozeCount ?: 0,
                    snoozeAvailable = row != null && SnoozeCounter.canSnooze(row.snoozeCount),
                )
            }
        }
    }

    /** Tutup = occurrence selesai. Row dihapus bila tidak ada snooze tersisa yang ditunda. */
    fun close() {
        val id = identity ?: return
        viewModelScope.launch {
            val row = alarmsDao.getByIdOnce(id) ?: return@launch
            if (!snoozedThisSession && row.snoozeCount == 0) {
                alarmsDao.deleteById(id)
            }
        }
    }
}
