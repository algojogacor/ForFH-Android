package com.aryariap.forfh.alarm

/**
 * Extras parsed from DAY_PREVIEW intent : murni, tanpa Android.
 */
data class DayPreviewExtras(
    val occurrenceDate: String, // tanggal besok yang direview (yyyy-MM-dd)
    val triggerAtMillis: Long,
)
