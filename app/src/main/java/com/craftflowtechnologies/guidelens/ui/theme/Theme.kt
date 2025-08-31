package com.craftflowtechnologies.guidelens.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// GuideLens Color Palette
object GuideLensColors {
    // Primary colors from the app logo
    val RiverBed = Color(0xFF475463)
    val DeepNavy = Color(0xFF131f2f)
    val SlateGray = Color(0xFF7e8b99)
    val SkyBlue = Color(0xFF5686be)
    val LightBlue = Color(0xFFcbdae7)
    val CharcoalBlue = Color(0xFF576372)
    val PowderBlue = Color(0xFF8fbadd)
    val SteelBlue = Color(0xFF4d6b90)
    val DarkSlate = Color(0xFF3f4f66)
    val MidGray = Color(0xFF6c727a)
    val DarkGray = Color(0xFF4c525b)
    
    // Zambian Color Palette (cultural integration)
    val ZambianEmeraldGreen = Color(0xFF228B22) // Zambian flag green
    val ZambianCopperOrange = Color(0xFFFF6600) // Copper mining orange
    val ZambianEagleRed = Color(0xFFDC143C) // Zambian eagle red
    val ZambianSunYellow = Color(0xFFFFD700) // African sun yellow
    val ZambianRiverBlue = Color(0xFF0066CC) // Zambezi river blue
    val ZambianEarthBrown = Color(0xFF8B4513) // African earth brown
    val ZambianMaizeGold = Color(0xFFDAA520) // Maize/corn gold
    val ZambianForestGreen = Color(0xFF228B22) // Dense forest green
    val ZambianSkyBlue = Color(0xFF87CEEB) // Clear African sky blue

    // Light Theme Colors (with Zambian influence)
    val LightPrimary = ZambianRiverBlue
    val LightOnPrimary = Color.White
    val LightPrimaryContainer = ZambianSkyBlue.copy(alpha = 0.3f)
    val LightOnPrimaryContainer = DeepNavy
    val LightSecondary = ZambianEmeraldGreen
    val LightOnSecondary = Color.White
    val LightSecondaryContainer = PowderBlue
    val LightOnSecondaryContainer = DarkSlate
    val LightBackground = Color(0xFFFAFBFC)
    val LightOnBackground = DeepNavy
    val LightSurface = Color.White
    val LightOnSurface = DeepNavy
    val LightSurfaceVariant = LightBlue.copy(alpha = 0.3f)
    val LightOnSurfaceVariant = RiverBed
    val LightOutline = SlateGray
    val LightError = Color(0xFFD32F2F)
    val LightOnError = Color.White

    // Dark Theme Colors (with Zambian influence)
    val DarkPrimary = ZambianMaizeGold
    val DarkOnPrimary = DeepNavy
    val DarkPrimaryContainer = ZambianEmeraldGreen
    val DarkOnPrimaryContainer = ZambianSkyBlue
    val DarkSecondary = ZambianCopperOrange
    val DarkOnSecondary = DeepNavy
    val DarkSecondaryContainer = DarkSlate
    val DarkOnSecondaryContainer = PowderBlue
    val DarkBackground = DeepNavy
    val DarkOnBackground = Color(0xFFE8ECF1)
    val DarkSurface = RiverBed
    val DarkOnSurface = Color(0xFFE8ECF1)
    val DarkSurfaceVariant = DarkGray
    val DarkOnSurfaceVariant = SlateGray
    val DarkOutline = MidGray
    val DarkError = Color(0xFFCF6679)
    val DarkOnError = Color(0xFF140C0D)

    // Glassmorphism and Neumorphic Colors (from old version)
    val LightCardBackground = Color(0xFFFFFFFF)
    val LightOverlayBackground = Color(0x80000000)
    val LightGradientStart = Color(0xFFF5F7FA)
    val LightGradientEnd = Color(0xFFE8ECF1)
    val LightTextPrimary = Color(0xFF1A1A2E)
    val LightTextSecondary = Color(0xFF4A5568)
    val LightTextTertiary = Color(0xFF718096)
    val LightDividerColor = Color(0xFFEDF2F7)
    val LightIconTint = Color(0xFF4A5568)
    val LightSuccessColor = Color(0xFF10B981)
    val LightWarningColor = Color(0xFFF59E0B)
    val LightGlassBackground = Color(0xE6FFFFFF)
    val LightGlassBorder = Color(0x33E2E8F0)
    val LightGlassOverlay = Color(0x0AFFFFFF)
    val LightShadowColor = Color(0x1A000000)
    val LightGlowColor = Color(0x336366F1)

    val DarkCardBackground = Color(0xFF2A2A3E)
    val DarkOverlayBackground = Color(0xCC000000)
    val DarkGradientStart = Color(0xFF0F0F1E)
    val DarkGradientEnd = Color(0xFF1A1A2E)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFFE2E8F0)
    val DarkTextTertiary = Color(0xFFCBD5E0)
    val DarkDividerColor = Color(0x1AFFFFFF)
    val DarkIconTint = Color(0xFFE2E8F0)
    val DarkSuccessColor = Color(0xFF34D399)
    val DarkWarningColor = Color(0xFFFBBF24)
    val DarkGlassBackground = Color(0xB32A2A3E)
    val DarkGlassBorder = Color(0x4DFFFFFF)
    val DarkGlassOverlay = Color(0x0DFFFFFF)
    val DarkShadowColor = Color(0x80000000)
    val DarkGlowColor = Color(0x4D818CF8)
}

// Cooking-specific colors for different cooking phases and elements
object CookingColors {
    // Cooking phase colors
    val PreparationPhase = Color(0xFF4CAF50) // Green
    val CookingPhase = Color(0xFFFF9800) // Orange
    val CompletedPhase = Color(0xFF2196F3) // Blue
    val PausedPhase = Color(0xFF9E9E9E) // Gray

    // Timer and alert colors
    val TimerRunning = Color(0xFF2196F3) // Blue
    val TimerWarning = Color(0xFFFF9800) // Orange
    val TimerCritical = Color(0xFFD32F2F) // Red
    val TimerPaused = Color(0xFF9E9E9E) // Gray

    // Temperature colors
    val LowHeat = Color(0xFF4CAF50) // Green
    val MediumHeat = Color(0xFFFF9800) // Orange
    val HighHeat = Color(0xFFD32F2F) // Red

    // Ingredient colors
    val Protein = Color(0xFFE91E63) // Pink
    val Vegetable = Color(0xFF4CAF50) // Green
    val Grain = Color(0xFFFF9800) // Orange
    val Dairy = Color(0xFF2196F3) // Blue
    val Spice = Color(0xFF9C27B0) // Purple

    // UI element colors
    val CriticalStep = Color(0xFFFFEBEE) // Light pink background
    val CriticalStepIcon = Color(0xFFE91E63) // Pink icon
    val TipBackground = Color(0xFFFFF8E1) // Light yellow
    val TipBorder = Color(0xFFFFE082) // Yellow border
    val TipIcon = Color(0xFFFF8F00) // Orange icon
}

// Composition local for custom colors
val LocalGuideLensColors = staticCompositionLocalOf {
    GuideLensColors // Default to light theme colors
}

// Extension property to access custom colors
val MaterialTheme.guideLensColors: GuideLensColors
    @Composable
    get() = LocalGuideLensColors.current

private val LightColorScheme = lightColorScheme(
    primary = GuideLensColors.LightPrimary,
    onPrimary = GuideLensColors.LightOnPrimary,
    primaryContainer = GuideLensColors.LightPrimaryContainer,
    onPrimaryContainer = GuideLensColors.LightOnPrimaryContainer,
    secondary = GuideLensColors.LightSecondary,
    onSecondary = GuideLensColors.LightOnSecondary,
    secondaryContainer = GuideLensColors.LightSecondaryContainer,
    onSecondaryContainer = GuideLensColors.LightOnSecondaryContainer,
    background = GuideLensColors.LightBackground,
    onBackground = GuideLensColors.LightOnBackground,
    surface = GuideLensColors.LightSurface,
    onSurface = GuideLensColors.LightOnSurface,
    surfaceVariant = GuideLensColors.LightSurfaceVariant,
    onSurfaceVariant = GuideLensColors.LightOnSurfaceVariant,
    outline = GuideLensColors.LightOutline,
    error = GuideLensColors.LightError,
    onError = GuideLensColors.LightOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = GuideLensColors.DarkPrimary,
    onPrimary = GuideLensColors.DarkOnPrimary,
    primaryContainer = GuideLensColors.DarkPrimaryContainer,
    onPrimaryContainer = GuideLensColors.DarkOnPrimaryContainer,
    secondary = GuideLensColors.DarkSecondary,
    onSecondary = GuideLensColors.DarkOnSecondary,
    secondaryContainer = GuideLensColors.DarkSecondaryContainer,
    onSecondaryContainer = GuideLensColors.DarkOnSecondaryContainer,
    background = GuideLensColors.DarkBackground,
    onBackground = GuideLensColors.DarkOnBackground,
    surface = GuideLensColors.DarkSurface,
    onSurface = GuideLensColors.DarkOnSurface,
    surfaceVariant = GuideLensColors.DarkSurfaceVariant,
    onSurfaceVariant = GuideLensColors.DarkOnSurfaceVariant,
    outline = GuideLensColors.DarkOutline,
    error = GuideLensColors.DarkError,
    onError = GuideLensColors.DarkOnError
)

class ThemeController {
    private val _isDarkTheme = mutableStateOf(false)
    val isDarkTheme: Boolean by _isDarkTheme

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }
}

@Composable
fun GuideLensTheme(
    themeController: ThemeController? = null, // Optional ThemeController for manual theme control
    useSystemTheme: Boolean = true, // Whether to follow system theme
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, // Enable dynamic colors on Android 12+
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isSystemDarkTheme = isSystemInDarkTheme()
    val isDarkTheme = if (useSystemTheme) {
        isSystemDarkTheme
    } else {
        themeController?.isDarkTheme ?: isSystemDarkTheme
    }

    val colors = if (isDarkTheme) GuideLensColors else GuideLensColors
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalGuideLensColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}