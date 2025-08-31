package com.craftflowtechnologies.guidelens.crafting

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.*

class CraftingToolsManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _activeProject = MutableStateFlow<CraftingProjectSession?>(null)
    val activeProject: StateFlow<CraftingProjectSession?> = _activeProject.asStateFlow()
    
    private val _materialInventory = MutableStateFlow<Map<String, InventoryItem>>(emptyMap())
    val materialInventory: StateFlow<Map<String, InventoryItem>> = _materialInventory.asStateFlow()
    
    private val _projectHistory = MutableStateFlow<List<CompletedProject>>(emptyList())
    val projectHistory: StateFlow<List<CompletedProject>> = _projectHistory.asStateFlow()

    fun getCraftingTools(): List<CraftingTool> {
        return listOf(
            CraftingTool(
                id = "project_planner",
                name = "Project Planner",
                description = "Plan your crafting project step by step",
                icon = Icons.Default.Assignment,
                action = { planProject() },
                category = ToolCategory.PLANNING,
                estimatedTime = "10-15 minutes"
            ),
            CraftingTool(
                id = "material_calculator",
                name = "Material Calculator",
                description = "Calculate materials needed for your project",
                icon = Icons.Default.Calculate,
                action = { calculateMaterials() },
                category = ToolCategory.CALCULATION,
                estimatedTime = "5 minutes"
            ),
            CraftingTool(
                id = "color_palette",
                name = "Color Palette Generator",
                description = "Generate harmonious color combinations",
                icon = Icons.Default.Palette,
                action = { generateColorPalette() },
                category = ToolCategory.DESIGN,
                estimatedTime = "5-10 minutes"
            ),
            CraftingTool(
                id = "pattern_designer",
                name = "Pattern Designer",
                description = "Create and customize patterns",
                icon = Icons.Default.GridOn,
                action = { designPattern() },
                category = ToolCategory.DESIGN,
                estimatedTime = "15-30 minutes"
            ),
            CraftingTool(
                id = "cost_estimator",
                name = "Cost Estimator",
                description = "Estimate total project cost",
                icon = Icons.Default.AttachMoney,
                action = { estimateCost() },
                category = ToolCategory.BUDGETING,
                estimatedTime = "5 minutes"
            ),
            CraftingTool(
                id = "skill_assessor",
                name = "Skill Assessment",
                description = "Assess your crafting skill level",
                icon = Icons.Default.Assessment,
                action = { assessSkill() },
                category = ToolCategory.LEARNING,
                estimatedTime = "10 minutes"
            ),
            CraftingTool(
                id = "project_tracker",
                name = "Progress Tracker",
                description = "Track your project progress",
                icon = Icons.Default.Timeline,
                action = { trackProgress() },
                category = ToolCategory.MANAGEMENT,
                estimatedTime = "Ongoing"
            ),
            CraftingTool(
                id = "inspiration_board",
                name = "Inspiration Board",
                description = "Collect and organize project ideas",
                icon = Icons.Default.Collections,
                action = { createInspirationBoard() },
                category = ToolCategory.INSPIRATION,
                estimatedTime = "10-20 minutes"
            ),
            CraftingTool(
                id = "troubleshooter",
                name = "Problem Solver",
                description = "Get help with crafting problems",
                icon = Icons.Default.Help,
                action = { troubleshootProblem() },
                category = ToolCategory.SUPPORT,
                estimatedTime = "5-15 minutes"
            ),
            CraftingTool(
                id = "tutorial_finder",
                name = "Tutorial Finder",
                description = "Find tutorials for specific techniques",
                icon = Icons.Default.School,
                action = { findTutorials() },
                category = ToolCategory.LEARNING,
                estimatedTime = "Variable"
            )
        )
    }

    private suspend fun planProject(): CraftingActionResult {
        return try {
            val planningSteps = listOf(
                "Define your project goal and vision",
                "Choose your crafting medium and style",
                "Sketch your design or find reference images",
                "List required materials and tools",
                "Break down the project into manageable steps",
                "Set realistic timeline and milestones",
                "Prepare your workspace"
            )
            
            CraftingActionResult.Success(
                title = "Project Planning Complete!",
                description = "Here's your crafting project plan:",
                steps = planningSteps,
                tips = listOf(
                    "Start with a simple sketch or mood board",
                    "Always buy 10-15% extra materials",
                    "Consider your skill level when setting timeline"
                )
            )
        } catch (e: Exception) {
            CraftingActionResult.Error("Failed to create project plan: ${e.message}")
        }
    }

    private suspend fun calculateMaterials(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Material Calculator",
            description = "Let me help you calculate the materials needed:",
            steps = listOf(
                "Tell me your project dimensions",
                "Specify the materials you're using",
                "I'll calculate quantities with waste allowance",
                "Get a shopping list with alternatives"
            ),
            calculation = MaterialCalculation(
                baseQuantity = "Based on your project size",
                wasteAllowance = "15% extra recommended",
                alternatives = listOf("Alternative material suggestions will appear here")
            )
        )
    }

    private suspend fun generateColorPalette(): CraftingActionResult {
        val colorSchemes = listOf(
            ColorScheme("Warm Autumn", listOf("#D2691E", "#CD853F", "#F4A460", "#DEB887")),
            ColorScheme("Cool Ocean", listOf("#4682B4", "#5F9EA0", "#87CEEB", "#B0E0E6")),
            ColorScheme("Spring Fresh", listOf("#98FB98", "#90EE90", "#FFB6C1", "#F0E68C")),
            ColorScheme("Earth Tones", listOf("#8B4513", "#A0522D", "#CD853F", "#F4A460"))
        )
        
        return CraftingActionResult.Success(
            title = "Color Palette Generator",
            description = "Here are some beautiful color combinations:",
            colorPalettes = colorSchemes,
            tips = listOf(
                "Use the 60-30-10 rule for color distribution",
                "Consider the room where your project will be displayed",
                "Test color combinations with small samples first"
            )
        )
    }

    private suspend fun designPattern(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Pattern Designer",
            description = "Let's create your custom pattern:",
            steps = listOf(
                "Choose your base pattern type (geometric, floral, abstract)",
                "Set your repeat size and scale",
                "Select colors from your palette",
                "Preview and adjust the pattern",
                "Export pattern for your project"
            ),
            tips = listOf(
                "Start with simple repeating elements",
                "Consider the scale relative to your project size",
                "Test patterns on paper before final materials"
            )
        )
    }

    private suspend fun estimateCost(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Cost Estimator",
            description = "Let me help you budget your project:",
            steps = listOf(
                "List all materials needed",
                "Research current market prices",
                "Factor in tool costs if needed",
                "Add 20% contingency for unexpected expenses",
                "Compare prices from different suppliers"
            ),
            costBreakdown = CostBreakdown(
                materials = "60-70% of total cost",
                tools = "20-30% (if purchasing new)",
                miscellaneous = "10-15% contingency"
            )
        )
    }

    private suspend fun assessSkill(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Skill Assessment",
            description = "Let's evaluate your crafting abilities:",
            steps = listOf(
                "Rate your experience with basic techniques",
                "Identify your strongest crafting areas",
                "Recognize areas for improvement",
                "Get personalized project recommendations",
                "Create a learning plan for skill development"
            ),
            skillAreas = listOf(
                SkillArea("Basic Techniques", "Cutting, gluing, measuring"),
                SkillArea("Color Theory", "Understanding color relationships"),
                SkillArea("Design Principles", "Composition and balance"),
                SkillArea("Problem Solving", "Adapting when things go wrong")
            )
        )
    }

    private suspend fun trackProgress(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Progress Tracker",
            description = "Keep track of your crafting journey:",
            steps = listOf(
                "Set project milestones",
                "Track time spent on each phase",
                "Document challenges and solutions",
                "Take progress photos",
                "Celebrate completed milestones"
            ),
            tips = listOf(
                "Take photos at each major step",
                "Note any modifications you make",
                "Track time to improve future estimates"
            )
        )
    }

    private suspend fun createInspirationBoard(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Inspiration Board",
            description = "Collect and organize your creative ideas:",
            steps = listOf(
                "Gather images that inspire you",
                "Note color combinations you love",
                "Save interesting techniques or textures",
                "Organize by project type or style",
                "Create mood boards for future projects"
            ),
            tips = listOf(
                "Use Pinterest, Instagram, or photos for inspiration",
                "Don't just save - analyze what you like about each piece",
                "Mix different styles to create something unique"
            )
        )
    }

    private suspend fun troubleshootProblem(): CraftingActionResult {
        val commonProblems = listOf(
            TroubleshootingTip(
                problem = "Paint isn't adhering properly",
                solutions = listOf(
                    "Clean surface thoroughly before painting",
                    "Use appropriate primer for your material",
                    "Check if paint is compatible with surface"
                )
            ),
            TroubleshootingTip(
                problem = "Fabric is fraying too much",
                solutions = listOf(
                    "Use pinking shears for edges",
                    "Apply fray check solution",
                    "Consider serging or zigzag stitching edges"
                )
            ),
            TroubleshootingTip(
                problem = "Measurements don't add up",
                solutions = listOf(
                    "Double-check your measuring tools",
                    "Ensure you're measuring twice, cutting once",
                    "Account for seam allowances and material thickness"
                )
            )
        )
        
        return CraftingActionResult.Success(
            title = "Problem Solver",
            description = "Let's fix that crafting challenge:",
            troubleshootingTips = commonProblems,
            tips = listOf(
                "Take a break and come back with fresh eyes",
                "Ask the crafting community for advice",
                "Sometimes 'mistakes' lead to creative discoveries"
            )
        )
    }

    private suspend fun findTutorials(): CraftingActionResult {
        return CraftingActionResult.Success(
            title = "Tutorial Finder",
            description = "I'll help you find the perfect tutorials:",
            steps = listOf(
                "Tell me what technique you want to learn",
                "I'll suggest beginner-friendly tutorials",
                "Find video and written instructions",
                "Get practice project recommendations",
                "Track your learning progress"
            ),
            tips = listOf(
                "Start with short, focused tutorials",
                "Practice each technique several times",
                "Don't be afraid to pause and replay sections"
            )
        )
    }

    fun cleanup() {
        scope.cancel()
    }
}

// Data models for crafting tools
data class CraftingTool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val action: suspend () -> CraftingActionResult,
    val category: ToolCategory,
    val estimatedTime: String,
    val difficulty: String = "Easy",
    val prerequisites: List<String> = emptyList()
)

enum class ToolCategory {
    PLANNING, CALCULATION, DESIGN, BUDGETING, LEARNING, MANAGEMENT, INSPIRATION, SUPPORT
}

sealed class CraftingActionResult {
    data class Success(
        val title: String,
        val description: String,
        val steps: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val colorPalettes: List<ColorScheme> = emptyList(),
        val calculation: MaterialCalculation? = null,
        val costBreakdown: CostBreakdown? = null,
        val skillAreas: List<SkillArea> = emptyList(),
        val troubleshootingTips: List<TroubleshootingTip> = emptyList()
    ) : CraftingActionResult()
    
    data class Error(val message: String) : CraftingActionResult()
}

@Serializable
data class ColorScheme(
    val name: String,
    val colors: List<String> // Hex color codes
)

@Serializable
data class MaterialCalculation(
    val baseQuantity: String,
    val wasteAllowance: String,
    val alternatives: List<String>
)

@Serializable
data class CostBreakdown(
    val materials: String,
    val tools: String,
    val miscellaneous: String
)

@Serializable
data class SkillArea(
    val name: String,
    val description: String,
    val level: String = "Beginner" // Beginner, Intermediate, Advanced
)

@Serializable
data class TroubleshootingTip(
    val problem: String,
    val solutions: List<String>
)

@Serializable
data class CraftingProjectSession(
    val id: String = UUID.randomUUID().toString(),
    val projectName: String,
    val type: String,
    val startDate: Long = System.currentTimeMillis(),
    val estimatedCompletionDate: Long,
    val currentPhase: ProjectPhase = ProjectPhase.PLANNING,
    val progress: Float = 0f,
    val materials: List<ProjectMaterial>,
    val steps: List<ProjectStep>,
    val notes: MutableList<String> = mutableListOf(),
    val photos: MutableList<String> = mutableListOf(), // Base64 encoded images
    val timeSpent: Long = 0, // milliseconds
    val challenges: MutableList<String> = mutableListOf()
)

@Serializable
enum class ProjectPhase {
    PLANNING, GATHERING_MATERIALS, IN_PROGRESS, FINISHING, COMPLETED, PAUSED
}

@Serializable
data class ProjectMaterial(
    val name: String,
    val quantity: String,
    val cost: Float,
    val acquired: Boolean = false,
    val alternatives: List<String> = emptyList()
)

@Serializable
data class ProjectStep(
    val id: String = UUID.randomUUID().toString(),
    val stepNumber: Int,
    val title: String,
    val description: String,
    val estimatedTime: Int, // minutes
    val completed: Boolean = false,
    val photos: List<String> = emptyList(),
    val notes: String = ""
)

@Serializable
data class CompletedProject(
    val id: String,
    val name: String,
    val type: String,
    val completedDate: Long,
    val totalTimeSpent: Long,
    val finalCost: Float,
    val rating: Int, // 1-5 stars
    val photos: List<String>,
    val lessonsLearned: List<String>,
    val wouldMakeAgain: Boolean
)

@Serializable
data class InventoryItem(
    val name: String,
    val category: String,
    val quantity: Float,
    val unit: String,
    val costPerUnit: Float,
    val expirationDate: Long? = null, // For items like paints, glues
    val location: String = "Craft Room",
    val lastUsed: Long? = null
)