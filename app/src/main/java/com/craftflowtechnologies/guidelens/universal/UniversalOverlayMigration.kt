package com.craftflowtechnologies.guidelens.universal

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.craftflowtechnologies.guidelens.cooking.EnhancedCookingSessionManager
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.ui.ThemeController
import com.craftflowtechnologies.guidelens.ai.*
import com.craftflowtechnologies.guidelens.credits.CreditsManager
import kotlinx.coroutines.CoroutineScope

/**
 * Migration example showing how to replace InteractiveCookingOverlay 
 * with UniversalArtifactOverlay for cooking artifacts
 */
@Composable
fun MigratedCookingOverlay(
    artifact: Artifact, // Must be a cooking artifact (ArtifactType.RECIPE)
    legacySessionManager: EnhancedCookingSessionManager,
    imageGenerator: ArtifactImageGenerator,
    onSendMessage: (String) -> Unit,
    onRequestImage: (String, Int) -> Unit,
    onCaptureProgress: () -> Unit,
    onBackPressed: () -> Unit = {},
    onDismiss: () -> Unit = {},
    isKeyboardVisible: Boolean = false,
    themeController: ThemeController,
    // Dependencies needed to create universal session manager
    artifactRepository: ArtifactRepository,
    contextManager: ArtifactContextManager,
    progressAnalysisSystem: ProgressAnalysisSystem,
    creditsManager: CreditsManager,
    coroutineScope: CoroutineScope
) {
    // Create universal session manager
    val universalSessionManager = remember {
        UniversalArtifactSessionManager(
            artifactRepository = artifactRepository,
            contextManager = contextManager,
            progressAnalysisSystem = progressAnalysisSystem,
            imageGenerator = imageGenerator,
            creditsManager = creditsManager,
            coroutineScope = coroutineScope
        )
    }
    
    val context = LocalContext.current
    
    // Start universal session when the overlay is first shown
    LaunchedEffect(artifact.id) {
        if (universalSessionManager.currentSession.value == null) {
            val result = universalSessionManager.startSession(
                userId = "current_user", // Replace with actual user ID
                artifact = artifact,
                environmentalContext = emptyMap()
            )
            
            if (result.isFailure) {
                // Handle error - could fallback to legacy session manager
                android.util.Log.e("UniversalOverlay", "Failed to start universal session", result.exceptionOrNull())
            }
        }
    }
    
    // Use the new universal overlay
    UniversalArtifactOverlay(
        artifact = artifact,
        sessionManager = universalSessionManager,
        imageGenerator = imageGenerator,
        onSendMessage = onSendMessage,
        onRequestImage = onRequestImage,
        onCaptureProgress = onCaptureProgress,
        onBackPressed = onBackPressed,
        onDismiss = onDismiss,
        isKeyboardVisible = isKeyboardVisible,
        themeController = themeController,
        context = context
    )
}

/**
 * Complete example of how to use UniversalArtifactOverlay for different agent types
 */
@Composable
fun UniversalOverlayExample(
    artifact: Artifact, // Any artifact type
    artifactRepository: ArtifactRepository,
    contextManager: ArtifactContextManager,
    progressAnalysisSystem: ProgressAnalysisSystem,
    imageGenerator: ArtifactImageGenerator,
    creditsManager: CreditsManager,
    coroutineScope: CoroutineScope,
    onSendMessage: (String) -> Unit,
    onRequestImage: (String, Int) -> Unit,
    onCaptureProgress: () -> Unit,
    onDismiss: () -> Unit,
    themeController: ThemeController
) {
    // Create universal session manager
    val universalSessionManager = remember {
        UniversalArtifactSessionManager(
            artifactRepository = artifactRepository,
            contextManager = contextManager,
            progressAnalysisSystem = progressAnalysisSystem,
            imageGenerator = imageGenerator,
            creditsManager = creditsManager,
            coroutineScope = coroutineScope
        )
    }
    
    val context = LocalContext.current
    
    // Start session
    LaunchedEffect(artifact.id) {
        universalSessionManager.startSession(
            userId = "current_user",
            artifact = artifact
        )
    }
    
    // The overlay automatically adapts to the artifact type
    UniversalArtifactOverlay(
        artifact = artifact,
        sessionManager = universalSessionManager,
        imageGenerator = imageGenerator,
        onSendMessage = { message ->
            // Enhanced message handling with agent context
            when (artifact.agentType) {
                "cooking" -> onSendMessage("🍳 Cooking: $message")
                "crafting" -> onSendMessage("🎨 Crafting: $message") 
                "diy" -> onSendMessage("🔨 DIY: $message")
                "buddy" -> onSendMessage("📚 Learning: $message")
                else -> onSendMessage(message)
            }
        },
        onRequestImage = { prompt, stepIndex ->
            // Agent-aware image generation
            val agentPrompt = when (artifact.agentType) {
                "cooking" -> "Cooking reference: $prompt"
                "crafting" -> "Crafting tutorial: $prompt"
                "diy" -> "DIY instruction: $prompt"
                "buddy" -> "Learning visual: $prompt"
                else -> prompt
            }
            onRequestImage(agentPrompt, stepIndex)
        },
        onCaptureProgress = onCaptureProgress,
        onDismiss = onDismiss,
        themeController = themeController,
        context = context
    )
}

/**
 * Demonstration of creating artifacts for different agent types
 */
object UniversalArtifactExamples {
    
    fun createCookingArtifact(): Artifact {
        val recipe = com.craftflowtechnologies.guidelens.cooking.Recipe(
            id = "recipe_001",
            title = "Classic Chocolate Chip Cookies",
            description = "Learn to make perfect chocolate chip cookies from scratch",
            prepTime = 15,
            cookTime = 12,
            servings = 24,
            difficulty = "Easy",
            ingredients = listOf(
                com.craftflowtechnologies.guidelens.cooking.Ingredient(
                    name = "All-purpose flour",
                    amount = "2¼",
                    unit = "cups"
                )
            ),
            steps = listOf(
                com.craftflowtechnologies.guidelens.cooking.CookingStep(
                    id = "1",
                    stepNumber = 1,
                    title = "Mix dry ingredients",
                    description = "Combine flour, baking soda, and salt in a bowl",
                    duration = 3,
                    techniques = listOf("mixing", "measuring"),
                    tips = listOf("Sift flour for better texture"),
                    visualCues = listOf("Ingredients are evenly combined")
                )
            )
        )
        
        return Artifact(
            type = ArtifactType.RECIPE,
            title = recipe.title,
            description = recipe.description,
            agentType = "cooking",
            userId = "current_user",
            contentData = ArtifactContent.RecipeContent(
                recipe = recipe
            ),
            estimatedDuration = recipe.prepTime + recipe.cookTime
        )
    }
    
    fun createCraftingArtifact(): Artifact {
        return Artifact(
            type = ArtifactType.CRAFT_PROJECT,
            title = "Handmade Greeting Cards",
            description = "Create beautiful personalized greeting cards",
            agentType = "crafting",
            userId = "current_user",
            contentData = ArtifactContent.CraftContent(
                materials = listOf(
                    Material(
                        name = "Cardstock",
                        amount = "5",
                        unit = "sheets",
                        category = "paper"
                    )
                ),
                tools = listOf(
                    Tool(
                        name = "Scissors",
                        required = true
                    )
                ),
                steps = listOf(
                    CraftStep(
                        stepNumber = 1,
                        title = "Prepare cardstock",
                        description = "Cut cardstock to size and fold",
                        duration = 10,
                        techniques = listOf("cutting", "folding")
                    )
                )
            ),
            difficulty = "Easy",
            estimatedDuration = 45
        )
    }
    
    fun createDIYArtifact(): Artifact {
        return Artifact(
            type = ArtifactType.DIY_GUIDE,
            title = "Install Floating Shelves",
            description = "Learn to safely install floating shelves",
            agentType = "diy", 
            userId = "current_user",
            contentData = ArtifactContent.DIYContent(
                materials = listOf(
                    Material(
                        name = "Floating shelf brackets",
                        amount = "2",
                        unit = "sets",
                        category = "hardware"
                    )
                ),
                tools = listOf(
                    Tool(
                        name = "Drill",
                        required = true,
                        safetyNotes = listOf("Always wear safety glasses")
                    )
                ),
                steps = listOf(
                    DIYStep(
                        stepNumber = 1,
                        title = "Mark bracket locations",
                        description = "Use a level to mark where brackets will go",
                        duration = 15,
                        safetyWarnings = listOf("Check for electrical wires"),
                        techniques = listOf("measuring", "leveling")
                    )
                ),
                safetyRequirements = listOf("Safety glasses", "Dust mask")
            ),
            difficulty = "Medium",
            estimatedDuration = 90
        )
    }
    
    fun createLearningArtifact(): Artifact {
        return Artifact(
            type = ArtifactType.LEARNING_MODULE,
            title = "Introduction to Photography",
            description = "Learn the basics of digital photography",
            agentType = "buddy",
            userId = "current_user", 
            contentData = ArtifactContent.TutorialContent(
                modules = listOf(
                    LearningModule(
                        title = "Camera Basics",
                        description = "Understand camera settings and controls",
                        content = "Learn about aperture, shutter speed, and ISO",
                        estimatedDuration = 20,
                        exercises = listOf(
                            Exercise(
                                question = "What controls depth of field?",
                                type = ExerciseType.MULTIPLE_CHOICE,
                                options = listOf("Aperture", "Shutter speed", "ISO"),
                                correctAnswer = "Aperture"
                            )
                        )
                    )
                ),
                objectives = listOf("Understand basic camera controls"),
                prerequisites = listOf("Basic computer skills")
            ),
            difficulty = "Beginner",
            estimatedDuration = 60
        )
    }
}

/**
 * Usage example for integrating the universal overlay into existing screens
 */
@Composable
fun IntegrateUniversalOverlay(
    showOverlay: Boolean,
    currentArtifact: Artifact?,
    // All your existing dependencies...
    onDismissOverlay: () -> Unit
) {
    // Your existing screen content...
    
    // Show universal overlay when needed
    if (showOverlay && currentArtifact != null) {
        // The overlay will automatically adapt based on currentArtifact.type and currentArtifact.agentType
        UniversalOverlayExample(
            artifact = currentArtifact,
            // ... pass your existing dependencies
            onDismiss = onDismissOverlay,
            // ... other parameters
            artifactRepository = TODO(), // Replace with actual instances
            contextManager = TODO(),
            progressAnalysisSystem = TODO(),
            imageGenerator = TODO(),
            creditsManager = TODO(),
            coroutineScope = TODO(),
            onSendMessage = { /* Handle message */ },
            onRequestImage = { _, _ -> /* Handle image request */ },
            onCaptureProgress = { /* Handle progress capture */ },
            themeController = TODO()
        )
    }
}