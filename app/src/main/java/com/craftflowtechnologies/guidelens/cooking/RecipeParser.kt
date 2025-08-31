package com.craftflowtechnologies.guidelens.cooking

import android.util.Log
import java.util.UUID

class RecipeParser {
    companion object {
        private const val TAG = "RecipeParser"
        
        fun parseRecipeFromText(text: String): Recipe? {
            try {
                // Look for recipe structure in markdown format
                val lines = text.lines()
                var title = ""
                val ingredients = mutableListOf<Ingredient>()
                val steps = mutableListOf<CookingStep>()
                var prepTime = 30 // default
                var cookTime = 30 // default
                var servings = 4 // default
                var difficulty = "Medium" // default
                var description = ""
                
                var currentSection = ""
                var stepNumber = 1
                
                for (line in lines) {
                    val trimmedLine = line.trim()
                    
                    when {
                        // Main title (# or ## header)
                        (trimmedLine.startsWith("# ") || trimmedLine.startsWith("## ")) && title.isEmpty() -> {
                            title = trimmedLine.removePrefix("## ").removePrefix("# ").trim()
                            Log.d(TAG, "Found title: $title")
                        }
                        
                        // Recipe title specifically (including **bold** titles)
                        (trimmedLine.contains("Recipe", ignoreCase = true) || 
                         trimmedLine.contains("Chicken", ignoreCase = true) ||
                         trimmedLine.contains("African", ignoreCase = true)) && 
                        (trimmedLine.startsWith("#") || trimmedLine.startsWith("**")) -> {
                            if (title.isEmpty()) {
                                title = trimmedLine
                                    .removePrefix("##").removePrefix("#").removePrefix("*")
                                    .replace("*", "").trim()
                                Log.d(TAG, "Found recipe title: $title")
                            }
                        }
                        
                        // Section headers
                        trimmedLine.startsWith("## Ingredients") || 
                        trimmedLine.contains("Ingredients:", ignoreCase = true) -> {
                            currentSection = "ingredients"
                            Log.d(TAG, "Entered ingredients section")
                        }
                        
                        trimmedLine.startsWith("## Instructions") || 
                        trimmedLine.startsWith("## Steps") ||
                        trimmedLine.contains("Instructions:", ignoreCase = true) -> {
                            currentSection = "instructions"
                            Log.d(TAG, "Entered instructions section")
                        }
                        
                        // Parse ingredients
                        currentSection == "ingredients" && 
                        (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ") || trimmedLine.startsWith("• ")) -> {
                            Log.d(TAG, "Found ingredient line: $trimmedLine")
                            val ingredient = parseIngredient(trimmedLine)
                            if (ingredient != null) {
                                ingredients.add(ingredient)
                                Log.d(TAG, "Added ingredient: ${ingredient.name}")
                            } else {
                                Log.d(TAG, "Failed to parse ingredient: $trimmedLine")
                            }
                        }
                        
                        // Parse numbered steps - handle various formats
                        currentSection == "instructions" && 
                        (trimmedLine.matches(Regex("^\\d+\\..*")) || 
                         trimmedLine.startsWith("**${stepNumber}.") ||
                         trimmedLine.matches(Regex("^\\d+\\.\\s*\\*\\*.*"))) -> {
                            Log.d(TAG, "Found step line: $trimmedLine")
                            val step = parseStep(trimmedLine, stepNumber)
                            if (step != null) {
                                steps.add(step)
                                stepNumber++
                                Log.d(TAG, "Added step $stepNumber: ${step.title}")
                            } else {
                                Log.d(TAG, "Failed to parse step: $trimmedLine")
                            }
                        }
                        
                        // Extract timing information
                        trimmedLine.contains("prep", ignoreCase = true) && 
                        trimmedLine.contains("min", ignoreCase = true) -> {
                            prepTime = extractTime(trimmedLine) ?: prepTime
                        }
                        
                        trimmedLine.contains("cook", ignoreCase = true) && 
                        trimmedLine.contains("min", ignoreCase = true) -> {
                            cookTime = extractTime(trimmedLine) ?: cookTime
                        }
                        
                        trimmedLine.contains("serves", ignoreCase = true) -> {
                            servings = extractServings(trimmedLine) ?: servings
                        }
                        
                        // Extract description (usually after title, before ingredients)
                        title.isNotEmpty() && currentSection.isEmpty() && 
                        trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") -> {
                            if (description.isEmpty()) {
                                description = trimmedLine
                            }
                        }
                    }
                }
                
                // Infer difficulty based on number of steps and cooking time
                difficulty = when {
                    steps.size <= 4 && cookTime <= 30 -> "Easy"
                    steps.size <= 8 && cookTime <= 60 -> "Medium"
                    else -> "Hard"
                }
                
                Log.d(TAG, "Recipe parsing debug: title='$title', ingredients=${ingredients.size}, steps=${steps.size}")
                Log.d(TAG, "Recipe content preview: ${text.take(200)}...")
                
                return if (title.isNotEmpty() && ingredients.isNotEmpty() && steps.isNotEmpty()) {
                    Recipe(
                        title = title,
                        description = description.ifEmpty { "Delicious homemade recipe" },
                        prepTime = prepTime,
                        cookTime = cookTime,
                        servings = servings,
                        difficulty = difficulty,
                        ingredients = ingredients,
                        steps = steps
                    )
                } else {
                    Log.d(TAG, "Recipe parsing failed: title=${title.isNotEmpty()}, ingredients=${ingredients.size}, steps=${steps.size}")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing recipe", e)
                return null
            }
        }
        
        private fun parseIngredient(line: String): Ingredient? {
            try {
                // Remove bullet point and clean up
                val cleanLine = line.removePrefix("* ").removePrefix("- ").removePrefix("• ").trim()
                
                // Pattern: "amount unit ingredient"
                // Examples: "2 cups flour", "1 large onion, diced", "1/2 tsp salt"
                val parts = cleanLine.split(" ", limit = 3)
                
                return when {
                    parts.size >= 3 -> {
                        val amount = parts[0]
                        val unit = parts[1]
                        val ingredient = parts.drop(2).joinToString(" ")
                        
                        // Check for prep instructions (after comma)
                        val (name, prepInstructions) = if (ingredient.contains(",")) {
                            val splitIngredient = ingredient.split(",", limit = 2)
                            splitIngredient[0].trim() to splitIngredient[1].trim()
                        } else {
                            ingredient to null
                        }
                        
                        Ingredient(
                            name = name,
                            amount = amount,
                            unit = unit,
                            prepInstructions = prepInstructions,
                            isOptional = cleanLine.contains("optional", ignoreCase = true)
                        )
                    }
                    
                    parts.size == 2 -> {
                        // Handle cases like "Salt and pepper to taste"
                        Ingredient(
                            name = cleanLine,
                            amount = "to taste",
                            unit = "",
                            isOptional = cleanLine.contains("optional", ignoreCase = true)
                        )
                    }
                    
                    else -> {
                        // Single ingredient without amount
                        Ingredient(
                            name = cleanLine,
                            amount = "",
                            unit = "",
                            isOptional = cleanLine.contains("optional", ignoreCase = true)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse ingredient: $line", e)
                return null
            }
        }
        
        private fun parseStep(line: String, stepNumber: Int): CookingStep? {
            try {
                // Clean up the line - handle various formatting patterns
                var cleanLine = line.trim()
                
                // Remove step number patterns
                cleanLine = cleanLine
                    .removePrefix("$stepNumber.")
                    .removePrefix("**$stepNumber.")
                    .removePrefix("$stepNumber. **")
                    .trim()
                
                // Clean up markdown formatting
                cleanLine = cleanLine
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1") // Remove bold formatting
                    .replace(Regex("\\[(.*?)\\]"), "") // Remove tip sections for now
                    .trim()
                
                // Extract title and description
                val (title, description) = if (cleanLine.contains(":")) {
                    val parts = cleanLine.split(":", limit = 2)
                    parts[0].trim() to parts[1].trim()
                } else {
                    // First sentence as title, rest as description
                    val sentences = cleanLine.split(". ")
                    if (sentences.size > 1) {
                        sentences[0] to sentences.drop(1).joinToString(". ")
                    } else {
                        cleanLine to cleanLine
                    }
                }
                
                // Extract timing information
                val duration = extractStepTime(description)
                
                // Extract temperature information
                val temperature = extractTemperature(description)
                
                // Extract techniques
                val techniques = extractTechniques(description)
                
                // Extract visual cues
                val visualCues = extractVisualCues(description)
                
                // Determine if critical step
                val criticalStep = description.contains("important", ignoreCase = true) ||
                        description.contains("critical", ignoreCase = true) ||
                        description.contains("careful", ignoreCase = true) ||
                        techniques.any { it in listOf("sear", "brown", "caramelize") }
                
                return CookingStep(
                    stepNumber = stepNumber,
                    title = title,
                    description = description,
                    duration = duration,
                    temperature = temperature,
                    techniques = techniques,
                    visualCues = visualCues,
                    criticalStep = criticalStep
                )
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse step: $line", e)
                return null
            }
        }

        private fun extractTime(text: String): Int? {
            val timeRegex = Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE)
            return timeRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
        
        private fun extractStepTime(text: String): Int? {
            // Look for time patterns in step descriptions
            val patterns = listOf(
                Regex("(\\d+)\\s*minutes?"),
                Regex("(\\d+)\\s*mins?"),
                Regex("for\\s+(\\d+)\\s*minutes?", RegexOption.IGNORE_CASE),
                Regex("cook\\s+for\\s+(\\d+)", RegexOption.IGNORE_CASE),
                Regex("simmer\\s+for\\s+(\\d+)", RegexOption.IGNORE_CASE)
            )
            
            for (pattern in patterns) {
                pattern.find(text)?.let { match ->
                    return match.groupValues[1].toIntOrNull()
                }
            }
            return null
        }
        
        private fun extractTemperature(text: String): String? {
            val tempPatterns = listOf(
                Regex("(\\d+)°?[CF]"),
                Regex("(medium|low|high)\\s+heat", RegexOption.IGNORE_CASE),
                Regex("(medium|low|high)-?(medium|low|high)?", RegexOption.IGNORE_CASE)
            )
            
            for (pattern in tempPatterns) {
                pattern.find(text)?.let { match ->
                    return match.value
                }
            }
            return null
        }
        
        private fun extractTechniques(text: String): List<String> {
            val commonTechniques = listOf(
                "sauté", "simmer", "boil", "fry", "bake", "roast", "grill", "steam",
                "sear", "brown", "caramelize", "reduce", "whisk", "fold", "knead",
                "marinate", "season", "garnish", "dice", "chop", "mince", "slice"
            )
            
            return commonTechniques.filter { technique ->
                text.contains(technique, ignoreCase = true)
            }
        }
        
        private fun extractVisualCues(text: String): List<String> {
            val commonCues = listOf(
                "golden brown", "bubbling", "translucent", "tender", "crispy",
                "caramelized", "fragrant", "thickened", "reduced", "melted",
                "softened", "wilted", "firm", "flaky", "glossy"
            )
            
            return commonCues.filter { cue ->
                text.contains(cue, ignoreCase = true)
            }
        }
        
        private fun extractServings(text: String): Int? {
            val servingsRegex = Regex("serves\\s+(\\d+)", RegexOption.IGNORE_CASE)
            return servingsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
        
        // Check if text contains a recipe
        fun containsRecipe(text: String): Boolean {
            val recipeIndicators = listOf(
                "ingredients:",
                "instructions:",
                "recipe",
                "## ingredients",
                "## instructions",
                "steps:",
                "directions:"
            )
            
            val hasIndicators = recipeIndicators.any { indicator ->
                text.contains(indicator, ignoreCase = true)
            }
            
            val hasNumberedSteps = text.contains(Regex("\\d+\\.\\s+\\w"))
            val hasBulletIngredients = text.contains(Regex("[*-]\\s+\\d"))
            
            return hasIndicators && (hasNumberedSteps || hasBulletIngredients)
        }
        
        // Test recipe parsing with debug output
        fun testRecipeParsing(text: String): Recipe? {
            Log.d(TAG, "=== TESTING RECIPE PARSING ===")
            Log.d(TAG, "Input text length: ${text.length}")
            Log.d(TAG, "Contains ingredients: ${text.contains("ingredients", ignoreCase = true)}")
            Log.d(TAG, "Contains instructions: ${text.contains("instructions", ignoreCase = true)}")
            Log.d(TAG, "Contains numbered steps: ${text.contains(Regex("\\d+\\."))}")
            Log.d(TAG, "Contains dash bullets: ${text.contains(Regex("-\\s+\\w"))}")
            
            val result = parseRecipeFromText(text)
            Log.d(TAG, "Parse result: ${if (result != null) "SUCCESS" else "FAILED"}")
            
            return result
        }
        
        // Create a sample recipe for testing
        fun createSampleRecipe(): Recipe {
            return Recipe(
                title = "Classic Pasta Marinara",
                description = "A simple and delicious pasta dish with homemade marinara sauce",
                prepTime = 15,
                cookTime = 25,
                servings = 4,
                difficulty = "Easy",
                ingredients = listOf(
                    Ingredient(name = "pasta", amount = "1", unit = "lb"),
                    Ingredient(name = "olive oil", amount = "2", unit = "tbsp"),
                    Ingredient(name = "garlic", amount = "3", unit = "cloves", prepInstructions = "minced"),
                    Ingredient(name = "crushed tomatoes", amount = "28", unit = "oz can"),
                    Ingredient(name = "salt", amount = "to taste", unit = ""),
                    Ingredient(name = "fresh basil", amount = "1/4", unit = "cup", prepInstructions = "chopped")
                ),
                steps = listOf(
                    CookingStep(
                        stepNumber = 1,
                        title = "Cook pasta",
                        description = "Bring a large pot of salted water to boil. Add pasta and cook according to package directions until al dente.",
                        duration = 10,
                        techniques = listOf("boil"),
                        visualCues = listOf("bubbling", "tender")
                    ),
                    CookingStep(
                        stepNumber = 2,
                        title = "Make sauce",
                        description = "Heat olive oil in a large pan over medium heat. Add minced garlic and sauté until fragrant, about 1 minute.",
                        duration = 1,
                        temperature = "medium heat",
                        techniques = listOf("sauté"),
                        visualCues = listOf("fragrant")
                    ),
                    CookingStep(
                        stepNumber = 3,
                        title = "Add tomatoes",
                        description = "Add crushed tomatoes, salt to taste. Simmer for 15 minutes until sauce thickens.",
                        duration = 15,
                        techniques = listOf("simmer"),
                        visualCues = listOf("thickened"),
                        criticalStep = true
                    ),
                    CookingStep(
                        stepNumber = 4,
                        title = "Combine and serve",
                        description = "Drain pasta and toss with sauce. Garnish with fresh basil and serve immediately.",
                        techniques = listOf("garnish"),
                        tips = listOf("Save some pasta water to adjust sauce consistency if needed")
                    )
                ),
                tips = listOf(
                    "Don't overcook the pasta - it should be firm to the bite",
                    "Fresh basil makes a big difference in flavor"
                )
            )
        }
    }
}