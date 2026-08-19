package com.aryariap.forfh.data.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogCatalogTest {

    @Test
    fun `static fallback contains all major versions`() {
        val entries = ChangelogCatalog.loadAll()
        assertTrue(entries.size >= 8)
        assertEquals("2.4.0", entries.first().version)
        assertEquals("1.0.0", entries.last().version)
    }

    @Test
    fun `getLatest returns v2_4_0`() {
        val latest = ChangelogCatalog.getLatest()
        assertEquals("2.4.0", latest.version)
        assertEquals(6, latest.versionCode)
        assertTrue(latest.highlights.isNotEmpty())
    }

    @Test
    fun `getForVersion retrieves correct entry by versionCode`() {
        val v240 = ChangelogCatalog.getForVersion(6)
        assertNotNull(v240)
        assertEquals("2.4.0", v240?.version)

        val v100 = ChangelogCatalog.getForVersion(1)
        assertNotNull(v100)
        assertEquals("1.0.0", v100?.version)

        val v110 = ChangelogCatalog.getForVersion(2)
        assertNotNull(v110)
        assertEquals("1.1.0", v110?.version)
    }

    @Test
    fun `getForVersionName retrieves correct entry by version name`() {
        val v240 = ChangelogCatalog.getForVersionName("2.4.0")
        assertNotNull(v240)
        assertEquals("2.4.0", v240?.version)

        val v230 = ChangelogCatalog.getForVersionName("v2.3.0")
        assertNotNull(v230)
        assertEquals("2.3.0", v230?.version)
    }
}
