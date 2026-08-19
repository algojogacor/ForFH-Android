package com.aryariap.forfh.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `remote version with higher minor is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v2.5.0", "2.4.0"))
        assertTrue(UpdateChecker.isNewerVersion("2.5.0", "2.4.0"))
    }

    @Test
    fun `remote version with higher patch is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v2.4.1", "2.4.0"))
    }

    @Test
    fun `remote version with higher major is newer`() {
        assertTrue(UpdateChecker.isNewerVersion("v3.0.0", "2.4.0"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(UpdateChecker.isNewerVersion("v2.4.0", "2.4.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.4.0", "2.4.0"))
    }

    @Test
    fun `older remote version is not newer`() {
        assertFalse(UpdateChecker.isNewerVersion("v2.3.0", "2.4.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.9.9", "2.4.0"))
    }

    @Test
    fun `parseReleaseHighlights extracts clean bullet points from markdown`() {
        val markdown = """
            # Release v2.4.0
            Apa yang baru:
            - **Pratinjau Online:** Intip daftar perubahan langsung di app.
            - `Halaman Unduh`: Desain Paper & Ink baru.
            * Unduhan APK langsung aktif tanpa kendala.
            
            ---
            Nikmati pembaruan terbaru!
        """.trimIndent()

        val highlights = UpdateChecker.parseReleaseHighlights(markdown)
        assertEquals(3, highlights.size)
        assertEquals("Pratinjau Online: Intip daftar perubahan langsung di app.", highlights[0])
        assertEquals("Halaman Unduh: Desain Paper & Ink baru.", highlights[1])
        assertEquals("Unduhan APK langsung aktif tanpa kendala.", highlights[2])
    }

    @Test
    fun `parseReleaseHighlights handles empty or null gracefully`() {
        assertTrue(UpdateChecker.parseReleaseHighlights(null).isEmpty())
        assertTrue(UpdateChecker.parseReleaseHighlights("").isEmpty())
        assertTrue(UpdateChecker.parseReleaseHighlights("   ").isEmpty())
    }
}
