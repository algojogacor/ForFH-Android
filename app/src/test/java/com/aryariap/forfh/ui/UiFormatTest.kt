package com.aryariap.forfh.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class UiFormatTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    private fun wib(s: String): ZonedDateTime =
        LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(zone)

    @Test
    fun `deadline epoch millis diformat WIB`() {
        // 2026-08-20T03:00:00.000Z = 2026-08-20 10:00 WIB
        assertEquals("21 Agu 2026 · 10:00", UiFormat.deadline(1_787_281_200_000L, zone)) // 1_787_281_200_000 = 21 Agu 03:00Z = 10:00 WIB
    }

    @Test
    fun `null deadline jadi label tak berdeadline`() {
        assertEquals("Tanpa deadline", UiFormat.deadline(null, zone))
    }

    @Test
    fun `range jam dari start dan end`() {
        assertEquals("08:00–09:40", UiFormat.range("08:00", "09:40"))
    }

    @Test
    fun `label status tugas`() {
        assertEquals("Belum", UiFormat.statusLabel("NOT_STARTED"))
        assertEquals("Selesai", UiFormat.statusLabel("DONE"))
        assertEquals("Terlambat", UiFormat.statusLabel("OVERDUE"))
        assertEquals("Proses", UiFormat.statusLabel("IN_PROGRESS"))
        assertEquals("Revisi", UiFormat.statusLabel("REVISION"))
    }

    @Test
    fun `nama hari dan indeks WIB`() {
        assertEquals("Minggu", UiFormat.dayName(0))
        assertEquals("Senin", UiFormat.dayName(1))
        assertEquals("Sabtu", UiFormat.dayName(6))
    }

    @Test
    fun `countdown format Indonesia - jam dan menit`() {
        assertEquals("2 j 47 m", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-17T10:17")))
        assertEquals("47 m", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-17T08:17")))
        assertEquals("1 j", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-17T08:30")))
        assertEquals("0 m", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-17T07:30")))
    }

    @Test
    fun `countdown lebih dari sehari memakai satuan hari`() {
        assertEquals("1 hari 1 j", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-18T08:30")))
        assertEquals("2 hari", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-19T07:30")))
        assertEquals("2 hari 47 m", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-19T08:17")))
    }

    @Test
    fun `countdown saat start sudah lewat - defensif 0 menit`() {
        assertEquals("0 m", UiFormat.countdownTo(wib("2026-08-17T07:30"), wib("2026-08-17T07:29")))
    }

    @Test
    fun `timeOf epoch millis jadi HHmm WIB`() {
        assertEquals("10:00", UiFormat.timeOf(1_787_281_200_000L, zone)) // 2026-08-21 03:00Z = 10:00 WIB
    }

    @Test
    fun `timeOf ZonedDateTime jadi HHmm`() {
        assertEquals("08:00", UiFormat.timeOf(wib("2026-08-17T08:00")))
    }
}
