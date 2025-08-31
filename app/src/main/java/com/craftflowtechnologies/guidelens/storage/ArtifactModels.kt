package com.craftflowtechnologies.guidelens.storage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import java.util.*
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.CookingStep
import com.craftflowtechnologies.guidelens.cooking.Ingredient

@Serializable
data class Artifact(
    val id: String = UUID.randomUUID().toString(),
    val type: ArtifactType,
    val title: String,
    val description: String,
    val agentType: String, // cooking, crafting, diy, buddy
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val tags: List<String> = emptyList(),
    
    // Content data
    val contentData: ArtifactContent,
    
    // Generated images for each stage
    val stageImages: List<StageImage> = emptyList(),
    val mainImage: GeneratedImage? = null, // Hero image showing final result
    
    // Usage and progress tracking
    val usageStats: ArtifactUsageStats = ArtifactUsageStats(),
    val currentProgress: ArtifactProgress? = null,
    
    // Offline availability
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null,
    val fileSizeBytes: Long = 0,
    
    // Sharing and collaboration
    val isShared: Boolean = false,
    val shareCode: String? = null,
    val originalArtifactId: String? = null, // If this is a variation
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val estimatedDuration: Int? = null, // minutes
    
    // AI generation metadata
    val generationMetadata: GenerationMetadata? = null
)

@Serializable
enum class ArtifactType {
    RECIPE,
    CRAFT_PROJECT,
    DIY_GUIDE,
    LEARNING_MODULE,
    SKILL_TUTORIAL
}

//@Serializable
//sealed class ArtifactContent {
//
//    @Serializable
//    data class TextContent(val text: String) : ArtifactContent()
//
//    @Serializable
//    data class RecipeContent(
//        val recipe: Recipe,
//        val variations: List<RecipeVariation> = emptyList(),
//        val shoppingList: List<ShoppingItem> = emptyList()
//    ) : ArtifactContent()
//
//    @Serializable
//    data class CraftContent(
//        val materials: List<Material>,
//        val tools: List<Tool>,
//        val steps: List<CraftStep>,
//        val techniques: List<String> = emptyList(),
//        val patterns: List<Pattern> = emptyList()
//    ) : ArtifactContent()
//
//    @Serializable
//    data class DIYContent(
//        val materials: List<Material>,
//        val tools: List<Tool>,
//        val steps: List<DIYStep>,
//        val safetyRequirements: List<String> = emptyList(),
//        val skillsRequired: List<String> = emptyList()
//    ) : ArtifactContent()
//
//    @Serializable
//    data class TutorialContent(
//        val modules: List<LearningModule>,
//        val objectives: List<String> = emptyList(),
//        val prerequisites: List<String> = emptyList()
//    ) : ArtifactContent()
//}

@Serializable
data class StageImage(
    val stageNumber: Int,
    val stepId: String?,
    val image: GeneratedImage,
    val description: String,
    val isKeyMilestone: Boolean = false
)
//data class Recipe(
//    val title: String,
//    val description: String,
//    val prepTime: Int,
//    val cookTime: Int,
//    val servings: Int,
//    val difficulty: String,
//    val ingredients: List<Ingredient>,
//    val steps: List<CookingStep>
//)


@Serializable
data class GeneratedImage(
    val id: String = UUID.randomUUID().toString(),
    val url: String? = null, // Remote URL
    val localPath: String? = null, // Local cache path
    val prompt: String,
    val model: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val width: Int = 512,
    val height: Int = 512,
    val costCredits: Int = 1, // Credits used to generate
    val isDownloaded: Boolean = false
)

@Serializable
data class ArtifactUsageStats(
    val timesAccessed: Int = 0,
    val totalTimeSpent: Long = 0, // milliseconds
    val completionRate: Float = 0f, // 0.0 to 1.0
    val lastAccessedAt: Long? = null,
    val sessionsStarted: Int = 0,
    val sessionsCompleted: Int = 0,
    val userRating: Float? = null, // 1.0 to 5.0
    val bookmarked: Boolean = false
)

@Serializable
data class ArtifactProgress(
    val currentStageIndex: Int = 0,
    val completedStages: Set<String> = emptySet(),
    val sessionStartTime: Long? = null,
    val sessionPaused: Boolean = false,
    val userNotes: List<ProgressNote> = emptyList(),
    val stageStates: Map<String, StageState> = emptyMap(),
    val contextData: Map<String, String> = emptyMap() // AI context preservation
)

@Serializable
data class ProgressNote(
    val stageIndex: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null // User-captured progress photo
)

@Serializable
data class StageState(
    val status: StageStatus = StageStatus.PENDING,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val aiAnalysis: AIAnalysis? = null,
    val userFeedback: String? = null
)

@Serializable
enum class StageStatus {
    PENDING, ACTIVE, COMPLETED, NEEDS_HELP, REQUIRES_FEEDBACK
}

@Serializable
data class AIAnalysis(
    val confidenceScore: Float, // 0.0 to 1.0
    val feedback: String,
    val suggestions: List<String> = emptyList(),
    val adjustments: List<StageAdjustment> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class StageAdjustment(
    val type: AdjustmentType,
    val description: String,
    val newInstructions: String? = null
)

@Serializable
enum class AdjustmentType {
    TIME_EXTENSION, TECHNIQUE_MODIFICATION, INGREDIENT_SUBSTITUTION, 
    ADDITIONAL_STEP, SKIP_STEP, TEMPERATURE_CHANGE
}

@Serializable
data class GenerationMetadata(
    val model: String,
    val prompt: String,
    val generationTime: Long, // milliseconds
    val tokensUsed: Int,
    val estimatedCost: Float, // USD
    val qualityScore: Float? = null, // AI-assessed quality 0.0 to 1.0
    val userPromptContext: String? = null
)

// Supporting data classes
//@Serializable
//data class RecipeVariation(
//    val name: String,
//    val description: String,
//    val modifications: List<String>,
//    val difficultyChange: String? = null // easier, harder, same
//)

@Serializable
data class ShoppingItem(
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val category: String, // produce, dairy, meat, etc.
    val estimatedCost: Float? = null,
    val isOptional: Boolean = false,
    val alternatives: List<String> = emptyList()
)

@Serializable
data class Material(
    val name: String,
    val amount: String,
    val unit: String,
    val category: String,
    val estimatedCost: Float? = null,
    val alternatives: List<String> = emptyList(),
    val whereToFind: String? = null
)

@Serializable
data class Tool(
    val name: String,
    val required: Boolean = true,
    val alternatives: List<String> = emptyList(),
    val safetyNotes: List<String> = emptyList()
)

@Serializable
data class CraftStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val duration: Int? = null, // minutes
    val techniques: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val visualCues: List<String> = emptyList(),
    val toolsNeeded: List<String> = emptyList()
)

@Serializable
data class DIYStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val duration: Int? = null, // minutes
    val safetyWarnings: List<String> = emptyList(),
    val toolsNeeded: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val measurementNotes: List<String> = emptyList()
)

@Serializable
data class Pattern(
    val name: String,
    val description: String,
    val downloadUrl: String? = null,
    val localPath: String? = null
)

@Serializable
data class LearningModule(
    val title: String,
    val description: String,
    val content: String,
    val exercises: List<Exercise> = emptyList(),
    val estimatedDuration: Int // minutes
)

@Serializable
data class Exercise(
    val question: String,
    val type: ExerciseType,
    val options: List<String> = emptyList(), // For multiple choice
    val correctAnswer: String? = null,
    val explanation: String? = null
)

@Serializable
enum class ExerciseType {
    MULTIPLE_CHOICE, TRUE_FALSE, OPEN_ENDED, PRACTICAL_DEMO
}

// User limits and credits system
@Serializable
data class UserLimits(
    val userId: String,
    val tier: UserTier,
    val creditsRemaining: Int,
    val creditsUsedToday: Int = 0,
    val artifactsCreatedThisWeek: Int = 0,
    val imagesGeneratedToday: Int = 0,
    val resetDate: Long, // When daily limits reset
    val weeklyResetDate: Long // When weekly limits reset
)

@Serializable
enum class UserTier {
    FREE, BASIC, PRO
}

// Tier limits configuration
object TierLimits {
    data class TierConfig(
        val artifactsPerWeek: Int,
        val imagesPerArtifact: Int,
        val imagesPerDay: Int,
        val creditsPerMonth: Int,
        val canGenerateOnDemand: Boolean,
        val offlineStorage: Boolean,
        val priorityGeneration: Boolean
    )
    
    val limits = mapOf(
        UserTier.FREE to TierConfig(
            artifactsPerWeek = 3,
            imagesPerArtifact = 5,
            imagesPerDay = 10,
            creditsPerMonth = 50,
            canGenerateOnDemand = false,
            offlineStorage = false,
            priorityGeneration = false
        ),
        UserTier.BASIC to TierConfig(
            artifactsPerWeek = 15,
            imagesPerArtifact = 10,
            imagesPerDay = 50,
            creditsPerMonth = 250,
            canGenerateOnDemand = true,
            offlineStorage = true,
            priorityGeneration = false
        ),
        UserTier.PRO to TierConfig(
            artifactsPerWeek = -1, // unlimited
            imagesPerArtifact = 20,
            imagesPerDay = 200,
            creditsPerMonth = 1000,
            canGenerateOnDemand = true,
            offlineStorage = true,
            priorityGeneration = true
        )
    )
}