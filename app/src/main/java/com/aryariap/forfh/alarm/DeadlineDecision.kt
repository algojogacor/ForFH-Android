package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Hasil keputusan notifikasi deadline H-1 — murni, tanpa Android (pola ReceiverGuard.evaluate). */
sealed interface DeadlineAction {
    /** Notif biasa (bukan full-screen) ditampilkan, lalu row dihapus (one-shot per plan). */
    data class Fire(val text: String) : DeadlineAction

    /** Row dihapus TANPA menampilkan apa pun (one-shot terkonsumsi: logout / tugas hilang / selesai). */
    data object CancelSilently : DeadlineAction

    /** Tidak menyentuh apa pun (intent stale: row tak ada / trigger tak cocok). */
    data object Ignore : DeadlineAction
}

/**
 * Guard berlapis TASK_DEADLINE — pola TASK_REMINDER (tidak lewat ReceiverGuard yang khusus
 * jalur CLASS_ALARM): row ada + trigger cocok + logged-in + task masih aktif + deadline task
 * MASIH cocok dengan row → Fire; logout / tugas hilang / DONE / due date bergeser →
 * CancelSilently; row tak ada / extras basi → Ignore.
 * Satu-satunya tempat keputusan handler — murni dan bisa di-unit-test
 * (DeadlineDecisionTest, plain JUnit4).
 */
object DeadlineDecision {
    fun decide(
        extras: DeadlineExtras,
        row: ScheduledAlarmEntity?,
        isLoggedIn: Boolean,
        task: TaskEntity?,
        today: LocalDate,
        zone: ZoneId = ZoneId.of("Asia/Jakarta"),
    ): DeadlineAction {
        if (row == null) return DeadlineAction.Ignore // identity tak ada → intent stale, jangan sentuh
        if (row.triggerAtMillis != extras.triggerAtMillis) return DeadlineAction.Ignore // extras basi
        if (!isLoggedIn) return DeadlineAction.CancelSilently // defense-in-depth pasca-logout
        if (task == null || task.status == "DONE") return DeadlineAction.CancelSilently // tugas hilang/selesai di sync
        // Stale row: due date task (epoch ms → tanggal WIB, konversi sama dgn TaskDeadlinePlanner)
        // tidak lagi sama dgn occurrenceDate row — deadline dipindah setelah row di-arm. Notif versi
        // lama tidak boleh tampil → cancel tanpa tampil; Reconcile membangun ulang utk tanggal baru.
        val deadlineDay = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        if (deadlineDay != LocalDate.parse(row.occurrenceDate)) return DeadlineAction.CancelSilently
        val day = LocalDate.parse(extras.occurrenceDate)
        return DeadlineAction.Fire(TaskDeadlineText.build(task, day, today))
    }
}
