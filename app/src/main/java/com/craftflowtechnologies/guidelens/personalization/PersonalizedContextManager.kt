package com.craftflowtechnologies.guidelens.personalization

import android.content.Context
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages personalized context injection for AI prompts
 * Integrates with UserDataManager to enhance AI responses with user data
 * while respecting privacy preferences
 */
class PersonalizedContextManager(
    private val context: Context,
    private val userDataManager: UserDataManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Cache for generated contexts to avoid regeneration
    private val _contextCache = MutableStateFlow<Map<String, CachedContext>>(emptyMap())
    private val contextCache: StateFlow<Map<String, CachedContext>> = _contextCache.asStateFlow()
    
    private val _isContextEnabled = MutableStateFlow(true)
    val isContextEnabled: StateFlow<Boolean> = _isContextEnabled.asStateFlow()

    /**
     * Generate personalized context for AI prompts based on agent type and user data
     */
    suspend fun generatePersonalizedPrompt(
        basePrompt: String,
        agentType: AgentType,
        sessionContext: SessionContext = SessionContext()
    ): String = withContext(Dispatchers.IO) {
        
        if (!_isContextEnabled.value) {
            return@withContext basePrompt
        }
        
        // Check cache first
        val cacheKey = "${agentType.name}_${sessionContext.mode}_${sessionContext.hashCode()}"
        val cachedContext = _contextCache.value[cacheKey]
        
        val personalizedContext = if (cachedContext?.isValid() == true) {
            cachedContext.context
        } else {
            val newContext = buildPersonalizedContext(agentType, sessionContext)
            updateCache(cacheKey, newContext)
            newContext
        }
        
        return@withContext combinePromptWithContext(basePrompt, personalizedContext, agentType)
    }

    /**
     * Build comprehensive personalized context based on agent type
     */
    private suspend fun buildPersonalizedContext(
        agentType: AgentType,
        sessionContext: SessionContext
    ): PersonalizedContext {
        val baseContext = userDataManager.generateAIContext()
        
        return PersonalizedContext(
            userContext = baseContext,
            agentSpecificContext = generateAgentSpecificContext(agentType),
            sessionContext = generateSessionContext(sessionContext),
            behavioralContext = generateBehavioralContext(agentType),
            temporalContext = generateTemporalContext(),
            preferenceContext = generatePreferenceContext(agentType)
        )
    }

    /**
     * Generate agent-specific contextual information
     */
    private suspend fun generateAgentSpecificContext(agentType: AgentType): String {
        return when (agentType) {
            AgentType.COOKING -> {
                val cookingProfile = userDataManager.loadCookingProfile()
                cookingProfile?.let { profile ->
                    buildString {
                        append("COOKING CONTEXT: ")
                        if (profile.skillLevel.isNotBlank()) {
                            append("User's cooking skill: ${profile.skillLevel}. ")
                        }
                        if (profile.preferredCuisines.isNotEmpty()) {
                            append("Prefers: ${profile.preferredCuisines.joinToString(", ")} cuisine. ")
                        }
                        if (profile.dietaryRestrictions.isNotEmpty()) {
                            append("Dietary needs: ${profile.dietaryRestrictions.joinToString(", ")}. ")
                        }
                        if (profile.allergies.isNotEmpty()) {
                            append("ALLERGIES: ${profile.allergies.joinToString(", ")} - CRITICAL to avoid. ")
                        }
                        if (profile.cookingGoals.isNotEmpty()) {
                            append("Goals: ${profile.cookingGoals.joinToString(", ")}. ")
                        }
                    }
                } ?: ""
            }
            
            AgentType.CRAFTING -> {
                buildString {
                    append("CRAFTING CONTEXT: ")
                    append("User enjoys creative projects and hands-on learning. ")
                    append("Focus on clear visual instructions and celebrate creativity. ")
                }
            }
            
            AgentType.DIY -> {
                buildString {
                    append("DIY CONTEXT: ")
                    append("SAFETY FIRST - always emphasize proper safety procedures. ")
                    append("User is working on home improvement projects. ")
                    append("Provide step-by-step guidance and tool recommendations. ")
                }
            }
            
            AgentType.BUDDY -> {
                val recentMoods = userDataManager.loadMoodEntries()
                    .filter { it.timestamp > System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000) }
                    .takeLast(3)
                
                buildString {
                    append("BUDDY CONTEXT: ")
                    append("Be a caring, supportive friend. ")
                    
                    if (recentMoods.isNotEmpty()) {
                        val avgMood = recentMoods.map { it.moodValue }.average()
                        when {
                            avgMood <= 2.5 -> append("User has been feeling down lately - provide extra support and encouragement. ")
                            avgMood >= 4.0 -> append("User has been feeling great lately - celebrate their positive energy! ")
                            else -> append("User's mood has been balanced lately. ")
                        }
                    }
                    
                    append("Listen actively, be empathetic, and offer gentle guidance when needed. ")
                }
            }
        }
    }

    /**
     * Generate session-specific context
     */
    private fun generateSessionContext(sessionContext: SessionContext): String {
        return buildString {
            append("SESSION CONTEXT: ")
            append("Mode: ${sessionContext.mode}. ")
            
            when (sessionContext.mode) {
                SessionMode.TEXT -> append("Provide detailed written instructions. ")
                SessionMode.VOICE -> append("Use conversational tone suitable for voice interaction. ")
                SessionMode.VIDEO -> append("You can see what the user is doing - provide real-time visual guidance. ")
            }
            
            if (sessionContext.isReturningSession) {
                append("User is returning to continue previous work. ")
            }
            
            if (sessionContext.timeOfDay.isNotBlank()) {
                append("Time: ${sessionContext.timeOfDay}. ")
            }
        }
    }

    /**
     * Generate behavioral context based on user patterns
     */
    private suspend fun generateBehavioralContext(agentType: AgentType): String {
        // This could be expanded with usage analytics
        return buildString {
            append("BEHAVIORAL CONTEXT: ")
            append("User prefers step-by-step guidance with clear explanations. ")
            append("Encourage questions and provide supportive feedback. ")
        }
    }

    /**
     * Generate temporal context (time of day, season, etc.)
     */
    private fun generateTemporalContext(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        val season = getCurrentSeason()
        
        return buildString {
            append("TEMPORAL CONTEXT: ")
            append("Current time: ${getTimeOfDayDescription(hour)} on $dayOfWeek. ")
            append("Season: $season. ")
            
            // Add time-appropriate suggestions
            when (hour) {
                in 6..11 -> append("Great time for morning activities and meal prep. ")
                in 12..17 -> append("Perfect time for afternoon projects. ")
                in 18..22 -> append("Evening time - consider quick and relaxing activities. ")
                else -> append("Late night - focus on quiet, calming activities. ")
            }
        }
    }

    /**
     * Generate preference-based context
     */
    private suspend fun generatePreferenceContext(agentType: AgentType): String {
        val profile = userDataManager.userProfile.value
        
        return profile?.let { 
            buildString {
                append("PREFERENCE CONTEXT: ")
                if (it.communicationStyle.isNotBlank()) {
                    append("Communication style: ${it.communicationStyle}. ")
                }
                if (it.accessibilityNeeds.isNotEmpty()) {
                    append("Accessibility needs: ${it.accessibilityNeeds.joinToString(", ")}. ")
                }
            }
        } ?: ""
    }

    /**
     * Combine base prompt with personalized context
     */
    private fun combinePromptWithContext(
        basePrompt: String,
        personalizedContext: PersonalizedContext,
        agentType: AgentType
    ): String {
        return buildString {
            // Start with agent role
            appendLine(getAgentRolePrompt(agentType))
            appendLine()
            
            // Add personalized contexts if they exist
            if (personalizedContext.userContext.isNotBlank()) {
                appendLine("USER PROFILE:")
                appendLine(personalizedContext.userContext)
                appendLine()
            }
            
            if (personalizedContext.agentSpecificContext.isNotBlank()) {
                appendLine(personalizedContext.agentSpecificContext)
                appendLine()
            }
            
            if (personalizedContext.sessionContext.isNotBlank()) {
                appendLine(personalizedContext.sessionContext)
                appendLine()
            }
            
            if (personalizedContext.temporalContext.isNotBlank()) {
                appendLine(personalizedContext.temporalContext)
                appendLine()
            }
            
            // Add behavioral and preference contexts
            if (personalizedContext.behavioralContext.isNotBlank()) {
                appendLine(personalizedContext.behavioralContext)
                appendLine()
            }
            
            if (personalizedContext.preferenceContext.isNotBlank()) {
                appendLine(personalizedContext.preferenceContext)
                appendLine()
            }
            
            // Add base prompt
            appendLine("USER REQUEST:")
            append(basePrompt)
            
            appendLine()
            appendLine()
            appendLine("Remember to be helpful, accurate, and personalized based on the context above. Maintain your agent personality while incorporating the user's specific needs and preferences.")
        }
    }

    private fun getAgentRolePrompt(agentType: AgentType): String {
        return when (agentType) {
            AgentType.COOKING -> """
                You are the Cooking Assistant in GuideLens. You provide expert culinary guidance, recipe help, and cooking instruction. 
                Your personality is encouraging, knowledgeable, and food-safety conscious. You love helping people create delicious meals.
            """.trimIndent()
            
            AgentType.CRAFTING -> """
                You are the Crafting Guru in GuideLens. You guide users through creative DIY projects with patience and enthusiasm.
                Your personality is creative, detail-oriented, and encouraging. You celebrate creativity and help users learn new skills.
            """.trimIndent()
            
            AgentType.DIY -> """
                You are the DIY Helper in GuideLens. You assist with home improvement and repair projects with a focus on safety.
                Your personality is methodical, safety-conscious, and practical. Safety is always your top priority.
            """.trimIndent()
            
            AgentType.BUDDY -> """
                You are Buddy, the caring companion in GuideLens. You're a supportive friend who listens, encourages, and helps with any task.
                Your personality is warm, empathetic, and adaptable. You're always here to support and encourage users.
            """.trimIndent()
        }
    }

    private fun updateCache(key: String, context: PersonalizedContext) {
        _contextCache.value = _contextCache.value + (key to CachedContext(
            context = context,
            createdAt = System.currentTimeMillis(),
            ttlMillis = 300_000 // 5 minutes cache
        ))
        
        // Clean up expired cache entries
        cleanupCache()
    }

    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        _contextCache.value = _contextCache.value.filterValues { it.createdAt + it.ttlMillis > now }
    }

    private fun getTimeOfDayDescription(hour: Int): String {
        return when (hour) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> "night"
        }
    }

    private fun getCurrentSeason(): String {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        return when (month) {
            12, 1, 2 -> "winter"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            9, 10, 11 -> "fall"
            else -> "spring"
        }
    }

    /**
     * Enable or disable personalized context injection
     */
    fun setContextEnabled(enabled: Boolean) {
        _isContextEnabled.value = enabled
    }

    /**
     * Clear all cached contexts
     */
    fun clearCache() {
        _contextCache.value = emptyMap()
    }

    fun cleanup() {
        scope.cancel()
    }
}

// Data models
@Serializable
data class PersonalizedContext(
    val userContext: String = "",
    val agentSpecificContext: String = "",
    val sessionContext: String = "",
    val behavioralContext: String = "",
    val temporalContext: String = "",
    val preferenceContext: String = ""
)

data class CachedContext(
    val context: PersonalizedContext,
    val createdAt: Long,
    val ttlMillis: Long = 300_000 // 5 minutes default
) {
    fun isValid(): Boolean = System.currentTimeMillis() - createdAt < ttlMillis
}

enum class AgentType {
    COOKING, CRAFTING, DIY, BUDDY
}

enum class SessionMode {
    TEXT, VOICE, VIDEO
}

@Serializable
data class SessionContext(
    val mode: SessionMode = SessionMode.TEXT,
    val isReturningSession: Boolean = false,
    val timeOfDay: String = "",
    val sessionDuration: Long = 0,
    val previousContext: String = "",
    val userTier: UserTier = UserTier.FREE
)

enum class UserTier {
    FREE, BASIC, PRO
}