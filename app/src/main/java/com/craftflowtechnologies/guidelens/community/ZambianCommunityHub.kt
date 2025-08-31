package com.craftflowtechnologies.guidelens.community

import androidx.compose.runtime.*
import com.craftflowtechnologies.guidelens.localization.ZambianGuideLocalization
import com.craftflowtechnologies.guidelens.utils.GuideErrorManager
import com.craftflowtechnologies.guidelens.utils.GuideErrorCategory
import com.craftflowtechnologies.guidelens.utils.GuideErrorSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Zambian Community Features Hub for GuideLens
 * Connects Zambian users with shared recipes, local knowledge, and cultural cooking wisdom
 */
class ZambianCommunityHub private constructor() {
    
    companion object {
        @JvmStatic
        val instance = ZambianCommunityHub()
        
        private const val MAX_COMMUNITY_RECIPES = 200
        private const val RECIPE_CACHE_DURATION_MS = 3600_000L // 1 hour
    }
    
    // Community state management
    private val _communityRecipes = MutableStateFlow<List<ZambianCommunityRecipe>>(emptyList())
    val communityRecipes: StateFlow<List<ZambianCommunityRecipe>> = _communityRecipes.asStateFlow()
    
    private val _featuredContent = MutableStateFlow<List<ZambianFeaturedContent>>(emptyList())
    val featuredContent: StateFlow<List<ZambianFeaturedContent>> = _featuredContent.asStateFlow()
    
    private val _localIngredients = MutableStateFlow<List<ZambianLocalIngredient>>(emptyList())
    val localIngredients: StateFlow<List<ZambianLocalIngredient>> = _localIngredients.asStateFlow()
    
    private val _seasonalRecommendations = MutableStateFlow<List<ZambianSeasonalRecommendation>>(emptyList())
    val seasonalRecommendations: StateFlow<List<ZambianSeasonalRecommendation>> = _seasonalRecommendations.asStateFlow()
    
    private val _communityTips = MutableStateFlow<List<ZambianCommunityTip>>(emptyList())
    val communityTips: StateFlow<List<ZambianCommunityTip>> = _communityTips.asStateFlow()
    
    // Internal storage and caching
    private val recipeStorage = ConcurrentHashMap<String, ZambianCommunityRecipe>()
    private val ingredientSubstitutions = ConcurrentHashMap<String, List<ZambianIngredientSubstitution>>()
    private val communityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        initializeCommunityData()
        startPeriodicUpdates()
    }
    
    /**
     * Share a recipe with the Zambian community
     */
    suspend fun shareRecipe(
        title: String,
        description: String,
        ingredients: List<String>,
        instructions: List<String>,
        region: ZambianRegion,
        difficulty: ZambianRecipeDifficulty,
        cookingTime: Int, // in minutes
        servings: Int,
        tags: List<String> = emptyList(),
        culturalNotes: String = "",
        authorName: String = "Anonymous"
    ): String = withContext(Dispatchers.IO) {
        
        try {
            val recipeId = generateRecipeId()
            val recipe = ZambianCommunityRecipe(
                id = recipeId,
                title = title,
                description = description,
                ingredients = ingredients,
                instructions = instructions,
                region = region,
                difficulty = difficulty,
                cookingTimeMinutes = cookingTime,
                servings = servings,
                tags = tags,
                culturalNotes = culturalNotes,
                authorName = authorName,
                createdAt = System.currentTimeMillis(),
                likes = 0,
                tried = 0,
                rating = 0f,
                verified = false
            )
            
            // Store recipe
            recipeStorage[recipeId] = recipe
            
            // Update community recipes list
            val currentRecipes = _communityRecipes.value.toMutableList()
            currentRecipes.add(0, recipe)
            
            // Keep only recent recipes
            if (currentRecipes.size > MAX_COMMUNITY_RECIPES) {
                currentRecipes.removeAt(currentRecipes.size - 1)
            }
            
            _communityRecipes.value = currentRecipes
            
            recipeId
            
        } catch (e: Exception) {
            GuideErrorManager.instance.reportError(
                exception = e,
                context = "Recipe sharing failed",
                category = GuideErrorCategory.DATA_SYNC,
                severity = GuideErrorSeverity.ERROR
            )
            ""
        }
    }
    
    /**
     * Get ingredient substitutions for international recipes
     */
    suspend fun getZambianSubstitutions(
        internationalIngredient: String
    ): List<ZambianIngredientSubstitution> = withContext(Dispatchers.IO) {
        
        val ingredient = internationalIngredient.lowercase().trim()
        
        // Return cached substitutions if available
        ingredientSubstitutions[ingredient]?.let { return@withContext it }
        
        // Generate substitutions
        val substitutions = generateSubstitutions(ingredient)
        
        // Cache for future use
        ingredientSubstitutions[ingredient] = substitutions
        
        substitutions
    }
    
    /**
     * Search community recipes by various criteria
     */
    suspend fun searchRecipes(
        query: String = "",
        region: ZambianRegion? = null,
        difficulty: ZambianRecipeDifficulty? = null,
        maxCookingTime: Int? = null,
        tags: List<String> = emptyList(),
        onlyVerified: Boolean = false
    ): List<ZambianCommunityRecipe> = withContext(Dispatchers.IO) {
        
        val allRecipes = _communityRecipes.value
        
        allRecipes.filter { recipe ->
            // Text search
            val matchesQuery = query.isBlank() || 
                recipe.title.contains(query, ignoreCase = true) ||
                recipe.description.contains(query, ignoreCase = true) ||
                recipe.ingredients.any { it.contains(query, ignoreCase = true) }
            
            // Filter criteria
            val matchesRegion = region == null || recipe.region == region
            val matchesDifficulty = difficulty == null || recipe.difficulty == difficulty
            val matchesTime = maxCookingTime == null || recipe.cookingTimeMinutes <= maxCookingTime
            val matchesTags = tags.isEmpty() || tags.any { tag -> 
                recipe.tags.any { recipeTag -> recipeTag.contains(tag, ignoreCase = true) }
            }
            val matchesVerification = !onlyVerified || recipe.verified
            
            matchesQuery && matchesRegion && matchesDifficulty && 
            matchesTime && matchesTags && matchesVerification
        }.sortedByDescending { it.rating }
    }
    
    /**
     * Get seasonal recommendations based on current month
     */
    fun getCurrentSeasonalRecommendations(): List<ZambianSeasonalRecommendation> {
        val currentMonth = LocalDateTime.now().monthValue
        val currentSeason = getZambianSeason(currentMonth)
        
        return _seasonalRecommendations.value.filter { 
            it.season == currentSeason 
        }.sortedBy { it.priority }
    }
    
    /**
     * Rate a community recipe
     */
    suspend fun rateRecipe(recipeId: String, rating: Float) = withContext(Dispatchers.IO) {
        val recipe = recipeStorage[recipeId] ?: return@withContext
        
        // Update recipe rating (simplified - in production would aggregate all user ratings)
        val updatedRecipe = recipe.copy(
            rating = ((recipe.rating * recipe.ratingCount) + rating) / (recipe.ratingCount + 1),
            ratingCount = recipe.ratingCount + 1
        )
        
        recipeStorage[recipeId] = updatedRecipe
        updateCommunityRecipes()
    }
    
    /**
     * Mark recipe as tried
     */
    suspend fun markRecipeAsTried(recipeId: String) = withContext(Dispatchers.IO) {
        val recipe = recipeStorage[recipeId] ?: return@withContext
        
        val updatedRecipe = recipe.copy(tried = recipe.tried + 1)
        recipeStorage[recipeId] = updatedRecipe
        updateCommunityRecipes()
    }
    
    /**
     * Like/unlike a recipe
     */
    suspend fun toggleRecipeLike(recipeId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        val recipe = recipeStorage[recipeId] ?: return@withContext
        
        val updatedRecipe = recipe.copy(
            likes = if (isLiked) recipe.likes + 1 else maxOf(0, recipe.likes - 1)
        )
        
        recipeStorage[recipeId] = updatedRecipe
        updateCommunityRecipes()
    }
    
    /**
     * Get recipes by region
     */
    fun getRecipesByRegion(region: ZambianRegion): List<ZambianCommunityRecipe> {
        return _communityRecipes.value.filter { it.region == region }
    }
    
    /**
     * Get trending recipes
     */
    fun getTrendingRecipes(limit: Int = 10): List<ZambianCommunityRecipe> {
        return _communityRecipes.value
            .sortedByDescending { it.likes + (it.tried * 0.5) + (it.rating * 2) }
            .take(limit)
    }
    
    /**
     * Submit a community tip
     */
    suspend fun submitCommunityTip(
        title: String,
        description: String,
        category: ZambianTipCategory,
        authorName: String = "Anonymous"
    ): String = withContext(Dispatchers.IO) {
        
        val tipId = generateTipId()
        val tip = ZambianCommunityTip(
            id = tipId,
            title = title,
            description = description,
            category = category,
            authorName = authorName,
            createdAt = System.currentTimeMillis(),
            likes = 0,
            verified = false
        )
        
        val currentTips = _communityTips.value.toMutableList()
        currentTips.add(0, tip)
        _communityTips.value = currentTips
        
        tipId
    }
    
    // Private helper functions
    private fun initializeCommunityData() {
        communityScope.launch {
            loadDefaultRecipes()
            loadLocalIngredients()
            loadSeasonalRecommendations()
            loadFeaturedContent()
        }
    }
    
    private suspend fun loadDefaultRecipes() {
        val defaultRecipes = getDefaultZambianRecipes()
        defaultRecipes.forEach { recipe ->
            recipeStorage[recipe.id] = recipe
        }
        
        _communityRecipes.value = defaultRecipes
    }
    
    private suspend fun loadLocalIngredients() {
        _localIngredients.value = getZambianLocalIngredients()
    }
    
    private suspend fun loadSeasonalRecommendations() {
        _seasonalRecommendations.value = getZambianSeasonalData()
    }
    
    private suspend fun loadFeaturedContent() {
        _featuredContent.value = getZambianFeaturedContent()
    }
    
    private fun updateCommunityRecipes() {
        val updatedRecipes = recipeStorage.values.toList()
            .sortedByDescending { it.createdAt }
        
        _communityRecipes.value = updatedRecipes
    }
    
    private fun startPeriodicUpdates() {
        communityScope.launch {
            while (isActive) {
                delay(RECIPE_CACHE_DURATION_MS)
                // In production, this would sync with backend
                // For now, we'll refresh local data
                loadSeasonalRecommendations()
            }
        }
    }
    
    private fun generateRecipeId(): String = "recipe_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
    private fun generateTipId(): String = "tip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
    
    private fun getZambianSeason(month: Int): ZambianSeason {
        return when (month) {
            12, 1, 2 -> ZambianSeason.WET_SEASON
            3, 4, 5 -> ZambianSeason.HARVEST_SEASON
            6, 7, 8 -> ZambianSeason.DRY_SEASON
            9, 10, 11 -> ZambianSeason.PLANTING_SEASON
            else -> ZambianSeason.DRY_SEASON
        }
    }
    
    private fun generateSubstitutions(ingredient: String): List<ZambianIngredientSubstitution> {
        val substitutionMap = mapOf(
            "quinoa" to listOf("samp", "pearl millet"),
            "kale" to listOf("chibwabwa", "rape leaves"),
            "spinach" to listOf("bonongwe", "chibwabwa"),
            "eggplant" to listOf("impwa"),
            "okra" to listOf("delele"),
            "peanut butter" to listOf("groundnut paste"),
            "corn" to listOf("mealie meal", "fresh maize"),
            "beef stock" to listOf("kapenta stock", "chicken stock"),
            "heavy cream" to listOf("thick sour milk", "evaporated milk"),
            "parmesan cheese" to listOf("local white cheese", "processed cheese"),
            "basil" to listOf("local mint leaves", "lemon grass"),
            "mushrooms" to listOf("dried wild mushrooms", "fresh champignons"),
            "bell peppers" to listOf("green peppers", "hot peppers (less quantity)"),
            "zucchini" to listOf("pumpkin", "butternut squash"),
            "asparagus" to listOf("green beans", "fresh peas")
        )
        
        return substitutionMap[ingredient]?.map { zambianIngredient ->
            ZambianIngredientSubstitution(
                originalIngredient = ingredient,
                zambianAlternative = zambianIngredient,
                substitutionRatio = "1:1",
                notes = "Common Zambian substitute",
                availability = ZambianAvailability.YEAR_ROUND,
                cost = ZambianCostLevel.AFFORDABLE
            )
        } ?: emptyList()
    }
    
    // Default data providers
    private fun getDefaultZambianRecipes(): List<ZambianCommunityRecipe> {
        return listOf(
            ZambianCommunityRecipe(
                id = "nshima_001",
                title = "Traditional Nshima",
                description = "The staple food of Zambia - smooth, perfectly cooked nshima",
                ingredients = listOf(
                    "4 cups mealie meal (fine)",
                    "6 cups water",
                    "1 tsp salt (optional)"
                ),
                instructions = listOf(
                    "Boil 4 cups of water in a heavy-bottomed pot",
                    "Add 1 cup of mealie meal while stirring to avoid lumps",
                    "Cook for 10 minutes, stirring occasionally",
                    "Add remaining mealie meal gradually while stirring vigorously",
                    "Cook for 15-20 minutes until smooth and thick",
                    "Stir continuously to prevent burning",
                    "Serve hot with your favorite relish"
                ),
                region = ZambianRegion.NATIONWIDE,
                difficulty = ZambianRecipeDifficulty.INTERMEDIATE,
                cookingTimeMinutes = 35,
                servings = 6,
                tags = listOf("staple", "traditional", "gluten-free"),
                culturalNotes = "Nshima is eaten with hands, rolling it into a ball to scoop up relish. It's the foundation of every Zambian meal.",
                authorName = "Community Recipe",
                createdAt = System.currentTimeMillis() - 86400000, // Yesterday
                likes = 150,
                tried = 89,
                rating = 4.8f,
                ratingCount = 45,
                verified = true
            ),
            
            ZambianCommunityRecipe(
                id = "kapenta_001",
                title = "Kapenta with Tomatoes",
                description = "Delicious small fish cooked in a rich tomato sauce",
                ingredients = listOf(
                    "500g dried kapenta",
                    "3 large tomatoes, chopped",
                    "1 large onion, sliced",
                    "3 cloves garlic, minced",
                    "2 tbsp cooking oil",
                    "1 tsp curry powder",
                    "Salt and pepper to taste",
                    "1 green pepper (optional)"
                ),
                instructions = listOf(
                    "Soak kapenta in warm water for 30 minutes, then drain",
                    "Heat oil in a large pan over medium heat",
                    "Fry onions until golden brown",
                    "Add garlic and cook for 1 minute",
                    "Add tomatoes and cook until soft",
                    "Add kapenta and stir gently",
                    "Add curry powder, salt, and pepper",
                    "Cook for 10-15 minutes until kapenta is heated through",
                    "Serve with nshima and vegetables"
                ),
                region = ZambianRegion.NATIONWIDE,
                difficulty = ZambianRecipeDifficulty.EASY,
                cookingTimeMinutes = 25,
                servings = 4,
                tags = listOf("protein", "fish", "traditional", "quick"),
                culturalNotes = "Kapenta are small fish from Lake Tanganyika and Kariba. They're a vital protein source in Zambian cuisine.",
                authorName = "Mama Chanda",
                createdAt = System.currentTimeMillis() - 172800000, // 2 days ago
                likes = 98,
                tried = 67,
                rating = 4.6f,
                ratingCount = 32,
                verified = true
            ),
            
            ZambianCommunityRecipe(
                id = "chibwabwa_001",
                title = "Chibwabwa with Groundnuts",
                description = "Nutritious pumpkin leaves cooked with groundnut powder",
                ingredients = listOf(
                    "1 bunch fresh chibwabwa (pumpkin leaves)",
                    "1 cup groundnut powder",
                    "1 onion, chopped",
                    "2 tomatoes, chopped",
                    "2 tbsp cooking oil",
                    "1 tsp salt",
                    "1 cube chicken stock (optional)"
                ),
                instructions = listOf(
                    "Clean chibwabwa leaves thoroughly and chop roughly",
                    "Heat oil in a pot and fry onions until soft",
                    "Add tomatoes and cook until mushy",
                    "Add chopped chibwabwa and stir",
                    "Cover and cook for 5 minutes until leaves wilt",
                    "Mix groundnut powder with a little water to make paste",
                    "Add groundnut paste to the pot and mix well",
                    "Add salt and stock cube if using",
                    "Cook for 10 minutes, stirring occasionally",
                    "Serve with nshima"
                ),
                region = ZambianRegion.NATIONWIDE,
                difficulty = ZambianRecipeDifficulty.EASY,
                cookingTimeMinutes = 20,
                servings = 4,
                tags = listOf("vegetables", "healthy", "traditional", "groundnuts"),
                culturalNotes = "Chibwabwa is rich in vitamins and minerals. The groundnuts add protein and a creamy texture.",
                authorName = "Ba Mwanza",
                createdAt = System.currentTimeMillis() - 259200000, // 3 days ago
                likes = 76,
                tried = 54,
                rating = 4.5f,
                ratingCount = 28,
                verified = true
            )
        )
    }
    
    private fun getZambianLocalIngredients(): List<ZambianLocalIngredient> {
        return listOf(
            ZambianLocalIngredient(
                name = "Mealie Meal",
                localNames = listOf("Ubwali", "Nshima flour"),
                description = "Finely ground white corn flour, the base for Zambia's staple food",
                season = ZambianSeason.HARVEST_SEASON,
                availability = ZambianAvailability.YEAR_ROUND,
                regions = listOf(ZambianRegion.NATIONWIDE),
                nutritionalInfo = "High in carbohydrates, provides energy",
                cookingTips = "Use fine grade for smooth nshima, coarse for porridge",
                cost = ZambianCostLevel.AFFORDABLE
            ),
            ZambianLocalIngredient(
                name = "Kapenta",
                localNames = listOf("Utupele"),
                description = "Small dried fish from Lake Tanganyika and Kariba",
                season = ZambianSeason.DRY_SEASON,
                availability = ZambianAvailability.YEAR_ROUND,
                regions = listOf(ZambianRegion.NORTHERN, ZambianRegion.SOUTHERN, ZambianRegion.CENTRAL),
                nutritionalInfo = "Rich in protein, calcium, and omega-3 fatty acids",
                cookingTips = "Soak before cooking to reduce saltiness",
                cost = ZambianCostLevel.MODERATE
            ),
            ZambianLocalIngredient(
                name = "Chibwabwa",
                localNames = listOf("Pumpkin leaves", "Mubvumira"),
                description = "Tender pumpkin leaves, rich in vitamins",
                season = ZambianSeason.WET_SEASON,
                availability = ZambianAvailability.SEASONAL,
                regions = listOf(ZambianRegion.NATIONWIDE),
                nutritionalInfo = "High in vitamins A, C, iron, and calcium",
                cookingTips = "Use young tender leaves, remove tough stems",
                cost = ZambianCostLevel.CHEAP
            )
        )
    }
    
    private fun getZambianSeasonalData(): List<ZambianSeasonalRecommendation> {
        return listOf(
            ZambianSeasonalRecommendation(
                season = ZambianSeason.WET_SEASON,
                title = "Fresh Vegetable Season",
                description = "Enjoy abundant fresh vegetables like chibwabwa, bonongwe, and fresh maize",
                recommendedIngredients = listOf("Chibwabwa", "Fresh maize", "Bonongwe", "Tomatoes"),
                priority = 1
            ),
            ZambianSeasonalRecommendation(
                season = ZambianSeason.HARVEST_SEASON,
                title = "Groundnut Harvest",
                description = "Fresh groundnuts are available - perfect time for traditional recipes",
                recommendedIngredients = listOf("Fresh groundnuts", "New maize", "Sweet potatoes"),
                priority = 1
            ),
            ZambianSeasonalRecommendation(
                season = ZambianSeason.DRY_SEASON,
                title = "Preserve and Store",
                description = "Time to use preserved foods - dried kapenta, stored grains",
                recommendedIngredients = listOf("Dried kapenta", "Stored mealie meal", "Dried vegetables"),
                priority = 2
            )
        )
    }
    
    private fun getZambianFeaturedContent(): List<ZambianFeaturedContent> {
        return listOf(
            ZambianFeaturedContent(
                id = "feature_001",
                title = "Traditional Cooking Month",
                description = "Discover authentic Zambian recipes passed down through generations",
                contentType = ZambianContentType.RECIPE_COLLECTION,
                imageUrl = "",
                priority = 1,
                validUntil = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L) // 30 days
            )
        )
    }
    
    fun destroy() {
        communityScope.cancel()
    }
}

// Data classes and enums
@Serializable
data class ZambianCommunityRecipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val region: ZambianRegion,
    val difficulty: ZambianRecipeDifficulty,
    val cookingTimeMinutes: Int,
    val servings: Int,
    val tags: List<String>,
    val culturalNotes: String,
    val authorName: String,
    val createdAt: Long,
    val likes: Int = 0,
    val tried: Int = 0,
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val verified: Boolean = false,
    val imageUrl: String? = null
)

@Serializable
data class ZambianIngredientSubstitution(
    val originalIngredient: String,
    val zambianAlternative: String,
    val substitutionRatio: String,
    val notes: String,
    val availability: ZambianAvailability,
    val cost: ZambianCostLevel
)

@Serializable
data class ZambianLocalIngredient(
    val name: String,
    val localNames: List<String>,
    val description: String,
    val season: ZambianSeason,
    val availability: ZambianAvailability,
    val regions: List<ZambianRegion>,
    val nutritionalInfo: String,
    val cookingTips: String,
    val cost: ZambianCostLevel
)

@Serializable
data class ZambianSeasonalRecommendation(
    val season: ZambianSeason,
    val title: String,
    val description: String,
    val recommendedIngredients: List<String>,
    val priority: Int
)

@Serializable
data class ZambianCommunityTip(
    val id: String,
    val title: String,
    val description: String,
    val category: ZambianTipCategory,
    val authorName: String,
    val createdAt: Long,
    val likes: Int,
    val verified: Boolean
)

@Serializable
data class ZambianFeaturedContent(
    val id: String,
    val title: String,
    val description: String,
    val contentType: ZambianContentType,
    val imageUrl: String,
    val priority: Int,
    val validUntil: Long
)

enum class ZambianRegion {
    LUSAKA,
    COPPERBELT,
    SOUTHERN,
    EASTERN,
    CENTRAL,
    NORTHERN,
    LUAPULA,
    NORTH_WESTERN,
    WESTERN,
    MUCHINGA,
    NATIONWIDE
}

enum class ZambianRecipeDifficulty {
    EASY,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

enum class ZambianSeason {
    WET_SEASON,      // Dec-Mar
    HARVEST_SEASON,  // Mar-May  
    DRY_SEASON,      // Jun-Aug
    PLANTING_SEASON  // Sep-Nov
}

enum class ZambianAvailability {
    YEAR_ROUND,
    SEASONAL,
    RARE,
    IMPORT_ONLY
}

enum class ZambianCostLevel {
    CHEAP,
    AFFORDABLE,
    MODERATE,
    EXPENSIVE,
    LUXURY
}

enum class ZambianTipCategory {
    COOKING_TECHNIQUE,
    INGREDIENT_PREP,
    STORAGE,
    NUTRITION,
    CULTURAL,
    EQUIPMENT,
    SAFETY
}

enum class ZambianContentType {
    RECIPE_COLLECTION,
    COOKING_TIP,
    CULTURAL_STORY,
    SEASONAL_GUIDE,
    INGREDIENT_GUIDE
}

// Composable utilities
@Composable
fun rememberZambianCommunityRecipes(): State<List<ZambianCommunityRecipe>> {
    return ZambianCommunityHub.instance.communityRecipes.collectAsState()
}

@Composable
fun rememberZambianSeasonalRecommendations(): State<List<ZambianSeasonalRecommendation>> {
    return ZambianCommunityHub.instance.seasonalRecommendations.collectAsState()
}

@Composable
fun rememberZambianLocalIngredients(): State<List<ZambianLocalIngredient>> {
    return ZambianCommunityHub.instance.localIngredients.collectAsState()
}

@Composable
fun rememberZambianFeaturedContent(): State<List<ZambianFeaturedContent>> {
    return ZambianCommunityHub.instance.featuredContent.collectAsState()
}