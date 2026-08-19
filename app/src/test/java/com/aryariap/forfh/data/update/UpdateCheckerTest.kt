package com.aryariap.forfh.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `remote version with higher minor is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v2.4.0", "2.3.0"))
        assertTrue(UpdateChecker.isNewerVersion("2.4.0", "2.3.0"))
    }

    @Test
    fun `remote version with higher patch is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v2.3.1", "2.3.0"))
    }

    @Test
    fun `remote version with higher major is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v3.0.0", "2.3.0"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(UpdateChecker.isNewerVersion("v2.3.0", "2.3.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.3.0", "2.3.0"))
    }

    @Test
    fun `older remote version is not newer`() {
        assertFalse(UpdateChecker.isNewerVersion("v2.2.0", "2.3.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.9.9", "2.3.0"))
    }
}
