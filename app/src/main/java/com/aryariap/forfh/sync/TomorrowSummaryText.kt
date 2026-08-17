package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import java.time.LocalDate
import java.time.ZoneId

/**
 * Task 3 - Pure-function text builder for the "Besok" (Tomorrow) notification body.
 *
 * Format: "Besok Senin: PIH 13:00 (A101), Ilmu Negara 15:00 · 1 tugas deadline"
 *
 * Rules:
 * - Day name: Indonesian local map (deterministic, not locale-dependent)
 * - Classes: sorted by startTime asc; name = courseCode ?: courseName;
 *   room non-null → " (${room})"; onlineUrl non-null → " (Daring)"
 * - Tasks: count status != "DONE" with dueAt in [tomorrow 00:00, tomorrow+1 00:00) WIB
 * - >0 tasks → " · N tugas deadline" (N=1: "1 tugas deadline"; N>1: "N tugas deadline")
 * - No classes but tasks → "Besok {day}: tanpa kuliah · N tugas deadline"
 * - Returns null when nothing to report
 */
object TomorrowSummaryText {

    /** Indonesian day names — deterministic, not locale-dependent. */
    private val INDONESIAN_DAY_NAMES = mapOf(
        1 to "Senin",
        2 to "Selasa",
        3 to "Rabu",
        4 to "Kamis",
        5 to "Jumat",
        6 to "Sabtu",
        7 to "Minggu",
    )

    fun build(
        schedules: List<ScheduleEntity>,
        tasks: List<TaskEntity>,
        tomorrow: LocalDate,
        zone: ZoneId,
    ): String? {
        // Filter schedules for tomorrow (match dayOfWeek to tomorrow's java.time day-of-week)
        val tomorrowDow = tomorrow.dayOfWeek.value // java.time: 1=Senin .. 7=Minggu

        val tomorrowSchedules = schedules
            .filter { it.enabled }
            .filter { scheduleDayOfWeek(it.dayOfWeek) == tomorrowDow }
            .sortedBy { it.startTime }

        // Count tasks due tomorrow (status != DONE, dueAt in [tomorrow 00:00, tomorrow+1 00:00) WIB)
        val tomorrowStart = tomorrow.atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowEnd = tomorrow.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val deadlineTasks = tasks.filter { task ->
            task.status != "DONE" &&
                task.dueAt != null &&
                task.dueAt >= tomorrowStart &&
                task.dueAt < tomorrowEnd
        }

        // Nothing to report
        if (tomorrowSchedules.isEmpty() && deadlineTasks.isEmpty()) {
            return null
        }

        val dayName = INDONESIAN_DAY_NAMES[tomorrowDow] ?: "Senin"

        val classPart = if (tomorrowSchedules.isEmpty()) {
            "tanpa kuliah"
        } else {
            tomorrowSchedules.joinToString(", ") { sched ->
                val name = sched.courseCode ?: sched.courseName
                val time = sched.startTime
                val roomOrOnline = when {
                    sched.room != null -> " (${sched.room})"
                    sched.onlineUrl != null -> " (Daring)"
                    else -> ""
                }
                "$name $time$roomOrOnline"
            }
        }

        val taskPart = when {
            deadlineTasks.isEmpty() -> ""
            deadlineTasks.size == 1 -> " · 1 tugas deadline"
            else -> " · ${deadlineTasks.size} tugas deadline"
        }

        return "Besok $dayName: $classPart$taskPart"
    }

    /**
     * Convert schedule day-of-week (0=Sunday..6=Saturday per API convention)
     * to java.time day-of-week (1=Senin..7=Minggu).
     * API: 0=Sunday, 1=Monday, ... 6=Saturday
     * java.time: 1=Mon, 2=Tue, ... 7=Sun
     */
    private fun scheduleDayOfWeek(apiDow: Int): Int {
        // API: 0=Sun, 1=Mon, ..., 6=Sat
        // java.time: 1=Mon, 2=Tue, ..., 7=Sun
        return when (apiDow) {
            0 -> 7  // Sunday → 7
            else -> apiDow // 1(Mon)..6(Sat) → 1..6
        }
    }
}
