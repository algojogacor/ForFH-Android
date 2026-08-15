package com.aryariap.forfh.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class UiFormatTest {
    private val zone = ZoneId.of("Asia/Jakarta")

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
}
