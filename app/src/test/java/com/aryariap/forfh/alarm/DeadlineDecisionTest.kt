package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Guard berlapis TASK_DEADLINE — murni, plain JUnit4 (pola ReceiverGuardTest).
 * SEMUA cabang: fire one-shot, row-absent (Ignore), trigger basi (Ignore),
 * logout (CancelSilently), tugas hilang (CancelSilently), tugas DONE (CancelSilently).
 */
class DeadlineDecisionTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 8, 17)

    private fun wibEpoch(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    /** Extras valid: deadline Selasa 18, trigger Senin 17 20:00 WIB. */
    private fun deadlineExtras(triggerMs: Long = wibEpoch(2026, 8, 17, 20, 0)) =
        DeadlineExtras(taskId = "t1", occurrenceDate = "2026-08-18", triggerAtMillis = triggerMs)

    private fun row(triggerMs: Long = wibEpoch(2026, 8, 17, 20, 0)) = ScheduledAlarmEntity(
        id = "taskdl|t1|2026-08-18", kind = "TASK_DEADLINE", scheduleId = null,
        offsetMinutes = 0, occurrenceDate = "2026-08-18", triggerAtMillis = triggerMs, snoozeCount = 0,
    )

    private fun task(status: String = "NOT_STARTED") = TaskEntity(
        id = "t1", courseId = null, courseName = "Hukum", courseCode = null, title = "Makalah",
        description = null, dueAt = wibEpoch(2026, 8, 18, 23, 59), status = status, computedStatus = null,
        priority = "medium", courseColor = null, subtasksJson = null,
    )

    @Test
    fun `semua guard lolos - Fire dengan teks TaskDeadlineText (hint deadline besok)`() {
        val action = DeadlineDecision.decide(deadlineExtras(), row(), isLoggedIn = true, task = task(), today)
            as DeadlineAction.Fire
        // Fire membawa output TaskDeadlineText — teks di-pin persis (integrasi decision ↔ text)
        assertEquals("📚 Hukum: Makalah — deadline besok", action.text)
    }

    @Test
    fun `row tidak ada - Ignore - intent stale tidak menyentuh apa pun`() {
        assertEquals(
            DeadlineAction.Ignore,
            DeadlineDecision.decide(deadlineExtras(), row = null, isLoggedIn = true, task = task(), today),
        )
    }

    @Test
    fun `trigger extras tidak cocok dengan row - Ignore`() {
        assertEquals(
            DeadlineAction.Ignore,
            DeadlineDecision.decide(
                deadlineExtras(triggerMs = wibEpoch(2026, 8, 17, 20, 0) - 60_000L),
                row(), isLoggedIn = true, task = task(), today,
            ),
        )
    }

    @Test
    fun `logout - CancelSilently - one-shot dikonsumsi`() {
        assertEquals(
            DeadlineAction.CancelSilently,
            DeadlineDecision.decide(deadlineExtras(), row(), isLoggedIn = false, task = task(), today),
        )
    }

    @Test
    fun `tugas hilang dari sync - CancelSilently`() {
        assertEquals(
            DeadlineAction.CancelSilently,
            DeadlineDecision.decide(deadlineExtras(), row(), isLoggedIn = true, task = null, today),
        )
    }

    @Test
    fun `tugas berstatus DONE - CancelSilently`() {
        assertEquals(
            DeadlineAction.CancelSilently,
            DeadlineDecision.decide(deadlineExtras(), row(), isLoggedIn = true, task = task(status = "DONE"), today),
        )
    }
}
