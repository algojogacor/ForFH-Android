package com.aryariap.forfh.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryariap.forfh.AppContainer
import com.aryariap.forfh.data.db.DueDateParser
import com.aryariap.forfh.ui.UiFormat
import com.aryariap.forfh.ui.info.CalendarListModel
import com.aryariap.forfh.ui.info.InfoCardModels
import com.aryariap.forfh.ui.info.SyncActivity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class CalendarTab { HARI_INI, SEMINGGU, BULAN }

enum class CalendarFilter { KULIAH, TUGAS, AKADEMIK }

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

data class TaskCalendarItem(
    val id: String,
    val title: String,
    val courseName: String?,
    val courseCode: String?,
    val courseColor: String?,
    val priority: String,
    val status: String,
    val computedStatus: String,
    val dueAtEpochMs: Long?,
    val dueDate: LocalDate?,
    val dueTimeText: String,
)

data class AcademicEventItem(
    val title: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val rawStart: String?,
    val rawEnd: String?,
    val extras: List<Pair<String, String>> = emptyList(),
) {
    val dateRangeText: String
        get() = UiFormat.formatAcademicRange(rawStart, rawEnd)

    fun statusBadge(targetDate: LocalDate): String {
        return when {
            endDate != null && endDate == targetDate -> "Berakhir Hari Ini"
            startDate != null && startDate == targetDate -> "Mulai Hari Ini"
            endDate != null && endDate == targetDate.plusDays(1) -> "Berakhir Besok"
            else -> "Sedang Berlangsung"
        }
    }

    fun spansAcross(date: LocalDate): Boolean {
        if (startDate == null) return false
        val end = endDate ?: startDate
        return !date.isBefore(startDate) && !date.isAfter(end)
    }
}

data class DayEvents(
    val date: LocalDate,
    val classes: List<JadwalItem> = emptyList(),
    val tasks: List<TaskCalendarItem> = emptyList(),
    val academic: List<AcademicEventItem> = emptyList(),
) {
    val isEmpty: Boolean get() = classes.isEmpty() && tasks.isEmpty() && academic.isEmpty()
}

data class JadwalHari(
    val dayIndex: Int,
    val label: String,
    val items: List<JadwalItem>,
    val tasks: List<TaskCalendarItem> = emptyList(),
    val academic: List<AcademicEventItem> = emptyList(),
)

data class JadwalUiState(
    val selectedTab: CalendarTab = CalendarTab.HARI_INI,
    val selectedMonth: YearMonth = YearMonth.now(ZoneId.of("Asia/Jakarta")),
    val selectedDate: LocalDate = LocalDate.now(ZoneId.of("Asia/Jakarta")),
    val activeFilters: Set<CalendarFilter> = setOf(CalendarFilter.KULIAH, CalendarFilter.TUGAS, CalendarFilter.AKADEMIK),
    val todayClasses: List<JadwalItem> = emptyList(),
    val todayTasks: List<TaskCalendarItem> = emptyList(),
    val todayAcademic: List<AcademicEventItem> = emptyList(),
    val week: List<JadwalHari> = emptyList(),
    val allSchedules: List<JadwalItem> = emptyList(),
    val allTasks: List<TaskCalendarItem> = emptyList(),
    val allAcademicEvents: List<AcademicEventItem> = emptyList(),
    val lastSyncStatus: String = "",
    val lastSyncAt: Long = 0L,
    val syncActivity: SyncActivity = SyncActivity.IDLE,
) {
    /** Backwards-compatible alias for today classes */
    val today: List<JadwalItem> get() = todayClasses

    fun getEventsForDate(date: LocalDate): DayEvents {
        val dayIndex = date.dayOfWeek.value % 7 // Senin=1..Minggu=7 -> 0=Sunday
        val classes = if (activeFilters.contains(CalendarFilter.KULIAH)) {
            allSchedules.filter { it.dayIndex == dayIndex && it.enabled }
        } else emptyList()

        val tasks = if (activeFilters.contains(CalendarFilter.TUGAS)) {
            allTasks.filter { it.dueDate == date }
        } else emptyList()

        val academic = if (activeFilters.contains(CalendarFilter.AKADEMIK)) {
            allAcademicEvents.filter { it.spansAcross(date) }
        } else emptyList()

        return DayEvents(date, classes, tasks, academic)
    }
}

class JadwalViewModel(private val container: com.aryariap.forfh.JadwalContainer) : ViewModel() {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val _state = MutableStateFlow(JadwalUiState())
    val state: StateFlow<JadwalUiState> = _state

    init {
        val schedulesFlow = container.schedulesDao.getAll()
        val tasksFlow = container.tasksDao.getAll()
        val kampusInfoFlow = container.kampusInfoDao.getKampusInfo()

        viewModelScope.launch {
            combine(schedulesFlow, tasksFlow, kampusInfoFlow) { schedEntities, taskEntities, infoEntities ->
                val nowZdt = ZonedDateTime.now(zone)
                val todayDate = nowZdt.toLocalDate()
                val todayIdx = nowZdt.dayOfWeek.value % 7 // 0=Sunday..6=Saturday

                // 1. Map Schedules
                val schedules = schedEntities.map {
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

                // 2. Map Tasks with parsed LocalDate & Time
                val tasks = taskEntities.map { t ->
                    val epochMs = t.dueAt
                    val date = epochMs?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    val timeStr = epochMs?.let { UiFormat.timeOf(it, zone) } ?: ""
                    TaskCalendarItem(
                        id = t.id,
                        title = t.title,
                        courseName = t.courseName,
                        courseCode = t.courseCode,
                        courseColor = t.courseColor,
                        priority = t.priority,
                        status = t.status,
                        computedStatus = t.computedStatus ?: t.status,
                        dueAtEpochMs = epochMs,
                        dueDate = date,
                        dueTimeText = timeStr,
                    )
                }

                // 3. Map Academic Calendar
                val kalenderInfo = infoEntities.firstOrNull { it.jenis == "kalender_akademik" }
                val academicEvents = if (kalenderInfo != null) {
                    val parsed = InfoCardModels.buildInfoCardModel(kalenderInfo.jenis, kalenderInfo.dataJson)
                    if (parsed is CalendarListModel) {
                        parsed.rows.map { r ->
                            val sDate = UiFormat.parseDateRobust(r.mulai)
                            val eDate = UiFormat.parseDateRobust(r.selesai) ?: sDate
                            AcademicEventItem(
                                title = r.kegiatan ?: "Kegiatan Akademik",
                                startDate = sDate,
                                endDate = eDate,
                                rawStart = r.mulai,
                                rawEnd = r.selesai,
                                extras = r.extras,
                            )
                        }
                    } else emptyList()
                } else emptyList()

                // Calculate today and week views
                val todayClasses = schedules.filter { it.dayIndex == todayIdx && it.enabled }
                val todayTasks = tasks.filter { it.dueDate == todayDate }
                val todayAcademic = academicEvents
                    .filter { it.spansAcross(todayDate) }
                    .sortedWith(compareBy(
                        { if (it.endDate == todayDate) 0 else if (it.startDate == todayDate) 1 else if (it.endDate != null && it.endDate <= todayDate.plusDays(7)) 2 else 3 },
                        { it.endDate ?: LocalDate.MAX }
                    ))

                // Week days for the current week
                val weekStartMonday = todayDate.minusDays(((todayDate.dayOfWeek.value - 1) % 7).toLong())
                val week = (0..6).map { offset ->
                    val date = weekStartMonday.plusDays(offset.toLong())
                    val dIdx = date.dayOfWeek.value % 7
                    JadwalHari(
                        dayIndex = dIdx,
                        label = UiFormat.dayName(dIdx),
                        items = schedules.filter { it.dayIndex == dIdx && it.enabled },
                        tasks = tasks.filter { it.dueDate == date },
                        academic = academicEvents.filter { it.spansAcross(date) },
                    )
                }

                _state.value.copy(
                    allSchedules = schedules,
                    allTasks = tasks,
                    allAcademicEvents = academicEvents,
                    todayClasses = todayClasses,
                    todayTasks = todayTasks,
                    todayAcademic = todayAcademic,
                    week = week,
                )
            }.collect { newState ->
                _state.value = newState
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

    fun selectTab(tab: CalendarTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
    }

    fun nextMonth() {
        val next = _state.value.selectedMonth.plusMonths(1)
        _state.value = _state.value.copy(
            selectedMonth = next,
            selectedDate = next.atDay(1),
        )
    }

    fun prevMonth() {
        val prev = _state.value.selectedMonth.minusMonths(1)
        _state.value = _state.value.copy(
            selectedMonth = prev,
            selectedDate = prev.atDay(1),
        )
    }

    fun jumpToToday() {
        val today = LocalDate.now(zone)
        _state.value = _state.value.copy(
            selectedMonth = YearMonth.from(today),
            selectedDate = today,
        )
    }

    fun toggleFilter(filter: CalendarFilter) {
        val current = _state.value.activeFilters.toMutableSet()
        if (current.contains(filter)) {
            // Minimal 1 filter aktif
            if (current.size > 1) current.remove(filter)
        } else {
            current.add(filter)
        }
        _state.value = _state.value.copy(activeFilters = current)
    }

    fun setFilterAll() {
        _state.value = _state.value.copy(
            activeFilters = setOf(CalendarFilter.KULIAH, CalendarFilter.TUGAS, CalendarFilter.AKADEMIK)
        )
    }

    fun syncNow() {
        container.enqueueSync()
    }
}
