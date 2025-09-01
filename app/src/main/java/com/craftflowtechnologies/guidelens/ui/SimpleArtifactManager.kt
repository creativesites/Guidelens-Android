package com.craftflowtechnologies.guidelens.ui

import android.content.Context
import android.util.Log
import com.craftflowtechnologies.guidelens.cooking.*
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Simplified artifact generation manager with enhanced parsing
 */
class SimpleArtifactManager(
    private val context: Context,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "SimpleArtifactManager"
    }
    
    private val enhancedParser = EnhancedArtifactParser()

    /**
     * Generate artifact from AI response using enhanced parsing
     */
    suspend fun generateArtifactFromResponse(
        response: String,
        requestType: String,
        agentId: String
    ): GenerationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Parsing AI response for artifact generation")
            
            val parseResult = enhancedParser.parseResponse(response, requestType)
            
            when (parseResult) {
                is ParseResult.Artifact -> {
                    val artifact = Artifact(
                        id = UUID.randomUUID().toString(),
                        title = parseResult.title,
                        description = "AI-generated ${requestType.replace("_", " ")} artifact",
                        agentType = agentId,
                        type = parseResult.type,
                        userId = "current_user",
                        contentData = parseResult.content,
                        generationMetadata = GenerationMetadata(
                            model = "gemini-2.5-flash",
                            prompt = "Generated from AI response",
                            generationTime = 1000L,
                            tokensUsed = response.length / 4, // Rough token estimate
                            estimatedCost = 0.01f,
                            userPromptContext = requestType
                        )
                    )
                    
                    val savedArtifact = artifactRepository.saveArtifact(artifact).getOrThrow()
                    GenerationResult.Success(savedArtifact, response)
                }
                is ParseResult.Text -> {
                    // Fallback to simple artifact generation if no structured data found
                    generateSimpleArtifact(
                        ArtifactRequest(
                            type = requestType,
                            agentId = agentId,
                            prompt = response,
                            parameters = emptyMap()
                        ),
                        response
                    )
                }
                is ParseResult.Error -> {
                    GenerationResult.Error(parseResult.message, Exception(parseResult.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating artifact from response", e)
            GenerationResult.Error("Failed to generate artifact: ${e.message}", e)
        }
    }

    /**
     * Generate simple artifact from request (fallback method)
     */
    suspend fun generateArtifact(
        request: ArtifactRequest,
        userInput: String = ""
    ): GenerationResult = generateSimpleArtifact(request, userInput)
    
    private suspend fun generateSimpleArtifact(
        request: ArtifactRequest,
        userInput: String = ""
    ): GenerationResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Generating artifact: ${request.type} for agent: ${request.agentId}")
            
            val enhancedPrompt = if (userInput.isNotEmpty()) userInput else request.prompt
            val artifactContent = createSimpleArtifactContent(request, enhancedPrompt)
            
            // Create artifact with proper ArtifactType enum
            val artifactType = when (request.type) {
                "recipe", "meal_plan" -> com.craftflowtechnologies.guidelens.storage.ArtifactType.RECIPE
                "craft_project" -> com.craftflowtechnologies.guidelens.storage.ArtifactType.CRAFT_PROJECT
                "diy_project" -> com.craftflowtechnologies.guidelens.storage.ArtifactType.DIY_GUIDE
                "learning_plan", "tutorial" -> com.craftflowtechnologies.guidelens.storage.ArtifactType.LEARNING_MODULE
                else -> com.craftflowtechnologies.guidelens.storage.ArtifactType.LEARNING_MODULE
            }
            
            val artifact = Artifact(
                id = UUID.randomUUID().toString(),
                title = extractSimpleTitle(enhancedPrompt, request.type),
                description = "Generated ${request.type.replace("_", " ")} artifact",
                agentType = request.agentId,
                type = artifactType,
                userId = "current_user", // TODO: Get from auth context
                contentData = artifactContent,
                generationMetadata = com.craftflowtechnologies.guidelens.storage.GenerationMetadata(
                    model = "gemini-2.5-flash",
                    prompt = enhancedPrompt,
                    generationTime = 1000L, // Mock generation time
                    tokensUsed = 1000,
                    estimatedCost = 0.01f,
                    userPromptContext = request.prompt
                )
            )
            
            val savedArtifact = try {
                artifactRepository.saveArtifact(artifact).getOrThrow()
            } catch (e: Exception) {
                throw Exception("Failed to save artifact: ${e.message}", e)
            }
            
            GenerationResult.Success(
                artifact = savedArtifact,
                generatedPrompt = enhancedPrompt
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating artifact", e)
            GenerationResult.Error(
                message = "Failed to generate artifact: ${e.message}",
                exception = e
            )
        }
    }
    
    /**
     * Create simple artifact content based on type
     */
    private fun createSimpleArtifactContent(
        request: ArtifactRequest,
        prompt: String
    ): ArtifactContent {
        return when (request.type) {
            "recipe" -> createSimpleRecipeContent(request, prompt)
            "meal_plan" -> createSimpleRecipeContent(request, prompt)
            "craft_project" -> createSimpleCraftContent(request, prompt)
            "diy_project" -> createSimpleDIYContent(request, prompt)
            "learning_plan", "tutorial" -> createSimpleTutorialContent(request, prompt)
            else -> ArtifactContent.TextContent(prompt)
        }
    }
    
    private fun createSimpleRecipeContent(
        request: ArtifactRequest,
        prompt: String
    ): ArtifactContent.RecipeContent {
        val recipeName = request.parameters["recipeName"] as? String 
            ?: extractSimpleTitle(prompt, "recipe")
            
        val servings = request.parameters["servings"] as? Int ?: 4
        val difficulty = request.parameters["difficulty"] as? String ?: "Medium"
        
        val recipe = Recipe(
            id = UUID.randomUUID().toString(),
            title = recipeName,
            description = "A delicious $recipeName recipe with step-by-step instructions",
            cuisine = "International",
            difficulty = difficulty,
            prepTime = 15,
            cookTime = 30,
            servings = servings,
            ingredients = createSimpleIngredients(),
            steps = createSimpleCookingSteps(),
            tags = listOf("easy", "delicious"),
            nutrition = createSimpleNutritionInfo(),
            tips = listOf(
                "Prep all ingredients before starting to cook",
                "Taste and adjust seasoning as needed"
            )
        )
        
        return ArtifactContent.RecipeContent(
            recipe = recipe,
            variations = listOf(
                ArtifactContent.RecipeVariation(
                    name = "Healthy Version",
                    description = "Lower calorie version"
                )
            ),
            shoppingList = createSimpleShoppingList(recipe.ingredients)
        )
    }
    
    private fun createSimpleCraftContent(
        request: ArtifactRequest,
        prompt: String
    ): ArtifactContent.CraftContent {
        return ArtifactContent.CraftContent(
            materials = listOf(
                com.craftflowtechnologies.guidelens.storage.Material(
                    name = "Basic craft materials",
                    amount = "1",
                    unit = "set",
                    category = "Primary",
                    estimatedCost = 15.99f
                )
            ),
            tools = listOf(
                com.craftflowtechnologies.guidelens.storage.Tool(
                    name = "Scissors",
                    required = true,
                    alternatives = listOf("Craft knife")
                )
            ),
            steps = listOf(
                com.craftflowtechnologies.guidelens.storage.CraftStep(
                    stepNumber = 1,
                    title = "Preparation",
                    description = "Gather all materials and prepare workspace",
                    duration = 15,
                    techniques = listOf("organizing")
                )
            ),
            techniques = listOf("cutting", "gluing"),
            patterns = listOf(
                com.craftflowtechnologies.guidelens.storage.Pattern(
                    name = "Basic template",
                    description = "Standard pattern"
                )
            )
        )
    }
    
    private fun createSimpleDIYContent(
        request: ArtifactRequest,
        prompt: String
    ): ArtifactContent.DIYContent {
        return ArtifactContent.DIYContent(
            materials = listOf(
                com.craftflowtechnologies.guidelens.storage.Material(
                    name = "Basic DIY materials",
                    amount = "1",
                    unit = "set",
                    category = "Primary",
                    estimatedCost = 25.99f
                )
            ),
            tools = listOf(
                com.craftflowtechnologies.guidelens.storage.Tool(
                    name = "Drill",
                    required = true,
                    alternatives = listOf("Screwdriver"),
                    safetyNotes = listOf("Wear safety glasses")
                )
            ),
            steps = listOf(
                com.craftflowtechnologies.guidelens.storage.DIYStep(
                    stepNumber = 1,
                    title = "Planning",
                    description = "Plan your project and take measurements",
                    duration = 30,
                    safetyWarnings = listOf("Measure twice, cut once")
                )
            ),
            safetyRequirements = listOf("Safety glasses", "Work gloves"),
            skillsRequired = listOf("measuring", "basic tool use")
        )
    }
    
    private fun createSimpleTutorialContent(
        request: ArtifactRequest,
        prompt: String
    ): ArtifactContent.TutorialContent {
        val topicName = request.parameters["topic"] as? String 
            ?: extractSimpleTitle(prompt, "learning topic")
            
        return ArtifactContent.TutorialContent(
            modules = listOf(
                com.craftflowtechnologies.guidelens.storage.LearningModule(
                    title = "Introduction to $topicName",
                    description = "Get familiar with the basics",
                    content = "Learn the fundamental concepts and get started with $topicName",
                    estimatedDuration = 60
                )
            ),
            objectives = listOf(
                "Understand the basics",
                "Apply concepts practically",
                "Build confidence"
            ),
            prerequisites = listOf("Basic understanding helpful but not required")
        )
    }
    
    // Helper functions
    private fun createSimpleIngredients(): List<Ingredient> {
        return listOf(
            Ingredient(
                id = UUID.randomUUID().toString(),
                name = "Main ingredient",
                amount = "1",
                unit = "cup",
                prepInstructions = "Prepare as needed"
            ),
            Ingredient(
                id = UUID.randomUUID().toString(),
                name = "Seasoning",
                amount = "1",
                unit = "tsp",
                prepInstructions = "To taste"
            )
        )
    }
    
    private fun createSimpleCookingSteps(): List<CookingStep> {
        return listOf(
            CookingStep(
                id = UUID.randomUUID().toString(),
                stepNumber = 1,
                title = "Preparation",
                description = "Prepare all ingredients",
                duration = 10,
                tips = listOf("Have everything ready")
            ),
            CookingStep(
                id = UUID.randomUUID().toString(),
                stepNumber = 2,
                title = "Cooking",
                description = "Cook according to recipe",
                duration = 20,
                tips = listOf("Monitor cooking progress")
            )
        )
    }
    
    private fun createSimpleNutritionInfo(): NutritionInfo {
        return NutritionInfo(
            calories = 300,
            protein = "20g",
            carbs = "30g",
            fat = "10g",
            fiber = "5g",
            sodium = "500mg"
        )
    }
    
    private fun createSimpleShoppingList(ingredients: List<Ingredient>): List<ShoppingItem> {
        return ingredients.map { ingredient ->
            ShoppingItem(
                ingredientName = ingredient.name,
                amount = ingredient.amount,
                unit = ingredient.unit,
                category = "General",
                estimatedCost = null,
                isOptional = ingredient.isOptional,
                alternatives = ingredient.alternatives
            )
        }
    }
    
    private fun extractSimpleTitle(prompt: String, fallbackPrefix: String): String {
        val words = prompt.split(" ").take(5)
        val title = words.joinToString(" ").replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        return if (title.isNotEmpty()) title else "$fallbackPrefix ${System.currentTimeMillis()}"
    }
}

/**
 * Simple result class
 */
sealed class GenerationResult {
    data class Success(
        val artifact: Artifact,
        val generatedPrompt: String
    ) : GenerationResult()
    
    data class Error(
        val message: String,
        val exception: Exception
    ) : GenerationResult()
}