package com.aryariap.forfh.alarm

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmFlowExtrasTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    @Test
    fun `class extras valid - semua field terisi`() {
        assertEquals(
            ClassExtras("s1", 120, "2026-08-17", 1750000000000L),
            AlarmFlowExtras.parseClassExtras("s1", "120", "2026-08-17", "1750000000000"),
        )
    }

    @Test
    fun `offset bukan angka - null`() {
        assertNull(AlarmFlowExtras.parseClassExtras("s1", "abc", "2026-08-17", "1750000000000"))
    }

    @Test
    fun `trigger bukan angka - null`() {
        assertNull(AlarmFlowExtras.parseClassExtras("s1", "120", "2026-08-17", "xyz"))
    }

    @Test
    fun `offset negatif - null`() {
        assertNull(AlarmFlowExtras.parseClassExtras("s1", "-5", "2026-08-17", "1750000000000"))
    }

    @Test
    fun `slot 09 15 20 yang cocok dengan trigger - terresolve`() {
        val date = "2026-08-17"
        fun t(slot: Int) = LocalDate.parse(date).atTime(slot, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(9, AlarmFlowExtras.resolveTaskSlot(date, t(9), zone))
        assertEquals(15, AlarmFlowExtras.resolveTaskSlot(date, t(15), zone))
        assertEquals(20, AlarmFlowExtras.resolveTaskSlot(date, t(20), zone))
    }

    @Test
    fun `trigger tidak cocok slot mana pun - null`() {
        val date = "2026-08-17"
        val t9 = LocalDate.parse(date).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        assertNull(AlarmFlowExtras.resolveTaskSlot(date, t9 + 60_000L, zone))
    }

    @Test
    fun `occurrenceDate rusak - null bukan throw`() {
        assertNull(AlarmFlowExtras.resolveTaskSlot("bukan-tanggal", 0L, zone))
    }
}
