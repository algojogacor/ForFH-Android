package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeCounterTest {

    @Test
    fun `maksimal 5 kali - snooze 0 sampai 4 diizinkan`() {
        assertTrue(SnoozeCounter.canSnooze(0))
        assertTrue(SnoozeCounter.canSnooze(4))
        assertFalse(SnoozeCounter.canSnooze(5))
        assertFalse(SnoozeCounter.canSnooze(6))
    }

    @Test
    fun `tiap snooze menambah tepat 3 menit`() {
        assertEquals(1_000_000L + 180_000L, SnoozeCounter.nextTrigger(1_000_000L))
        assertEquals(1, SnoozeCounter.nextCount(0))
        assertEquals(5, SnoozeCounter.nextCount(4))
    }

    @Test
    fun `konstanta - 5 kali dan 3 menit dalam millis`() {
        assertEquals(5, SnoozeCounter.MAX_SNOOZE)
        assertEquals(3 * 60 * 1000L, SnoozeCounter.SNOOZE_MS)
    }
}
