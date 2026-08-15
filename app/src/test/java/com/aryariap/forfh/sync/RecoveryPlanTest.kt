package com.aryariap.forfh.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryPlanTest {

    @Test
    fun `BOOT_COMPLETED dan MY_PACKAGE_REPLACED memicu reconcile`() {
        assertEquals(RecoveryPlan.Mode.RECONCILE, RecoveryPlan.modeFor("android.intent.action.BOOT_COMPLETED"))
        assertEquals(RecoveryPlan.Mode.RECONCILE, RecoveryPlan.modeFor("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun `action lain tidak memicu apa pun`() {
        assertNull(RecoveryPlan.modeFor("com.aryariap.forfh.action.CLASS_ALARM"))
        assertNull(RecoveryPlan.modeFor(null))
    }
}
