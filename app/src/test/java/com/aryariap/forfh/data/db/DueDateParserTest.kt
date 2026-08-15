package com.aryariap.forfh.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DueDateParserTest {

    @Test
    fun `ISO string dikonversi ke epoch millis`() {
        assertEquals(1_787_281_200_000L, DueDateParser.parseToEpochMillis("2026-08-21T03:00:00.000Z"))
    }

    @Test
    fun `epoch ms numerik diterima apa adanya`() {
        assertEquals(1_787_281_200_000L, DueDateParser.parseToEpochMillis("1787281200000"))
    }

    @Test
    fun `null tetap null`() {
        assertNull(DueDateParser.parseToEpochMillis(null))
    }

    @Test
    fun `string tak valid menghasilkan null bukan crash`() {
        assertNull(DueDateParser.parseToEpochMillis("bukan-tanggal"))
        assertNull(DueDateParser.parseToEpochMillis("2026-13-99T99:00:00Z"))
    }
}
