package com.aryariap.forfh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = ForfhColors.LinearIndigo,
    onPrimary = Color.White,
    primaryContainer = ForfhColors.Surface3,
    onPrimaryContainer = ForfhColors.LinearIndigo,
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = ForfhColors.LightSurface2,
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = ForfhColors.Teal,
    onTertiary = Color.White,
    background = ForfhColors.LightBg,
    onBackground = ForfhColors.LightInk,
    surface = ForfhColors.LightSurface,
    onSurface = ForfhColors.LightInk,
    surfaceVariant = ForfhColors.LightSurface2,
    onSurfaceVariant = ForfhColors.LightInk2,
    outline = ForfhColors.LightLine,
    outlineVariant = ForfhColors.Line2,
    error = ForfhColors.Error,
    onError = Color.White,
    errorContainer = ForfhColors.ErrorContainer,
    onErrorContainer = ForfhColors.Error,
)

internal val DarkScheme = darkColorScheme(
    primary = ForfhColors.LinearIndigo,
    onPrimary = Color.White,
    primaryContainer = ForfhColors.LinearIndigoSubtle,
    onPrimaryContainer = Color.White,
    secondary = ForfhColors.TextSecondary,
    onSecondary = ForfhColors.PitchBlack,
    secondaryContainer = ForfhColors.SurfaceSecondary,
    onSecondaryContainer = ForfhColors.TextPrimary,
    tertiary = ForfhColors.Teal,
    onTertiary = Color.White,
    background = ForfhColors.PitchBlack,
    onBackground = ForfhColors.TextPrimary,
    surface = ForfhColors.SurfaceElevated,
    onSurface = ForfhColors.TextPrimary,
    surfaceVariant = ForfhColors.SurfaceSecondary,
    onSurfaceVariant = ForfhColors.TextMuted,
    outline = ForfhColors.BorderSubtle,
    outlineVariant = ForfhColors.BorderStrong,
    error = ForfhColors.DarkError,
    onError = ForfhColors.PitchBlack,
    errorContainer = ForfhColors.DarkErrorContainer,
    onErrorContainer = ForfhColors.DarkError,
)

@Composable
fun ForfhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = ForfhTypography,
        shapes = ForfhShapes,
        content = content,
    )
}
