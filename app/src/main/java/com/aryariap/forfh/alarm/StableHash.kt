package com.aryariap.forfh.alarm

object StableHash {
    /**
     * RequestCode PendingIntent & notificationId — deterministic lintas proses
     * (String.hashCode dijamin spesifikasi Java), non-negatif (notificationId wajib >= 0).
     */
    fun of(identity: String): Int = identity.hashCode() and 0x7FFFFFFF
}
