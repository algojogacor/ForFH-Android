package com.aryariap.forfh.data.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogCatalogTest {

    @Test
    fun `static fallback contains all major versions`() {
        val entries = ChangelogCatalog.loadAll()
        assertTrue(entries.isNotEmpty())
        assertEquals("2.3.0", entries.first().version)
    }

    @Test
    fun `getLatest returns v2_3_0`() {
        val latest = ChangelogCatalog.getLatest()
        assertEquals("2.3.0", latest.version)
        assertEquals(5, latest.versionCode)
        assertTrue(latest.highlights.isNotEmpty())
    }

    @Test
    fun `getForVersion retrieves correct entry by versionCode`() {
        val v220 = ChangelogCatalog.getForVersion(4)
        assertNotNull(v220)
        assertEquals("2.2.0", v220?.version)

        val v210 = ChangelogCatalog.getForVersion(3)
        assertNotNull(v210)
        assertEquals("2.1.0", v210?.version)
    }
}
