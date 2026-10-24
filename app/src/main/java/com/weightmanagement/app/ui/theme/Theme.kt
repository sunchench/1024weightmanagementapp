package com.weightmanagement.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B79C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E6FF),
    onPrimaryContainer = Color(0xFF001D34),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color.Black,
    background = Color(0xFFF7FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE3EDF5),
    onSurface = Color(0xFF1A1C1E),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FC4FF),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF004B7A),
    onPrimaryContainer = Color(0xFFD2E6FF),
    secondary = Color(0xFF7FD9CD),
    background = Color(0xFF111416),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF2A2F33),
    onSurface = Color(0xFFE2E6E9),
    error = Color(0xFFF2B8B5)
)

@Composable
fun WeightManagementTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
