package com.aryariap.forfh.sync

import com.aryariap.forfh.alarm.AlarmPlanner
import com.aryariap.forfh.alarm.AlarmScheduler
import com.aryariap.forfh.data.db.ScheduledAlarmsDao
import com.aryariap.forfh.data.db.SchedulesDao
import com.aryariap.forfh.data.prefs.Preferences
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.ZonedDateTime

/** Seam untuk lapisan UI/ViewModel: reschedule penuh dari state Room + prefs (pola mute/unmute). */
interface RescheduleAll {
    suspend fun rescheduleAll()
}

class AlarmRescheduler(
    private val planner: AlarmPlanner,
    private val scheduler: AlarmScheduler,
    private val alarmsDao: ScheduledAlarmsDao,
    private val schedulesDao: SchedulesDao,
    private val prefs: Preferences,
    /** Task 4: dipanggil setelah execute() selesai (refresh widget; default no-op agar test lama utuh). */
    private val onAlarmsChanged: suspend () -> Unit = {},
) : RescheduleAll {
    private val zone = ZoneId.of("Asia/Jakarta")

    /** Cancel semua yang obsolete lalu bangun ulang; sesi snooze aktif dipertahankan apa adanya (§8.1). */
    override suspend fun rescheduleAll() = execute(compute(fullRebuild = true))

    /**
     * Boot/MY_PACKAGE_REPLACED (§8.9): AlarmManager KOSONG setelah reboot — re-arm SEMUA row Room.
     * Idempotent: identity sama → requestCode sama (StableHash) → setExact mengganti, tidak ganda.
     * Snooze dipertahankan apa adanya: schedule() memakai triggerAtMillis tersimpan, snoozeCount
     * tak disentuh, Room tak di-write (§8.1). (Fix round final review: sebelumnya memakai
     * computeOps(fullRebuild=false) yang mengeluarkan Keep utk row cocok → alarm mati diam-diam
     * sampai occurrence bergeser atau jadwal berubah di server.)
     */
    suspend fun reconcile() {
        alarmsDao.getAllOnce().forEach { scheduler.schedule(it) }
    }

    /** Pasang satu row (snooze, reschedule setelah restore exact) — tidak menyentuh row lain. */
    suspend fun scheduleRow(row: com.aryariap.forfh.data.db.ScheduledAlarmEntity) {
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }

    /** Cancel + hapus row identity (dipakai guard SkipCancel & slot tugas selesai). */
    suspend fun cancelAlarm(identity: String) {
        alarmsDao.getByIdOnce(identity)?.let { scheduler.cancel(it) }
        alarmsDao.deleteById(identity)
    }

    /** Logout §8.10: cancel seluruh alarm tanpa menghapus row (row dihapus terpisah). */
    suspend fun cancelAll() {
        alarmsDao.getAllOnce().forEach { scheduler.cancel(it) }
    }

    /** Pola one-shot tugas: hapus row hari ini, pasang row besok (§8.7). */
    suspend fun replaceTaskSlotRow(slotHour: Int, now: ZonedDateTime) {
        val (date, trigger) = planner.nextTaskSlot(slotHour, now)
        val identity = AlarmPlanner.taskIdentity(slotHour, date)
        val row = com.aryariap.forfh.data.db.ScheduledAlarmEntity(
            id = identity,
            kind = "TASK_REMINDER",
            scheduleId = null,
            offsetMinutes = 0,
            occurrenceDate = date.toString(),
            triggerAtMillis = trigger,
            snoozeCount = 0,
        )
        alarmsDao.upsert(row)
        scheduler.schedule(row)
    }

    private suspend fun compute(fullRebuild: Boolean): List<AlarmOp> {
        val now = ZonedDateTime.now(zone)
        return ReconcilePlanner(planner).computeOps(
            current = alarmsDao.getAllOnce(),
            schedules = schedulesDao.getEnabledOnce(),
            offsetsByDay = prefs.offsets.first().perDay,
            now = now,
            fullRebuild = fullRebuild,
            // "Matikan seluruh alarm hari ini": tanggal disimpan DataStore, alarm kelas hari itu
            // ditiadakan + row snooze aktif ikut di-cancel; basi otomatis saat ganti hari.
            skipDates = prefs.mutedDate.first()?.let { setOf(it) } ?: emptySet(),
        )
    }

    private suspend fun execute(ops: List<AlarmOp>) {
        for (op in ops) {
            when (op) {
                is AlarmOp.Schedule -> {
                    alarmsDao.upsert(op.row)
                    scheduler.schedule(op.row)
                }
                is AlarmOp.Cancel -> {
                    scheduler.cancel(op.row)
                    alarmsDao.deleteById(op.row.id)
                }
                AlarmOp.Keep -> Unit
            }
        }
        // Task 4: SEMUA jalur reschedule penuh (sync sukses, mute/unmute, ubah offset,
        // exact-restore) melewati execute, refresh widget di sini. SyncWorker memanggil
        // rescheduleAll() saat sync sukses, jadi titik "sync sukses" plan T4 tercakup di sini.
        // onAlarmsChanged = refreshAll yang menelan kegagalannya sendiri: refresh widget
        // tidak pernah menggagalkan alur alarm.
        onAlarmsChanged()
    }
}
