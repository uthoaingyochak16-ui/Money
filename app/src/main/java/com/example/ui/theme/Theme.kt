package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE2E8F0),
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Slate400,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate200,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Slate400,
    outline = DarkBorder,
    outlineVariant = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = Slate700,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate300
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
