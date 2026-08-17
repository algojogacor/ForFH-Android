package com.aryariap.forfh.sync

import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TomorrowPlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = TomorrowPlanner(zone)

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun schedule(dow: Int, enabled: Boolean = true) = ScheduleEntity(
        id = "s$dow", courseId = "c1", courseName = "MK", courseCode = "MK01",
        courseColor = "#3b82f6", lecturer = null, credits = 2, dayOfWeek = dow,
        startTime = "08:00", endTime = "09:40", room = null, onlineUrl = null, enabled = enabled,
    )

    @Test
    fun `besok ada kuliah - row DAY_PREVIEW hari ini jam 2000 WIB`() {
        // 2026-08-18 = Selasa (dayOfWeek java=2). now Senin 10:00.
        val now = wib("2026-08-17T10:00")
        val row = planner.computeDayPreview(listOf(schedule(2)), now)!!
        assertEquals("tmrw|2026-08-18", row.id)
        assertEquals("DAY_PREVIEW", row.kind)
        assertEquals(null, row.scheduleId)
        assertEquals(0, row.offsetMinutes)
        assertEquals("2026-08-18", row.occurrenceDate)
        assertEquals(
            wib("2026-08-17T20:00").toInstant().toEpochMilli(),
            row.triggerAtMillis,
        )
        assertEquals(0, row.snoozeCount)
    }

    @Test
    fun `besok ada 2 kuliah - tetap satu row DAY_PREVIEW`() {
        val now = wib("2026-08-17T10:00")
        val row = planner.computeDayPreview(listOf(schedule(2), schedule(2)), now)!!
        assertEquals("tmrw|2026-08-18", row.id)
    }

    @Test
    fun `besok tidak ada jadwal - null`() {
        val now = wib("2026-08-17T10:00")
        assertNull(planner.computeDayPreview(emptyList(), now))
        assertNull(planner.computeDayPreview(listOf(schedule(1)), now)) // now=Senin, besok=Selasa (dow=2)
        assertNull(planner.computeDayPreview(listOf(schedule(3)), now)) // now=Senin, besok=Selasa (dow=2)
    }

    @Test
    fun `sekarang jam 2030 WIB - sudah lewat trigger null`() {
        val now = wib("2026-08-17T20:30")
        assertNull(planner.computeDayPreview(listOf(schedule(2)), now))
    }

    @Test
    fun `besok ada jadwal tapi enabled false - null`() {
        val now = wib("2026-08-17T10:00")
        assertNull(planner.computeDayPreview(listOf(schedule(2, enabled = false)), now))
    }

    @Test
    fun `sekarang tepat jam 2000 - trigger masa lalu null`() {
        val now = wib("2026-08-17T20:00")
        assertNull(planner.computeDayPreview(listOf(schedule(2)), now))
    }

    @Test
    fun `besok Senin ada kuliah - now Minggu 1000`() {
        // 2026-08-23 = Minggu. tomorrow = 2026-08-24 = Senin (java dow=1, schedule dow=1)
        val now = wib("2026-08-23T10:00")
        val row = planner.computeDayPreview(listOf(schedule(1)), now)!!
        assertEquals("tmrw|2026-08-24", row.id)
        assertEquals(
            wib("2026-08-23T20:00").toInstant().toEpochMilli(),
            row.triggerAtMillis,
        )
    }

    @Test
    fun `besok Minggu ada kuliah - schedule dow 0`() {
        // 2026-08-22 = Sabtu. tomorrow = 2026-08-23 = Minggu (java dow=7, schedule dow=0)
        val now = wib("2026-08-22T10:00")
        val row = planner.computeDayPreview(listOf(schedule(0)), now)!!
        assertEquals("tmrw|2026-08-23", row.id)
        assertEquals(
            wib("2026-08-22T20:00").toInstant().toEpochMilli(),
            row.triggerAtMillis,
        )
    }

    @Test
    fun `identity helper`() {
        assertEquals(
            "tmrw|2026-08-18",
            TomorrowPlanner.dayPreviewIdentity(java.time.LocalDate.of(2026, 8, 18)),
        )
    }
}
