package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Matematika notifikasi deadline tugas H-1 — murni, tanpa Android, bisa unit-test (pola AlarmPlanner).
 *
 * Semantik (spec Task 5): untuk tiap tugas aktif (status != DONE) dengan dueAt terparse,
 *   - deadline besok (H-1) → row trigger HARI INI 20:00 WIB;
 *   - deadline hari ini → row trigger hari ini 20:00 WIB HANYA bila now masih sebelum 20:00
 *     (now >= 20:00 → skip: trigger di masa lalu / deadline hari ini sudah lewat);
 *   - deadline < hari ini atau > besok → tanpa row (H-1 saja).
 * Identity one-shot `taskdl|{taskId}|{deadlineDay}` — Task 6 memasangnya lewat ReconcilePlanner.
 */
class TaskDeadlinePlanner(private val zone: ZoneId = ZoneId.of("Asia/Jakarta")) {

    companion object {
        const val DEADLINE_HOUR = 20
        private const val PREFIX = "taskdl"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun taskDeadlineIdentity(taskId: String, deadlineDay: LocalDate): String =
            "$PREFIX|$taskId|${deadlineDay.format(DATE_FMT)}"
    }

    fun computeTasks(tasks: List<TaskEntity>, now: ZonedDateTime): List<ScheduledAlarmEntity> {
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val triggerAtMillis = today.atTime(DEADLINE_HOUR, 0).atZone(zone).toInstant().toEpochMilli()
        val nowMs = now.toInstant().toEpochMilli()
        val rows = mutableListOf<ScheduledAlarmEntity>()
        for (t in tasks) {
            if (t.status == "DONE") continue
            val dueAt = t.dueAt ?: continue
            val deadlineDay = Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
            val eligible = when {
                deadlineDay == tomorrow -> true
                deadlineDay == today && nowMs < triggerAtMillis -> true
                else -> false
            }
            if (!eligible) continue
            rows += ScheduledAlarmEntity(
                id = taskDeadlineIdentity(t.id, deadlineDay),
                kind = "TASK_DEADLINE",
                scheduleId = null,
                offsetMinutes = 0,
                occurrenceDate = deadlineDay.toString(),
                triggerAtMillis = triggerAtMillis,
                snoozeCount = 0,
            )
        }
        return rows
    }
}
