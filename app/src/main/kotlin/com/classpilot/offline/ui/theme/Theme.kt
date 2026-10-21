package com.classpilot.offline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Colour palette ────────────────────────────────────────────────────────────
// Primary: Indigo-violet
val Indigo400 = Color(0xFF7C4DFF)
val Indigo600 = Color(0xFF651FFF)
val IndigoLight = Color(0xFFB39DFF)
// Surface darks
val Surface900 = Color(0xFF0E0E1A)
val Surface800 = Color(0xFF14142B)
val Surface700 = Color(0xFF1C1C3A)
val Surface600 = Color(0xFF232345)
val Surface200 = Color(0xFFE8E8F8)
// Accent colours
val Green400 = Color(0xFF00E676)
val Red400 = Color(0xFFFF1744)
val Amber400 = Color(0xFFFFAB00)
val Cyan400 = Color(0xFF00E5FF)
val Pink400 = Color(0xFFFF4081)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Color.White,
    primaryContainer = Surface700,
    onPrimaryContainer = IndigoLight,
    secondary = Cyan400,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003543),
    onSecondaryContainer = Cyan400,
    tertiary = Pink400,
    background = Surface900,
    onBackground = Color(0xFFE8E8FF),
    surface = Surface800,
    onSurface = Color(0xFFE8E8FF),
    surfaceVariant = Surface700,
    onSurfaceVariant = Color(0xFFB0B0D0),
    error = Red400,
    onError = Color.Black,
    outline = Color(0xFF444466)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    background = Color(0xFFF5F5FF),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFEEEEFF),
    onSurfaceVariant = Color(0xFF444466),
    error = Color(0xFFC62828),
    outline = Color(0xFFBBBBDD)
)

@Composable
fun ClassPilotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClassPilotTypography,
        content = content
    )
}
