package com.craftflowtechnologies.guidelens.storage

import com.craftflowtechnologies.guidelens.cooking.CookingStep
import com.craftflowtechnologies.guidelens.cooking.Ingredient
import kotlinx.serialization.Serializable
import com.craftflowtechnologies.guidelens.cooking.Recipe

@Serializable
sealed class ArtifactContent {
    @Serializable
    data class TextContent(val text: String) : ArtifactContent()

    @Serializable
    data class RecipeVariation(
        val name: String, // e.g., "Vegan Version"
        val description: String? = null, // short note
        val modifiedIngredients: List<Ingredient>? = null, // changed or replaced ingredients
        val modifiedSteps: List<CookingStep>? = null, // optional steps specific to this variation
        val prepTimeAdjustment: Int? = null, // in minutes
        val cookTimeAdjustment: Int? = null, // in minutes
        val tags: List<String> = emptyList() // e.g., ["low-carb", "high-protein"]
    )
    @Serializable
    data class RecipeContent(
        val recipe: Recipe,
        val variations: List<RecipeVariation> = emptyList(),
        val shoppingList: List<ShoppingItem> = emptyList()
    ) : ArtifactContent()
    
    @Serializable
    data class CraftContent(
        val materials: List<Material>,
        val tools: List<Tool>,
        val steps: List<CraftStep>,
        val techniques: List<String> = emptyList(),
        val patterns: List<Pattern> = emptyList()
    ) : ArtifactContent()
    
    @Serializable
    data class DIYContent(
        val materials: List<Material>,
        val tools: List<Tool>,
        val steps: List<DIYStep>,
        val safetyRequirements: List<String> = emptyList(),
        val skillsRequired: List<String> = emptyList()
    ) : ArtifactContent()
    
    @Serializable
    data class TutorialContent(
        val modules: List<LearningModule>,
        val objectives: List<String> = emptyList(),
        val prerequisites: List<String> = emptyList()
    ) : ArtifactContent()
}