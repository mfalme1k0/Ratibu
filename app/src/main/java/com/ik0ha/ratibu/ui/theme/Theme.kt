package com.ik0ha.ratibu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkCyanLuxuryColorScheme = darkColorScheme(
    primary = CyanSecondary, // Use the lighter cyan for primary in dark mode
    secondary = CyanLight,
    tertiary = CyanTertiary,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

private val LightCyanLuxuryColorScheme = lightColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    tertiary = CyanTertiary,
    background = CyanBackground,
    surface = CyanSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = CyanText,
    onSurface = CyanText,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun RatibuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkCyanLuxuryColorScheme
    } else {
        LightCyanLuxuryColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// To allow dynamic theme updates, we can create a composition local or just a stateful wrapper
@Composable
fun RatibuAppTheme(
    themePreference: Boolean?, // null for system, true for dark, false for light
    content: @Composable () -> Unit
) {
    val isDark = themePreference ?: isSystemInDarkTheme()
    RatibuTheme(darkTheme = isDark, content = content)
}
