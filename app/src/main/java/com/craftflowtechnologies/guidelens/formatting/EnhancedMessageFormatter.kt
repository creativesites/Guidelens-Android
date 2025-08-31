package com.craftflowtechnologies.guidelens.formatting

import androidx.compose.ui.graphics.Color
import com.craftflowtechnologies.guidelens.charts.*
import com.craftflowtechnologies.guidelens.localization.LocalizationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.regex.Pattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
/**
 * Enhanced message formatter that converts AI responses into rich, interactive content
 * with charts, localized formatting, and structured data visualization
 */
class EnhancedMessageFormatter(
    private val localizationManager: LocalizationManager
) {
    
    /**
     * Parse and format AI response into enhanced message content
     */
    suspend fun formatMessage(
        rawMessage: String,
        agentType: String
    ): FormattedMessage = withContext(Dispatchers.Default) {
        
        val sections = parseMessageSections(rawMessage)
        val formattedElements = mutableListOf<MessageElement>()
        
        sections.forEach { section ->
            when {
                section.isChartData() -> {
                    val chartElement = parseChartData(section, agentType)
                    chartElement?.let { formattedElements.add(it) }
                }
                section.isRecipeData() -> {
                    val recipeElement = parseRecipeData(section)
                    formattedElements.add(recipeElement)
                }
                section.isInstructionList() -> {
                    val instructionElement = parseInstructions(section)
                    formattedElements.add(instructionElement)
                }
                section.isCostBreakdown() -> {
                    val costElement = parseCostData(section)
                    costElement?.let { formattedElements.add(it) }
                }
                section.isProgressData() -> {
                    val progressElement = parseProgressData(section)
                    progressElement?.let { formattedElements.add(it) }
                }
                section.isLocalizedContent() -> {
                    val localizedElement = parseLocalizedContent(section)
                    formattedElements.add(localizedElement)
                }
                else -> {
                    // Regular text content
                    formattedElements.add(
                        MessageElement.Text(
                            content = section,
                            style = TextStyle.BODY
                        )
                    )
                }
            }
        }
        
        FormattedMessage(
            elements = formattedElements,
            hasInteractiveContent = formattedElements.any { it.isInteractive() },
            metadata = MessageMetadata(
                locale = localizationManager.currentLocale.value,
                tribalLanguage = localizationManager.selectedTribalLanguage.value?.name,
                agentType = agentType
            )
        )
    }
    
    /**
     * Parse message into logical sections for processing
     */
    private fun parseMessageSections(message: String): List<String> {
        // Split by double newlines or specific markers
        val sections = message.split(Regex("\\n\\s*\\n|\\[CHART\\]|\\[RECIPE\\]|\\[INSTRUCTIONS\\]|\\[COST\\]|\\[PROGRESS\\]|\\[LOCAL\\]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        return sections
    }
    
    /**
     * Parse chart data from message section
     */
    private fun parseChartData(section: String, agentType: String): MessageElement? {
        return when {
            section.contains("nutrition", ignoreCase = true) -> {
                parseNutritionChart(section)
            }
            section.contains("mood", ignoreCase = true) -> {
                parseMoodChart(section)
            }
            section.contains("progress", ignoreCase = true) -> {
                parseProgressChart(section)
            }
            section.contains("cost", ignoreCase = true) -> {
                parseCostChart(section)
            }
            else -> null
        }
    }

    /**
     * Parse nutrition chart data
     */
    private fun parseNutritionChart(section: String): MessageElement.Chart? {
        val numberPattern = Pattern.compile("(\\d+(?:\\.\\d+)?|\\d+)", Pattern.CASE_INSENSITIVE)
        val caloriesPattern = Pattern.compile("calories?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val proteinPattern = Pattern.compile("protein?s?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val carbsPattern = Pattern.compile("carb(?:ohydrate)?s?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val fatsPattern = Pattern.compile("fats?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val fiberPattern = Pattern.compile("fiber?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val sugarPattern = Pattern.compile("sugar?s?:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)
        val sodiumPattern = Pattern.compile("sodium:\\s*${numberPattern.pattern()}", Pattern.CASE_INSENSITIVE)

        fun extractNumber(matcher: java.util.regex.Matcher): Float {
            return if (matcher.find()) {
                matcher.group(1).toFloatOrNull() ?: 0f
            } else 0f
        }

        val calories = extractNumber(caloriesPattern.matcher(section))
        val proteins = extractNumber(proteinPattern.matcher(section))
        val carbs = extractNumber(carbsPattern.matcher(section))
        val fats = extractNumber(fatsPattern.matcher(section))
        val fiber = extractNumber(fiberPattern.matcher(section))
        val sugar = extractNumber(sugarPattern.matcher(section))
        val sodium = extractNumber(sodiumPattern.matcher(section))

        if (calories > 0 || proteins > 0 || carbs > 0 || fats > 0) {
            return MessageElement.Chart(
                chartType = ChartType.NUTRITION,
                data = NutritionData(
                    calories = calories,
                    proteins = proteins,
                    carbohydrates = carbs,
                    fats = fats,
                    fiber = fiber,
                    sugar = sugar,
                    sodium = sodium
                ),
                title = "Nutritional Information",
                chartJsConfig = generateNutritionChartJsConfig(calories, proteins, carbs, fats, fiber, sugar, sodium)
            )
        }

        return null
    }

    /**
     * Parse mood chart data
     */
    private fun parseMoodChart(section: String): MessageElement.Chart? {
        val moodPattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2})\\s*:\\s*(\\d+(?:\\.\\d+)?|\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = moodPattern.matcher(section)
        val moodPoints = mutableListOf<MoodDataPoint>()

        while (matcher.find()) {
            val date = matcher.group(1)
            val value = matcher.group(2).toFloatOrNull() ?: 3f
            moodPoints.add(MoodDataPoint(date, value))
        }

        if (moodPoints.isNotEmpty()) {
            return MessageElement.Chart(
                chartType = ChartType.MOOD_TRACKING,
                data = MoodData(moodPoints),
                title = "Mood Tracking",
                chartJsConfig = generateMoodChartJsConfig(moodPoints)
            )
        }

        return null
    }

    /**
     * Parse progress chart data
     */
    private fun parseProgressChart(section: String): MessageElement.Chart? {
        val progressPattern = Pattern.compile("([\\w\\s]+):\\s*(\\d+(?:\\.\\d+)?|\\d+)\\s*/\\s*(\\d+(?:\\.\\d+)?|\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = progressPattern.matcher(section)
        val categories = mutableListOf<ProgressCategory>()
        val colors = listOf(
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
            Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFF00BCD4)
        )

        var colorIndex = 0
        while (matcher.find() && colorIndex < colors.size) {
            val name = matcher.group(1).trim()
            val progress = matcher.group(2).toFloatOrNull() ?: 0f
            val maxProgress = matcher.group(3).toFloatOrNull() ?: 100f

            categories.add(
                ProgressCategory(
                    name = name,
                    progress = progress,
                    maxProgress = maxProgress,
                    color = colors[colorIndex++]
                )
            )
        }

        if (categories.isNotEmpty()) {
            return MessageElement.Chart(
                chartType = ChartType.PROGRESS,
                data = ProgressData("Progress Overview", categories),
                title = "Progress Overview",
                chartJsConfig = generateProgressChartJsConfig(categories)
            )
        }

        return null
    }

    /**
     * Parse cost chart data
     */
    private fun parseCostChart(section: String): MessageElement.Chart? {
        val locale = localizationManager.currentLocale.value
        val currency = when (locale.currency) {
            "ZMW" -> "K"
            "USD" -> "$"
            else -> locale.currency
        }

        val costPattern = Pattern.compile("([\\w\\s]+):\\s*[K$]?(\\d+(?:\\.\\d+)?|\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = costPattern.matcher(section)
        val costItems = mutableListOf<CostItem>()
        val colors = listOf(
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
            Color(0xFF9C27B0), Color(0xFFE91E63)
        )

        var colorIndex = 0
        var total = 0f

        while (matcher.find() && colorIndex < colors.size) {
            val name = matcher.group(1).trim()
            val amount = matcher.group(2).toFloatOrNull() ?: 0f

            if (name.lowercase() != "total") {
                costItems.add(
                    CostItem(
                        name = name,
                        amount = amount,
                        color = colors[colorIndex++]
                    )
                )
                total += amount
            }
        }

        if (costItems.isNotEmpty()) {
            return MessageElement.Chart(
                chartType = ChartType.COST_BREAKDOWN,
                data = CostBreakdownData(
                    total = total,
                    currency = currency,
                    items = costItems
                ),
                title = "Cost Breakdown",
                chartJsConfig = generateCostChartJsConfig(costItems, currency)
            )
        }

        return null
    }

    /**
     * Generate Chart.js config for nutrition chart
     */
    private fun generateNutritionChartJsConfig(
        calories: Float, proteins: Float, carbs: Float, fats: Float,
        fiber: Float, sugar: Float, sodium: Float
    ): String {
        val config = mapOf(
            "type" to "bar",
            "data" to mapOf(
                "labels" to listOf("Calories", "Proteins", "Carbs", "Fats", "Fiber", "Sugar", "Sodium"),
                "datasets" to listOf(
                    mapOf(
                        "label" to "Nutrition (g)",
                        "data" to listOf(calories, proteins, carbs, fats, fiber, sugar, sodium),
                        "backgroundColor" to listOf(
                            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#E91E63", "#00BCD4", "#FFC107"
                        ),
                        "borderColor" to listOf(
                            "#388E3C", "#1976D2", "#F57C00", "#7B1FA2", "#C2185B", "#0097A7", "#FFA000"
                        ),
                        "borderWidth" to 1
                    )
                )
            ),
            "options" to mapOf(
                "scales" to mapOf(
                    "y" to mapOf(
                        "beginAtZero" to true,
                        "title" to mapOf("display" to true, "text" to "Amount (g/kcal)")
                    )
                ),
                "plugins" to mapOf(
                    "title" to mapOf("display" to true, "text" to "Nutritional Information")
                )
            )
        )
        return Json.encodeToString(config)
    }

    /**
     * Generate Chart.js config for mood chart
     */
    private fun generateMoodChartJsConfig(moodPoints: List<MoodDataPoint>): String {
        val config = mapOf(
            "type" to "line",
            "data" to mapOf(
                "labels" to moodPoints.map { it.date },
                "datasets" to listOf(
                    mapOf(
                        "label" to "Mood Score",
                        "data" to moodPoints.map { it.value },
                        "borderColor" to "#2196F3",
                        "backgroundColor" to "#2196F388",
                        "fill" to false,
                        "tension" to 0.4
                    )
                )
            ),
            "options" to mapOf(
                "scales" to mapOf(
                    "y" to mapOf(
                        "beginAtZero" to true,
                        "max" to 10,
                        "title" to mapOf("display" to true, "text" to "Mood Score")
                    ),
                    "x" to mapOf(
                        "title" to mapOf("display" to true, "text" to "Date")
                    )
                ),
                "plugins" to mapOf(
                    "title" to mapOf("display" to true, "text" to "Mood Tracking")
                )
            )
        )
        return Json.encodeToString(config)
    }

    /**
     * Generate Chart.js config for progress chart
     */
    private fun generateProgressChartJsConfig(categories: List<ProgressCategory>): String {
        val config = mapOf(
            "type" to "bar",
            "data" to mapOf(
                "labels" to categories.map { it.name },
                "datasets" to listOf(
                    mapOf(
                        "label" to "Progress",
                        "data" to categories.map { it.progress },
                        "backgroundColor" to categories.map { it.color.toHexString() },
                        "borderColor" to categories.map { it.color.toHexString() },
                        "borderWidth" to 1
                    )
                )
            ),
            "options" to mapOf(
                "scales" to mapOf(
                    "y" to mapOf(
                        "beginAtZero" to true,
                        "max" to (categories.maxOfOrNull { it.maxProgress }?.toDouble() ?: 100.0), // ✅ wrap in ()
                        "title" to mapOf("display" to true, "text" to "Progress")
                    )
                ),
                "plugins" to mapOf(
                    "title" to mapOf("display" to true, "text" to "Progress Overview")
                )
            )
        )
        return Json.encodeToString(config)
    }

    /**
     * Generate Chart.js config for cost chart
     */
    private fun generateCostChartJsConfig(costItems: List<CostItem>, currency: String): String {
        val config = mapOf(
            "type" to "pie",
            "data" to mapOf(
                "labels" to costItems.map { it.name },
                "datasets" to listOf(
                    mapOf(
                        "label" to "Cost ($currency)",
                        "data" to costItems.map { it.amount },
                        "backgroundColor" to costItems.map { it.color.toHexString() },
                        "borderColor" to costItems.map { it.color.toHexString() },
                        "borderWidth" to 1
                    )
                )
            ),
            "options" to mapOf(
                "plugins" to mapOf(
                    "title" to mapOf("display" to true, "text" to "Cost Breakdown"),
                    "legend" to mapOf("position" to "right")
                )
            )
        )
        return Json.encodeToString(config)
    }

    // Helper to convert Color to hex string
    private fun Color.toHexString(): String {
        val r = (red * 255).toInt().coerceIn(0, 255)
        val g = (green * 255).toInt().coerceIn(0, 255)
        val b = (blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }
    /**
     * Parse recipe data with localization
     */
    private fun parseRecipeData(section: String): MessageElement {
        val titlePattern = Pattern.compile("^([^\\n]+)", Pattern.MULTILINE)
        val ingredientPattern = Pattern.compile("(?:ingredients?|what you need):\\s*([^\\n]+(?:\\n\\s*-[^\\n]+)*)", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE)
        val stepsPattern = Pattern.compile("(?:steps?|instructions?|method):\\s*([^\\n]+(?:\\n\\s*\\d+\\.[^\\n]+)*)", Pattern.CASE_INSENSITIVE or Pattern.MULTILINE)
        
        val title = titlePattern.matcher(section).let { if (it.find()) it.group(1).trim() else "Recipe" }
        val ingredients = ingredientPattern.matcher(section).let { 
            if (it.find()) {
                it.group(1).split("\n").map { ingredient -> ingredient.replace(Regex("^\\s*-\\s*"), "").trim() }
            } else emptyList()
        }
        val steps = stepsPattern.matcher(section).let {
            if (it.find()) {
                it.group(1).split("\n").map { step -> step.replace(Regex("^\\s*\\d+\\.\\s*"), "").trim() }
            } else emptyList()
        }
        
        // Localize ingredients
        val localizedIngredients = localizeIngredients(ingredients)
        
        return MessageElement.Recipe(
            title = title,
            ingredients = localizedIngredients,
            steps = steps,
            culturalNotes = getCulturalRecipeNotes(title),
            localizedTips = getLocalizedRecipeTips(title)
        )
    }
    
    /**
     * Parse instruction lists with numbering
     */
    private fun parseInstructions(section: String): MessageElement {
        val steps = section.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, step ->
                val cleanStep = step.replace(Regex("^\\d+\\.\\s*"), "").replace(Regex("^-\\s*"), "")
                InstructionStep(
                    number = index + 1,
                    instruction = cleanStep,
                    isCompleted = false
                )
            }
        
        return MessageElement.Instructions(
            title = "Instructions",
            steps = steps,
            allowInteraction = true
        )
    }
    
    /**
     * Parse cost data with localized currency
     */
    private fun parseCostData(section: String): MessageElement.CostBreakdown? {
        return parseCostChart(section)?.let { chartElement ->
            val chartData = chartElement.data as? CostBreakdownData
            chartData?.let {
                MessageElement.CostBreakdown(
                    title = "Cost Breakdown",
                    items = it.items,
                    total = it.total,
                    currency = it.currency,
                    localizedNotes = getLocalizedCostNotes()
                )
            }
        }
    }
    
    /**
     * Parse progress data with skills tracking
     */
    private fun parseProgressData(section: String): MessageElement.ProgressTracking? {
        return parseProgressChart(section)?.let { chartElement ->
            val progressData = chartElement.data as? ProgressData
            progressData?.let {
                MessageElement.ProgressTracking(
                    title = it.title,
                    categories = it.categories,
                    motivationalMessage = getMotivationalMessage(it.categories)
                )
            }
        }
    }
    
    /**
     * Parse localized content with cultural expressions
     */
    private fun parseLocalizedContent(section: String): MessageElement {
        val culturalExpressions = localizationManager.getCulturalExpressions()
        val seasonalContext = localizationManager.getSeasonalContext()
        
        // Check if section contains cultural expressions
        val usedExpressions = culturalExpressions.filter { expression ->
            section.contains(expression.phrase, ignoreCase = true)
        }
        
        return MessageElement.LocalizedContent(
            content = section,
            culturalExpressions = usedExpressions,
            seasonalContext = seasonalContext,
            localizedTerms = extractLocalizedTerms(section)
        )
    }
    
    // Helper methods for localization
    private fun localizeIngredients(ingredients: List<String>): List<LocalizedIngredient> {
        val localRecipes = localizationManager.getLocalizedRecipes()
        
        return ingredients.map { ingredient ->
            // Try to find local names and alternatives
            val localAlternative = findLocalIngredientAlternative(ingredient)
            LocalizedIngredient(
                name = ingredient,
                localName = localAlternative?.localName,
                alternatives = localAlternative?.alternatives ?: emptyList(),
                availability = localAlternative?.availability ?: "Check local markets"
            )
        }
    }
    
    private fun findLocalIngredientAlternative(ingredient: String): LocalIngredientInfo? {
        val localMaterials = localizationManager.getLocalizedDIYMaterials()
        // This would be expanded with a comprehensive ingredient mapping database
        return when (ingredient.lowercase()) {
            "flour" -> LocalIngredientInfo("flour", "mealie meal", listOf("cassava flour", "sweet potato flour"), "Available at most shops")
            "milk" -> LocalIngredientInfo("milk", "amafi/mukaka", listOf("powdered milk", "sour milk"), "Available fresh in towns")
            else -> null
        }
    }
    
    private fun getCulturalRecipeNotes(recipeTitle: String): String {
        val locale = localizationManager.currentLocale.value
        return when (locale.countryCode) {
            "ZM" -> "In Zambia, this dish is often shared during family gatherings. Ubuntu philosophy encourages sharing meals with neighbors."
            "ZW" -> "This is a traditional Zimbabwean preparation. In our culture, cooking together strengthens family bonds."
            else -> "This recipe reflects our rich cultural heritage and community traditions."
        }
    }
    
    private fun getLocalizedRecipeTips(recipeTitle: String): List<String> {
        val locale = localizationManager.currentLocale.value
        val tips = mutableListOf<String>()
        
        when (locale.countryCode) {
            "ZM" -> {
                tips.add("During load shedding, use a charcoal brazier (imbabula) for cooking")
                tips.add("Fresh vegetables are best bought early morning at the market")
                tips.add("Keep ingredients cool using clay pots (imbiya) during hot weather")
            }
            "ZW" -> {
                tips.add("During power cuts, cook with firewood or gas if available")
                tips.add("Shop for fresh produce at mbare or local vegetable markets")
                tips.add("Store food in cool, dark places during the hot season")
            }
        }
        
        return tips
    }
    
    private fun getLocalizedCostNotes(): String {
        val locale = localizationManager.currentLocale.value
        return when (locale.countryCode) {
            "ZM" -> "Prices shown in Kwacha (ZMW). Check local markets for bulk discounts. Shop early for best prices."
            "ZW" -> "Prices in USD. Consider buying in bulk and sharing costs with neighbors. Check multiple vendors."
            else -> "Prices may vary by location and season."
        }
    }
    
    private fun getMotivationalMessage(categories: List<ProgressCategory>): String {
        val overallProgress = categories.map { it.progress / it.maxProgress }.average()
        val tribalLang = localizationManager.selectedTribalLanguage.value
        
        val baseMessage = when {
            overallProgress >= 0.8 -> "Excellent progress! You're doing amazing!"
            overallProgress >= 0.6 -> "Great work! Keep building your skills!"
            overallProgress >= 0.4 -> "Good progress! Stay motivated!"
            else -> "Every expert was once a beginner. Keep going!"
        }
        
        // Add cultural encouragement
        return if (tribalLang != null) {
            when (tribalLang.name) {
                "Bemba" -> "$baseMessage Umwana ashenda atasha nyina ukwabula - keep learning!"
                "Shona" -> "$baseMessage Rume rimwe harikombi inda - we're here to support you!"
                "Ndebele" -> "$baseMessage Ubuntu ngumuntu ngabanye abantu - we grow together!"
                else -> baseMessage
            }
        } else baseMessage
    }
    
    private fun extractLocalizedTerms(content: String): List<LocalizedTerm> {
        val terms = mutableListOf<LocalizedTerm>()
        val tribalLang = localizationManager.selectedTribalLanguage.value
        
        tribalLang?.let { lang ->
            lang.commonGreetings.forEach { (phrase, translation) ->
                if (content.contains(phrase, ignoreCase = true)) {
                    terms.add(LocalizedTerm(phrase, translation, lang.name))
                }
            }
        }
        
        return terms
    }
}

// Extension functions
private fun String.isChartData(): Boolean = 
    contains("chart", ignoreCase = true) || 
    contains("nutrition", ignoreCase = true) ||
    contains("progress", ignoreCase = true) ||
    contains("mood", ignoreCase = true) ||
    contains("cost", ignoreCase = true) ||
    Regex("\\d+(?:\\.\\d+)?\\s*[gkm]?g?").containsMatchIn(this)

private fun String.isRecipeData(): Boolean = 
    contains("recipe", ignoreCase = true) || 
    contains("ingredients", ignoreCase = true) ||
    contains("instructions", ignoreCase = true)

private fun String.isInstructionList(): Boolean =
    Regex("^\\s*\\d+\\.").containsMatchIn(this) ||
    contains("steps:", ignoreCase = true) ||
    contains("instructions:", ignoreCase = true)

private fun String.isCostBreakdown(): Boolean =
    Regex("[K$]\\d+(?:\\.\\d+)?").containsMatchIn(this) ||
    contains("total", ignoreCase = true) ||
    contains("cost", ignoreCase = true)

private fun String.isProgressData(): Boolean =
    contains("progress", ignoreCase = true) ||
    Regex("\\d+/\\d+").containsMatchIn(this) ||
    contains("completed", ignoreCase = true)

private fun String.isLocalizedContent(): Boolean =
    contains("cultural", ignoreCase = true) ||
    contains("traditional", ignoreCase = true) ||
    contains("local", ignoreCase = true)

// Data models
@Serializable
data class FormattedMessage(
    val elements: List<MessageElement>,
    val hasInteractiveContent: Boolean,
    val metadata: MessageMetadata
)

@Serializable
data class MessageMetadata(
    val locale: com.craftflowtechnologies.guidelens.localization.LocaleInfo,
    val tribalLanguage: String?,
    val agentType: String
)
// Data models
@Serializable
sealed interface ChartData

@Serializable
data class NutritionData(
    val calories: Float,
    val proteins: Float,
    val carbohydrates: Float,
    val fats: Float,
    val fiber: Float,
    val sugar: Float,
    val sodium: Float
) : ChartData

@Serializable
data class MoodData(
    val points: List<MoodDataPoint>
) : ChartData

@Serializable
data class MoodDataPoint(
    val date: String,
    val value: Float
)

@Serializable
data class ProgressData(
    val title: String,
    val categories: List<ProgressCategory>
) : ChartData

@Serializable
data class ProgressCategory(
    val name: String,
    val progress: Float,
    val maxProgress: Float,
    @Serializable(with = ColorAsHexSerializer::class)
    val color: Color
)

@Serializable
data class CostBreakdownData(
    val total: Float,
    val currency: String,
    val items: List<CostItem>
) : ChartData

@Serializable
data class CostItem(
    val name: String,
    val amount: Float,
    @Serializable(with = ColorAsHexSerializer::class)
    val color: Color
)


@Serializable
sealed class MessageElement {
    abstract fun isInteractive(): Boolean
    
    @Serializable
    data class Text(
        val content: String,
        val style: TextStyle
    ) : MessageElement() {
        override fun isInteractive() = false
    }


    data class Chart(
        val chartType: ChartType,
        val data: ChartData, // Updated to use ChartData sealed interface
        val title: String,
        val chartJsConfig: String // Store Chart.js JSON config
    ) : MessageElement() {
        override fun isInteractive() = true
    }
    
    @Serializable
    data class Recipe(
        val title: String,
        val ingredients: List<LocalizedIngredient>,
        val steps: List<String>,
        val culturalNotes: String,
        val localizedTips: List<String>
    ) : MessageElement() {
        override fun isInteractive() = false
    }
    
    @Serializable
    data class Instructions(
        val title: String,
        val steps: List<InstructionStep>,
        val allowInteraction: Boolean
    ) : MessageElement() {
        override fun isInteractive() = allowInteraction
    }
    
    @Serializable
    data class CostBreakdown(
        val title: String,
        val items: List<CostItem>,
        val total: Float,
        val currency: String,
        val localizedNotes: String
    ) : MessageElement() {
        override fun isInteractive() = true
    }
    
    @Serializable
    data class ProgressTracking(
        val title: String,
        val categories: List<ProgressCategory>,
        val motivationalMessage: String
    ) : MessageElement() {
        override fun isInteractive() = true
    }
    
    @Serializable
    data class LocalizedContent(
        val content: String,
        val culturalExpressions: List<com.craftflowtechnologies.guidelens.localization.CulturalExpression>,
        val seasonalContext: String,
        val localizedTerms: List<LocalizedTerm>
    ) : MessageElement() {
        override fun isInteractive() = false
    }
}

enum class ChartType {
    NUTRITION, MOOD_TRACKING, PROGRESS, COST_BREAKDOWN
}

enum class TextStyle {
    HEADING, BODY, CAPTION, EMPHASIS
}

@Serializable
data class LocalizedIngredient(
    val name: String,
    val localName: String?,
    val alternatives: List<String>,
    val availability: String
)

@Serializable
data class InstructionStep(
    val number: Int,
    val instruction: String,
    val isCompleted: Boolean
)

@Serializable
data class LocalizedTerm(
    val phrase: String,
    val translation: String,
    val language: String
)

data class LocalIngredientInfo(
    val name: String,
    val localName: String,
    val alternatives: List<String>,
    val availability: String
)

object ColorAsHexSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val argb = value.value.toULong()
        val hex = "#%08X".format(argb.toLong()) // ARGB hex format
        encoder.encodeString(hex)
    }

    override fun deserialize(decoder: Decoder): Color {
        val hex = decoder.decodeString().removePrefix("#")
        val colorLong = hex.toULong(16)
        return Color(colorLong)
    }
}