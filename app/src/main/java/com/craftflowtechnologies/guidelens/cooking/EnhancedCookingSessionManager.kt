package com.craftflowtechnologies.guidelens.cooking

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



@Serializable
data class EnhancedCookingSession(
    val sessionId: String,
    val userId: String,
    val artifact: Artifact,
    val startTime: Long = System.currentTimeMillis(),
    val currentStageIndex: Int = 0,
    val isActive: Boolean = true,
    val isPaused: Boolean = false,
    
    // Enhanced context tracking
    val sessionContext: SessionContext = SessionContext(),
    val aiInteractions: List<AIInteraction> = emptyList(),
    val userModifications: List<SessionModification> = emptyList(),
    val progressSnapshots: List<ProgressSnapshot> = emptyList(),
    val environmentalContext: Map<String, String> = emptyMap()
)

@Serializable
data class SessionContext(
    val conversationHistory: List<ChatMessage> = emptyList(),
    val activeTimers: Map<String, TimerState> = emptyMap(),
    val completedStages: Set<String> = emptySet(),
    val currentFocus: String? = null, // What the user is currently working on
    val difficultyAdjustments: List<DifficultyAdjustment> = emptyList(),
    val userEmotionalState: EmotionalState = EmotionalState.NEUTRAL,
    val sessionGoals: List<String> = emptyList(),
    val knowledgeGained: List<String> = emptyList()
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val stageIndex: Int,
    val messageType: MessageType = MessageType.TEXT,
    val attachments: List<String> = emptyList(), // Image URLs, etc.
    val contextTags: List<String> = emptyList() // technique, help_request, progress_update, etc.
)

@Serializable
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

@Serializable
enum class MessageType {
    TEXT, IMAGE, VOICE, PROGRESS_UPDATE, SYSTEM_NOTIFICATION
}

@Serializable
data class AIInteraction(
    val id: String,
    val type: InteractionType,
    val trigger: String, // What caused this interaction
    val prompt: String,
    val response: String,
    val confidence: Float,
    val timestamp: Long,
    val stageIndex: Int,
    val outcome: InteractionOutcome = InteractionOutcome.HELPFUL
)

@Serializable
enum class InteractionType {
    GUIDANCE_REQUEST, PROGRESS_ANALYSIS, TECHNIQUE_EXPLANATION, 
    TROUBLESHOOTING, ENCOURAGEMENT, WARNING, SUGGESTION
}

@Serializable
enum class InteractionOutcome {
    HELPFUL, NEUTRAL, CONFUSING, IGNORED
}

@Serializable
data class SessionModification(
    val id: String,
    val type: ModificationType,
    val originalInstruction: String,
    val modifiedInstruction: String,
    val reason: String,
    val timestamp: Long,
    val stageIndex: Int
)

@Serializable
data class ProgressSnapshot(
    val timestamp: Long,
    val stageIndex: Int,
    val imageUrl: String?,
    val aiAnalysis: AIAnalysis?,
    val userNote: String?,
    val confidenceScore: Float,
    val nextStepRecommendation: String?
)

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
    CONFIDENT, NEUTRAL, CONFUSED, FRUSTRATED, EXCITED, TIRED
}

class EnhancedCookingSessionManager(
    private val artifactRepository: ArtifactRepository,
    private val contextManager: ArtifactContextManager,
    private val progressAnalysisSystem: ProgressAnalysisSystem,
    private val imageGenerator: ArtifactImageGenerator,
    private val creditsManager: CreditsManager,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "EnhancedCookingSessionManager"
    }
    
    private val _currentSession = MutableStateFlow<EnhancedCookingSession?>(null)
    val currentSession: StateFlow<EnhancedCookingSession?> = _currentSession.asStateFlow()
    
    private val _sessionHistory = MutableStateFlow<List<EnhancedCookingSession>>(emptyList())
    val sessionHistory: StateFlow<List<EnhancedCookingSession>> = _sessionHistory.asStateFlow()
    
    /**
     * Start an enhanced cooking session with full context awareness
     */
    suspend fun startEnhancedSession(
        userId: String,
        artifact: Artifact,
        environmentalContext: Map<String, String> = emptyMap()
    ): Result<EnhancedCookingSession> {
        return try {
            // Check for similar sessions and provide context
            val similarSessions = findSimilarSessions(userId, artifact)
            val contextualSuggestions = if (similarSessions.isNotEmpty()) {
                contextManager.generateContextualSuggestions(
                    userId = userId,
                    query = artifact.title,
                    matches = similarSessions.map { session ->
                        ArtifactMatch(
                            artifactId = session.artifact.id,
                            similarity = 0.9f,
                            matchType = MatchType.SIMILAR_RECIPE,
                            title = session.artifact.title,
                            description = session.artifact.description,
                            lastUsed = session.startTime,
                            usageCount = 1,
                            userRating = null
                        )
                    }
                )
            } else emptyList()
            
            val sessionId = java.util.UUID.randomUUID().toString()
            
            val session = EnhancedCookingSession(
                sessionId = sessionId,
                userId = userId,
                artifact = artifact,
                environmentalContext = environmentalContext,
                sessionContext = SessionContext(
                    sessionGoals = determineSessionGoals(artifact, contextualSuggestions)
                )
            )
            
            _currentSession.value = session
            
            // Initialize AI with context
            initializeAIContext(session)
            
            // Update cooking context
            contextManager.updateCookingContext(
                userId = userId,
                sessionId = sessionId,
                artifact = artifact,
                sessionData = mapOf(
                    "start_time" to session.startTime,
                    "session_type" to "enhanced_cooking"
                )
            )
            
            Log.d(TAG, "Started enhanced cooking session: $sessionId")
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting enhanced session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Process user message with full context awareness
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
            // Add user message to conversation history
            val userMessage = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                role = MessageRole.USER,
                content = message,
                timestamp = System.currentTimeMillis(),
                stageIndex = session.currentStageIndex,
                messageType = messageType,
                attachments = attachments,
                contextTags = classifyMessage(message, session)
            )
            
            // Update session with new message
            val updatedSession = session.copy(
                sessionContext = session.sessionContext.copy(
                    conversationHistory = session.sessionContext.conversationHistory + userMessage
                )
            )
            _currentSession.value = updatedSession
            
            // Analyze emotional state if needed
            val emotionalState = analyzeEmotionalState(message, session.sessionContext.conversationHistory)
            if (emotionalState != session.sessionContext.userEmotionalState) {
                updateEmotionalState(emotionalState)
            }
            
            // Generate contextual response
            val aiResponse = generateContextualResponse(message, updatedSession)
            
            // Add AI response to conversation
            val assistantMessage = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                role = MessageRole.ASSISTANT,
                content = aiResponse,
                timestamp = System.currentTimeMillis(),
                stageIndex = session.currentStageIndex,
                contextTags = listOf("contextual_response")
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
     * Process progress image with AI analysis and context updates
     */
    suspend fun processProgressImage(
        imageUri: android.net.Uri,
        userNote: String? = null
    ): Result<ProgressSnapshot> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )
        
        return try {
            // Get current stage information
            val currentStage = session.artifact.stageImages.getOrNull(session.currentStageIndex)
            val stageDescription = when (val content = session.artifact.contentData) {
                is ArtifactContent.RecipeContent -> {
                    content.recipe.steps.getOrNull(session.currentStageIndex)?.description ?: ""
                }
                else -> ""
            }
            
            // Analyze progress with AI
            val analysisRequest = ProgressAnalysisRequest(
                artifactId = session.artifact.id,
                currentStageIndex = session.currentStageIndex,
                stepDescription = stageDescription,
                expectedOutcome = currentStage?.description ?: "",
                visualCues = extractVisualCues(session.artifact, session.currentStageIndex),
                userImagePath = imageUri.toString(),
                contextualInfo = buildContextualInfo(session)
            )
            
            // For this example, we'll create a mock bitmap - in real implementation,
            // you'd load the actual bitmap from the URI
            val mockBitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
            
            val analysisResult = progressAnalysisSystem.analyzeProgress(analysisRequest, mockBitmap)
            
            // Create progress snapshot
            val snapshot = ProgressSnapshot(
                timestamp = System.currentTimeMillis(),
                stageIndex = session.currentStageIndex,
                imageUrl = imageUri.toString(),
                aiAnalysis = AIAnalysis(
                    confidenceScore = analysisResult.confidenceScore,
                    feedback = analysisResult.overallFeedback,
                    suggestions = analysisResult.suggestions,
                    adjustments = analysisResult.stageAdjustments
                ),
                userNote = userNote,
                confidenceScore = analysisResult.confidenceScore,
                nextStepRecommendation = analysisResult.nextStepRecommendations.firstOrNull()
            )
            
            // Update session with progress snapshot
            val updatedSession = session.copy(
                progressSnapshots = session.progressSnapshots + snapshot
            )
            _currentSession.value = updatedSession
            
            // Generate follow-up response based on analysis
            val followUpMessage = generateProgressFollowUp(analysisResult, session)
            if (followUpMessage.isNotEmpty()) {
                // Add follow-up message to conversation
                processSystemMessage(followUpMessage, "progress_analysis")
            }
            
            Result.success(snapshot)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing progress image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Request on-demand image generation during session
     */
    suspend fun requestSessionImage(
        prompt: String,
        stageIndex: Int = _currentSession.value?.currentStageIndex ?: 0
    ): Result<String> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )
        
        return try {
            // Check credits
            val canAfford = creditsManager.canAfford(
                userId = session.userId,
                feature = "on_demand_image",
                quality = "standard"
            )
            
            if (!canAfford.getOrDefault(false)) {
                return Result.failure(Exception("Insufficient credits for image generation"))
            }
            
            // Generate image
            val imageResult = imageGenerator.generateOnDemandImage(
                prompt = prompt,
                userId = session.userId,
                artifactId = session.artifact.id,
                stageIndex = stageIndex
            )
            
            if (imageResult.success && imageResult.image != null) {
                // Spend credits
                creditsManager.spendCredits(
                    userId = session.userId,
                    feature = "on_demand_image",
                    artifactId = session.artifact.id,
                    metadata = mapOf(
                        "prompt" to prompt,
                        "stage_index" to stageIndex.toString()
                    )
                )
                
                // Add system message about image generation
                processSystemMessage(
                    "I've generated an image to help with your current step: ${imageResult.image.url}",
                    "image_generated"
                )
                
                Result.success(imageResult.image.url ?: "")
            } else {
                Result.failure(Exception(imageResult.error ?: "Failed to generate image"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting session image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete current stage and move to next
     */
    suspend fun completeCurrentStage(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val stageId = session.currentStageIndex.toString()
            val isLastStage = session.currentStageIndex >= session.artifact.stageImages.size - 1
            
            val updatedSession = session.copy(
                currentStageIndex = if (isLastStage) session.currentStageIndex else session.currentStageIndex + 1,
                sessionContext = session.sessionContext.copy(
                    completedStages = session.sessionContext.completedStages + stageId,
                    currentFocus = if (isLastStage) "session_completion" else "next_stage"
                )
            )
            
            _currentSession.value = updatedSession
            
            // Generate stage completion message
            val completionMessage = if (isLastStage) {
                generateCompletionMessage(session)
            } else {
                generateStageTransitionMessage(session, updatedSession.currentStageIndex)
            }
            
            processSystemMessage(completionMessage, "stage_completion")
            
            // If session complete, finalize it
            if (isLastStage) {
                finalizeSession()
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error completing current stage", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pause session while maintaining full context
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
            
            // Save session state for resumption
            saveSessionState(pausedSession)
            
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
     * Resume paused session with full context restoration
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
            
            // Generate context-aware resume message
            val resumeMessage = generateResumeMessage(session)
            processSystemMessage(resumeMessage, "session_resumed")
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming session", e)
            Result.failure(e)
        }
    }
    
    /**
     * Navigate to next step
     */
    suspend fun nextStep(): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val recipe = (session.artifact.contentData as? ArtifactContent.RecipeContent)?.recipe
            val totalSteps = recipe?.steps?.size ?: 0
            
            if (session.currentStageIndex < totalSteps - 1) {
                val newStepIndex = session.currentStageIndex + 1
                
                // Update session state
                val updatedSession = session.copy(
                    currentStageIndex = newStepIndex
                )
                _currentSession.value = updatedSession
                
                // Update artifact progress in database
                try {
                    val updatedArtifact = session.artifact.copy(
                        currentProgress = session.artifact.currentProgress?.copy(
                            currentStageIndex = newStepIndex
                        ) ?: ArtifactProgress(
                            currentStageIndex = newStepIndex,
                            completedStages = session.sessionContext.completedStages
                        )
                    )
                    val saveResult = artifactRepository.saveArtifact(updatedArtifact)
                    if (saveResult.isSuccess) {
                        Log.d(TAG, "Updated artifact progress to step ${newStepIndex + 1}")
                    } else {
                        Log.w(TAG, "Failed to save artifact progress: ${saveResult.exceptionOrNull()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update artifact progress in database", e)
                }
                
                val nextStepMessage = "Moving to step ${newStepIndex + 1}"
                processSystemMessage(nextStepMessage, "step_navigation")
                
                Log.d(TAG, "Successfully moved to step ${newStepIndex + 1}")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Already at last step (${session.currentStageIndex + 1}/$totalSteps)")
                Result.failure(IllegalStateException("Already at last step"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next step", e)
            Result.failure(e)
        }
    }
    
    /**
     * Navigate to previous step
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
     * Complete a specific step and mark it as done
     */
    suspend fun completeStep(stepId: String): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(
            IllegalStateException("No active session")
        )

        return try {
            val completedStages = session.sessionContext.completedStages + stepId
            
            // Update session state
            val updatedSession = session.copy(
                sessionContext = session.sessionContext.copy(
                    completedStages = completedStages
                )
            )
            _currentSession.value = updatedSession
            
            // Update artifact progress in database
            try {
                val updatedArtifact = session.artifact.copy(
                    currentProgress = session.artifact.currentProgress?.copy(
                        completedStages = completedStages
                    ) ?: ArtifactProgress(
                        currentStageIndex = session.currentStageIndex,
                        completedStages = completedStages
                    )
                )
                val saveResult = artifactRepository.saveArtifact(updatedArtifact)
                if (saveResult.isSuccess) {
                    Log.d(TAG, "Updated completed stages in database: ${completedStages.size} stages")
                } else {
                    Log.w(TAG, "Failed to save completed stages: ${saveResult.exceptionOrNull()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update completed stages in database", e)
            }
            
            val completionMessage = "Step $stepId completed! Great work!"
            processSystemMessage(completionMessage, "step_completed")
            
            Log.d(TAG, "Successfully completed step: $stepId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error completing step", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private suspend fun findSimilarSessions(
        userId: String,
        artifact: Artifact
    ): List<EnhancedCookingSession> {
        // TODO: Implement similar session detection
        return emptyList()
    }
    
    private fun determineSessionGoals(
        artifact: Artifact,
        suggestions: List<ContextualSuggestion>
    ): List<String> {
        val goals = mutableListOf<String>()
        
        // Add artifact-specific goals
        when (artifact.type) {
            ArtifactType.RECIPE -> {
                goals.add("Successfully complete the recipe")
                goals.add("Learn new cooking techniques")
            }
            else -> {
                goals.add("Complete the project successfully")
            }
        }
        
        // Add context-based goals from suggestions
//        suggestions.forEach { suggestion ->
//            when (suggestion.type) {
//                SuggestionType.LEARN_FROM_HISTORY -> {
//                    goals.add("Improve on previous attempts")
//                }
//                else -> {}
//            }
//        }
        
        return goals
    }
    
    private suspend fun initializeAIContext(session: EnhancedCookingSession) {
        // Initialize AI with session context
        val initMessage = "Starting cooking session for '${session.artifact.title}'. " +
                "I'll help you throughout the process with contextual guidance."
        
        processSystemMessage(initMessage, "session_start")
    }
    
    private fun classifyMessage(message: String, session: EnhancedCookingSession): List<String> {
        val tags = mutableListOf<String>()
        
        // Basic classification
        when {
            message.contains("help", ignoreCase = true) -> tags.add("help_request")
            message.contains("done", ignoreCase = true) -> tags.add("completion_signal")
            message.contains("problem", ignoreCase = true) -> tags.add("issue_report")
            message.contains("how", ignoreCase = true) -> tags.add("technique_question")
            message.contains("why", ignoreCase = true) -> tags.add("explanation_request")
        }
        
        return tags
    }
    
    private fun analyzeEmotionalState(
        message: String,
        history: List<ChatMessage>
    ): EmotionalState {
        // Simple emotion detection based on keywords and patterns
        return when {
            message.contains(Regex("frustrated|stuck|difficult|hard|can't", RegexOption.IGNORE_CASE)) -> 
                EmotionalState.FRUSTRATED
            message.contains(Regex("excited|great|awesome|love", RegexOption.IGNORE_CASE)) -> 
                EmotionalState.EXCITED
            message.contains(Regex("confused|don't understand|not sure", RegexOption.IGNORE_CASE)) -> 
                EmotionalState.CONFUSED
            message.contains(Regex("confident|easy|got it|perfect", RegexOption.IGNORE_CASE)) -> 
                EmotionalState.CONFIDENT
            else -> EmotionalState.NEUTRAL
        }
    }
    
    private suspend fun updateEmotionalState(newState: EmotionalState) {
        val session = _currentSession.value ?: return
        
        val updatedSession = session.copy(
            sessionContext = session.sessionContext.copy(
                userEmotionalState = newState
            )
        )
        _currentSession.value = updatedSession
        
        // Adjust AI response style based on emotional state
        when (newState) {
            EmotionalState.FRUSTRATED -> {
                processSystemMessage(
                    "I notice you might be having some difficulty. Don't worry, I'm here to help!",
                    "emotional_support"
                )
            }
            EmotionalState.EXCITED -> {
                processSystemMessage(
                    "I love your enthusiasm! Let's keep this momentum going!",
                    "positive_reinforcement"
                )
            }
            else -> {} // No specific response needed
        }
    }
    
    private suspend fun generateContextualResponse(
        message: String,
        session: EnhancedCookingSession
    ): String {
        // Build context for AI response
        val context = CookingContext(
            recipe = (session.artifact.contentData as ArtifactContent.RecipeContent).recipe,
            currentStepIndex = session.currentStageIndex,
            timeElapsed = System.currentTimeMillis() - session.startTime,
            completedSteps = session.sessionContext.completedStages.toList(),
            userModifications = session.userModifications.map { it.reason }
        )
        
        val request = InteractiveGuidanceRequest(
            userMessage = message,
            currentContext = context,
            previousAnalyses = session.progressSnapshots.mapNotNull { it.aiAnalysis },
            userPreferences = mapOf(
                "emotional_state" to session.sessionContext.userEmotionalState.name.lowercase(),
                "communication_style" to "encouraging"
            )
        )
        
        val response = progressAnalysisSystem.provideInteractiveGuidance(request)
        return response.response
    }
    
    private suspend fun processSystemMessage(message: String, type: String) {
        val session = _currentSession.value ?: return
        
        val systemMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            role = MessageRole.SYSTEM,
            content = message,
            timestamp = System.currentTimeMillis(),
            stageIndex = session.currentStageIndex,
            contextTags = listOf(type)
        )
        
        val updatedSession = session.copy(
            sessionContext = session.sessionContext.copy(
                conversationHistory = session.sessionContext.conversationHistory + systemMessage
            )
        )
        _currentSession.value = updatedSession
    }
    
    private fun extractVisualCues(artifact: Artifact, stageIndex: Int): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.getOrNull(stageIndex)?.visualCues ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun buildContextualInfo(session: EnhancedCookingSession): Map<String, String> {
        return mapOf(
            "emotional_state" to session.sessionContext.userEmotionalState.name,
            "session_duration" to ((System.currentTimeMillis() - session.startTime) / 60000).toString(),
            "stages_completed" to session.sessionContext.completedStages.size.toString(),
            "total_stages" to session.artifact.stageImages.size.toString()
        )
    }
    
    private fun generateProgressFollowUp(
        analysis: ProgressAnalysisResult,
        session: EnhancedCookingSession
    ): String {
        return when {
            analysis.needsIntervention -> 
                "I see you might need some guidance here. ${analysis.suggestions.firstOrNull() ?: "Let me help you with this step."}"
            analysis.confidenceScore > 0.8f -> 
                "Excellent work! You're doing great. ${analysis.nextStepRecommendations.firstOrNull() ?: ""}"
            else -> 
                "You're making good progress! ${analysis.overallFeedback}"
        }
    }
    
    private fun generateCompletionMessage(session: EnhancedCookingSession): String {
        return "Congratulations! You've successfully completed '${session.artifact.title}'! " +
               "How do you feel about the final result?"
    }
    
    private fun generateStageTransitionMessage(session: EnhancedCookingSession, nextStageIndex: Int): String {
        return "Great job completing that stage! Now let's move on to step ${nextStageIndex + 1}."
    }
    
    private fun generateResumeMessage(session: EnhancedCookingSession): String {
        val timeElapsed = (System.currentTimeMillis() - session.startTime) / 60000 // minutes
        return "Welcome back! We were working on stage ${session.currentStageIndex + 1} of '${session.artifact.title}'. " +
               "You've been cooking for about $timeElapsed minutes. Ready to continue?"
    }
    
    private suspend fun saveSessionState(session: EnhancedCookingSession) {
        // TODO: Implement session state persistence
        Log.d(TAG, "Saving session state: ${session.sessionId}")
    }
    
    private suspend fun finalizeSession() {
        val session = _currentSession.value ?: return
        
        try {
            // Update session history
            _sessionHistory.value = _sessionHistory.value + session
            
            // Update cooking context with completion data
            contextManager.updateCookingContext(
                userId = session.userId,
                sessionId = session.sessionId,
                artifact = session.artifact,
                sessionData = mapOf(
                    "end_time" to System.currentTimeMillis(),
                    "completed" to true,
                    "stages_completed" to session.sessionContext.completedStages.size,
                    "avg_confidence" to session.progressSnapshots.map { it.confidenceScore }.average().toFloat()
                )
            )
            
            // Clear current session
            _currentSession.value = null
            
            Log.d(TAG, "Session finalized: ${session.sessionId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing session", e)
        }
    }
}