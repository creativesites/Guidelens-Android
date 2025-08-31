package com.craftflowtechnologies.guidelens.storage

import android.content.Context
import android.util.Log
import com.craftflowtechnologies.guidelens.chat.ChatSessionManager
import com.craftflowtechnologies.guidelens.ui.ChatMessage
import com.craftflowtechnologies.guidelens.ui.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeExtractionManager(
    private val context: Context,
    private val chatSessionManager: ChatSessionManager,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "RecipeExtractionManager"
    }

    suspend fun extractRecipesFromAllChatSessions(currentUser: User?): Result<List<Artifact>> = withContext(Dispatchers.IO) {
        if (currentUser == null) {
            return@withContext Result.failure(Exception("No user provided"))
        }

        try {
            val allSessions = chatSessionManager.getSessionsForUser(currentUser.id)
            val extractedRecipes = mutableListOf<Artifact>()

            Log.i(TAG, "Found ${allSessions.size} chat sessions for user ${currentUser.name}")

            allSessions.forEach { session ->
                try {
                    // Extract recipes from assistant messages in this session
                    val assistantMessages = session.messages
                        .filter { !it.isFromUser && it.text.length > 200 }
                        .map { it.text }

                    if (assistantMessages.isNotEmpty()) {
                        Log.i(TAG, "Processing ${assistantMessages.size} assistant messages from session: ${session.name}")
                        
                        val sessionRecipes = artifactRepository.extractRecipesFromChatHistory(
                            userId = currentUser.id,
                            chatMessages = assistantMessages,
                            agentType = session.agentId
                        )
                        
                        if (sessionRecipes.isSuccess) {
                            val recipes = sessionRecipes.getOrNull() ?: emptyList()
                            extractedRecipes.addAll(recipes)
                            Log.i(TAG, "Extracted ${recipes.size} recipes from session: ${session.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing session ${session.name}", e)
                }
            }

            Log.i(TAG, "Total extracted recipes: ${extractedRecipes.size}")
            Result.success(extractedRecipes)

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recipes from chat sessions", e)
            Result.failure(e)
        }
    }

    suspend fun extractRecipesFromSingleSession(
        sessionId: String,
        currentUser: User?
    ): Result<List<Artifact>> = withContext(Dispatchers.IO) {
        if (currentUser == null) {
            return@withContext Result.failure(Exception("No user provided"))
        }

        try {
            val allSessions = chatSessionManager.getSessionsForUser(currentUser.id)
            val session = allSessions.find { it.id == sessionId }
                ?: return@withContext Result.failure(Exception("Session not found"))

            val assistantMessages = session.messages
                .filter { !it.isFromUser && it.text.length > 200 }
                .map { it.text }

            if (assistantMessages.isEmpty()) {
                Log.i(TAG, "No suitable messages found in session: ${session.name}")
                return@withContext Result.success(emptyList())
            }

            val extractionResult = artifactRepository.extractRecipesFromChatHistory(
                userId = currentUser.id,
                chatMessages = assistantMessages,
                agentType = session.agentId
            )

            if (extractionResult.isSuccess) {
                val recipes = extractionResult.getOrNull() ?: emptyList()
                Log.i(TAG, "Extracted ${recipes.size} recipes from session: ${session.name}")
                Result.success(recipes)
            } else {
                extractionResult
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recipes from session", e)
            Result.failure(e)
        }
    }

    suspend fun extractRecipeFromMessage(
        message: ChatMessage,
        currentUser: User?,
        agentType: String
    ): Result<Artifact?> = withContext(Dispatchers.IO) {
        if (currentUser == null) {
            return@withContext Result.failure(Exception("No user provided"))
        }

        if (message.isFromUser || message.text.length < 200) {
            return@withContext Result.success(null) // Not suitable for recipe extraction
        }

        try {
            val extractionResult = artifactRepository.extractAndSaveRecipeFromMessage(
                message = message.text,
                userId = currentUser.id,
                agentType = agentType
            )

            if (extractionResult.isSuccess) {
                val recipe = extractionResult.getOrNull()
                if (recipe != null) {
                    Log.i(TAG, "Successfully extracted recipe: ${recipe.title}")
                }
                Result.success(recipe)
            } else {
                extractionResult
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recipe from message", e)
            Result.failure(e)
        }
    }

    suspend fun getRecipeExtractionStats(currentUser: User?): ExtractionStats? {
        if (currentUser == null) return null

        return try {
            val allArtifacts = artifactRepository.getAllArtifactsForUser(currentUser.id)
            val recipes = allArtifacts.filter { it.type == ArtifactType.RECIPE }
            val autoExtracted = recipes.filter { it.tags.contains("auto-extracted") }

            ExtractionStats(
                totalRecipes = recipes.size,
                autoExtractedRecipes = autoExtracted.size,
                manuallyCreatedRecipes = recipes.size - autoExtracted.size,
                lastExtractionDate = autoExtracted.maxByOrNull { it.createdAt }?.createdAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting extraction stats", e)
            null
        }
    }

    data class ExtractionStats(
        val totalRecipes: Int,
        val autoExtractedRecipes: Int,
        val manuallyCreatedRecipes: Int,
        val lastExtractionDate: Long?
    )
}