package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TvDarkColorScheme = darkColorScheme(
    primary = TvCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = TvCyanVariant,
    onPrimaryContainer = Color.White,
    secondary = TvSkySecondary,
    onSecondary = Color.Black,
    secondaryContainer = TvSurfaceVariant,
    onSecondaryContainer = TvTextPrimary,
    tertiary = TvGoldTertiary,
    onTertiary = Color.Black,
    background = TvBackground,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    error = TvStatusRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = Typography,
        content = content
    )
}
