package com.aryariap.forfh.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmOffsetsTest {

    @Test
    fun `semua aktif menghasilkan 180 120 60`() {
        assertEquals(listOf(180, 120, 60), AlarmOffsets(true, true, true).activeOffsets())
    }

    @Test
    fun `hanya 2 jam aktif`() {
        assertEquals(listOf(120), AlarmOffsets(false, true, false).activeOffsets())
    }

    @Test
    fun `semua nonaktif kosong`() {
        assertEquals(emptyList<Int>(), AlarmOffsets(false, false, false).activeOffsets())
    }
}
