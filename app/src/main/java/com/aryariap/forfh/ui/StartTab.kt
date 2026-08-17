package com.aryariap.forfh.ui

/**
 * Maps intent extras to a tab index for deep-link from widget/notification.
 * open_tab: Int (0=Jadwal, 1=Tugas, 2=Info, 3=Atur). Sentinel -1 = not provided.
 * Legacy: open_tasks=true maps to tab 1 (Tugas).
 */
object StartTab {
    /**
     * @param openTasks legacy extra, true → tab 1
     * @param openTab open_tab extra, -1 sentinel means not provided
     * @return tab index 0..3, or null if no directive
     */
    fun fromIntent(openTasks: Boolean, openTab: Int): Int? = when {
        openTasks -> 1
        openTab in 0..3 -> openTab
        else -> null
    }
}
