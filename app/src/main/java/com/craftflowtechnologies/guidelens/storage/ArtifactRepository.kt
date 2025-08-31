package com.craftflowtechnologies.guidelens.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Note: Entities are defined in ArtifactDatabase.kt to avoid duplicates

// Room DAOs are imported from ArtifactDao.kt

// Note: Database class is defined in ArtifactDatabase.kt to avoid duplicates

// Type Converters for Room are defined in ArtifactDatabase.kt

// Main Repository Class
class ArtifactRepository(
    private val context: Context,
    private val database: ArtifactDatabase,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val artifactDao = database.artifactDao()
    private val userLimitsDao = database.userLimitsDao()
    private val cachedImageDao = database.cachedImageDao()

    companion object {
        private const val TAG = "ArtifactRepository"
        private const val CACHE_DIR = "artifact_cache"
        private const val MAX_CACHE_SIZE = 500 * 1024 * 1024L // 500MB
    }

    // Artifact CRUD Operations
    suspend fun saveArtifact(artifact: Artifact): Result<Artifact> = withContext(Dispatchers.IO) {
        try {
            val entity = artifact.toEntity()
            artifactDao.insertArtifact(entity)

            // Download and cache images if artifact is marked for offline use
            if (artifact.isDownloaded) {
                downloadArtifactImages(artifact)
            }

            Result.success(artifact)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving artifact", e)
            Result.failure(e)
        }
    }

    suspend fun getArtifactById(id: String, userId: String): Artifact? = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(id)?.takeIf { it.userId == userId }
            entity?.toArtifact()?.also { artifact ->
                // Update usage stats
                updateUsageStats(artifact.id, userId) { stats ->
                    stats.copy(
                        timesAccessed = stats.timesAccessed + 1,
                        lastAccessedAt = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting artifact by id", e)
            null
        }
    }

    suspend fun getAllArtifactsForUser(userId: String): List<Artifact> = withContext(Dispatchers.IO) {
        try {
            artifactDao.getRecentArtifacts(userId, 1000).mapNotNull { it.toArtifact() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting artifacts for user", e)
            emptyList()
        }
    }

    suspend fun getArtifactsByType(userId: String, type: ArtifactType): List<Artifact> = withContext(Dispatchers.IO) {
        try {
            artifactDao.getUserArtifactsByType(userId, type.name).mapNotNull { it.toArtifact() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting artifacts by type", e)
            emptyList()
        }
    }

    suspend fun searchArtifacts(query: String, userId: String): List<Artifact> = withContext(Dispatchers.IO) {
        try {
            artifactDao.searchUserArtifacts(userId, query)
                .mapNotNull { it.toArtifact() }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching artifacts", e)
            emptyList()
        }
    }

    suspend fun deleteArtifact(artifactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId)
            if (entity != null) {
                // Delete cached images
                cachedImageDao.deleteImagesForArtifact(artifactId)
                deleteArtifactFiles(artifactId)

                // Delete artifact
                artifactDao.deleteArtifact(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting artifact", e)
            Result.failure(e)
        }
    }

    // Offline Management
    suspend fun downloadArtifactForOffline(artifactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return@withContext Result.failure(
                Exception("Artifact not found")
            )

            val artifact = entity.toArtifact() ?: return@withContext Result.failure(
                Exception("Failed to parse artifact")
            )

            // Download all images
            val downloadResult = downloadArtifactImages(artifact)
            if (downloadResult.isSuccess) {
                // Update artifact as downloaded
                val updatedEntity = entity.copy(
                    isDownloaded = true,
                    downloadedAt = System.currentTimeMillis(),
                    fileSizeBytes = calculateArtifactSize(artifactId)
                )
                artifactDao.updateArtifact(updatedEntity)
                Result.success(Unit)
            } else {
                downloadResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading artifact for offline", e)
            Result.failure(e)
        }
    }

    suspend fun removeOfflineArtifact(artifactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return@withContext Result.failure(
                Exception("Artifact not found")
            )

            // Delete cached images and files
            cachedImageDao.deleteImagesForArtifact(artifactId)
            deleteArtifactFiles(artifactId)

            // Update artifact as not downloaded
            val updatedEntity = entity.copy(
                isDownloaded = false,
                downloadedAt = null,
                fileSizeBytes = 0
            )
            artifactDao.updateArtifact(updatedEntity)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing offline artifact", e)
            Result.failure(e)
        }
    }

    suspend fun getDownloadedArtifacts(userId: String): List<Artifact> = withContext(Dispatchers.IO) {
        try {
            artifactDao.getDownloadedArtifacts(userId).mapNotNull { it.toArtifact() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting downloaded artifacts", e)
            emptyList()
        }
    }

    // Progress Management
    suspend fun updateArtifactProgress(
        artifactId: String,
        userId: String,
        progress: ArtifactProgress
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return@withContext Result.failure(
                Exception("Artifact not found")
            )

            if (entity.userId != userId) {
                return@withContext Result.failure(Exception("Unauthorized access"))
            }

            val updatedEntity = entity.copy(
                currentProgress = progress,
                updatedAt = System.currentTimeMillis()
            )
            artifactDao.updateArtifact(updatedEntity)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating artifact progress", e)
            Result.failure(e)
        }
    }

    suspend fun addProgressNote(
        artifactId: String,
        userId: String,
        note: ProgressNote
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return@withContext Result.failure(
                Exception("Artifact not found")
            )

            if (entity.userId != userId) {
                return@withContext Result.failure(Exception("Unauthorized access"))
            }

            val currentProgress = entity.currentProgress ?: ArtifactProgress()

            val updatedProgress = currentProgress.copy(
                userNotes = currentProgress.userNotes + note
            )

            val updatedEntity = entity.copy(
                currentProgress = updatedProgress,
                updatedAt = System.currentTimeMillis()
            )
            artifactDao.updateArtifact(updatedEntity)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding progress note", e)
            Result.failure(e)
        }
    }

    // User Limits Management
    suspend fun getUserLimits(userId: String): UserLimits? = withContext(Dispatchers.IO) {
        try {
            userLimitsDao.getUserLimits(userId)?.toUserLimits()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user limits", e)
            null
        }
    }

    suspend fun updateUserLimits(limits: UserLimits): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userLimitsDao.insertUserLimits(limits.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user limits", e)
            Result.failure(e)
        }
    }

    suspend fun checkArtifactCreationLimits(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val limits = getUserLimits(userId) ?: return@withContext Result.success(true)
            val tierConfig = TierLimits.limits[limits.tier] ?: return@withContext Result.success(true)

            val canCreate = tierConfig.artifactsPerWeek == -1 ||
                    limits.artifactsCreatedThisWeek < tierConfig.artifactsPerWeek

            Result.success(canCreate)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking artifact creation limits", e)
            Result.failure(e)
        }
    }

    suspend fun checkImageGenerationLimits(userId: String, requestedImages: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val limits = getUserLimits(userId) ?: return@withContext Result.success(true)
            val tierConfig = TierLimits.limits[limits.tier] ?: return@withContext Result.success(true)

            val canGenerate = limits.imagesGeneratedToday + requestedImages <= tierConfig.imagesPerDay &&
                    limits.creditsRemaining >= requestedImages

            Result.success(canGenerate)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking image generation limits", e)
            Result.failure(e)
        }
    }

    // Usage Statistics
    private suspend fun updateUsageStats(
        artifactId: String,
        userId: String,
        updater: (ArtifactUsageStats) -> ArtifactUsageStats
    ) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return
            if (entity.userId != userId) return

            val currentStats = entity.usageStats
            val updatedStats = updater(currentStats)

            val updatedEntity = entity.copy(
                usageStats = updatedStats,
                updatedAt = System.currentTimeMillis()
            )
            artifactDao.updateArtifact(updatedEntity)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating usage stats", e)
        }
    }

    // Cache Management
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cachedImageDao.getTotalCacheSize() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache size", e)
            0L
        }
    }

    suspend fun clearCache(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete cached images for artifacts that aren't downloaded for offline
            val cachedImages = cachedImageDao.getImagesForArtifact("*") // Get all cached images
            cachedImages.forEach { image ->
                val file = File(image.localPath)
                if (file.exists()) file.delete()
                cachedImageDao.deleteCachedImage(image)
            }

            // Clean up orphaned cache files
            cleanupCacheFiles()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            Result.failure(e)
        }
    }

    // Recipe Extraction and Auto-Save
    suspend fun extractAndSaveRecipeFromMessage(
        message: String,
        userId: String,
        agentType: String,
        sessionId: String? = null
    ): Result<Artifact?> = withContext(Dispatchers.IO) {
        try {
            // Enhanced recipe detection with better filtering
            val hasLikelyRecipeStructure = hasRecipeStructure(message)
            val hasStrongRecipeIndicators = hasStrongRecipeIndicators(message)
            val isNotConversational = !isConversationalMessage(message)
            
            // Only extract if message has strong recipe indicators, proper structure, and isn't conversational
            if (!hasStrongRecipeIndicators || !hasLikelyRecipeStructure || !isNotConversational || message.length < 300) {
                Log.d(TAG, "Message filtered out - hasStrongIndicators: $hasStrongRecipeIndicators, hasStructure: $hasLikelyRecipeStructure, notConversational: $isNotConversational, length: ${message.length}")
                return@withContext Result.success(null) // Not a recipe
            }
            
            // Extract recipe title from the beginning of the message
            val lines = message.split("\n").filter { it.isNotBlank() }
            val title = extractRecipeTitle(lines) ?: "Recipe from ${getCurrentDate()}"
            
            // Check if this recipe already exists
            val existingRecipes = searchArtifacts(title, userId)
            if (existingRecipes.any { it.title.equals(title, ignoreCase = true) }) {
                return@withContext Result.success(null) // Recipe already exists
            }
            
            // Create recipe artifact
            val recipeContent = ArtifactContent.TextContent(message) // For now, store as text
            
            val artifact = Artifact(
                type = ArtifactType.RECIPE,
                title = title,
                description = extractRecipeDescription(lines),
                agentType = agentType,
                userId = userId,
                tags = listOf("recipe", agentType, "auto-extracted"),
                contentData = recipeContent,
                difficulty = estimateRecipeDifficulty(message),
                estimatedDuration = estimateRecipeTime(message)
            )
            
            val saveResult = saveArtifact(artifact)
            if (saveResult.isSuccess) {
                Log.i(TAG, "Auto-extracted and saved recipe: $title")
                Result.success(artifact)
            } else {
                saveResult.map { null }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recipe from message", e)
            Result.failure(e)
        }
    }
    
    suspend fun extractRecipesFromChatHistory(
        userId: String,
        chatMessages: List<String>,
        agentType: String
    ): Result<List<Artifact>> = withContext(Dispatchers.IO) {
        try {
            val extractedRecipes = mutableListOf<Artifact>()
            
            chatMessages.forEach { message ->
                val extractionResult = extractAndSaveRecipeFromMessage(message, userId, agentType)
                if (extractionResult.isSuccess && extractionResult.getOrNull() != null) {
                    extractedRecipes.add(extractionResult.getOrNull()!!)
                }
            }
            
            Log.i(TAG, "Extracted ${extractedRecipes.size} recipes from chat history")
            Result.success(extractedRecipes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting recipes from chat history", e)
            Result.failure(e)
        }
    }
    
    private fun extractRecipeTitle(lines: List<String>): String? {
        // Look for recipe title patterns
        for (line in lines.take(5)) { // Check first 5 lines
            val trimmedLine = line.trim()
            if (trimmedLine.length in 10..80 && !trimmedLine.contains("ingredients", ignoreCase = true)) {
                // Remove markdown formatting
                val cleanTitle = trimmedLine.replace(Regex("[#*_]"), "").trim()
                if (cleanTitle.isNotBlank()) {
                    return cleanTitle
                }
            }
        }
        return null
    }
    
    private fun extractRecipeDescription(lines: List<String>): String {
        // Look for description after title
        val description = lines.drop(1).take(3)
            .filter { it.length > 20 && !it.contains("ingredients", ignoreCase = true) }
            .firstOrNull()
        
        return description?.take(200) ?: "Auto-extracted recipe from chat"
    }
    
    private fun estimateRecipeDifficulty(message: String): String {
        val complexWords = listOf("marinate", "ferment", "proof", "reduction", "emulsion", "technique", "temperature")
        val complexCount = complexWords.count { message.contains(it, ignoreCase = true) }
        
        return when {
            complexCount >= 3 -> "Hard"
            complexCount >= 1 -> "Medium"
            else -> "Easy"
        }
    }
    
    private fun estimateRecipeTime(message: String): Int? {
        // Look for time mentions
        val timePattern = Regex("""(\d+)\s*(minutes?|mins?|hours?|hrs?)""", RegexOption.IGNORE_CASE)
        val matches = timePattern.findAll(message)
        
        val totalMinutes = matches.sumOf { match ->
            val number = match.groupValues[1].toIntOrNull() ?: 0
            val unit = match.groupValues[2].lowercase()
            when {
                unit.startsWith("hour") || unit.startsWith("hr") -> number * 60
                else -> number
            }
        }
        
        return if (totalMinutes > 0) totalMinutes else null
    }
    
    private fun getCurrentDate(): String {
        return SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())
    }

    // Enhanced recipe detection functions
    private fun hasStrongRecipeIndicators(message: String): Boolean {
        val strongIndicators = listOf(
            // Multiple ingredient indicators
            Regex("\\d+\\s*(cups?|tbsp|tsp|oz|lbs?|grams?|ml|liters?)", RegexOption.IGNORE_CASE),
            // Step indicators
            Regex("step\\s*\\d+", RegexOption.IGNORE_CASE),
            Regex("instructions?\\s*:", RegexOption.IGNORE_CASE),
            Regex("directions?\\s*:", RegexOption.IGNORE_CASE),
            // Combined indicators
            Regex("ingredients?\\s*:.*steps?", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
            Regex("ingredients?\\s*:.*instructions?", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        )
        
        return strongIndicators.any { it.containsMatchIn(message) }
    }
    
    private fun hasRecipeStructure(message: String): Boolean {
        val lines = message.split("\n").filter { it.isNotBlank() }
        
        // Look for structured lists (ingredients and steps)
        val hasNumberedSteps = lines.count { line ->
            Regex("^\\s*\\d+\\.").containsMatchIn(line)
        } >= 2
        
        val hasBulletedIngredients = lines.count { line ->
            Regex("^\\s*[-*•]\\s").containsMatchIn(line)
        } >= 2
        
        val hasIngredientSection = message.contains(Regex("ingredients?\\s*:", RegexOption.IGNORE_CASE))
        val hasInstructionSection = message.contains(Regex("(instructions?|directions?|steps?)\\s*:", RegexOption.IGNORE_CASE))
        
        return (hasNumberedSteps && hasBulletedIngredients) || 
               (hasIngredientSection && hasInstructionSection) ||
               (hasNumberedSteps && hasIngredientSection)
    }
    
    private fun isConversationalMessage(message: String): Boolean {
        val conversationalIndicators = listOf(
            // Questions
            Regex("(what|how|can|could|would|should|do you|are you|have you)\\s", RegexOption.IGNORE_CASE),
            // Personal statements
            Regex("\\bi\\s+(am|was|will|would|should|could|can|have|had|want|need|think|feel|like|love|hate)", RegexOption.IGNORE_CASE),
            // Response indicators
            Regex("(yes|no|sure|okay|thanks|thank you|please|sorry)\\b", RegexOption.IGNORE_CASE),
            // Conversational starters
            Regex("(hi|hello|hey|good morning|good afternoon)", RegexOption.IGNORE_CASE),
            // Help requests without structured content
            Regex("help me|can you help|i need help|assistance", RegexOption.IGNORE_CASE)
        )
        
        val conversationalCount = conversationalIndicators.count { it.containsMatchIn(message) }
        val totalSentences = message.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }.size
        
        // Consider it conversational if more than 50% of sentences contain conversational indicators
        return conversationalCount.toFloat() / totalSentences.toFloat() > 0.3f
    }

    // Favorites Management
    suspend fun toggleArtifactFavorite(artifactId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val entity = artifactDao.getArtifactById(artifactId) ?: return@withContext Result.failure(
                Exception("Artifact not found")
            )
            
            if (entity.userId != userId) {
                return@withContext Result.failure(Exception("Unauthorized access"))
            }
            
            val currentStats = entity.usageStats
            val updatedStats = currentStats.copy(
                bookmarked = !currentStats.bookmarked
            )
            
            val updatedEntity = entity.copy(
                usageStats = updatedStats,
                updatedAt = System.currentTimeMillis()
            )
            
            artifactDao.updateArtifact(updatedEntity)
            Result.success(updatedStats.bookmarked)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling artifact favorite", e)
            Result.failure(e)
        }
    }

    // Private Helper Methods
    private suspend fun downloadArtifactImages(artifact: Artifact): Result<Unit> {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) cacheDir.mkdirs()

            // Download main image
            artifact.mainImage?.let { image ->
                if (image.url != null) {
                    downloadImage(image, cacheDir, artifact.id)
                }
            }

            // Download stage images
            artifact.stageImages.forEach { stageImage ->
                if (stageImage.image.url != null) {
                    downloadImage(stageImage.image, cacheDir, artifact.id)
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading artifact images", e)
            return Result.failure(e)
        }
    }

    private suspend fun downloadImage(image: GeneratedImage, cacheDir: File, artifactId: String) {
        try {
            if (image.url == null) return

            val request = Request.Builder().url(image.url).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val imageFile = File(cacheDir, "${image.id}.jpg")
                val outputStream = FileOutputStream(imageFile)
                response.body?.byteStream()?.copyTo(outputStream)
                outputStream.close()

                // Save to cache database
                val cachedImage = CachedImageEntity(
                    id = image.id,
                    url = image.url,
                    localPath = imageFile.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                    fileSizeBytes = imageFile.length(),
                    artifactId = artifactId
                )
                cachedImageDao.insertCachedImage(cachedImage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: ${image.id}", e)
        }
    }

    private suspend fun calculateArtifactSize(artifactId: String): Long {
        return try {
            val images = cachedImageDao.getImagesForArtifact(artifactId)
            images.sumOf { it.fileSizeBytes }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating artifact size", e)
            0L
        }
    }

    private suspend fun deleteArtifactFiles(artifactId: String) {
        try {
            val images = cachedImageDao.getImagesForArtifact(artifactId)
            images.forEach { cachedImage ->
                val file = File(cachedImage.localPath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting artifact files", e)
        }
    }

    private suspend fun cleanupCacheFiles() {
        try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            if (!cacheDir.exists()) return

            val allCachedImages = mutableSetOf<String>()
            // Get all cached image paths from database
            val cachedImages = cachedImageDao.getImagesForArtifact("*")
            cachedImages.forEach { allCachedImages.add(it.localPath) }

            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && !allCachedImages.contains(file.absolutePath)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up cache files", e)
        }
    }

}

// Extension functions for entity conversions
private fun Artifact.toEntity(): ArtifactEntity {
    return ArtifactEntity(
        id = id,
        type = type,
        title = title,
        description = description,
        agentType = agentType,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        version = version,
        tags = tags,
        contentData = contentData,
        usageStats = usageStats,
        currentProgress = currentProgress,
        isDownloaded = isDownloaded,
        downloadedAt = downloadedAt,
        fileSizeBytes = fileSizeBytes,
        isShared = isShared,
        shareCode = shareCode,
        originalArtifactId = originalArtifactId,
        difficulty = difficulty,
        estimatedDuration = estimatedDuration,
        generationMetadata = generationMetadata
    )
}

private fun ArtifactEntity.toArtifact(): Artifact? {
    return try {
        Artifact(
            id = id,
            type = type,
            title = title,
            description = description,
            agentType = agentType,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
            tags = tags,
            contentData = contentData,
            stageImages = emptyList(), // Will be loaded separately if needed
            mainImage = null, // Will be loaded separately if needed
            usageStats = usageStats,
            currentProgress = currentProgress,
            isDownloaded = isDownloaded,
            downloadedAt = downloadedAt,
            fileSizeBytes = fileSizeBytes,
            isShared = isShared,
            shareCode = shareCode,
            originalArtifactId = originalArtifactId,
            difficulty = difficulty,
            estimatedDuration = estimatedDuration,
            generationMetadata = generationMetadata
        )
    } catch (e: Exception) {
        Log.e("ArtifactRepository", "Error converting entity to artifact", e)
        null
    }
}

private fun UserLimits.toEntity(): UserLimitsEntity {
    return UserLimitsEntity(
        userId = userId,
        tier = tier,
        artifactsCreatedToday = 0, // Will be calculated from database
        artifactsCreatedThisWeek = artifactsCreatedThisWeek,
        imagesGeneratedToday = imagesGeneratedToday,
        creditsRemaining = creditsRemaining,
        lastResetDate = System.currentTimeMillis(),
        isPremium = tier != UserTier.FREE,
        subscriptionExpiresAt = null
    )
}

private fun UserLimitsEntity.toUserLimits(): UserLimits {
    return UserLimits(
        userId = userId,
        tier = tier,
        creditsRemaining = creditsRemaining,
        creditsUsedToday = 0, // Will be calculated from usage
        artifactsCreatedThisWeek = artifactsCreatedThisWeek,
        imagesGeneratedToday = imagesGeneratedToday,
        resetDate = lastResetDate,
        weeklyResetDate = lastResetDate
    )
}