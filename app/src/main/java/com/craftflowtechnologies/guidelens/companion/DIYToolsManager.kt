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
 * DIY Tools Manager - Handles home improvement and repair project creation
 * Provides specialized tools for DIY projects with safety-first approach
 */
class DIYToolsManager(
    private val geminiClient: EnhancedGeminiClient,
    private val imageGenerator: GeminiImageGenerator,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "DIYToolsManager"
        private val json = Json { ignoreUnknownKeys = true }
    }
    
    /**
     * Generate a complete DIY project artifact from user description
     */
    suspend fun generateDIYProject(
        userId: String,
        projectDescription: String,
        skillLevel: String = "Beginner",
        budget: String = "Under $100",
        timeAvailable: String = "Weekend project",
        toolsAvailable: List<String> = emptyList()
    ): Result<Artifact> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = buildDIYProjectPrompt(
                description = projectDescription,
                skillLevel = skillLevel,
                budget = budget,
                timeAvailable = timeAvailable,
                availableTools = toolsAvailable
            )
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                contextData = mapOf(
                    "agent_type" to "diy",
                    "skill_level" to skillLevel,
                    "budget" to budget,
                    "time_available" to timeAvailable,
                    "available_tools" to toolsAvailable.joinToString(", ")
                ),
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val diyProject = parseDIYProjectResponse(response.getOrNull() ?: "")
                val artifact = createDIYArtifact(userId, diyProject)
                
                // Generate step images with safety focus
                val stageImages = imageGenerator.generateStepImages(artifact, maxImages = 4)
                val finalArtifact = artifact.copy(stageImages = stageImages)
                
                // Save artifact
                artifactRepository.saveArtifact(finalArtifact)
                
                Result.success(finalArtifact)
            } else {
                Result.failure(Exception("Failed to generate DIY project: ${response.exceptionOrNull()?.message}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating DIY project", e)
            Result.failure(e)
        }
    }
    
    /**
     * Safety assessment and requirement generator
     */
    suspend fun assessSafetyRequirements(
        projectType: String,
        tools: List<String>,
        materials: List<String>,
        workspace: String = "garage"
    ): Result<SafetyAssessment> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                As a safety expert, assess the safety requirements for this DIY project:
                
                Project Type: ${projectType}
                Tools: ${tools.joinToString(", ")}
                Materials: ${materials.joinToString(", ")}
                Workspace: ${workspace}
                
                Provide comprehensive safety assessment in JSON format:
                {
                  "safety_rating": "Low/Medium/High Risk",
                  "required_ppe": [
                    {
                      "item": "safety equipment name",
                      "reason": "why it's needed",
                      "critical": true/false,
                      "alternatives": ["alternative options"]
                    }
                  ],
                  "workspace_requirements": [
                    {
                      "requirement": "workspace need",
                      "importance": "critical/recommended/nice-to-have",
                      "details": "specific details"
                    }
                  ],
                  "tool_safety": [
                    {
                      "tool": "tool name",
                      "hazards": ["potential dangers"],
                      "precautions": ["safety measures"],
                      "training_needed": true/false
                    }
                  ],
                  "material_hazards": [
                    {
                      "material": "material name",
                      "hazards": ["health/safety concerns"],
                      "handling": "safe handling instructions",
                      "storage": "storage requirements",
                      "disposal": "disposal instructions"
                    }
                  ],
                  "emergency_procedures": [
                    {
                      "scenario": "emergency type",
                      "action": "what to do",
                      "prevention": "how to prevent"
                    }
                  ],
                  "safety_checklist": ["pre-start safety items"],
                  "recommendations": ["additional safety recommendations"]
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val safetyData = extractJsonFromResponse(response.getOrNull() ?: "")
                val safetyAssessment = json.decodeFromString<SafetyAssessment>(safetyData)
                Result.success(safetyAssessment)
            } else {
                Result.failure(Exception("Failed to assess safety requirements"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error assessing safety requirements", e)
            Result.failure(e)
        }
    }
    
    /**
     * Material calculator with waste factors and cost estimation
     */
    suspend fun calculateMaterialsWithWaste(
        projectSpecs: Map<String, Any>, // dimensions, quantity, etc.
        materialType: String,
        quality: String = "standard",
        location: String = "US" // for regional pricing
    ): Result<MaterialEstimate> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                Calculate materials needed for this DIY project with waste factors:
                
                Project Specifications: ${projectSpecs.entries.joinToString(", ") { "${it.key}: ${it.value}" }}
                Material Type: ${materialType}
                Quality Level: ${quality}
                Location: ${location}
                
                Provide detailed material calculation in JSON format:
                {
                  "primary_materials": [
                    {
                      "name": "material name",
                      "base_quantity": "exact amount needed",
                      "waste_factor": "percentage",
                      "final_quantity": "amount to purchase",
                      "unit": "unit of measurement",
                      "price_range": "low-high price",
                      "quality_grades": ["economy", "standard", "premium"],
                      "purchasing_tips": ["buying advice"]
                    }
                  ],
                  "hardware": [
                    {
                      "item": "hardware piece",
                      "quantity": "amount",
                      "size_spec": "specifications",
                      "material": "steel/stainless/etc",
                      "estimated_cost": "price range"
                    }
                  ],
                  "total_cost_estimate": {
                    "low": "minimum cost",
                    "typical": "standard cost",
                    "high": "premium cost"
                  },
                  "waste_management": {
                    "expected_waste": "percentage",
                    "reusable_scraps": ["what can be saved"],
                    "disposal_method": "how to dispose properly"
                  },
                  "purchasing_strategy": {
                    "bulk_savings": "where bulk makes sense",
                    "seasonal_timing": "best time to buy",
                    "store_recommendations": ["where to shop"]
                  },
                  "alternatives": [
                    {
                      "option": "alternative material",
                      "pros": ["advantages"],
                      "cons": ["disadvantages"],
                      "cost_difference": "price comparison"
                    }
                  ]
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val estimateData = extractJsonFromResponse(response.getOrNull() ?: "")
                val materialEstimate = json.decodeFromString<MaterialEstimate>(estimateData)
                Result.success(materialEstimate)
            } else {
                Result.failure(Exception("Failed to calculate materials"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating materials", e)
            Result.failure(e)
        }
    }
    
    /**
     * Tool recommendation and rental vs purchase advisor
     */
    suspend fun recommendTools(
        projectType: String,
        budget: String,
        frequency: String, // "one-time", "occasional", "regular"
        skillLevel: String = "beginner"
    ): Result<ToolRecommendation> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                Recommend tools for this DIY project with purchase vs rental advice:
                
                Project: ${projectType}
                Budget: ${budget}
                Usage Frequency: ${frequency}
                Skill Level: ${skillLevel}
                
                Provide comprehensive tool recommendations in JSON:
                {
                  "essential_tools": [
                    {
                      "tool": "tool name",
                      "purpose": "what it's used for",
                      "recommendation": "buy/rent/borrow",
                      "reasoning": "why this recommendation",
                      "price_range": "cost to buy",
                      "rental_cost": "cost to rent",
                      "alternatives": ["cheaper alternatives"],
                      "quality_needed": "professional/standard/basic",
                      "brands_recommended": ["good brands for this tool"]
                    }
                  ],
                  "nice_to_have": [
                    {
                      "tool": "optional tool",
                      "benefit": "what it improves",
                      "priority": "high/medium/low",
                      "cost": "price range"
                    }
                  ],
                  "safety_equipment": [
                    {
                      "item": "safety gear",
                      "mandatory": true/false,
                      "cost": "price range",
                      "where_to_buy": "purchasing advice"
                    }
                  ],
                  "total_investment": {
                    "buy_all": "cost if buying everything",
                    "rent_specialty": "cost if renting specialty tools",
                    "recommended_approach": "best strategy for this user"
                  },
                  "tool_care": [
                    {
                      "tool": "tool name",
                      "maintenance": "care instructions",
                      "storage": "storage requirements",
                      "lifespan": "expected duration with proper care"
                    }
                  ],
                  "learning_resources": ["where to learn tool usage"],
                  "local_resources": ["where to rent/buy locally"]
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val toolData = extractJsonFromResponse(response.getOrNull() ?: "")
                val toolRecommendation = json.decodeFromString<ToolRecommendation>(toolData)
                Result.success(toolRecommendation)
            } else {
                Result.failure(Exception("Failed to recommend tools"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error recommending tools", e)
            Result.failure(e)
        }
    }
    
    /**
     * Code compliance checker and permit advisor
     */
    suspend fun checkCodeCompliance(
        projectType: String,
        location: String,
        projectScope: String,
        structural: Boolean = false
    ): Result<ComplianceCheck> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                Check building code compliance and permit requirements:
                
                Project Type: ${projectType}
                Location: ${location} (general region)
                Project Scope: ${projectScope}
                Structural Changes: ${structural}
                
                Important: Provide general guidance only. Always recommend consulting local authorities for specific requirements.
                
                Provide compliance information in JSON:
                {
                  "permit_likely_required": true/false,
                  "permit_type": "type of permit if needed",
                  "code_considerations": [
                    {
                      "aspect": "code area (electrical, plumbing, etc.)",
                      "requirements": "general requirements",
                      "common_violations": ["typical mistakes"],
                      "inspection_points": ["what inspectors look for"]
                    }
                  ],
                  "professional_required": {
                    "electrician": true/false,
                    "plumber": true/false,
                    "structural_engineer": true/false,
                    "reasoning": "why professional help is needed"
                  },
                  "diy_limitations": ["what you cannot do yourself"],
                  "inspection_schedule": ["typical inspection points"],
                  "common_issues": ["frequently failed inspections"],
                  "preparation_tips": ["how to prepare for inspection"],
                  "cost_estimates": {
                    "permit_fees": "typical range",
                    "professional_consultation": "cost range",
                    "inspection_fees": "typical fees"
                  },
                  "next_steps": ["action items"],
                  "disclaimer": "reminder to check local codes"
                }
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val complianceData = extractJsonFromResponse(response.getOrNull() ?: "")
                val complianceCheck = json.decodeFromString<ComplianceCheck>(complianceData)
                Result.success(complianceCheck)
            } else {
                Result.failure(Exception("Failed to check code compliance"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking code compliance", e)
            Result.failure(e)
        }
    }
    
    /**
     * Troubleshooting advisor for when things go wrong
     */
    suspend fun getTroubleshootingHelp(
        problem: String,
        projectType: String,
        stepInvolved: String?,
        toolsUsed: List<String> = emptyList()
    ): Result<TroubleshootingAdvice> = withContext(Dispatchers.IO) {
        
        try {
            val prompt = """
                Help troubleshoot this DIY project problem:
                
                Problem: ${problem}
                Project Type: ${projectType}
                ${stepInvolved?.let { "Step: $it" } ?: ""}
                Tools Used: ${toolsUsed.joinToString(", ")}
                
                Provide systematic troubleshooting advice:
                
                1. IMMEDIATE SAFETY: Any safety concerns to address first?
                2. DIAGNOSIS: What likely went wrong and why?
                3. ASSESSMENT: How bad is the problem? Can it be fixed?
                4. SOLUTIONS: Step-by-step fix procedures
                5. PREVENTION: How to avoid this in the future
                6. WHEN TO CALL A PRO: Signs you need professional help
                
                Be practical, safety-focused, and honest about skill requirements.
                Include cost implications of different solutions.
            """.trimIndent()
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                agentType = "diy"
            )
            
            if (response.isSuccess) {
                val advice = TroubleshootingAdvice(
                    problem = problem,
                    projectType = projectType,
                    diagnosis = extractDiagnosis(response.getOrNull() ?: ""),
                    solutions = extractSolutions(response.getOrNull() ?: ""),
                    safetyConcerns = extractSafetyConcerns(response.getOrNull() ?: ""),
                    preventionTips = extractPreventionTips(response.getOrNull() ?: ""),
                    professionalHelp = shouldCallProfessional(response.getOrNull() ?: ""),
                    fullAdvice = response.getOrNull() ?: ""
                )
                Result.success(advice)
            } else {
                Result.failure(Exception("Failed to get troubleshooting advice"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting troubleshooting help", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private fun buildDIYProjectPrompt(
        description: String,
        skillLevel: String,
        budget: String,
        timeAvailable: String,
        availableTools: List<String>
    ): String {
        return """
            As the DIY Helper in GuideLens, create a comprehensive DIY project with safety as the top priority:
            
            Project Request: $description
            Skill Level: $skillLevel
            Budget: $budget
            Time Available: $timeAvailable
            ${if (availableTools.isNotEmpty()) "Tools Available: ${availableTools.joinToString(", ")}" else ""}
            
            Create a detailed DIY project with:
            
            1. PROJECT OVERVIEW:
            - Clear, descriptive title
            - Detailed description with final outcome
            - Realistic time estimate
            - Skill level verification
            - Budget breakdown
            
            2. SAFETY FIRST:
            - Required safety equipment (PPE)
            - Workspace safety setup
            - Tool safety requirements
            - Material handling precautions
            - Emergency procedures
            
            3. MATERIALS & TOOLS:
            - Detailed materials list with quantities
            - Tools required (own vs rent/buy)
            - Hardware specifications
            - Where to source materials
            - Cost estimates with waste factors
            
            4. STEP-BY-STEP INSTRUCTIONS:
            - Clear, numbered steps with safety callouts
            - Measurement requirements
            - Quality check points
            - Tool usage instructions
            - Time estimates per step
            
            5. CODE & PERMITS:
            - Building code considerations
            - Permit requirements (if any)
            - Professional consultation needs
            
            6. TROUBLESHOOTING:
            - Common problems and solutions
            - When to stop and call a professional
            - Quality standards and testing
            
            Format as structured JSON. Emphasize safety, be realistic about skill requirements, and provide professional-quality guidance!
        """.trimIndent()
    }
    
    private fun parseDIYProjectResponse(response: String): DIYProject {
        return try {
            val jsonData = extractJsonFromResponse(response)
            json.decodeFromString<DIYProject>(jsonData)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse structured response, creating basic project", e)
            createBasicDIYProject(response)
        }
    }
    
    private fun createBasicDIYProject(response: String): DIYProject {
        return DIYProject(
            title = "DIY Home Project",
            description = response.take(200),
            difficulty = "Medium",
            estimatedTime = 240, // 4 hours default
            budget = "Under $100",
            materials = extractMaterialsFromText(response),
            tools = extractToolsFromText(response),
            steps = extractStepsFromText(response),
            safetyRequirements = listOf("Basic safety gear required"),
            skillsRequired = listOf("Basic DIY skills")
        )
    }
    
    private fun createDIYArtifact(userId: String, diyProject: DIYProject): Artifact {
        return Artifact(
            type = ArtifactType.DIY_GUIDE,
            title = diyProject.title,
            description = diyProject.description,
            agentType = "diy",
            userId = userId,
            contentData = ArtifactContent.DIYContent(
                materials = diyProject.materials,
                tools = diyProject.tools,
                steps = diyProject.steps.mapIndexed { index, step ->
                    DIYStep(
                        stepNumber = index + 1,
                        title = step.title,
                        description = step.description,
                        duration = step.timeMinutes,
                        safetyWarnings = step.safetyWarnings,
                        toolsNeeded = step.toolsNeeded,
                        techniques = step.techniques,
                        tips = step.tips,
                        measurementNotes = step.measurementNotes
                    )
                },
                safetyRequirements = diyProject.safetyRequirements,
                skillsRequired = diyProject.skillsRequired
            ),
            difficulty = diyProject.difficulty,
            estimatedDuration = diyProject.estimatedTime,
            tags = listOf("diy", diyProject.difficulty.lowercase(), "safety-focused")
        )
    }
    
    private fun extractJsonFromResponse(response: String): String {
        val startIndex = response.indexOf("{")
        val endIndex = response.lastIndexOf("}") + 1
        
        return if (startIndex != -1 && endIndex > startIndex) {
            response.substring(startIndex, endIndex)
        } else {
            """{"title": "DIY Project", "description": "${response.take(100)}", "difficulty": "Medium"}"""
        }
    }
    
    // Text parsing helpers (simplified for example)
    private fun extractMaterialsFromText(text: String): List<Material> {
        return listOf(
            Material(
                name = "Construction materials",
                amount = "as needed",
                unit = "various",
                category = "building"
            )
        )
    }
    
    private fun extractToolsFromText(text: String): List<Tool> {
        return listOf(
            Tool(
                name = "Basic tools",
                required = true,
                safetyNotes = listOf("Use proper safety equipment")
            )
        )
    }
    
    private fun extractStepsFromText(text: String): List<DIYProjectStep> {
        return listOf(
            DIYProjectStep(
                title = "Begin Project",
                description = text.take(100),
                timeMinutes = 60,
                safetyWarnings = listOf("Follow all safety precautions")
            )
        )
    }
    
    // Troubleshooting response parsing
    private fun extractDiagnosis(response: String): String {
        val diagnosisStart = response.indexOf("DIAGNOSIS:")
        val nextSection = response.indexOf("ASSESSMENT:", diagnosisStart)
        
        return if (diagnosisStart != -1 && nextSection != -1) {
            response.substring(diagnosisStart + 10, nextSection).trim()
        } else {
            "Analysis needed to determine root cause"
        }
    }
    
    private fun extractSolutions(response: String): List<String> {
        val solutionsStart = response.indexOf("SOLUTIONS:")
        val nextSection = response.indexOf("PREVENTION:", solutionsStart)
        
        return if (solutionsStart != -1 && nextSection != -1) {
            response.substring(solutionsStart + 10, nextSection)
                .split("\n")
                .filter { it.trim().isNotEmpty() }
        } else {
            listOf("Review problem details and consult professional if needed")
        }
    }
    
    private fun extractSafetyConcerns(response: String): List<String> {
        val safetyStart = response.indexOf("IMMEDIATE SAFETY:")
        val nextSection = response.indexOf("DIAGNOSIS:", safetyStart)
        
        return if (safetyStart != -1 && nextSection != -1) {
            response.substring(safetyStart + 17, nextSection)
                .split("\n")
                .filter { it.trim().isNotEmpty() }
        } else {
            listOf("Ensure workspace safety before proceeding")
        }
    }
    
    private fun extractPreventionTips(response: String): List<String> {
        val preventionStart = response.indexOf("PREVENTION:")
        val nextSection = response.indexOf("WHEN TO CALL", preventionStart)
        
        return if (preventionStart != -1 && nextSection != -1) {
            response.substring(preventionStart + 11, nextSection)
                .split("\n")
                .filter { it.trim().isNotEmpty() }
        } else {
            listOf("Follow best practices and safety guidelines")
        }
    }
    
    private fun shouldCallProfessional(response: String): Boolean {
        val professionalSection = response.indexOf("WHEN TO CALL A PRO:")
        return professionalSection != -1 && 
               response.substring(professionalSection).contains("recommend", ignoreCase = true)
    }
}

// Data classes for DIY tools
@Serializable
data class DIYProject(
    val title: String,
    val description: String,
    val difficulty: String,
    val estimatedTime: Int,
    val budget: String,
    val materials: List<Material>,
    val tools: List<Tool>,
    val steps: List<DIYProjectStep>,
    val safetyRequirements: List<String>,
    val skillsRequired: List<String>
)

@Serializable
data class DIYProjectStep(
    val title: String,
    val description: String,
    val timeMinutes: Int? = null,
    val safetyWarnings: List<String> = emptyList(),
    val toolsNeeded: List<String> = emptyList(),
    val techniques: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val measurementNotes: List<String> = emptyList()
)

@Serializable
data class SafetyAssessment(
    val safety_rating: String,
    val required_ppe: List<PPEItem>,
    val workspace_requirements: List<WorkspaceRequirement>,
    val tool_safety: List<ToolSafety>,
    val material_hazards: List<MaterialHazard>,
    val emergency_procedures: List<EmergencyProcedure>,
    val safety_checklist: List<String>,
    val recommendations: List<String>
)

@Serializable
data class PPEItem(
    val item: String,
    val reason: String,
    val critical: Boolean,
    val alternatives: List<String>
)

@Serializable
data class WorkspaceRequirement(
    val requirement: String,
    val importance: String,
    val details: String
)

@Serializable
data class ToolSafety(
    val tool: String,
    val hazards: List<String>,
    val precautions: List<String>,
    val training_needed: Boolean
)

@Serializable
data class MaterialHazard(
    val material: String,
    val hazards: List<String>,
    val handling: String,
    val storage: String,
    val disposal: String
)

@Serializable
data class EmergencyProcedure(
    val scenario: String,
    val action: String,
    val prevention: String
)

@Serializable
data class MaterialEstimate(
    val primary_materials: List<PrimaryMaterial>,
    val hardware: List<HardwareItem>,
    val total_cost_estimate: CostEstimate,
    val waste_management: WasteManagement,
    val purchasing_strategy: PurchasingStrategy,
    val alternatives: List<MaterialAlternative>
)

@Serializable
data class PrimaryMaterial(
    val name: String,
    val base_quantity: String,
    val waste_factor: String,
    val final_quantity: String,
    val unit: String,
    val price_range: String,
    val quality_grades: List<String>,
    val purchasing_tips: List<String>
)

@Serializable
data class HardwareItem(
    val item: String,
    val quantity: String,
    val size_spec: String,
    val material: String,
    val estimated_cost: String
)

@Serializable
data class CostEstimate(
    val low: String,
    val typical: String,
    val high: String
)

@Serializable
data class WasteManagement(
    val expected_waste: String,
    val reusable_scraps: List<String>,
    val disposal_method: String
)

@Serializable
data class PurchasingStrategy(
    val bulk_savings: String,
    val seasonal_timing: String,
    val store_recommendations: List<String>
)

@Serializable
data class MaterialAlternative(
    val option: String,
    val pros: List<String>,
    val cons: List<String>,
    val cost_difference: String
)

@Serializable
data class ToolRecommendation(
    val essential_tools: List<EssentialTool>,
    val nice_to_have: List<OptionalTool>,
    val safety_equipment: List<SafetyEquipment>,
    val total_investment: TotalInvestment,
    val tool_care: List<ToolCare>,
    val learning_resources: List<String>,
    val local_resources: List<String>
)

@Serializable
data class EssentialTool(
    val tool: String,
    val purpose: String,
    val recommendation: String,
    val reasoning: String,
    val price_range: String,
    val rental_cost: String,
    val alternatives: List<String>,
    val quality_needed: String,
    val brands_recommended: List<String>
)

@Serializable
data class OptionalTool(
    val tool: String,
    val benefit: String,
    val priority: String,
    val cost: String
)

@Serializable
data class SafetyEquipment(
    val item: String,
    val mandatory: Boolean,
    val cost: String,
    val where_to_buy: String
)

@Serializable
data class TotalInvestment(
    val buy_all: String,
    val rent_specialty: String,
    val recommended_approach: String
)

@Serializable
data class ToolCare(
    val tool: String,
    val maintenance: String,
    val storage: String,
    val lifespan: String
)

@Serializable
data class ComplianceCheck(
    val permit_likely_required: Boolean,
    val permit_type: String,
    val code_considerations: List<CodeConsideration>,
    val professional_required: ProfessionalRequired,
    val diy_limitations: List<String>,
    val inspection_schedule: List<String>,
    val common_issues: List<String>,
    val preparation_tips: List<String>,
    val cost_estimates: ComplianceCosts,
    val next_steps: List<String>,
    val disclaimer: String
)

@Serializable
data class CodeConsideration(
    val aspect: String,
    val requirements: String,
    val common_violations: List<String>,
    val inspection_points: List<String>
)

@Serializable
data class ProfessionalRequired(
    val electrician: Boolean,
    val plumber: Boolean,
    val structural_engineer: Boolean,
    val reasoning: String
)

@Serializable
data class ComplianceCosts(
    val permit_fees: String,
    val professional_consultation: String,
    val inspection_fees: String
)

@Serializable
data class TroubleshootingAdvice(
    val problem: String,
    val projectType: String,
    val diagnosis: String,
    val solutions: List<String>,
    val safetyConcerns: List<String>,
    val preventionTips: List<String>,
    val professionalHelp: Boolean,
    val fullAdvice: String
)