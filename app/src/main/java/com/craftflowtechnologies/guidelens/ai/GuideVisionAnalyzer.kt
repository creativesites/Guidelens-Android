package com.craftflowtechnologies.guidelens.ai

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.craftflowtechnologies.guidelens.utils.GuideErrorManager
import com.craftflowtechnologies.guidelens.utils.GuideErrorCategory
import com.craftflowtechnologies.guidelens.utils.GuideErrorSeverity
import com.craftflowtechnologies.guidelens.utils.GuidePerformanceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * AI-Powered Visual Recognition System for GuideLens
 * Provides real-time analysis of cooking ingredients, tools, and cooking stages
 * Optimized for Zambian cooking context and ingredients
 */
class GuideVisionAnalyzer private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuideVisionAnalyzer()
        
        private const val ANALYSIS_CONFIDENCE_THRESHOLD = 0.7f
        const val CACHE_DURATION_MS = 30_000L // 30 seconds
    }
    
    // Analysis state management
    private val _currentAnalysis = MutableStateFlow<GuideVisionResult?>(null)
    val currentAnalysis: StateFlow<GuideVisionResult?> = _currentAnalysis.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _analysisHistory = MutableStateFlow<List<GuideVisionResult>>(emptyList())
    val analysisHistory: StateFlow<List<GuideVisionResult>> = _analysisHistory.asStateFlow()
    
    // Caching for performance
    private val analysisCache = ConcurrentHashMap<String, GuideCachedAnalysis>()
    
    /**
     * Primary analysis function - analyzes frame for ingredients, tools, and cooking stages
     */
    suspend fun analyzeFrame(
        image: ImageBitmap,
        context: GuideVisionContext = GuideVisionContext.GENERAL,
        priority: GuideVisionPriority = GuideVisionPriority.NORMAL
    ): GuideVisionResult = withContext(Dispatchers.Default) {
        
        _isAnalyzing.value = true
        
        try {
            val result = GuidePerformanceManager.instance.measureSuspendOperation("vision_analysis") {
                performVisionAnalysis(image, context, priority)
            }
            
            _currentAnalysis.value = result
            addToHistory(result)
            
            result
            
        } catch (e: Exception) {
            GuideErrorManager.instance.reportError(
                exception = e,
                context = "Vision analysis failed",
                category = GuideErrorCategory.AI_SERVICE,
                severity = GuideErrorSeverity.WARNING
            )
            
            GuideVisionResult.error("Analysis failed: ${e.message}")
            
        } finally {
            _isAnalyzing.value = false
        }
    }
    
    /**
     * Specialized analysis for Zambian cooking ingredients
     */
    suspend fun analyzeZambianIngredients(image: ImageBitmap): GuideVisionResult {
        return analyzeFrame(
            image = image,
            context = GuideVisionContext.ZAMBIAN_INGREDIENTS,
            priority = GuideVisionPriority.HIGH
        )
    }
    
    /**
     * Analyze cooking stage/progress
     */
    suspend fun analyzeCookingStage(
        image: ImageBitmap,
        expectedStage: String? = null
    ): GuideCookingStageResult {
        return withContext(Dispatchers.Default) {
            try {
                val analysis = analyzeFrame(image, GuideVisionContext.COOKING_STAGE)
                
                GuideCookingStageResult(
                    stage = detectCookingStage(analysis),
                    confidence = analysis.confidence,
                    suggestions = generateStageSuggestions(analysis, expectedStage),
                    warnings = detectCookingWarnings(analysis)
                )
                
            } catch (e: Exception) {
                GuideCookingStageResult.error("Stage analysis failed")
            }
        }
    }
    
    /**
     * Quick ingredient identification for shopping/preparation
     */
    suspend fun quickIdentifyIngredients(image: ImageBitmap): List<GuideIngredient> {
        return withContext(Dispatchers.Default) {
            try {
                val analysis = analyzeFrame(image, GuideVisionContext.INGREDIENT_IDENTIFICATION)
                extractIngredients(analysis)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    // Core analysis implementation
    private suspend fun performVisionAnalysis(
        image: ImageBitmap,
        context: GuideVisionContext,
        priority: GuideVisionPriority
    ): GuideVisionResult {
        
        // Check cache first
        val cacheKey = generateCacheKey(image, context)
        analysisCache[cacheKey]?.let { cached ->
            if (cached.isValid()) {
                return cached.result
            }
        }
        
        // Convert to Android Bitmap for processing
        val bitmap = image.asAndroidBitmap()
        
        // Perform analysis based on context
        val result = when (context) {
            GuideVisionContext.ZAMBIAN_INGREDIENTS -> analyzeZambianIngredientsInternal(bitmap)
            GuideVisionContext.COOKING_TOOLS -> analyzeCookingTools(bitmap)
            GuideVisionContext.COOKING_STAGE -> analyzeCookingStageInternal(bitmap)
            GuideVisionContext.INGREDIENT_IDENTIFICATION -> analyzeIngredientsGeneral(bitmap)
            GuideVisionContext.FOOD_SAFETY -> analyzeFoodSafety(bitmap)
            GuideVisionContext.GENERAL -> analyzeGeneral(bitmap)
        }
        
        // Cache result if confidence is high enough
        if (result.confidence > ANALYSIS_CONFIDENCE_THRESHOLD) {
            analysisCache[cacheKey] = GuideCachedAnalysis(
                result = result,
                timestamp = System.currentTimeMillis()
            )
        }
        
        return result
    }
    
    // Specialized analysis implementations
    private suspend fun analyzeZambianIngredientsInternal(bitmap: Bitmap): GuideVisionResult {
        // This would integrate with actual ML models
        // For now, we'll simulate with intelligent recognition patterns
        
        val detectedIngredients = mutableListOf<GuideDetectedItem>()
        val zambinaIngredients = getZambianIngredientDatabase()
        
        // Simulate ML detection with common Zambian ingredients
        // In production, this would use TensorFlow Lite or similar
        detectedIngredients.addAll(
            simulateIngredientDetection(bitmap, zambinaIngredients)
        )
        
        return GuideVisionResult(
            detectedItems = detectedIngredients,
            context = GuideVisionContext.ZAMBIAN_INGREDIENTS,
            confidence = calculateOverallConfidence(detectedIngredients),
            suggestions = generateZambianCookingSuggestions(detectedIngredients),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun analyzeCookingTools(bitmap: Bitmap): GuideVisionResult {
        val detectedTools = simulateToolDetection(bitmap)
        
        return GuideVisionResult(
            detectedItems = detectedTools,
            context = GuideVisionContext.COOKING_TOOLS,
            confidence = calculateOverallConfidence(detectedTools),
            suggestions = generateToolUsageTips(detectedTools),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun analyzeCookingStageInternal(bitmap: Bitmap): GuideVisionResult {
        val stageAnalysis = simulateStageDetection(bitmap)
        
        return GuideVisionResult(
            detectedItems = stageAnalysis,
            context = GuideVisionContext.COOKING_STAGE,
            confidence = calculateOverallConfidence(stageAnalysis),
            suggestions = generateStageGuidance(stageAnalysis),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun analyzeIngredientsGeneral(bitmap: Bitmap): GuideVisionResult {
        val ingredients = simulateGeneralIngredientDetection(bitmap)
        
        return GuideVisionResult(
            detectedItems = ingredients,
            context = GuideVisionContext.INGREDIENT_IDENTIFICATION,
            confidence = calculateOverallConfidence(ingredients),
            suggestions = generateIngredientTips(ingredients),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun analyzeFoodSafety(bitmap: Bitmap): GuideVisionResult {
        val safetyChecks = simulateFoodSafetyAnalysis(bitmap)
        
        return GuideVisionResult(
            detectedItems = safetyChecks,
            context = GuideVisionContext.FOOD_SAFETY,
            confidence = calculateOverallConfidence(safetyChecks),
            suggestions = generateSafetyRecommendations(safetyChecks),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun analyzeGeneral(bitmap: Bitmap): GuideVisionResult {
        val generalItems = simulateGeneralDetection(bitmap)
        
        return GuideVisionResult(
            detectedItems = generalItems,
            context = GuideVisionContext.GENERAL,
            confidence = calculateOverallConfidence(generalItems),
            suggestions = generateGeneralSuggestions(generalItems),
            timestamp = System.currentTimeMillis()
        )
    }
    
    // Helper functions
    private fun generateCacheKey(image: ImageBitmap, context: GuideVisionContext): String {
        // Simple hash based on image dimensions and context
        return "${image.width}x${image.height}_${context.name}_${image.hashCode()}"
    }
    
    private fun addToHistory(result: GuideVisionResult) {
        val currentHistory = _analysisHistory.value.toMutableList()
        currentHistory.add(0, result)
        
        // Keep only last 50 analyses
        if (currentHistory.size > 50) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _analysisHistory.value = currentHistory
    }
    
    private fun calculateOverallConfidence(items: List<GuideDetectedItem>): Float {
        if (items.isEmpty()) return 0f
        return items.map { it.confidence }.average().toFloat()
    }
    
    // Data simulation functions (replace with actual ML in production)
    private fun getZambianIngredientDatabase(): List<ZambianIngredient> {
        return listOf(
            ZambianIngredient("Mealie Meal", "White corn flour", listOf("nshima", "ubwali")),
            ZambianIngredient("Kapenta", "Small dried fish", listOf("protein", "traditional")),
            ZambianIngredient("Chibwabwa", "Pumpkin leaves", listOf("greens", "vegetables")),
            ZambianIngredient("Delele", "Okra", listOf("vegetables", "traditional")),
            ZambianIngredient("Impwa", "Eggplant", listOf("vegetables")),
            ZambianIngredient("Groundnuts", "Peanuts", listOf("protein", "traditional")),
            ZambianIngredient("Sweet Potato", "Orange tubers", listOf("carbs", "traditional")),
            ZambianIngredient("Cassava", "Root vegetable", listOf("carbs", "traditional")),
            ZambianIngredient("Bream", "Fresh fish", listOf("protein", "local")),
            ZambianIngredient("Samp", "Crushed corn", listOf("carbs", "traditional"))
        )
    }
    
    private fun simulateIngredientDetection(
        bitmap: Bitmap, 
        database: List<ZambianIngredient>
    ): List<GuideDetectedItem> {
        // Simulate ML detection - in production this would use actual computer vision
        return database.take(3).map { ingredient ->
            GuideDetectedItem(
                id = ingredient.name.lowercase().replace(" ", "_"),
                name = ingredient.name,
                category = GuideItemCategory.INGREDIENT,
                confidence = (0.7f + Math.random().toFloat() * 0.3f).coerceAtMost(1f),
                boundingBox = GuideBoundingBox(0.1f, 0.1f, 0.8f, 0.8f),
                attributes = mapOf(
                    "type" to ingredient.description,
                    "uses" to ingredient.commonUses.joinToString(", "),
                    "zambian_name" to (ingredient.localNames.firstOrNull() ?: ingredient.name)
                )
            )
        }
    }
    
    private fun simulateToolDetection(bitmap: Bitmap): List<GuideDetectedItem> {
        val commonTools = listOf(
            "Cooking Pot", "Wooden Spoon", "Knife", "Cutting Board", 
            "Mortar and Pestle", "Clay Pot", "Strainer"
        )
        
        return commonTools.take(2).map { tool ->
            GuideDetectedItem(
                id = tool.lowercase().replace(" ", "_"),
                name = tool,
                category = GuideItemCategory.TOOL,
                confidence = (0.6f + Math.random().toFloat() * 0.4f).coerceAtMost(1f),
                boundingBox = GuideBoundingBox(0.2f, 0.2f, 0.6f, 0.6f),
                attributes = mapOf("usage_tips" to "Handle with care")
            )
        }
    }
    
    private fun simulateStageDetection(bitmap: Bitmap): List<GuideDetectedItem> {
        val stages = listOf("Raw", "Cooking", "Nearly Done", "Ready", "Overcooked")
        val selectedStage = stages.random()
        
        return listOf(
            GuideDetectedItem(
                id = "cooking_stage",
                name = selectedStage,
                category = GuideItemCategory.COOKING_STAGE,
                confidence = (0.7f + Math.random().toFloat() * 0.3f).coerceAtMost(1f),
                boundingBox = GuideBoundingBox(0f, 0f, 1f, 1f),
                attributes = mapOf(
                    "stage" to selectedStage,
                    "next_action" to getNextAction(selectedStage)
                )
            )
        )
    }
    
    private fun simulateGeneralIngredientDetection(bitmap: Bitmap): List<GuideDetectedItem> {
        // Generic ingredient detection
        return listOf(
            GuideDetectedItem(
                id = "ingredient_1",
                name = "Vegetable",
                category = GuideItemCategory.INGREDIENT,
                confidence = 0.8f,
                boundingBox = GuideBoundingBox(0.1f, 0.1f, 0.5f, 0.5f)
            )
        )
    }
    
    private fun simulateFoodSafetyAnalysis(bitmap: Bitmap): List<GuideDetectedItem> {
        return listOf(
            GuideDetectedItem(
                id = "safety_check",
                name = "Food appears safe",
                category = GuideItemCategory.SAFETY_CHECK,
                confidence = 0.9f,
                boundingBox = GuideBoundingBox(0f, 0f, 1f, 1f),
                attributes = mapOf("status" to "safe")
            )
        )
    }
    
    private fun simulateGeneralDetection(bitmap: Bitmap): List<GuideDetectedItem> {
        return listOf(
            GuideDetectedItem(
                id = "general_item",
                name = "Kitchen Item",
                category = GuideItemCategory.GENERAL,
                confidence = 0.7f,
                boundingBox = GuideBoundingBox(0.2f, 0.2f, 0.8f, 0.8f)
            )
        )
    }
    
    // Suggestion generation functions
    private fun generateZambianCookingSuggestions(items: List<GuideDetectedItem>): List<String> {
        val suggestions = mutableListOf<String>()
        
        items.forEach { item ->
            when (item.name) {
                "Mealie Meal" -> suggestions.add("Perfect for making nshima! Use 3:1 water to mealie meal ratio.")
                "Kapenta" -> suggestions.add("Soak kapenta in warm water before cooking to remove excess salt.")
                "Chibwabwa" -> suggestions.add("Remove tough stems and cook with groundnut powder for authentic taste.")
                "Groundnuts" -> suggestions.add("Toast lightly before grinding for better flavor in your relish.")
            }
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("Try combining these ingredients with traditional Zambian spices!")
        }
        
        return suggestions
    }
    
    private fun generateToolUsageTips(items: List<GuideDetectedItem>): List<String> {
        return items.map { item ->
            when (item.name) {
                "Wooden Spoon" -> "Perfect for stirring nshima - won't scratch your pot!"
                "Mortar and Pestle" -> "Ideal for grinding spices and making fresh groundnut powder"
                "Clay Pot" -> "Great for slow cooking traditional stews"
                else -> "Use this tool carefully for best results"
            }
        }
    }
    
    private fun generateStageGuidance(items: List<GuideDetectedItem>): List<String> {
        return items.mapNotNull { item ->
            when (item.attributes["stage"]) {
                "Raw" -> "Start cooking on medium heat"
                "Cooking" -> "Stir occasionally to prevent burning"
                "Nearly Done" -> "Reduce heat and taste for seasoning"
                "Ready" -> "Your dish is ready to serve!"
                "Overcooked" -> "Next time, reduce cooking time slightly"
                else -> null
            }
        }
    }
    
    private fun generateIngredientTips(items: List<GuideDetectedItem>): List<String> {
        return listOf("Fresh ingredients detected - perfect for cooking!")
    }
    
    private fun generateSafetyRecommendations(items: List<GuideDetectedItem>): List<String> {
        return listOf("Food appears safe to consume")
    }
    
    private fun generateGeneralSuggestions(items: List<GuideDetectedItem>): List<String> {
        return listOf("Point your camera at ingredients or cooking tools for specific guidance")
    }
    
    // Helper functions for cooking stage analysis
    private fun detectCookingStage(analysis: GuideVisionResult): String {
        return analysis.detectedItems.firstOrNull { it.category == GuideItemCategory.COOKING_STAGE }?.name ?: "Unknown"
    }
    
    private fun generateStageSuggestions(analysis: GuideVisionResult, expectedStage: String?): List<String> {
        return analysis.suggestions
    }
    
    private fun detectCookingWarnings(analysis: GuideVisionResult): List<String> {
        val warnings = mutableListOf<String>()
        
        analysis.detectedItems.forEach { item ->
            if (item.name == "Overcooked") {
                warnings.add("Food appears overcooked - reduce heat next time")
            }
        }
        
        return warnings
    }
    
    private fun extractIngredients(analysis: GuideVisionResult): List<GuideIngredient> {
        return analysis.detectedItems
            .filter { it.category == GuideItemCategory.INGREDIENT }
            .map { item ->
                GuideIngredient(
                    name = item.name,
                    confidence = item.confidence,
                    category = item.attributes["type"] ?: "Unknown",
                    zambianName = item.attributes["zambian_name"] ?: item.name
                )
            }
    }
    
    private fun getNextAction(stage: String): String {
        return when (stage) {
            "Raw" -> "Start cooking"
            "Cooking" -> "Continue cooking"
            "Nearly Done" -> "Check seasoning"
            "Ready" -> "Serve immediately"
            "Overcooked" -> "Reduce heat next time"
            else -> "Monitor closely"
        }
    }
}

// Data classes and enums
data class GuideVisionResult(
    val detectedItems: List<GuideDetectedItem>,
    val context: GuideVisionContext,
    val confidence: Float,
    val suggestions: List<String>,
    val timestamp: Long
) {
    companion object {
        fun error(message: String) = GuideVisionResult(
            detectedItems = emptyList(),
            context = GuideVisionContext.GENERAL,
            confidence = 0f,
            suggestions = listOf(message),
            timestamp = System.currentTimeMillis()
        )
    }
}

data class GuideDetectedItem(
    val id: String,
    val name: String,
    val category: GuideItemCategory,
    val confidence: Float,
    val boundingBox: GuideBoundingBox,
    val attributes: Map<String, String> = emptyMap()
)

data class GuideBoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class GuideItemCategory {
    INGREDIENT,
    TOOL,
    COOKING_STAGE,
    SAFETY_CHECK,
    GENERAL
}

enum class GuideVisionContext {
    ZAMBIAN_INGREDIENTS,
    COOKING_TOOLS,
    COOKING_STAGE,
    INGREDIENT_IDENTIFICATION,
    FOOD_SAFETY,
    GENERAL
}

enum class GuideVisionPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

data class GuideCookingStageResult(
    val stage: String,
    val confidence: Float,
    val suggestions: List<String>,
    val warnings: List<String>
) {
    companion object {
        fun error(message: String) = GuideCookingStageResult(
            stage = "Unknown",
            confidence = 0f,
            suggestions = emptyList(),
            warnings = listOf(message)
        )
    }
}

data class GuideIngredient(
    val name: String,
    val confidence: Float,
    val category: String,
    val zambianName: String
)

data class ZambianIngredient(
    val name: String,
    val description: String,
    val commonUses: List<String>,
    val localNames: List<String> = emptyList()
)

private data class GuideCachedAnalysis(
    val result: GuideVisionResult,
    val timestamp: Long
) {
    fun isValid(): Boolean {
        return System.currentTimeMillis() - timestamp < GuideVisionAnalyzer.CACHE_DURATION_MS
    }
}

// Composable utilities
@Composable
fun rememberGuideVisionAnalysis(): State<GuideVisionResult?> {
    return GuideVisionAnalyzer.instance.currentAnalysis.collectAsState()
}

@Composable
fun rememberGuideVisionState(): State<Boolean> {
    return GuideVisionAnalyzer.instance.isAnalyzing.collectAsState()
}