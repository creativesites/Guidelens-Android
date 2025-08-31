package com.craftflowtechnologies.guidelens.universal

import android.util.Log
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.ai.ProgressAnalysisSystem
import com.craftflowtechnologies.guidelens.ai.InteractiveGuidanceRequest
import com.craftflowtechnologies.guidelens.ai.InteractiveGuidanceResponse
import com.craftflowtechnologies.guidelens.ai.CookingContext
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.CookingStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Universal agent integration layer that routes requests to appropriate AI agents
 */
class UniversalAgentIntegration(
    private val geminiClient: EnhancedGeminiClient,
    private val progressAnalysisSystem: ProgressAnalysisSystem
) {
    
    /**
     * Generate agent-specific response based on user message and session context
     */
    suspend fun generateResponse(
        message: String,
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            val agentPrompt = buildAgentPrompt(message, session, contentAdapter)
            val contextData = buildContextData(session, contentAdapter)
            
            val response = geminiClient.generateContextualResponse(
                prompt = agentPrompt,
                contextData = contextData,
                agentType = session.agentType
            )
            
            if (response.isSuccess) {
                val responseText = response.getOrNull() ?: "I'm here to help you continue with your ${session.agentType} session!"
                Result.success(responseText)
            } else {
                Log.e(TAG, "Failed to generate agent response", response.exceptionOrNull())
                Result.success(getFallbackResponse(session.agentType, message))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating agent response", e)
            Result.success(getFallbackResponse(session.agentType, message))
        }
    }
    
    /**
     * Process progress analysis for any artifact type
     */
    suspend fun analyzeProgress(
        session: UniversalArtifactSession,
        progressMessage: String,
        contentAdapter: ContentAdapter
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            // Create a universal guidance request
            val guidanceRequest = createUniversalGuidanceRequest(session, progressMessage, contentAdapter)
            
            val guidanceResponse = progressAnalysisSystem.provideInteractiveGuidance(guidanceRequest)
            
            Result.success(guidanceResponse.response)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing progress", e)
            Result.success("Great progress! Keep up the good work on your ${session.agentType} project.")
        }
    }
    
    /**
     * Generate contextual help for any agent type
     */
    suspend fun generateContextualHelp(
        helpMessage: String,
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            val helpPrompt = buildContextualHelpPrompt(helpMessage, session, contentAdapter)
            val contextData = buildHelpContextData(session, contentAdapter)
            
            val response = geminiClient.generateContextualResponse(
                prompt = helpPrompt,
                contextData = contextData,
                agentType = session.agentType
            )
            
            if (response.isSuccess) {
                Result.success(response.getOrNull() ?: getDefaultHelpResponse(session.agentType))
            } else {
                Result.success(getDefaultHelpResponse(session.agentType))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating contextual help", e)
            Result.success(getDefaultHelpResponse(session.agentType))
        }
    }
    
    /**
     * Get agent-specific system prompts
     */
    fun getSystemPrompt(agentType: String, context: String = ""): String {
        val basePrompt = when (agentType.lowercase()) {
            "cooking" -> COOKING_SYSTEM_PROMPT
            "crafting" -> CRAFTING_SYSTEM_PROMPT
            "diy" -> DIY_SYSTEM_PROMPT
            "buddy" -> BUDDY_SYSTEM_PROMPT
            else -> UNIVERSAL_SYSTEM_PROMPT
        }
        
        return if (context.isNotEmpty()) {
            "$basePrompt\n\nAdditional Context: $context"
        } else {
            basePrompt
        }
    }
    
    // Private helper methods
    private fun buildAgentPrompt(
        message: String,
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): String {
        val systemPrompt = getSystemPrompt(session.agentType)
        val currentStepData = UniversalStepData.fromAdapter(contentAdapter, session.artifact, session.currentStageIndex)
        
        return """
            $systemPrompt
            
            CURRENT SESSION CONTEXT:
            - Project: ${session.artifact.title}
            - Agent Type: ${session.agentType}
            - Current Step: ${session.currentStageIndex + 1} of ${contentAdapter.getTotalSteps(session.artifact)}
            - Step Title: ${currentStepData.title}
            - Step Description: ${currentStepData.description}
            - Session Duration: ${(System.currentTimeMillis() - session.startTime) / 60000} minutes
            - Completed Steps: ${session.sessionContext.completedStages.size}
            - User Emotional State: ${session.sessionContext.userEmotionalState}
            
            STEP DETAILS:
            - Duration: ${currentStepData.duration?.let { "${it} minutes" } ?: "Not specified"}
            - Techniques: ${currentStepData.techniques.joinToString(", ").takeIf { it.isNotEmpty() } ?: "None"}
            - Required Items: ${currentStepData.requiredItems.joinToString(", ").takeIf { it.isNotEmpty() } ?: "None"}
            
            USER MESSAGE: "$message"
            
            Please provide a helpful, contextual response that:
            1. Addresses their specific question or need
            2. Considers their current progress and step
            3. Matches the ${session.agentType} agent personality
            4. Provides actionable guidance
            5. Is encouraging and supportive
        """.trimIndent()
    }
    
    private fun buildContextData(
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): Map<String, String> {
        return mapOf(
            "agent_type" to session.agentType,
            "artifact_type" to session.artifact.type.toString(),
            "difficulty" to session.artifact.difficulty,
            "total_steps" to contentAdapter.getTotalSteps(session.artifact).toString(),
            "current_step" to (session.currentStageIndex + 1).toString(),
            "completed_steps" to session.sessionContext.completedStages.size.toString(),
            "session_duration_minutes" to ((System.currentTimeMillis() - session.startTime) / 60000).toString(),
            "emotional_state" to session.sessionContext.userEmotionalState.toString(),
            "is_paused" to session.isPaused.toString()
        )
    }
    
    private fun createUniversalGuidanceRequest(
        session: UniversalArtifactSession,
        message: String,
        contentAdapter: ContentAdapter
    ): InteractiveGuidanceRequest {
        // Convert universal session to cooking context for now
        // In a full implementation, we'd have universal context classes
        val cookingContext = when (val content = session.artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                CookingContext(
                    recipe = content.recipe,
                    currentStepIndex = session.currentStageIndex,
                    timeElapsed = System.currentTimeMillis() - session.startTime,
                    completedSteps = session.sessionContext.completedStages.toList()
                )
            }
            else -> {
                // Create a mock context for non-cooking artifacts
                // In production, we'd have proper universal context handling
                CookingContext(
                    recipe = createMockRecipe(session.artifact, contentAdapter),
                    currentStepIndex = session.currentStageIndex,
                    timeElapsed = System.currentTimeMillis() - session.startTime,
                    completedSteps = session.sessionContext.completedStages.toList()
                )
            }
        }
        
        return InteractiveGuidanceRequest(
            userMessage = message,
            currentContext = cookingContext,
            previousAnalyses = emptyList(), // Could be enhanced with actual analysis history
            userPreferences = mapOf(
                "agent_type" to session.agentType,
                "emotional_state" to session.sessionContext.userEmotionalState.toString(),
                "communication_style" to "encouraging"
            )
        )
    }
    
    private fun buildContextualHelpPrompt(
        helpMessage: String,
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): String {
        val currentStepData = UniversalStepData.fromAdapter(contentAdapter, session.artifact, session.currentStageIndex)
        
        return """
            ${getSystemPrompt(session.agentType)}
            
            HELP REQUEST CONTEXT:
            - Project: ${session.artifact.title}
            - Current Step: ${session.currentStageIndex + 1} - ${currentStepData.title}
            - Step Description: ${currentStepData.description}
            - Difficulty: ${session.artifact.difficulty}
            - User Request: "$helpMessage"
            
            STEP DETAILS:
            - Techniques Required: ${currentStepData.techniques.joinToString(", ")}
            - Tips Available: ${currentStepData.tips.joinToString("; ")}
            - Duration: ${currentStepData.duration?.let { "${it} minutes" } ?: "Variable"}
            
            Please provide specific, actionable help that:
            1. Directly addresses their question
            2. Breaks down complex concepts into simple steps
            3. Includes safety considerations (especially for DIY)
            4. Offers alternatives or troubleshooting tips
            5. Encourages them to continue
        """.trimIndent()
    }
    
    private fun buildHelpContextData(
        session: UniversalArtifactSession,
        contentAdapter: ContentAdapter
    ): Map<String, String> {
        return buildContextData(session, contentAdapter) + mapOf(
            "help_type" to "contextual_assistance",
            "user_needs_support" to "true"
        )
    }
    
    private fun getFallbackResponse(agentType: String, message: String): String {
        return when (agentType.lowercase()) {
            "cooking" -> "I'm here to help with your cooking! Let me guide you through this step."
            "crafting" -> "Let's work on your craft project together! I'll help you through each step."
            "diy" -> "Safety first! I'm here to help you complete your DIY project successfully."
            "buddy" -> "I'm here to support your learning journey! Let's figure this out together."
            else -> "I'm here to help you with your project! Let's work through this step by step."
        }
    }
    
    private fun getDefaultHelpResponse(agentType: String): String {
        return when (agentType.lowercase()) {
            "cooking" -> "Don't worry! Cooking can be tricky, but I'm here to help. Let's take this step slowly and make sure you understand each part."
            "crafting" -> "Crafting takes patience and practice! Let me break this down into smaller, manageable steps for you."
            "diy" -> "DIY projects can be challenging, but with the right approach and safety measures, we'll get through this together!"
            "buddy" -> "Learning new things can be tough sometimes, but that's totally normal! Let's approach this from a different angle."
            else -> "I understand this might be challenging. Let me help break this down into simpler steps you can follow."
        }
    }
    
    private fun createMockRecipe(artifact: Artifact, contentAdapter: ContentAdapter): Recipe {
        // Create a mock recipe structure for non-cooking artifacts
        // This is a temporary solution - in production we'd have universal context classes
        return Recipe(
            id = artifact.id,
            title = artifact.title,
            description = artifact.description,
            prepTime = 0,
            cookTime = artifact.estimatedDuration ?: 30,
            servings = 1,
            difficulty = artifact.difficulty,
            ingredients = emptyList(),
            steps = (0 until contentAdapter.getTotalSteps(artifact)).map { index ->
                val stepData = UniversalStepData.fromAdapter(contentAdapter, artifact, index)
                CookingStep(
                    id = (index + 1).toString(),
                    stepNumber = index + 1,
                    title = stepData.title,
                    description = stepData.description,
                    duration = stepData.duration,
                    techniques = stepData.techniques,
                    tips = stepData.tips,
                    visualCues = stepData.visualCues,
                    requiredEquipment = stepData.requiredItems
                )
            }
        )
    }
    
    companion object {
        private const val TAG = "UniversalAgentIntegration"
        
        // Agent-specific system prompts
        private const val COOKING_SYSTEM_PROMPT = """
            You are the Cooking Assistant agent in GuideLens. 
            Provide real-time cooking guidance with enthusiasm and expertise.
            Focus on: technique correction, timing, safety, ingredient substitutions.
            Be encouraging, specific, and always prioritize food safety.
            Use cooking terminology appropriately and share helpful tips.
        """
        
        private const val CRAFTING_SYSTEM_PROMPT = """
            You are the Crafting Guru agent in GuideLens.
            Guide users through craft projects with patience and creativity.
            Focus on: technique improvement, tool usage, project troubleshooting.
            Be patient, detail-oriented, and celebrate progress and creativity.
            Encourage experimentation while maintaining project integrity.
        """
        
        private const val DIY_SYSTEM_PROMPT = """
            You are the DIY Helper agent in GuideLens.
            Assist with home improvement and repair projects with safety as the top priority.
            Focus on: safety first, proper tool use, step-by-step guidance.
            Be safety-conscious, methodical, and always emphasize proper safety gear.
            Provide clear measurements and quality checks.
        """
        
        private const val BUDDY_SYSTEM_PROMPT = """
            You are Buddy, the friendly general assistant in GuideLens.
            Help with any skill or learning task not covered by specialized agents.
            Focus on: encouragement, learning support, general guidance, skill assessment.
            Be supportive, adaptable, enthusiastic about learning, and patient.
            Adapt your teaching style to the user's needs and learning pace.
        """
        
        private const val UNIVERSAL_SYSTEM_PROMPT = """
            You are a helpful AI assistant in GuideLens.
            Provide guidance and support for various skill-based tasks.
            Be encouraging, clear, and adapt your communication style to the task at hand.
            Focus on breaking down complex tasks into manageable steps.
        """
    }
}