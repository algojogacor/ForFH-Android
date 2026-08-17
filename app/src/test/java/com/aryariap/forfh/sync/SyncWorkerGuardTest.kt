package com.aryariap.forfh.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for SyncWorker.shouldSkipSync guard.
 *
 * Verifies that sync (periodic/one-shot) is skipped when the user is logged out,
 * while reconcile runs regardless of login state, and logged-in users always proceed.
 */
class SyncWorkerGuardTest {

    @Test
    fun `logged out plus sync mode returns true`() {
        val skip = SyncWorker.shouldSkipSync(isLoggedIn = false, mode = "sync")
        assertTrue(skip)
    }

    @Test
    fun `logged out plus reconcile mode returns false`() {
        val skip = SyncWorker.shouldSkipSync(isLoggedIn = false, mode = "reconcile")
        assertFalse(skip)
    }

    @Test
    fun `logged in plus any mode returns false`() {
        assertFalse(SyncWorker.shouldSkipSync(isLoggedIn = true, mode = "sync"))
        assertFalse(SyncWorker.shouldSkipSync(isLoggedIn = true, mode = "reconcile"))
        assertFalse(SyncWorker.shouldSkipSync(isLoggedIn = true, mode = null))
    }
}
