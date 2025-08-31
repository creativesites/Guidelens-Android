package com.craftflowtechnologies.guidelens.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Enterprise-grade localization manager for Zambia
 * Supports English (primary), Bemba, Nyanja, Tonga, and Lozi languages
 */
class ZambianGuideLocalization private constructor() {
    
    companion object {
        @JvmStatic
        val instance = ZambianGuideLocalization()
        
        // Zambian locale constants
        const val LOCALE_ENGLISH_ZM = "en-ZM"
        const val LOCALE_BEMBA = "bem-ZM"
        const val LOCALE_NYANJA = "ny-ZM"
        const val LOCALE_TONGA = "to-ZM"
        const val LOCALE_LOZI = "lz-ZM"
        
        // Currency
        const val CURRENCY_CODE = "ZMW"
        const val CURRENCY_SYMBOL = "K"
        
        // Time zone
        const val TIME_ZONE = "Africa/Lusaka"
    }
    
    // Current locale state
    var currentLocale: String = LOCALE_ENGLISH_ZM
        private set
    
    // Localized strings for UI components
    private val guideLensStrings = mapOf(
        LOCALE_ENGLISH_ZM to EnglishZambianStrings(),
        LOCALE_BEMBA to BembaStrings(),
        LOCALE_NYANJA to NyanjaStrings(),
        LOCALE_TONGA to TongaStrings(),
        LOCALE_LOZI to LoziStrings()
    )
    
    fun setLocale(locale: String) {
        if (guideLensStrings.containsKey(locale)) {
            currentLocale = locale
        }
    }
    
    fun getString(key: StringKey): String {
        return guideLensStrings[currentLocale]?.get(key) ?: 
               guideLensStrings[LOCALE_ENGLISH_ZM]?.get(key) ?: key.name
    }
    
    fun getFormattedString(key: StringKey, vararg args: Any): String {
        val template = getString(key)
        return String.format(template, *args)
    }
    
    // Zambian-specific formatting
    fun formatCurrency(amount: Double): String {
        return "$CURRENCY_SYMBOL%.2f".format(amount)
    }
    
    fun formatDateTime(timestamp: Long): String {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        return formatter.format(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.of(TIME_ZONE)))
    }
    
    // Cultural preferences
    fun getPreferredDateFormat(): String = when(currentLocale) {
        LOCALE_ENGLISH_ZM -> "dd/MM/yyyy"
        else -> "dd/MM/yyyy"
    }
    
    fun getPreferredTimeFormat(): String = when(currentLocale) {
        LOCALE_ENGLISH_ZM -> "HH:mm"
        else -> "HH:mm"
    }
}

// String keys enum for type safety
enum class StringKey {
    // App general
    APP_NAME,
    WELCOME,
    CONTINUE,
    CANCEL,
    OK,
    YES,
    NO,
    DONE,
    NEXT,
    PREVIOUS,
    SAVE,
    LOAD,
    DELETE,
    EDIT,
    SHARE,
    SETTINGS,
    HELP,
    ABOUT,
    
    // Authentication
    LOGIN,
    LOGOUT,
    REGISTER,
    USERNAME,
    PASSWORD,
    EMAIL,
    PHONE_NUMBER,
    FORGOT_PASSWORD,
    
    // Agents
    COOKING_AGENT,
    CRAFTING_AGENT,
    DIY_AGENT,
    COMPANION_AGENT,
    
    // Agent descriptions
    COOKING_AGENT_DESC,
    CRAFTING_AGENT_DESC,
    DIY_AGENT_DESC,
    COMPANION_AGENT_DESC,
    
    // Cooking specific
    RECIPE,
    INGREDIENTS,
    INSTRUCTIONS,
    COOKING_TIME,
    PREP_TIME,
    SERVINGS,
    DIFFICULTY,
    EASY,
    MEDIUM,
    HARD,
    STEP,
    NEXT_STEP,
    COMPLETE_STEP,
    START_COOKING,
    PAUSE_COOKING,
    TIMER,
    
    // Common Zambian foods and terms
    NSHIMA,
    KAPENTA,
    BREAM,
    CHIBWABWA,
    DELELE,
    IMPWA,
    SAMP,
    BEANS,
    MEALIE_MEAL,
    GROUNDNUTS,
    SWEET_POTATO,
    CASSAVA,
    
    // Voice and video
    LISTENING,
    SPEAKING,
    START_RECORDING,
    STOP_RECORDING,
    VOICE_MODE,
    VIDEO_MODE,
    MUTE,
    UNMUTE,
    CAMERA_ON,
    CAMERA_OFF,
    
    // Measurements (Metric - Zambian standard)
    GRAM,
    KILOGRAM,
    MILLILITER,
    LITER,
    TEASPOON,
    TABLESPOON,
    CUP,
    PINCH,
    
    // Time related
    MINUTE,
    MINUTES,
    HOUR,
    HOURS,
    DAY,
    DAYS,
    WEEK,
    WEEKS,
    MONTH,
    MONTHS,
    
    // Status messages
    THINKING,
    PROCESSING,
    READY,
    COMPLETED,
    IN_PROGRESS,
    PAUSED,
    ERROR,
    SUCCESS,
    
    // Common greetings and phrases
    GOOD_MORNING,
    GOOD_AFTERNOON,
    GOOD_EVENING,
    GOOD_NIGHT,
    THANK_YOU,
    PLEASE,
    EXCUSE_ME,
    SORRY,
    
    // Connectivity
    ONLINE,
    OFFLINE,
    CONNECTING,
    CONNECTION_ERROR,
    RETRY,
    
    // Permissions
    CAMERA_PERMISSION,
    MICROPHONE_PERMISSION,
    STORAGE_PERMISSION,
    LOCATION_PERMISSION,
    PERMISSION_REQUIRED,
    GRANT_PERMISSION
}

// Base interface for language strings
interface GuideLanguageStrings {
    fun get(key: StringKey): String
}

// English (Zambian) - Default language
class EnglishZambianStrings : GuideLanguageStrings {
    private val strings = mapOf(
        StringKey.APP_NAME to "GuideLens",
        StringKey.WELCOME to "Welcome to GuideLens",
        StringKey.CONTINUE to "Continue",
        StringKey.CANCEL to "Cancel",
        StringKey.OK to "OK",
        StringKey.YES to "Yes",
        StringKey.NO to "No",
        StringKey.DONE to "Done",
        StringKey.NEXT to "Next",
        StringKey.PREVIOUS to "Previous",
        StringKey.SAVE to "Save",
        StringKey.LOAD to "Load",
        StringKey.DELETE to "Delete",
        StringKey.EDIT to "Edit",
        StringKey.SHARE to "Share",
        StringKey.SETTINGS to "Settings",
        StringKey.HELP to "Help",
        StringKey.ABOUT to "About",
        
        // Agents
        StringKey.COOKING_AGENT to "Cooking Assistant",
        StringKey.CRAFTING_AGENT to "Crafting Guru",
        StringKey.DIY_AGENT to "DIY Helper",
        StringKey.COMPANION_AGENT to "Buddy",
        
        StringKey.COOKING_AGENT_DESC to "Your personal cooking guide for traditional and modern recipes",
        StringKey.CRAFTING_AGENT_DESC to "Expert help with arts, crafts, and creative projects",
        StringKey.DIY_AGENT_DESC to "Home improvement and repair guidance",
        StringKey.COMPANION_AGENT_DESC to "Friendly assistant for learning and everyday tasks",
        
        // Cooking
        StringKey.RECIPE to "Recipe",
        StringKey.INGREDIENTS to "Ingredients",
        StringKey.INSTRUCTIONS to "Instructions",
        StringKey.COOKING_TIME to "Cooking Time",
        StringKey.PREP_TIME to "Prep Time",
        StringKey.SERVINGS to "Servings",
        StringKey.DIFFICULTY to "Difficulty",
        StringKey.EASY to "Easy",
        StringKey.MEDIUM to "Medium",
        StringKey.HARD to "Hard",
        StringKey.STEP to "Step",
        StringKey.NEXT_STEP to "Next Step",
        StringKey.COMPLETE_STEP to "Complete Step",
        StringKey.START_COOKING to "Start Cooking",
        StringKey.TIMER to "Timer",
        
        // Zambian foods
        StringKey.NSHIMA to "Nshima",
        StringKey.KAPENTA to "Kapenta",
        StringKey.BREAM to "Bream",
        StringKey.CHIBWABWA to "Chibwabwa (Pumpkin Leaves)",
        StringKey.DELELE to "Delele (Okra)",
        StringKey.IMPWA to "Impwa (Eggplant)",
        StringKey.SAMP to "Samp",
        StringKey.BEANS to "Beans",
        StringKey.MEALIE_MEAL to "Mealie Meal",
        StringKey.GROUNDNUTS to "Groundnuts",
        StringKey.SWEET_POTATO to "Sweet Potato",
        StringKey.CASSAVA to "Cassava",
        
        // Voice
        StringKey.LISTENING to "Listening...",
        StringKey.SPEAKING to "Speaking...",
        StringKey.VOICE_MODE to "Voice Mode",
        StringKey.VIDEO_MODE to "Video Mode",
        StringKey.MUTE to "Mute",
        StringKey.UNMUTE to "Unmute",
        
        // Status
        StringKey.THINKING to "Thinking...",
        StringKey.PROCESSING to "Processing...",
        StringKey.READY to "Ready",
        StringKey.COMPLETED to "Completed",
        StringKey.IN_PROGRESS to "In Progress",
        
        // Greetings
        StringKey.GOOD_MORNING to "Good morning",
        StringKey.GOOD_AFTERNOON to "Good afternoon",
        StringKey.GOOD_EVENING to "Good evening",
        StringKey.THANK_YOU to "Thank you",
        StringKey.PLEASE to "Please"
    )
    
    override fun get(key: StringKey): String = strings[key] ?: key.name
}

// Bemba language strings
class BembaStrings : GuideLanguageStrings {
    private val strings = mapOf(
        StringKey.APP_NAME to "GuideLens",
        StringKey.WELCOME to "Mwaiseni ku GuideLens",
        StringKey.CONTINUE to "Pitilila",
        StringKey.CANCEL to "Siluka",
        StringKey.OK to "Nomba",
        StringKey.YES to "Ee",
        StringKey.NO to "Awe",
        StringKey.DONE to "Nalefilwa",
        StringKey.NEXT to "Ukutila",
        StringKey.PREVIOUS to "Ukwafika",
        
        // Cooking terms
        StringKey.COOKING_AGENT to "Umutemfwa wa Kulya",
        StringKey.RECIPE to "Ukulya",
        StringKey.NSHIMA to "Ubwali",
        StringKey.KAPENTA to "Utupele",
        
        // Greetings
        StringKey.GOOD_MORNING to "Mwabuka shani",
        StringKey.GOOD_AFTERNOON to "Mwalewa shani", 
        StringKey.GOOD_EVENING to "Mwatamfya shani",
        StringKey.THANK_YOU to "Natotela"
    )
    
    override fun get(key: StringKey): String = strings[key] ?: EnglishZambianStrings().get(key)
}

// Nyanja language strings
class NyanjaStrings : GuideLanguageStrings {
    private val strings = mapOf(
        StringKey.APP_NAME to "GuideLens",
        StringKey.WELCOME to "Takulandirani ku GuideLens",
        StringKey.CONTINUE to "Pitirizani",
        StringKey.CANCEL to "Lekani",
        StringKey.OK to "Chabwino",
        StringKey.YES to "Inde",
        StringKey.NO to "Ayi",
        StringKey.DONE to "Zamaliza",
        
        // Cooking
        StringKey.COOKING_AGENT to "Wothandizira Kuphika",
        StringKey.NSHIMA to "Nsima",
        
        // Greetings
        StringKey.GOOD_MORNING to "Mwadzuka bwanji",
        StringKey.GOOD_AFTERNOON to "Mwaswera bwanji",
        StringKey.GOOD_EVENING to "Mwadzanja bwanji",
        StringKey.THANK_YOU to "Zikomo"
    )
    
    override fun get(key: StringKey): String = strings[key] ?: EnglishZambianStrings().get(key)
}

// Tonga language strings
class TongaStrings : GuideLanguageStrings {
    private val strings = mapOf(
        StringKey.APP_NAME to "GuideLens",
        StringKey.WELCOME to "Kayi ku GuideLens",
        StringKey.GOOD_MORNING to "Wauka buti",
        StringKey.GOOD_EVENING to "Walaza buti",
        StringKey.THANK_YOU to "Lumbanya"
    )
    
    override fun get(key: StringKey): String = strings[key] ?: EnglishZambianStrings().get(key)
}

// Lozi language strings  
class LoziStrings : GuideLanguageStrings {
    private val strings = mapOf(
        StringKey.APP_NAME to "GuideLens",
        StringKey.WELCOME to "Lu tiile ku GuideLens",
        StringKey.GOOD_MORNING to "Lu kotile hande",
        StringKey.GOOD_EVENING to "Ku manuka hande", 
        StringKey.THANK_YOU to "Ni lumela"
    )
    
    override fun get(key: StringKey): String = strings[key] ?: EnglishZambianStrings().get(key)
}

// Composition local for accessing localization throughout the app
val LocalGuideLensLocalization = staticCompositionLocalOf<ZambianGuideLocalization> {
    error("ZambianGuideLocalization not provided")
}

@Composable
fun GuideLocalizationProvider(
    localization: ZambianGuideLocalization = ZambianGuideLocalization.instance,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGuideLensLocalization provides localization,
        content = content
    )
}

// Helper extension for easy access to strings
@Composable
fun stringResource(key: StringKey): String {
    return LocalGuideLensLocalization.current.getString(key)
}

@Composable
fun stringResource(key: StringKey, vararg args: Any): String {
    return LocalGuideLensLocalization.current.getFormattedString(key, *args)
}

// Zambian cultural preferences for UI
object ZambianGuideDefaults {
    // Typography preferences for Zambian languages
    val primaryFontFamily = FontFamily.Default
    val titleFontSize = 20.sp
    val bodyFontSize = 16.sp
    val captionFontSize = 12.sp
    
    // Cultural color preferences
    val zambianFlag = listOf(
        android.graphics.Color.parseColor("#00A651"), // Green
        android.graphics.Color.parseColor("#FF6B00"), // Orange  
        android.graphics.Color.parseColor("#DE2010"), // Red
        android.graphics.Color.parseColor("#000000")  // Black
    )
    
    // Default to Zambia locale and settings
    fun getDefaultLocale(): String = ZambianGuideLocalization.LOCALE_ENGLISH_ZM
    
    // Business hours in Zambian context (CAT - Central Africa Time)
    val businessHoursStart = 8 // 8:00 AM
    val businessHoursEnd = 17   // 5:00 PM
    
    // Weekend days (Friday afternoon prayers, Sunday church)
    val weekendDays = listOf(6, 7) // Saturday, Sunday
    
    // Public holidays and cultural considerations
    val culturalHolidays = listOf(
        "Independence Day", // October 24
        "Unity Day",       // First Monday in July
        "Heroes Day",      // First Monday in July
        "Farmers Day",     // First Monday in August
        "Youth Day",       // First Monday in March
        "Labour Day",      // May 1
        "Africa Day"       // May 25
    )
}