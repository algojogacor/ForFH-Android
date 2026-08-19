package com.aryariap.forfh.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatWidgetRoomTest {

    @Test
    fun `ruang panjang dengan gedung disingkat ke kode ruangan`() {
        assertEquals("R. LG02 B", formatWidgetRoom("Ruang Kelas AP - LG02 B - A.G. Pringgodigdo (3.06)"))
    }

    @Test
    fun `ruang standar dengan prefix Ruang disingkat`() {
        assertEquals("R. 301", formatWidgetRoom("Ruang 301 - Gedung A"))
        assertEquals("R. 302", formatWidgetRoom("Ruang Kelas 302"))
    }

    @Test
    fun `ruang daring atau online dipertahankan ringkas`() {
        assertEquals("Daring", formatWidgetRoom("Daring (Zoom Meeting)"))
        assertEquals("Daring", formatWidgetRoom("Online via HEBAT"))
    }

    @Test
    fun `ruang null atau kosong mengembalikan null`() {
        assertNull(formatWidgetRoom(null))
        assertNull(formatWidgetRoom(""))
        assertNull(formatWidgetRoom("   "))
    }
}
