package com.craftflowtechnologies.guidelens.personalization

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Zambian Localization and Customization Manager
 * Handles cultural customization, local languages, and Zambian-specific features
 */
class ZambianLocalizationManager(private val context: Context) {
    
    private val userDataManager = UserDataManager(context)
    private val preferencesRepository = ZambianPreferencesRepository(context, userDataManager)
    
    private val _currentLanguage = MutableStateFlow(ZambianLanguage.ENGLISH)
    val currentLanguage: StateFlow<ZambianLanguage> = _currentLanguage.asStateFlow()
    
    private val _currentRegion = MutableStateFlow(ZambianRegion.LUSAKA)
    val currentRegion: StateFlow<ZambianRegion> = _currentRegion.asStateFlow()
    
    private val _culturalSettings = MutableStateFlow(CulturalSettings())
    val culturalSettings: StateFlow<CulturalSettings> = _culturalSettings.asStateFlow()
    
    private val _localFeatures = MutableStateFlow(LocalFeatures())
    val localFeatures: StateFlow<LocalFeatures> = _localFeatures.asStateFlow()
    
    init {
        loadSettings()
    }
    
    enum class ZambianLanguage(
        val displayName: String,
        val localName: String,
        val code: String
    ) {
        ENGLISH("English", "English", "en"),
        BEMBA("Bemba", "Icibemba", "bem"),
        NYANJA("Nyanja/Chewa", "Chinyanja", "ny"),
        TONGA("Tonga", "Chitonga", "to"),
        LOZI("Lozi", "Silozi", "loz"),
        KAONDE("Kaonde", "Kikaonde", "kqn"),
        LUNDA("Lunda", "Chilunda", "lun"),
        LUVALE("Luvale", "Chiluvale", "lue")
    }
    
    enum class ZambianRegion(
        val displayName: String,
        val province: String,
        val majorLanguages: List<ZambianLanguage>
    ) {
        LUSAKA("Lusaka", "Lusaka Province", listOf(ZambianLanguage.ENGLISH, ZambianLanguage.NYANJA, ZambianLanguage.BEMBA)),
        NDOLA("Ndola", "Copperbelt Province", listOf(ZambianLanguage.BEMBA, ZambianLanguage.ENGLISH)),
        KITWE("Kitwe", "Copperbelt Province", listOf(ZambianLanguage.BEMBA, ZambianLanguage.ENGLISH)),
        LIVINGSTONE("Livingstone", "Southern Province", listOf(ZambianLanguage.TONGA, ZambianLanguage.ENGLISH)),
        CHIPATA("Chipata", "Eastern Province", listOf(ZambianLanguage.NYANJA, ZambianLanguage.ENGLISH)),
        KASAMA("Kasama", "Northern Province", listOf(ZambianLanguage.BEMBA, ZambianLanguage.ENGLISH)),
        SOLWEZI("Solwezi", "North-Western Province", listOf(ZambianLanguage.KAONDE, ZambianLanguage.LUNDA, ZambianLanguage.ENGLISH)),
        MONGU("Mongu", "Western Province", listOf(ZambianLanguage.LOZI, ZambianLanguage.ENGLISH)),
        KABWE("Kabwe", "Central Province", listOf(ZambianLanguage.ENGLISH, ZambianLanguage.BEMBA)),
        MANSA("Mansa", "Luapula Province", listOf(ZambianLanguage.BEMBA, ZambianLanguage.ENGLISH))
    }
    
    data class CulturalSettings(
        val useTraditionalGreetings: Boolean = true,
        val showLocalPrayers: Boolean = true,
        val useLocalTimeFormat: Boolean = true,
        val showSeasonalGuidance: Boolean = true,
        val includeFamilyContext: Boolean = true,
        val respectElders: Boolean = true,
        val communityOriented: Boolean = true,
        val useLocalMeasurements: Boolean = true,
        val showLocalFestivals: Boolean = true,
        val useUbuntuPhilosophy: Boolean = true
    )
    
    data class LocalFeatures(
        val enableLocalCuisine: Boolean = true,
        val enableTraditionalCrafts: Boolean = true,
        val enableLocalFarming: Boolean = true,
        val enableMiningSupport: Boolean = true,
        val enableLocalBusinessSupport: Boolean = true,
        val enableEducationSupport: Boolean = true,
        val enableCommunityHelp: Boolean = true,
        
        // Young Adult Features
        val enableDatingAdvice: Boolean = false,
        val enableCareerGuidance: Boolean = true,
        val enableChristianTone: Boolean = false,
        val enableDailyVerses: Boolean = false,
        val enableDevotionals: Boolean = false,
        val enableInspirationalMessages: Boolean = true,
        val enableMotivationalMessages: Boolean = true,
        val enableSuccessStories: Boolean = true,
        val enableTikTokTrends: Boolean = false,
        val enableGossipAndHotTopics: Boolean = false,
        val enableSocialMediaTips: Boolean = true,
        val enableRelationshipAdvice: Boolean = false,
        val enableFinancialLiteracy: Boolean = true,
        val enableSkillDevelopment: Boolean = true
    )
    
    // Traditional Zambian greetings
    fun getTraditionalGreeting(): String {
        return when (_currentLanguage.value) {
            ZambianLanguage.BEMBA -> when (getCurrentTimeOfDay()) {
                TimeOfDay.MORNING -> "Mwapoleni bushe! (Good morning!)"
                TimeOfDay.AFTERNOON -> "Mwaswali bushe! (Good afternoon!)" 
                TimeOfDay.EVENING -> "Mukeleko bushe! (Good evening!)"
                TimeOfDay.NIGHT -> "Mupumule bwino! (Sleep well!)"
            }
            ZambianLanguage.NYANJA -> when (getCurrentTimeOfDay()) {
                TimeOfDay.MORNING -> "Mwadzuka bwanji! (How did you wake up!)"
                TimeOfDay.AFTERNOON -> "Muli bwanji masana! (How are you this afternoon!)"
                TimeOfDay.EVENING -> "Muli bwanji madzulo! (How are you this evening!)"
                TimeOfDay.NIGHT -> "Gonani bwino! (Sleep well!)"
            }
            ZambianLanguage.TONGA -> when (getCurrentTimeOfDay()) {
                TimeOfDay.MORNING -> "Mwaluka buti! (How did you wake up!)"
                TimeOfDay.AFTERNOON -> "Mwali buti! (How are you!)"
                TimeOfDay.EVENING -> "Mwali buti masiye! (How are you this evening!)"
                TimeOfDay.NIGHT -> "Mulale kabotu! (Sleep well!)"
            }
            ZambianLanguage.LOZI -> when (getCurrentTimeOfDay()) {
                TimeOfDay.MORNING -> "Mu sakuhali hande! (Good morning!)"
                TimeOfDay.AFTERNOON -> "Mu sakuzwile hande! (Good afternoon!)"
                TimeOfDay.EVENING -> "Mu sakuleta hande! (Good evening!)"
                TimeOfDay.NIGHT -> "Mu lalelle hantle! (Sleep well!)"
            }
            else -> when (getCurrentTimeOfDay()) {
                TimeOfDay.MORNING -> "Good morning!"
                TimeOfDay.AFTERNOON -> "Good afternoon!"
                TimeOfDay.EVENING -> "Good evening!"
                TimeOfDay.NIGHT -> "Good night!"
            }
        }
    }
    
    fun getLocalizedString(key: String): String {
        return when (_currentLanguage.value) {
            ZambianLanguage.BEMBA -> getBembaTranslation(key)
            ZambianLanguage.NYANJA -> getNyanjaTranslation(key)
            ZambianLanguage.TONGA -> getTongaTranslation(key)
            ZambianLanguage.LOZI -> getLoziTranslation(key)
            else -> getEnglishTranslation(key)
        }
    }
    
    fun getLocalizedAgentName(agentId: String): String {
        return when (_currentLanguage.value) {
            ZambianLanguage.BEMBA -> when (agentId) {
                "cooking" -> "Umushimbi wa Chakulya (Cooking Helper)"
                "crafting" -> "Umushimbi wa Ubumba (Crafting Helper)"
                "diy" -> "Umushimbi wa Kupanga (DIY Helper)"
                "buddy" -> "Mubwenza (Friend/Buddy)"
                else -> agentId
            }
            ZambianLanguage.NYANJA -> when (agentId) {
                "cooking" -> "Wothandizira Chakudya (Cooking Helper)"
                "crafting" -> "Wothandizira Kupanga (Crafting Helper)"
                "diy" -> "Wothandizira Kukonza (DIY Helper)"
                "buddy" -> "Bwenzi (Friend/Buddy)"
                else -> agentId
            }
            ZambianLanguage.TONGA -> when (agentId) {
                "cooking" -> "Muyandizi wa Chakulya (Cooking Helper)"
                "crafting" -> "Muyandizi wa Mabvuto (Crafting Helper)"
                "diy" -> "Muyandizi wa Kukonzya (DIY Helper)"
                "buddy" -> "Mukutu (Friend/Buddy)"
                else -> agentId
            }
            else -> when (agentId) {
                "cooking" -> "Cooking Assistant"
                "crafting" -> "Crafting Guru"
                "diy" -> "DIY Helper"
                "buddy" -> "Buddy"
                else -> agentId
            }
        }
    }
    
    fun getZambianColors(): ZambianColors {
        return ZambianColors(
            emeraldGreen = Color(0xFF228B22), // Zambian flag green
            copperOrange = Color(0xFFFF6600), // Copper mining orange
            eagleRed = Color(0xFFDC143C), // Zambian eagle red
            sunYellow = Color(0xFFFFD700), // African sun yellow
            riverBlue = Color(0xFF0066CC), // Zambezi river blue
            earthBrown = Color(0xFF8B4513), // African earth brown
            maizeGold = Color(0xFFDAA520), // Maize/corn gold
            crimson = Color(0xFFDC143C), // Traditional Zambian red
            forestGreen = Color(0xFF228B22), // Dense forest green
            skyBlue = Color(0xFF87CEEB) // Clear African sky blue
        )
    }
    
    fun getCulturallyAppropriateResponse(message: String, agentType: String): String {
        val culturalPrefix = if (_culturalSettings.value.useTraditionalGreetings) {
            when (_currentLanguage.value) {
                ZambianLanguage.BEMBA -> "Ee, nafwaya ukutusaidisha. "
                ZambianLanguage.NYANJA -> "Inde, ndikufuna kukuthandizani. "
                ZambianLanguage.TONGA -> "Eya, ndalayanda kukuyandiza. "
                else -> "Yes, I'm happy to help you. "
            }
        } else ""
        
        val communityContext = if (_culturalSettings.value.communityOriented) {
            "Let me consider what would be best for you and your family. "
        } else ""
        
        return culturalPrefix + communityContext
    }
    
    fun getLocalMeasurements(): LocalMeasurements {
        return LocalMeasurements(
            currency = "ZMW", // Zambian Kwacha
            temperatureUnit = "°C",
            distanceUnit = "km",
            weightUnit = "kg",
            liquidUnit = "litres",
            timeFormat = if (_culturalSettings.value.useLocalTimeFormat) "24h" else "12h"
        )
    }
    
    fun getSeasonalGuidance(): String? {
        if (!_culturalSettings.value.showSeasonalGuidance) return null
        
        return when (getCurrentMonth()) {
            in 5..8 -> "It's dry season now. Perfect time for construction projects and outdoor crafts!"
            in 11..3 -> "Rainy season - great for indoor activities and food preservation tips!"
            else -> "Perfect weather for outdoor activities!"
        }
    }
    
    fun saveSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesRepository.updateLanguage(_currentLanguage.value)
            preferencesRepository.updateRegion(_currentRegion.value)
            preferencesRepository.updateCulturalSettings(_culturalSettings.value)
            preferencesRepository.updateLocalFeatures(_localFeatures.value)
        }
    }
    
    private fun loadSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesRepository.zambianPreferences.collect { preferences ->
                val languageCode = preferences.preferredLanguage
                _currentLanguage.value = ZambianLanguage.entries.find { it.code == languageCode } ?: ZambianLanguage.ENGLISH
                
                val regionName = preferences.preferredRegion
                _currentRegion.value = ZambianRegion.entries.find { it.name == regionName } ?: ZambianRegion.LUSAKA
                
                // Update cultural settings
                _culturalSettings.value = CulturalSettings(
                    useTraditionalGreetings = preferences.useTraditionalGreetings,
                    showLocalPrayers = preferences.showLocalPrayers,
                    useLocalTimeFormat = preferences.useLocalTimeFormat,
                    showSeasonalGuidance = preferences.showSeasonalGuidance,
                    includeFamilyContext = preferences.includeFamilyContext,
                    respectElders = preferences.respectElders,
                    communityOriented = preferences.communityOriented,
                    useLocalMeasurements = preferences.useLocalMeasurements,
                    showLocalFestivals = preferences.showLocalFestivals,
                    useUbuntuPhilosophy = preferences.useUbuntuPhilosophy
                )
                
                // Update local features
                _localFeatures.value = LocalFeatures(
                    enableLocalCuisine = preferences.enableLocalCuisine,
                    enableTraditionalCrafts = preferences.enableTraditionalCrafts,
                    enableLocalFarming = preferences.enableLocalFarming,
                    enableMiningSupport = preferences.enableMiningSupport,
                    enableLocalBusinessSupport = preferences.enableLocalBusinessSupport,
                    enableEducationSupport = preferences.enableEducationSupport,
                    enableCommunityHelp = preferences.enableCommunityHelp,
                    
                    // Young Adult Features - Default values for now, will be loaded from preferences later
                    enableDatingAdvice = false,
                    enableCareerGuidance = true,
                    enableChristianTone = false,
                    enableDailyVerses = false,
                    enableDevotionals = false,
                    enableInspirationalMessages = true,
                    enableMotivationalMessages = true,
                    enableSuccessStories = true,
                    enableTikTokTrends = false,
                    enableGossipAndHotTopics = false,
                    enableSocialMediaTips = true,
                    enableRelationshipAdvice = false,
                    enableFinancialLiteracy = true,
                    enableSkillDevelopment = true
                )
            }
        }
    }
    
    private fun getCurrentTimeOfDay(): TimeOfDay {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.AFTERNOON
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
    
    private fun getCurrentMonth(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    }
    
    private enum class TimeOfDay { MORNING, AFTERNOON, EVENING, NIGHT }
    
    private fun getBembaTranslation(key: String): String {
        return when (key) {
            "welcome" -> "Mukeleni"
            "help" -> "Ubusaidi"
            "cooking" -> "Ukulya"
            "crafting" -> "Ubushimbi"
            "friend" -> "Ububwenza"
            "thank_you" -> "Twalumba sana"
            "good_luck" -> "Mwakabila"
            "family" -> "Umukowa"
            "community" -> "Ukutumine"
            "respect" -> "Ukutina"
            "wisdom" -> "Ubushiku"
            else -> key
        }
    }
    
    private fun getNyanjaTranslation(key: String): String {
        return when (key) {
            "welcome" -> "Takulandirani"
            "help" -> "Chithandizo"
            "cooking" -> "Kuphika"
            "crafting" -> "Kupanga"
            "friend" -> "Bwenzi"
            "thank_you" -> "Zikomo kwambiri"
            "good_luck" -> "Tsogoloni bwino"
            "family" -> "Banja"
            "community" -> "Anthu onse"
            "respect" -> "Ulemu"
            "wisdom" -> "Nzeru"
            else -> key
        }
    }
    
    private fun getTongaTranslation(key: String): String {
        return when (key) {
            "welcome" -> "Tukutamikile"
            "help" -> "Buyandazi"
            "cooking" -> "Kulya"
            "crafting" -> "Kusika"
            "friend" -> "Mukutu"
            "thank_you" -> "Twalumba lyoonse"
            "good_luck" -> "Mukakonzye bwino"
            "family" -> "Bana"
            "community" -> "Bantu bonse"
            "respect" -> "Bulumbu"
            "wisdom" -> "Buyanda"
            else -> key
        }
    }
    
    private fun getLoziTranslation(key: String): String {
        return when (key) {
            "welcome" -> "Mu tambuluwilile"
            "help" -> "Thuso"
            "cooking" -> "Ho apea"
            "crafting" -> "Ho etsa"
            "friend" -> "Mokhoi"
            "thank_you" -> "Kea leboha haholo"
            "good_luck" -> "Ho lokelang hantle"
            "family" -> "Lelapa"
            "community" -> "Sechaba"
            "respect" -> "Tlhompho"
            "wisdom" -> "Bohlale"
            else -> key
        }
    }
    
    private fun getEnglishTranslation(key: String): String {
        return when (key) {
            "welcome" -> "Welcome"
            "help" -> "Help"
            "cooking" -> "Cooking"
            "crafting" -> "Crafting"
            "friend" -> "Friend"
            "thank_you" -> "Thank you"
            "good_luck" -> "Good luck"
            "family" -> "Family"
            "community" -> "Community"
            "respect" -> "Respect"
            "wisdom" -> "Wisdom"
            else -> key
        }
    }
    
    fun setLanguage(language: ZambianLanguage) {
        _currentLanguage.value = language
        saveSettings()
    }
    
    fun setRegion(region: ZambianRegion) {
        _currentRegion.value = region
        saveSettings()
    }
    
    fun updateCulturalSettings(settings: CulturalSettings) {
        _culturalSettings.value = settings
        saveSettings()
    }
    
    fun updateLocalFeatures(features: LocalFeatures) {
        _localFeatures.value = features
        saveSettings()
    }
}

data class ZambianColors(
    val emeraldGreen: Color,
    val copperOrange: Color,
    val eagleRed: Color,
    val sunYellow: Color,
    val riverBlue: Color,
    val earthBrown: Color,
    val maizeGold: Color,
    val crimson: Color,
    val forestGreen: Color,
    val skyBlue: Color
)

data class LocalMeasurements(
    val currency: String,
    val temperatureUnit: String,
    val distanceUnit: String,
    val weightUnit: String,
    val liquidUnit: String,
    val timeFormat: String
)

data class CulturalSettings(
    val useTraditionalGreetings: Boolean = true,
    val includeFamilyContext: Boolean = true,
    val communityOriented: Boolean = true,
    val useUbuntuPhilosophy: Boolean = true,
    val useLocalTimeFormat: Boolean = true,
    val showSeasonalGuidance: Boolean = true,
    val useLocalMeasurements: Boolean = true
)

