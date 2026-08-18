package com.aryariap.forfh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = ForfhColors.Navy,
    onPrimary = Color.White,
    primaryContainer = ForfhColors.Surface3,
    onPrimaryContainer = ForfhColors.Navy,
    secondary = ForfhColors.Brass,
    onSecondary = Color.White,
    secondaryContainer = ForfhColors.BrassTonal,
    onSecondaryContainer = ForfhColors.Brass,
    tertiary = ForfhColors.Teal,
    onTertiary = Color.White,
    background = ForfhColors.Background,
    onBackground = ForfhColors.Ink,
    surface = ForfhColors.Surface,
    onSurface = ForfhColors.Ink,
    surfaceVariant = ForfhColors.Surface2,
    onSurfaceVariant = ForfhColors.Ink2,
    outline = ForfhColors.Line,
    outlineVariant = ForfhColors.Line2,
    error = ForfhColors.Error,
    onError = Color.White,
    errorContainer = ForfhColors.ErrorContainer,
    onErrorContainer = ForfhColors.Error,
)

internal val DarkScheme = darkColorScheme(
    primary = ForfhColors.DarkPrimary,
    onPrimary = ForfhColors.NavyDark,
    primaryContainer = ForfhColors.DarkSurface3,
    onPrimaryContainer = ForfhColors.DarkPrimary,
    secondary = ForfhColors.DarkSecondary,
    onSecondary = ForfhColors.NavyDark,
    secondaryContainer = ForfhColors.DarkSecondaryContainer,
    onSecondaryContainer = ForfhColors.DarkSecondary,
    tertiary = ForfhColors.Teal,
    onTertiary = Color.White,
    background = ForfhColors.DarkBackground,
    onBackground = ForfhColors.DarkInk,
    surface = ForfhColors.DarkSurface,
    onSurface = ForfhColors.DarkInk,
    surfaceVariant = ForfhColors.DarkSurface2,
    onSurfaceVariant = ForfhColors.DarkInk2,
    outline = ForfhColors.DarkLine,
    outlineVariant = ForfhColors.DarkLine2,
    error = ForfhColors.DarkError,
    onError = ForfhColors.NavyDark,
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
        content = content,
    )
}
