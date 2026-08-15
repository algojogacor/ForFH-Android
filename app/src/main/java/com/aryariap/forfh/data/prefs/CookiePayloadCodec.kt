package com.aryariap.forfh.data.prefs

object CookiePayloadCodec {
    /** Encode peta host→cookie jadi satu string (baris "host\tvalue"), sebelum dienkripsi. */
    fun encode(map: Map<String, String>): String =
        map.entries.joinToString("\n") { "${it.key}\t${it.value}" }

    fun decode(payload: String): Map<String, String> = payload
        .split('\n')
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }
        .toMap()
}
