package com.emotiontracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A calm, low-saturation teal/slate palette rather than stock Material purple —
// deliberately quiet, since this is a mental-health-adjacent app people may check
// during a stressed moment. Avoids alarm-red except for genuine warnings.
private val TrackerLight = lightColorScheme(
    primary = Color(0xFF2A6F6F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEAE8),
    onPrimaryContainer = Color(0xFF0A2626),
    secondary = Color(0xFF4E6360),
    background = Color(0xFFF7FAF9),
    surface = Color(0xFFF7FAF9),
    surfaceVariant = Color(0xFFDDE5E3),
    error = Color(0xFFB3261E)
)

private val TrackerDark = darkColorScheme(
    primary = Color(0xFF8FD3CF),
    onPrimary = Color(0xFF07322F),
    primaryContainer = Color(0xFF17403E),
    onPrimaryContainer = Color(0xFFCDEAE8),
    secondary = Color(0xFFB4CCC7),
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF3A4442),
    error = Color(0xFFFFB4AB)
)

@Composable
fun TrackerTheme(darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) TrackerDark else TrackerLight,
        content = content
    )
}
