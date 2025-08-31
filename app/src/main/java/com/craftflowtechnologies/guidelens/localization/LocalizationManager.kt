package com.craftflowtechnologies.guidelens.localization

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Comprehensive localization manager for GuideLens with deep cultural integration
 * Specializing in Southern African localization (Zambia, Zimbabwe)
 */
class LocalizationManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _currentLocale = MutableStateFlow(LocaleInfo.DEFAULT)
    val currentLocale: StateFlow<LocaleInfo> = _currentLocale.asStateFlow()
    
    private val _selectedTribalLanguage = MutableStateFlow<TribalLanguage?>(null)
    val selectedTribalLanguage: StateFlow<TribalLanguage?> = _selectedTribalLanguage.asStateFlow()
    
    // Localized content cache
    private val _localizedContent = MutableStateFlow<Map<String, LocalizedContent>>(emptyMap())
    
    init {
        initializeDefaultLocale()
        loadLocalizedContent()
    }

    private fun initializeDefaultLocale() {
        // Try to detect from system locale or default to Zambian English
        val systemLocale = Locale.getDefault()
        val detectedLocale = when (systemLocale.country.uppercase()) {
            "ZM" -> LOCALES.find { it.countryCode == "ZM" } ?: LOCALES.first()
            "ZW" -> LOCALES.find { it.countryCode == "ZW" } ?: LOCALES.first()
            else -> LOCALES.first() // Default to Zambian English
        }
        _currentLocale.value = detectedLocale
    }

    private fun loadLocalizedContent() {
        scope.launch {
            // Load all localized content for current locale
            val content = generateLocalizedContent(_currentLocale.value)
            _localizedContent.value = content
        }
    }

    /**
     * Set user's country and cultural preferences
     */
    suspend fun setLocale(locale: LocaleInfo, tribalLanguage: TribalLanguage? = null) {
        _currentLocale.value = locale
        _selectedTribalLanguage.value = tribalLanguage
        loadLocalizedContent()
    }

    /**
     * Generate localized AI prompt context
     */
    suspend fun generateLocalizedPromptContext(): String {
        val locale = _currentLocale.value
        val tribalLang = _selectedTribalLanguage.value
        
        return buildString {
            appendLine("LOCALIZATION CONTEXT:")
            appendLine("Country: ${locale.countryName} (${locale.countryCode})")
            appendLine("Cultural Region: ${locale.culturalRegion}")
            appendLine("Primary Language: ${locale.primaryLanguage}")
            appendLine("Currency: ${locale.currency}")
            appendLine("Time Zone: ${locale.timeZone}")
            
            if (tribalLang != null) {
                appendLine("Tribal Language: ${tribalLang.name} (${tribalLang.tribe})")
                appendLine("Language Family: ${tribalLang.languageFamily}")
                appendLine("Common Greetings: ${tribalLang.commonGreetings.joinToString(", ")}")
                appendLine("Cultural Notes: ${tribalLang.culturalNotes}")
            }
            
            appendLine()
            appendLine("LOCALIZATION INSTRUCTIONS:")
            appendLine("- Use ${locale.primaryLanguage} as the primary language")
            appendLine("- Include local cultural references and context")
            appendLine("- Reference local ingredients, tools, and materials available in ${locale.countryName}")
            appendLine("- Use appropriate currency (${locale.currency}) for pricing")
            appendLine("- Consider local climate: ${locale.climate}")
            appendLine("- Be aware of local customs: ${locale.culturalNotes}")
            
            if (tribalLang != null) {
                appendLine("- Occasionally use ${tribalLang.name} greetings and expressions")
                appendLine("- Incorporate cultural wisdom from ${tribalLang.tribe} traditions")
                appendLine("- ${tribalLang.usageInstructions}")
            }
            
            // Add specific content guidance
            appendLine()
            appendLine("CONTENT LOCALIZATION:")
            appendLine("- Recipes: Focus on ${locale.commonIngredients.joinToString(", ")}")
            appendLine("- DIY Projects: Use materials commonly available in ${locale.countryName}")
            appendLine("- Seasonal Awareness: ${locale.seasons}")
            appendLine("- Local Measurements: Use ${locale.measurementSystem}")
            
            if (locale.localChallenges.isNotEmpty()) {
                appendLine("- Address local challenges: ${locale.localChallenges.joinToString(", ")}")
            }
            
            appendLine()
        }
    }

    /**
     * Get localized recipes and ingredients
     */
    fun getLocalizedRecipes(): List<LocalizedRecipe> {
        return when (_currentLocale.value.countryCode) {
            "ZM" -> ZAMBIAN_RECIPES
            "ZW" -> ZIMBABWEAN_RECIPES
            else -> ZAMBIAN_RECIPES // Default
        }
    }

    /**
     * Get localized DIY materials and tools
     */
    fun getLocalizedDIYMaterials(): List<LocalMaterial> {
        return when (_currentLocale.value.countryCode) {
            "ZM" -> ZAMBIAN_DIY_MATERIALS
            "ZW" -> ZIMBABWEAN_DIY_MATERIALS
            else -> ZAMBIAN_DIY_MATERIALS
        }
    }

    /**
     * Get localized crafting supplies
     */
    fun getLocalizedCraftingSupplies(): List<CraftingSupply> {
        return when (_currentLocale.value.countryCode) {
            "ZM" -> ZAMBIAN_CRAFTING_SUPPLIES
            "ZW" -> ZIMBABWEAN_CRAFTING_SUPPLIES
            else -> ZAMBIAN_CRAFTING_SUPPLIES
        }
    }

    /**
     * Get cultural greetings and expressions
     */
    fun getCulturalExpressions(): List<CulturalExpression> {
        val expressions = mutableListOf<CulturalExpression>()
        
        // Add country-specific expressions
        expressions.addAll(
            when (_currentLocale.value.countryCode) {
                "ZM" -> ZAMBIAN_EXPRESSIONS
                "ZW" -> ZIMBABWEAN_EXPRESSIONS
                else -> ZAMBIAN_EXPRESSIONS
            }
        )
        
        // Add tribal language expressions if selected
        _selectedTribalLanguage.value?.let { tribalLang ->
            expressions.addAll(tribalLang.expressions)
        }
        
        return expressions
    }

    /**
     * Generate seasonal content awareness
     */
    fun getSeasonalContext(): String {
        val locale = _currentLocale.value
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        
        return when (currentMonth) {
            12, 1, 2 -> "${locale.seasons} - Hot season: Focus on cooling recipes and indoor activities"
            3, 4, 5 -> "${locale.seasons} - Cool/Dry season: Perfect for outdoor projects and hearty meals" 
            6, 7, 8 -> "${locale.seasons} - Cold season: Warm comfort foods and indoor crafts"
            9, 10, 11 -> "${locale.seasons} - Hot season returning: Light meals and ventilation projects"
            else -> locale.seasons
        }
    }

    /**
     * Localize pricing and measurements
     */
    fun localizePrice(amount: Double): String {
        val locale = _currentLocale.value
        return when (locale.currency) {
            "ZMW" -> "K${String.format("%.2f", amount)}"
            "USD" -> "$${String.format("%.2f", amount)}"
            else -> "${locale.currency} ${String.format("%.2f", amount)}"
        }
    }

    /**
     * Convert measurements to local system
     */
    fun localizeMeasurement(value: Double, unit: String): String {
        val locale = _currentLocale.value
        return when (locale.measurementSystem) {
            "Metric" -> "${value}${unit}"
            "Imperial" -> convertToImperial(value, unit)
            "Mixed" -> "${value}${unit} (${convertToImperial(value, unit)})"
            else -> "${value}${unit}"
        }
    }

    private fun convertToImperial(value: Double, unit: String): String {
        return when (unit.lowercase()) {
            "ml" -> "${(value * 0.033814).toInt()}fl oz"
            "l" -> "${(value * 0.264172).toInt()}gal"
            "g" -> "${(value * 0.035274).toInt()}oz"
            "kg" -> "${(value * 2.20462).toInt()}lbs"
            "cm" -> "${(value * 0.393701).toInt()}in"
            "m" -> "${(value * 3.28084).toInt()}ft"
            else -> "${value}${unit}"
        }
    }

    private suspend fun generateLocalizedContent(locale: LocaleInfo): Map<String, LocalizedContent> {
        // This would typically load from a database or API
        return mapOf(
            "cooking" to LocalizedContent(
                greetings = listOf("Welcome to the kitchen!", "Let's cook something delicious!"),
                commonPhrases = listOf("Taste and adjust", "Cook until tender"),
                culturalNotes = "In ${locale.countryName}, we love to share meals with family and friends."
            ),
            "crafting" to LocalizedContent(
                greetings = listOf("Let's create something beautiful!", "Time to get creative!"),
                commonPhrases = listOf("Measure twice, cut once", "Take your time"),
                culturalNotes = "Traditional ${locale.countryName} crafts often use natural materials."
            )
        )
    }

    fun cleanup() {
        scope.cancel()
    }

    companion object {
        val LOCALES = listOf(
            LocaleInfo(
                countryCode = "ZM",
                countryName = "Zambia",
                culturalRegion = "Southern Africa",
                primaryLanguage = "English",
                currency = "ZMW",
                timeZone = "Africa/Lusaka",
                climate = "Tropical with three seasons: hot (September-November), rainy (December-April), cool/dry (May-August)",
                measurementSystem = "Metric",
                commonIngredients = listOf("maize meal", "cassava", "sweet potatoes", "groundnuts", "beans", "pumpkin leaves", "tomatoes", "onions", "fish", "chicken"),
                seasons = "Hot season (Sep-Nov), Rainy season (Dec-Apr), Cool/Dry season (May-Aug)",
                culturalNotes = "Ubuntu philosophy - 'I am because we are'. Community-focused, respect for elders, sharing meals is important",
                localChallenges = listOf("Power outages (load shedding)", "Water shortages", "Limited imported goods", "Rural vs urban resource availability"),
                tribalLanguages = listOf("Bemba", "Nyanja", "Tonga", "Lozi", "Kaonde", "Lunda", "Luvale")
            ),
            LocaleInfo(
                countryCode = "ZW", 
                countryName = "Zimbabwe",
                culturalRegion = "Southern Africa",
                primaryLanguage = "English",
                currency = "USD",
                timeZone = "Africa/Harare",
                climate = "Subtropical highland with wet season (November-March) and dry season (April-October)",
                measurementSystem = "Metric",
                commonIngredients = listOf("sadza meal", "rape (kale)", "beef", "chicken", "matemba", "okra", "butternut", "tomatoes", "onions", "cooking oil"),
                seasons = "Hot wet season (Nov-Mar), Cool dry season (Apr-Oct)",
                culturalNotes = "Hunhu/Ubuntu values, strong family ties, respect for ancestors and elders, communal living",
                localChallenges = listOf("Economic challenges", "Power cuts", "Water shortages", "Limited foreign currency", "Inflation"),
                tribalLanguages = listOf("Shona", "Ndebele", "Kalanga", "Tonga", "Shangaan", "Venda", "Nambia")
            )
        )

        val TRIBAL_LANGUAGES = mapOf(
            // Zambian Languages
            "Bemba" to TribalLanguage(
                name = "Bemba",
                tribe = "Bemba people",
                countryCode = "ZM",
                languageFamily = "Bantu",
                commonGreetings = listOf("Muli shani" to "How are you?", "Twafumya" to "Good morning", "Natotela" to "Thank you"),
                expressions = listOf(
                    CulturalExpression("Ukubomba kwa friends", "Helping friends", "Community support"),
                    CulturalExpression("Abantu ni bantu", "People are people", "Everyone deserves respect"),
                    CulturalExpression("Umwana ashenda atasha nyina ukwabula", "A child who travels learns more than their mother", "Travel and learning")
                ),
                culturalNotes = "Largest ethnic group in Zambia, known for hospitality and storytelling traditions",
                usageInstructions = "Use Bemba greetings warmly, incorporate proverbs about community and learning"
            ),
            "Nyanja" to TribalLanguage(
                name = "Nyanja/Chewa",
                tribe = "Chewa people", 
                countryCode = "ZM",
                languageFamily = "Bantu",
                commonGreetings = listOf("Muli bwanji" to "How are you?", "Zikomo" to "Thank you", "Tsiku labwino" to "Good day"),
                expressions = listOf(
                    CulturalExpression("Pamodzi ndi mphamvu", "Unity is strength", "Community power"),
                    CulturalExpression("Chala chamunthu ndi ufulu wake", "One's hand is their freedom", "Self-reliance"),
                    CulturalExpression("Mayeso ndi mphunziro", "Tests are lessons", "Learning from challenges")
                ),
                culturalNotes = "Widely spoken in Eastern Province, known for Gule Wamkulu traditional dance",
                usageInstructions = "Use when discussing community projects or learning experiences"
            ),
            "Tonga" to TribalLanguage(
                name = "Tonga",
                tribe = "Tonga people",
                countryCode = "ZM", 
                languageFamily = "Bantu",
                commonGreetings = listOf("Muli indi" to "How are you?", "Twalumba" to "Thank you", "Syalana bubi" to "Good evening"),
                expressions = listOf(
                    CulturalExpression("Bantu babotu bakakamane", "Good people help each other", "Mutual aid"),
                    CulturalExpression("Cino ncamuwana ciyanda", "What you see is what you want", "Contentment"),
                    CulturalExpression("Mulimu wakusyangana utegwa", "Work done together succeeds", "Teamwork")
                ),
                culturalNotes = "From Southern Province, known for cattle keeping and traditional crafts",
                usageInstructions = "Perfect for DIY and farming-related discussions"
            ),
            
            // Zimbabwean Languages
            "Shona" to TribalLanguage(
                name = "Shona",
                tribe = "Shona people",
                countryCode = "ZW",
                languageFamily = "Bantu", 
                commonGreetings = listOf("Makadii" to "How are you?", "Tatenda" to "Thank you", "Mangwanani" to "Good morning"),
                expressions = listOf(
                    CulturalExpression("Rume rimwe harikombi inda", "One finger cannot crush lice", "Unity needed"),
                    CulturalExpression("Chakafukidza dzimba matenga", "What covers houses is the roof", "Preparation is key"),
                    CulturalExpression("Kukura kwemukaka hakupedzi umama", "Growing up doesn't end motherhood", "Eternal care")
                ),
                culturalNotes = "Largest ethnic group in Zimbabwe, rich in proverbs and traditional knowledge",
                usageInstructions = "Use Shona proverbs when giving advice or explaining concepts"
            ),
            "Ndebele" to TribalLanguage(
                name = "Ndebele",
                tribe = "Ndebele people",
                countryCode = "ZW",
                languageFamily = "Bantu",
                commonGreetings = listOf("Unjani" to "How are you?", "Ngiyabonga" to "Thank you", "Sawubona" to "Hello"),
                expressions = listOf(
                    CulturalExpression("Ubuntu ngumuntu ngabanye abantu", "A person is a person through other people", "Interconnectedness"),
                    CulturalExpression("Isihlangu samaqhawe", "The shield of heroes", "Courage and protection"),
                    CulturalExpression("Ukuphila kuyimfundiso", "Living is learning", "Life lessons")
                ),
                culturalNotes = "Known for beautiful beadwork, traditional architecture, and warrior heritage",
                usageInstructions = "Use when discussing crafts, building, or courage in facing challenges"
            )
        )
    }
}

// Data Models
@Serializable
data class LocaleInfo(
    val countryCode: String,
    val countryName: String,
    val culturalRegion: String,
    val primaryLanguage: String,
    val currency: String,
    val timeZone: String,
    val climate: String,
    val measurementSystem: String,
    val commonIngredients: List<String>,
    val seasons: String,
    val culturalNotes: String,
    val localChallenges: List<String>,
    val tribalLanguages: List<String>
) {
    companion object {
        val DEFAULT = LocaleInfo(
            countryCode = "ZM",
            countryName = "Zambia", 
            culturalRegion = "Southern Africa",
            primaryLanguage = "English",
            currency = "ZMW",
            timeZone = "Africa/Lusaka",
            climate = "Tropical",
            measurementSystem = "Metric",
            commonIngredients = listOf("maize meal", "vegetables", "meat"),
            seasons = "Tropical seasons",
            culturalNotes = "Ubuntu philosophy",
            localChallenges = emptyList(),
            tribalLanguages = emptyList()
        )
    }
}

@Serializable
data class TribalLanguage(
    val name: String,
    val tribe: String,
    val countryCode: String,
    val languageFamily: String,
    val commonGreetings: List<Pair<String, String>>, // (phrase, translation)
    val expressions: List<CulturalExpression>,
    val culturalNotes: String,
    val usageInstructions: String
)

@Serializable
data class CulturalExpression(
    val phrase: String,
    val meaning: String,
    val context: String
)

@Serializable
data class LocalizedContent(
    val greetings: List<String>,
    val commonPhrases: List<String>,
    val culturalNotes: String
)

@Serializable
data class LocalizedRecipe(
    val name: String,
    val localName: String?,
    val ingredients: List<String>,
    val culturalSignificance: String,
    val difficulty: String,
    val cookingMethod: String
)

@Serializable
data class LocalMaterial(
    val name: String,
    val localName: String?,
    val availability: String,
    val approximatePrice: Double,
    val whereToFind: String,
    val alternatives: List<String>
)

@Serializable
data class CraftingSupply(
    val name: String,
    val localName: String?,
    val traditionalUse: String,
    val availability: String,
    val approximatePrice: Double,
    val culturalSignificance: String?
)

// Localized recipe data
val ZAMBIAN_RECIPES = listOf(
    LocalizedRecipe(
        name = "Nshima with Chicken",
        localName = "Nshima na Inkoko",
        ingredients = listOf("maize meal", "chicken", "tomatoes", "onions", "cooking oil", "salt"),
        culturalSignificance = "Staple food of Zambia, brings families together",
        difficulty = "Easy",
        cookingMethod = "Traditional pot cooking"
    ),
    LocalizedRecipe(
        name = "Kapenta with Vegetables", 
        localName = "Kapenta na Ifisabi",
        ingredients = listOf("kapenta fish", "rape/kale", "tomatoes", "onions", "groundnut powder"),
        culturalSignificance = "Popular protein source from Lake Kariba",
        difficulty = "Medium", 
        cookingMethod = "Pan frying then stewing"
    )
)

val ZIMBABWEAN_RECIPES = listOf(
    LocalizedRecipe(
        name = "Sadza with Meat",
        localName = "Sadza nenyama",
        ingredients = listOf("mealie meal", "beef", "tomatoes", "onions", "cooking oil"),
        culturalSignificance = "National dish, eaten with hands in traditional style",
        difficulty = "Easy",
        cookingMethod = "Pot cooking with wooden spoon stirring"
    ),
    LocalizedRecipe(
        name = "Madora with Sadza",
        localName = "Madora (Mopane worms)",
        ingredients = listOf("dried mopane worms", "tomatoes", "onions", "oil", "mealie meal"),
        culturalSignificance = "Traditional protein, seasonal delicacy",
        difficulty = "Medium",
        cookingMethod = "Rehydrate then fry with vegetables"
    )
)

val ZAMBIAN_DIY_MATERIALS = listOf(
    LocalMaterial(
        name = "Bricks",
        localName = "Matofali", 
        availability = "Local brick makers",
        approximatePrice = 0.50,
        whereToFind = "Building supply stores, local manufacturers",
        alternatives = listOf("Clay bricks", "Cement blocks", "Adobe bricks")
    ),
    LocalMaterial(
        name = "Thatch Grass",
        localName = "Udaka/Matete",
        availability = "Rural areas, seasonal",
        approximatePrice = 25.0,
        whereToFind = "Rural markets, direct from farmers",
        alternatives = listOf("Iron sheets", "Tiles", "Plastic sheeting")
    )
)

val ZIMBABWEAN_DIY_MATERIALS = listOf(
    LocalMaterial(
        name = "Pole Timber", 
        localName = "Matanda",
        availability = "Forestry areas",
        approximatePrice = 15.0,
        whereToFind = "Timber yards, rural areas",
        alternatives = listOf("Steel poles", "Concrete posts", "Brick pillars")
    )
)

val ZAMBIAN_CRAFTING_SUPPLIES = listOf(
    CraftingSupply(
        name = "Grass Weaving Material",
        localName = "Ubushiku",
        traditionalUse = "Basket making, mat weaving",
        availability = "Rural areas, markets",
        approximatePrice = 5.0,
        culturalSignificance = "Traditional women's craft, passed down generations"
    )
)

val ZIMBABWEAN_CRAFTING_SUPPLIES = listOf(
    CraftingSupply(
        name = "Clay",
        localName = "Dhaka",
        traditionalUse = "Pottery, traditional pots",
        availability = "River banks, specialized suppliers", 
        approximatePrice = 2.0,
        culturalSignificance = "Ancient pottery traditions, ceremonial and daily use"
    )
)

val ZAMBIAN_EXPRESSIONS = listOf(
    CulturalExpression("Pali ubulanda", "There's a relationship", "Everything is connected"),
    CulturalExpression("Kumakasa", "At the corner", "Meeting point for community discussions")
)

val ZIMBABWEAN_EXPRESSIONS = listOf(
    CulturalExpression("Dare guru", "The big meeting", "Community decision making"),
    CulturalExpression("Kusina chakafukidza", "What has no cover", "Something obvious or exposed")
)