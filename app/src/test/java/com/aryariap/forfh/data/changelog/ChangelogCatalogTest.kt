package com.aryariap.forfh.data.changelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogCatalogTest {

    @Test
    fun `static fallback contains all major versions`() {
        val entries = ChangelogCatalog.loadAll()
        assertTrue(entries.size >= 7)
        assertEquals("2.3.0", entries.first().version)
        assertEquals("1.0.0", entries.last().version)
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
        val v100 = ChangelogCatalog.getForVersion(1)
        assertNotNull(v100)
        assertEquals("1.0.0", v100?.version)

        val v110 = ChangelogCatalog.getForVersion(2)
        assertNotNull(v110)
        assertEquals("1.1.0", v110?.version)

        val v120 = ChangelogCatalog.getForVersion(3)
        assertNotNull(v120)
        assertEquals("1.2.0", v120?.version)
    }

    @Test
    fun `getForVersionName retrieves correct entry by version name`() {
        val v210 = ChangelogCatalog.getForVersionName("2.1.0")
        assertNotNull(v210)
        assertEquals("2.1.0", v210?.version)

        val v200 = ChangelogCatalog.getForVersionName("v2.0.0")
        assertNotNull(v200)
        assertEquals("2.0.0", v200?.version)
    }
}
