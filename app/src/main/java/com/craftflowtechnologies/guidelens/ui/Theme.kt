package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Theme color schemes
val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF64748B),
    error = Color(0xFFEF4444),
    onError = Color.White
)

val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White
)

// Extended theme colors for custom use
data class GuideLensColors(
    val gradientStart: Color,
    val gradientEnd: Color,
    val overlayBackground: Color,
    val cardBackground: Color,
    val borderColor: Color,
    val successColor: Color,
    val warningColor: Color,
    val infoColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val iconTint: Color,
    val divider: Color,
    val ripple: Color
)

val DarkGuideLensColors = GuideLensColors(
    gradientStart = Color(0xFF0F172A),
    gradientEnd = Color(0xFF1E293B),
    overlayBackground = Color(0x80000000),
    cardBackground = Color(0xFF1E293B),
    borderColor = Color(0xFF334155),
    successColor = Color(0xFF10B981),
    warningColor = Color(0xFFF59E0B),
    infoColor = Color(0xFF3B82F6),
    textPrimary = Color.White,
    textSecondary = Color(0xFFE2E8F0),
    textTertiary = Color(0xFF94A3B8),
    iconTint = Color.White,
    divider = Color(0xFF334155),
    ripple = Color(0x20FFFFFF)
)

val LightGuideLensColors = GuideLensColors(
    gradientStart = Color(0xFFF8FAFC),
    gradientEnd = Color(0xFFE2E8F0),
    overlayBackground = Color(0x80000000),
    cardBackground = Color.White,
    borderColor = Color(0xFFE2E8F0),
    successColor = Color(0xFF059669),
    warningColor = Color(0xFFD97706),
    infoColor = Color(0xFF2563EB),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF64748B),
    iconTint = Color(0xFF1E293B),
    divider = Color(0xFFE2E8F0),
    ripple = Color(0x20000000)
)

// Composition locals for accessing theme colors
val LocalGuideLensColors = compositionLocalOf { DarkGuideLensColors }

// Theme controller
class ThemeController {
    var isDarkTheme by mutableStateOf(true)
       private set
    
    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
    
}

@Composable
fun GuideLensTheme(
    themeController: ThemeController,
    content: @Composable () -> Unit
) {
    val isDarkTheme = themeController.isDarkTheme
    val colorScheme = if (isDarkTheme) DarkColors else LightColors
    val guideLensColors = if (isDarkTheme) DarkGuideLensColors else LightGuideLensColors
    
    CompositionLocalProvider(
        LocalGuideLensColors provides guideLensColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// Extension property to access GuideLens colors
val MaterialTheme.guideLensColors: GuideLensColors
    @Composable get() = LocalGuideLensColors.current