package com.aryariap.forfh.data.db

import java.time.Instant

object DueDateParser {
    /** Server mengirim timestamp sebagai ISO-8601 (mis. "2026-08-20T03:00:00.000Z") atau epoch ms. Keduanya → epoch millis. */
    fun parseToEpochMillis(value: String?): Long? {
        if (value == null) return null
        return runCatching {
            if (value.all { it.isDigit() }) value.toLong() else Instant.parse(value).toEpochMilli()
        }.getOrNull()
    }
}
