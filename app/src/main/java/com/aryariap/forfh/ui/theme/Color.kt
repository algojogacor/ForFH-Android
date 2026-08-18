package com.aryariap.forfh.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Token Warna Linear Design System, Todoist, dan Notion Calendar.
 * Base OLED Pitch Black dengan layer permukaan Zinc, Linear Indigo, Todoist Priority, dan Notion Indicator.
 */
object ForfhColors {
    // Linear Base Surfaces (Dark Theme)
    val PitchBlack = Color(0xFF000000)          // bg-base Linear
    val SurfaceElevated = Color(0xFF111113)     // bg-surface-elevated Linear (Card)
    val SurfaceSecondary = Color(0xFF161618)    // bg-surface-secondary Linear (Sub-card / Input)
    val SurfaceHover = Color(0xFF1F1F23)        // bg-surface-hover Linear (Interactive row)
    val SurfaceOverlay = Color(0xFF18181B)      // Modal / Bottom sheet

    // Linear Borders
    val BorderSubtle = Color(0x14FFFFFF)        // rgba(255, 255, 255, 0.08)
    val BorderStrong = Color(0x26FFFFFF)        // rgba(255, 255, 255, 0.15)
    val BorderFocus = Color(0xFF5E6AD2)         // Linear Indigo Focus

    // Linear Typography Colors
    val TextPrimary = Color(0xFFFFFFFF)         // White
    val TextSecondary = Color(0xFFD4D4D8)       // Zinc-300
    val TextMuted = Color(0xFF71717A)           // Zinc-500
    val TextQuaternary = Color(0xFF52525B)      // Zinc-600

    // Linear Brand & Accents
    val LinearIndigo = Color(0xFF5E6AD2)        // Primary Accent
    val LinearIndigoHover = Color(0xFF6E7BE2)
    val LinearIndigoSubtle = Color(0x265E6AD2)  // 15% opacity

    // Todoist Priority Scale (P1–P4)
    val PriorityP1 = Color(0xFFDC4C3E)          // Urgent (Red)
    val PriorityP2 = Color(0xFFE15E00)          // High (Orange)
    val PriorityP3 = Color(0xFF246FE0)          // Medium (Blue)
    val PriorityP4 = Color(0xFF71717A)          // Low / Normal (Grey)

    // Notion Calendar Accent & Time Indicator
    val NotionTimeIndicator = Color(0xFFE25553) // Red timeline line & dot
    val NotionEventTint = Color(0x265E6AD2)

    // Status Semantik
    val StatusBelumBg = Color(0x1A71717A)
    val StatusBelumFg = Color(0xFFA1A1AA)

    val StatusProsesBg = Color(0x1AF59E0B)
    val StatusProsesFg = Color(0xFFF59E0B)

    val StatusRevisiBg = Color(0x1A8B5CF6)
    val StatusRevisiFg = Color(0xFFA78BFA)

    val StatusTerlambatBg = Color(0x1ADC4C3E)
    val StatusTerlambatFg = Color(0xFFF87171)

    val StatusSelesaiBg = Color(0x1A10B981)
    val StatusSelesaiFg = Color(0xFF34D399)

    val StatusMengirimBg = Color(0x1A3B82F6)
    val StatusMengirimFg = Color(0xFF60A5FA)

    val StatusGagalBg = Color(0x1AEF4444)
    val StatusGagalFg = Color(0xFFF87171)

    // Aksen Mata Kuliah (Linear Tinted Pastels terkalibrasi)
    val Terakota = Color(0xFFF43F5E)
    val Selasih = Color(0xFF10B981)
    val Nila = Color(0xFF5E6AD2)
    val Okra = Color(0xFFF59E0B)
    val Plum = Color(0xFFA855F7)
    val Teal = Color(0xFF06B6D4)
    val Bata = Color(0xFFEF4444)
    val BatuTulis = Color(0xFF64748B)

    val CourseAccents = listOf(
        Nila, Terakota, Selasih, Okra, Plum, Teal, Bata, BatuTulis
    )

    // Fondasi Light Theme (Clean Minimal Editorial)
    val LightBg = Color(0xFFF8FAFC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurface2 = Color(0xFFF1F5F9)
    val LightLine = Color(0xFFE2E8F0)
    val LightInk = Color(0xFF0F172A)
    val LightInk2 = Color(0xFF475569)

    // Alarm Lockscreen OLED Pitch Black
    val AlarmBackground = Color(0xFF000000)
    val AlarmSurface = Color(0xFF111113)
    val AlarmLine = Color(0x26FFFFFF)
    val AlarmBrass = Color(0xFFF59E0B)

    // Alias Kompatibilitas Sistem
    val Navy = LinearIndigo
    val NavyDark = SurfaceElevated
    val NavyHeroEnd = SurfaceSecondary
    val Brass = Color(0xFFF59E0B)
    val BrassTonal = Color(0x26F59E0B)
    val Background = LightBg
    val Surface = LightSurface
    val Surface2 = LightSurface2
    val Surface3 = Color(0xFFE2E8F0)
    val Ink = LightInk
    val Ink2 = LightInk2
    val Ink3 = Color(0xFF94A3B8)
    val Line = LightLine
    val Line2 = Color(0xFFCBD5E1)
    val Error = Color(0xFFEF4444)
    val ErrorContainer = Color(0x26EF4444)

    val DarkPrimary = LinearIndigo
    val DarkSecondary = Color(0xFFD4D4D8)
    val DarkSecondaryContainer = SurfaceSecondary
    val DarkBackground = PitchBlack
    val DarkSurface = SurfaceElevated
    val DarkSurface2 = SurfaceSecondary
    val DarkSurface3 = SurfaceHover
    val DarkInk = TextPrimary
    val DarkInk2 = TextSecondary
    val DarkInk3 = TextMuted
    val DarkLine = BorderSubtle
    val DarkLine2 = BorderStrong
    val DarkError = Color(0xFFF87171)
    val DarkErrorContainer = Color(0x26EF4444)

    val Accent = LinearIndigo
    val AccentHover = LinearIndigoHover
    val AccentSubtle = SurfaceHover
    val AccentDark = LinearIndigo
    val AccentHoverDark = LinearIndigoHover
    val CanvasLight = LightBg
    val Surface1Light = LightSurface
    val TextPrimaryLight = LightInk
    val TextSecondaryLight = LightInk2
    val BorderStrongLight = LightLine
    val CanvasDark = PitchBlack
    val Surface1Dark = SurfaceElevated
    val Surface2Dark = SurfaceSecondary
    val TextPrimaryDark = TextPrimary
    val TextSecondaryDark = TextSecondary
    val BorderStrongDark = BorderStrong
    val Danger = Color(0xFFEF4444)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val LinearPurple = Color(0xFFA855F7)
    val Surface1 = SurfaceElevated
    val StatusSuccess = StatusSelesaiFg
}
