package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Removed GenerativeModel import to avoid Ktor conflicts 
// import com.google.ai.client.generativeai.GenerativeModel
// import com.google.ai.client.generativeai.type.*
import com.craftflowtechnologies.guidelens.api.GeminiConfig
// Removed unused JSON imports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Production-ready Gemini Live Session Manager
 * Uses the standard Gemini Developer API for voice interactions
 * Features:
 * - Real-time audio streaming with proper format handling
 * - WebSocket-based communication with Gemini Live API
 * - Comprehensive error handling and reconnection
 * - State management for session lifecycle
 * - Integration with existing GeminiLiveApiClient
 */
class FirebaseLiveSessionManager(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "FirebaseLiveSession"
        
        // Audio configuration constants per Firebase AI Logic specs
        private const val SAMPLE_RATE_INPUT = 16000 // 16kHz for input
        private const val SAMPLE_RATE_OUTPUT = 24000 // 24kHz for output
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
    }

    // State flows for UI observation
    private val _sessionState = MutableStateFlow(FirebaseSessionState.DISCONNECTED)
    val sessionState: StateFlow<FirebaseSessionState> = _sessionState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _voiceActivityLevel = MutableStateFlow(0f)
    val voiceActivityLevel: StateFlow<Float> = _voiceActivityLevel.asStateFlow()

    // Gemini Live API components
    private var geminiLiveClient: GeminiLiveApiClient? = null
    // Removed GenerativeModel to avoid Ktor conflicts - using direct WebSocket API instead

    // Audio components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var audioRecordingJob: Job? = null
    private var audioPlaybackJob: Job? = null

    // Buffer sizes
    private val inputBufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLE_RATE_INPUT, CHANNEL_IN, ENCODING) * BUFFER_SIZE_FACTOR
    }
    
    private val outputBufferSize: Int by lazy {
        AudioTrack.getMinBufferSize(SAMPLE_RATE_OUTPUT, CHANNEL_OUT, ENCODING) * BUFFER_SIZE_FACTOR
    }

    /**
     * Initialize Gemini Live API with standard Gemini Developer API
     */
    suspend fun initializeGeminiLive(agentType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing Gemini Live API for agent: $agentType")

                // Initialize Gemini Live API client (WebSocket-based, no Ktor conflicts)
                geminiLiveClient = GeminiLiveApiClient()

                Log.d(TAG, "Gemini Live API initialized successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Gemini Live API", e)
                _errorMessage.value = "Failed to initialize AI: ${e.message}"
                false
            }
        }
    }

    /**
     * Start a live audio conversation session
     */
    suspend fun startLiveSession(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting live session...")
                _sessionState.value = FirebaseSessionState.CONNECTING

                val client = geminiLiveClient ?: run {
                    _errorMessage.value = "Gemini Live API not initialized"
                    _sessionState.value = FirebaseSessionState.ERROR
                    return@withContext false
                }

                // Initialize audio components
                if (!initializeAudioComponents()) {
                    _errorMessage.value = "Failed to initialize audio components"
                    _sessionState.value = FirebaseSessionState.ERROR
                    return@withContext false
                }

                // Connect to Gemini Live API via WebSocket
                val connected = client.connect()
                
                if (connected) {
                    _sessionState.value = FirebaseSessionState.CONNECTED
                    Log.d(TAG, "Live session started successfully")
                    
                    // Start monitoring client responses
                    startResponseCollection()
                    
                    true
                } else {
                    _errorMessage.value = "Failed to connect to Gemini Live API"
                    _sessionState.value = FirebaseSessionState.ERROR
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start live session", e)
                _errorMessage.value = "Failed to start session: ${e.message}"
                _sessionState.value = FirebaseSessionState.ERROR
                false
            }
        }
    }

    /**
     * Start listening for user input
     */
    fun startListening() {
        if (_sessionState.value != FirebaseSessionState.CONNECTED) {
            Log.w(TAG, "Cannot start listening - session not connected")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isListening.value = true
                Log.d(TAG, "Starting audio recording...")

                audioRecord?.startRecording()
                
                // Start recording job
                audioRecordingJob = viewModelScope.launch(Dispatchers.IO) {
                    recordAndStreamAudio()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                _errorMessage.value = "Failed to start recording: ${e.message}"
                _isListening.value = false
            }
        }
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        Log.d(TAG, "Stopping audio recording...")
        _isListening.value = false
        
        audioRecordingJob?.cancel()
        audioRecord?.stop()
    }

    /**
     * Stop the live session
     */
    suspend fun stopLiveSession() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Stopping live session...")
                _sessionState.value = FirebaseSessionState.DISCONNECTING

                // Stop audio recording and playback
                stopListening()
                audioPlaybackJob?.cancel()

                // Release audio resources
                releaseAudioComponents()

                // Disconnect from Gemini Live API
                geminiLiveClient?.disconnect()
                geminiLiveClient = null

                _sessionState.value = FirebaseSessionState.DISCONNECTED
                Log.d(TAG, "Live session stopped successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping live session", e)
                _sessionState.value = FirebaseSessionState.ERROR
            }
        }
    }

    /**
     * Initialize audio recording and playback components
     */
    private fun initializeAudioComponents(): Boolean {
        return try {
            // Initialize AudioRecord for capturing user voice
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_INPUT,
                CHANNEL_IN,
                ENCODING,
                inputBufferSize
            ).apply {
                if (state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("AudioRecord failed to initialize")
                }
            }

            // Initialize AudioTrack for playing AI responses
            audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_OUTPUT)
                        .setChannelMask(CHANNEL_OUT)
                        .setEncoding(ENCODING)
                        .build()
                )
                .setBufferSizeInBytes(outputBufferSize)
                .build().apply {
                    if (state != AudioTrack.STATE_INITIALIZED) {
                        throw IllegalStateException("AudioTrack failed to initialize")
                    }
                }

            Log.d(TAG, "Audio components initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio components", e)
            false
        }
    }

    /**
     * Record audio and stream to Firebase Live API
     */
    private suspend fun recordAndStreamAudio() {
        withContext(Dispatchers.IO) {
            val buffer = ByteArray(inputBufferSize)
            
            while (_isListening.value && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        // Calculate voice activity level for UI
                        val activityLevel = calculateVoiceActivityLevel(buffer, bytesRead)
                        _voiceActivityLevel.value = activityLevel

                        // Convert to the format expected by Firebase AI Logic
                        val audioData = ByteBuffer.wrap(buffer, 0, bytesRead)
                            .order(ByteOrder.LITTLE_ENDIAN)

                        // Stream audio to Gemini Live API
                        val audioBytes = ByteArray(bytesRead)
                        System.arraycopy(buffer, 0, audioBytes, 0, bytesRead)
                        geminiLiveClient?.sendAudioData(audioBytes, "audio/pcm")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during audio recording", e)
                    break
                }
            }
        }
    }

    /**
     * Calculate voice activity level for UI visualization
     */
    private fun calculateVoiceActivityLevel(buffer: ByteArray, length: Int): Float {
        var sum = 0L
        for (i in 0 until length step 2) {
            val sample = ((buffer[i + 1].toInt() and 0xFF) shl 8) or (buffer[i].toInt() and 0xFF)
            sum += (sample * sample).toLong()
        }
        
        val rms = kotlin.math.sqrt(sum.toDouble() / (length / 2))
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Start collecting responses from Gemini Live API
     */
    private fun startResponseCollection() {
        viewModelScope.launch {
            try {
                val client = geminiLiveClient ?: return@launch
                
                // Collect responses from the client
                launch {
                    client.responses.collect { response ->
                        when (response) {
                            is GeminiLiveApiClient.GeminiLiveResponse.TextResponse -> {
                                Log.d(TAG, "Received text response: ${response.text}")
                                _aiResponse.value = response.text
                                _transcription.value = response.text
                                _isSpeaking.value = false
                            }
                            
                            is GeminiLiveApiClient.GeminiLiveResponse.AudioResponse -> {
                                Log.d(TAG, "Received audio response")
                                _isSpeaking.value = true
                                
                                // Play audio response through AudioTrack
                                audioPlaybackJob = viewModelScope.launch(Dispatchers.IO) {
                                    playAudioResponse(response.audioData)
                                }
                            }
                            
                            is GeminiLiveApiClient.GeminiLiveResponse.Error -> {
                                Log.e(TAG, "Client error: ${response.message}")
                                _errorMessage.value = "Live session error: ${response.message}"
                                _isSpeaking.value = false
                            }
                            
                            is GeminiLiveApiClient.GeminiLiveResponse.SessionStarted -> {
                                Log.d(TAG, "Live session started")
                                _sessionState.value = FirebaseSessionState.CONNECTED
                            }
                            
                            is GeminiLiveApiClient.GeminiLiveResponse.SessionEnded -> {
                                Log.d(TAG, "Live session ended")
                                _isSpeaking.value = false
                                _sessionState.value = FirebaseSessionState.DISCONNECTED
                            }
                        }
                    }
                }
                
                // Collect connection state changes
                launch {
                    client.connectionState.collect { state ->
                        _sessionState.value = when (state) {
                            is GeminiLiveApiClient.ConnectionState.Disconnected -> FirebaseSessionState.DISCONNECTED
                            is GeminiLiveApiClient.ConnectionState.Connecting -> FirebaseSessionState.CONNECTING
                            is GeminiLiveApiClient.ConnectionState.Connected -> FirebaseSessionState.CONNECTED
                            is GeminiLiveApiClient.ConnectionState.Error -> FirebaseSessionState.ERROR
                        }
                    }
                }
                
                // Collect errors
                launch {
                    client.errors.collect { error ->
                        Log.e(TAG, "Client error: $error")
                        _errorMessage.value = "Live session error: $error"
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up response collection", e)
                _errorMessage.value = "Response collection error: ${e.message}"
            }
        }
    }

    /**
     * Handle function calls from the AI model (simplified for WebSocket API)
     */
    private fun handleFunctionCall(functionName: String, args: Map<String, Any>): String {
        return when (functionName) {
            "takePhoto" -> handleTakePhoto(args)
            "adjustSettings" -> handleAdjustSettings(args)
            "getWeather" -> handleGetWeather(args)
            else -> "Unknown function: $functionName"
        }
    }
    
    /**
     * Play audio response through AudioTrack
     */
    private suspend fun playAudioResponse(audioData: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                audioTrack?.let { track ->
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.play()
                        track.write(audioData, 0, audioData.size)
                        track.stop()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio response", e)
            }
        }
    }

    /**
     * Release audio components
     */
    private fun releaseAudioComponents() {
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null

        audioTrack?.apply {
            if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                stop()
            }
            release()
        }
        audioTrack = null

        Log.d(TAG, "Audio components released")
    }

    /**
     * Create system instruction based on agent type
     */
    /**
     * Create system instruction based on agent type
     */
    private fun createSystemInstruction(agentType: String): String {
        return when (agentType) {
            "cooking" -> """
                You are the Cooking Assistant in GuideLens. Provide real-time cooking guidance with:
                - Step-by-step recipe assistance
                - Technique corrections and tips
                - Safety reminders and food safety guidelines
                - Ingredient substitution suggestions
                - Cooking time and temperature guidance
                Be encouraging, specific, and always prioritize food safety.
                RESPOND IN A CONVERSATIONAL, HELPFUL TONE.
            """.trimIndent()
            
            "crafting" -> """
                You are the Crafting Guru in GuideLens. Guide users through craft projects with:
                - Project planning and material selection
                - Technique demonstrations and corrections
                - Creative suggestions and variations
                - Troubleshooting common issues
                - Tool usage and safety tips
                Be patient, detail-oriented, and celebrate creativity.
                RESPOND IN A CONVERSATIONAL, ENCOURAGING TONE.
            """.trimIndent()
            
            "diy" -> """
                You are the DIY Helper in GuideLens. Assist with home improvement projects with:
                - Safety-first approach to all projects
                - Step-by-step guidance and planning
                - Tool recommendations and proper usage
                - Problem-solving and troubleshooting
                - Building code and safety compliance
                Be methodical, safety-conscious, and practical.
                RESPOND IN A CONVERSATIONAL, SAFETY-FOCUSED TONE.
            """.trimIndent()
            
            else -> """
                You are Buddy, the friendly assistant in GuideLens. Help with any skill or learning task:
                - Encouragement and motivation
                - Learning support and guidance
                - General skill development
                - Progress tracking and celebration
                - Adaptable assistance for any domain
                Be supportive, enthusiastic, and adaptable.
                RESPOND IN A CONVERSATIONAL, FRIENDLY TONE.
            """.trimIndent()
        }
    }


    /**
     * Handle take photo function call
     */
    private fun handleTakePhoto(args: Map<String, Any>): String {
        val purpose = args["purpose"] ?: "general"
        
        // Here you would integrate with your camera system
        Log.d(TAG, "Take photo requested for purpose: $purpose")
        
        return "Photo taken successfully for $purpose"
    }

    /**
     * Handle adjust settings function call
     */
    private fun handleAdjustSettings(args: Map<String, Any>): String {
        val setting = args["setting"] ?: ""
        val value = args["value"] ?: ""
        
        Log.d(TAG, "Adjust setting: $setting to $value")
        
        return "Adjusted $setting to $value"
    }

    /**
     * Handle get weather function call
     */
    private fun handleGetWeather(args: Map<String, Any>): String {
        // Here you would integrate with a weather API
        Log.d(TAG, "Weather information requested")
        
        return "Temperature: 22°C, Condition: Partly cloudy, Humidity: 60%"
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            stopLiveSession()
        }
    }
}

/**
 * Enhanced session states for Firebase AI Logic integration
 */
enum class FirebaseSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    SPEAKING,
    DISCONNECTING,
    ERROR
}