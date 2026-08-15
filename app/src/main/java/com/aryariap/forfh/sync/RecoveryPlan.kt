package com.aryariap.forfh.sync

object RecoveryPlan {
    enum class Mode { RECONCILE }

    /**
     * BOOT_COMPLETED dikirim hanya setelah unlock pertama (receiver non-directBootAware) —
     * deferred path: sebelum unlock, storage credential-encrypted tak bisa dibaca, tidak ada
     * alarm yang di-rebuild dan tidak ada yang tampil; begitu unlock, reconcile otomatis (spec §8.9).
     */
    fun modeFor(action: String?): Mode? = when (action) {
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        -> Mode.RECONCILE
        else -> null
    }
}
