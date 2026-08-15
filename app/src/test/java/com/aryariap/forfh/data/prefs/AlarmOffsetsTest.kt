package com.aryariap.forfh.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOffsetsTest {

    @Test
    fun `default semua hari 180 120 60 - perilaku versi sebelumnya`() {
        val o = AlarmOffsets.defaults()
        for (day in 0..6) {
            assertEquals(listOf(180, 120, 60), o.offsetsFor(day))
        }
    }

    @Test
    fun `per hari - tiap hari daftar sendiri`() {
        // Senin (API 1): 1j30m+1j; Rabu (API 3): 4j+3j+2j+1j30m+1j; Selasa (API 2) kosong
        val o = AlarmOffsets(
            mapOf(
                1 to listOf(90, 60),
                3 to listOf(240, 180, 120, 90, 60),
            ),
        )
        assertEquals(listOf(90, 60), o.offsetsFor(1))
        assertEquals(listOf(240, 180, 120, 90, 60), o.offsetsFor(3))
        assertEquals(emptyList<Int>(), o.offsetsFor(2))
        assertEquals(emptyList<Int>(), o.offsetsFor(5))
    }

    @Test
    fun `offsetFor selalu sorted descending walau input acak`() {
        val o = AlarmOffsets(mapOf(0 to listOf(60, 480, 90, 28, 45)))
        assertEquals(listOf(480, 90, 60, 45, 28), o.offsetsFor(0))
    }

    @Test
    fun `fromLegacy - toggle lama dipetakan ke semua hari`() {
        // 3j nonaktif, 2j+1j aktif → semua hari [120, 60]
        val o = AlarmOffsets.fromLegacy(offset3h = false, offset2h = true, offset1h = true)
        for (day in 0..6) assertEquals(listOf(120, 60), o.offsetsFor(day))
    }

    @Test
    fun `format - jam menit bebas`() {
        assertEquals("1 j", formatOffsetMinutes(60))
        assertEquals("1 j 30 m", formatOffsetMinutes(90))
        assertEquals("1 j 13 m", formatOffsetMinutes(73))
        assertEquals("13 m", formatOffsetMinutes(13))
        assertEquals("8 j", formatOffsetMinutes(480))
        assertEquals("3 m", formatOffsetMinutes(3))
    }
}
