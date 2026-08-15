package com.aryariap.forfh.alarm

/** State snooze ada di Room (snoozeCount row identity) — di sini hanya aturan murni (spec §8.6). */
object SnoozeCounter {
    const val MAX_SNOOZE = 5
    const val SNOOZE_MS = 3 * 60 * 1000L // +3 menit, WIB (epoch tak bergantung zona)

    fun canSnooze(snoozeCount: Int): Boolean = snoozeCount < MAX_SNOOZE

    fun nextTrigger(currentTriggerAtMillis: Long): Long = currentTriggerAtMillis + SNOOZE_MS

    fun nextCount(current: Int): Int = current + 1
}
