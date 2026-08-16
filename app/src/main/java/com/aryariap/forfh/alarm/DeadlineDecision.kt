package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity
import com.aryariap.forfh.data.db.TaskEntity
import java.time.LocalDate

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
 * jalur CLASS_ALARM): row ada + trigger cocok + logged-in + task masih aktif → Fire;
 * logout / tugas hilang / DONE → CancelSilently; row tak ada / extras basi → Ignore.
 * Satu-satunya tempat keputusan handler — semantik identik dengan handler sebelum pemurnian,
 * kini bisa di-unit-test (DeadlineDecisionTest, plain JUnit4).
 */
object DeadlineDecision {
    fun decide(
        extras: DeadlineExtras,
        row: ScheduledAlarmEntity?,
        isLoggedIn: Boolean,
        task: TaskEntity?,
        today: LocalDate,
    ): DeadlineAction {
        if (row == null) return DeadlineAction.Ignore // identity tak ada → intent stale, jangan sentuh
        if (row.triggerAtMillis != extras.triggerAtMillis) return DeadlineAction.Ignore // extras basi
        if (!isLoggedIn) return DeadlineAction.CancelSilently // defense-in-depth pasca-logout
        if (task == null || task.status == "DONE") return DeadlineAction.CancelSilently // tugas hilang/selesai di sync
        val deadlineDay = LocalDate.parse(extras.occurrenceDate)
        return DeadlineAction.Fire(TaskDeadlineText.build(task, deadlineDay, today))
    }
}
