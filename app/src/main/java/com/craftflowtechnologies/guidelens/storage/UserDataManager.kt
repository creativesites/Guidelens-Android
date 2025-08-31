package com.craftflowtechnologies.guidelens.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.text.SimpleDateFormat
import java.util.*

/**
 * Privacy-focused local user data storage system
 * - All data stored locally on device only
 * - Encrypted using Android EncryptedSharedPreferences
 * - User has full control over data deletion
 * - No cloud sync unless explicitly enabled by user
 */
class UserDataManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Encrypted storage for sensitive user data
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "user_data_encrypted",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Regular prefs for non-sensitive settings
    private val regularPrefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    
    // Flow-based reactive data streams
    private val _personalizedContext = MutableStateFlow(PersonalizedContext())
    val personalizedContext: StateFlow<PersonalizedContext> = _personalizedContext.asStateFlow()
    
    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        // Load existing data on startup
        scope.launch {
            loadAllUserData()
        }
    }

    private suspend fun loadAllUserData() = withContext(Dispatchers.IO) {
        try {
            // Load privacy settings first
            val privacyJson = regularPrefs.getString(PRIVACY_SETTINGS_KEY, null)
            if (privacyJson != null) {
                val privacy = json.decodeFromString<PrivacySettings>(privacyJson)
                _privacySettings.value = privacy
            }
            
            // Only load other data if user has opted in
            if (_privacySettings.value.allowPersonalization) {
                loadUserProfile()
                loadPersonalizedContext()
            }
        } catch (e: Exception) {
            // Handle loading errors gracefully
            e.printStackTrace()
        }
    }

    // User Profile Management
    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowPersonalization) return@withContext
        
        try {
            val profileJson = json.encodeToString(profile)
            encryptedPrefs.edit()
                .putString(USER_PROFILE_KEY, profileJson)
                .putLong(LAST_UPDATED_KEY, System.currentTimeMillis())
                .apply()
            
            _userProfile.value = profile
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadUserProfile() = withContext(Dispatchers.IO) {
        try {
            val profileJson = encryptedPrefs.getString(USER_PROFILE_KEY, null)
            if (profileJson != null) {
                val profile = json.decodeFromString<UserProfile>(profileJson)
                _userProfile.value = profile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Personalized Context Management
    suspend fun updatePersonalizedContext(
        interests: List<String>? = null,
        goals: List<String>? = null,
        preferences: Map<String, String>? = null,
        recentActivity: List<String>? = null
    ) = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowPersonalization) return@withContext
        
        val currentContext = _personalizedContext.value
        val updatedContext = currentContext.copy(
            interests = interests ?: currentContext.interests,
            goals = goals ?: currentContext.goals,
            preferences = preferences ?: currentContext.preferences,
            recentActivity = recentActivity ?: currentContext.recentActivity,
            lastUpdated = System.currentTimeMillis()
        )
        
        try {
            val contextJson = json.encodeToString(updatedContext)
            encryptedPrefs.edit()
                .putString(PERSONALIZED_CONTEXT_KEY, contextJson)
                .apply()
            
            _personalizedContext.value = updatedContext
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadPersonalizedContext() = withContext(Dispatchers.IO) {
        try {
            val contextJson = encryptedPrefs.getString(PERSONALIZED_CONTEXT_KEY, null)
            if (contextJson != null) {
                val context = json.decodeFromString<PersonalizedContext>(contextJson)
                _personalizedContext.value = context
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cooking Profile Management
    suspend fun saveCookingProfile(profile: UserCookingProfile) = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowCookingData) return@withContext
        
        try {
            val profileJson = json.encodeToString(profile)
            encryptedPrefs.edit()
                .putString(COOKING_PROFILE_KEY, profileJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadCookingProfile(): UserCookingProfile? = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowCookingData) return@withContext null
        
        try {
            val profileJson = encryptedPrefs.getString(COOKING_PROFILE_KEY, null)
            return@withContext if (profileJson != null) {
                json.decodeFromString<UserCookingProfile>(profileJson)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    // Mood and Wellness Data
    suspend fun saveMoodEntry(entry: MoodEntry) = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowWellnessData) return@withContext
        
        try {
            val existingEntries = loadMoodEntries().toMutableList()
            existingEntries.add(entry)
            
            // Keep only last 90 days of data
            val cutoffDate = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            val filteredEntries = existingEntries.filter { it.timestamp > cutoffDate }
            
            val entriesJson = json.encodeToString(filteredEntries)
            encryptedPrefs.edit()
                .putString(MOOD_ENTRIES_KEY, entriesJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadMoodEntries(): List<MoodEntry> = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowWellnessData) return@withContext emptyList()
        
        try {
            val entriesJson = encryptedPrefs.getString(MOOD_ENTRIES_KEY, null)
            return@withContext if (entriesJson != null) {
                json.decodeFromString<List<MoodEntry>>(entriesJson)
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    // Privacy Settings Management
    suspend fun updatePrivacySettings(settings: PrivacySettings) = withContext(Dispatchers.IO) {
        try {
            val settingsJson = json.encodeToString(settings)
            regularPrefs.edit()
                .putString(PRIVACY_SETTINGS_KEY, settingsJson)
                .apply()
            
            _privacySettings.value = settings
            
            // If user disabled personalization, clear related data
            if (!settings.allowPersonalization) {
                clearPersonalizationData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Usage Analytics (Privacy-Safe)
    suspend fun recordToolUsage(agentId: String, toolId: String, duration: Long) = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowUsageAnalytics) return@withContext
        
        try {
            val usageData = loadUsageData().toMutableMap()
            val key = "${agentId}_${toolId}"
            val currentUsage = usageData[key] ?: ToolUsage(agentId, toolId, 0, 0L)
            
            usageData[key] = currentUsage.copy(
                usageCount = currentUsage.usageCount + 1,
                totalDuration = currentUsage.totalDuration + duration,
                lastUsed = System.currentTimeMillis()
            )
            
            val usageJson = json.encodeToString(usageData)
            regularPrefs.edit()
                .putString(USAGE_DATA_KEY, usageJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadUsageData(): Map<String, ToolUsage> = withContext(Dispatchers.IO) {
        try {
            val usageJson = regularPrefs.getString(USAGE_DATA_KEY, null)
            return@withContext if (usageJson != null) {
                json.decodeFromString<Map<String, ToolUsage>>(usageJson)
            } else emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap()
        }
    }

    // Generate AI Context
    suspend fun generateAIContext(): String = withContext(Dispatchers.IO) {
        if (!_privacySettings.value.allowPersonalization) {
            return@withContext "The user prefers to keep interactions private and impersonal."
        }
        
        val context = StringBuilder()
        val profile = _userProfile.value
        val personalizedContext = _personalizedContext.value
        
        // Add user profile information
        profile?.let {
            if (it.name.isNotBlank()) {
                context.append("User's name is ${it.name}. ")
            }
            if (it.interests.isNotEmpty()) {
                context.append("User is interested in: ${it.interests.joinToString(", ")}. ")
            }
            if (it.goals.isNotEmpty()) {
                context.append("User's current goals include: ${it.goals.joinToString(", ")}. ")
            }
            if (it.communicationStyle.isNotBlank()) {
                context.append("User prefers ${it.communicationStyle} communication style. ")
            }
        }
        
        // Add personalized context
        if (personalizedContext.preferences.isNotEmpty()) {
            context.append("User preferences: ")
            personalizedContext.preferences.forEach { (key, value) ->
                context.append("$key: $value; ")
            }
        }
        
        if (personalizedContext.recentActivity.isNotEmpty()) {
            context.append("Recent activity: ${personalizedContext.recentActivity.takeLast(3).joinToString(", ")}. ")
        }
        
        // Add cooking profile if available and allowed
        if (_privacySettings.value.allowCookingData) {
            val cookingProfile = loadCookingProfile()
            cookingProfile?.let {
                if (it.skillLevel.isNotBlank()) {
                    context.append("Cooking skill level: ${it.skillLevel}. ")
                }
                if (it.dietaryRestrictions.isNotEmpty()) {
                    context.append("Dietary restrictions: ${it.dietaryRestrictions.joinToString(", ")}. ")
                }
                if (it.preferredCuisines.isNotEmpty()) {
                    context.append("Favorite cuisines: ${it.preferredCuisines.joinToString(", ")}. ")
                }
            }
        }
        
        // Add mood context if available and recent
        if (_privacySettings.value.allowWellnessData) {
            val recentMoods = loadMoodEntries()
                .filter { it.timestamp > System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000) } // Last week
                .takeLast(3)
            
            if (recentMoods.isNotEmpty()) {
                val avgMood = recentMoods.map { it.moodValue }.average()
                when {
                    avgMood <= 2.5 -> context.append("User has been feeling down lately - provide extra support and encouragement. ")
                    avgMood >= 4.0 -> context.append("User has been feeling great lately - celebrate their positive energy! ")
                    else -> context.append("User's mood has been balanced lately. ")
                }
            }
        }
        
        // Add usage patterns to help personalize responses
        if (_privacySettings.value.allowUsageAnalytics) {
            val usageData = loadUsageData()
            val mostUsedTools = usageData.values
                .sortedByDescending { it.usageCount }
                .take(3)
                .map { "${it.agentId}:${it.toolId}" }
            
            if (mostUsedTools.isNotEmpty()) {
                context.append("User frequently uses: ${mostUsedTools.joinToString(", ")}. ")
            }
        }
        
        return@withContext context.toString().trim()
    }

    // Data Export for User
    suspend fun exportUserData(): UserDataExport = withContext(Dispatchers.IO) {
        UserDataExport(
            profile = if (_privacySettings.value.allowPersonalization) _userProfile.value else null,
            personalizedContext = if (_privacySettings.value.allowPersonalization) _personalizedContext.value else null,
            cookingProfile = if (_privacySettings.value.allowCookingData) loadCookingProfile() else null,
            moodEntries = if (_privacySettings.value.allowWellnessData) loadMoodEntries() else emptyList(),
            usageData = if (_privacySettings.value.allowUsageAnalytics) loadUsageData() else emptyMap(),
            privacySettings = _privacySettings.value,
            exportDate = System.currentTimeMillis()
        )
    }

    // Complete Data Deletion
    suspend fun deleteAllUserData() = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit().clear().apply()
            regularPrefs.edit().clear().apply()
            
            // Reset all flows to default values
            _userProfile.value = null
            _personalizedContext.value = PersonalizedContext()
            _privacySettings.value = PrivacySettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun clearPersonalizationData() = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit()
                .remove(USER_PROFILE_KEY)
                .remove(PERSONALIZED_CONTEXT_KEY)
                .apply()
            
            _userProfile.value = null
            _personalizedContext.value = PersonalizedContext()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Simple key-value storage for backward compatibility
    fun saveData(key: String, value: String) {
        regularPrefs.edit().putString(key, value).apply()
    }
    
    fun getData(key: String, defaultValue: String): String {
        return regularPrefs.getString(key, defaultValue) ?: defaultValue
    }
    
    fun cleanup() {
        scope.cancel()
    }

    companion object {
        private const val USER_PROFILE_KEY = "user_profile"
        private const val PERSONALIZED_CONTEXT_KEY = "personalized_context"
        private const val COOKING_PROFILE_KEY = "cooking_profile"
        private const val MOOD_ENTRIES_KEY = "mood_entries"
        private const val PRIVACY_SETTINGS_KEY = "privacy_settings"
        private const val USAGE_DATA_KEY = "usage_data"
        private const val LAST_UPDATED_KEY = "last_updated"
    }
}

// Data models
@Serializable
data class UserProfile(
    val name: String = "",
    val age: Int? = null,
    val interests: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val communicationStyle: String = "friendly", // formal, casual, friendly, encouraging
    val timezone: String = TimeZone.getDefault().id,
    val languagePreference: String = "en",
    val accessibilityNeeds: List<String> = emptyList(),
    val createdDate: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class PersonalizedContext(
    val interests: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val preferences: Map<String, String> = emptyMap(),
    val recentActivity: List<String> = emptyList(),
    val learningHistory: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class PrivacySettings(
    val allowPersonalization: Boolean = false,
    val allowCookingData: Boolean = false,
    val allowWellnessData: Boolean = false,
    val allowUsageAnalytics: Boolean = false,
    val allowDataExport: Boolean = true,
    val dataRetentionDays: Int = 90,
    val lastReviewed: Long = System.currentTimeMillis()
)

@Serializable
data class ToolUsage(
    val agentId: String,
    val toolId: String,
    val usageCount: Int,
    val totalDuration: Long,
    val lastUsed: Long = System.currentTimeMillis()
)

@Serializable
data class MoodEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val moodValue: Int, // 1-5
    val moodLabel: String,
    val notes: String = "",
    val triggers: List<String> = emptyList()
)

@Serializable
data class UserCookingProfile(
    val skillLevel: String = "Beginner", // Beginner, Intermediate, Advanced
    val preferredCuisines: List<String> = emptyList(),
    val dietaryRestrictions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val favoriteIngredients: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val cookingGoals: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
data class UserDataExport(
    val profile: UserProfile?,
    val personalizedContext: PersonalizedContext?,
    val cookingProfile: UserCookingProfile?,
    val moodEntries: List<MoodEntry>,
    val usageData: Map<String, ToolUsage>,
    val privacySettings: PrivacySettings,
    val exportDate: Long
)