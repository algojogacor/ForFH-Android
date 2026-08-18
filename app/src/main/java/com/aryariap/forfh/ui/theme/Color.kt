package com.aryariap.forfh.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Token Warna Sistem Desain Konstitusi (Bold Editorial Utility) untuk ForFH Android.
 */
object ForfhColors {
    // Fondasi Utama Light
    val Navy = Color(0xFF14325B)          // primary
    val NavyDark = Color(0xFF0E2440)      // pressed / hero gradient start
    val NavyHeroEnd = Color(0xFF1D4478)   // hero gradient end
    val Brass = Color(0xFFA67C2E)         // secondary : aksen kuningan
    val BrassTonal = Color(0xFFF4EAD3)    // secondaryContainer
    val Background = Color(0xFFF4F6F9)    // bg light
    val Surface = Color(0xFFFFFFFF)       // surface light
    val Surface2 = Color(0xFFEDF1F6)      // tile ikon, segmen tab
    val Surface3 = Color(0xFFE3E9F1)      // tombol tonal, pill nav aktif
    val Ink = Color(0xFF141B26)           // teks utama
    val Ink2 = Color(0xFF57606E)          // teks sekunder
    val Ink3 = Color(0xFF8A93A1)          // teks tersier / placeholder
    val Line = Color(0xFFDCE2EA)          // border kartu / row
    val Line2 = Color(0xFFC9D1DC)         // border tombol outline, track toggle
    val Error = Color(0xFFB3261E)
    val ErrorContainer = Color(0xFFFBE9E7)

    // Alarm Khusus (OLED pitch-black untuk lock screen)
    val AlarmBackground = Color(0xFF07090D)
    val AlarmSurface = Color(0xFF101623)
    val AlarmLine = Color(0xFF26334B)
    val AlarmBrass = Color(0xFFD6B25C)

    // Fondasi Utama Dark
    val DarkPrimary = Color(0xFF9DB9E8)
    val DarkSecondary = Color(0xFFD6B25C)
    val DarkSecondaryContainer = Color(0xFF3B2E10)
    val DarkBackground = Color(0xFF0C1220)
    val DarkSurface = Color(0xFF131C2D)
    val DarkSurface2 = Color(0xFF1A2540)
    val DarkSurface3 = Color(0xFF223152)
    val DarkInk = Color(0xFFE8ECF4)
    val DarkInk2 = Color(0xFFA5AEC0)
    val DarkInk3 = Color(0xFF6E7A8E)
    val DarkLine = Color(0xFF243149)
    val DarkLine2 = Color(0xFF33425E)
    val DarkError = Color(0xFFF2B8B5)
    val DarkErrorContainer = Color(0xFF3A1C1A)

    // Aksen Dinamis Mata Kuliah (8 palet terkalibrasi AA)
    val Terakota = Color(0xFFC05B3A)
    val Selasih = Color(0xFF4E7A5B)
    val Nila = Color(0xFF4B5BB5)
    val Okra = Color(0xFFB07B2A)
    val Plum = Color(0xFF8A4A6B)
    val Teal = Color(0xFF2F7D7A)
    val Bata = Color(0xFFA84338)
    val BatuTulis = Color(0xFF5B6B80)

    val CourseAccents = listOf(
        Terakota, Selasih, Nila, Okra, Plum, Teal, Bata, BatuTulis
    )

    // Warna Semantik Status Tugas
    val StatusBelumBg = Color(0xFFECEFF3)
    val StatusBelumFg = Color(0xFF5B6B80)
    val StatusBelumDarkBg = Color(0xFF232C3A)
    val StatusBelumDarkFg = Color(0xFF9AA8BC)

    val StatusProsesBg = Color(0xFFFBF0DC)
    val StatusProsesFg = Color(0xFFA05B08)
    val StatusProsesDarkBg = Color(0xFF35280F)
    val StatusProsesDarkFg = Color(0xFFE8B873)

    val StatusRevisiBg = Color(0xFFF0E9FB)
    val StatusRevisiFg = Color(0xFF6D28D9)
    val StatusRevisiDarkBg = Color(0xFF2A1D44)
    val StatusRevisiDarkFg = Color(0xFFC4A8F0)

    val StatusTerlambatBg = Color(0xFFFBE9E7)
    val StatusTerlambatFg = Color(0xFFB3261E)
    val StatusTerlambatDarkBg = Color(0xFF3A1C1A)
    val StatusTerlambatDarkFg = Color(0xFFF2B8B5)

    val StatusSelesaiBg = Color(0xFFE4F2E8)
    val StatusSelesaiFg = Color(0xFF2F7D4F)
    val StatusSelesaiDarkBg = Color(0xFF16301F)
    val StatusSelesaiDarkFg = Color(0xFF8FD3A8)

    val StatusMengirimBg = Color(0xFFE7EEFC)
    val StatusMengirimFg = Color(0xFF2563EB)
    val StatusMengirimDarkBg = Color(0xFF17264A)
    val StatusMengirimDarkFg = Color(0xFF9DB9F5)

    val StatusGagalBg = Color(0xFFFBE9E7)
    val StatusGagalFg = Color(0xFF991B1B)
    val StatusGagalDarkBg = Color(0xFF3A1C1A)
    val StatusGagalDarkFg = Color(0xFFF2B8B5)

    // Todoist Priority Colors (P1–P4)
    val PriorityP1 = Color(0xFFDC4C3E)
    val PriorityP2 = Color(0xFFE15E00)
    val PriorityP3 = Color(0xFF246FE0)
    val PriorityP4 = Color(0xFF71717A)

    // Notion Calendar Time Indicator & Tints
    val NotionTimeIndicator = Color(0xFFE25553)
    val NotionEventTint = Color(0x265E6AD2)

    // Alias Kompatibilitas Legacy & Widget
    val Accent = Navy
    val AccentHover = NavyDark
    val AccentSubtle = Surface2
    val AccentDark = DarkPrimary
    val AccentHoverDark = DarkSecondary
    val CanvasLight = Background
    val Surface1Light = Surface
    val TextPrimaryLight = Ink
    val TextSecondaryLight = Ink2
    val BorderStrongLight = Line
    val CanvasDark = DarkBackground
    val Surface1Dark = DarkSurface
    val Surface2Dark = DarkSurface2
    val TextPrimaryDark = DarkInk
    val TextSecondaryDark = DarkInk2
    val BorderStrongDark = DarkLine
    val Danger = Error
    val Success = Color(0xFF2F7D4F)
    val Warning = Brass
}
