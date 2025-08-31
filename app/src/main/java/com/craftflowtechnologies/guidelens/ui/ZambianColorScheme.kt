package com.craftflowtechnologies.guidelens.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Zambian-inspired color scheme for GuideLens
 * Colors represent Zambian heritage, culture, and natural elements
 */
data class ZambianColorScheme(
    // Primary Zambian Colors
    val emeraldGreen: Color = Color(0xFF00A86B),        // Zambian flag green
    val copperOrange: Color = Color(0xFFB87333),        // Copper mining heritage
    val eagleRed: Color = Color(0xFFDC143C),           // National eagle red
    val sunYellow: Color = Color(0xFFFFD700),          // African sun
    val riverBlue: Color = Color(0xFF4682B4),          // Zambezi river
    val earthBrown: Color = Color(0xFF8B4513),         // African soil
    
    // Secondary Zambian Colors
    val maizeGold: Color = Color(0xFFDAA520),          // Staple crop
    val forestGreen: Color = Color(0xFF228B22),        // Rich forests
    val skyBlue: Color = Color(0xFF87CEEB),            // Clear African skies
    val safariTan: Color = Color(0xFFD2B48C),          // Wildlife heritage
    val mineralSilver: Color = Color(0xFFC0C0C0),      // Mining industry
    val victoriaMist: Color = Color(0xFFE6F3FF),       // Victoria Falls mist
    
    // Cultural Colors
    val chitengePurple: Color = Color(0xFF9370DB),     // Traditional chitenge patterns
    val basketWeaveAmber: Color = Color(0xFFFFBF00),   // Traditional crafts
    val drummingRed: Color = Color(0xFF8B0000),        // Cultural ceremonies
    val pearlWhite: Color = Color(0xFFF8F8FF),         // Pure African light
    
    // Seasonal Colors
    val rainySeasonGreen: Color = Color(0xFF32CD32),   // Wet season vibrancy
    val drySeasonOcher: Color = Color(0xFFCC7722),     // Dry season earth
    val harvestGold: Color = Color(0xFFFFD700),        // Harvest time
    val floweringPink: Color = Color(0xFFFF69B4),      // Jacaranda blooms
    
    // UI Enhancement Colors
    val warmNeutral: Color = Color(0xFFF5F5DC),        // Warm background
    val coolNeutral: Color = Color(0xFFF0F8FF),        // Cool background
    val accentTeal: Color = Color(0xFF008080),         // Modern accent
    val accentCoral: Color = Color(0xFFFF7F50),        // Vibrant accent
) {
    companion object {
        /**
         * Default Zambian color scheme
         */
        fun default() = ZambianColorScheme()
        
        /**
         * High contrast version for accessibility
         */
        fun highContrast() = ZambianColorScheme(
            emeraldGreen = Color(0xFF006B3C),
            copperOrange = Color(0xFF8B4513),
            eagleRed = Color(0xFF8B0000),
            sunYellow = Color(0xFFFFD700),
            riverBlue = Color(0xFF0000CD),
            earthBrown = Color(0xFF654321)
        )
        
        /**
         * Muted version for reduced visual stress
         */
        fun muted() = ZambianColorScheme(
            emeraldGreen = Color(0xFF5F8A5F),
            copperOrange = Color(0xFF8B7355),
            eagleRed = Color(0xFF8B5A5A),
            sunYellow = Color(0xFFDDDD77),
            riverBlue = Color(0xFF5F7A8B),
            earthBrown = Color(0xFF8B7355)
        )
        
        /**
         * Seasonal color adjustments
         */
        fun forSeason(season: ZambianSeason): ZambianColorScheme {
            val base = default()
            return when (season) {
                ZambianSeason.WET_SEASON -> base.copy(
                    emeraldGreen = base.rainySeasonGreen,
                    earthBrown = Color(0xFF8B6914)
                )
                ZambianSeason.DRY_SEASON -> base.copy(
                    sunYellow = base.drySeasonOcher,
                    earthBrown = Color(0xFFD2691E)
                )
                ZambianSeason.HARVEST -> base.copy(
                    sunYellow = base.harvestGold,
                    emeraldGreen = Color(0xFF6B8E23)
                )
                ZambianSeason.FLOWERING -> base.copy(
                    emeraldGreen = Color(0xFF9ACD32),
                    sunYellow = base.floweringPink
                )
            }
        }
    }
    
    /**
     * Get color by semantic name
     */
    fun getColorByName(name: String): Color? {
        return when (name.lowercase()) {
            "emerald", "green", "flag" -> emeraldGreen
            "copper", "orange", "mining" -> copperOrange
            "eagle", "red", "bird" -> eagleRed
            "sun", "yellow", "gold" -> sunYellow
            "river", "blue", "zambezi" -> riverBlue
            "earth", "brown", "soil" -> earthBrown
            "maize", "grain" -> maizeGold
            "forest" -> forestGreen
            "sky" -> skyBlue
            "safari" -> safariTan
            "mineral", "silver" -> mineralSilver
            "mist", "falls" -> victoriaMist
            "purple", "chitenge" -> chitengePurple
            "amber", "basket" -> basketWeaveAmber
            "ceremony", "drumming" -> drummingRed
            "pearl", "white" -> pearlWhite
            else -> null
        }
    }
    
    /**
     * Get primary color palette as list
     */
    fun getPrimaryColors(): List<Color> {
        return listOf(
            emeraldGreen, copperOrange, eagleRed, 
            sunYellow, riverBlue, earthBrown
        )
    }
    
    /**
     * Get secondary color palette as list
     */
    fun getSecondaryColors(): List<Color> {
        return listOf(
            maizeGold, forestGreen, skyBlue,
            safariTan, mineralSilver, victoriaMist
        )
    }
    
    /**
     * Get cultural color palette as list
     */
    fun getCulturalColors(): List<Color> {
        return listOf(
            chitengePurple, basketWeaveAmber, 
            drummingRed, pearlWhite
        )
    }
}

/**
 * Zambian seasons for color theming
 */
enum class ZambianSeason(val displayName: String, val months: IntRange) {
    WET_SEASON("Rainy Season", 11..4),      // November to April
    DRY_SEASON("Dry Season", 5..8),         // May to August  
    HARVEST("Harvest Time", 4..6),          // April to June
    FLOWERING("Flowering Season", 9..11)    // September to November
}

/**
 * Color accessibility utilities
 */
object ZambianColorAccessibility {
    /**
     * Check if color combination meets WCAG contrast requirements
     */
    fun meetsContrastRequirement(
        foreground: Color, 
        background: Color, 
        level: ContrastLevel = ContrastLevel.AA
    ): Boolean {
        val contrast = calculateContrastRatio(foreground, background)
        return when (level) {
            ContrastLevel.AA -> contrast >= 4.5
            ContrastLevel.AAA -> contrast >= 7.0
        }
    }
    
    /**
     * Calculate contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val l1 = getRelativeLuminance(color1)
        val l2 = getRelativeLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Get relative luminance of a color
     */
    private fun getRelativeLuminance(color: Color): Double {
        val r = if (color.red <= 0.03928) color.red.toDouble() / 12.92 else ((color.red.toDouble() + 0.055) / 1.055).pow(2.4)
        val g = if (color.green <= 0.03928) color.green.toDouble() / 12.92 else ((color.green.toDouble() + 0.055) / 1.055).pow(2.4)
        val b = if (color.blue <= 0.03928) color.blue.toDouble() / 12.92 else ((color.blue.toDouble() + 0.055) / 1.055).pow(2.4)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    
    enum class ContrastLevel {
        AA, AAA
    }
}