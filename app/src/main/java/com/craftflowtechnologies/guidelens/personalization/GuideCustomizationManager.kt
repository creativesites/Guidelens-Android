package com.craftflowtechnologies.guidelens.personalization

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.localization.ZambianGuideLocalization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Enterprise-grade customization manager for GuideLens
 * Handles all user preferences, accessibility settings, and personalization
 */
class GuideCustomizationManager private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuideCustomizationManager()
    }
    
    // State flows for reactive updates
    private val _currentTheme = MutableStateFlow(GuideThemeMode.SYSTEM)
    val currentTheme: StateFlow<GuideThemeMode> = _currentTheme.asStateFlow()
    
    private val _fontSize = MutableStateFlow(GuideFontSize.MEDIUM)
    val fontSize: StateFlow<GuideFontSize> = _fontSize.asStateFlow()
    
    private val _locale = MutableStateFlow(ZambianGuideLocalization.LOCALE_ENGLISH_ZM)
    val locale: StateFlow<String> = _locale.asStateFlow()
    
    private val _colorScheme = MutableStateFlow(GuideColorScheme.ZAMBIAN_DEFAULT)
    val colorScheme: StateFlow<GuideColorScheme> = _colorScheme.asStateFlow()
    
    private val _accessibility = MutableStateFlow(GuideAccessibilitySettings())
    val accessibility: StateFlow<GuideAccessibilitySettings> = _accessibility.asStateFlow()
    
    private val _animations = MutableStateFlow(GuideAnimationSettings())
    val animations: StateFlow<GuideAnimationSettings> = _animations.asStateFlow()
    
    private val _userPreferences = MutableStateFlow(GuideUserPreferences())
    val userPreferences: StateFlow<GuideUserPreferences> = _userPreferences.asStateFlow()
    
    // Update methods
    fun updateTheme(theme: GuideThemeMode) {
        _currentTheme.value = theme
    }
    
    fun updateFontSize(size: GuideFontSize) {
        _fontSize.value = size
    }
    
    fun updateLocale(locale: String) {
        _locale.value = locale
        ZambianGuideLocalization.instance.setLocale(locale)
    }
    
    fun updateColorScheme(scheme: GuideColorScheme) {
        _colorScheme.value = scheme
    }
    
    fun updateAccessibility(settings: GuideAccessibilitySettings) {
        _accessibility.value = settings
    }
    
    fun updateAnimations(settings: GuideAnimationSettings) {
        _animations.value = settings
    }
    
    fun updateUserPreferences(preferences: GuideUserPreferences) {
        _userPreferences.value = preferences
    }
    
    // Convenience methods
    fun toggleHighContrast() {
        val current = _accessibility.value
        _accessibility.value = current.copy(highContrast = !current.highContrast)
    }
    
    fun toggleReducedMotion() {
        val current = _animations.value
        _animations.value = current.copy(reducedMotion = !current.reducedMotion)
    }
    
    // Initialize with defaults for Zambia
    init {
        // Set Zambian defaults
        _locale.value = ZambianGuideLocalization.LOCALE_ENGLISH_ZM
        _colorScheme.value = GuideColorScheme.ZAMBIAN_DEFAULT
    }
}

// Theme configuration
enum class GuideThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    HIGH_CONTRAST_LIGHT,
    HIGH_CONTRAST_DARK
}

// Font size options
enum class GuideFontSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
    ACCESSIBILITY(1.5f)
}

// Color scheme definitions
enum class GuideColorScheme(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color
) {
    ZAMBIAN_DEFAULT(
        primary = Color(0xFF00A651),     // Zambian flag green
        secondary = Color(0xFFFF6B00),   // Zambian flag orange
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F)
    ),
    
    ZAMBIAN_HERITAGE(
        primary = Color(0xFFDE2010),     // Zambian flag red
        secondary = Color(0xFF00A651),   // Zambian flag green
        background = Color(0xFFFFF8F0),
        surface = Color(0xFFFFF8F0),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF000000), // Zambian flag black
        onSurface = Color(0xFF000000)
    ),
    
    COPPER_BELT(
        primary = Color(0xFFB87333),     // Copper color
        secondary = Color(0xFF8B4513),   // Saddle brown
        background = Color(0xFFFAF0E6),
        surface = Color(0xFFFAF0E6),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF2F1B14),
        onSurface = Color(0xFF2F1B14)
    ),
    
    VICTORIA_FALLS(
        primary = Color(0xFF0077BE),     // Water blue
        secondary = Color(0xFF87CEEB),   // Sky blue
        background = Color(0xFFF0F8FF),
        surface = Color(0xFFF0F8FF),
        onPrimary = Color.White,
        onSecondary = Color(0xFF003F5C),
        onBackground = Color(0xFF1E3A8A),
        onSurface = Color(0xFF1E3A8A)
    ),
    
    KAFUE_SUNSET(
        primary = Color(0xFFFF4500),     // Orange red
        secondary = Color(0xFFFFD700),   // Gold
        background = Color(0xFFFFF8DC),
        surface = Color(0xFFFFF8DC),
        onPrimary = Color.White,
        onSecondary = Color(0xFF8B4513),
        onBackground = Color(0xFF8B0000),
        onSurface = Color(0xFF8B0000)
    )
}

// Accessibility settings
data class GuideAccessibilitySettings(
    val highContrast: Boolean = false,
    val largeText: Boolean = false,
    val boldText: Boolean = false,
    val reduceTransparency: Boolean = false,
    val voiceOver: Boolean = false,
    val hapticFeedback: Boolean = true,
    val soundFeedback: Boolean = true,
    val visualIndicators: Boolean = false,
    val screenReaderSupport: Boolean = false,
    val keyboardNavigation: Boolean = false
)

// Animation settings
data class GuideAnimationSettings(
    val reducedMotion: Boolean = false,
    val animationDuration: GuideAnimationSpeed = GuideAnimationSpeed.NORMAL,
    val enableParticleEffects: Boolean = true,
    val enableTransitions: Boolean = true,
    val enableGestures: Boolean = true
)

enum class GuideAnimationSpeed(val multiplier: Float) {
    SLOW(1.5f),
    NORMAL(1.0f),
    FAST(0.75f),
    INSTANT(0.1f)
}

// User preferences specific to cooking and guidance
data class GuideUserPreferences(
    val preferredAgent: String = "cooking",
    val defaultMode: GuideInteractionMode = GuideInteractionMode.TEXT,
    val autoTranscription: Boolean = true,
    val voiceActivation: Boolean = false,
    val backgroundMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val offlineMode: Boolean = false,
    val dataSaver: Boolean = false,
    val measurementUnit: GuideMeasurementUnit = GuideMeasurementUnit.METRIC,
    val temperatureUnit: GuideTemperatureUnit = GuideTemperatureUnit.CELSIUS,
    val timeFormat: GuideTimeFormat = GuideTimeFormat.HOUR_24,
    val defaultDifficulty: GuideDifficulty = GuideDifficulty.BEGINNER,
    val showHints: Boolean = true,
    val autoNext: Boolean = false,
    val sessionTimeout: GuideSessionTimeout = GuideSessionTimeout.MINUTES_30
)

enum class GuideInteractionMode {
    TEXT,
    VOICE,
    VIDEO,
    MIXED
}

enum class GuideMeasurementUnit {
    METRIC,      // Default for Zambia (grams, liters, celsius)
    IMPERIAL,    // For international users
    MIXED        // Both shown
}

enum class GuideTemperatureUnit {
    CELSIUS,     // Default for Zambia
    FAHRENHEIT,
    BOTH
}

enum class GuideTimeFormat {
    HOUR_12,
    HOUR_24     // Default for Zambia
}

enum class GuideDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class GuideSessionTimeout(val minutes: Int) {
    MINUTES_15(15),
    MINUTES_30(30),
    MINUTES_60(60),
    HOURS_2(120),
    NEVER(-1)
}

// Composable for providing customization context
val LocalGuideCustomization = staticCompositionLocalOf<GuideCustomizationManager> {
    error("GuideCustomizationManager not provided")
}

@Composable
fun GuideCustomizationProvider(
    customization: GuideCustomizationManager = GuideCustomizationManager.instance,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGuideCustomization provides customization,
        content = content
    )
}

// Convenient access to current settings
@Composable
fun rememberGuideTheme(): GuideThemeMode {
    val customization = LocalGuideCustomization.current
    return customization.currentTheme.collectAsState().value
}

@Composable
fun rememberGuideFontSize(): GuideFontSize {
    val customization = LocalGuideCustomization.current
    return customization.fontSize.collectAsState().value
}

@Composable
fun rememberGuideColorScheme(): GuideColorScheme {
    val customization = LocalGuideCustomization.current
    return customization.colorScheme.collectAsState().value
}

@Composable
fun rememberGuideAccessibility(): GuideAccessibilitySettings {
    val customization = LocalGuideCustomization.current
    return customization.accessibility.collectAsState().value
}

@Composable
fun rememberGuideAnimations(): GuideAnimationSettings {
    val customization = LocalGuideCustomization.current
    return customization.animations.collectAsState().value
}

@Composable
fun rememberGuideUserPreferences(): GuideUserPreferences {
    val customization = LocalGuideCustomization.current
    return customization.userPreferences.collectAsState().value
}

// Helper to determine if dark theme should be used
@Composable
fun shouldUseDarkTheme(): Boolean {
    val themeMode = rememberGuideTheme()
    val systemInDark = isSystemInDarkTheme()
    
    return when (themeMode) {
        GuideThemeMode.LIGHT, GuideThemeMode.HIGH_CONTRAST_LIGHT -> false
        GuideThemeMode.DARK, GuideThemeMode.HIGH_CONTRAST_DARK -> true
        GuideThemeMode.SYSTEM -> systemInDark
    }
}

// Helper to get appropriate animation duration based on settings
//@Composable
//fun getGuideAnimationDuration(baseDuration: Int): Int {
//    val animationSettings = rememberGuideAnimations()
//    val accessibility = rememberGuideAccessibility()
//
//    return when {
//        accessibility.reducedMotion -> (baseDuration * 0.1f).toInt()
//        animationSettings.reducedMotion -> (baseDuration * 0.5f).toInt()
//        else -> (baseDuration * animationSettings.animationDuration.multiplier).toInt()
//    }
//}

// Helper to get scaled font sizes
@Composable
fun getScaledFontSize(baseSize: androidx.compose.ui.unit.TextUnit): androidx.compose.ui.unit.TextUnit {
    val fontSize = rememberGuideFontSize()
    val accessibility = rememberGuideAccessibility()
    
    val scale = if (accessibility.largeText) fontSize.scale * 1.2f else fontSize.scale
    
    return when (baseSize.isSp) {
        true -> (baseSize.value * scale).sp
        else -> baseSize
    }
}

// Helper to get appropriate spacing based on accessibility
@Composable
fun getGuideSpacing(baseSpacing: Dp): Dp {
    val accessibility = rememberGuideAccessibility()
    
    return if (accessibility.largeText) baseSpacing * 1.25f else baseSpacing
}

// Cultural defaults specifically for Zambia
object ZambianGuideDefaults {
    
    // Default settings optimized for Zambian users
    fun getDefaultSettings() = GuideUserPreferences(
        measurementUnit = GuideMeasurementUnit.METRIC,
        temperatureUnit = GuideTemperatureUnit.CELSIUS,
        timeFormat = GuideTimeFormat.HOUR_24,
        defaultDifficulty = GuideDifficulty.BEGINNER,
        showHints = true,
        autoNext = false,
        sessionTimeout = GuideSessionTimeout.MINUTES_30
    )
    
    // Common Zambian cooking preferences
    val zambianCookingDefaults = mapOf(
        "defaultCookingMethod" to "boiling", // Common for nshima
        "preferredMeal" to "nshima",
        "defaultServings" to 4,
        "preferredSpiceLevel" to "mild",
        "commonIngredients" to listOf("mealie meal", "kapenta", "chibwabwa", "groundnuts")
    )
    
    // Cultural color associations
    val culturalColors = mapOf(
        "copper" to Color(0xFFB87333),      // Copper mining heritage
        "emerald" to Color(0xFF00A651),      // Emerald mining
        "sunset" to Color(0xFFFF4500),       // African sunset
        "earth" to Color(0xFF8B4513),        // Rich soil
        "water" to Color(0xFF0077BE)         // Victoria Falls
    )
}