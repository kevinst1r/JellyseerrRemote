package com.ecp.jellyseerrremote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkPrimary = Color(0xFF90CAF9)
private val DarkOnPrimary = Color(0xFF003258)
private val DarkPrimaryContainer = Color(0xFF00497D)
private val DarkOnPrimaryContainer = Color(0xFFCFE6FF)
private val DarkSecondary = Color(0xFFB3CDF8)
private val DarkOnSecondary = Color(0xFF1A3254)
private val DarkTertiary = Color(0xFFFFB74D)
private val DarkOnTertiary = Color(0xFF4A2800)
private val DarkError = Color(0xFFF44336)
private val DarkOnError = Color(0xFF690005)
private val DarkBackground = Color(0xFF111318)
private val DarkOnBackground = Color(0xFFE3E2E6)
private val DarkSurface = Color(0xFF111318)
private val DarkOnSurface = Color(0xFFE3E2E6)
private val DarkSurfaceVariant = Color(0xFF1F2228)
private val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
private val DarkOutline = Color(0xFF8E9099)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    error = DarkError,
    onError = DarkOnError,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    surfaceContainerHigh = Color(0xFF1A1D24)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
