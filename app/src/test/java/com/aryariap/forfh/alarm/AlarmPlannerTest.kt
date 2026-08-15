package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AlarmPlannerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val planner = AlarmPlanner(zone)

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `Senin 0800 offset 2 jam - trigger Senin 0600`() {
        val now = wib("2026-08-15T10:00") // Sabtu
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 17), occ.occurrenceDate)
        assertEquals("class|s1|120|2026-08-17", occ.identity)
        assertEquals(wibEpoch(2026, 8, 17, 6, 0), occ.triggerAtMillis) // Senin 06:00 WIB (start 08:00 − 120 mnt)
    }

    @Test
    fun `trigger hari ini sudah lewat - lompat ke minggu berikutnya`() {
        val now = wib("2026-08-17T07:00") // Senin 07:00, trigger-nya Senin 06:00 sudah lewat
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 24), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 24, 6, 0), occ.triggerAtMillis) // Senin 24 06:00 WIB
    }

    @Test
    fun `persis sama dengan now dianggap lewat`() {
        val now = wib("2026-08-17T06:00") // Senin 06:00 == trigger
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 1, startTime = "08:00", offsetMinutes = 120, now = now)
        assertEquals(LocalDate.of(2026, 8, 24), occ.occurrenceDate)
    }

    @Test
    fun `lintas minggu - jadwal Selasa dihitung dari Kamis`() {
        val now = wib("2026-08-13T09:00") // Kamis
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 2, startTime = "07:00", offsetMinutes = 60, now = now)
        assertEquals(LocalDate.of(2026, 8, 18), occ.occurrenceDate) // Selasa berikutnya
        assertEquals(wibEpoch(2026, 8, 18, 6, 0), occ.triggerAtMillis)
    }

    @Test
    fun `pergantian tanggal - malam Senin jadwal Selasa pagi`() {
        val now = wib("2026-08-17T23:30") // Senin 23:30
        val occ = planner.nextClassOccurrence("s1", dayOfWeek = 2, startTime = "07:00", offsetMinutes = 60, now = now)
        assertEquals(LocalDate.of(2026, 8, 18), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 18, 6, 0), occ.triggerAtMillis)
    }

    @Test
    fun `hari Minggu = dayOfWeek 0`() {
        val now = wib("2026-08-15T10:00") // Sabtu
        val occ = planner.nextClassOccurrence("s0", dayOfWeek = 0, startTime = "09:00", offsetMinutes = 0, now = now)
        assertEquals(LocalDate.of(2026, 8, 16), occ.occurrenceDate)
        assertEquals(wibEpoch(2026, 8, 16, 9, 0), occ.triggerAtMillis)
    }

    @Test
    fun `slot tugas - 09 pagi sudah lewat berarti besok, 15 siang masih hari ini`() {
        val now = wib("2026-08-15T10:00")
        val (date09, t09) = planner.nextTaskSlot(9, now)
        val (date15, t15) = planner.nextTaskSlot(15, now)
        assertEquals(LocalDate.of(2026, 8, 16), date09)
        assertEquals(wibEpoch(2026, 8, 16, 9, 0), t09)
        assertEquals(LocalDate.of(2026, 8, 15), date15)
        assertEquals(wibEpoch(2026, 8, 15, 15, 0), t15)
    }

    @Test
    fun `identity tugas - format task|slot|date`() {
        assertEquals("task|09|2026-08-15", AlarmPlanner.taskIdentity(9, LocalDate.of(2026, 8, 15)))
        assertEquals("task|20|2026-08-15", AlarmPlanner.taskIdentity(20, LocalDate.of(2026, 8, 15)))
    }
}
