package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmFlowExtras
import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import java.time.LocalTime
import java.time.ZonedDateTime

sealed interface AlarmOp {
    /** Pasang + simpan row (menimpa bila identity sama). */
    data class Schedule(val row: ScheduledAlarmEntity) : AlarmOp

    /** Cancel PendingIntent + hapus row. */
    data class Cancel(val row: ScheduledAlarmEntity) : AlarmOp

    /** Tidak disentuh (sudah benar / sesi snooze aktif dipertahankan). */
    data object Keep : AlarmOp
}

/**
 * Murni: menghitung operasi alarm dari Room — tidak menyentuh AlarmManager.
 * Invariant spec: row snooze aktif (snoozeCount > 0 && trigger future) TIDAK di-cancel/reset;
 * snoozeCount tidak pernah turun oleh proses lain.
 */
class ReconcilePlanner(private val planner: AlarmPlanner) {

    fun computeOps(
        current: List<ScheduledAlarmEntity>,
        schedules: List<ScheduleEntity>,
        offsetsByDay: Map<Int, List<Int>>,
        now: ZonedDateTime,
        fullRebuild: Boolean,
        skipDates: Set<String> = emptySet(),
    ): List<AlarmOp> {
        val desired = desiredRows(schedules, offsetsByDay, now, skipDates)
        val currentById = current.associateBy { it.id }
        val nowMs = now.toInstant().toEpochMilli()
        val ops = mutableListOf<AlarmOp>()

        for ((identity, row) in desired) {
            val existing = currentById[identity]
            ops += when {
                existing == null -> AlarmOp.Schedule(row)
                existing.snoozeCount > 0 && existing.triggerAtMillis > nowMs -> AlarmOp.Keep
                existing.snoozeCount == 0 && existing.triggerAtMillis == row.triggerAtMillis -> AlarmOp.Keep
                else -> AlarmOp.Schedule(row) // trigger lama beda (jadwal berubah) → timpa
            }
        }

        if (fullRebuild) {
            for (row in current) {
                val snoozed = row.snoozeCount > 0 && row.triggerAtMillis > nowMs
                // Pengecualian snooze-Keep: user eksplisit mematikan seluruh alarm tanggal itu
                // ("Matikan seluruh alarm hari ini") → row snooze hari itu ikut di-cancel.
                val mutedDay = row.kind == "CLASS_ALARM" && row.occurrenceDate in skipDates
                if (row.id !in desired && (!snoozed || mutedDay)) ops += AlarmOp.Cancel(row)
            }
        }
        return ops
    }

    private fun desiredRows(
        schedules: List<ScheduleEntity>,
        offsetsByDay: Map<Int, List<Int>>,
        now: ZonedDateTime,
        skipDates: Set<String>,
    ): Map<String, ScheduledAlarmEntity> {
        // Aturan pengguna: alarm kelas HANYA untuk kuliah pertama hari itu (start paling awal).
        // Saat kuliah pertama mulai user masih di rumah (mungkin tidur); kuliah berikutnya ia
        // sudah pasti bangun & di kampus → tidak perlu alarm apa pun (bukan juga notif).
        val firstStartByDay = schedules
            .filter { it.enabled }
            .groupBy { it.dayOfWeek }
            .mapValues { (_, list) -> list.minOf { LocalTime.parse(it.startTime) } }

        val result = mutableMapOf<String, ScheduledAlarmEntity>()
        for (s in schedules) {
            if (!s.enabled) continue // caller pakai getEnabledOnce; filter di sini utk pure function yang tahan input enabled=false
            if (LocalTime.parse(s.startTime) != firstStartByDay[s.dayOfWeek]) continue // bukan kuliah pertama → tanpa alarm
            for (offset in offsetsByDay[s.dayOfWeek].orEmpty()) { // per hari: daftar offset harinya sendiri
                val occ = planner.nextClassOccurrence(s.id, s.dayOfWeek, s.startTime, offset, now)
                val occDate = occ.occurrenceDate.toString()
                if (occDate in skipDates) continue // user mematikan seluruh alarm tanggal itu
                result[occ.identity] = ScheduledAlarmEntity(
                    id = occ.identity,
                    kind = "CLASS_ALARM",
                    scheduleId = s.id,
                    offsetMinutes = offset,
                    occurrenceDate = occ.occurrenceDate.toString(),
                    triggerAtMillis = occ.triggerAtMillis,
                    snoozeCount = 0,
                )
            }
        }
        // Deviasi T8: slot tugas di-own di AlarmFlowExtras sejak T7 fix round — satu sumber kebenaran,
        // bukan companion TASK_SLOTS sendiri (menghindari dua owner konstanta yang bisa melenceng).
        for (slot in AlarmFlowExtras.TASK_SLOTS) {
            val (date, trigger) = planner.nextTaskSlot(slot, now)
            val identity = AlarmPlanner.taskIdentity(slot, date)
            result[identity] = ScheduledAlarmEntity(
                id = identity,
                kind = "TASK_REMINDER",
                scheduleId = null,
                offsetMinutes = 0,
                occurrenceDate = date.toString(),
                triggerAtMillis = trigger,
                snoozeCount = 0,
            )
        }
        return result
    }
}
