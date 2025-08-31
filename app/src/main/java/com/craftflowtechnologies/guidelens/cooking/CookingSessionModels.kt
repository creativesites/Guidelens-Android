package com.craftflowtechnologies.guidelens.cooking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import com.craftflowtechnologies.guidelens.formatting.NutritionData

@Serializable
data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val prepTime: Int, // minutes
    val cookTime: Int, // minutes
    val servings: Int,
    val difficulty: String, // Easy, Medium, Hard
    val ingredients: List<Ingredient>,
    val steps: List<CookingStep>,
    val tags: List<String> = emptyList(),
    val nutrition: NutritionInfo? = null,
    val nutritionData: NutritionData? = null, // Enhanced nutrition data for charts
    val tips: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(), // AI-generated or user-uploaded images
    val estimatedCost: Float? = null, // Estimated cost in local currency
    val cuisine: String? = null, // Italian, Mexican, etc.
    val mealType: String? = null // Breakfast, Lunch, Dinner, Snack
)

@Serializable
data class Ingredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: String,
    val unit: String,
    val isOptional: Boolean = false,
    val alternatives: List<String> = emptyList(),
    val prepInstructions: String? = null // "diced", "minced", etc.
)

@Serializable
data class CookingStep(
    val id: String = UUID.randomUUID().toString(),
    val stepNumber: Int,
    val title: String,
    val description: String,
    val duration: Int? = null, // minutes, null if no specific timing
    val temperature: String? = null, // "350°F", "medium heat", etc.
    val techniques: List<String> = emptyList(), // "sauté", "simmer", etc.
    val visualCues: List<String> = emptyList(), // "golden brown", "bubbling", etc.
    val tips: List<String> = emptyList(),
    val criticalStep: Boolean = false, // If this step is crucial for success
    val canSkip: Boolean = false,
    val simultaneousSteps: List<String> = emptyList(), // Step IDs that can be done at same time
    val requiredEquipment: List<String> = emptyList()
)

@Serializable
data class NutritionInfo(
    val calories: Int,
    val protein: String,
    val carbs: String,
    val fat: String,
    val fiber: String? = null,
    val sodium: String? = null
)

// Active cooking session state
@Serializable
data class CookingSession(
    val id: String = UUID.randomUUID().toString(),
    val recipe: Recipe,
    val startTime: Long = System.currentTimeMillis(),
    val currentStepIndex: Int = 0,
    val stepStates: MutableMap<String, StepState> = mutableMapOf(),
    val timers: MutableMap<String, TimerState> = mutableMapOf(),
    val userModifications: MutableList<UserModification> = mutableListOf(),
    val notes: MutableList<String> = mutableListOf(),
    val isActive: Boolean = true,
    val isPaused: Boolean = false,
    val completedSteps: MutableSet<String> = mutableSetOf()
)

@Serializable
data class StepState(
    val stepId: String,
    val status: StepStatus = StepStatus.PENDING,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val userNotes: String? = null,
    val needsHelp: Boolean = false,
    val modifications: List<String> = emptyList()
)

@Serializable
enum class StepStatus {
    PENDING, ACTIVE, PAUSED, COMPLETED, SKIPPED, NEEDS_HELP
}

@Serializable
data class TimerState(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val originalDuration: Long, // milliseconds
    val remainingTime: Long,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val stepId: String? = null,
    val type: TimerType = TimerType.COOKING
)

@Serializable
enum class TimerType {
    COOKING, PREP, RESTING, MARINATING, TOTAL_TIME
}

@Serializable
data class UserModification(
    val stepId: String,
    val type: ModificationType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class ModificationType {
    INGREDIENT_SUBSTITUTION, TIME_ADJUSTMENT, TEMPERATURE_CHANGE, TECHNIQUE_MODIFICATION, SKIPPED_STEP
}

// User preferences and learning
@Serializable
data class UserCookingProfile(
    val userId: String,
    val name: String,
    val preferredCuisines: List<String> = emptyList(),
    val skillLevel: String = "Beginner", // Beginner, Intermediate, Advanced
    val dietaryRestrictions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val favoriteIngredients: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val cookingHistory: List<CookingHistoryEntry> = emptyList(),
    val preferredMealTypes: List<String> = emptyList(), // breakfast, lunch, dinner, snacks
    val cookingGoals: List<String> = emptyList(), // healthy eating, quick meals, etc.
    val lastUpdated: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
)

@Serializable
data class CookingHistoryEntry(
    val recipeId: String,
    val recipeName: String,
    val completedAt: String,
    val rating: Int? = null, // 1-5 stars
    val notes: String? = null,
    val modifications: List<UserModification> = emptyList(),
    val cookingTime: Long, // actual time taken in milliseconds
    val difficulty: String,
    val success: Boolean = true
)

// UI States
@Serializable
data class CookingUIState(
    val currentPhase: CookingPhase = CookingPhase.OVERVIEW,
    val showIngredientChecklist: Boolean = false,
    val showNutritionInfo: Boolean = false,
    val showTips: Boolean = false,
    val activeTimers: List<TimerState> = emptyList(),
    val showHelpDialog: Boolean = false,
    val helpContext: String? = null,
    val voiceInstructionsEnabled: Boolean = true,
    val autoAdvance: Boolean = false,
    val showProgress: Boolean = true
)

@Serializable
enum class CookingPhase {
    OVERVIEW, INGREDIENTS_PREP, COOKING, COMPLETED, PAUSED
}

// Interactive elements
data class CookingAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: () -> Unit
)

// Predefined cooking actions
object CookingActions {
    fun getActionsForStep(step: CookingStep): List<CookingAction> {
        return buildList {
            if (step.duration != null) {
                add(
                    CookingAction(
                        id = "start_timer",
                        title = "Start Timer",
                        description = "Start ${step.duration} minute timer",
                        icon = Icons.Default.Timer,
                        action = { }
                    )
                )
            }
            
            add(
                CookingAction(
                    id = "need_help",
                    title = "Need Help",
                    description = "Get guidance for this step",
                    icon = Icons.Default.Help,
                    action = { }
                )
            )
            
            add(
                CookingAction(
                    id = "step_complete",
                    title = "Step Complete",
                    description = "Mark this step as done",
                    icon = Icons.Default.CheckCircle,
                    action = { }
                )
            )
            
            if (step.canSkip) {
                add(
                    CookingAction(
                        id = "skip_step",
                        title = "Skip Step",
                        description = "Skip this optional step",
                        icon = Icons.Default.SkipNext,
                        action = { }
                    )
                )
            }
            
            add(
                CookingAction(
                    id = "pause_cooking",
                    title = "Pause",
                    description = "Pause cooking session",
                    icon = Icons.Default.Pause,
                    action = { }
                )
            )
        }
    }
}

// Smart suggestions based on user behavior and context
@Serializable
data class SmartSuggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val confidence: Float, // 0.0 to 1.0
    val action: String? = null
)

@Serializable
enum class SuggestionType {
    INGREDIENT_SUBSTITUTION, TIME_ADJUSTMENT, TECHNIQUE_TIP, SAFETY_REMINDER, PERSONALIZED_TIP
}