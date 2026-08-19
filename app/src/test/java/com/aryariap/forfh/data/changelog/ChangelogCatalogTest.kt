package com.aryariap.forfh.data.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogCatalogTest {

    @Test
    fun `static fallback contains all major versions`() {
        val entries = ChangelogCatalog.loadAll()
        assertTrue(entries.size >= 9)
        assertEquals("2.5.0", entries.first().version)
        assertEquals("1.0.0", entries.last().version)
    }

    @Test
    fun `getLatest returns v2_5_0`() {
        val latest = ChangelogCatalog.getLatest()
        assertEquals("2.5.0", latest.version)
        assertEquals(7, latest.versionCode)
        assertTrue(latest.highlights.isNotEmpty())
    }

    @Test
    fun `getForVersion retrieves correct entry by versionCode`() {
        val v250 = ChangelogCatalog.getForVersion(7)
        assertNotNull(v250)
        assertEquals("2.5.0", v250?.version)

        val v240 = ChangelogCatalog.getForVersion(6)
        assertNotNull(v240)
        assertEquals("2.4.0", v240?.version)

        val v100 = ChangelogCatalog.getForVersion(1)
        assertNotNull(v100)
        assertEquals("1.0.0", v100?.version)
    }

    @Test
    fun `getForVersionName retrieves correct entry by version name`() {
        val v250 = ChangelogCatalog.getForVersionName("2.5.0")
        assertNotNull(v250)
        assertEquals("2.5.0", v250?.version)

        val v240 = ChangelogCatalog.getForVersionName("v2.4.0")
        assertNotNull(v240)
        assertEquals("2.4.0", v240?.version)
    }
}
