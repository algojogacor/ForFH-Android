package com.aryariap.forfh.data.prefs

data class AlarmOffsets(
    val offset3h: Boolean,
    val offset2h: Boolean,
    val offset1h: Boolean,
) {
    /** Offsets aktif dalam menit, urutan terbesar dulu (3j → 2j → 1j). */
    fun activeOffsets(): List<Int> = buildList {
        if (offset3h) add(180)
        if (offset2h) add(120)
        if (offset1h) add(60)
    }
}
