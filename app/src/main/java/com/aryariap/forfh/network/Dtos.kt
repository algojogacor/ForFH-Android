package com.aryariap.forfh.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val success: Boolean, val user: UserDto)

@Serializable
data class UserDto(val id: String, val username: String, val displayName: String, val nim: String)

@Serializable
data class SchedulesResponse(val schedules: List<ScheduleDto>)

@Serializable
data class ScheduleDto(
    val id: String,
    val courseId: String,
    val courseName: String,
    val courseCode: String? = null,
    val courseColor: String = "#3b82f6",
    val lecturer: String? = null,
    val credits: Int = 2,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val onlineUrl: String? = null,
    val enabled: Int, // 0|1
)

@Serializable
data class TasksResponse(val tasks: List<TaskDto>)

@Serializable
data class TaskDto(
    val id: String,
    val userId: String,
    val courseId: String? = null,
    val title: String,
    val description: String? = null,
    val type: String = "assignment",
    val dueAt: String? = null,               // ISO-8601 string dari server
    val internalTargetAt: String? = null,
    val priority: String = "medium",
    val estimatedMinutes: Int? = null,
    val status: String = "NOT_STARTED",
    val progress: Int = 0,
    val source: String = "manual",
    val completedAt: String? = null,
    val deletedAt: String? = null,
    val version: Int = 0,
    val externalId: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val computedStatus: String? = null,      // hanya ada di list endpoint
    val course: CourseDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
)

@Serializable
data class CourseDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val code: String? = null,
    val lecturer: String? = null,
    val credits: Int = 2,
    val color: String = "#3b82f6",
)

@Serializable
data class SubtaskDto(
    val id: String,
    val userId: String,
    val taskId: String,
    val title: String,
    val completed: Int = 0,
    val orderIndex: Int = 0,
    val estimatedMinutes: Int? = null,
    val dueAt: String? = null,
    val deletedAt: String? = null,
    val version: Int = 0,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class MarkDoneRequest(val status: String) // body PUT /api/tasks/{id} — app hanya pakai status (REQ-13)

@Serializable
data class SuccessResponse(val success: Boolean, val taskId: String? = null, val scheduleId: String? = null)

@Serializable
data class ErrorBody(val error: String? = null)
