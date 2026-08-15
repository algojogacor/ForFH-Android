package com.aryariap.forfh.network

import com.aryariap.forfh.data.db.DueDateParser
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.TaskEntity
import kotlinx.serialization.json.Json

/** Encode subtasks ringkas (tanpa default fields) utk kolom subtasksJson. */
private val subtasksJsonCodec = Json { encodeDefaults = false }

fun ScheduleDto.toEntity(): ScheduleEntity = ScheduleEntity(
    id = id,
    courseId = courseId,
    courseName = courseName,
    courseCode = courseCode,
    courseColor = courseColor.ifBlank { "#3b82f6" },
    lecturer = lecturer,
    credits = credits,
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    room = room,
    onlineUrl = onlineUrl,
    enabled = enabled != 0,
)

fun TaskDto.toEntity(nowMs: Long): TaskEntity = TaskEntity(
    id = id,
    courseId = courseId,
    courseName = course?.name,
    courseCode = course?.code,
    title = title,
    description = description,
    dueAt = DueDateParser.parseToEpochMillis(dueAt),
    status = status,
    computedStatus = computedStatus ?: computeComputedStatus(status, dueAt, nowMs),
    priority = priority,
    courseColor = course?.color,
    subtasksJson = subtasks.takeIf { it.isNotEmpty() }?.let { subtasksJsonCodec.encodeToString(it) },
)

/** OVERDUE dinamis: status != DONE dan dueAt di masa lalu (spesifikasi list endpoint). */
fun computeComputedStatus(status: String, dueAtIso: String?, nowMs: Long): String? {
    if (status == "DONE") return null
    val due = DueDateParser.parseToEpochMillis(dueAtIso) ?: return null
    return if (due < nowMs) "OVERDUE" else null
}
