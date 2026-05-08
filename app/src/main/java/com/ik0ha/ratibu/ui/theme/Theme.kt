package com.ik0ha.ratibu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyanLuxuryColorScheme = lightColorScheme(
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
    darkTheme: Boolean = false, // Enforce light theme for the requested "Light Look"
    content: @Composable () -> Unit
) {
    // We ignore darkTheme and dynamicColor to maintain the specific brand identity requested
    MaterialTheme(
        colorScheme = CyanLuxuryColorScheme,
        typography = Typography,
        content = content
    )
}
