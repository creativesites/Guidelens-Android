package com.craftflowtechnologies.guidelens.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GeneralCountry(
    val code: String,
    val name: String,
    val flag: String,
    val language: Language,
    val currency: String = "USD",
    val measurementSystem: MeasurementSystem = MeasurementSystem.METRIC
)

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

enum class MeasurementSystem(val displayName: String) {
    METRIC("Metric (kg, °C, ml)"),
    IMPERIAL("Imperial (lbs, °F, cups)")
}

data class GeneralLocalizationPreferences(
    val selectedCountry: GeneralCountry = GeneralCountries.UNITED_STATES,
    val selectedLanguage: Language = GeneralLanguages.ENGLISH,
    val measurementSystem: MeasurementSystem = MeasurementSystem.METRIC,
    val dateFormat: String = "MM/dd/yyyy",
    val timeFormat: String = "12h"
)

object GeneralLanguages {
    val ENGLISH = Language("en", "English", "English")
    val SPANISH = Language("es", "Spanish", "Español")
    val FRENCH = Language("fr", "French", "Français")
    val GERMAN = Language("de", "German", "Deutsch")
    val ITALIAN = Language("it", "Italian", "Italiano")
    val PORTUGUESE = Language("pt", "Portuguese", "Português")
    val JAPANESE = Language("ja", "Japanese", "日本語")
    val KOREAN = Language("ko", "Korean", "한국어")
    val CHINESE = Language("zh", "Chinese", "中文")
    val ARABIC = Language("ar", "Arabic", "العربية")
    val HINDI = Language("hi", "Hindi", "हिन्दी")
    val RUSSIAN = Language("ru", "Russian", "Русский")
    
    val ALL = listOf(
        ENGLISH, SPANISH, FRENCH, GERMAN, ITALIAN, PORTUGUESE,
        JAPANESE, KOREAN, CHINESE, ARABIC, HINDI, RUSSIAN
    )
}

object GeneralCountries {
    // Popular countries
    val UNITED_STATES = GeneralCountry("US", "United States", "🇺🇸", GeneralLanguages.ENGLISH, "USD", MeasurementSystem.IMPERIAL)
    val CANADA = GeneralCountry("CA", "Canada", "🇨🇦", GeneralLanguages.ENGLISH, "CAD", MeasurementSystem.METRIC)
    val UNITED_KINGDOM = GeneralCountry("GB", "United Kingdom", "🇬🇧", GeneralLanguages.ENGLISH, "GBP", MeasurementSystem.IMPERIAL)
    val AUSTRALIA = GeneralCountry("AU", "Australia", "🇦🇺", GeneralLanguages.ENGLISH, "AUD", MeasurementSystem.METRIC)
    val GERMANY = GeneralCountry("DE", "Germany", "🇩🇪", GeneralLanguages.GERMAN, "EUR", MeasurementSystem.METRIC)
    val FRANCE = GeneralCountry("FR", "France", "🇫🇷", GeneralLanguages.FRENCH, "EUR", MeasurementSystem.METRIC)
    val SPAIN = GeneralCountry("ES", "Spain", "🇪🇸", GeneralLanguages.SPANISH, "EUR", MeasurementSystem.METRIC)
    val ITALY = GeneralCountry("IT", "Italy", "🇮🇹", GeneralLanguages.ITALIAN, "EUR", MeasurementSystem.METRIC)
    val JAPAN = GeneralCountry("JP", "Japan", "🇯🇵", GeneralLanguages.JAPANESE, "JPY", MeasurementSystem.METRIC)
    val SOUTH_KOREA = GeneralCountry("KR", "South Korea", "🇰🇷", GeneralLanguages.KOREAN, "KRW", MeasurementSystem.METRIC)
    val CHINA = GeneralCountry("CN", "China", "🇨🇳", GeneralLanguages.CHINESE, "CNY", MeasurementSystem.METRIC)
    val BRAZIL = GeneralCountry("BR", "Brazil", "🇧🇷", GeneralLanguages.PORTUGUESE, "BRL", MeasurementSystem.METRIC)
    val MEXICO = GeneralCountry("MX", "Mexico", "🇲🇽", GeneralLanguages.SPANISH, "MXN", MeasurementSystem.METRIC)
    val INDIA = GeneralCountry("IN", "India", "🇮🇳", GeneralLanguages.HINDI, "INR", MeasurementSystem.METRIC)
    val RUSSIA = GeneralCountry("RU", "Russia", "🇷🇺", GeneralLanguages.RUSSIAN, "RUB", MeasurementSystem.METRIC)
    val NETHERLANDS = GeneralCountry("NL", "Netherlands", "🇳🇱", GeneralLanguages.ENGLISH, "EUR", MeasurementSystem.METRIC)
    val SWEDEN = GeneralCountry("SE", "Sweden", "🇸🇪", GeneralLanguages.ENGLISH, "SEK", MeasurementSystem.METRIC)
    val NORWAY = GeneralCountry("NO", "Norway", "🇳🇴", GeneralLanguages.ENGLISH, "NOK", MeasurementSystem.METRIC)
    val SWITZERLAND = GeneralCountry("CH", "Switzerland", "🇨🇭", GeneralLanguages.GERMAN, "CHF", MeasurementSystem.METRIC)
    val SOUTH_AFRICA = GeneralCountry("ZA", "South Africa", "🇿🇦", GeneralLanguages.ENGLISH, "ZAR", MeasurementSystem.METRIC)
    
    // African countries (to complement existing LocalizationManager)
    val ZAMBIA = GeneralCountry("ZM", "Zambia", "🇿🇲", GeneralLanguages.ENGLISH, "ZMW", MeasurementSystem.METRIC)
    val ZIMBABWE = GeneralCountry("ZW", "Zimbabwe", "🇿🇼", GeneralLanguages.ENGLISH, "USD", MeasurementSystem.METRIC)
    val NIGERIA = GeneralCountry("NG", "Nigeria", "🇳🇬", GeneralLanguages.ENGLISH, "NGN", MeasurementSystem.METRIC)
    val KENYA = GeneralCountry("KE", "Kenya", "🇰🇪", GeneralLanguages.ENGLISH, "KES", MeasurementSystem.METRIC)
    val GHANA = GeneralCountry("GH", "Ghana", "🇬🇭", GeneralLanguages.ENGLISH, "GHS", MeasurementSystem.METRIC)
    val EGYPT = GeneralCountry("EG", "Egypt", "🇪🇬", GeneralLanguages.ARABIC, "EGP", MeasurementSystem.METRIC)
    
    val ALL = listOf(
        UNITED_STATES, CANADA, UNITED_KINGDOM, AUSTRALIA, GERMANY, FRANCE,
        SPAIN, ITALY, JAPAN, SOUTH_KOREA, CHINA, BRAZIL, MEXICO, INDIA,
        RUSSIA, NETHERLANDS, SWEDEN, NORWAY, SWITZERLAND, SOUTH_AFRICA,
        ZAMBIA, ZIMBABWE, NIGERIA, KENYA, GHANA, EGYPT
    ).sortedBy { it.name }
    
    val POPULAR = listOf(
        UNITED_STATES, UNITED_KINGDOM, CANADA, AUSTRALIA, GERMANY, FRANCE, JAPAN, CHINA, INDIA, BRAZIL
    )
    
    val AFRICAN = listOf(
        SOUTH_AFRICA, ZAMBIA, ZIMBABWE, NIGERIA, KENYA, GHANA, EGYPT
    )
}

class GeneralLocalizationManager {
    private val _preferences = MutableStateFlow(GeneralLocalizationPreferences())
    val preferences: StateFlow<GeneralLocalizationPreferences> = _preferences.asStateFlow()
    
    fun setCountry(country: GeneralCountry) {
        _preferences.value = _preferences.value.copy(
            selectedCountry = country,
            selectedLanguage = country.language,
            measurementSystem = country.measurementSystem
        )
    }
    
    fun setLanguage(language: Language) {
        _preferences.value = _preferences.value.copy(selectedLanguage = language)
    }
    
    fun setMeasurementSystem(system: MeasurementSystem) {
        _preferences.value = _preferences.value.copy(measurementSystem = system)
    }
    
    fun getCurrentCountry(): GeneralCountry = _preferences.value.selectedCountry
    fun getCurrentLanguage(): Language = _preferences.value.selectedLanguage
    fun getCurrentMeasurementSystem(): MeasurementSystem = _preferences.value.measurementSystem
    
    fun getSystemPromptModifier(): String {
        val country = getCurrentCountry()
        val language = getCurrentLanguage()
        val measurement = getCurrentMeasurementSystem()
        
        return "Respond in ${language.name} (${language.nativeName}). " +
                "Use ${measurement.displayName} measurements. " +
                "Consider cultural context for ${country.name}. " +
                "Currency references should use ${country.currency}."
    }
    
    // Helper functions for measurement conversions
    fun formatTemperature(celsius: Double): String {
        return when (_preferences.value.measurementSystem) {
            MeasurementSystem.METRIC -> "${celsius.toInt()}°C"
            MeasurementSystem.IMPERIAL -> "${(celsius * 9/5 + 32).toInt()}°F"
        }
    }
    
    fun formatWeight(grams: Double): String {
        return when (_preferences.value.measurementSystem) {
            MeasurementSystem.METRIC -> "${grams.toInt()}g"
            MeasurementSystem.IMPERIAL -> "${(grams * 0.035274).toInt()}oz"
        }
    }
    
    fun formatVolume(milliliters: Double): String {
        return when (_preferences.value.measurementSystem) {
            MeasurementSystem.METRIC -> "${milliliters.toInt()}ml"
            MeasurementSystem.IMPERIAL -> "${(milliliters * 0.004227).toInt()} cups"
        }
    }
}

@Composable
fun rememberGeneralLocalizationManager(): GeneralLocalizationManager {
    return remember { GeneralLocalizationManager() }
}