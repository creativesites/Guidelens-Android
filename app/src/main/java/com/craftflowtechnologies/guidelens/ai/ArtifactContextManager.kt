package com.craftflowtechnologies.guidelens.ai

import android.util.Log
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ArtifactMatch(
    val artifactId: String,
    val similarity: Float, // 0.0 to 1.0
    val matchType: MatchType,
    val title: String,
    val description: String,
    val lastUsed: Long,
    val usageCount: Int,
    val userRating: Float?,
    val variations: List<String> = emptyList()
)

@Serializable
enum class MatchType {
    EXACT, SIMILAR_RECIPE, VARIATION, SAME_TECHNIQUE, SAME_INGREDIENTS, CONTEXTUAL
}

@Serializable
data class ContextualSuggestion(
    val type: SuggestionType,
    val message: String,
    val actionType: String, // "use_existing", "create_variation", "continue_new"
    val artifactId: String?,
    val confidence: Float,
    val reasoning: String
)

@Serializable
enum class SuggestionType {
    REUSE_EXISTING, CREATE_VARIATION, IMPROVE_PREVIOUS, LEARN_FROM_HISTORY, WARNING_SIMILAR
}

@Serializable
data class CookingContextState(
    val userId: String,
    val currentSessionId: String,
    val sessionHistory: List<SessionSnapshot> = emptyList(),
    val userPreferences: Map<String, String> = emptyMap(),
    val skillProgression: Map<String, Float> = emptyMap(), // technique -> proficiency (0.0-1.0)
    val frequentIngredients: Map<String, Int> = emptyMap(), // ingredient -> usage count
    val cookingPatterns: Map<String, String> = emptyMap(), // time patterns, difficulty progression, etc.
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class SessionSnapshot(
    val sessionId: String,
    val artifactId: String,
    val startTime: Long,
    val endTime: Long?,
    val completed: Boolean,
    val stagesCompleted: Int,
    val totalStages: Int,
    val averageConfidence: Float,
    val techniques: List<String>,
    val challenges: List<String> = emptyList(),
    val modifications: List<String> = emptyList()
)

class ArtifactContextManager(
    private val artifactRepository: ArtifactRepository,
    enhancedGeminiClient: EnhancedGeminiClient
) {
    companion object {
        private const val TAG = "ArtifactContextManager"
        private const val SIMILARITY_THRESHOLD = 0.7f
        private const val EXACT_MATCH_THRESHOLD = 0.95f
        private const val MIN_REUSE_TIME_HOURS = 24
    }
    
    /**
     * Detect if user is requesting something similar to existing artifacts
     */
//    suspend fun detectSimilarArtifacts(
//        userId: String,
//        query: String,
//        artifactType: ArtifactType
//    ): List<ArtifactMatch> = withContext(Dispatchers.IO) {
//
//        try {
//            val userArtifacts = artifactRepository.getArtifactsByType(userId, artifactType)
//                .kotlinx.coroutines.flow.first()
//
//            val matches = mutableListOf<ArtifactMatch>()
//
//            userArtifacts.forEach { artifact ->
//                val similarity = calculateSimilarity(query, artifact)
//
//                if (similarity >= SIMILARITY_THRESHOLD) {
//                    val matchType = determineMatchType(similarity, query, artifact)
//
//                    matches.add(
//                        ArtifactMatch(
//                            artifactId = artifact.id,
//                            similarity = similarity,
//                            matchType = matchType,
//                            title = artifact.title,
//                            description = artifact.description,
//                            lastUsed = artifact.usageStats.lastAccessedAt ?: 0L,
//                            usageCount = artifact.usageStats.timesAccessed,
//                            userRating = artifact.usageStats.userRating,
//                            variations = findVariations(artifact, userArtifacts)
//                        )
//                    )
//                }
//            }
//
//            // Sort by similarity and recency
//            matches.sortedWith(
//                compareByDescending<ArtifactMatch> { it.similarity }
//                    .thenByDescending { it.lastUsed }
//            )
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error detecting similar artifacts", e)
//            emptyList()
//        }
//    }
    
    /**
     * Generate contextual suggestions based on detected matches
     */
    suspend fun generateContextualSuggestions(
        userId: String,
        query: String,
        matches: List<ArtifactMatch>
    ): List<ContextualSuggestion> = withContext(Dispatchers.IO) {
        
        val suggestions = mutableListOf<ContextualSuggestion>()
        
        try {
            val context = getCookingContext(userId)
            
            matches.forEach { match ->
                when {
                    // Exact or near-exact match
                    match.similarity >= EXACT_MATCH_THRESHOLD -> {
                        suggestions.add(
                            ContextualSuggestion(
                                type = SuggestionType.REUSE_EXISTING,
                                message = buildReuseMessage(match, context),
                                actionType = "use_existing",
                                artifactId = match.artifactId,
                                confidence = match.similarity,
                                reasoning = "Found very similar recipe you've made before"
                            )
                        )
                    }
                    
                    // Similar with good rating - suggest variation
                    match.similarity >= 0.8f && (match.userRating ?: 0f) >= 4.0f -> {
                        suggestions.add(
                            ContextualSuggestion(
                                type = SuggestionType.CREATE_VARIATION,
                                message = buildVariationMessage(match, query),
                                actionType = "create_variation",
                                artifactId = match.artifactId,
                                confidence = match.similarity,
                                reasoning = "Based on your highly-rated similar recipe"
                            )
                        )
                    }
                    
                    // Similar with low rating - suggest improvement
                    match.similarity >= 0.8f && (match.userRating ?: 0f) < 3.0f -> {
                        suggestions.add(
                            ContextualSuggestion(
                                type = SuggestionType.IMPROVE_PREVIOUS,
                                message = buildImprovementMessage(match, query),
                                actionType = "create_variation",
                                artifactId = match.artifactId,
                                confidence = match.similarity * 0.8f, // Reduce confidence for low-rated
                                reasoning = "Opportunity to improve on previous attempt"
                            )
                        )
                    }
                    
                    // Recent similar attempt - warn about redundancy
                    isRecentAttempt(match) -> {
                        suggestions.add(
                            ContextualSuggestion(
                                type = SuggestionType.WARNING_SIMILAR,
                                message = "You recently made '${match.title}'. Would you like to use that recipe instead?",
                                actionType = "use_existing",
                                artifactId = match.artifactId,
                                confidence = match.similarity,
                                reasoning = "Recently created similar recipe"
                            )
                        )
                    }
                }
            }
            
            // Add learning-based suggestions
            val learningBasedSuggestions = generateLearningBasedSuggestions(context, query)
            suggestions.addAll(learningBasedSuggestions)
            
            // Sort by confidence and relevance
            suggestions.sortedByDescending { it.confidence }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating contextual suggestions", e)
            emptyList()
        }
    }
    
    /**
     * Update cooking context with session data
     */
    suspend fun updateCookingContext(
        userId: String,
        sessionId: String,
        artifact: Artifact,
        sessionData: Map<String, Any>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            val currentContext = getCookingContext(userId)
            
            // Create session snapshot
            val snapshot = SessionSnapshot(
                sessionId = sessionId,
                artifactId = artifact.id,
                startTime = sessionData["start_time"] as? Long ?: System.currentTimeMillis(),
                endTime = sessionData["end_time"] as? Long,
                completed = sessionData["completed"] as? Boolean ?: false,
                stagesCompleted = sessionData["stages_completed"] as? Int ?: 0,
                totalStages = artifact.stageImages.size,
                averageConfidence = sessionData["avg_confidence"] as? Float ?: 0.5f,
                techniques = extractTechniques(artifact),
                challenges = sessionData["challenges"] as? List<String> ?: emptyList(),
                modifications = sessionData["modifications"] as? List<String> ?: emptyList()
            )
            
            // Update context
            val updatedContext = currentContext.copy(
                currentSessionId = sessionId,
                sessionHistory = (currentContext.sessionHistory + snapshot).takeLast(50), // Keep last 50 sessions
                skillProgression = updateSkillProgression(currentContext.skillProgression, snapshot),
                frequentIngredients = updateIngredientFrequency(currentContext.frequentIngredients, artifact),
                cookingPatterns = updateCookingPatterns(currentContext.cookingPatterns, snapshot) as Map<String, String>,
                lastUpdated = System.currentTimeMillis()
            )
            
            // TODO: Save updated context to persistent storage
            saveContextToStorage(updatedContext)
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cooking context", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get AI-enhanced recommendations based on cooking history
     */
    suspend fun getPersonalizedRecommendations(
        userId: String,
        currentArtifact: Artifact? = null
    ): List<ContextualSuggestion> = withContext(Dispatchers.IO) {
        
        try {
            val context = getCookingContext(userId)
            val recommendations = mutableListOf<ContextualSuggestion>()
            
            // Skill progression recommendations
            val skillRecommendations = generateSkillBasedRecommendations(context)
            recommendations.addAll(skillRecommendations)
            
            // Pattern-based recommendations
            val patternRecommendations = generatePatternBasedRecommendations(context)
            recommendations.addAll(patternRecommendations)
            
            // Context-aware recommendations for current session
            if (currentArtifact != null) {
                val sessionRecommendations = generateSessionRecommendations(context, currentArtifact)
                recommendations.addAll(sessionRecommendations)
            }
            
            recommendations.sortedByDescending { it.confidence }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting personalized recommendations", e)
            emptyList()
        }
    }
    
    /**
     * Analyze cooking progression and suggest next steps
     */
//    suspend fun analyzeProgressionAndSuggest(
//        userId: String,
//        timeframe: String = "monthly"
//    ): Map<String, Any> = withContext(Dispatchers.IO) {
//
//        try {
//            val context = getCookingContext(userId)
//            val recentSessions = getRecentSessions(context, timeframe)
//
//            val analysis = mutableMapOf<String, Any>()
//
//            // Skill progression analysis
//            analysis["skill_improvements"] = analyzeSkillImprovements(recentSessions)
//            analysis["mastered_techniques"] = identifyMasteredTechniques(context.skillProgression)
//            analysis["suggested_challenges"] = suggestNextChallenges(context)
//
//            // Usage patterns
//            analysis["cooking_frequency"] = calculateCookingFrequency(recentSessions)
//            analysis["preferred_difficulty"] = identifyPreferredDifficulty(recentSessions)
//            analysis["favorite_cuisines"] = identifyFavoriteCuisines(recentSessions)
//
//            // Personalized insights
//            analysis["insights"] = generatePersonalizedInsights(context, recentSessions)
//            analysis["next_goals"] = suggestCookingGoals(context)
//
//            analysis
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error analyzing progression", e)
//            mapOf("error" to e.message)
//        }
//    }
    
    // Private helper methods
    private fun calculateSimilarity(query: String, artifact: Artifact): Float {
        val queryTokens = tokenize(query.lowercase())
        val titleTokens = tokenize(artifact.title.lowercase())
        val descTokens = tokenize(artifact.description.lowercase())
        
        // Calculate title similarity
        val titleSimilarity = jaccardSimilarity(queryTokens, titleTokens)
        
        // Calculate description similarity
        val descSimilarity = jaccardSimilarity(queryTokens, descTokens)
        
        // Content-specific similarity
        var contentSimilarity = 0f
        when (artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                val ingredientTokens = artifact.contentData.recipe.ingredients
                    .flatMap { tokenize(it.name.lowercase()) }
                contentSimilarity = jaccardSimilarity(queryTokens, ingredientTokens as Set<String>)
            }
            else -> { /* Handle other content types */ }
        }
        
        // Weighted combination
        return (titleSimilarity * 0.5f + descSimilarity * 0.3f + contentSimilarity * 0.2f)
    }
    
    private fun jaccardSimilarity(set1: Set<String>, set2: Set<String>): Float {
        if (set1.isEmpty() && set2.isEmpty()) return 1.0f
        val intersection = set1.intersect(set2).size
        val union = set1.union(set2).size
        return intersection.toFloat() / union.toFloat()
    }
    
    private fun tokenize(text: String): Set<String> {
        return text.split(Regex("[\\s,.-]+"))
            .filter { it.length > 2 }
            .toSet()
    }
    
    private fun determineMatchType(similarity: Float, query: String, artifact: Artifact): MatchType {
        return when {
            similarity >= EXACT_MATCH_THRESHOLD -> MatchType.EXACT
            similarity >= 0.9f -> MatchType.VARIATION
            query.contains("recipe", ignoreCase = true) && 
                artifact.type == ArtifactType.RECIPE -> MatchType.SIMILAR_RECIPE
            else -> MatchType.CONTEXTUAL
        }
    }
    
    private suspend fun findVariations(
        artifact: Artifact,
        allArtifacts: List<Artifact>
    ): List<String> {
        return allArtifacts
            .filter { it.id != artifact.id && it.originalArtifactId == artifact.id }
            .map { it.title }
    }
    
    private fun buildReuseMessage(match: ArtifactMatch, context: CookingContextState): String {
        return when {
            match.userRating != null && match.userRating >= 4.0f -> 
                "You loved '${match.title}' (rated ${match.userRating}/5). Use this recipe again?"
            match.usageCount > 1 -> 
                "You've made '${match.title}' ${match.usageCount} times. It's one of your favorites!"
            else -> 
                "You have a very similar recipe: '${match.title}'. Would you like to use it instead?"
        }
    }
    
    private fun buildVariationMessage(match: ArtifactMatch, query: String): String {
        return "I found your highly-rated '${match.title}'. Would you like me to create a variation based on your new request?"
    }
    
    private fun buildImprovementMessage(match: ArtifactMatch, query: String): String {
        return "You previously tried '${match.title}' but weren't completely satisfied. Shall we create an improved version?"
    }
    
    private fun isRecentAttempt(match: ArtifactMatch): Boolean {
        val hoursAgo = (System.currentTimeMillis() - match.lastUsed) / (1000 * 60 * 60)
        return hoursAgo < MIN_REUSE_TIME_HOURS
    }
    
    private suspend fun getCookingContext(userId: String): CookingContextState {
        // TODO: Load from persistent storage
        return CookingContextState(
            userId = userId,
            currentSessionId = "",
            sessionHistory = emptyList(),
            skillProgression = mapOf(
                "knife_skills" to 0.3f,
                "timing" to 0.5f,
                "seasoning" to 0.4f,
                "temperature_control" to 0.6f
            )
        )
    }
    
    private fun generateLearningBasedSuggestions(
        context: CookingContextState,
        query: String
    ): List<ContextualSuggestion> {
        val suggestions = mutableListOf<ContextualSuggestion>()
        
        // Suggest practicing weak skills
        context.skillProgression.entries
            .filter { it.value < 0.5f }
            .forEach { (skill, proficiency) ->
                suggestions.add(
                    ContextualSuggestion(
                        type = SuggestionType.LEARN_FROM_HISTORY,
                        message = "This recipe could help you practice $skill (current level: ${(proficiency * 100).toInt()}%)",
                        actionType = "continue_new",
                        artifactId = null,
                        confidence = 1.0f - proficiency,
                        reasoning = "Skill development opportunity"
                    )
                )
            }
        
        return suggestions
    }
    
    private fun extractTechniques(artifact: Artifact): List<String> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.steps.flatMap { it.techniques }
            }
            is ArtifactContent.CraftContent -> {
                content.steps.flatMap { it.techniques }
            }
            is ArtifactContent.DIYContent -> {
                content.steps.flatMap { it.techniques }
            }
            else -> emptyList()
        }
    }
    
    private fun updateSkillProgression(
        currentSkills: Map<String, Float>,
        snapshot: SessionSnapshot
    ): Map<String, Float> {
        val updatedSkills = currentSkills.toMutableMap()
        
        // Improve skills based on successful completion
        if (snapshot.completed && snapshot.averageConfidence > 0.7f) {
            snapshot.techniques.forEach { technique ->
                val currentLevel = updatedSkills[technique] ?: 0f
                val improvement = 0.1f * snapshot.averageConfidence
                updatedSkills[technique] = (currentLevel + improvement).coerceAtMost(1.0f)
            }
        }
        
        return updatedSkills
    }
    
    private fun updateIngredientFrequency(
        currentFrequency: Map<String, Int>,
        artifact: Artifact
    ): Map<String, Int> {
        val updatedFrequency = currentFrequency.toMutableMap()
        
        when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                content.recipe.ingredients.forEach { ingredient ->
                    val current = updatedFrequency[ingredient.name] ?: 0
                    updatedFrequency[ingredient.name] = current + 1
                }
            }
            else -> { /* Handle other content types */ }
        }
        
        return updatedFrequency
    }
    
    private fun updateCookingPatterns(
        currentPatterns: Map<String, Any>,
        snapshot: SessionSnapshot
    ): Map<String, Any> {
        val updatedPatterns = currentPatterns.toMutableMap()
        
        // Update completion rate
        val completionRate = updatedPatterns["completion_rate"] as? Float ?: 0.5f
        val newCompletionRate = if (snapshot.completed) {
            (completionRate * 0.9f + 1.0f * 0.1f).coerceAtMost(1.0f)
        } else {
            (completionRate * 0.9f + 0.0f * 0.1f).coerceAtLeast(0.0f)
        }
        updatedPatterns["completion_rate"] = newCompletionRate
        
        // Update average session duration
        snapshot.endTime?.let { endTime ->
            val duration = endTime - snapshot.startTime
            val avgDuration = updatedPatterns["avg_session_duration"] as? Long ?: duration
            updatedPatterns["avg_session_duration"] = (avgDuration + duration) / 2
        }
        
        return updatedPatterns
    }
    
    private suspend fun saveContextToStorage(context: CookingContextState) {
        // TODO: Implement persistent storage for context
        Log.d(TAG, "Saving context for user ${context.userId}")
    }
    
    // Additional helper methods for recommendations and analysis
    private fun generateSkillBasedRecommendations(context: CookingContextState): List<ContextualSuggestion> {
        // TODO: Implement skill-based recommendations
        return emptyList()
    }
    
    private fun generatePatternBasedRecommendations(context: CookingContextState): List<ContextualSuggestion> {
        // TODO: Implement pattern-based recommendations
        return emptyList()
    }
    
    private fun generateSessionRecommendations(
        context: CookingContextState,
        artifact: Artifact
    ): List<ContextualSuggestion> {
        // TODO: Implement session-specific recommendations
        return emptyList()
    }
    
    private fun getRecentSessions(context: CookingContextState, timeframe: String): List<SessionSnapshot> {
        val cutoffTime = when (timeframe) {
            "weekly" -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            "monthly" -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
            else -> 0L
        }
        
        return context.sessionHistory.filter { it.startTime >= cutoffTime }
    }
    
    private fun analyzeSkillImprovements(sessions: List<SessionSnapshot>): Map<String, Float> {
        // TODO: Analyze skill improvements over time
        return emptyMap()
    }
    
    private fun identifyMasteredTechniques(skillProgression: Map<String, Float>): List<String> {
        return skillProgression.entries
            .filter { it.value >= 0.8f }
            .map { it.key }
    }
    
    private fun suggestNextChallenges(context: CookingContextState): List<String> {
        // TODO: Suggest appropriate next challenges based on skill level
        return emptyList()
    }
    
    private fun calculateCookingFrequency(sessions: List<SessionSnapshot>): Float {
        // TODO: Calculate how frequently user cooks
        return sessions.size.toFloat()
    }
    
    private fun identifyPreferredDifficulty(sessions: List<SessionSnapshot>): String {
        // TODO: Identify user's preferred difficulty level
        return "Medium"
    }
    
    private fun identifyFavoriteCuisines(sessions: List<SessionSnapshot>): List<String> {
        // TODO: Identify favorite cuisines from session history
        return emptyList()
    }
    
    private fun generatePersonalizedInsights(
        context: CookingContextState,
        sessions: List<SessionSnapshot>
    ): List<String> {
        // TODO: Generate personalized insights
        return emptyList()
    }
    
    private fun suggestCookingGoals(context: CookingContextState): List<String> {
        // TODO: Suggest cooking goals based on current skills and patterns
        return listOf(
            "Master knife skills",
            "Learn temperature control",
            "Expand to new cuisines"
        )
    }
}