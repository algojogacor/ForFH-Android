package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {

    class FakeAlarmApi(var exactAvailable: Boolean) : AlarmApi {
        val calls = mutableListOf<String>()
        val actions = mutableListOf<String?>()
        var lastExtras: Map<String, String>? = null
        var lastAction: String? = null

        override fun canScheduleExact(): Boolean = exactAvailable
        override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
            calls += "exact:$requestCode:$triggerAtMillis"
            lastExtras = extras
            actions += action
            lastAction = action
        }
        override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, action: String?, extras: Map<String, String>) {
            calls += "window:$requestCode:$triggerAtMillis:$windowLengthMillis"
            lastExtras = extras
            actions += action
            lastAction = action
        }
        override fun cancel(requestCode: Int, action: String?) {
            calls += "cancel:$requestCode"
            actions += action
            lastAction = action
        }
    }

    private fun classRow(trigger: Long, snoozeCount: Int = 0) = ScheduledAlarmEntity(
        id = "class|s1|120|2026-08-17",
        kind = "CLASS_ALARM",
        scheduleId = "s1",
        offsetMinutes = 120,
        occurrenceDate = "2026-08-17",
        triggerAtMillis = trigger,
        snoozeCount = snoozeCount,
    )

    private fun deadlineRow(trigger: Long) = ScheduledAlarmEntity(
        id = "taskdl|t1|2026-08-18",
        kind = "TASK_DEADLINE",
        scheduleId = null,
        offsetMinutes = 0,
        occurrenceDate = "2026-08-18",
        triggerAtMillis = trigger,
        snoozeCount = 0,
    )

    @Test
    fun `exact tersedia - setExactAndAllowWhileIdle dipanggil dengan requestCode stableHash`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        assertEquals(
            listOf("exact:${StableHash.of(row.id)}:1750000000000"),
            api.calls,
        )
    }

    @Test
    fun `exact dicabut - setWindow dengan windowLength 10 menit dari trigger`() {
        val api = FakeAlarmApi(exactAvailable = false)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        assertEquals(
            listOf("window:${StableHash.of(row.id)}:1750000000000:600000"),
            api.calls,
        )
    }

    @Test
    fun `restore exact - kembali setExactAndAllowWhileIdle dengan trigger tersimpan (snooze)`() {
        val api = FakeAlarmApi(exactAvailable = false)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L, snoozeCount = 2)
        scheduler.schedule(row)
        api.exactAvailable = true
        scheduler.schedule(row) // rescheduleAll setelah exact dikembalikan — trigger snooze dipertahankan
        assertEquals("exact:${StableHash.of(row.id)}:1750000000000", api.calls.last())
    }

    @Test
    fun `extras membawa identity fields - receiver tidak menebak-nebak`() {
        val api = FakeAlarmApi(exactAvailable = true)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L))
        val extras = api.lastExtras!!
        assertEquals("s1", extras["scheduleId"])
        assertEquals("120", extras["offsetMinutes"])
        assertEquals("2026-08-17", extras["occurrenceDate"])
        assertEquals("1750000000000", extras["triggerAtMillis"])
        // identity kelas tidak mengandung taskId → extra taskId tidak ada
        assertTrue(extras.containsKey("taskId").not())
    }

    @Test
    fun `kind CLASS_ALARM - PendingIntent action ACTION_CLASS_ALARM`() {
        val api = FakeAlarmApi(exactAvailable = true)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L))
        assertEquals(AlarmReceiver.ACTION_CLASS_ALARM, api.lastAction)
    }

    @Test
    fun `kind TASK_REMINDER - PendingIntent action ACTION_TASK_REMINDER`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val row = ScheduledAlarmEntity(
            id = "task|15|2026-08-17", kind = "TASK_REMINDER", scheduleId = null,
            offsetMinutes = 0, occurrenceDate = "2026-08-17",
            triggerAtMillis = 1_750_000_000_000L, snoozeCount = 0,
        )
        AlarmScheduler(api).schedule(row)
        assertEquals(AlarmReceiver.ACTION_TASK_REMINDER, api.lastAction)
        assertTrue(api.lastExtras!!.containsKey("taskId").not())
    }

    @Test
    fun `kind TASK_DEADLINE - action ACTION_TASK_DEADLINE dan extra taskId dari identity`() {
        val api = FakeAlarmApi(exactAvailable = true)
        AlarmScheduler(api).schedule(deadlineRow(trigger = 1_750_000_000_000L))
        assertEquals(AlarmReceiver.ACTION_TASK_DEADLINE, api.lastAction)
        val extras = api.lastExtras!!
        assertEquals("t1", extras["taskId"]) // receiver tahu tugas mana tanpa menebak-nebak
        assertEquals("2026-08-18", extras["occurrenceDate"])
    }

    @Test
    fun `kind tak dikenal - tanpa action - receiver no-op (else branch)`() {
        val api = FakeAlarmApi(exactAvailable = true)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L).copy(kind = "MISTERI"))
        assertEquals(null, api.lastAction)
    }

    @Test
    fun `cancel memakai action yang sama dengan schedule - PendingIntent matching butuh filterEquals sama`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val row = deadlineRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        scheduler.cancel(row)
        // schedule + cancel → action ACTION_TASK_DEADLINE dua kali (bila beda, cancel tidak kena)
        assertEquals(listOf(AlarmReceiver.ACTION_TASK_DEADLINE, AlarmReceiver.ACTION_TASK_DEADLINE), api.actions)
    }

    @Test
    fun `cancel memakai requestCode yang sama dengan schedule`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val row = classRow(trigger = 1_750_000_000_000L)
        scheduler.schedule(row)
        scheduler.cancel(row)
        assertEquals("cancel:${StableHash.of(row.id)}", api.calls.last())
    }

    @Test
    fun `fallback window tidak lebih kecil dari 10 menit`() {
        val api = FakeAlarmApi(exactAvailable = false)
        AlarmScheduler(api).schedule(classRow(trigger = 1_750_000_000_000L))
        assertTrue(api.calls.last().endsWith(":600000"))
    }

    @Test
    fun `reschedule - extras triggerAtMillis segar mengikuti trigger terbaru`() {
        val api = FakeAlarmApi(exactAvailable = true)
        val scheduler = AlarmScheduler(api)
        val rowA = classRow(trigger = 1_750_000_000_000L)
        val rowB = classRow(trigger = 1_760_000_000_000L)
        scheduler.schedule(rowA)
        scheduler.schedule(rowB) // id sama, trigger berubah (snooze/reschedule) — extras harus segar
        assertEquals("1760000000000", api.lastExtras!!["triggerAtMillis"])
    }
}
