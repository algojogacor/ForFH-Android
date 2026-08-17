package com.aryariap.forfh.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test for StartTab.fromIntent mapping logic (V1.2 Task 5).
 * 4 cases: valid open_tab, invalid open_tab, legacy open_tasks, none.
 */
class StartTabTest {

    @Test
    fun `valid open_tab 0 returns 0`() {
        assertEquals(0, StartTab.fromIntent(openTasks = false, openTab = 0))
    }

    @Test
    fun `valid open_tab 1 returns 1`() {
        assertEquals(1, StartTab.fromIntent(openTasks = false, openTab = 1))
    }

    @Test
    fun `valid open_tab 2 returns 2`() {
        assertEquals(2, StartTab.fromIntent(openTasks = false, openTab = 2))
    }

    @Test
    fun `valid open_tab 3 returns 3`() {
        assertEquals(3, StartTab.fromIntent(openTasks = false, openTab = 3))
    }

    @Test
    fun `invalid open_tab outside 0-3 returns legacy open_tasks or null`() {
        // openTab=-1 sentinel → falls back to openTasks=false → null
        assertNull(StartTab.fromIntent(openTasks = false, openTab = -1))
        // openTab=4 → invalid, but openTasks=false → null
        assertNull(StartTab.fromIntent(openTasks = false, openTab = 4))
        // openTab=-2 → invalid, openTasks=false → null
        assertNull(StartTab.fromIntent(openTasks = false, openTab = -2))
    }

    @Test
    fun `legacy open_tasks true returns 1 regardless of openTab sentinel`() {
        // openTab=-1 is sentinel (not provided), openTasks=true should win
        assertEquals(1, StartTab.fromIntent(openTasks = true, openTab = -1))
        // openTab=0 is valid but openTasks legacy should win
        assertEquals(1, StartTab.fromIntent(openTasks = true, openTab = 0))
    }

    @Test
    fun `no open_tasks and no valid open_tab returns null`() {
        assertNull(StartTab.fromIntent(openTasks = false, openTab = -1))
    }
}
