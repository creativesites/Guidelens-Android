package com.craftflowtechnologies.guidelens.examples

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.InvalidationTracker
import com.craftflowtechnologies.guidelens.ai.*
import com.craftflowtechnologies.guidelens.api.*
import com.craftflowtechnologies.guidelens.cooking.*
import com.craftflowtechnologies.guidelens.credits.CreditsManager
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import com.craftflowtechnologies.guidelens.storage.ArtifactDatabase
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel to integrate XAI with the new artifact system
 */
class XAIIntegrationViewModel(
    private val context: Context,
    private val xaiApiKey: String
) : ViewModel() {
    
    // Initialize components with XAI integration
    private val xaiClient = XAIClient(xaiApiKey)
    private val artifactRepository = ArtifactRepository(
        context = context,
        database = ArtifactDatabase.getDatabase(context),
        httpClient = okhttp3.OkHttpClient()
    )
    
    private val geminiClient = EnhancedGeminiClient(
        context = context,
        offlineModelManager = OfflineModelManager(context)
    )
    
    private val progressAnalysisSystem = ProgressAnalysisSystem(
        geminiClient = geminiClient,
        artifactRepository = artifactRepository
    )
    
    private val contextManager = ArtifactContextManager(artifactRepository, geminiClient)
    private val creditsManager = CreditsManager(artifactRepository)
    private val imageGenerator = ArtifactImageGenerator(
        xaiImageClient = XAIImageClient(context),
        artifactRepository = artifactRepository
    )
    
    private val enhancedSessionManager = EnhancedCookingSessionManager(
        artifactRepository = artifactRepository,
        contextManager = contextManager,
        progressAnalysisSystem = progressAnalysisSystem,
        imageGenerator = imageGenerator,
        creditsManager = creditsManager,
        coroutineScope = viewModelScope
    )
    
    // UI State
    var uiState by mutableStateOf(XAIIntegrationState())
        private set
    
    /**
     * Example: Create a new recipe artifact with multi-stage image generation
     */
    fun createRecipeArtifact(recipeName: String, userId: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, message = "Creating recipe...")
                
                // First, check for similar existing recipes
//                val similarArtifacts = contextManager.detectSimilarArtifacts(
//                    userId = userId,
//                    query = recipeName,
//                    artifactType = ArtifactType.RECIPE
//                )
//
//                if (similarArtifacts.isNotEmpty()) {
//                    val suggestions = contextManager.generateContextualSuggestions(
//                        userId = userId,
//                        query = recipeName,
//                        matches = similarArtifacts
//                    )
//
//                    uiState = uiState.copy(
//                        isLoading = false,
//                        message = "Found similar recipes!",
//                        suggestions = suggestions
//                    )
//                    return@launch
//                }
                
                // Generate new recipe using XAI
                val recipePrompt = """
                    Create a detailed recipe for $recipeName with:
                    - Clear ingredient list with measurements
                    - 6 step-by-step cooking instructions
                    - Cooking time and difficulty level
                    
                    Format as JSON with the recipe structure.
                """.trimIndent()
                
                val recipeResponse = xaiClient.generateStructuredResponse(
                    prompt = recipePrompt,
                    systemPrompt = "You are an expert chef creating detailed, easy-to-follow recipes."
                )
                
                if (recipeResponse.isSuccess) {
                    val recipeJson = recipeResponse.getOrNull()!!
                    val recipe = parseRecipeFromJson(recipeJson)
                    
                    // Create artifact
                    val artifact = createRecipeArtifact(recipe, userId)
                    
                    // Generate multi-stage images (7 images for 6-step recipe)
                    val imageResult = imageGenerator.generateRecipeImages(
                        recipe = recipe,
                        userId = userId,
                        includeAllSteps = true
                    )
                    
                    // Update artifact with generated images
                    imageGenerator.updateArtifactWithImages(
                        artifactId = artifact.id,
                        batchResult = imageResult,
                        userId = userId
                    )
                    
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Recipe created successfully with ${imageResult.successCount} images!",
                        currentArtifact = artifact,
                        generatedImages = imageResult.stageImages.size + (if (imageResult.mainImage != null) 1 else 0)
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Failed to generate recipe: ${recipeResponse.exceptionOrNull()?.message}",
                        error = recipeResponse.exceptionOrNull()?.message
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Error creating recipe: ${e.message}",
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Example: Start an enhanced cooking session with context awareness
     */
    fun startCookingSession(artifact: Artifact, userId: String) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, message = "Starting cooking session...")
                
                val sessionResult = enhancedSessionManager.startEnhancedSession(
                    userId = userId,
                    artifact = artifact,
                    environmentalContext = mapOf(
                        "kitchen_type" to "home",
                        "experience_level" to "intermediate",
                        "time_available" to "60_minutes"
                    )
                )
                
                if (sessionResult.isSuccess) {
                    val session = sessionResult.getOrNull()!!
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Cooking session started! I'll guide you through each step.",
                        currentSession = session,
                        sessionActive = true
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Failed to start session: ${sessionResult.exceptionOrNull()?.message}",
                        error = sessionResult.exceptionOrNull()?.message
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Error starting session: ${e.message}",
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Example: Process user message during cooking with full context
     */
    fun sendCookingMessage(message: String) {
        viewModelScope.launch {
            try {
                val response = enhancedSessionManager.processUserMessage(message)
                
                if (response.isSuccess) {
                    val aiResponse = response.getOrNull()!!
                    uiState = uiState.copy(
                        message = aiResponse,
                        conversationHistory = uiState.conversationHistory + listOf(
                            "User: $message",
                            "AI: $aiResponse"
                        )
                    )
                } else {
                    uiState = uiState.copy(
                        message = "I'm having trouble right now, but let's keep cooking!",
                        error = response.exceptionOrNull()?.message
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    message = "Communication error: ${e.message}",
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Example: Analyze cooking progress with camera feedback
     */
    fun analyzeProgress(imageUri: Uri, userNote: String? = null) {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, message = "Analyzing your progress...")
                
                val progressResult = enhancedSessionManager.processProgressImage(
                    imageUri = imageUri,
                    userNote = userNote
                )
                
                if (progressResult.isSuccess) {
                    val snapshot = progressResult.getOrNull()!!
                    val analysis = snapshot.aiAnalysis
                    
                    val feedbackMessage = when {
                        analysis?.confidenceScore ?: 0f > 0.8f -> 
                            "Excellent work! ${analysis?.feedback}"
                        analysis?.confidenceScore ?: 0f > 0.6f -> 
                            "Good progress! ${analysis?.feedback}"
                        else -> 
                            "Let me help you with this step. ${analysis?.feedback}"
                    }
                    
                    uiState = uiState.copy(
                        isLoading = false,
                        message = feedbackMessage,
                        lastAnalysis = analysis,
                        progressSnapshots = uiState.progressSnapshots + snapshot
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Couldn't analyze the image right now, but you're doing great!",
                        error = progressResult.exceptionOrNull()?.message
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Analysis error: ${e.message}",
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Example: Request on-demand image generation during cooking
     */
    fun requestHelpImage(prompt: String) {
        viewModelScope.launch {
            try {
                // Check credits first
                val session = uiState.currentSession
                if (session == null) {
                    uiState = uiState.copy(message = "No active cooking session")
                    return@launch
                }
                
                val canAfford = creditsManager.canAfford(
                    userId = session.userId,
                    feature = "on_demand_image"
                )
                
                if (!canAfford.getOrDefault(false)) {
                    uiState = uiState.copy(
                        message = "Not enough credits for image generation. You can continue cooking without images!"
                    )
                    return@launch
                }
                
                uiState = uiState.copy(isLoading = true, message = "Generating helpful image...")
                
                val imageResult = enhancedSessionManager.requestSessionImage(
                    prompt = prompt,
                    stageIndex = session.currentStageIndex
                )
                
                if (imageResult.isSuccess) {
                    val imageUrl = imageResult.getOrNull()!!
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Here's an image to help with this step!",
                        lastGeneratedImage = imageUrl
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        message = "Couldn't generate image right now, but I can still help with text guidance!",
                        error = imageResult.exceptionOrNull()?.message
                    )
                }
                
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Image generation error: ${e.message}",
                    error = e.message
                )
            }
        }
    }
    
    // Helper methods
    private fun parseRecipeFromJson(json: String): Recipe {
        // Simplified JSON parsing - in real implementation, use proper JSON deserialization
        return Recipe(
            title = "Sample Recipe",
            description = "A delicious sample recipe",
            prepTime = 15,
            cookTime = 30,
            servings = 4,
            difficulty = "Medium",
            ingredients = listOf(
                Ingredient(name = "Sample ingredient", amount = "1", unit = "cup")
            ),
            steps = listOf(
                CookingStep(
                    stepNumber = 1,
                    title = "Prepare ingredients",
                    description = "Get all ingredients ready",
                    duration = 5
                )
            )
        )
    }
    
    private fun createRecipeArtifact(recipe: Recipe, userId: String): Artifact {
        return Artifact(
            type = ArtifactType.RECIPE,
            title = recipe.title,
            description = recipe.description,
            agentType = "cooking",
            userId = userId,
            contentData = ArtifactContent.RecipeContent(
                recipe = recipe,
                variations = emptyList(),
                shoppingList = recipe.ingredients.map { ingredient ->
                    ShoppingItem(
                        ingredientName = ingredient.name,
                        amount = ingredient.amount,
                        unit = ingredient.unit,
                        category = "general"
                    )
                }
            )
        )
    }
}


/**
 * UI State for XAI Integration example
 */
data class XAIIntegrationState(
    val isLoading: Boolean = false,
    val message: String = "Ready to cook!",
    val error: String? = null,
    val currentArtifact: Artifact? = null,
    val currentSession: EnhancedCookingSession? = null,
    val sessionActive: Boolean = false,
    val suggestions: List<ContextualSuggestion> = emptyList(),
    val conversationHistory: List<String> = emptyList(),
    val progressSnapshots: List<ProgressSnapshot> = emptyList(),
    val lastAnalysis: AIAnalysis? = null,
    val lastGeneratedImage: String? = null,
    val generatedImages: Int = 0
)

/**
 * Factory for creating the ViewModel with dependencies
 */
class XAIIntegrationViewModelFactory(
    private val context: Context,
    private val xaiApiKey: String
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(XAIIntegrationViewModel::class.java)) {
            return XAIIntegrationViewModel(context, xaiApiKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Extension function for ArtifactDatabase
 */
//fun ArtifactDatabase.Companion.getDatabase(context: Context): ArtifactDatabase {
//    // This would typically be implemented with Room database builder
//    // For this example, we'll just throw a not implemented error
//    throw NotImplementedError("Database initialization not implemented in this example")
//}