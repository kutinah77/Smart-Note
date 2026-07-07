package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDark = staticCompositionLocalOf { false }

private val MizanLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    secondary = CoralAccent,
    onSecondary = Color.White,
    tertiary = SoftGreen,
    error = SoftRed,
    background = IvoryBackground,
    surface = LightSurface,
    surfaceVariant = Color(0xFFF7F9FC),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = BorderLight
)

private val MizanDarkColorScheme = darkColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    secondary = CoralDark,
    onSecondary = Color.White,
    tertiary = SoftGreen,
    error = SoftRed,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF262626), // Slightly lighter than surface
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

@Composable
fun MizanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MizanDarkColorScheme else MizanLightColorScheme

    CompositionLocalProvider(LocalIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
