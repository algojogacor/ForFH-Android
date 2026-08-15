package com.aryariap.forfh.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookiePayloadCodecTest {

    @Test
    fun `roundtrip peta cookie`() {
        val map = mapOf(
            "usual-olwen-algojogacorbgt-a2be655b.koyeb.app" to "__Host-forfh-session=abc123; Path=/",
            "koyeb.app" to "session=xyz",
        )
        assertEquals(map, CookiePayloadCodec.decode(CookiePayloadCodec.encode(map)))
    }

    @Test
    fun `encode peta kosong menghasilkan string kosong`() {
        assertEquals("", CookiePayloadCodec.encode(emptyMap()))
    }

    @Test
    fun `decode string kosong menghasilkan peta kosong`() {
        assertTrue(CookiePayloadCodec.decode("").isEmpty())
        assertTrue(CookiePayloadCodec.decode("\n\n").isEmpty())
    }

    @Test
    fun `baris tanpa tab dibuang`() {
        val decoded = CookiePayloadCodec.decode("baris-sampah\nhost\tvalue")
        assertEquals(mapOf("host" to "value"), decoded)
    }
}
