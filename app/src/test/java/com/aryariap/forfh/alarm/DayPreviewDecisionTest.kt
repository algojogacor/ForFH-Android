package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Guard murni DAY_PREVIEW : murni, plain JUnit4 (pola DeadlineDecisionTest).
 * SEMUA cabang: row null (Ignore), trigger mismatch (Ignore),
 * logout (CancelSilently), valid (Fire).
 */
class DayPreviewDecisionTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 8, 17) // Senin

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    /** Extras valid: besok = 2026-08-18 (Selasa), trigger 2026-08-17 20:00 WIB. */
    private fun previewExtras(triggerMs: Long = wibEpoch(2026, 8, 17, 20, 0)) =
        DayPreviewExtras(occurrenceDate = "2026-08-18", triggerAtMillis = triggerMs)

    private fun row(triggerMs: Long = wibEpoch(2026, 8, 17, 20, 0)) = ScheduledAlarmEntity(
        id = "tmrw|2026-08-18", kind = "DAY_PREVIEW", scheduleId = null,
        offsetMinutes = 0, occurrenceDate = "2026-08-18", triggerAtMillis = triggerMs, snoozeCount = 0,
    )

    @Test
    fun `semua guard lolos - Fire`() {
        val action = DayPreviewDecision.decide(
            previewExtras(), row(), isLoggedIn = true,
        )
        assertEquals(DayPreviewAction.Fire, action)
    }

    @Test
    fun `row tidak ada - Ignore`() {
        assertEquals(
            DayPreviewAction.Ignore,
            DayPreviewDecision.decide(previewExtras(), row = null, isLoggedIn = true),
        )
    }

    @Test
    fun `trigger extras tidak cocok dengan row - Ignore`() {
        assertEquals(
            DayPreviewAction.Ignore,
            DayPreviewDecision.decide(
                previewExtras(triggerMs = wibEpoch(2026, 8, 17, 20, 0) - 60_000L),
                row(), isLoggedIn = true,
            ),
        )
    }

    @Test
    fun `logout - CancelSilently`() {
        assertEquals(
            DayPreviewAction.CancelSilently,
            DayPreviewDecision.decide(previewExtras(), row(), isLoggedIn = false),
        )
    }
}
