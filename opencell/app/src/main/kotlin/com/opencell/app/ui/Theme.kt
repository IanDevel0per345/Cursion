package com.opencell.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opencell.app.AppContainer
import com.opencell.app.data.ThemeMode

object OcColors {
    val Teal = Color(0xFF5EEAD4)
    val TealDark = Color(0xFF0F766E)
    val Ink = Color(0xFF0B1220)
    val Surface = Color(0xFF131C2E)
    val Success = Color(0xFF34D399)
    val Warning = Color(0xFFFBBF24)
    val Error = Color(0xFFF87171)
}

private val Dark = darkColorScheme(
    primary = OcColors.Teal, onPrimary = OcColors.Ink, secondary = Color(0xFF93C5FD),
    background = OcColors.Ink, onBackground = Color(0xFFE6EDF7),
    surface = OcColors.Surface, onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1C2740), onSurfaceVariant = Color(0xFFB4C0D4),
    error = OcColors.Error, outline = Color(0xFF3A4A68),
)

private val Light = lightColorScheme(
    primary = OcColors.TealDark, onPrimary = Color.White, secondary = Color(0xFF1D4ED8),
    background = Color(0xFFF6F8FB), onBackground = Color(0xFF0B1220),
    surface = Color.White, onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE8EEF6), onSurfaceVariant = Color(0xFF3A4A68),
    error = Color(0xFFB91C1C), outline = Color(0xFF8A9AB8),
)

private val OcTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val OcShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(28.dp),
)

val LocalReduceMotion = compositionLocalOf { false }
val LocalHaptics = compositionLocalOf { true }

@Composable
fun OpenCellTheme(container: AppContainer, content: @Composable () -> Unit) {
    val s by container.settings.collectAsState()
    val dark = when (s.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalReduceMotion provides s.reduceMotion, LocalHaptics provides s.haptics) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = OcTypography, shapes = OcShapes, content = content)
    }
}
