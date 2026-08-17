package com.aryariap.forfh.alarm

import com.aryariap.forfh.data.db.ScheduledAlarmEntity

/** Hasil keputusan notifikasi DAY_PREVIEW H-1 — murni, tanpa Android (pola DeadlineDecision). */
sealed interface DayPreviewAction {
    /** Row valid, logged-in → Fire (tampilkan notif). */
    data object Fire : DayPreviewAction

    /** Row dihapus TANPA menampilkan apa pun (logout). */
    data object CancelSilently : DayPreviewAction

    /** Tidak menyentuh apa pun (intent stale: row tak ada / trigger tak cocok). */
    data object Ignore : DayPreviewAction
}

/**
 * Guard DAY_PREVIEW — murni dan bisa di-unit-test (DayPreviewDecisionTest, plain JUnit4).
 * Row ada + trigger cocok + logged-in → Fire; logout → CancelSilently;
 * row tak ada / extras basi → Ignore.
 */
object DayPreviewDecision {
    fun decide(
        extras: DayPreviewExtras,
        row: ScheduledAlarmEntity?,
        isLoggedIn: Boolean,
    ): DayPreviewAction {
        if (row == null) return DayPreviewAction.Ignore // identity tak ada → intent stale
        if (row.triggerAtMillis != extras.triggerAtMillis) return DayPreviewAction.Ignore // extras basi
        if (!isLoggedIn) return DayPreviewAction.CancelSilently // defense-in-depth pasca-logout
        return DayPreviewAction.Fire
    }
}
