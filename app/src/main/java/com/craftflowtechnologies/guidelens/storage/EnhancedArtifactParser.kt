package com.craftflowtechnologies.guidelens.storage

import android.util.Log
import com.craftflowtechnologies.guidelens.cooking.*
import java.util.*

/**
 * Enhanced artifact parser that can detect and parse various types of artifacts
 * from AI responses beyond just recipes
 */
class EnhancedArtifactParser {
    
    companion object {
        private const val TAG = "ArtifactParser"
        
        // Keywords that indicate different artifact types
        private val RECIPE_KEYWORDS = listOf("ingredients", "recipe", "cook", "preparation", "serves", "yield")
        private val CRAFT_KEYWORDS = listOf("materials", "craft", "supplies", "pattern", "template", "create", "make")
        private val DIY_KEYWORDS = listOf("tools", "install", "repair", "build", "fix", "maintenance", "project")
        private val TUTORIAL_KEYWORDS = listOf("steps", "learn", "tutorial", "guide", "lesson", "instructions")
        private val CHECKLIST_KEYWORDS = listOf("checklist", "todo", "tasks", "items", "list")
    }
    
    /**
     * Parse AI response and attempt to extract structured artifact data
     */
    fun parseResponse(response: String, requestType: String): ParseResult {
        return try {
            Log.d(TAG, "Parsing response for type: $requestType")
            
            when (requestType.lowercase()) {
                "recipe", "meal_plan", "cooking_technique" -> parseRecipeArtifact(response)
                "craft_project", "craft_pattern", "craft_technique", "craft_gift_guide" -> parseCraftArtifact(response)
                "diy_project", "repair_guide", "installation_guide", "maintenance_schedule" -> parseDIYArtifact(response)
                "learning_plan", "tutorial", "checklist", "resource_guide" -> parseTutorialArtifact(response)
                else -> detectAndParseArtifact(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing artifact", e)
            ParseResult.Error("Failed to parse response: ${e.message}")
        }
    }
    
    /**
     * Auto-detect artifact type and parse accordingly
     */
    private fun detectAndParseArtifact(response: String): ParseResult {
        val lowercaseResponse = response.lowercase()
        
        return when {
            containsKeywords(lowercaseResponse, RECIPE_KEYWORDS) -> parseRecipeArtifact(response)
            containsKeywords(lowercaseResponse, CRAFT_KEYWORDS) -> parseCraftArtifact(response)
            containsKeywords(lowercaseResponse, DIY_KEYWORDS) -> parseDIYArtifact(response)
            containsKeywords(lowercaseResponse, TUTORIAL_KEYWORDS) -> parseTutorialArtifact(response)
            containsKeywords(lowercaseResponse, CHECKLIST_KEYWORDS) -> parseChecklistArtifact(response)
            else -> ParseResult.Text(response)
        }
    }
    
    private fun containsKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    /**
     * Parse recipe-type artifacts
     */
    private fun parseRecipeArtifact(response: String): ParseResult {
        val title = extractTitle(response) ?: "Recipe Guide"
        val ingredients = extractIngredients(response)
        val steps = extractSteps(response)
        
        if (ingredients.isNotEmpty() || steps.isNotEmpty()) {
            val recipe = Recipe(
                id = UUID.randomUUID().toString(),
                title = title,
                description = "AI-generated recipe guide",
                cuisine = "International",
                difficulty = extractDifficulty(response),
                prepTime = extractTime(response, "prep"),
                cookTime = extractTime(response, "cook"),
                servings = extractServings(response),
                ingredients = ingredients,
                steps = steps,
                tags = extractTags(response),
                nutrition = null, // Could be enhanced later
                tips = extractTips(response)
            )
            
            val content = ArtifactContent.RecipeContent(
                recipe = recipe,
                variations = emptyList<ArtifactContent.RecipeVariation>(),
                shoppingList = ingredients.map { ingredient ->
                    ShoppingItem(
                        ingredientName = ingredient.name,
                        amount = ingredient.amount,
                        unit = ingredient.unit,
                        category = "General",
                        estimatedCost = null,
                        isOptional = ingredient.isOptional ?: false,
                        alternatives = ingredient.alternatives ?: emptyList()
                    )
                }
            )
            
            return ParseResult.Artifact(ArtifactType.RECIPE, title, content)
        }
        
        return ParseResult.Text(response)
    }
    
    /**
     * Parse craft project artifacts
     */
    private fun parseCraftArtifact(response: String): ParseResult {
        val title = extractTitle(response) ?: "Craft Project"
        val materials = extractMaterials(response)
        val tools = extractTools(response)
        val steps = extractCraftSteps(response)
        
        if (materials.isNotEmpty() || steps.isNotEmpty()) {
            val content = ArtifactContent.CraftContent(
                materials = materials,
                tools = tools,
                steps = steps,
                techniques = extractTechniques(response),
                patterns = extractPatterns(response)
            )
            
            return ParseResult.Artifact(ArtifactType.CRAFT_PROJECT, title, content)
        }
        
        return ParseResult.Text(response)
    }
    
    /**
     * Parse DIY project artifacts
     */
    private fun parseDIYArtifact(response: String): ParseResult {
        val title = extractTitle(response) ?: "DIY Project"
        val materials = extractMaterials(response)
        val tools = extractTools(response)
        val steps = extractDIYSteps(response)
        val safetyRequirements = extractSafetyRequirements(response)
        
        if (materials.isNotEmpty() || steps.isNotEmpty()) {
            val content = ArtifactContent.DIYContent(
                materials = materials,
                tools = tools,
                steps = steps,
                safetyRequirements = safetyRequirements,
                skillsRequired = extractSkills(response)
            )
            
            return ParseResult.Artifact(ArtifactType.DIY_GUIDE, title, content)
        }
        
        return ParseResult.Text(response)
    }
    
    /**
     * Parse tutorial/learning artifacts
     */
    private fun parseTutorialArtifact(response: String): ParseResult {
        val title = extractTitle(response) ?: "Learning Guide"
        val modules = extractLearningModules(response)
        val objectives = extractObjectives(response)
        
        if (modules.isNotEmpty() || objectives.isNotEmpty()) {
            val content = ArtifactContent.TutorialContent(
                modules = modules,
                objectives = objectives,
                prerequisites = extractPrerequisites(response)
            )
            
            return ParseResult.Artifact(ArtifactType.LEARNING_MODULE, title, content)
        }
        
        return ParseResult.Text(response)
    }
    
    /**
     * Parse checklist artifacts
     */
    private fun parseChecklistArtifact(response: String): ParseResult {
        val title = extractTitle(response) ?: "Task Checklist"
        val items = extractChecklistItems(response)
        
        if (items.isNotEmpty()) {
            // Convert to tutorial format with checkable steps
            val modules = listOf(
                LearningModule(
                    title = title,
                    description = "Organized task checklist",
                    content = items.joinToString("\n") { "☐ $it" },
                    estimatedDuration = items.size * 5 // 5 minutes per item
                )
            )
            
            val content = ArtifactContent.TutorialContent(
                modules = modules,
                objectives = listOf("Complete all tasks efficiently"),
                prerequisites = emptyList()
            )
            
            return ParseResult.Artifact(ArtifactType.LEARNING_MODULE, title, content)
        }
        
        return ParseResult.Text(response)
    }
    
    // Helper extraction methods
    private fun extractTitle(response: String): String? {
        val patterns = listOf(
            """(?i)^#\s*(.+)""".toRegex(),
            """(?i)^(\*\*)?(.+?)(\*\*)?\s*recipe""".toRegex(),
            """(?i)^(\*\*)?(.+?)(\*\*)?\s*guide""".toRegex(),
            """(?i)^(\*\*)?(.+?)(\*\*)?\s*project""".toRegex()
        )
        
        for (pattern in patterns) {
            pattern.find(response)?.let { match ->
                return match.groups[2]?.value?.trim() ?: match.groups[1]?.value?.trim()
            }
        }
        
        // Extract first line if it looks like a title
        val firstLine = response.lines().firstOrNull()?.trim()
        if (firstLine != null && firstLine.length < 100 && !firstLine.contains('.')) {
            return firstLine.removePrefix("#").removePrefix("**").removeSuffix("**").trim()
        }
        
        return null
    }
    
    private fun extractIngredients(response: String): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()
        val ingredientPattern = """(?i)(?:^|\n)[-•*]?\s*(\d+(?:\.\d+)?)\s*(\w+)?\s*(.+)""".toRegex()
        
        val lines = response.lines()
        var inIngredientSection = false
        
        for (line in lines) {
            if (line.lowercase().contains("ingredient")) {
                inIngredientSection = true
                continue
            }
            if (inIngredientSection && (line.lowercase().contains("instruction") || line.lowercase().contains("step"))) {
                break
            }
            
            if (inIngredientSection) {
                ingredientPattern.find(line)?.let { match ->
                    val amount = match.groups[1]?.value ?: "1"
                    val unit = match.groups[2]?.value ?: ""
                    val name = match.groups[3]?.value?.trim() ?: line.trim()
                    
                    ingredients.add(
                        Ingredient(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            amount = amount,
                            unit = unit,
                            prepInstructions = null
                        )
                    )
                }
            }
        }
        
        return ingredients
    }
    
    private fun extractSteps(response: String): List<CookingStep> {
        val steps = mutableListOf<CookingStep>()
        val stepPattern = """(?i)(?:^|\n)\s*(\d+[\.\)])\s*(.+)""".toRegex()
        
        val lines = response.lines()
        var inStepSection = false
        var stepNumber = 1
        
        for (line in lines) {
            if (line.lowercase().contains("instruction") || line.lowercase().contains("step") || line.lowercase().contains("method")) {
                inStepSection = true
                continue
            }
            
            if (inStepSection && line.trim().isNotEmpty()) {
                val cleanLine = line.trim()
                if (cleanLine.length > 10) { // Avoid parsing headers
                    steps.add(
                        CookingStep(
                            id = UUID.randomUUID().toString(),
                            stepNumber = stepNumber++,
                            title = "Step $stepNumber",
                            description = cleanLine.removePrefix("${stepNumber}.").removePrefix("${stepNumber})").trim(),
                            duration = null,
                            tips = emptyList()
                        )
                    )
                }
            }
        }
        
        return steps
    }
    
    private fun extractMaterials(response: String): List<Material> {
        val materials = mutableListOf<Material>()
        val lines = response.lines()
        var inMaterialSection = false
        
        for (line in lines) {
            if (line.lowercase().contains("material") || line.lowercase().contains("supplies")) {
                inMaterialSection = true
                continue
            }
            if (inMaterialSection && (line.lowercase().contains("tool") || line.lowercase().contains("step"))) {
                break
            }
            
            if (inMaterialSection && line.trim().startsWith("-") || line.trim().startsWith("•")) {
                val materialName = line.trim().removePrefix("-").removePrefix("•").trim()
                if (materialName.isNotEmpty()) {
                    materials.add(
                        Material(
                            name = materialName,
                            amount = "1",
                            unit = "piece",
                            category = "General",
                            estimatedCost = null
                        )
                    )
                }
            }
        }
        
        return materials
    }
    
    private fun extractTools(response: String): List<Tool> {
        val tools = mutableListOf<Tool>()
        val lines = response.lines()
        var inToolSection = false
        
        for (line in lines) {
            if (line.lowercase().contains("tool") || line.lowercase().contains("equipment")) {
                inToolSection = true
                continue
            }
            if (inToolSection && line.lowercase().contains("step")) {
                break
            }
            
            if (inToolSection && (line.trim().startsWith("-") || line.trim().startsWith("•"))) {
                val toolName = line.trim().removePrefix("-").removePrefix("•").trim()
                if (toolName.isNotEmpty()) {
                    tools.add(
                        Tool(
                            name = toolName,
                            required = true,
                            alternatives = emptyList()
                        )
                    )
                }
            }
        }
        
        return tools
    }
    
    private fun extractCraftSteps(response: String): List<CraftStep> {
        return extractGenericSteps(response).mapIndexed { index, step ->
            CraftStep(
                stepNumber = index + 1,
                title = "Step ${index + 1}",
                description = step,
                duration = null,
                techniques = emptyList()
            )
        }
    }
    
    private fun extractDIYSteps(response: String): List<DIYStep> {
        return extractGenericSteps(response).mapIndexed { index, step ->
            DIYStep(
                stepNumber = index + 1,
                title = "Step ${index + 1}",
                description = step,
                duration = null,
                safetyWarnings = extractSafetyFromStep(step)
            )
        }
    }
    
    private fun extractGenericSteps(response: String): List<String> {
        val steps = mutableListOf<String>()
        val lines = response.lines()
        var inStepSection = false
        
        for (line in lines) {
            if (line.lowercase().contains("step") || line.lowercase().contains("instruction")) {
                inStepSection = true
                continue
            }
            
            if (inStepSection && line.trim().isNotEmpty()) {
                val cleanLine = line.trim()
                if ((cleanLine.startsWith("-") || cleanLine.startsWith("•") || cleanLine.matches("""\d+\.\s*.*""".toRegex())) && cleanLine.length > 10) {
                    steps.add(cleanLine.removePrefix("-").removePrefix("•").replaceFirst("""\d+\.\s*""".toRegex(), "").trim())
                }
            }
        }
        
        return steps
    }
    
    private fun extractLearningModules(response: String): List<LearningModule> {
        val modules = mutableListOf<LearningModule>()
        val sections = response.split("""(?i)(module|lesson|chapter)\s*\d+""".toRegex())
        
        sections.forEachIndexed { index, section ->
            if (section.trim().length > 50) {
                modules.add(
                    LearningModule(
                        title = "Module ${index + 1}",
                        description = section.lines().firstOrNull()?.take(100) ?: "",
                        content = section.trim(),
                        estimatedDuration = estimateReadingTime(section)
                    )
                )
            }
        }
        
        return modules.ifEmpty {
            listOf(
                LearningModule(
                    title = "Learning Guide",
                    description = "Comprehensive guide",
                    content = response,
                    estimatedDuration = estimateReadingTime(response)
                )
            )
        }
    }
    
    private fun extractObjectives(response: String): List<String> {
        val objectives = mutableListOf<String>()
        val lines = response.lines()
        
        for (line in lines) {
            if (line.lowercase().contains("objective") || line.lowercase().contains("goal") || line.lowercase().contains("learn")) {
                val objective = line.trim().removePrefix("-").removePrefix("•").trim()
                if (objective.isNotEmpty() && objective.length > 10) {
                    objectives.add(objective)
                }
            }
        }
        
        return objectives.ifEmpty { listOf("Master the concepts and apply them practically") }
    }
    
    private fun extractChecklistItems(response: String): List<String> {
        val items = mutableListOf<String>()
        val lines = response.lines()
        
        for (line in lines) {
            val trimmed = line.trim()
            if ((trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("☐") || trimmed.matches("""\d+\.\s*.*""".toRegex())) && trimmed.length > 5) {
                val item = trimmed.removePrefix("-").removePrefix("•").removePrefix("☐").replaceFirst("""\d+\.\s*""".toRegex(), "").trim()
                if (item.isNotEmpty()) {
                    items.add(item)
                }
            }
        }
        
        return items
    }
    
    // Additional helper methods
    private fun extractDifficulty(response: String): String {
        return when {
            response.lowercase().contains("easy") || response.lowercase().contains("beginner") -> "Easy"
            response.lowercase().contains("hard") || response.lowercase().contains("advanced") -> "Hard"
            response.lowercase().contains("medium") || response.lowercase().contains("intermediate") -> "Medium"
            else -> "Medium"
        }
    }
    
    private fun extractTime(response: String, type: String): Int {
        val pattern = """(?i)${type}(?:\s+time)?\s*:?\s*(\d+)\s*(?:min|minute)""".toRegex()
        return pattern.find(response)?.groups?.get(1)?.value?.toIntOrNull() ?: 30
    }
    
    private fun extractServings(response: String): Int {
        val pattern = """(?i)(?:serves?|yield|portion)\s*:?\s*(\d+)""".toRegex()
        return pattern.find(response)?.groups?.get(1)?.value?.toIntOrNull() ?: 4
    }
    
    private fun extractTags(response: String): List<String> {
        val tags = mutableListOf<String>()
        if (response.lowercase().contains("vegetarian")) tags.add("vegetarian")
        if (response.lowercase().contains("vegan")) tags.add("vegan")
        if (response.lowercase().contains("gluten-free")) tags.add("gluten-free")
        if (response.lowercase().contains("quick") || response.lowercase().contains("fast")) tags.add("quick")
        return tags
    }
    
    private fun extractTips(response: String): List<String> {
        val tips = mutableListOf<String>()
        val lines = response.lines()
        
        for (line in lines) {
            if (line.lowercase().contains("tip") || line.lowercase().contains("note")) {
                val tip = line.trim().removePrefix("Tip:").removePrefix("Note:").trim()
                if (tip.isNotEmpty() && tip.length > 10) {
                    tips.add(tip)
                }
            }
        }
        
        return tips
    }
    
    private fun extractTechniques(response: String): List<String> {
        return listOf("basic crafting")
    }
    
    private fun extractPatterns(response: String): List<Pattern> {
        return emptyList()
    }
    
    private fun extractSafetyRequirements(response: String): List<String> {
        val safety = mutableListOf<String>()
        if (response.lowercase().contains("safety") || response.lowercase().contains("warning")) {
            safety.add("Follow all safety precautions")
        }
        return safety
    }
    
    private fun extractSafetyFromStep(step: String): List<String> {
        return if (step.lowercase().contains("careful") || step.lowercase().contains("warning")) {
            listOf("Exercise caution during this step")
        } else emptyList()
    }
    
    private fun extractSkills(response: String): List<String> {
        return listOf("basic skills")
    }
    
    private fun extractPrerequisites(response: String): List<String> {
        return listOf("No prerequisites required")
    }
    
    private fun estimateReadingTime(text: String): Int {
        return (text.split(" ").size / 200) * 60 // 200 words per minute
    }
}

/**
 * Result of parsing an AI response
 */
sealed class ParseResult {
    data class Artifact(
        val type: ArtifactType,
        val title: String,
        val content: ArtifactContent
    ) : ParseResult()
    
    data class Text(val content: String) : ParseResult()
    data class Error(val message: String) : ParseResult()
}