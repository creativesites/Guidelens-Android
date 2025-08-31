package com.craftflowtechnologies.guidelens.ai

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.CookingStep
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class ProgressAnalysisRequest(
    val artifactId: String,
    val currentStageIndex: Int,
    val stepDescription: String,
    val expectedOutcome: String,
    val visualCues: List<String> = emptyList(),
    val userImagePath: String,
    val contextualInfo: Map<String, String> = emptyMap()
)

@Serializable
data class ProgressAnalysisResult(
    val success: Boolean,
    val confidenceScore: Float, // 0.0 to 1.0
    val overallFeedback: String,
    val specificObservations: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val nextStepRecommendations: List<String> = emptyList(),
    val stageAdjustments: List<StageAdjustment> = emptyList(),
    val isOnTrack: Boolean = true,
    val needsIntervention: Boolean = false,
    val estimatedTimeToComplete: Int? = null, // minutes
    val difficultyAdjustment: String? = null, // "easier", "harder", "same"
    val error: String? = null
)

@Serializable
data class InteractiveGuidanceRequest(
    val userMessage: String,
    val currentContext: CookingContext,
    val previousAnalyses: List<AIAnalysis> = emptyList(),
    val userPreferences: Map<String, String> = emptyMap()
)

@Serializable
data class CookingContext(
    val recipe: Recipe,
    val currentStepIndex: Int,
    val timeElapsed: Long, // milliseconds since start
    val completedSteps: List<String>,
    val userModifications: List<String> = emptyList(),
    val environmentalFactors: Map<String, String> = emptyMap() // altitude, humidity, etc.
)

@Serializable
data class InteractiveGuidanceResponse(
    val response: String,
    val actionType: GuidanceActionType,
    val suggestions: List<String> = emptyList(),
    val adjustments: List<StageAdjustment> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val contextUpdate: Map<String, String> = emptyMap()
)

@Serializable
enum class GuidanceActionType {
    CONTINUE, PAUSE, ADJUST_TECHNIQUE, MODIFY_RECIPE, REQUEST_IMAGE, 
    SEEK_CLARIFICATION, PROVIDE_ENCOURAGEMENT, ISSUE_WARNING
}

class ProgressAnalysisSystem(
    private val geminiClient: EnhancedGeminiClient,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "ProgressAnalysisSystem"
        private const val ANALYSIS_PROMPT_TEMPLATE = """
            You are an expert cooking instructor analyzing a student's progress in real-time.
            
            RECIPE CONTEXT:
            - Recipe: %s
            - Current Step: %d of %d
            - Step Description: %s
            - Expected Visual Cues: %s
            - Time Elapsed: %s minutes
            
            ANALYSIS TASK:
            Analyze the provided image of the user's current cooking progress and provide detailed feedback.
            
            Focus on:
            1. How well the current state matches the expected outcome
            2. Proper technique execution
            3. Visual indicators of success or potential issues
            4. Food safety considerations
            5. Timing and doneness assessment
            
            Provide response in JSON format:
            {
              "confidenceScore": 0.0-1.0,
              "overallFeedback": "encouraging and specific feedback",
              "specificObservations": ["observation1", "observation2"],
              "suggestions": ["suggestion1", "suggestion2"],
              "nextStepRecommendations": ["recommendation1"],
              "isOnTrack": true/false,
              "needsIntervention": true/false,
              "estimatedTimeToComplete": minutes_number,
              "difficultyAdjustment": "easier/harder/same"
            }
            
            Be encouraging but precise. If you see potential issues, provide clear guidance on how to correct them.
        """
        
        private const val INTERACTIVE_GUIDANCE_PROMPT = """
            You are an expert cooking assistant providing real-time guidance during cooking sessions.
            You maintain full context of the cooking session and provide personalized, contextual responses.
            
            CURRENT COOKING CONTEXT:
            - Recipe: %s
            - Current Step: %d of %d (%s)
            - Time Cooking: %s minutes
            - Completed Steps: %s
            - Previous AI Feedback: %s
            
            USER MESSAGE: "%s"
            
            Provide helpful, contextual response considering:
            1. The user's current cooking stage and progress
            2. Any previous challenges or successes in this session
            3. The specific technique or process they're working on
            4. Food safety and quality considerations
            5. Their apparent skill level based on progress
            
            Respond in JSON format:
            {
              "response": "your helpful response",
              "actionType": "CONTINUE/PAUSE/ADJUST_TECHNIQUE/etc",
              "suggestions": ["specific actionable tips"],
              "followUpQuestions": ["clarifying questions if needed"],
              "contextUpdate": {"key": "value updates to remember"}
            }
            
            Be conversational, encouraging, and specific to their current situation.
        """
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Analyze user's cooking progress using camera image
     */
    suspend fun analyzeProgress(
        request: ProgressAnalysisRequest,
        userImageBitmap: Bitmap
    ): ProgressAnalysisResult = withContext(Dispatchers.IO) {
        
        try {
            // Get artifact for context
            val artifact = artifactRepository.getArtifactById(request.artifactId, "current_user")
            val recipe = (artifact?.contentData as? ArtifactContent.RecipeContent)?.recipe
            
            if (recipe == null) {
                return@withContext ProgressAnalysisResult(
                    success = false,
                    confidenceScore = 0f,
                    overallFeedback = "Unable to load recipe context",
                    error = "Recipe not found"
                )
            }
            
            // Save image temporarily for analysis
            val imageFile = saveImageForAnalysis(userImageBitmap, request.artifactId)
            
            // Prepare analysis prompt
            val prompt = ANALYSIS_PROMPT_TEMPLATE.format(
                recipe.title,
                request.currentStageIndex + 1,
                recipe.steps.size,
                request.stepDescription,
                request.visualCues.joinToString(", "),
                formatElapsedTime(System.currentTimeMillis())
            )
            
            // Send to XAI for analysis
            val analysisResponse = geminiClient.analyzeImageWithContext(
                imageUri = Uri.fromFile(imageFile),
                prompt = prompt,
                contextData = buildContextData(recipe, request)
            )
            
            if (analysisResponse.isSuccess) {
                val responseText = analysisResponse.getOrNull() ?: ""
                val result = parseAnalysisResponse(responseText, request.currentStageIndex)
                
                // Save analysis to artifact progress
                saveAnalysisToProgress(request.artifactId, result, imageFile.absolutePath)
                
                // Clean up temporary image
                imageFile.delete()
                
                result
            } else {
                Log.e(TAG, "Gemini analysis failed", analysisResponse.exceptionOrNull())
                ProgressAnalysisResult(
                    success = false,
                    confidenceScore = 0f,
                    overallFeedback = "Unable to analyze image at this time",
                    error = analysisResponse.exceptionOrNull()?.message
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing progress", e)
            ProgressAnalysisResult(
                success = false,
                confidenceScore = 0f,
                overallFeedback = "Analysis failed due to technical error",
                error = e.message
            )
        }
    }
    
    /**
     * Provide interactive guidance during cooking session
     */
    suspend fun provideInteractiveGuidance(
        request: InteractiveGuidanceRequest
    ): InteractiveGuidanceResponse = withContext(Dispatchers.IO) {
        
        try {
            val context = request.currentContext
            val timeElapsed = (System.currentTimeMillis() - context.timeElapsed) / (1000 * 60) // minutes
            
            val prompt = INTERACTIVE_GUIDANCE_PROMPT.format(
                context.recipe.title,
                context.currentStepIndex + 1,
                context.recipe.steps.size,
                context.recipe.steps.getOrNull(context.currentStepIndex)?.title ?: "Unknown",
                timeElapsed,
                context.completedSteps.joinToString(", "),
                request.previousAnalyses.takeLast(2).joinToString("; ") { it.feedback },
                request.userMessage
            )
            
            val response = geminiClient.generateContextualResponse(
                prompt = prompt,
                contextData = buildInteractiveContext(context, request.userPreferences),
                agentType = "cooking"
            )
            
            if (response.isSuccess) {
                val responseText = response.getOrNull() ?: ""
                parseInteractiveResponse(responseText)
            } else {
                Log.e(TAG, "Interactive guidance failed", response.exceptionOrNull())
                InteractiveGuidanceResponse(
                    response = "I'm having trouble processing your request right now. Let me help you continue with the current step.",
                    actionType = GuidanceActionType.CONTINUE,
                    suggestions = listOf("Continue with the current step as described")
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error providing interactive guidance", e)
            InteractiveGuidanceResponse(
                response = "I'm experiencing technical difficulties, but let's keep cooking! Focus on the current step.",
                actionType = GuidanceActionType.CONTINUE
            )
        }
    }
    
    /**
     * Generate adaptive stage adjustments based on user performance
     */
    suspend fun generateStageAdjustments(
        artifactId: String,
        currentProgress: ArtifactProgress,
        userPerformanceData: Map<String, Any>
    ): List<StageAdjustment> = withContext(Dispatchers.IO) {
        
        try {
            val artifact = artifactRepository.getArtifactById(artifactId, "current_user")
            val recipe = (artifact?.contentData as? ArtifactContent.RecipeContent)?.recipe
            
            if (recipe == null) return@withContext emptyList()
            
            val adjustments = mutableListOf<StageAdjustment>()
            
            // Analyze performance patterns
            val avgConfidenceScore = currentProgress.stageStates.values
                .mapNotNull { it.aiAnalysis?.confidenceScore }
                .average()
                .toFloat()
            
            val hasRepeatedIssues = currentProgress.stageStates.values
                .count { it.status == StageStatus.NEEDS_HELP } > 1
            
            val isAheadOfSchedule = userPerformanceData["timeElapsed"] as? Long ?: 0L < 
                                   recipe.cookTime * 60 * 1000L * 0.8 // 80% of expected time
            
            // Generate appropriate adjustments
            when {
                avgConfidenceScore < 0.6f && hasRepeatedIssues -> {
                    adjustments.add(
                        StageAdjustment(
                            type = AdjustmentType.TECHNIQUE_MODIFICATION,
                            description = "Simplified technique for better success",
                            newInstructions = "Let's try a more straightforward approach for the remaining steps"
                        )
                    )
                }
                
                avgConfidenceScore > 0.8f && isAheadOfSchedule -> {
                    adjustments.add(
                        StageAdjustment(
                            type = AdjustmentType.ADDITIONAL_STEP,
                            description = "Advanced technique opportunity",
                            newInstructions = "You're doing great! Here's an optional enhancement you could try"
                        )
                    )
                }
                
                userPerformanceData["temperatureIssues"] == true -> {
                    adjustments.add(
                        StageAdjustment(
                            type = AdjustmentType.TEMPERATURE_CHANGE,
                            description = "Temperature adjustment for better results",
                            newInstructions = "Let's adjust the heat to get better control"
                        )
                    )
                }
            }
            
            adjustments
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating stage adjustments", e)
            emptyList()
        }
    }
    
    /**
     * Evaluate completion readiness and provide final feedback
     */
    suspend fun evaluateCompletionReadiness(
        artifactId: String,
        finalImages: List<String>
    ): InteractiveGuidanceResponse = withContext(Dispatchers.IO) {
        
        try {
            val artifact = artifactRepository.getArtifactById(artifactId, "current_user")
            val recipe = (artifact?.contentData as? ArtifactContent.RecipeContent)?.recipe
            
            if (recipe == null) {
                return@withContext InteractiveGuidanceResponse(
                    response = "Great job completing the recipe!",
                    actionType = GuidanceActionType.PROVIDE_ENCOURAGEMENT
                )
            }
            
            // Analyze final results if images provided
            val finalAnalysis = if (finalImages.isNotEmpty()) {
                // TODO: Implement final image analysis
                "Your final dish looks excellent!"
            } else {
                "I hope your ${recipe.title} turned out delicious!"
            }
            
            val suggestions = mutableListOf<String>()
            
            // Add completion suggestions based on recipe type
            when (recipe.mealType?.lowercase()) {
                "dinner" -> suggestions.add("Let the dish rest for a few minutes before serving")
                "dessert" -> suggestions.add("Consider a garnish or dusting of powdered sugar")
                else -> suggestions.add("Taste and adjust seasoning if needed")
            }
            
            InteractiveGuidanceResponse(
                response = "Congratulations! You've successfully completed ${recipe.title}. $finalAnalysis",
                actionType = GuidanceActionType.PROVIDE_ENCOURAGEMENT,
                suggestions = suggestions,
                followUpQuestions = listOf(
                    "How do you think it turned out?",
                    "Would you like to save any notes about this cooking session?"
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating completion", e)
            InteractiveGuidanceResponse(
                response = "Congratulations on completing your cooking session!",
                actionType = GuidanceActionType.PROVIDE_ENCOURAGEMENT
            )
        }
    }
    
    // Private helper methods
    private suspend fun saveImageForAnalysis(bitmap: Bitmap, artifactId: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "${artifactId}_progress_${timestamp}.jpg"
        val file = File.createTempFile(filename, ".jpg")
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        
        return file
    }
    
    private fun buildContextData(recipe: Recipe, request: ProgressAnalysisRequest): Map<String, String> {
        return mapOf(
            "recipe_difficulty" to recipe.difficulty,
            "cooking_method" to recipe.steps.getOrNull(request.currentStageIndex)?.techniques?.joinToString(", ").orEmpty(),
            "cuisine_type" to (recipe.cuisine ?: "general"),
            "user_modifications" to request.contextualInfo.getOrDefault("modifications", "none")
        )
    }
    
    private fun buildInteractiveContext(
        context: CookingContext,
        userPreferences: Map<String, String>
    ): Map<String, String> {
        return mapOf(
            "skill_level" to userPreferences.getOrDefault("skill_level", "beginner"),
            "dietary_restrictions" to userPreferences.getOrDefault("dietary_restrictions", "none"),
            "cooking_experience" to userPreferences.getOrDefault("experience", "limited"),
            "preferred_communication_style" to userPreferences.getOrDefault("communication", "encouraging")
        )
    }
    
    private fun parseAnalysisResponse(responseText: String, stageIndex: Int): ProgressAnalysisResult {
        return try {
            // Try to extract JSON from the response
            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}") + 1
            
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonText = responseText.substring(jsonStart, jsonEnd)
                val parsedResponse = json.decodeFromString<ProgressAnalysisResult>(jsonText)
                parsedResponse.copy(success = true)
            } else {
                // Fallback to text parsing
                ProgressAnalysisResult(
                    success = true,
                    confidenceScore = 0.7f,
                    overallFeedback = responseText,
                    isOnTrack = true
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON response, using fallback", e)
            ProgressAnalysisResult(
                success = true,
                confidenceScore = 0.6f,
                overallFeedback = responseText.take(200) + "...",
                isOnTrack = true
            )
        }
    }
    
    private fun parseInteractiveResponse(responseText: String): InteractiveGuidanceResponse {
        return try {
            val jsonStart = responseText.indexOf("{")
            val jsonEnd = responseText.lastIndexOf("}") + 1
            
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val jsonText = responseText.substring(jsonStart, jsonEnd)
                json.decodeFromString<InteractiveGuidanceResponse>(jsonText)
            } else {
                InteractiveGuidanceResponse(
                    response = responseText,
                    actionType = GuidanceActionType.CONTINUE
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse interactive response", e)
            InteractiveGuidanceResponse(
                response = responseText.take(200),
                actionType = GuidanceActionType.CONTINUE
            )
        }
    }
    
    private suspend fun saveAnalysisToProgress(
        artifactId: String,
        result: ProgressAnalysisResult,
        imagePath: String
    ) {
        try {
            val artifact = artifactRepository.getArtifactById(artifactId, "current_user")
            val currentProgress = artifact?.currentProgress ?: ArtifactProgress()
            
            val analysis = AIAnalysis(
                confidenceScore = result.confidenceScore,
                feedback = result.overallFeedback,
                suggestions = result.suggestions,
                adjustments = result.stageAdjustments
            )
            
            val updatedStageStates = currentProgress.stageStates.toMutableMap()
            updatedStageStates[currentProgress.currentStageIndex.toString()] = StageState(
                status = if (result.needsIntervention) StageStatus.NEEDS_HELP else StageStatus.ACTIVE,
                aiAnalysis = analysis
            )
            
            val updatedProgress = currentProgress.copy(
                stageStates = updatedStageStates,
                userNotes = currentProgress.userNotes + ProgressNote(
                    stageIndex = currentProgress.currentStageIndex,
                    note = "AI Analysis: ${result.overallFeedback.take(100)}",
                    imageUrl = imagePath
                )
            )
            
            artifactRepository.updateArtifactProgress(artifactId, "current_user", updatedProgress)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving analysis to progress", e)
        }
    }
    
    private fun formatElapsedTime(startTime: Long): String {
        val elapsed = (System.currentTimeMillis() - startTime) / (1000 * 60)
        return elapsed.toString()
    }
}