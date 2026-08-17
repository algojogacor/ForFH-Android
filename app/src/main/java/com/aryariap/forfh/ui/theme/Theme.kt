package com.aryariap.forfh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// internal (bukan private) supaya widget Glance memakai skema warna yang sama (ForfhWidget.kt)
internal val LightScheme = lightColorScheme(
    primary = ForfhColors.Accent,
    onPrimary = Color.White,
    primaryContainer = ForfhColors.AccentSubtle,
    onPrimaryContainer = ForfhColors.AccentHover,
    background = ForfhColors.CanvasLight,
    onBackground = ForfhColors.TextPrimaryLight,
    surface = ForfhColors.Surface1Light,
    onSurface = ForfhColors.TextPrimaryLight,
    surfaceVariant = Color(0xFFF5F3F0),
    onSurfaceVariant = ForfhColors.TextSecondaryLight,
    outline = ForfhColors.BorderStrongLight,
    error = ForfhColors.Danger,
    onError = Color.White,
    secondary = ForfhColors.Warning,
    tertiary = ForfhColors.Success,
)

internal val DarkScheme = darkColorScheme(
    primary = ForfhColors.AccentDark,
    onPrimary = ForfhColors.CanvasDark,
    primaryContainer = ForfhColors.AccentHover,
    onPrimaryContainer = ForfhColors.AccentSubtle,
    background = ForfhColors.CanvasDark,
    onBackground = ForfhColors.TextPrimaryDark,
    surface = ForfhColors.Surface1Dark,
    onSurface = ForfhColors.TextPrimaryDark,
    surfaceVariant = ForfhColors.Surface2Dark,
    onSurfaceVariant = ForfhColors.TextSecondaryDark,
    outline = ForfhColors.BorderStrongDark,
    error = ForfhColors.Danger,
    onError = Color.White,
)

@Composable
fun ForfhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ForfhTypography,
        content = content,
    )
}
