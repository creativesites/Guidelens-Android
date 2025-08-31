package com.craftflowtechnologies.guidelens.audio

import androidx.compose.runtime.*
import com.craftflowtechnologies.guidelens.localization.ZambianGuideLocalization
import com.craftflowtechnologies.guidelens.utils.GuideErrorManager
import com.craftflowtechnologies.guidelens.utils.GuideErrorCategory
import com.craftflowtechnologies.guidelens.utils.GuideErrorSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Contextual
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-Language Voice Commands System for GuideLens
 * Supports voice commands in English, Bemba, Nyanja, Tonga, and Lozi
 */
class GuideMultiLanguageVoice private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuideMultiLanguageVoice()
        
        private const val COMMAND_CONFIDENCE_THRESHOLD = 0.7f
        private const val MAX_COMMAND_HISTORY = 100
    }
    
    // Voice command state management
    private val _recognizedCommands = MutableStateFlow<List<GuideVoiceCommand>>(emptyList())
    val recognizedCommands: StateFlow<List<GuideVoiceCommand>> = _recognizedCommands.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow(ZambianGuideLocalization.LOCALE_ENGLISH_ZM)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _voiceCommandStats = MutableStateFlow<GuideVoiceCommandStats>(GuideVoiceCommandStats())
    val voiceCommandStats: StateFlow<GuideVoiceCommandStats> = _voiceCommandStats.asStateFlow()
    
    // Internal voice processing
    private val commandPatterns = ConcurrentHashMap<String, List<GuideCommandPattern>>()
    private val commandHistory = mutableListOf<GuideVoiceCommand>()
    private val voiceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    init {
        initializeCommandPatterns()
    }
    
    /**
     * Start listening for voice commands in the current language
     */
    suspend fun startListening(language: String = _currentLanguage.value) = withContext(Dispatchers.Main) {
        if (_isListening.value) return@withContext
        
        _isListening.value = true
        _currentLanguage.value = language
        
        try {
            // Start actual voice recognition (would integrate with Android Speech Recognizer)
            startVoiceRecognition(language)
            
        } catch (e: Exception) {
            GuideErrorManager.instance.reportError(
                exception = e,
                context = "Voice command listening failed",
                category = GuideErrorCategory.VOICE_PROCESSING,
                severity = GuideErrorSeverity.ERROR
            )
            _isListening.value = false
        }
    }
    
    /**
     * Stop listening for voice commands
     */
    suspend fun stopListening() = withContext(Dispatchers.Main) {
        _isListening.value = false
        stopVoiceRecognition()
    }
    
    /**
     * Process recognized speech and extract commands
     */
    suspend fun processRecognizedSpeech(
        recognizedText: String,
        language: String,
        confidence: Float
    ): GuideVoiceCommand? = withContext(Dispatchers.Default) {
        
        if (confidence < COMMAND_CONFIDENCE_THRESHOLD) {
            return@withContext null
        }
        
        try {
            // Clean and normalize the text
            val normalizedText = normalizeRecognizedText(recognizedText, language)
            
            // Find matching command patterns
            val matchingCommand = findMatchingCommand(normalizedText, language)
            
            if (matchingCommand != null) {
                val voiceCommand = GuideVoiceCommand(
                    id = generateCommandId(),
                    originalText = recognizedText,
                    normalizedText = normalizedText,
                    language = language,
                    command = matchingCommand.command,
                    parameters = extractParameters(normalizedText, matchingCommand),
                    confidence = confidence,
                    timestamp = System.currentTimeMillis()
                )
                
                addToHistory(voiceCommand)
                updateStats(voiceCommand)
                
                return@withContext voiceCommand
            }
            
            return@withContext null
            
        } catch (e: Exception) {
            GuideErrorManager.instance.reportError(
                exception = e,
                context = "Voice command processing failed",
                category = GuideErrorCategory.VOICE_PROCESSING,
                severity = GuideErrorSeverity.WARNING
            )
            return@withContext null
        }
    }
    
    /**
     * Execute a recognized voice command
     */
    suspend fun executeVoiceCommand(
        voiceCommand: GuideVoiceCommand,
        onCommandExecuted: (GuideVoiceCommandType, Map<String, Any>) -> Unit
    ): Boolean = withContext(Dispatchers.Default) {
        
        try {
            val executionResult = when (voiceCommand.command) {
                GuideVoiceCommandType.NEXT_STEP -> {
                    onCommandExecuted(GuideVoiceCommandType.NEXT_STEP, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.PREVIOUS_STEP -> {
                    onCommandExecuted(GuideVoiceCommandType.PREVIOUS_STEP, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.REPEAT_STEP -> {
                    onCommandExecuted(GuideVoiceCommandType.REPEAT_STEP, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.MARK_COMPLETE -> {
                    onCommandExecuted(GuideVoiceCommandType.MARK_COMPLETE, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.SET_TIMER -> {
                    val duration = voiceCommand.parameters["duration"] as? Int ?: 0
                    if (duration > 0) {
                        onCommandExecuted(GuideVoiceCommandType.SET_TIMER, mapOf("duration" to duration))
                        true
                    } else false
                }
                
                GuideVoiceCommandType.PAUSE_SESSION -> {
                    onCommandExecuted(GuideVoiceCommandType.PAUSE_SESSION, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.RESUME_SESSION -> {
                    onCommandExecuted(GuideVoiceCommandType.RESUME_SESSION, voiceCommand.parameters)
                    true
                }
                
                GuideVoiceCommandType.REQUEST_HELP -> {
                    val helpTopic = voiceCommand.parameters["topic"] as? String ?: "general"
                    onCommandExecuted(GuideVoiceCommandType.REQUEST_HELP, mapOf("topic" to helpTopic))
                    true
                }
                
                GuideVoiceCommandType.CHANGE_LANGUAGE -> {
                    val newLanguage = voiceCommand.parameters["language"] as? String
                    if (newLanguage != null && isValidLanguage(newLanguage)) {
                        _currentLanguage.value = newLanguage
                        ZambianGuideLocalization.instance.setLocale(newLanguage)
                        true
                    } else false
                }
                
                else -> false
            }
            
            // Update command execution stats
            updateExecutionStats(voiceCommand, executionResult)
            
            return@withContext executionResult
            
        } catch (e: Exception) {
            GuideErrorManager.instance.reportError(
                exception = e,
                context = "Voice command execution failed",
                category = GuideErrorCategory.VOICE_PROCESSING,
                severity = GuideErrorSeverity.ERROR
            )
            return@withContext false
        }
    }
    
    /**
     * Get available voice commands for the current language
     */
    fun getAvailableCommands(language: String = _currentLanguage.value): List<GuideVoiceCommandInfo> {
        return commandPatterns[language]?.map { pattern ->
            GuideVoiceCommandInfo(
                command = pattern.command,
                patterns = pattern.patterns,
                description = pattern.description,
                examples = pattern.examples
            )
        } ?: emptyList()
    }
    
    /**
     * Get voice command suggestions based on current context
     */
    fun getContextualSuggestions(
        context: GuideVoiceContext,
        language: String = _currentLanguage.value
    ): List<String> {
        return when (context) {
            GuideVoiceContext.COOKING_IN_PROGRESS -> {
                getLocalizedSuggestions(language, listOf(
                    "next_step", "mark_complete", "set_timer", "request_help"
                ))
            }
            GuideVoiceContext.RECIPE_READING -> {
                getLocalizedSuggestions(language, listOf(
                    "next_step", "previous_step", "repeat_step"
                ))
            }
            GuideVoiceContext.PAUSED_SESSION -> {
                getLocalizedSuggestions(language, listOf(
                    "resume_session", "request_help"
                ))
            }
            GuideVoiceContext.GENERAL -> {
                getLocalizedSuggestions(language, listOf(
                    "start_cooking", "request_help", "change_language"
                ))
            }
        }
    }
    
    /**
     * Toggle between supported languages
     */
    suspend fun switchLanguage(newLanguage: String): Boolean {
        return if (isValidLanguage(newLanguage)) {
            _currentLanguage.value = newLanguage
            ZambianGuideLocalization.instance.setLocale(newLanguage)
            
            // Restart listening with new language if currently listening
            if (_isListening.value) {
                stopListening()
                delay(500)
                startListening(newLanguage)
            }
            
            true
        } else {
            false
        }
    }
    
    // Private implementation methods
    private fun initializeCommandPatterns() {
        voiceScope.launch {
            // English (Zambian) Commands
            commandPatterns[ZambianGuideLocalization.LOCALE_ENGLISH_ZM] = listOf(
                GuideCommandPattern(
                    command = GuideVoiceCommandType.NEXT_STEP,
                    patterns = listOf("next", "next step", "continue", "move on", "proceed"),
                    description = "Move to the next step",
                    examples = listOf("Next step", "Continue", "Move on")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.PREVIOUS_STEP,
                    patterns = listOf("previous", "back", "go back", "previous step", "last step"),
                    description = "Go back to previous step",
                    examples = listOf("Go back", "Previous step")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REPEAT_STEP,
                    patterns = listOf("repeat", "say again", "repeat step", "again"),
                    description = "Repeat the current step",
                    examples = listOf("Repeat", "Say again")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.MARK_COMPLETE,
                    patterns = listOf("done", "complete", "finished", "mark done", "step complete"),
                    description = "Mark current step as complete",
                    examples = listOf("Done", "Complete", "Finished")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.SET_TIMER,
                    patterns = listOf("set timer", "timer", "set timer for", "start timer"),
                    description = "Set a cooking timer",
                    examples = listOf("Set timer for 10 minutes", "Timer 5 minutes")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REQUEST_HELP,
                    patterns = listOf("help", "help me", "I need help", "assistance", "what do I do"),
                    description = "Request help or guidance",
                    examples = listOf("Help", "I need help", "What do I do?")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.PAUSE_SESSION,
                    patterns = listOf("pause", "stop", "wait", "pause session"),
                    description = "Pause the current session",
                    examples = listOf("Pause", "Stop", "Wait")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.RESUME_SESSION,
                    patterns = listOf("resume", "continue", "start again", "resume session"),
                    description = "Resume paused session",
                    examples = listOf("Resume", "Continue", "Start again")
                )
            )
            
            // Bemba Commands
            commandPatterns[ZambianGuideLocalization.LOCALE_BEMBA] = listOf(
                GuideCommandPattern(
                    command = GuideVoiceCommandType.NEXT_STEP,
                    patterns = listOf("pitilila", "ukutila", "endelani"),
                    description = "Pitilila ku step yine",
                    examples = listOf("Pitilila", "Ukutila")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.PREVIOUS_STEP,
                    patterns = listOf("ukwafika", "bwelela", "step yashita"),
                    description = "Bwelela ku step yashita",
                    examples = listOf("Ukwafika", "Bwelela")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.MARK_COMPLETE,
                    patterns = listOf("nalefilwa", "namalila", "napela"),
                    description = "Lepesa step ule",
                    examples = listOf("Nalefilwa", "Namalila")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REQUEST_HELP,
                    patterns = listOf("ndetontonkanya", "mulafwaine", "ndelanda ubwafwano"),
                    description = "Landa ubwafwano",
                    examples = listOf("Mulafwaine", "Ndetontonkanya")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.PAUSE_SESSION,
                    patterns = listOf("imika", "lekela", "puma"),
                    description = "Imika session",
                    examples = listOf("Imika", "Lekela")
                )
            )
            
            // Nyanja Commands
            commandPatterns[ZambianGuideLocalization.LOCALE_NYANJA] = listOf(
                GuideCommandPattern(
                    command = GuideVoiceCommandType.NEXT_STEP,
                    patterns = listOf("pitirizani", "chotsata", "kenako"),
                    description = "Pitirizani ku step yotsatira",
                    examples = listOf("Pitirizani", "Chotsata")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.PREVIOUS_STEP,
                    patterns = listOf("bwererani", "kumbuyo", "step yakale"),
                    description = "Bwererani ku step yakale",
                    examples = listOf("Bwererani", "Kumbuyo")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.MARK_COMPLETE,
                    patterns = listOf("zamaliza", "tatha", "kwana"),
                    description = "Maliza step iyi",
                    examples = listOf("Zamaliza", "Tatha")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REQUEST_HELP,
                    patterns = listOf("ndithandizeni", "chithandizo", "ndisadziwa"),
                    description = "Pemphaní chithandizo",
                    examples = listOf("Ndithandizeni", "Chithandizo")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.SET_TIMER,
                    patterns = listOf("ikani timer", "werengani nthawi", "timer"),
                    description = "Ikani timer",
                    examples = listOf("Ikani timer", "Timer")
                )
            )
            
            // Tonga Commands
            commandPatterns[ZambianGuideLocalization.LOCALE_TONGA] = listOf(
                GuideCommandPattern(
                    command = GuideVoiceCommandType.NEXT_STEP,
                    patterns = listOf("enda", "limbuke", "chindi chayakutalika"),
                    description = "Enda kuchindi chayakutalika",
                    examples = listOf("Enda", "Limbuke")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.MARK_COMPLETE,
                    patterns = listOf("ndamana", "ndasiila", "ndapedzya"),
                    description = "Manina chindi ichi",
                    examples = listOf("Ndamana", "Ndasiila")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REQUEST_HELP,
                    patterns = listOf("ndibwezye", "lutabukompe", "ndali kusaka bwaafwano"),
                    description = "Landa bwaafwano",
                    examples = listOf("Ndibwezye", "Lutabukompe")
                )
            )
            
            // Lozi Commands
            commandPatterns[ZambianGuideLocalization.LOCALE_LOZI] = listOf(
                GuideCommandPattern(
                    command = GuideVoiceCommandType.NEXT_STEP,
                    patterns = listOf("tebelelang", "kenang", "sebaka se se latelang"),
                    description = "Kenang ho sebaka se se latelang",
                    examples = listOf("Tebelelang", "Kenang")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.MARK_COMPLETE,
                    patterns = listOf("ke fedile", "ke qetile", "ke tswile"),
                    description = "Qeta sebaka sena",
                    examples = listOf("Ke fedile", "Ke qetile")
                ),
                
                GuideCommandPattern(
                    command = GuideVoiceCommandType.REQUEST_HELP,
                    patterns = listOf("ke batla thuso", "ntuseng", "ke sa tsebe"),
                    description = "Batla thuso",
                    examples = listOf("Ke batla thuso", "Ntuseng")
                )
            )
        }
    }
    
    private suspend fun startVoiceRecognition(language: String) {
        // In production, this would integrate with Android's SpeechRecognizer
        // For now, we'll simulate voice recognition
        voiceScope.launch {
            while (_isListening.value) {
                delay(1000) // Simulate listening interval
                // Actual implementation would process audio input here
            }
        }
    }
    
    private suspend fun stopVoiceRecognition() {
        // Stop actual voice recognition
    }
    
    private fun normalizeRecognizedText(text: String, language: String): String {
        return text.lowercase().trim()
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Remove punctuation
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }
    
    private fun findMatchingCommand(normalizedText: String, language: String): GuideCommandPattern? {
        val patterns = commandPatterns[language] ?: return null
        
        return patterns.find { pattern ->
            pattern.patterns.any { patternText ->
                normalizedText.contains(patternText, ignoreCase = true)
            }
        }
    }
    
    private fun extractParameters(text: String, pattern: GuideCommandPattern): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()
        
        when (pattern.command) {
            GuideVoiceCommandType.SET_TIMER -> {
                // Extract duration from timer commands
                val duration = extractTimerDuration(text)
                if (duration > 0) {
                    parameters["duration"] = duration
                }
            }
            GuideVoiceCommandType.REQUEST_HELP -> {
                // Extract help topic if mentioned
                val topic = extractHelpTopic(text)
                parameters["topic"] = topic
            }
            else -> {
                // No specific parameters needed
            }
        }
        
        return parameters
    }
    
    private fun extractTimerDuration(text: String): Int {
        // Extract minutes from text like "set timer for 10 minutes"
        val minuteRegex = Regex("(\\d+)\\s*(minute|min)")
        val match = minuteRegex.find(text.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
    
    private fun extractHelpTopic(text: String): String {
        return when {
            text.contains("ingredient", ignoreCase = true) -> "ingredients"
            text.contains("technique", ignoreCase = true) -> "technique"
            text.contains("timer", ignoreCase = true) -> "timing"
            text.contains("temperature", ignoreCase = true) -> "temperature"
            else -> "general"
        }
    }
    
    private fun addToHistory(command: GuideVoiceCommand) {
        commandHistory.add(0, command)
        
        // Keep history manageable
        if (commandHistory.size > MAX_COMMAND_HISTORY) {
            commandHistory.removeAt(commandHistory.size - 1)
        }
        
        // Update recognized commands flow
        _recognizedCommands.value = commandHistory.take(10)
    }
    
    private fun updateStats(command: GuideVoiceCommand) {
        val currentStats = _voiceCommandStats.value
        val updatedStats = currentStats.copy(
            totalCommands = currentStats.totalCommands + 1,
            commandsByLanguage = currentStats.commandsByLanguage.toMutableMap().apply {
                this[command.language] = (this[command.language] ?: 0) + 1
            },
            commandsByType = currentStats.commandsByType.toMutableMap().apply {
                this[command.command] = (this[command.command] ?: 0) + 1
            },
            averageConfidence = updateMovingAverage(
                currentStats.averageConfidence,
                command.confidence,
                currentStats.totalCommands
            )
        )
        
        _voiceCommandStats.value = updatedStats
    }
    
    private fun updateExecutionStats(command: GuideVoiceCommand, success: Boolean) {
        val currentStats = _voiceCommandStats.value
        val updatedStats = currentStats.copy(
            successfulExecutions = if (success) currentStats.successfulExecutions + 1 else currentStats.successfulExecutions,
            failedExecutions = if (!success) currentStats.failedExecutions + 1 else currentStats.failedExecutions
        )
        
        _voiceCommandStats.value = updatedStats
    }
    
    private fun isValidLanguage(language: String): Boolean {
        return language in listOf(
            ZambianGuideLocalization.LOCALE_ENGLISH_ZM,
            ZambianGuideLocalization.LOCALE_BEMBA,
            ZambianGuideLocalization.LOCALE_NYANJA,
            ZambianGuideLocalization.LOCALE_TONGA,
            ZambianGuideLocalization.LOCALE_LOZI
        )
    }
    
    private fun getLocalizedSuggestions(language: String, commandKeys: List<String>): List<String> {
        val patterns = commandPatterns[language] ?: return emptyList()
        
        return commandKeys.mapNotNull { key ->
            patterns.find { pattern ->
                pattern.command.name.lowercase().contains(key.replace("_", ""))
            }?.examples?.firstOrNull()
        }
    }
    
    private fun updateMovingAverage(current: Float, newValue: Float, count: Int): Float {
        return if (count == 0) newValue else (current * count + newValue) / (count + 1)
    }
    
    private fun generateCommandId(): String {
        return "cmd_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    fun destroy() {
        voiceScope.cancel()
    }
}

// Data classes and enums
//@Serializable
data class GuideVoiceCommand(
    val id: String,
    val originalText: String,
    val normalizedText: String,
    val language: String,
    val command: GuideVoiceCommandType,
    val parameters: Map<String, @Contextual Any>,
    val confidence: Float,
    val timestamp: Long
)

//@Serializable
data class GuideCommandPattern(
    val command: GuideVoiceCommandType,
    val patterns: List<String>,
    val description: String,
    val examples: List<String>
)

//@Serializable
data class GuideVoiceCommandInfo(
    val command: GuideVoiceCommandType,
    val patterns: List<String>,
    val description: String,
    val examples: List<String>
)

//@Serializable
data class GuideVoiceCommandStats(
    val totalCommands: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0,
    val commandsByLanguage: Map<String, Int> = emptyMap(),
    val commandsByType: Map<GuideVoiceCommandType, Int> = emptyMap(),
    val averageConfidence: Float = 0f
)

enum class GuideVoiceCommandType {
    NEXT_STEP,
    PREVIOUS_STEP,
    REPEAT_STEP,
    MARK_COMPLETE,
    SET_TIMER,
    CANCEL_TIMER,
    PAUSE_SESSION,
    RESUME_SESSION,
    REQUEST_HELP,
    CHANGE_LANGUAGE,
    START_COOKING,
    STOP_COOKING,
    TAKE_PHOTO,
    SAVE_PROGRESS,
    OTHER
}

enum class GuideVoiceContext {
    COOKING_IN_PROGRESS,
    RECIPE_READING,
    PAUSED_SESSION,
    GENERAL
}

// Composable utilities
@Composable
fun rememberGuideVoiceCommands(): State<List<GuideVoiceCommand>> {
    return GuideMultiLanguageVoice.instance.recognizedCommands.collectAsState()
}

@Composable
fun rememberGuideVoiceLanguage(): State<String> {
    return GuideMultiLanguageVoice.instance.currentLanguage.collectAsState()
}

@Composable
fun rememberGuideVoiceListening(): State<Boolean> {
    return GuideMultiLanguageVoice.instance.isListening.collectAsState()
}

@Composable
fun rememberGuideVoiceStats(): State<GuideVoiceCommandStats> {
    return GuideMultiLanguageVoice.instance.voiceCommandStats.collectAsState()
}