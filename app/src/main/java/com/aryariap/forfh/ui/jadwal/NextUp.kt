package com.aryariap.forfh.ui.jadwal

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.data.db.ScheduleEntity
import java.time.ZonedDateTime

/**
 * Kelas berikutnya (ruling R5, Task 1/4): earliest upcoming class START di semua schedule
 * enabled, null hanya bila tidak ada schedule enabled. Satu sumber kebenaran untuk kartu
 * "Berikutnya" (NextUpViewModel) dan widget jadwal (ForfhWidget).
 *
 * Semantik: tiap schedule dihitung via AlarmPlanner.nextClassOccurrence(offset=0) → startDateTime
 * (hari ini bila belum lewat, minggu depan bila sudah), lalu diambil yang paling awal. skipDates
 * dan mute TIDAK diterapkan (murni tampilan). Pemanggil menyerahkan schedule enabled saja.
 */
fun nextUp(
    schedules: List<ScheduleEntity>,
    now: ZonedDateTime,
): Pair<ScheduleEntity, ZonedDateTime>? =
    schedules
        .map { schedule ->
            schedule to PLANNER.nextClassOccurrence(
                schedule.id, schedule.dayOfWeek, schedule.startTime, 0, now,
            ).startDateTime
        }
        .minByOrNull { it.second }

/** Planner WIB (zone default AlarmPlanner) — cocok dengan now WIB dari semua pemanggil. */
private val PLANNER = AlarmPlanner()
