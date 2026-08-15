package com.aryariap.forfh.data.prefs

/**
 * Offset alarm PER HARI dalam seminggu — tiap hari daftar menit bebas sendiri
 * (request user 2026-08-15: bukan toggle tetap 3j/2j/1j; bisa nilai apa pun,
 * mis. Senin cuma [90, 60] karena masuk pagi, Rabu boleh [240, 180, 120, 90, 60]).
 * Key map: dayOfWeek konvensi API ForFH 0=Minggu .. 6=Sabtu.
 */
data class AlarmOffsets(
    val perDay: Map<Int, List<Int>>,
) {
    /** Daftar menit aktif untuk satu hari, urutan terbesar dulu. Hari tanpa entri = tidak ada alarm. */
    fun offsetsFor(dayOfWeek: Int): List<Int> =
        perDay[dayOfWeek].orEmpty().sortedDescending()

    companion object {
        const val MIN_OFFSET_MINUTES = 1
        const val MAX_OFFSET_MINUTES = 720 // 12 jam — lebih dari itu absurd (menembus ke hari sebelumnya)

        /** Default 3j/2j/1j — perilaku versi toggle sebelumnya (migrasi tanpa kehilangan). */
        val DEFAULT_OFFSETS = listOf(180, 120, 60)

        fun defaults(): AlarmOffsets = AlarmOffsets((0..6).associateWith { DEFAULT_OFFSETS })

        /** Migrasi dari toggle lama (offset_3h/2h/1h) → daftar yang sama untuk semua hari. */
        fun fromLegacy(offset3h: Boolean, offset2h: Boolean, offset1h: Boolean): AlarmOffsets {
            val base = buildList {
                if (offset3h) add(180)
                if (offset2h) add(120)
                if (offset1h) add(60)
            }
            return AlarmOffsets((0..6).associateWith { base })
        }
    }
}

/** "1 j 13 m" — format chip & preview dialog. Murni, bisa unit-test. */
fun formatOffsetMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m m"
        m == 0 -> "$h j"
        else -> "$h j $m m"
    }
}
