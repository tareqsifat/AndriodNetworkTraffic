package com.example.networkmonitor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark cybersecurity-themed palette
val CyberGreen = Color(0xFF00E676)
val CyberGreenDark = Color(0xFF00C853)
val DangerRed = Color(0xFFFF1744)
val WarnAmber = Color(0xFFFFAB00)
val SafeGreen = Color(0xFF00E676)
val SurfaceDark = Color(0xFF0D1117)
val SurfaceCard = Color(0xFF161B22)
val SurfaceElevated = Color(0xFF1C2128)
val OnSurfaceText = Color(0xFFE6EDF3)
val SubtleText = Color(0xFF8B949E)
val BorderColor = Color(0xFF30363D)

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = Color(0xFF003314),
    primaryContainer = Color(0xFF00531F),
    onPrimaryContainer = CyberGreen,
    secondary = Color(0xFF58A6FF),
    onSecondary = Color(0xFF003166),
    background = SurfaceDark,
    onBackground = OnSurfaceText,
    surface = SurfaceCard,
    onSurface = OnSurfaceText,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = SubtleText,
    error = DangerRed,
    outline = BorderColor
)

@Composable
fun NetworkMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
