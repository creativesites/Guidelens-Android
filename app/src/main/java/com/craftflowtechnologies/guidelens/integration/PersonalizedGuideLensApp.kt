package com.craftflowtechnologies.guidelens.integration

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.ai.PersonalizedAIClient
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.personalization.AgentType
import com.craftflowtechnologies.guidelens.personalization.SessionMode
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import com.craftflowtechnologies.guidelens.ui.Agent
import com.craftflowtechnologies.guidelens.ui.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Integration layer that enhances the existing GuideLensApp with personalized AI capabilities
 * This class manages the bridge between the old system and new personalized features
 */
class PersonalizedGuideLensIntegration(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    // Initialize user data storage
    val userDataManager = UserDataManager(context)
    
    // Enhanced AI client that uses personalization
    private lateinit var personalizedAIClient: PersonalizedAIClient
    
    // State for personalization features
    private val _isPersonalizationEnabled = mutableStateOf(true)
    val isPersonalizationEnabled: State<Boolean> = _isPersonalizationEnabled
    
    private val _showPrivacySettings = mutableStateOf(false)
    val showPrivacySettings: State<Boolean> = _showPrivacySettings
    
    /**
     * Initialize the personalized AI client with the provided EnhancedGeminiClient
     */
    fun initialize(enhancedGeminiClient: EnhancedGeminiClient) {
        personalizedAIClient = PersonalizedAIClient(
            context = context,
            userDataManager = userDataManager,
            enhancedGeminiClient = enhancedGeminiClient
        )
    }
    
    /**
     * Send a personalized message that integrates with the existing chat system
     */
    suspend fun sendPersonalizedMessage(
        message: String,
        selectedAgent: Agent,
        sessionId: String?,
        images: List<String> = emptyList(),
        isVoiceMode: Boolean = false,
        isVideoMode: Boolean = false
    ): Result<ChatMessage> {
        
        val agentType = mapAgentToType(selectedAgent.id)
        val sessionMode = when {
            isVideoMode -> SessionMode.VIDEO
            isVoiceMode -> SessionMode.VOICE
            else -> SessionMode.TEXT
        }
        
        return try {
            val response = personalizedAIClient.sendPersonalizedMessage(
                message = message,
                agentType = agentType,
                sessionId = sessionId,
                images = images,
                mode = sessionMode
            )
            
            response.map { aiResponse ->
                ChatMessage(
                    text = aiResponse.text,
                    isFromUser = false,
                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    images = images
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get personalized tool recommendations for the current agent
     */
    suspend fun getToolRecommendations(selectedAgent: Agent) = 
        personalizedAIClient.getPersonalizedToolRecommendations(mapAgentToType(selectedAgent.id))
    
    /**
     * Generate a personalized learning path
     */
    suspend fun generateLearningPath(selectedAgent: Agent) = 
        personalizedAIClient.generateLearningPath(mapAgentToType(selectedAgent.id))
    
    /**
     * Enhanced message sending that checks for personalization preferences
     */
    suspend fun enhancedSendMessage(
        originalSendFunction: suspend (String, List<String>) -> Result<ChatMessage>,
        message: String,
        selectedAgent: Agent,
        sessionId: String?,
        images: List<String> = emptyList(),
        isVoiceMode: Boolean = false,
        isVideoMode: Boolean = false
    ): Result<ChatMessage> {
        
        // Check if personalization is enabled
        val privacySettings = userDataManager.privacySettings.value
        
        return if (privacySettings.allowPersonalization && _isPersonalizationEnabled.value) {
            // Use personalized AI client
            sendPersonalizedMessage(
                message = message,
                selectedAgent = selectedAgent,
                sessionId = sessionId,
                images = images,
                isVoiceMode = isVoiceMode,
                isVideoMode = isVideoMode
            )
        } else {
            // Use original function without personalization
            originalSendFunction(message, images)
        }
    }
    
    /**
     * Show privacy settings screen
     */
    fun showPrivacySettings() {
        _showPrivacySettings.value = true
    }
    
    /**
     * Hide privacy settings screen
     */
    fun hidePrivacySettings() {
        _showPrivacySettings.value = false
    }
    
    /**
     * Toggle personalization on/off
     */
    fun togglePersonalization() {
        _isPersonalizationEnabled.value = !_isPersonalizationEnabled.value
        personalizedAIClient.setPersonalizationEnabled(_isPersonalizationEnabled.value)
    }
    
    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(
        name: String? = null,
        interests: List<String>? = null,
        goals: List<String>? = null,
        communicationStyle: String? = null
    ) {
        val currentProfile = userDataManager.userProfile.value
        val updatedProfile = currentProfile?.copy(
            name = name ?: currentProfile.name,
            interests = interests ?: currentProfile.interests,
            goals = goals ?: currentProfile.goals,
            communicationStyle = communicationStyle ?: currentProfile.communicationStyle,
            lastUpdated = System.currentTimeMillis()
        ) ?: com.craftflowtechnologies.guidelens.storage.UserProfile(
            name = name ?: "",
            interests = interests ?: emptyList(),
            goals = goals ?: emptyList(),
            communicationStyle = communicationStyle ?: "friendly"
        )
        
        userDataManager.saveUserProfile(updatedProfile)
    }
    
    /**
     * Record tool usage for analytics
     */
    suspend fun recordToolUsage(agentId: String, toolId: String, duration: Long) {
        userDataManager.recordToolUsage(agentId, toolId, duration)
    }
    
    /**
     * Record mood entry for Buddy agent
     */
    suspend fun recordMoodEntry(moodValue: Int, moodLabel: String, notes: String = "", triggers: List<String> = emptyList()) {
        val moodEntry = com.craftflowtechnologies.guidelens.storage.MoodEntry(
            moodValue = moodValue,
            moodLabel = moodLabel,
            notes = notes,
            triggers = triggers
        )
        userDataManager.saveMoodEntry(moodEntry)
    }
    
    /**
     * Save cooking profile information
     */
    suspend fun saveCookingProfile(
        skillLevel: String,
        preferredCuisines: List<String> = emptyList(),
        dietaryRestrictions: List<String> = emptyList(),
        allergies: List<String> = emptyList(),
        cookingGoals: List<String> = emptyList()
    ) {
        val cookingProfile = com.craftflowtechnologies.guidelens.storage.UserCookingProfile(
            skillLevel = skillLevel,
            preferredCuisines = preferredCuisines,
            dietaryRestrictions = dietaryRestrictions,
            allergies = allergies,
            cookingGoals = cookingGoals
        )
        userDataManager.saveCookingProfile(cookingProfile)
    }
    
    /**
     * Get personalized context for debugging/monitoring
     */
    suspend fun getPersonalizedContext(): String {
        return userDataManager.generateAIContext()
    }
    
    /**
     * Map Agent ID to AgentType enum
     */
    private fun mapAgentToType(agentId: String): AgentType {
        return when (agentId) {
            "cooking" -> AgentType.COOKING
            "crafting" -> AgentType.CRAFTING
            "diy" -> AgentType.DIY
            "companion", "buddy" -> AgentType.BUDDY
            else -> AgentType.BUDDY // Default fallback
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        if (::personalizedAIClient.isInitialized) {
            personalizedAIClient.cleanup()
        }
        userDataManager.cleanup()
    }
}

/**
 * Composable function to provide the PersonalizedGuideLensIntegration to the component tree
 */
@Composable
fun rememberPersonalizedGuideLensIntegration(
    context: Context,
    coroutineScope: CoroutineScope
): PersonalizedGuideLensIntegration {
    return remember(context, coroutineScope) {
        PersonalizedGuideLensIntegration(context, coroutineScope)
    }
}

/**
 * Enhanced agent selector that includes personalization indicators
 */
@Composable
fun PersonalizedAgentInfo(
    agent: Agent,
    personalizedIntegration: PersonalizedGuideLensIntegration
) {
    val toolRecommendations = remember { mutableStateOf(emptyList<com.craftflowtechnologies.guidelens.ai.ToolRecommendation>()) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(agent) {
        coroutineScope.launch {
            try {
                toolRecommendations.value = personalizedIntegration.getToolRecommendations(agent)
            } catch (e: Exception) {
                // Handle error gracefully
                toolRecommendations.value = emptyList()
            }
        }
    }
    
    // Display personalized info about the agent
    if (toolRecommendations.value.isNotEmpty()) {
        val highPriorityTools = toolRecommendations.value
            .filter { it.priority == com.craftflowtechnologies.guidelens.ai.Priority.HIGH }
        
        if (highPriorityTools.isNotEmpty()) {
            // This could be used to show personalized suggestions in the UI
            // For now, it's just stored for potential use
        }
    }
}