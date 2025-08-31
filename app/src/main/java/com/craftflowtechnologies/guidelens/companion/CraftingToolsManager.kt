package com.craftflowtechnologies.guidelens.companion

import android.util.Log
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.media.GeminiImageGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

/**
 * Crafting Tools Manager - Handles craft project creation and management
 * Provides specialized tools for crafting projects and artifact generation
 */
class CraftingToolsManager(
    private val geminiClient: EnhancedGeminiClient,
    private val imageGenerator: GeminiImageGenerator,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "CraftingToolsManager"
        private val json = Json { ignoreUnknownKeys = true }
    }
    
    /**
     * Generate a complete craft project artifact from user description
     */
    suspend fun generateCraftProject(
        userId: String,
        projectDescription: String,
        skillLevel: String = "Beginner",
        timePreference: String = "1-3 hours",
        materials: List<String> = emptyList()
    ): Result<Artifact> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = buildCraftProjectPrompt(
                description = projectDescription,
                skillLevel = skillLevel,
                timePreference = timePreference,
                availableMaterials = materials
            )
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                contextData = mapOf(
                    "agent_type" to "crafting",
                    "skill_level" to skillLevel,
                    "time_preference" to timePreference,
                    "user_materials" to materials.joinToString(", ")
                ),
                agentType = "crafting"
            )
            
            if (response.isSuccess) {
                val craftProject = parseCraftProjectResponse(response.getOrNull() ?: "")
                val artifact = createCraftArtifact(userId, craftProject)
                
                // Generate step images
                val stageImages = imageGenerator.generateStepImages(artifact, maxImages = 4)
                val finalArtifact = artifact.copy(stageImages = stageImages)
                
                // Save artifact
                artifactRepository.saveArtifact(finalArtifact)
                
                Result.success(finalArtifact)
            } else {
                Result.failure(Exception("Failed to generate craft project: ${response.exceptionOrNull()?.message}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating craft project", e)
            Result.failure(e)
        }
    }
    
    /**
     * Color palette generator and matcher
     */
    suspend fun generateColorPalette(
        baseColors: List<String> = emptyList(),
        style: String = "harmonious",
        projectType: String = "general"
    ): Result<ColorPalette> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                As a professional color consultant, create a ${style} color palette for a ${projectType} craft project.
                ${if (baseColors.isNotEmpty()) "Incorporate these colors: ${baseColors.joinToString(", ")}" else ""}
                
                Provide the response in this JSON format:
                {
                  "palette_name": "descriptive name",
                  "colors": [
                    {
                      "name": "color name",
                      "hex": "#hex code",
                      "rgb": "rgb values",
                      "description": "when to use this color",
                      "emotion": "feeling this color evokes"
                    }
                  ],
                  "usage_tips": ["tip1", "tip2", "tip3"],
                  "harmony_type": "complementary/analogous/triadic/etc",
                  "best_for": ["project type 1", "project type 2"]
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "crafting"
            )
            
            if (response.isSuccess) {
                val paletteData = extractJsonFromResponse(response.getOrNull() ?: "")
                val colorPalette = json.decodeFromString<ColorPalette>(paletteData)
                Result.success(colorPalette)
            } else {
                Result.failure(Exception("Failed to generate color palette"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating color palette", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pattern generator and customizer
     */
    suspend fun generatePattern(
        patternType: String, // "geometric", "floral", "abstract", "textile"
        complexity: String = "medium",
        colors: List<String> = emptyList(),
        size: String = "standard"
    ): Result<PatternDesign> = withContext(Dispatchers.IO) {
        
        try {
            val colorPalette = if (colors.isEmpty()) {
                generateColorPalette(style = "balanced", projectType = patternType).getOrNull()
            } else {
                null
            }
            
            val finalColors = colors.ifEmpty { 
                colorPalette?.colors?.map { it.hex } ?: listOf("#333333", "#FFFFFF", "#FF6B6B")
            }
            
            val prompt = buildPatternPrompt(patternType, complexity, finalColors, size)
            
            // Generate pattern image
            val imageResult = imageGenerator.generateImage(
                prompt = prompt,
                agentType = "crafting"
            )
            
            val patternDesign = PatternDesign(
                id = UUID.randomUUID().toString(),
                name = "${patternType.capitalize()} Pattern",
                type = patternType,
                complexity = complexity,
                colors = finalColors,
                size = size,
                imageUrl = imageResult.image?.localPath,
                instructions = generatePatternInstructions(patternType, complexity),
                colorPalette = colorPalette
            )
            
            Result.success(patternDesign)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating pattern", e)
            Result.failure(e)
        }
    }
    
    /**
     * Material calculator and substitution suggestions
     */
    suspend fun calculateMaterials(
        projectType: String,
        dimensions: Map<String, Float>, // width, height, depth, etc.
        difficulty: String = "medium"
    ): Result<MaterialCalculation> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                As a craft materials expert, calculate the materials needed for a ${projectType} project.
                Dimensions: ${dimensions.entries.joinToString(", ") { "${it.key}: ${it.value}" }}
                Difficulty level: ${difficulty}
                
                Provide detailed material calculations including:
                1. Primary materials with quantities
                2. Tools required
                3. Alternative materials if primary ones are unavailable
                4. Cost estimation (budget ranges)
                5. Where to source materials
                
                Format as JSON:
                {
                  "materials": [
                    {
                      "name": "material name",
                      "quantity": "amount",
                      "unit": "unit of measurement",
                      "category": "category",
                      "essential": true/false,
                      "alternatives": ["alt1", "alt2"],
                      "estimated_cost": "price range",
                      "where_to_buy": ["store1", "store2"]
                    }
                  ],
                  "tools": [
                    {
                      "name": "tool name",
                      "essential": true/false,
                      "alternatives": ["alt tools"],
                      "rental_option": true/false
                    }
                  ],
                  "total_estimated_cost": "range",
                  "difficulty_notes": "specific considerations",
                  "waste_factor": "percentage to add for mistakes"
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "crafting"
            )
            
            if (response.isSuccess) {
                val calculationData = extractJsonFromResponse(response.getOrNull() ?: "")
                val materialCalculation = json.decodeFromString<MaterialCalculation>(calculationData)
                Result.success(materialCalculation)
            } else {
                Result.failure(Exception("Failed to calculate materials"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating materials", e)
            Result.failure(e)
        }
    }
    
    /**
     * Technique advisor and troubleshooter
     */
    suspend fun getTechniqueAdvice(
        technique: String,
        problem: String? = null,
        materialType: String? = null
    ): Result<TechniqueAdvice> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = if (problem != null) {
                """
                I'm having trouble with the ${technique} technique in my craft project.
                ${materialType?.let { "Working with: $it" } ?: ""}
                Problem: ${problem}
                
                Please provide:
                1. Diagnosis of what might be going wrong
                2. Step-by-step solution
                3. Prevention tips for future projects
                4. Alternative techniques if this one isn't working
                5. Visual cues to know when it's done correctly
                """
            } else {
                """
                Explain the ${technique} crafting technique in detail.
                ${materialType?.let { "Specifically for working with: $it" } ?: ""}
                
                Please cover:
                1. When and why to use this technique
                2. Tools and materials needed
                3. Step-by-step instructions
                4. Common mistakes to avoid
                5. Tips for mastery
                6. Variations of the technique
                """
            }.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "crafting"
            )
            
            if (response.isSuccess) {
                val advice = TechniqueAdvice(
                    technique = technique,
                    explanation = response.getOrNull() ?: "",
                    difficulty = assessTechniqueDifficulty(technique),
                    timeRequired = estimateTechniqueTime(technique),
                    toolsNeeded = extractToolsFromAdvice(response.getOrNull() ?: ""),
                    troubleshooting = problem != null
                )
                Result.success(advice)
            } else {
                Result.failure(Exception("Failed to get technique advice"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting technique advice", e)
            Result.failure(e)
        }
    }
    
    /**
     * Project customizer - modify existing craft projects
     */
    suspend fun customizeProject(
        originalArtifact: Artifact,
        customizations: Map<String, String>
    ): Result<Artifact> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                Modify this craft project based on the user's customization requests:
                
                Original Project: ${originalArtifact.title}
                Description: ${originalArtifact.description}
                
                Requested Changes:
                ${customizations.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
                
                Provide updated project details in the same structured format, maintaining the quality and feasibility of the original while incorporating the requested changes.
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "crafting"
            )
            
            if (response.isSuccess) {
                val customizedProject = parseCraftProjectResponse(response.getOrNull() ?: "")
                val customizedArtifact = createCraftArtifact(originalArtifact.userId, customizedProject)
                    .copy(
                        id = UUID.randomUUID().toString(),
                        originalArtifactId = originalArtifact.id,
                        title = "${originalArtifact.title} (Custom)",
                        tags = originalArtifact.tags + "customized"
                    )
                
                // Generate new step images for customized version
                val stageImages = imageGenerator.generateStepImages(customizedArtifact, maxImages = 4)
                val finalArtifact = customizedArtifact.copy(stageImages = stageImages)
                
                artifactRepository.saveArtifact(finalArtifact)
                Result.success(finalArtifact)
            } else {
                Result.failure(Exception("Failed to customize project"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error customizing project", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private fun buildCraftProjectPrompt(
        description: String,
        skillLevel: String,
        timePreference: String,
        availableMaterials: List<String>
    ): String {
        return """
            As the Crafting Guru in GuideLens, create a detailed craft project based on this description:
            
            Project Request: $description
            Skill Level: $skillLevel
            Time Available: $timePreference
            ${if (availableMaterials.isNotEmpty()) "Available Materials: ${availableMaterials.joinToString(", ")}" else ""}
            
            Create a comprehensive craft project with:
            
            1. PROJECT OVERVIEW:
            - Creative and engaging title
            - Inspiring description
            - Estimated time and difficulty
            - Final project dimensions/size
            
            2. MATERIALS LIST:
            - Specific materials with quantities
            - Tools required
            - Optional decorative elements
            - Alternative options for flexibility
            
            3. STEP-BY-STEP INSTRUCTIONS:
            - Clear, numbered steps
            - Techniques used in each step
            - Time estimates for major steps
            - Visual cues for completion
            - Pro tips for better results
            
            4. DESIGN VARIATIONS:
            - Color scheme options
            - Size variations
            - Style modifications
            - Personalization ideas
            
            5. TROUBLESHOOTING:
            - Common issues and solutions
            - How to fix mistakes
            - Quality checks
            
            Format the response as structured JSON that can be parsed into a craft project.
            Be creative, encouraging, and focus on achievable results that build confidence!
        """.trimIndent()
    }
    
    private fun buildPatternPrompt(
        patternType: String,
        complexity: String,
        colors: List<String>,
        size: String
    ): String {
        val colorDescription = colors.joinToString(", ")
        return """
            Create a ${complexity} complexity ${patternType} pattern design using colors: $colorDescription.
            The pattern should be ${size} sized, repeatable, and suitable for craft projects.
            Make it visually appealing with good balance and rhythm.
            High-resolution, crisp lines, perfect for printing or digital use.
        """.trimIndent()
    }
    
    private fun generatePatternInstructions(patternType: String, complexity: String): List<String> {
        return when (patternType.lowercase()) {
            "geometric" -> listOf(
                "Start with basic shapes as foundation",
                "Maintain consistent spacing between elements",
                "Use rulers or guides for precision",
                "Build complexity gradually",
                "Check alignment frequently"
            )
            "floral" -> listOf(
                "Begin with main flower motifs",
                "Add leaves and stems naturally",
                "Vary sizes for visual interest",
                "Connect elements with flowing lines",
                "Balance positive and negative space"
            )
            "textile" -> listOf(
                "Consider the weave pattern",
                "Plan color transitions carefully",
                "Test small sections first",
                "Maintain consistent tension",
                "Check pattern alignment regularly"
            )
            else -> listOf(
                "Plan your layout before starting",
                "Work from center outward",
                "Maintain consistency in elements",
                "Step back periodically to check overall balance",
                "Make adjustments as needed"
            )
        }
    }
    
    private fun parseCraftProjectResponse(response: String): CraftProject {
        return try {
            // Extract JSON from response if it's wrapped in text
            val jsonData = extractJsonFromResponse(response)
            json.decodeFromString<CraftProject>(jsonData)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured response, creating basic project", e)
            createBasicCraftProject(response)
        }
    }
    
    private fun createBasicCraftProject(response: String): CraftProject {
        return CraftProject(
            title = "Custom Craft Project",
            description = response.take(200),
            difficulty = "Medium",
            estimatedTime = 120,
            materials = extractMaterialsFromText(response),
            tools = extractToolsFromText(response),
            steps = extractStepsFromText(response),
            techniques = extractTechniquesFromText(response)
        )
    }
    
    private fun extractJsonFromResponse(response: String): String {
        val startIndex = response.indexOf("{")
        val endIndex = response.lastIndexOf("}") + 1
        
        return if (startIndex != -1 && endIndex > startIndex) {
            response.substring(startIndex, endIndex)
        } else {
            """{"title": "Craft Project", "description": "${response.take(100)}", "difficulty": "Medium"}"""
        }
    }
    
    private fun createCraftArtifact(userId: String, craftProject: CraftProject): Artifact {
        return Artifact(
            type = ArtifactType.CRAFT_PROJECT,
            title = craftProject.title,
            description = craftProject.description,
            agentType = "crafting",
            userId = userId,
            contentData = ArtifactContent.CraftContent(
                materials = craftProject.materials,
                tools = craftProject.tools,
                steps = craftProject.steps.mapIndexed { index, step ->
                    CraftStep(
                        stepNumber = index + 1,
                        title = step.title,
                        description = step.description,
                        duration = step.timeMinutes,
                        techniques = step.techniques,
                        tips = step.tips,
                        toolsNeeded = step.toolsNeeded
                    )
                },
                techniques = craftProject.techniques
            ),
            difficulty = craftProject.difficulty,
            estimatedDuration = craftProject.estimatedTime,
            tags = listOf("crafting", craftProject.difficulty.lowercase())
        )
    }
    
    // Text parsing helper methods
    private fun extractMaterialsFromText(text: String): List<Material> {
        // Simple extraction - in production, use more sophisticated parsing
        return listOf(
            Material(
                name = "Craft materials",
                amount = "as needed",
                unit = "various",
                category = "crafting"
            )
        )
    }
    
    private fun extractToolsFromText(text: String): List<Tool> {
        return listOf(
            Tool(name = "Basic craft tools", required = true)
        )
    }
    
    private fun extractStepsFromText(text: String): List<CraftProjectStep> {
        return listOf(
            CraftProjectStep(
                title = "Begin Crafting",
                description = text.take(100),
                timeMinutes = 30
            )
        )
    }
    
    private fun extractTechniquesFromText(text: String): List<String> {
        return listOf("basic crafting techniques")
    }
    
    private fun extractToolsFromAdvice(advice: String): List<String> {
        // Extract mentioned tools from advice text
        val toolKeywords = listOf("scissors", "needle", "brush", "knife", "ruler", "glue", "thread")
        return toolKeywords.filter { advice.contains(it, ignoreCase = true) }
    }
    
    private fun assessTechniqueDifficulty(technique: String): String {
        val basicTechniques = listOf("cutting", "gluing", "folding", "measuring")
        val advancedTechniques = listOf("weaving", "carving", "embossing", "marbling")
        
        return when {
            basicTechniques.any { technique.contains(it, ignoreCase = true) } -> "Beginner"
            advancedTechniques.any { technique.contains(it, ignoreCase = true) } -> "Advanced"
            else -> "Intermediate"
        }
    }
    
    private fun estimateTechniqueTime(technique: String): Int {
        // Return estimated minutes for technique
        return when {
            technique.contains("quick", ignoreCase = true) -> 15
            technique.contains("detailed", ignoreCase = true) -> 60
            else -> 30
        }
    }
}

// Data classes for crafting tools
@Serializable
data class CraftProject(
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedTime: Int,
    val materials: List<Material>,
    val tools: List<Tool>,
    val steps: List<CraftProjectStep>,
    val techniques: List<String> = emptyList(),
    val variations: List<String> = emptyList()
)

@Serializable
data class CraftProjectStep(
    val title: String,
    val description: String,
    val timeMinutes: Int? = null,
    val techniques: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val toolsNeeded: List<String> = emptyList()
)

@Serializable
data class ColorPalette(
    val palette_name: String,
    val colors: List<ColorInfo>,
    val usage_tips: List<String>,
    val harmony_type: String,
    val best_for: List<String>
)

@Serializable
data class ColorInfo(
    val name: String,
    val hex: String,
    val rgb: String,
    val description: String,
    val emotion: String
)

@Serializable
data class PatternDesign(
    val id: String,
    val name: String,
    val type: String,
    val complexity: String,
    val colors: List<String>,
    val size: String,
    val imageUrl: String?,
    val instructions: List<String>,
    val colorPalette: ColorPalette?
)

@Serializable
data class MaterialCalculation(
    val materials: List<MaterialInfo>,
    val tools: List<ToolInfo>,
    val total_estimated_cost: String,
    val difficulty_notes: String,
    val waste_factor: String
)

@Serializable
data class MaterialInfo(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val essential: Boolean,
    val alternatives: List<String>,
    val estimated_cost: String,
    val where_to_buy: List<String>
)

@Serializable
data class ToolInfo(
    val name: String,
    val essential: Boolean,
    val alternatives: List<String>,
    val rental_option: Boolean
)

@Serializable
data class TechniqueAdvice(
    val technique: String,
    val explanation: String,
    val difficulty: String,
    val timeRequired: Int,
    val toolsNeeded: List<String>,
    val troubleshooting: Boolean
)