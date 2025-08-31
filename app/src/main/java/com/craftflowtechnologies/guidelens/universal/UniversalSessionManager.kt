package com.craftflowtechnologies.guidelens.universal

import android.util.Log
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.ai.*
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import com.craftflowtechnologies.guidelens.credits.CreditsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Universal session manager that works across all artifact types and agents
 * Replaces the cooking-specific session manager with a generic implementation
 */
@Serializable
data class UniversalArtifactSession(
    val sessionId: String,
    val userId: String,
    val artifact: Artifact,
    val agentType: String, // cooking, crafting, diy, buddy
    val startTime: Long = System.currentTimeMillis(),
    val currentStageIndex: Int = 0,
    val isActive: Boolean = true,
    val isPaused: Boolean = false,
    
    // Enhanced context tracking - universal across all agents
    val sessionContext: UniversalSessionContext = UniversalSessionContext(),
    val aiInteractions: List<UniversalAIInteraction> = emptyList(),
    val userModifications: List<UniversalSessionModification> = emptyList(),
    val progressSnapshots: List<UniversalProgressSnapshot> = emptyList(),
    val environmentalContext: Map<String, String> = emptyMap()
)

@Serializable
data class UniversalSessionContext(
    val conversationHistory: List<UniversalChatMessage> = emptyList(),
    val activeTimers: Map<String, TimerState> = emptyMap(),
    val completedStages: Set<String> = emptySet(),
    val currentFocus: String? = null, // What the user is currently working on
    val difficultyAdjustments: List<DifficultyAdjustment> = emptyList(),
    val userEmotionalState: EmotionalState = EmotionalState.NEUTRAL,
    val sessionGoals: List<String> = emptyList(),
    val knowledgeGained: List<String> = emptyList(),
    val agentSpecificData: Map<String, String> = emptyMap() // Agent-specific context data
)

@Serializable
data class UniversalChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val stageIndex: Int,
    val messageType: MessageType = MessageType.TEXT,
    val attachments: List<String> = emptyList(),
    val contextTags: List<String> = emptyList(),
    val agentType: String // Which agent this message is for
)

@Serializable
data class UniversalAIInteraction(
    val id: String,
    val type: InteractionType,
    val trigger: String,
    val prompt: String,
    val response: String,
    val confidence: Float,
    val timestamp: Long,
    val stageIndex: Int,
    val outcome: InteractionOutcome = InteractionOutcome.HELPFUL,
    val agentType: String
)

@Serializable
data class UniversalSessionModification(
    val id: String,
    val type: ModificationType,
    val originalInstruction: String,
    val modifiedInstruction: String,
    val reason: String,
    val timestamp: Long,
    val stageIndex: Int,
    val agentType: String
)

@Serializable
data class UniversalProgressSnapshot(
    val timestamp: Long,
    val stageIndex: Int,
    val imageUrl: String?,
    val aiAnalysis: AIAnalysis?,
    val userNote: String?,
    val confidenceScore: Float,
    val nextStepRecommendation: String?,
    val agentType: String
)

@Serializable
enum class ModificationType {
    TIME_EXTENSION, TECHNIQUE_MODIFICATION, INGREDIENT_SUBSTITUTION,
    MATERIAL_SUBSTITUTION, TOOL_ALTERNATIVE, ADDITIONAL_STEP, 
    SKIP_STEP, TEMPERATURE_CHANGE, DIFFICULTY_ADJUSTMENT,
    SAFETY_MODIFICATION, CUSTOMIZATION
}

@Serializable
enum class InteractionType {
    GUIDANCE_REQUEST, PROGRESS_ANALYSIS, TECHNIQUE_EXPLANATION,
    TROUBLESHOOTING, ENCOURAGEMENT, WARNING, SUGGESTION,
    SAFETY_ALERT, CUSTOMIZATION_REQUEST, SKILL_ASSESSMENT
}

@Serializable
enum class InteractionOutcome {
    HELPFUL, NEUTRAL, CONFUSING, IGNORED, VERY_HELPFUL
}

@Serializable
data class DifficultyAdjustment(
    val fromDifficulty: String,
    val toDifficulty: String,
    val reason: String,
    val stageIndex: Int,
    val timestamp: Long
)

@Serializable
enum class EmotionalState {
    CONFIDENT, NEUTRAL, CONFUSED, FRUSTRATED, EXCITED, TIRED, FOCUSED, OVERWHELMED
}

@Serializable
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
enum class MessageType {
    TEXT, IMAGE, VOICE, PROGRESS_UPDATE, SYSTEM_NOTIFICATION
}

@Serializable
data class TimerState(
    val id: String,
    val duration: Long, // milliseconds
    val remainingTime: Long,
    val isRunning: Boolean,
    val isPaused: Boolean,
    val description: String
)

/**
 * Universal session manager that handles all artifact types
 */
class UniversalArtifactSessionManager(
    private val artifactRepository: ArtifactRepository,
    private val contextManager: ArtifactContextManager,
    private val progressAnalysisSystem: ProgressAnalysisSystem,
    private val imageGenerator: ArtifactImageGenerator,
    private val creditsManager: CreditsManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "UniversalArtifactSessionManager"
    }
    
    private val _currentSession = MutableStateFlow<UniversalArtifactSession?>(null)
    val currentSession: StateFlow<UniversalArtifactSession?> = _currentSession.asStateFlow()
    
    private val _sessionHistory = MutableStateFlow<List<UniversalArtifactSession>>(emptyList())
    val sessionHistory: StateFlow<List<UniversalArtifactSession>> = _sessionHistory.asStateFlow()
    
    /**
     * Start a universal session for any artifact type
     */
    suspend fun startSession(
        userId: String,
        artifact: Artifact,
        environmentalContext: Map<String, String> = emptyMap()
    ): Result<UniversalArtifactSession> {
        return try {
            val sessionId = java.util.UUID.randomUUID().toString()
            
            val session = UniversalArtifactSession(
                sessionId = sessionId,
                userId = userId,
                artifact = artifact,
                agentType = artifact.agentType,
                environmentalContext = environmentalContext,
                sessionContext = UniversalSessionContext(
                    sessionGoals = determineSessionGoals(artifact)
                )
            )
            
            _currentSession.value = session
            
            // Initialize AI context for the appropriate agent
            initializeAgentContext(session)
            
            // Update context manager
            contextManager.updateCookingContext(
                userId = userId,
                sessionId = sessionId,
                artifact = artifact,
                sessionData = mapOf(
                    "start_time" to session.startTime,
                    "session_type" to "universal_${artifact.agentType}"
                )
            )
            
            Log.d(TAG, "Started ${artifact.agentType} session: $sessionId")
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting universal session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process user message with agent-aware routing
     */
    suspend fun processUserMessage(
        message: String,
        messageType: MessageType = MessageType.TEXT,
        attachments: List<String> = emptyList()
    ): Result<String> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )
        
        return try {
            val userMessage = UniversalChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                role = MessageRole.USER,
                content = message,
                timestamp = System.currentTimeMillis(),
                stageIndex = session.currentStageIndex,
                messageType = messageType,
                attachments = attachments,
                contextTags = classifyMessage(message, session),
                agentType = session.agentType
            )
            
            // Update session with new message
            val updatedSession = session.copy(
                sessionContext = session.sessionContext.copy(
                    conversationHistory = session.sessionContext.conversationHistory + userMessage
                )
            )
            _currentSession.value = updatedSession
            
            // Generate agent-specific response
            val aiResponse = generateAgentResponse(message, updatedSession)
            
            // Add AI response to conversation
            val assistantMessage = UniversalChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                role = MessageRole.ASSISTANT,
                content = aiResponse,
                timestamp = System.currentTimeMillis(),
                stageIndex = session.currentStageIndex,
                contextTags = listOf("agent_response"),
                agentType = session.agentType
            )
            
            // Update session with AI response
            val finalSession = updatedSession.copy(
                sessionContext = updatedSession.sessionContext.copy(
                    conversationHistory = updatedSession.sessionContext.conversationHistory + assistantMessage
                )
            )
            _currentSession.value = finalSession
            
            Result.success(aiResponse)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing user message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Navigate to next step - universal across all artifact types
     */
    suspend fun nextStep(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val totalSteps = getTotalSteps(session.artifact)
            
            if (session.currentStageIndex < totalSteps - 1) {
                val newStepIndex = session.currentStageIndex + 1
                
                val updatedSession = session.copy(
                    currentStageIndex = newStepIndex
                )
                _currentSession.value = updatedSession
                
                // Update artifact progress
                updateArtifactProgress(session.artifact, newStepIndex, session.sessionContext.completedStages)
                
                val nextStepMessage = "Moving to step ${newStepIndex + 1}"
                processSystemMessage(nextStepMessage, "step_navigation")
                
                Log.d(TAG, "Successfully moved to step ${newStepIndex + 1}")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Already at last step")
                Result.failure(IllegalStateException("Already at last step"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next step", e)
            Result.failure(e)
        }
    }
    
    /**
     * Navigate to previous step - universal across all artifact types
     */
    suspend fun previousStep(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            if (session.currentStageIndex > 0) {
                val updatedSession = session.copy(
                    currentStageIndex = session.currentStageIndex - 1
                )
                _currentSession.value = updatedSession
                
                val prevStepMessage = "Going back to step ${updatedSession.currentStageIndex + 1}"
                processSystemMessage(prevStepMessage, "step_navigation")
                
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Already at first step"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to previous step", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete a specific step - universal implementation
     */
    suspend fun completeStep(stepId: String): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val completedStages = session.sessionContext.completedStages + stepId
            
            val updatedSession = session.copy(
                sessionContext = session.sessionContext.copy(
                    completedStages = completedStages
                )
            )
            _currentSession.value = updatedSession
            
            // Update artifact progress
            updateArtifactProgress(session.artifact, session.currentStageIndex, completedStages)
            
            val completionMessage = "Step $stepId completed! Great work!"
            processSystemMessage(completionMessage, "step_completed")
            
            Log.d(TAG, "Successfully completed step: $stepId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing step", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pause session while maintaining context
     */
    suspend fun pauseSession(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val pausedSession = session.copy(
                isPaused = true,
                sessionContext = session.sessionContext.copy(
                    currentFocus = "paused"
                )
            )
            
            _currentSession.value = pausedSession
            
            processSystemMessage(
                "Session paused. You can resume anytime and I'll remember exactly where we left off!",
                "session_paused"
            )
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Resume paused session
     */
    suspend fun resumeSession(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val resumedSession = session.copy(
                isPaused = false,
                sessionContext = session.sessionContext.copy(
                    currentFocus = "resumed"
                )
            )
            
            _currentSession.value = resumedSession
            
            val resumeMessage = generateResumeMessage(session)
            processSystemMessage(resumeMessage, "session_resumed")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming session", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private fun determineSessionGoals(artifact: Artifact): List<String> {
        val goals = mutableListOf<String>()
        
        when (artifact.type) {
            ArtifactType.RECIPE -> {
                goals.add("Successfully complete the recipe")
                goals.add("Learn new cooking techniques")
            }
            ArtifactType.CRAFT_PROJECT -> {
                goals.add("Complete the craft project")
                goals.add("Master new crafting techniques")
            }
            ArtifactType.DIY_GUIDE -> {
                goals.add("Finish the DIY project safely")
                goals.add("Learn proper tool usage")
            }
            ArtifactType.LEARNING_MODULE -> {
                goals.add("Complete all learning modules")
                goals.add("Understand key concepts")
            }
            ArtifactType.SKILL_TUTORIAL -> {
                goals.add("Master the skill")
                goals.add("Practice until proficient")
            }
        }
        
        return goals
    }
    
    private suspend fun initializeAgentContext(session: UniversalArtifactSession) {
        val initMessage = when (session.agentType) {
            "cooking" -> "Starting cooking session for '${session.artifact.title}'. Let's create something delicious!"
            "crafting" -> "Starting crafting session for '${session.artifact.title}'. Let's make something beautiful!"
            "diy" -> "Starting DIY session for '${session.artifact.title}'. Safety first - let's build something amazing!"
            "buddy" -> "Starting learning session for '${session.artifact.title}'. I'm here to help you learn!"
            else -> "Starting session for '${session.artifact.title}'. Let's work together!"
        }
        
        processSystemMessage(initMessage, "session_start")
    }
    
    private fun classifyMessage(message: String, session: UniversalArtifactSession): List<String> {
        val tags = mutableListOf<String>()
        
        // Universal classifications
        when {
            message.contains("help", ignoreCase = true) -> tags.add("help_request")
            message.contains("done", ignoreCase = true) -> tags.add("completion_signal")
            message.contains("problem", ignoreCase = true) -> tags.add("issue_report")
            message.contains("how", ignoreCase = true) -> tags.add("technique_question")
            message.contains("why", ignoreCase = true) -> tags.add("explanation_request")
        }
        
        // Agent-specific classifications
        when (session.agentType) {
            "cooking" -> {
                when {
                    message.contains("temperature", ignoreCase = true) -> tags.add("temperature_query")
                    message.contains("time", ignoreCase = true) -> tags.add("timing_query")
                }
            }
            "diy" -> {
                when {
                    message.contains("safe", ignoreCase = true) -> tags.add("safety_concern")
                    message.contains("tool", ignoreCase = true) -> tags.add("tool_question")
                }
            }
            "crafting" -> {
                when {
                    message.contains("material", ignoreCase = true) -> tags.add("material_query")
                    message.contains("pattern", ignoreCase = true) -> tags.add("pattern_question")
                }
            }
        }
        
        return tags
    }
    
    private suspend fun generateAgentResponse(
        message: String,
        session: UniversalArtifactSession
    ): String {
        // This would route to the appropriate agent-specific response generator
        return when (session.agentType) {
            "cooking" -> generateCookingResponse(message, session)
            "crafting" -> generateCraftingResponse(message, session)
            "diy" -> generateDIYResponse(message, session)
            "buddy" -> generateBuddyResponse(message, session)
            else -> "I'm here to help you with your ${session.artifact.type.name.lowercase()}!"
        }
    }
    
    private suspend fun generateCookingResponse(message: String, session: UniversalArtifactSession): String {
        // Use existing cooking AI logic
        return "Great question about cooking! Let me help you with that step."
    }
    
    private suspend fun generateCraftingResponse(message: String, session: UniversalArtifactSession): String {
        return "I love helping with crafting projects! Here's what I suggest..."
    }
    
    private suspend fun generateDIYResponse(message: String, session: UniversalArtifactSession): String {
        return "Safety first! For your DIY project, here's my recommendation..."
    }
    
    private suspend fun generateBuddyResponse(message: String, session: UniversalArtifactSession): String {
        return "I'm here to support your learning journey! Let's figure this out together."
    }
    
    private fun getTotalSteps(artifact: Artifact): Int {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> content.recipe.steps.size
            is ArtifactContent.CraftContent -> content.steps.size
            is ArtifactContent.DIYContent -> content.steps.size
            is ArtifactContent.TutorialContent -> content.modules.size
            else -> artifact.stageImages.size
        }
    }
    
    private suspend fun updateArtifactProgress(
        artifact: Artifact,
        currentStageIndex: Int,
        completedStages: Set<String>
    ) {
        try {
            val updatedArtifact = artifact.copy(
                currentProgress = artifact.currentProgress?.copy(
                    currentStageIndex = currentStageIndex,
                    completedStages = completedStages
                ) ?: ArtifactProgress(
                    currentStageIndex = currentStageIndex,
                    completedStages = completedStages
                )
            )
            artifactRepository.saveArtifact(updatedArtifact)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update artifact progress", e)
        }
    }
    
    private suspend fun processSystemMessage(message: String, type: String) {
        val session = _currentSession.value ?: return
        
        val systemMessage = UniversalChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            role = MessageRole.SYSTEM,
            content = message,
            timestamp = System.currentTimeMillis(),
            stageIndex = session.currentStageIndex,
            contextTags = listOf(type),
            agentType = session.agentType
        )
        
        val updatedSession = session.copy(
            sessionContext = session.sessionContext.copy(
                conversationHistory = session.sessionContext.conversationHistory + systemMessage
            )
        )
        _currentSession.value = updatedSession
    }
    
    private fun generateResumeMessage(session: UniversalArtifactSession): String {
        val timeElapsed = (System.currentTimeMillis() - session.startTime) / 60000
        val agentGreeting = when (session.agentType) {
            "cooking" -> "Ready to continue cooking?"
            "crafting" -> "Ready to continue crafting?"
            "diy" -> "Ready to continue building?"
            "buddy" -> "Ready to continue learning?"
            else -> "Ready to continue?"
        }
        
        return "Welcome back! We were working on step ${session.currentStageIndex + 1} of '${session.artifact.title}'. " +
               "You've been working for about $timeElapsed minutes. $agentGreeting"
    }
}