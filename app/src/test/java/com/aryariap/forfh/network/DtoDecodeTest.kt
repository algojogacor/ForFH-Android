package com.aryariap.forfh.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DtoDecodeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `login response terdecode - user dan success`() {
        val r = json.decodeFromString<LoginResponse>(
            """{"success":true,"user":{"id":"u1","username":"nama","displayName":"Nama","nim":"123"}}""",
        )
        assertEquals(true, r.success)
        assertEquals("u1", r.user.id)
        assertEquals("123", r.user.nim)
    }

    @Test
    fun `schedule dto - enabled int 0-1, color default, nullable field`() {
        val r = json.decodeFromString<SchedulesResponse>(
            """{"schedules":[
                {"id":"s1","courseId":"c1","courseName":"Hukum","courseColor":"#c9a84c",
                 "lecturer":null,"credits":3,"dayOfWeek":1,"startTime":"08:00","endTime":"09:40",
                 "room":"A101","onlineUrl":null,"enabled":1},
                {"id":"s2","courseId":"c2","courseName":"Tata Negara","courseCode":"TN101",
                 "dayOfWeek":3,"startTime":"10:00","endTime":"11:40","enabled":0}
            ]}""",
        )
        assertEquals(2, r.schedules.size)
        assertEquals(1, r.schedules[0].enabled)
        assertEquals(0, r.schedules[1].enabled)
        assertEquals("#3b82f6", r.schedules[1].courseColor) // default
        assertNull(r.schedules[0].lecturer)
        assertEquals("TN101", r.schedules[1].courseCode)
    }

    @Test
    fun `task dto - dueAt ISO string, computedStatus, course dan subtasks`() {
        val r = json.decodeFromString<TasksResponse>(
            """{"tasks":[{
                "id":"t1","userId":"u1","courseId":"c1","title":"Makalah",
                "description":null,"type":"assignment","dueAt":"2026-08-20T03:00:00.000Z",
                "internalTargetAt":null,"priority":"high","estimatedMinutes":120,
                "status":"NOT_STARTED","progress":0,"source":"manual","completedAt":null,
                "deletedAt":null,"version":1,"externalId":null,
                "createdAt":"2026-08-01T03:00:00.000Z","updatedAt":"2026-08-01T03:00:00.000Z",
                "computedStatus":"OVERDUE",
                "course":{"id":"c1","userId":"u1","name":"Hukum","code":"HK101","color":"#c9a84c"},
                "subtasks":[{"id":"st1","userId":"u1","taskId":"t1","title":"Bab 1",
                             "completed":0,"orderIndex":1,"estimatedMinutes":60,"dueAt":null,
                             "deletedAt":null,"version":0,"createdAt":"2026-08-01T03:00:00.000Z",
                             "updatedAt":"2026-08-01T03:00:00.000Z"}]
            }]}""",
        )
        val t = r.tasks.single()
        assertEquals("OVERDUE", t.computedStatus)
        assertEquals("2026-08-20T03:00:00.000Z", t.dueAt)
        assertEquals("#c9a84c", t.course?.color)
        assertEquals(1, t.subtasks.size)
        assertNotNull(t.course)
    }

    @Test
    fun `mark done response dan error body terdecode`() {
        val ok = json.decodeFromString<SuccessResponse>("""{"success":true}""")
        assertEquals(true, ok.success)
        val err = json.decodeFromString<ErrorBody>("""{"error":"Judul tugas wajib diisi."}""")
        assertEquals("Judul tugas wajib diisi.", err.error)
    }
}
