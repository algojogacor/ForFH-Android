package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {

    class FakeAlarmApi(var exactAvailable: Boolean) : AlarmApi {
        val calls = mutableListOf<String>()
        var lastExtras: Map<String, String>? = null

        override fun canScheduleExact(): Boolean = exactAvailable
        override fun setExactAndAllowWhileIdle(triggerAtMillis: Long, requestCode: Int, extras: Map<String, String>) {
            calls += "exact:$requestCode:$triggerAtMillis"
            lastExtras = extras
        }
        override fun setWindow(triggerAtMillis: Long, windowLengthMillis: Long, requestCode: Int, extras: Map<String, String>) {
            calls += "window:$requestCode:$triggerAtMillis:$windowLengthMillis"
            lastExtras = extras
        }
        override fun cancel(requestCode: Int) {
            calls += "cancel:$requestCode"
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
