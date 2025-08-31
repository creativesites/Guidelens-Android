package com.craftflowtechnologies.guidelens.ai

import android.content.Context
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.personalization.PersonalizedContextManager
import com.craftflowtechnologies.guidelens.personalization.AgentType
import com.craftflowtechnologies.guidelens.personalization.SessionContext
import com.craftflowtechnologies.guidelens.personalization.SessionMode
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

/**
 * Enhanced AI client that provides personalized responses by integrating
 * user data and context with AI model interactions
 */
class PersonalizedAIClient(
    private val context: Context,
    private val userDataManager: UserDataManager,
    private val enhancedGeminiClient: EnhancedGeminiClient
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val personalizedContextManager = PersonalizedContextManager(
        context = context,
        userDataManager = userDataManager
    )
    
    // Current session state
    private val _currentSession = MutableStateFlow<AISession?>(null)
    val currentSession: StateFlow<AISession?> = _currentSession.asStateFlow()
    
    private val _isPersonalizationEnabled = MutableStateFlow(true)
    val isPersonalizationEnabled: StateFlow<Boolean> = _isPersonalizationEnabled.asStateFlow()

    /**
     * Send personalized chat message with context injection
     */
    suspend fun sendPersonalizedMessage(
        message: String,
        agentType: AgentType,
        sessionId: String? = null,
        images: List<String> = emptyList(),
        mode: SessionMode = SessionMode.TEXT
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        
        try {
            // Create or update session
            val session = getCurrentOrCreateSession(sessionId, agentType, mode)
            
            // Build session context
            val sessionContext = SessionContext(
                mode = mode,
                isReturningSession = session.messageCount > 0,
                timeOfDay = getCurrentTimeOfDay(),
                sessionDuration = System.currentTimeMillis() - session.startTime,
                previousContext = session.lastContext
            )
            
            // Generate personalized prompt if personalization is enabled
            val personalizedPrompt = if (_isPersonalizationEnabled.value) {
                personalizedContextManager.generatePersonalizedPrompt(
                    basePrompt = message,
                    agentType = agentType,
                    sessionContext = sessionContext
                )
            } else {
                message
            }
            
            // Record tool usage for analytics
            recordToolUsage(agentType, "chat_message")
            
            // Send to AI model
            val response = when (mode) {
                SessionMode.TEXT -> {
                    if (images.isNotEmpty()) {
                        enhancedGeminiClient.sendMessageWithImages(personalizedPrompt, images)
                    } else {
                        enhancedGeminiClient.sendMessage(personalizedPrompt)
                    }
                }
                SessionMode.VOICE -> {
                    // Voice mode handling would go here
                    enhancedGeminiClient.sendMessage(personalizedPrompt + "\n\nNote: Provide response suitable for voice interaction.")
                }
                SessionMode.VIDEO -> {
                    // Video mode handling would go here
                    if (images.isNotEmpty()) {
                        enhancedGeminiClient.sendMessageWithImages(personalizedPrompt + "\n\nNote: You can see what the user is doing through their camera.", images)
                    } else {
                        enhancedGeminiClient.sendMessage(personalizedPrompt + "\n\nNote: Provide visual guidance suitable for video interaction.")
                    }
                }
            }
            
            // Update session
            updateSession(session, personalizedPrompt, response.getOrNull()?.text ?: "")
            
            // Transform response to include personalized context
            return@withContext response.map { aiResponse ->
                AIResponse(
                    text = aiResponse.text,
                    isPersonalized = _isPersonalizationEnabled.value,
                    contextUsed = if (_isPersonalizationEnabled.value) personalizedPrompt else null,
                    agentType = agentType,
                    sessionId = session.id,
                    confidence = aiResponse.confidence ?: 1.0f,
                    responseTime = System.currentTimeMillis() - session.lastMessageTime
                )
            }
            
        } catch (e: Exception) {
            Result.failure(PersonalizationException("Failed to send personalized message: ${e.message}", e))
        }
    }

    /**
     * Get agent-specific tool recommendations based on user data
     */
    suspend fun getPersonalizedToolRecommendations(
        agentType: AgentType
    ): List<ToolRecommendation> = withContext(Dispatchers.IO) {
        
        val userContext = userDataManager.generateAIContext()
        val recommendations = mutableListOf<ToolRecommendation>()
        
        when (agentType) {
            AgentType.COOKING -> {
                val cookingProfile = userDataManager.loadCookingProfile()
                cookingProfile?.let { profile ->
                    // Recommend tools based on skill level
                    when (profile.skillLevel.lowercase()) {
                        "beginner" -> recommendations.addAll(getBeginnerCookingTools())
                        "intermediate" -> recommendations.addAll(getIntermediateCookingTools())
                        "advanced" -> recommendations.addAll(getAdvancedCookingTools())
                    }
                    
                    // Add dietary restriction specific tools
                    if (profile.dietaryRestrictions.isNotEmpty()) {
                        recommendations.add(ToolRecommendation(
                            id = "dietary_scanner",
                            title = "Ingredient Scanner",
                            description = "Scan ingredients to check compatibility with your dietary restrictions",
                            priority = Priority.HIGH,
                            reason = "You have dietary restrictions: ${profile.dietaryRestrictions.joinToString(", ")}"
                        ))
                    }
                }
            }
            
            AgentType.BUDDY -> {
                val recentMoods = userDataManager.loadMoodEntries()
                    .filter { it.timestamp > System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000) }
                
                if (recentMoods.isNotEmpty()) {
                    val avgMood = recentMoods.map { it.moodValue }.average()
                    if (avgMood <= 2.5) {
                        recommendations.addAll(getSupportiveTools())
                    } else if (avgMood >= 4.0) {
                        recommendations.addAll(getCelebrationTools())
                    }
                }
            }
            
            AgentType.CRAFTING -> {
                recommendations.addAll(getCraftingTools())
            }
            
            AgentType.DIY -> {
                recommendations.addAll(getDIYTools())
            }
        }
        
        return@withContext recommendations.sortedByDescending { it.priority.ordinal }
    }

    /**
     * Generate personalized learning path for user
     */
    suspend fun generateLearningPath(
        agentType: AgentType,
        skillArea: String? = null
    ): LearningPath = withContext(Dispatchers.IO) {
        
        val userContext = userDataManager.generateAIContext()
        val userProfile = userDataManager.userProfile.value
        
        when (agentType) {
            AgentType.COOKING -> {
                val cookingProfile = userDataManager.loadCookingProfile()
                LearningPath(
                    title = "Personalized Cooking Journey",
                    steps = buildCookingLearningSteps(cookingProfile, userProfile),
                    estimatedDuration = "2-4 weeks",
                    difficulty = cookingProfile?.skillLevel ?: "Beginner"
                )
            }
            
            AgentType.CRAFTING -> {
                LearningPath(
                    title = "Creative Crafting Path",
                    steps = buildCraftingLearningSteps(userProfile),
                    estimatedDuration = "3-6 weeks",
                    difficulty = "Beginner"
                )
            }
            
            AgentType.DIY -> {
                LearningPath(
                    title = "Home Improvement Skills",
                    steps = buildDIYLearningSteps(userProfile),
                    estimatedDuration = "4-8 weeks",
                    difficulty = "Beginner"
                )
            }
            
            AgentType.BUDDY -> {
                LearningPath(
                    title = "Personal Growth Journey",
                    steps = buildWellnessLearningSteps(userProfile),
                    estimatedDuration = "Ongoing",
                    difficulty = "Personal"
                )
            }
        }
    }

    /**
     * Record user interaction for analytics and personalization improvement
     */
    private suspend fun recordToolUsage(agentType: AgentType, toolId: String) {
        userDataManager.recordToolUsage(
            agentId = agentType.name.lowercase(),
            toolId = toolId,
            duration = 0 // Will be updated when interaction completes
        )
    }

    private fun getCurrentOrCreateSession(
        sessionId: String?,
        agentType: AgentType,
        mode: SessionMode
    ): AISession {
        val currentTime = System.currentTimeMillis()
        
        return if (sessionId != null && _currentSession.value?.id == sessionId) {
            _currentSession.value!!
        } else {
            val newSession = AISession(
                id = sessionId ?: generateSessionId(),
                agentType = agentType,
                mode = mode,
                startTime = currentTime,
                lastMessageTime = currentTime
            )
            _currentSession.value = newSession
            newSession
        }
    }

    private fun updateSession(session: AISession, userPrompt: String, aiResponse: String) {
        _currentSession.value = session.copy(
            messageCount = session.messageCount + 1,
            lastMessageTime = System.currentTimeMillis(),
            lastUserPrompt = userPrompt,
            lastAIResponse = aiResponse,
            lastContext = userPrompt.take(500) // Store truncated context
        )
    }

    private fun getCurrentTimeOfDay(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> "night"
        }
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // Tool recommendation helpers
    private fun getBeginnerCookingTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("basic_recipes", "Simple Recipes", "Easy recipes perfect for beginners", Priority.HIGH, "Matched to your beginner skill level"),
        ToolRecommendation("cooking_timer", "Cooking Timer", "Never overcook again with smart timers", Priority.MEDIUM, "Essential for timing your cooking")
    )

    private fun getIntermediateCookingTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("technique_guide", "Advanced Techniques", "Learn professional cooking methods", Priority.HIGH, "Ready for the next skill level"),
        ToolRecommendation("flavor_pairing", "Flavor Combinations", "Discover amazing flavor pairings", Priority.MEDIUM, "Expand your cooking creativity")
    )

    private fun getAdvancedCookingTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("recipe_creation", "Recipe Creator", "Design your own original recipes", Priority.HIGH, "Perfect for your advanced skills"),
        ToolRecommendation("molecular_cooking", "Modern Techniques", "Explore molecular gastronomy", Priority.MEDIUM, "Push your boundaries")
    )

    private fun getSupportiveTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("mood_boost", "Mood Lifter", "Activities to help you feel better", Priority.HIGH, "You've been feeling down lately"),
        ToolRecommendation("breathing_exercise", "Calming Breaths", "Relaxing breathing exercises", Priority.MEDIUM, "Find some peace and calm")
    )

    private fun getCelebrationTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("celebration", "Celebrate Success", "Celebrate your achievements", Priority.HIGH, "You've been feeling great!"),
        ToolRecommendation("gratitude", "Gratitude Practice", "Share what you're thankful for", Priority.MEDIUM, "Keep the positive energy flowing")
    )

    private fun getCraftingTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("project_planner", "Project Planner", "Plan your next crafting project", Priority.HIGH, ""),
        ToolRecommendation("color_palette", "Color Guide", "Create beautiful color combinations", Priority.MEDIUM, "")
    )

    private fun getDIYTools(): List<ToolRecommendation> = listOf(
        ToolRecommendation("safety_check", "Safety First", "Essential safety guidelines", Priority.HIGH, "Safety is always the priority"),
        ToolRecommendation("tool_guide", "Tool Identifier", "Identify the right tools for your project", Priority.MEDIUM, "")
    )

    // Learning path builders
    private fun buildCookingLearningSteps(cookingProfile: com.craftflowtechnologies.guidelens.storage.UserCookingProfile?, userProfile: com.craftflowtechnologies.guidelens.storage.UserProfile?): List<LearningStep> {
        return listOf(
            LearningStep("Master Basic Techniques", "knife skills, cooking methods", "Week 1", false),
            LearningStep("Understand Flavors", "seasoning, herb combinations", "Week 2", false),
            LearningStep("Plan Complete Meals", "meal planning, nutrition", "Week 3-4", false)
        )
    }

    private fun buildCraftingLearningSteps(userProfile: com.craftflowtechnologies.guidelens.storage.UserProfile?): List<LearningStep> {
        return listOf(
            LearningStep("Basic Materials", "paper, fabric, adhesives", "Week 1", false),
            LearningStep("Simple Projects", "cards, decorations", "Week 2-3", false),
            LearningStep("Intermediate Skills", "sewing, painting techniques", "Week 4-6", false)
        )
    }

    private fun buildDIYLearningSteps(userProfile: com.craftflowtechnologies.guidelens.storage.UserProfile?): List<LearningStep> {
        return listOf(
            LearningStep("Tool Safety", "proper usage, maintenance", "Week 1-2", false),
            LearningStep("Basic Repairs", "simple fixes, troubleshooting", "Week 3-4", false),
            LearningStep("Project Planning", "measurement, material calculation", "Week 5-8", false)
        )
    }

    private fun buildWellnessLearningSteps(userProfile: com.craftflowtechnologies.guidelens.storage.UserProfile?): List<LearningStep> {
        return listOf(
            LearningStep("Self-Awareness", "mood tracking, reflection", "Ongoing", false),
            LearningStep("Coping Skills", "breathing, mindfulness", "Ongoing", false),
            LearningStep("Personal Growth", "goal setting, celebration", "Ongoing", false)
        )
    }

    /**
     * Enable or disable personalization
     */
    fun setPersonalizationEnabled(enabled: Boolean) {
        _isPersonalizationEnabled.value = enabled
        personalizedContextManager.setContextEnabled(enabled)
    }

    /**
     * Clear personalization cache
     */
    fun clearPersonalizationCache() {
        personalizedContextManager.clearCache()
    }

    fun cleanup() {
        personalizedContextManager.cleanup()
        scope.cancel()
    }
}

// Data models
@Serializable
data class AIResponse(
    val text: String,
    val isPersonalized: Boolean,
    val contextUsed: String? = null,
    val agentType: AgentType,
    val sessionId: String,
    val confidence: Float = 1.0f,
    val responseTime: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class AISession(
    val id: String,
    val agentType: AgentType,
    val mode: SessionMode,
    val startTime: Long,
    val messageCount: Int = 0,
    val lastMessageTime: Long,
    val lastUserPrompt: String = "",
    val lastAIResponse: String = "",
    val lastContext: String = ""
)

data class ToolRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: Priority,
    val reason: String
)

enum class Priority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
data class LearningPath(
    val title: String,
    val steps: List<LearningStep>,
    val estimatedDuration: String,
    val difficulty: String
)

@Serializable
data class LearningStep(
    val title: String,
    val description: String,
    val timeframe: String,
    val completed: Boolean
)

class PersonalizationException(message: String, cause: Throwable? = null) : Exception(message, cause)