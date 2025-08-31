package com.craftflowtechnologies.guidelens.credits

import android.util.Log
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.math.max

@Serializable
data class CreditTransaction(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: TransactionType,
    val amount: Int, // positive for earned/purchased, negative for spent
    val reason: String,
    val artifactId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val balanceAfter: Int,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class TransactionType {
    PURCHASE, EARNED_DAILY, EARNED_ACTIVITY, SPENT_IMAGE_GENERATION, 
    SPENT_PREMIUM_FEATURE, REFUND, BONUS, PENALTY
}

@Serializable
data class CreditBalance(
    val userId: String,
    val totalCredits: Int,
    val dailyCreditsRemaining: Int,
    val weeklyCreditsUsed: Int,
    val monthlyCreditsUsed: Int,
    val tier: UserTier,
    val lastDailyReset: Long,
    val lastWeeklyReset: Long,
    val lastMonthlyReset: Long,
    val lifetimeCreditsEarned: Int = 0,
    val lifetimeCreditsSpent: Int = 0
)

@Serializable
data class CreditCost(
    val feature: String,
    val baseCost: Int,
    val tierMultiplier: Map<UserTier, Float> = mapOf(
        UserTier.FREE to 1.0f,
        UserTier.BASIC to 0.8f,
        UserTier.PRO to 0.5f
    ),
    val qualityMultiplier: Map<String, Float> = mapOf(
        "draft" to 0.5f,
        "standard" to 1.0f,
        "high" to 1.5f,
        "premium" to 2.0f
    )
)

@Serializable
data class UsageAnalytics(
    val userId: String,
    val period: String, // "daily", "weekly", "monthly"
    val imagesGenerated: Int = 0,
    val artifactsCreated: Int = 0,
    val premiumFeaturesUsed: Int = 0,
    val averageQualityUsed: Float = 1.0f,
    val mostUsedFeatures: List<String> = emptyList(),
    val efficiencyScore: Float = 1.0f // credits spent vs value received
)

class CreditsManager(
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "CreditsManager"
        
        // Credit costs for different features
        private val FEATURE_COSTS = mapOf(
            "image_generation_standard" to CreditCost("Standard Image", 1),
            "image_generation_high" to CreditCost("High Quality Image", 2),
            "image_generation_premium" to CreditCost("Premium Image", 3),
            "on_demand_image" to CreditCost("On-Demand Image", 2),
            "batch_artifact_images" to CreditCost("Artifact Image Set", 5),
            "ai_analysis" to CreditCost("AI Progress Analysis", 1),
            "recipe_generation" to CreditCost("AI Recipe Generation", 3),
            "craft_guide_generation" to CreditCost("AI Craft Guide", 4),
            "diy_guide_generation" to CreditCost("AI DIY Guide", 4),
            "voice_guidance" to CreditCost("Voice Guidance Session", 2),
            "live_video_guidance" to CreditCost("Live Video Session", 5)
        )
        
        // Daily credit allowances by tier
        private val DAILY_ALLOWANCES = mapOf(
            UserTier.FREE to 10,
            UserTier.BASIC to 25,
            UserTier.PRO to 50
        )
        
        // Welcome bonuses for new users
        private val WELCOME_BONUSES = mapOf(
            UserTier.FREE to 25,
            UserTier.BASIC to 100,
            UserTier.PRO to 200
        )
    }
    
    /**
     * Get current credit balance for user
     */
    suspend fun getCreditBalance(userId: String): CreditBalance = withContext(Dispatchers.IO) {
        try {
            val userLimits = artifactRepository.getUserLimits(userId) ?: return@withContext createNewUserBalance(userId)
            
            val now = System.currentTimeMillis()
            val currentBalance = CreditBalance(
                userId = userId,
                totalCredits = userLimits.creditsRemaining,
                dailyCreditsRemaining = calculateDailyCreditsRemaining(userLimits, now),
                weeklyCreditsUsed = userLimits.creditsUsedToday, // This should be weekly, but using daily for now
                monthlyCreditsUsed = 0, // TODO: Track monthly usage
                tier = userLimits.tier,
                lastDailyReset = userLimits.resetDate,
                lastWeeklyReset = userLimits.weeklyResetDate,
                lastMonthlyReset = userLimits.weeklyResetDate, // TODO: Add monthly reset
                lifetimeCreditsEarned = 0, // TODO: Calculate from transactions
                lifetimeCreditsSpent = 0 // TODO: Calculate from transactions
            )
            
            // Check if daily reset is needed
            if (isDailyResetNeeded(userLimits.resetDate, now)) {
                resetDailyCredits(userId, userLimits.tier)
            }
            
            currentBalance
        } catch (e: Exception) {
            Log.e(TAG, "Error getting credit balance", e)
            createNewUserBalance(userId)
        }
    }
    
    /**
     * Check if user can afford a specific feature
     */
    suspend fun canAfford(
        userId: String, 
        feature: String, 
        quality: String = "standard"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val balance = getCreditBalance(userId)
            val cost = calculateFeatureCost(feature, balance.tier, quality)
            
            val canAfford = balance.totalCredits >= cost
            Result.success(canAfford)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking affordability", e)
            Result.failure(e)
        }
    }
    
    /**
     * Spend credits for a feature
     */
    suspend fun spendCredits(
        userId: String,
        feature: String,
        quality: String = "standard",
        artifactId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Result<CreditTransaction> = withContext(Dispatchers.IO) {
        try {
            val balance = getCreditBalance(userId)
            val cost = calculateFeatureCost(feature, balance.tier, quality)
            
            if (balance.totalCredits < cost) {
                return@withContext Result.failure(
                    InsufficientCreditsException("Not enough credits. Need $cost, have ${balance.totalCredits}")
                )
            }
            
            // Create transaction
            val transaction = CreditTransaction(
                userId = userId,
                type = getTransactionTypeForFeature(feature),
                amount = -cost,
                reason = "Used $feature (${quality})",
                artifactId = artifactId,
                balanceAfter = balance.totalCredits - cost,
                metadata = metadata + mapOf(
                    "feature" to feature,
                    "quality" to quality,
                    "cost" to cost.toString()
                )
            )
            
            // Update user limits
            val userLimits = artifactRepository.getUserLimits(userId)
            if (userLimits != null) {
                val updatedLimits = userLimits.copy(
                    creditsRemaining = userLimits.creditsRemaining - cost,
                    creditsUsedToday = userLimits.creditsUsedToday + cost
                )
                artifactRepository.updateUserLimits(updatedLimits)
            }
            
            // TODO: Save transaction to database
            
            Result.success(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Error spending credits", e)
            Result.failure(e)
        }
    }
    
    /**
     * Add credits to user account
     */
    suspend fun addCredits(
        userId: String,
        amount: Int,
        type: TransactionType,
        reason: String,
        metadata: Map<String, String> = emptyMap()
    ): Result<CreditTransaction> = withContext(Dispatchers.IO) {
        try {
            val balance = getCreditBalance(userId)
            
            val transaction = CreditTransaction(
                userId = userId,
                type = type,
                amount = amount,
                reason = reason,
                balanceAfter = balance.totalCredits + amount,
                metadata = metadata
            )
            
            // Update user limits
            val userLimits = artifactRepository.getUserLimits(userId)
            if (userLimits != null) {
                val updatedLimits = userLimits.copy(
                    creditsRemaining = userLimits.creditsRemaining + amount
                )
                artifactRepository.updateUserLimits(updatedLimits)
            }
            
            // TODO: Save transaction to database
            
            Result.success(transaction)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding credits", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get feature cost for user's tier
     */
    fun getFeatureCost(
        feature: String, 
        userTier: UserTier, 
        quality: String = "standard"
    ): Int {
        return calculateFeatureCost(feature, userTier, quality)
    }
    
    /**
     * Get usage analytics for user
     */
    suspend fun getUsageAnalytics(
        userId: String, 
        period: String = "weekly"
    ): UsageAnalytics = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement actual analytics calculation from transaction history
            UsageAnalytics(
                userId = userId,
                period = period,
                imagesGenerated = 0,
                artifactsCreated = 0,
                premiumFeaturesUsed = 0,
                mostUsedFeatures = listOf("image_generation_standard", "ai_analysis"),
                efficiencyScore = 0.8f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting usage analytics", e)
            UsageAnalytics(userId = userId, period = period)
        }
    }
    
    /**
     * Check if user qualifies for bonus credits
     */
    suspend fun checkBonusEligibility(userId: String): List<BonusOpportunity> = withContext(Dispatchers.IO) {
        try {
            val bonuses = mutableListOf<BonusOpportunity>()
            val balance = getCreditBalance(userId)
            val analytics = getUsageAnalytics(userId)
            
            // Daily login bonus
            if (isDailyBonusAvailable(userId)) {
                bonuses.add(
                    BonusOpportunity(
                        type = "daily_login",
                        credits = 5,
                        description = "Daily login bonus",
                        requirement = "Log in daily"
                    )
                )
            }
            
            // Efficiency bonus
            if (analytics.efficiencyScore > 0.8f) {
                bonuses.add(
                    BonusOpportunity(
                        type = "efficiency",
                        credits = 10,
                        description = "Efficient usage bonus",
                        requirement = "Use credits wisely"
                    )
                )
            }
            
            // Low balance assistance
            if (balance.totalCredits < 5) {
                bonuses.add(
                    BonusOpportunity(
                        type = "low_balance_help",
                        credits = 15,
                        description = "Low balance assistance",
                        requirement = "Watch ad or complete tutorial"
                    )
                )
            }
            
            bonuses
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bonus eligibility", e)
            emptyList()
        }
    }
    
    /**
     * Grant welcome bonus to new users
     */
    suspend fun grantWelcomeBonus(userId: String, tier: UserTier): Result<CreditTransaction> {
        val bonusAmount = WELCOME_BONUSES[tier] ?: 25
        return addCredits(
            userId = userId,
            amount = bonusAmount,
            type = TransactionType.BONUS,
            reason = "Welcome to GuideLens!",
            metadata = mapOf("bonus_type" to "welcome", "tier" to tier.name)
        )
    }
    
    // Private helper methods
    private suspend fun createNewUserBalance(userId: String): CreditBalance {
        val tier = UserTier.FREE // Default tier for new users
        val dailyAllowance = DAILY_ALLOWANCES[tier] ?: 10
        val welcomeBonus = WELCOME_BONUSES[tier] ?: 25
        
        val now = System.currentTimeMillis()
        val newLimits = UserLimits(
            userId = userId,
            tier = tier,
            creditsRemaining = dailyAllowance + welcomeBonus,
            creditsUsedToday = 0,
            artifactsCreatedThisWeek = 0,
            imagesGeneratedToday = 0,
            resetDate = now + 24 * 60 * 60 * 1000, // Tomorrow
            weeklyResetDate = now + 7 * 24 * 60 * 60 * 1000 // Next week
        )
        
        artifactRepository.updateUserLimits(newLimits)
        
        // Grant welcome bonus
        grantWelcomeBonus(userId, tier)
        
        return CreditBalance(
            userId = userId,
            totalCredits = dailyAllowance + welcomeBonus,
            dailyCreditsRemaining = dailyAllowance,
            weeklyCreditsUsed = 0,
            monthlyCreditsUsed = 0,
            tier = tier,
            lastDailyReset = now,
            lastWeeklyReset = now,
            lastMonthlyReset = now
        )
    }
    
    private fun calculateFeatureCost(feature: String, tier: UserTier, quality: String): Int {
        val costConfig = FEATURE_COSTS[feature] ?: return 1
        val baseCost = costConfig.baseCost
        val tierMultiplier = costConfig.tierMultiplier[tier] ?: 1.0f
        val qualityMultiplier = costConfig.qualityMultiplier[quality] ?: 1.0f
        
        return max(1, (baseCost * tierMultiplier * qualityMultiplier).toInt())
    }
    
    private fun getTransactionTypeForFeature(feature: String): TransactionType {
        return when {
            feature.contains("image") -> TransactionType.SPENT_IMAGE_GENERATION
            feature.contains("premium") -> TransactionType.SPENT_PREMIUM_FEATURE
            else -> TransactionType.SPENT_PREMIUM_FEATURE
        }
    }
    
    private fun isDailyResetNeeded(lastResetTime: Long, currentTime: Long): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000
        return currentTime > lastResetTime + oneDayInMillis
    }
    
    private suspend fun resetDailyCredits(userId: String, tier: UserTier) {
        try {
            val dailyAllowance = DAILY_ALLOWANCES[tier] ?: 10
            addCredits(
                userId = userId,
                amount = dailyAllowance,
                type = TransactionType.EARNED_DAILY,
                reason = "Daily credit allowance",
                metadata = mapOf("tier" to tier.name)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting daily credits", e)
        }
    }
    
    private fun calculateDailyCreditsRemaining(userLimits: UserLimits, currentTime: Long): Int {
        val dailyAllowance = DAILY_ALLOWANCES[userLimits.tier] ?: 10
        return if (isDailyResetNeeded(userLimits.resetDate, currentTime)) {
            dailyAllowance
        } else {
            max(0, dailyAllowance - userLimits.creditsUsedToday)
        }
    }
    
    private suspend fun isDailyBonusAvailable(userId: String): Boolean {
        // TODO: Check if user has claimed daily bonus today
        return true
    }
}

@Serializable
data class BonusOpportunity(
    val type: String,
    val credits: Int,
    val description: String,
    val requirement: String,
    val expiresAt: Long? = null
)

class InsufficientCreditsException(message: String) : Exception(message)