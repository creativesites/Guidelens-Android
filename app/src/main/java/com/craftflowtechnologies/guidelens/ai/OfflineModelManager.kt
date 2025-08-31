package com.craftflowtechnologies.guidelens.ai

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages offline AI model inference for GuideLens
 *
 * This manager handles:
 * - Network connectivity detection
 * - Local model downloading and caching
 * - Fallback between offline and online models
 * - Simple text generation for basic chat functionality
 */
class OfflineModelManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "OfflineModelManager"
        private const val PREFS_NAME = "offline_model_prefs"
        private const val PREF_MODEL_DOWNLOADED = "model_downloaded"
        private const val PREF_MODEL_VERSION = "model_version"
        private const val PREF_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
        private const val MODEL_DIR = "offline_models"
        private const val CURRENT_MODEL_VERSION = "1.0"

        // Simple rule-based responses for offline mode
        private val OFFLINE_RESPONSES = mapOf(
            "cooking" to listOf(
                "Let me help you with that cooking task! I'm working offline, so I'll provide basic guidance.",
                "For cooking advice while offline, I recommend checking the basics: timing, temperature, and ingredients.",
                "I can help with general cooking tips offline. For detailed recipes, please connect to the internet.",
                "Safety first in the kitchen! Make sure you have proper ventilation and clean workspace."
            ),
            "crafting" to listOf(
                "I can provide basic crafting guidance while offline. What project are you working on?",
                "For crafting projects, remember to organize your materials and workspace first.",
                "While offline, I can give general crafting tips. Complex tutorials work better with internet connection.",
                "Take your time with crafting - precision often matters more than speed."
            ),
            "diy" to listOf(
                "Safety is key for DIY projects! I can provide basic guidance while offline.",
                "For DIY work, always start by checking you have the right tools and safety equipment.",
                "While offline, I'll help with general DIY principles. Complex projects need online resources.",
                "Measure twice, cut once - that's a fundamental DIY rule that works offline or online!"
            ),
            "buddy" to listOf(
                "I'm here to help! While I'm offline, my responses will be more general but still useful.",
                "Even offline, I can provide encouragement and basic guidance for your tasks.",
                "Working offline means simpler responses, but I'm still your helpful assistant!",
                "Let me help you with what I can while we're offline. What do you need assistance with?"
            )
        )
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _modelStatus = MutableStateFlow(ModelStatus.NOT_DOWNLOADED)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _isOfflineModeEnabled = MutableStateFlow(
        sharedPreferences.getBoolean(PREF_OFFLINE_MODE_ENABLED, false)
    )
    val isOfflineModeEnabled: StateFlow<Boolean> = _isOfflineModeEnabled.asStateFlow()

    private var responseIndex = 0

    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
        LOADING,
        READY,
        ERROR
    }

    init {
        checkModelStatus()
        updateNetworkStatus()
    }

    /**
     * Enable or disable offline mode
     */
    fun setOfflineModeEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(PREF_OFFLINE_MODE_ENABLED, enabled)
            .apply()
        _isOfflineModeEnabled.value = enabled

        Log.d(TAG, "Offline mode ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if offline mode is available (enabled AND model ready)
     */
    fun isOfflineModeAvailable(): Boolean {
        return _isOfflineModeEnabled.value && _modelStatus.value == ModelStatus.READY
    }

    /**
     * Check if we should use offline mode
     * Only use offline if:
     * 1. Offline mode is enabled by user
     * 2. Model is ready
     * 3. No internet connection
     */
    fun shouldUseOfflineMode(): Boolean {
        val offlineAvailable = isOfflineModeAvailable()
        val isOffline = !_isOnline.value

        Log.d(TAG, "Should use offline mode? Enabled: ${_isOfflineModeEnabled.value}, " +
                "Model ready: ${_modelStatus.value == ModelStatus.READY}, " +
                "Is offline: $isOffline")

        return offlineAvailable && isOffline
    }

    /**
     * Check if model is initialized and ready
     */
    fun isModelInitialized(): Boolean {
        return _modelStatus.value == ModelStatus.READY
    }

    /**
     * Check if we're online and have network connectivity
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            false
        }
    }

    /**
     * Update network status
     */
    fun updateNetworkStatus() {
        _isOnline.value = isNetworkAvailable()
    }

    /**
     * Generate response using offline model or fallback
     */
    suspend fun generateOfflineResponse(
        prompt: String,
        agentType: String,
        context: String = "",
        historyContext: String
    ): Result<String> = withContext(Dispatchers.Default) {
        return@withContext try {
            // Check if offline mode is actually available
            if (!isOfflineModeAvailable()) {
                return@withContext Result.failure(
                    Exception("Offline mode is not available. Please enable offline mode and download the model.")
                )
            }

            // For now, use rule-based responses
            // In a full implementation, this would use a local TensorFlow Lite model
            val responses = OFFLINE_RESPONSES[agentType] ?: OFFLINE_RESPONSES["buddy"]!!

            val baseResponse = responses[responseIndex % responses.size]
            responseIndex++

            // Add some context-aware logic
            val contextualResponse = generateContextualResponse(prompt, agentType, baseResponse)

            Result.success("🔷 Offline Mode: $contextualResponse")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating offline response", e)
            Result.failure(e)
        }
    }

    /**
     * Generate context-aware responses based on the user's input
     */
    private fun generateContextualResponse(prompt: String, agentType: String, baseResponse: String): String {
        val lowerPrompt = prompt.lowercase()

        return when (agentType) {
            "cooking" -> when {
                lowerPrompt.contains("recipe") -> "I can provide basic cooking guidance offline. For detailed recipes, consider reconnecting to the internet for the best experience."
                lowerPrompt.contains("temperature") || lowerPrompt.contains("time") -> "Cooking times and temperatures vary by dish. Generally, use medium heat and check frequently when unsure."
                lowerPrompt.contains("ingredient") -> "For ingredient substitutions, think about similar textures and flavors. Online mode provides better substitution suggestions."
                lowerPrompt.contains("safety") -> "Kitchen safety basics: wash hands, clean surfaces, check temperatures, and avoid cross-contamination."
                else -> baseResponse
            }
            "crafting" -> when {
                lowerPrompt.contains("material") -> "Choose materials based on your project needs. Quality materials often lead to better results."
                lowerPrompt.contains("tool") -> "Using the right tools makes crafting easier. Make sure your tools are clean and sharp when needed."
                lowerPrompt.contains("pattern") -> "For patterns and detailed instructions, online mode provides access to comprehensive guides."
                else -> baseResponse
            }
            "diy" -> when {
                lowerPrompt.contains("tool") -> "Always use the right tool for the job and wear appropriate safety gear."
                lowerPrompt.contains("safety") -> "DIY safety: wear protective equipment, work in good lighting, and don't rush."
                lowerPrompt.contains("measure") -> "Accurate measurements are crucial for DIY success. Double-check before cutting or drilling."
                else -> baseResponse
            }
            else -> when {
                lowerPrompt.contains("help") -> "I'm here to assist! While offline, my responses are more general but I'll do my best to help."
                lowerPrompt.contains("how") -> "I can provide general guidance offline. For detailed instructions, online mode works better."
                else -> baseResponse
            }
        }
    }

    /**
     * Check if local model is available
     */
    private fun checkModelStatus() {
        val isDownloaded = sharedPreferences.getBoolean(PREF_MODEL_DOWNLOADED, false)
        val version = sharedPreferences.getString(PREF_MODEL_VERSION, "")

        _modelStatus.value = when {
            !isDownloaded -> ModelStatus.NOT_DOWNLOADED
            version != CURRENT_MODEL_VERSION -> ModelStatus.NOT_DOWNLOADED
            else -> ModelStatus.READY // Assume ready if downloaded with correct version
        }
    }

    /**
     * Download local model (placeholder for actual implementation)
     */
    suspend fun downloadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            _modelStatus.value = ModelStatus.DOWNLOADING

            // Simulate download process
            // In a real implementation, this would download a compressed model file
            kotlinx.coroutines.delay(3000)

            // Mark as downloaded
            sharedPreferences.edit()
                .putBoolean(PREF_MODEL_DOWNLOADED, true)
                .putString(PREF_MODEL_VERSION, CURRENT_MODEL_VERSION)
                .apply()

            _modelStatus.value = ModelStatus.READY

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            _modelStatus.value = ModelStatus.ERROR
            Result.failure(e)
        }
    }

    /**
     * Load the local model into memory (placeholder)
     */
    suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.Default) {
        return@withContext try {
            if (_modelStatus.value != ModelStatus.DOWNLOADED) {
                return@withContext Result.failure(Exception("Model not downloaded"))
            }

            _modelStatus.value = ModelStatus.LOADING

            // Simulate loading process
            kotlinx.coroutines.delay(2000)

            _modelStatus.value = ModelStatus.READY

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            _modelStatus.value = ModelStatus.ERROR
            Result.failure(e)
        }
    }

    /**
     * Clear downloaded model to free space
     */
    fun clearModel() {
        try {
            val modelDir = File(context.filesDir, MODEL_DIR)
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }

            sharedPreferences.edit()
                .putBoolean(PREF_MODEL_DOWNLOADED, false)
                .remove(PREF_MODEL_VERSION)
                .apply()

            _modelStatus.value = ModelStatus.NOT_DOWNLOADED

            Log.d(TAG, "Model cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing model", e)
        }
    }

    /**
     * Get model size estimation (in MB)
     */
    fun getEstimatedModelSize(): Int {
        // Gemma 3B would be quite large, but we're using a placeholder
        // For a real lightweight model, this might be 50-200MB
        return 150 // MB
    }

    /**
     * Check if device has enough storage for the model
     */
    fun hasEnoughStorage(): Boolean {
        return try {
            val availableBytes = context.filesDir.freeSpace
            val requiredBytes = getEstimatedModelSize() * 1024 * 1024L
            availableBytes > requiredBytes * 2 // 2x buffer
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage", e)
            false
        }
    }

    /**
     * Get offline mode availability status
     */
    fun getOfflineCapabilityStatus(): String {
        return when {
            !_isOfflineModeEnabled.value -> "Offline mode disabled"
            _modelStatus.value == ModelStatus.NOT_DOWNLOADED -> "Enable offline mode and download model in settings"
            _modelStatus.value == ModelStatus.DOWNLOADING -> "Downloading offline model..."
            _modelStatus.value == ModelStatus.DOWNLOADED -> "Offline model ready to load"
            _modelStatus.value == ModelStatus.LOADING -> "Loading offline model..."
            _modelStatus.value == ModelStatus.READY -> "Offline mode ready - will activate when offline"
            _modelStatus.value == ModelStatus.ERROR -> "Offline mode error - please retry download"
            else -> "Unknown offline status"
        }
    }
}