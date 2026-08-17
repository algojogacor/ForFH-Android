package com.aryariap.forfh.widget

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pin regresi anti-clip widget (bug lama "FHK25 terpotong"): heuristik ukuran font
 * compactNameFontSize/standardNameFontSize harus mengecil tepat di batas baris terestimasi
 * (lihat KDoc fungsinya di ForfhWidget.kt; budget tinggi compact 65.75dp, standard 44.75dp).
 * Murni JVM (TextUnit value class, tanpa Robolectric) — pola sama dengan UiFormatTest.
 */
class WidgetNameFontSizeTest {

    // compact: 15sp sampai 45 char (3 baris @15sp), 13sp sampai 68 (4 baris @13sp), 12sp sisanya
    @Test
    fun `compact 45 char tetap 15sp dan 46 char turun ke 13sp`() {
        assertEquals(15.sp, compactNameFontSize(45))
        assertEquals(13.sp, compactNameFontSize(46))
    }

    @Test
    fun `compact 68 char tetap 13sp dan 69 char turun ke 12sp`() {
        assertEquals(13.sp, compactNameFontSize(68))
        assertEquals(12.sp, compactNameFontSize(69))
    }

    @Test
    fun `compact nama sangat panjang tidak pernah di bawah 12sp`() {
        assertEquals(12.sp, compactNameFontSize(76))
        assertEquals(12.sp, compactNameFontSize(200))
    }

    @Test
    fun `compact nama kosong dan pendek memakai ukuran penuh`() {
        assertEquals(15.sp, compactNameFontSize(0))
        assertEquals(15.sp, compactNameFontSize(5))
    }

    // standard: 17sp sampai 48, 15sp sampai 56, 13sp sampai 64, 12sp sampai 70, 11sp sisanya
    @Test
    fun `standard 48 char tetap 17sp dan 49 char turun ke 15sp`() {
        assertEquals(17.sp, standardNameFontSize(48))
        assertEquals(15.sp, standardNameFontSize(49))
    }

    @Test
    fun `standard 56 char tetap 15sp dan 57 char turun ke 13sp`() {
        assertEquals(15.sp, standardNameFontSize(56))
        assertEquals(13.sp, standardNameFontSize(57))
    }

    @Test
    fun `standard 64 char tetap 13sp dan 65 char turun ke 12sp`() {
        assertEquals(13.sp, standardNameFontSize(64))
        assertEquals(12.sp, standardNameFontSize(65))
    }

    @Test
    fun `standard 70 char tetap 12sp dan 71 char turun ke 11sp`() {
        assertEquals(12.sp, standardNameFontSize(70))
        assertEquals(11.sp, standardNameFontSize(71))
    }

    @Test
    fun `standard nama sangat panjang tidak pernah di bawah 11sp`() {
        assertEquals(11.sp, standardNameFontSize(114))
        assertEquals(11.sp, standardNameFontSize(300))
    }
}
