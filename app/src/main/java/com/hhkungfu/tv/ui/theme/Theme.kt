package com.hhkungfu.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NetflixRed,
    secondary = NetflixGold,
    tertiary = NetflixDarkRed,
    background = NetflixBlack,
    surface = NetflixSurface,
    onPrimary = TextPrimary,
    onSecondary = NetflixBlack,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun HHKungfuTVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
