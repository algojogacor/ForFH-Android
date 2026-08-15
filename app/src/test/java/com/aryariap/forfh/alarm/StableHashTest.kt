package com.aryariap.forfh.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableHashTest {

    @Test
    fun `deterministik - nilai sama utk identity sama`() {
        val id = "class|s1|120|2026-08-17"
        assertEquals(StableHash.of(id), StableHash.of(id))
    }

    @Test
    fun `identity berbeda menghasilkan requestCode berbeda`() {
        assertNotEquals(
            StableHash.of("class|s1|120|2026-08-17"),
            StableHash.of("class|s1|120|2026-08-24"),
        )
    }

    @Test
    fun `selalu non-negatif - aman utk notificationId`() {
        assertTrue(StableHash.of("task|09|2026-08-15") >= 0)
        assertTrue(StableHash.of("class|s1|180|2026-08-17") >= 0)
    }
}
