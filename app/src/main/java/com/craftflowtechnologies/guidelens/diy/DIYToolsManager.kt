package com.craftflowtechnologies.guidelens.diy

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.*

class DIYToolsManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _activeProjects = MutableStateFlow<List<DIYProject>>(emptyList())
    val activeProjects: StateFlow<List<DIYProject>> = _activeProjects.asStateFlow()
    
    private val _toolInventory = MutableStateFlow<Map<String, DIYTool>>(emptyMap())
    val toolInventory: StateFlow<Map<String, DIYTool>> = _toolInventory.asStateFlow()
    
    private val _safetyAlerts = MutableSharedFlow<SafetyAlert>(extraBufferCapacity = 10)
    val safetyAlerts: SharedFlow<SafetyAlert> = _safetyAlerts.asSharedFlow()

    fun getDIYTools(): List<DIYHelperTool> {
        return listOf(
            DIYHelperTool(
                id = "project_assessor",
                name = "Project Assessor",
                description = "Evaluate project complexity and requirements",
                icon = Icons.Default.Assessment,
                action = { assessProject() },
                category = DIYCategory.PLANNING,
                safetyLevel = SafetyLevel.LOW,
                estimatedTime = "15-30 minutes"
            ),
            DIYHelperTool(
                id = "tool_identifier",
                name = "Tool Identifier",
                description = "Identify tools needed for your project",
                icon = Icons.Default.BuildCircle,
                action = { identifyTools() },
                category = DIYCategory.TOOLS,
                safetyLevel = SafetyLevel.LOW,
                estimatedTime = "10 minutes"
            ),
            DIYHelperTool(
                id = "safety_checker",
                name = "Safety Checker",
                description = "Review safety requirements and precautions",
                icon = Icons.Default.Security,
                action = { checkSafety() },
                category = DIYCategory.SAFETY,
                safetyLevel = SafetyLevel.CRITICAL,
                estimatedTime = "10-15 minutes"
            ),
            DIYHelperTool(
                id = "cost_calculator",
                name = "Cost Calculator",
                description = "Calculate project materials and labor costs",
                icon = Icons.Default.Calculate,
                action = { calculateCosts() },
                category = DIYCategory.BUDGETING,
                safetyLevel = SafetyLevel.LOW,
                estimatedTime = "15 minutes"
            ),
            DIYHelperTool(
                id = "permit_checker",
                name = "Permit Checker",
                description = "Check if permits are required for your project",
                icon = Icons.Default.Gavel,
                action = { checkPermits() },
                category = DIYCategory.LEGAL,
                safetyLevel = SafetyLevel.MEDIUM,
                estimatedTime = "20 minutes"
            ),
            DIYHelperTool(
                id = "measurement_guide",
                name = "Measurement Guide",
                description = "Accurate measuring and calculation helper",
                icon = Icons.Default.Straighten,
                action = { guideMeasurement() },
                category = DIYCategory.PLANNING,
                safetyLevel = SafetyLevel.LOW,
                estimatedTime = "Variable"
            ),
            DIYHelperTool(
                id = "troubleshooter",
                name = "Problem Solver",
                description = "Diagnose and solve DIY problems",
                icon = Icons.Default.Support,
                action = { troubleshootIssue() },
                category = DIYCategory.SUPPORT,
                safetyLevel = SafetyLevel.MEDIUM,
                estimatedTime = "10-45 minutes"
            ),
            DIYHelperTool(
                id = "code_compliance",
                name = "Code Compliance",
                description = "Check building codes and standards",
                icon = Icons.Default.Rule,
                action = { checkCodes() },
                category = DIYCategory.LEGAL,
                safetyLevel = SafetyLevel.HIGH,
                estimatedTime = "30 minutes"
            ),
            DIYHelperTool(
                id = "emergency_guide",
                name = "Emergency Guide",
                description = "Emergency procedures and first aid",
                icon = Icons.Default.LocalHospital,
                action = { provideEmergencyGuide() },
                category = DIYCategory.SAFETY,
                safetyLevel = SafetyLevel.CRITICAL,
                estimatedTime = "5-10 minutes"
            ),
            DIYHelperTool(
                id = "skill_matcher",
                name = "Skill Matcher",
                description = "Match your skill level to appropriate projects",
                icon = Icons.Default.EmojiEvents,
                action = { matchSkills() },
                category = DIYCategory.PLANNING,
                safetyLevel = SafetyLevel.LOW,
                estimatedTime = "15 minutes"
            )
        )
    }

    private suspend fun assessProject(): DIYActionResult {
        val assessmentCriteria = listOf(
            "Project complexity and skill level required",
            "Time investment (including prep and cleanup)",
            "Tool and equipment requirements",
            "Material costs and availability",
            "Safety considerations and risks",
            "Potential need for permits or inspections",
            "Impact on home value and functionality"
        )
        
        return DIYActionResult.Success(
            title = "Project Assessment Complete",
            description = "Here's what I found about your DIY project:",
            assessmentCriteria = assessmentCriteria,
            recommendations = listOf(
                "Start with smaller practice projects if you're new to this type of work",
                "Budget 20-30% extra for unexpected issues",
                "Have a backup plan if the project takes longer than expected"
            ),
            riskFactors = listOf(
                RiskFactor("Electrical Work", "HIGH - May require licensed electrician", SafetyLevel.CRITICAL),
                RiskFactor("Plumbing", "MEDIUM - Check local codes first", SafetyLevel.HIGH),
                RiskFactor("Structural Changes", "HIGH - Consult engineer first", SafetyLevel.CRITICAL)
            )
        )
    }

    private suspend fun identifyTools(): DIYActionResult {
        val toolCategories = mapOf(
            "Basic Hand Tools" to listOf("Hammer", "Screwdrivers", "Pliers", "Level", "Tape Measure"),
            "Power Tools" to listOf("Drill", "Circular Saw", "Jigsaw", "Sander", "Router"),
            "Safety Equipment" to listOf("Safety Glasses", "Work Gloves", "Dust Mask", "Hearing Protection"),
            "Measuring Tools" to listOf("Tape Measure", "Square", "Level", "Stud Finder"),
            "Specialty Tools" to listOf("Varies by project", "May require rental")
        )
        
        return DIYActionResult.Success(
            title = "Tool Identification Guide",
            description = "Here are the tools you'll need:",
            toolCategories = toolCategories,
            tips = listOf(
                "Invest in quality basic tools - they'll last longer",
                "Consider renting expensive specialty tools",
                "Always have backup batteries for cordless tools",
                "Keep tools clean and properly maintained"
            ),
            safetyReminders = listOf(
                "Always wear appropriate safety gear",
                "Read tool manuals before first use",
                "Inspect tools before each use",
                "Store tools safely away from children"
            )
        )
    }

    private suspend fun checkSafety(): DIYActionResult {
        val safetyChecklist = listOf(
            SafetyItem("Personal Protective Equipment", "Safety glasses, gloves, appropriate clothing", true),
            SafetyItem("Workspace Preparation", "Clear area, adequate lighting, ventilation", true),
            SafetyItem("Tool Inspection", "Check tools for damage before use", true),
            SafetyItem("Emergency Preparedness", "First aid kit, emergency contacts, fire extinguisher", true),
            SafetyItem("Electrical Safety", "Turn off power, use GFCI outlets, avoid water", true),
            SafetyItem("Chemical Safety", "Read MSDS sheets, proper ventilation, disposal", true)
        )
        
        val criticalWarnings = listOf(
            "NEVER work on electrical systems while power is on",
            "Always test electrical connections with a multimeter",
            "Use proper ladder safety - 4:1 ratio",
            "Ensure adequate ventilation when using chemicals",
            "Have someone nearby when doing potentially dangerous work"
        )
        
        return DIYActionResult.Success(
            title = "Safety Assessment",
            description = "Safety is our top priority. Here's your safety checklist:",
            safetyChecklist = safetyChecklist,
            criticalWarnings = criticalWarnings,
            emergencyContacts = EmergencyContacts(
                poison = "1-800-222-1222",
                medical = "911",
                gasLeak = "Call gas company immediately",
                electricalEmergency = "Call utility company"
            )
        )
    }

    private suspend fun calculateCosts(): DIYActionResult {
        return DIYActionResult.Success(
            title = "Cost Calculator",
            description = "Let's break down your project costs:",
            costBreakdown = DIYCostBreakdown(
                materials = CostCategory("Materials", "60-70% of total", "Lumber, fasteners, finish materials"),
                tools = CostCategory("Tools", "20-30% of total", "New tools or rentals needed"),
                permits = CostCategory("Permits", "2-5% of total", "Building permits, inspections"),
                contingency = CostCategory("Contingency", "15-20% of total", "Unexpected issues, mistakes"),
                labor = CostCategory("Professional Help", "Variable", "Hired help for specific tasks")
            ),
            tips = listOf(
                "Get quotes from multiple suppliers",
                "Check for sales and bulk discounts",
                "Factor in delivery costs for heavy materials",
                "Consider tool rental vs. purchase costs"
            ),
            budgetingTips = listOf(
                "Always add 20% contingency for unexpected costs",
                "Start with essential materials, add extras later",
                "Consider doing project in phases if budget is tight"
            )
        )
    }

    private suspend fun checkPermits(): DIYActionResult {
        val permitTypes = listOf(
            PermitInfo("Building Permit", "Required for structural changes, additions", "2-4 weeks", "$100-500"),
            PermitInfo("Electrical Permit", "Required for new circuits, major electrical work", "1-2 weeks", "$50-200"),
            PermitInfo("Plumbing Permit", "Required for new plumbing, major repairs", "1-3 weeks", "$50-300"),
            PermitInfo("Mechanical Permit", "Required for HVAC work", "1-2 weeks", "$75-250")
        )
        
        return DIYActionResult.Success(
            title = "Permit Requirements",
            description = "Here's what you need to know about permits:",
            permitInfo = permitTypes,
            processSteps = listOf(
                "Contact your local building department",
                "Submit detailed project plans and specifications",
                "Pay permit fees",
                "Schedule required inspections",
                "Complete work according to approved plans",
                "Pass final inspection"
            ),
            tips = listOf(
                "Apply for permits before starting work",
                "Keep all permits and inspection records",
                "Don't try to hide work that requires permits"
            )
        )
    }

    private suspend fun guideMeasurement(): DIYActionResult {
        val measurementTips = listOf(
            "Measure twice, cut once - the golden rule of DIY",
            "Use the right tool: tape measure for long distances, ruler for precision",
            "Mark your measurements clearly with a pencil",
            "Account for material thickness in your calculations",
            "Double-check all measurements before making cuts"
        )
        
        return DIYActionResult.Success(
            title = "Measurement Guide",
            description = "Accurate measurements are crucial for success:",
            measurementTips = measurementTips,
            commonMistakes = listOf(
                "Not accounting for blade width (kerf) when cutting",
                "Measuring from the wrong reference point",
                "Not checking if surfaces are actually square",
                "Forgetting to add material for joints and overlaps"
            ),
            tools = listOf("Tape measure", "Speed square", "Level", "Marking pencil", "Calculator")
        )
    }

    private suspend fun troubleshootIssue(): DIYActionResult {
        val commonIssues = listOf(
            TroubleshootingGuide(
                issue = "Screw won't go in smoothly",
                causes = listOf("Wrong screw size", "Need pilot hole", "Screw is dull"),
                solutions = listOf("Drill pilot hole", "Use correct screw size", "Apply soap to screw threads")
            ),
            TroubleshootingGuide(
                issue = "Paint isn't adhering well",
                causes = listOf("Surface not clean", "Wrong primer", "Humidity too high"),
                solutions = listOf("Clean surface thoroughly", "Use appropriate primer", "Check weather conditions")
            ),
            TroubleshootingGuide(
                issue = "Joint is not tight",
                causes = listOf("Wood moved", "Wrong glue", "Insufficient clamping pressure"),
                solutions = listOf("Re-cut joint", "Use appropriate adhesive", "Apply more clamping pressure")
            )
        )
        
        return DIYActionResult.Success(
            title = "Problem Solver",
            description = "Let's diagnose and fix your DIY challenge:",
            troubleshootingGuides = commonIssues,
            generalTips = listOf(
                "Take a step back and reassess the situation",
                "Check that you're using the right technique",
                "Don't be afraid to start over if needed",
                "Sometimes a different approach works better"
            )
        )
    }

    private suspend fun checkCodes(): DIYActionResult {
        return DIYActionResult.Success(
            title = "Code Compliance Guide",
            description = "Building codes ensure safety and quality:",
            codeAreas = listOf(
                CodeArea("Electrical", "NEC standards, outlet spacing, GFCI requirements"),
                CodeArea("Plumbing", "Pipe sizing, venting requirements, fixture spacing"),
                CodeArea("Structural", "Load requirements, fastener specifications, span tables"),
                CodeArea("Fire Safety", "Smoke detector placement, egress requirements"),
                CodeArea("Accessibility", "ADA compliance for public spaces")
            ),
            resources = listOf(
                "Contact local building department for current codes",
                "International Building Code (IBC) for general construction",
                "National Electrical Code (NEC) for electrical work",
                "Uniform Plumbing Code (UPC) for plumbing"
            ),
            warnings = listOf(
                "Codes vary by location - always check local requirements",
                "Some work requires licensed professionals",
                "Code violations can affect insurance and resale value"
            )
        )
    }

    private suspend fun provideEmergencyGuide(): DIYActionResult {
        val emergencyProcedures = listOf(
            EmergencyProcedure(
                type = "Electrical Shock",
                immediateSteps = listOf(
                    "DO NOT touch the person if still in contact with electricity",
                    "Turn off power at breaker or unplug device",
                    "Call 911 immediately",
                    "Begin CPR if trained and person is unconscious"
                )
            ),
            EmergencyProcedure(
                type = "Severe Cut",
                immediateSteps = listOf(
                    "Apply direct pressure to wound with clean cloth",
                    "Elevate injured area above heart if possible",
                    "Call 911 for severe bleeding",
                    "Do not remove objects embedded in wound"
                )
            ),
            EmergencyProcedure(
                type = "Chemical Exposure",
                immediateSteps = listOf(
                    "Remove contaminated clothing",
                    "Flush with water for 15-20 minutes",
                    "Call Poison Control: 1-800-222-1222",
                    "Seek immediate medical attention"
                )
            )
        )
        
        return DIYActionResult.Success(
            title = "Emergency Response Guide",
            description = "Be prepared for DIY emergencies:",
            emergencyProcedures = emergencyProcedures,
            firstAidKit = listOf(
                "Adhesive bandages", "Sterile gauze pads", "Medical tape", 
                "Antiseptic wipes", "Instant cold compress", "Emergency contact list"
            ),
            prevention = listOf(
                "Keep first aid kit easily accessible",
                "Have someone nearby during risky work",
                "Keep emergency numbers posted",
                "Know how to shut off utilities quickly"
            )
        )
    }

    private suspend fun matchSkills(): DIYActionResult {
        val skillLevels = mapOf(
            "Beginner" to SkillLevelGuide(
                description = "New to DIY projects",
                suitableProjects = listOf("Painting", "Basic carpentry", "Organizing"),
                toolsNeeded = listOf("Basic hand tools", "Safety equipment"),
                timeframe = "Weekends, take your time"
            ),
            "Intermediate" to SkillLevelGuide(
                description = "Some DIY experience",
                suitableProjects = listOf("Tile work", "Cabinet installation", "Deck building"),
                toolsNeeded = listOf("Power tools", "Specialty tools"),
                timeframe = "Multi-weekend projects"
            ),
            "Advanced" to SkillLevelGuide(
                description = "Extensive DIY experience",
                suitableProjects = listOf("Room additions", "Major renovations", "Complex systems"),
                toolsNeeded = listOf("Professional-grade tools", "Specialty equipment"),
                timeframe = "Multi-week/month projects"
            )
        )
        
        return DIYActionResult.Success(
            title = "Skill Level Assessment",
            description = "Find projects that match your abilities:",
            skillLevels = skillLevels,
            progressionTips = listOf(
                "Start with simpler versions of complex projects",
                "Master basic skills before advancing",
                "Take classes or watch tutorials for new techniques",
                "Practice on scrap materials first"
            )
        )
    }

    fun cleanup() {
        scope.cancel()
    }
}

// Data models for DIY tools
data class DIYHelperTool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val action: suspend () -> DIYActionResult,
    val category: DIYCategory,
    val safetyLevel: SafetyLevel,
    val estimatedTime: String,
    val prerequisites: List<String> = emptyList()
)

enum class DIYCategory {
    PLANNING, TOOLS, SAFETY, BUDGETING, LEGAL, SUPPORT
}

enum class SafetyLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

sealed class DIYActionResult {
    data class Success(
        val title: String,
        val description: String,
        val assessmentCriteria: List<String> = emptyList(),
        val recommendations: List<String> = emptyList(),
        val riskFactors: List<RiskFactor> = emptyList(),
        val toolCategories: Map<String, List<String>> = emptyMap(),
        val tips: List<String> = emptyList(),
        val safetyReminders: List<String> = emptyList(),
        val safetyChecklist: List<SafetyItem> = emptyList(),
        val criticalWarnings: List<String> = emptyList(),
        val emergencyContacts: EmergencyContacts? = null,
        val costBreakdown: DIYCostBreakdown? = null,
        val budgetingTips: List<String> = emptyList(),
        val permitInfo: List<PermitInfo> = emptyList(),
        val processSteps: List<String> = emptyList(),
        val measurementTips: List<String> = emptyList(),
        val commonMistakes: List<String> = emptyList(),
        val tools: List<String> = emptyList(),
        val troubleshootingGuides: List<TroubleshootingGuide> = emptyList(),
        val generalTips: List<String> = emptyList(),
        val codeAreas: List<CodeArea> = emptyList(),
        val resources: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
        val emergencyProcedures: List<EmergencyProcedure> = emptyList(),
        val firstAidKit: List<String> = emptyList(),
        val prevention: List<String> = emptyList(),
        val skillLevels: Map<String, SkillLevelGuide> = emptyMap(),
        val progressionTips: List<String> = emptyList()
    ) : DIYActionResult()
    
    data class Error(val message: String) : DIYActionResult()
}

@Serializable
data class RiskFactor(
    val factor: String,
    val description: String,
    val level: SafetyLevel
)

@Serializable
data class SafetyItem(
    val item: String,
    val description: String,
    val required: Boolean
)

@Serializable
data class EmergencyContacts(
    val poison: String,
    val medical: String,
    val gasLeak: String,
    val electricalEmergency: String
)

@Serializable
data class DIYCostBreakdown(
    val materials: CostCategory,
    val tools: CostCategory,
    val permits: CostCategory,
    val contingency: CostCategory,
    val labor: CostCategory
)

@Serializable
data class CostCategory(
    val name: String,
    val percentage: String,
    val description: String
)

@Serializable
data class PermitInfo(
    val type: String,
    val description: String,
    val timeframe: String,
    val cost: String
)

@Serializable
data class TroubleshootingGuide(
    val issue: String,
    val causes: List<String>,
    val solutions: List<String>
)

@Serializable
data class CodeArea(
    val area: String,
    val description: String
)

@Serializable
data class EmergencyProcedure(
    val type: String,
    val immediateSteps: List<String>
)

@Serializable
data class SkillLevelGuide(
    val description: String,
    val suitableProjects: List<String>,
    val toolsNeeded: List<String>,
    val timeframe: String
)

@Serializable
data class DIYProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val status: ProjectStatus,
    val startDate: Long,
    val estimatedCompletion: Long,
    val actualCompletion: Long? = null,
    val skillLevel: String,
    val safetyLevel: SafetyLevel,
    val permits: List<String> = emptyList(),
    val tools: List<String>,
    val materials: List<String>,
    val steps: List<String>,
    val notes: MutableList<String> = mutableListOf(),
    val photos: MutableList<String> = mutableListOf(),
    val issues: MutableList<String> = mutableListOf(),
    val totalCost: Float = 0f
)

@Serializable
enum class ProjectStatus {
    PLANNING, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
}

@Serializable
data class DIYTool(
    val name: String,
    val type: String,
    val owned: Boolean,
    val condition: ToolCondition,
    val lastMaintenance: Long? = null,
    val purchaseDate: Long? = null,
    val cost: Float? = null,
    val location: String = "Garage"
)

@Serializable
enum class ToolCondition {
    NEW, GOOD, FAIR, NEEDS_MAINTENANCE, BROKEN
}

@Serializable
data class SafetyAlert(
    val type: AlertType,
    val message: String,
    val severity: SafetyLevel,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class AlertType {
    TOOL_SAFETY, CHEMICAL_HAZARD, ELECTRICAL_WARNING, STRUCTURAL_CONCERN, PERMIT_REQUIRED
}