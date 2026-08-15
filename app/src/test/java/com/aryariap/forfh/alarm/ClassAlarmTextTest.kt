package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassAlarmTextTest {

    private fun sched(room: String?, onlineUrl: String?, start: String, end: String, name: String) = ScheduleEntity(
        id = "s1", courseId = "c1", courseName = name, courseCode = null,
        courseColor = "#c9a84c", lecturer = null, credits = 2, dayOfWeek = 1,
        startTime = start, endTime = end, room = room, onlineUrl = onlineUrl, enabled = true,
    )

    @Test
    fun `dengan ruang - ruang dan jam`() {
        val s = sched(room = "A101", onlineUrl = null, start = "08:00", end = "09:40", name = "Hukum")
        assertEquals("Hukum", ClassAlarmText.title(s))
        assertEquals("A101 · 08:00–09:40", ClassAlarmText.body(s))
    }

    @Test
    fun `tanpa ruang tapi daring - label Daring`() {
        val s = sched(room = null, onlineUrl = "https://zoom.us/xyz", start = "10:00", end = "11:40", name = "Hukum")
        assertEquals("Daring · 10:00–11:40", ClassAlarmText.body(s))
    }

    @Test
    fun `tanpa ruang dan tanpa daring - hanya jam`() {
        val s = sched(room = null, onlineUrl = null, start = "08:00", end = "09:40", name = "Hukum")
        assertEquals("08:00–09:40", ClassAlarmText.body(s))
    }
}
