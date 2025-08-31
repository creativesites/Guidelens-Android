package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Accessibility utilities and enhancements for GuideLens
 * Ensures the app is usable by people with diverse abilities
 */
object AccessibilityUtils {
    
    /**
     * Semantic roles for better screen reader support
     */
    enum class GuideLensRole {
        AGENT_RESPONSE,
        USER_MESSAGE,
        SYSTEM_STATUS,
        RECIPE_STEP,
        SAFETY_WARNING,
        CULTURAL_GREETING,
        NAVIGATION_ITEM
    }
    
    /**
     * Content importance levels for assistive technologies
     */
    enum class ContentImportance {
        CRITICAL,    // Safety warnings, errors
        HIGH,        // Main content, responses
        MEDIUM,      // Secondary information
        LOW          // Decorative elements
    }
    
    /**
     * Accessibility settings for user customization
     */
    data class AccessibilitySettings(
        val largeText: Boolean = false,
        val highContrast: Boolean = false,
        val reduceMotion: Boolean = false,
        val screenReaderEnabled: Boolean = false,
        val voiceNavigationEnabled: Boolean = false,
        val hapticFeedbackEnabled: Boolean = true,
        val fontSizeMultiplier: Float = 1.0f,
        val buttonSizeMultiplier: Float = 1.0f,
        val colorBlindnessMode: ColorBlindnessMode = ColorBlindnessMode.NONE
    )
    
    enum class ColorBlindnessMode {
        NONE,
        PROTANOPIA,      // Red-blind
        DEUTERANOPIA,    // Green-blind
        TRITANOPIA,      // Blue-blind
        MONOCHROME       // Complete color blindness
    }
    
    /**
     * Apply accessibility enhancements to text
     */
    @Composable
    fun AccessibleText(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = 14.sp,
        fontWeight: FontWeight = FontWeight.Normal,
        color: Color = LocalContentColor.current,
        role: GuideLensRole = GuideLensRole.USER_MESSAGE,
        importance: ContentImportance = ContentImportance.MEDIUM,
        accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
        customSemantics: (SemanticsPropertyReceiver.() -> Unit)? = null
    ) {
        val adjustedFontSize = fontSize * accessibilitySettings.fontSizeMultiplier
        val adjustedColor = if (accessibilitySettings.highContrast) {
            enhanceContrastColor(color)
        } else {
            color
        }
        
        Text(
            text = text,
            modifier = modifier.semantics {
                // Apply role-specific semantics
                applyRoleSemantics(role, this)
                
                // Set content importance
                when (importance) {
                    ContentImportance.CRITICAL -> {
                        liveRegion = LiveRegionMode.Assertive
                        contentDescription = "Critical: $text"
                    }
                    ContentImportance.HIGH -> {
                        liveRegion = LiveRegionMode.Polite
                    }
                    ContentImportance.MEDIUM -> {
                        // Default semantics
                    }
                    ContentImportance.LOW -> {
                        invisibleToUser()
                    }
                }
                
                // Custom semantics
                customSemantics?.invoke(this)
            },
            fontSize = adjustedFontSize,
            fontWeight = fontWeight,
            color = adjustedColor
        )
    }
    
    /**
     * Apply accessibility enhancements to buttons
     */
    @Composable
    fun AccessibleButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
        contentDescription: String? = null,
        role: GuideLensRole = GuideLensRole.NAVIGATION_ITEM,
        content: @Composable () -> Unit
    ) {
        val adjustedSize = 48.dp * accessibilitySettings.buttonSizeMultiplier
        val minimumTouchTarget = 48.dp // Material Design minimum
        val actualSize = maxOf(adjustedSize, minimumTouchTarget)
        
        Button(
            onClick = {
                if (accessibilitySettings.hapticFeedbackEnabled) {
                    // TODO: Add haptic feedback
                }
                onClick()
            },
            modifier = modifier
                .size(actualSize)
                .semantics {
                    applyRoleSemantics(role, this)
                    contentDescription?.let {
                        this.contentDescription = it
                    }
                    if (!enabled) {
                        disabled()
                    }
                },
            enabled = enabled
        ) {
            content()
        }
    }
    
    /**
     * Accessible card component with proper focus handling
     */
    @Composable
    fun AccessibleCard(
        onClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier,
        role: GuideLensRole = GuideLensRole.NAVIGATION_ITEM,
        contentDescription: String? = null,
        accessibilitySettings: AccessibilitySettings = AccessibilitySettings(),
        content: @Composable () -> Unit
    ) {
        val cardModifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics {
                applyRoleSemantics(role, this)
                contentDescription?.let {
                    this.contentDescription = it
                }
                onClick?.let {
                    this.onClick { 
                        if (accessibilitySettings.hapticFeedbackEnabled) {
                            // TODO: Add haptic feedback
                        }
                        it()
                        true 
                    }
                }
            }
        
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = cardModifier
            ) {
                content()
            }
        } else {
            Card(
                modifier = cardModifier
            ) {
                content()
            }
        }
    }
    
    /**
     * Accessible navigation announcement for screen readers
     */
    @Composable
    fun AccessibilityAnnouncement(
        announcement: String,
        priority: AnnouncementPriority = AnnouncementPriority.POLITE
    ) {
        val semanticsModifier = Modifier.semantics {
            liveRegion = when (priority) {
                AnnouncementPriority.ASSERTIVE -> LiveRegionMode.Assertive
                AnnouncementPriority.POLITE -> LiveRegionMode.Polite
            }
            contentDescription = announcement
            invisibleToUser() // Don't show visually, just announce
        }
        
        // Invisible component that only provides semantics
        androidx.compose.foundation.layout.Box(
            modifier = semanticsModifier
        )
    }
    
    enum class AnnouncementPriority {
        POLITE,      // Wait for current speech to finish
        ASSERTIVE    // Interrupt current speech
    }
    
    /**
     * Apply role-specific semantic properties
     */
    private fun applyRoleSemantics(role: GuideLensRole, semantics: SemanticsPropertyReceiver) {
        with(semantics) {
            when (role) {
                GuideLensRole.AGENT_RESPONSE -> {
                    this.role = Role.Button
                    stateDescription = "AI response"
                }
                GuideLensRole.USER_MESSAGE -> {
                    this.role = Role.Button
                    stateDescription = "User message"
                }
                GuideLensRole.SYSTEM_STATUS -> {
                    this.role = Role.Tab
                    liveRegion = LiveRegionMode.Polite
                }
                GuideLensRole.RECIPE_STEP -> {
                    this.role = Role.Button
                    stateDescription = "Recipe step"
                }
                GuideLensRole.SAFETY_WARNING -> {
                    this.role = Role.Button
                    stateDescription = "Safety warning"
                    liveRegion = LiveRegionMode.Assertive
                }
                GuideLensRole.CULTURAL_GREETING -> {
                    this.role = Role.Button
                    stateDescription = "Cultural greeting"
                }
                GuideLensRole.NAVIGATION_ITEM -> {
                    this.role = Role.Button
                    stateDescription = "Navigation"
                }
            }
        }
    }
    
    /**
     * Enhance color contrast for high contrast mode
     */
    private fun enhanceContrastColor(color: Color): Color {
        // Simple contrast enhancement - in production, use proper WCAG calculations
        return if (isLightColor(color)) {
            Color.Black
        } else {
            Color.White
        }
    }
    
    /**
     * Check if a color is considered light
     */
    private fun isLightColor(color: Color): Boolean {
        val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
        return luminance > 0.5
    }
    
    /**
     * Adapt colors for color blindness
     */
    fun adaptColorForColorBlindness(
        color: Color, 
        mode: ColorBlindnessMode
    ): Color {
        return when (mode) {
            ColorBlindnessMode.NONE -> color
            ColorBlindnessMode.PROTANOPIA -> simulateProtanopia(color)
            ColorBlindnessMode.DEUTERANOPIA -> simulateDeuteranopia(color)
            ColorBlindnessMode.TRITANOPIA -> simulateTritanopia(color)
            ColorBlindnessMode.MONOCHROME -> Color(
                red = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue,
                green = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue,
                blue = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue,
                alpha = color.alpha
            )
        }
    }
    
    private fun simulateProtanopia(color: Color): Color {
        // Simplified protanopia simulation
        return Color(
            red = 0.567f * color.red + 0.433f * color.green,
            green = 0.558f * color.red + 0.442f * color.green,
            blue = color.blue,
            alpha = color.alpha
        )
    }
    
    private fun simulateDeuteranopia(color: Color): Color {
        // Simplified deuteranopia simulation
        return Color(
            red = 0.625f * color.red + 0.375f * color.green,
            green = 0.7f * color.red + 0.3f * color.green,
            blue = color.blue,
            alpha = color.alpha
        )
    }
    
    private fun simulateTritanopia(color: Color): Color {
        // Simplified tritanopia simulation
        return Color(
            red = color.red,
            green = 0.95f * color.green + 0.05f * color.blue,
            blue = 0.433f * color.green + 0.567f * color.blue,
            alpha = color.alpha
        )
    }
    
    /**
     * Voice navigation utilities
     */
    object VoiceNavigation {
        val supportedCommands = listOf(
            "navigate back",
            "open settings",
            "start new chat",
            "switch to cooking",
            "switch to crafting",
            "switch to diy",
            "switch to buddy",
            "take photo",
            "start recording",
            "stop recording",
            "repeat last message",
            "increase text size",
            "decrease text size",
            "toggle dark mode",
            "help"
        )
        
        fun processVoiceCommand(command: String): VoiceCommand? {
            val normalizedCommand = command.lowercase().trim()
            
            return when {
                normalizedCommand.contains("navigate") && normalizedCommand.contains("back") -> 
                    VoiceCommand.NAVIGATE_BACK
                normalizedCommand.contains("settings") -> 
                    VoiceCommand.OPEN_SETTINGS
                normalizedCommand.contains("new chat") -> 
                    VoiceCommand.NEW_CHAT
                normalizedCommand.contains("cooking") -> 
                    VoiceCommand.SWITCH_AGENT("cooking")
                normalizedCommand.contains("crafting") -> 
                    VoiceCommand.SWITCH_AGENT("crafting")
                normalizedCommand.contains("diy") -> 
                    VoiceCommand.SWITCH_AGENT("diy")
                normalizedCommand.contains("buddy") -> 
                    VoiceCommand.SWITCH_AGENT("buddy")
                normalizedCommand.contains("photo") -> 
                    VoiceCommand.TAKE_PHOTO
                normalizedCommand.contains("dark mode") -> 
                    VoiceCommand.TOGGLE_THEME
                normalizedCommand.contains("help") -> 
                    VoiceCommand.HELP
                else -> null
            }
        }
    }
    
    sealed class VoiceCommand {
        object NAVIGATE_BACK : VoiceCommand()
        object OPEN_SETTINGS : VoiceCommand()
        object NEW_CHAT : VoiceCommand()
        data class SWITCH_AGENT(val agentId: String) : VoiceCommand()
        object TAKE_PHOTO : VoiceCommand()
        object START_RECORDING : VoiceCommand()
        object STOP_RECORDING : VoiceCommand()
        object REPEAT_LAST : VoiceCommand()
        object INCREASE_TEXT_SIZE : VoiceCommand()
        object DECREASE_TEXT_SIZE : VoiceCommand()
        object TOGGLE_THEME : VoiceCommand()
        object HELP : VoiceCommand()
    }
}

/**
 * Performance optimization utilities
 */
object PerformanceUtils {
    
    /**
     * Lazy list performance optimizations
     */
    @Stable
    data class LazyListConfig(
        val prefetchDistance: Int = 3,
        val beyondBoundsItemCount: Int = 5,
        val reverseLayout: Boolean = false
    )
    
    /**
     * Image loading performance configurations
     */
    @Stable
    data class ImageConfig(
        val maxImageSize: Dp = 400.dp,
        val compressionQuality: Float = 0.8f,
        val enableMemoryCache: Boolean = true,
        val enableDiskCache: Boolean = true,
        val placeholderEnabled: Boolean = true
    )
    
    /**
     * Animation performance settings
     */
    @Stable
    data class AnimationConfig(
        val enableAnimations: Boolean = true,
        val reducedMotion: Boolean = false,
        val animationDuration: Long = 300L,
        val animationEasing: androidx.compose.animation.core.Easing = 
            androidx.compose.animation.core.FastOutSlowInEasing
    )
    
    /**
     * Memory management utilities
     */
    fun isLowMemoryDevice(): Boolean {
        // TODO: Implement actual memory checking
        return false
    }
    
    fun getOptimalImageSize(targetSize: Dp, density: androidx.compose.ui.unit.Density): Int {
        return with(density) { targetSize.toPx().toInt() }
    }
    
    /**
     * Battery optimization
     */
    fun isBatteryOptimizationNeeded(): Boolean {
        // TODO: Implement battery level checking
        return false
    }
    
    fun getOptimizedAnimationConfig(): AnimationConfig {
        return if (isBatteryOptimizationNeeded()) {
            AnimationConfig(
                enableAnimations = false,
                reducedMotion = true,
                animationDuration = 150L
            )
        } else {
            AnimationConfig()
        }
    }
}

/**
 * Composable for applying accessibility and performance optimizations
 */
@Composable
fun rememberAccessibilitySettings(): AccessibilityUtils.AccessibilitySettings {
    // TODO: Load from user preferences or system settings
    return remember {
        AccessibilityUtils.AccessibilitySettings()
    }
}

@Composable
fun rememberPerformanceConfig(): PerformanceUtils.AnimationConfig {
    return remember {
        PerformanceUtils.getOptimizedAnimationConfig()
    }
}