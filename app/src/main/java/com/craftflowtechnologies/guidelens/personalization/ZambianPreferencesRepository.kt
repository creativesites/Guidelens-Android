package com.craftflowtechnologies.guidelens.personalization

import android.content.Context
import android.util.Log
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Repository for managing Zambian personalization preferences
 * Handles both local storage and future Supabase integration
 */
class ZambianPreferencesRepository(
    private val context: Context,
    private val userDataManager: UserDataManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true 
    }
    
    private val _zambianPreferences = MutableStateFlow(ZambianUserPreferences())
    val zambianPreferences: StateFlow<ZambianUserPreferences> = _zambianPreferences.asStateFlow()
    
    init {
        scope.launch {
            loadZambianPreferences()
        }
    }
    
    suspend fun saveZambianPreferences(preferences: ZambianUserPreferences) = withContext(Dispatchers.IO) {
        try {
            val preferencesJson = json.encodeToString(preferences)
            userDataManager.saveData(ZAMBIAN_PREFERENCES_KEY, preferencesJson)
            _zambianPreferences.value = preferences
            
            Log.d("ZambianPrefs", "Saved Zambian preferences: $preferences")
        } catch (e: Exception) {
            Log.e("ZambianPrefs", "Failed to save preferences", e)
        }
    }
    
    private suspend fun loadZambianPreferences() = withContext(Dispatchers.IO) {
        try {
            val preferencesJson = userDataManager.getData(ZAMBIAN_PREFERENCES_KEY, "")
            if (preferencesJson.isNotEmpty()) {
                val preferences = json.decodeFromString<ZambianUserPreferences>(preferencesJson)
                _zambianPreferences.value = preferences
                Log.d("ZambianPrefs", "Loaded Zambian preferences: $preferences")
            } else {
                // Set default Zambian preferences
                val defaultPreferences = ZambianUserPreferences()
                saveZambianPreferences(defaultPreferences)
            }
        } catch (e: Exception) {
            Log.e("ZambianPrefs", "Failed to load preferences", e)
            // Fallback to defaults
            _zambianPreferences.value = ZambianUserPreferences()
        }
    }
    
    suspend fun updateLanguage(language: ZambianLocalizationManager.ZambianLanguage) {
        val current = _zambianPreferences.value
        saveZambianPreferences(current.copy(preferredLanguage = language.code))
    }
    
    suspend fun updateRegion(region: ZambianLocalizationManager.ZambianRegion) {
        val current = _zambianPreferences.value
        saveZambianPreferences(current.copy(preferredRegion = region.name))
    }
    
    suspend fun updateCulturalSettings(settings: ZambianLocalizationManager.CulturalSettings) {
        val current = _zambianPreferences.value
        saveZambianPreferences(
            current.copy(
                useTraditionalGreetings = settings.useTraditionalGreetings,
                showLocalPrayers = settings.showLocalPrayers,
                useLocalTimeFormat = settings.useLocalTimeFormat,
                showSeasonalGuidance = settings.showSeasonalGuidance,
                includeFamilyContext = settings.includeFamilyContext,
                respectElders = settings.respectElders,
                communityOriented = settings.communityOriented,
                useLocalMeasurements = settings.useLocalMeasurements,
                showLocalFestivals = settings.showLocalFestivals,
                useUbuntuPhilosophy = settings.useUbuntuPhilosophy
            )
        )
    }
    
    suspend fun updateLocalFeatures(features: ZambianLocalizationManager.LocalFeatures) {
        val current = _zambianPreferences.value
        saveZambianPreferences(
            current.copy(
                enableLocalCuisine = features.enableLocalCuisine,
                enableTraditionalCrafts = features.enableTraditionalCrafts,
                enableLocalFarming = features.enableLocalFarming,
                enableMiningSupport = features.enableMiningSupport,
                enableLocalBusinessSupport = features.enableLocalBusinessSupport,
                enableEducationSupport = features.enableEducationSupport,
                enableCommunityHelp = features.enableCommunityHelp
            )
        )
    }
    
    fun cleanup() {
        scope.cancel()
    }
    
    companion object {
        private const val ZAMBIAN_PREFERENCES_KEY = "zambian_preferences"
    }
}

@Serializable
data class ZambianUserPreferences(
    val preferredLanguage: String = "en", // English by default
    val preferredRegion: String = "LUSAKA", // Lusaka by default
    
    // Cultural Settings
    val useTraditionalGreetings: Boolean = true,
    val showLocalPrayers: Boolean = true,
    val useLocalTimeFormat: Boolean = true,
    val showSeasonalGuidance: Boolean = true,
    val includeFamilyContext: Boolean = true,
    val respectElders: Boolean = true,
    val communityOriented: Boolean = true,
    val useLocalMeasurements: Boolean = true,
    val showLocalFestivals: Boolean = true,
    val useUbuntuPhilosophy: Boolean = true,
    
    // Local Features
    val enableLocalCuisine: Boolean = true,
    val enableTraditionalCrafts: Boolean = true,
    val enableLocalFarming: Boolean = true,
    val enableMiningSupport: Boolean = true,
    val enableLocalBusinessSupport: Boolean = true,
    val enableEducationSupport: Boolean = true,
    val enableCommunityHelp: Boolean = true,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)