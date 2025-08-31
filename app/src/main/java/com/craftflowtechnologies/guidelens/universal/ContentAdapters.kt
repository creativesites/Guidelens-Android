package com.craftflowtechnologies.guidelens.universal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.CookingStep
import kotlinx.serialization.Serializable

/**
 * Base interface for content adapters that handle different artifact types
 */
interface ContentAdapter {
    val agentType: String
    val primaryColor: Color
    val secondaryColor: Color
    val icon: ImageVector
    
    fun getCurrentStepTitle(artifact: Artifact, stepIndex: Int): String
    fun getCurrentStepDescription(artifact: Artifact, stepIndex: Int): String
    fun getTotalSteps(artifact: Artifact): Int
    fun getStepDuration(artifact: Artifact, stepIndex: Int): Int?
    fun getStepTechniques(artifact: Artifact, stepIndex: Int): List<String>
    fun getStepTips(artifact: Artifact, stepIndex: Int): List<String>
    fun getStepVisualCues(artifact: Artifact, stepIndex: Int): List<String>
    fun getRequiredItems(artifact: Artifact, stepIndex: Int): List<String> // ingredients, materials, tools
    fun getStepSpecificData(artifact: Artifact, stepIndex: Int): Map<String, Any>
}

/**
 * Recipe/Cooking content adapter
 */
class RecipeContentAdapter : ContentAdapter {
    override val agentType = "cooking"
    override val primaryColor = Color(0xFF32D74B) // Green for cooking
    override val secondaryColor = Color(0xFFFF9500) // Orange for cooking
    override val icon = Icons.Rounded.Restaurant
    
    override fun getCurrentStepTitle(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.title ?: "Step ${stepIndex + 1}"
            }
            else -> "Step ${stepIndex + 1}"
        }
    }
    
    override fun getCurrentStepDescription(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.description ?: ""
            }
            else -> ""
        }
    }
    
    override fun getTotalSteps(artifact: Artifact): Int {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> content.recipe.steps.size
            else -> 0
        }
    }
    
    override fun getStepDuration(artifact: Artifact, stepIndex: Int): Int? {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.duration
            }
            else -> null
        }
    }
    
    override fun getStepTechniques(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.techniques ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepTips(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.tips ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepVisualCues(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stepIndex)?.visualCues ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getRequiredItems(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                val step = content.recipe.steps.getOrNull(stepIndex)
                step?.requiredEquipment ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepSpecificData(artifact: Artifact, stepIndex: Int): Map<String, Any> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                val step = content.recipe.steps.getOrNull(stepIndex)
                mapOf(
                    "temperature" to (step?.temperature ?: ""),
                    "criticalStep" to (step?.criticalStep ?: false),
                    "canSkip" to (step?.canSkip ?: false)
                )
            }
            else -> emptyMap()
        }
    }
}

/**
 * Crafting content adapter
 */
class CraftingContentAdapter : ContentAdapter {
    override val agentType = "crafting"
    override val primaryColor = Color(0xFFBF5AF2) // Purple for crafting
    override val secondaryColor = Color(0xFF00C7BE) // Teal for crafting
    override val icon = Icons.Rounded.Palette
    
    override fun getCurrentStepTitle(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.title ?: "Step ${stepIndex + 1}"
            }
            else -> "Step ${stepIndex + 1}"
        }
    }
    
    override fun getCurrentStepDescription(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.description ?: ""
            }
            else -> ""
        }
    }
    
    override fun getTotalSteps(artifact: Artifact): Int {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> content.steps.size
            else -> 0
        }
    }
    
    override fun getStepDuration(artifact: Artifact, stepIndex: Int): Int? {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.duration
            }
            else -> null
        }
    }
    
    override fun getStepTechniques(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.techniques ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepTips(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.tips ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepVisualCues(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                content.steps.getOrNull(stepIndex)?.visualCues ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getRequiredItems(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                val step = content.steps.getOrNull(stepIndex)
                step?.toolsNeeded ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepSpecificData(artifact: Artifact, stepIndex: Int): Map<String, Any> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.CraftContent -> {
                mapOf(
                    "materials" to content.materials.map { "${it.name} (${it.amount} ${it.unit})" },
                    "tools" to content.tools.map { it.name },
                    "techniques" to content.techniques
                )
            }
            else -> emptyMap()
        }
    }
}

/**
 * DIY content adapter
 */
class DIYContentAdapter : ContentAdapter {
    override val agentType = "diy"
    override val primaryColor = Color(0xFFFF9F0A) // Orange for DIY
    override val secondaryColor = Color(0xFF007AFF) // Blue for DIY
    override val icon = Icons.Rounded.Build
    
    override fun getCurrentStepTitle(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.title ?: "Step ${stepIndex + 1}"
            }
            else -> "Step ${stepIndex + 1}"
        }
    }
    
    override fun getCurrentStepDescription(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.description ?: ""
            }
            else -> ""
        }
    }
    
    override fun getTotalSteps(artifact: Artifact): Int {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> content.steps.size
            else -> 0
        }
    }
    
    override fun getStepDuration(artifact: Artifact, stepIndex: Int): Int? {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.duration
            }
            else -> null
        }
    }
    
    override fun getStepTechniques(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.techniques ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepTips(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.tips ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepVisualCues(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                content.steps.getOrNull(stepIndex)?.measurementNotes ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getRequiredItems(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                val step = content.steps.getOrNull(stepIndex)
                step?.toolsNeeded ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    override fun getStepSpecificData(artifact: Artifact, stepIndex: Int): Map<String, Any> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.DIYContent -> {
                val step = content.steps.getOrNull(stepIndex)
                mapOf(
                    "safetyWarnings" to (step?.safetyWarnings ?: emptyList()),
                    "measurementNotes" to (step?.measurementNotes ?: emptyList()),
                    "materials" to content.materials.map { "${it.name} (${it.amount} ${it.unit})" },
                    "tools" to content.tools.map { it.name },
                    "safetyRequirements" to content.safetyRequirements
                )
            }
            else -> emptyMap()
        }
    }
}

/**
 * Tutorial/Learning content adapter
 */
class TutorialContentAdapter : ContentAdapter {
    override val agentType = "buddy"
    override val primaryColor = Color(0xFF30D158) // Green for learning
    override val secondaryColor = Color(0xFF5856D6) // Purple for learning
    override val icon = Icons.Rounded.School
    
    override fun getCurrentStepTitle(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> {
                content.modules.getOrNull(stepIndex)?.title ?: "Module ${stepIndex + 1}"
            }
            else -> "Module ${stepIndex + 1}"
        }
    }
    
    override fun getCurrentStepDescription(artifact: Artifact, stepIndex: Int): String {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> {
                content.modules.getOrNull(stepIndex)?.description ?: ""
            }
            else -> ""
        }
    }
    
    override fun getTotalSteps(artifact: Artifact): Int {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> content.modules.size
            else -> 0
        }
    }
    
    override fun getStepDuration(artifact: Artifact, stepIndex: Int): Int? {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> {
                content.modules.getOrNull(stepIndex)?.estimatedDuration
            }
            else -> null
        }
    }
    
    override fun getStepTechniques(artifact: Artifact, stepIndex: Int): List<String> {
        // For tutorials, techniques would be learning methods
        return listOf("reading", "practice", "reflection")
    }
    
    override fun getStepTips(artifact: Artifact, stepIndex: Int): List<String> {
        return listOf(
            "Take your time to understand each concept",
            "Practice what you learn",
            "Don't hesitate to ask questions"
        )
    }
    
    override fun getStepVisualCues(artifact: Artifact, stepIndex: Int): List<String> {
        return listOf("Progress indicator", "Key concepts highlighted", "Interactive elements")
    }
    
    override fun getRequiredItems(artifact: Artifact, stepIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> {
                content.prerequisites
            }
            else -> emptyList()
        }
    }
    
    override fun getStepSpecificData(artifact: Artifact, stepIndex: Int): Map<String, Any> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.TutorialContent -> {
                val module = content.modules.getOrNull(stepIndex)
                mapOf(
                    "content" to (module?.content ?: ""),
                    "exercises" to (module?.exercises ?: emptyList()),
                    "objectives" to content.objectives,
                    "prerequisites" to content.prerequisites
                )
            }
            else -> emptyMap()
        }
    }
}

/**
 * Factory for creating content adapters
 */
object ContentAdapterFactory {
    fun createAdapter(artifact: Artifact): ContentAdapter {
        return when (artifact.type) {
            ArtifactType.RECIPE -> RecipeContentAdapter()
            ArtifactType.CRAFT_PROJECT -> CraftingContentAdapter()
            ArtifactType.DIY_GUIDE -> DIYContentAdapter()
            ArtifactType.LEARNING_MODULE, ArtifactType.SKILL_TUTORIAL -> TutorialContentAdapter()
        }
    }
    
    fun createAdapter(agentType: String): ContentAdapter {
        return when (agentType.lowercase()) {
            "cooking" -> RecipeContentAdapter()
            "crafting" -> CraftingContentAdapter()
            "diy" -> DIYContentAdapter()
            "buddy" -> TutorialContentAdapter()
            else -> RecipeContentAdapter() // Default fallback
        }
    }
}

/**
 * Universal step data structure for all content types
 */
@Serializable
data class UniversalStepData(
    val title: String,
    val description: String,
    val duration: Int?,
    val techniques: List<String>,
    val tips: List<String>,
    val visualCues: List<String>,
    val requiredItems: List<String>,
    val specificData: Map<String, String>
) {
    companion object {
        fun fromAdapter(adapter: ContentAdapter, artifact: Artifact, stepIndex: Int): UniversalStepData {
            val specificData = adapter.getStepSpecificData(artifact, stepIndex)
                .mapValues { it.value.toString() }
            
            return UniversalStepData(
                title = adapter.getCurrentStepTitle(artifact, stepIndex),
                description = adapter.getCurrentStepDescription(artifact, stepIndex),
                duration = adapter.getStepDuration(artifact, stepIndex),
                techniques = adapter.getStepTechniques(artifact, stepIndex),
                tips = adapter.getStepTips(artifact, stepIndex),
                visualCues = adapter.getStepVisualCues(artifact, stepIndex),
                requiredItems = adapter.getRequiredItems(artifact, stepIndex),
                specificData = specificData
            )
        }
    }
}