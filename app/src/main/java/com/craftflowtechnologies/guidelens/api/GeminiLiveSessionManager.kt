package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import com.craftflowtechnologies.guidelens.audio.RealTimeAudioManager
import com.craftflowtechnologies.guidelens.audio.AudioCapabilities
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Enhanced Gemini Live Session Manager for real-time video/audio interactions
 * Handles WebSocket connections, audio/video streaming, and AI responses
 */
class GeminiLiveSessionManager(
    private val context: Context,
    private val geminiLiveClient: GeminiLiveApiClient
) : ViewModel() {
    
    // Audio Manager for real-time audio processing
    private val audioManager: RealTimeAudioManager by lazy {
        RealTimeAudioManager(context)
    }
    
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.DISCONNECTED)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    
    private val _aiInsights = MutableStateFlow("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()
    
    private val _detectedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val detectedObjects: StateFlow<List<DetectedObject>> = _detectedObjects.asStateFlow()
    
    private val _emotionalContext = MutableStateFlow(EmotionalContext.NEUTRAL)
    val emotionalContext: StateFlow<EmotionalContext> = _emotionalContext.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _responseTime = MutableStateFlow(0L)
    val responseTime: StateFlow<Long> = _responseTime.asStateFlow()
    
    private val _processingQueue = MutableStateFlow(0)
    val processingQueue: StateFlow<Int> = _processingQueue.asStateFlow()
    
    // Audio streaming state flows
    private val _audioChunks = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioChunks: SharedFlow<ByteArray> = _audioChunks.asSharedFlow()
    
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()
    
    private var frameProcessingJob: Job? = null
    private var audioProcessingJob: Job? = null
    private var audioStreamingJob: Job? = null
    
    init {
        // Observe Gemini Live API responses
        viewModelScope.launch {
            geminiLiveClient.responses.collect { response ->
                handleGeminiResponse(response)
            }
        }
        
        // Observe connection state
        viewModelScope.launch {
            geminiLiveClient.connectionState.collect { state ->
                _sessionState.value = when (state) {
                    GeminiLiveApiClient.ConnectionState.Disconnected -> SessionState.DISCONNECTED
                    GeminiLiveApiClient.ConnectionState.Connecting -> SessionState.CONNECTING
                    GeminiLiveApiClient.ConnectionState.Connected -> SessionState.CONNECTED
                    GeminiLiveApiClient.ConnectionState.Error -> SessionState.DISCONNECTING
                }
            }
        }
        
        // Observe audio level from audio manager
        viewModelScope.launch {
            audioManager.audioLevel.collect { level ->
                _audioLevel.value = level
            }
        }
        
        // Observe recording state
        viewModelScope.launch {
            audioManager.isRecording.collect { recording ->
                _isRecording.value = recording
            }
        }
        
        // Observe audio playback state
        viewModelScope.launch {
            audioManager.isPlaying.collect { playing ->
                _isPlayingAudio.value = playing
            }
        }
    }
    
    suspend fun startLiveSession(agentType: String, userTier: String = "pro"): Boolean {
        return try {
            _sessionState.value = SessionState.CONNECTING
            
            // Update session context
            val context = GeminiSessionContext(
                userTier = userTier,
                mode = "video",
                sessionType = agentType,
                complexity = "complex"
            )
            geminiLiveClient.updateSessionContext(context)
            
            // Connect to Gemini Live API
            val connected = geminiLiveClient.connect()
            
            if (connected) {
                _sessionState.value = SessionState.CONNECTED
                Log.d("LiveSessionManager", "Live session started successfully")
                
                // Initialize audio components
                val audioInitialized = audioManager.initializeAudio()
                if (!audioInitialized) {
                    Log.w("LiveSessionManager", "Audio initialization failed, text-only mode")
                }
                
                // Start periodic frame processing for real-time analysis
                startFrameProcessing()
                
                // Start audio streaming if initialized
                if (audioInitialized) {
                    startAudioStreaming()
                }
            } else {
                _sessionState.value = SessionState.DISCONNECTED
                Log.e("LiveSessionManager", "Failed to start live session")
            }
            
            connected
        } catch (e: Exception) {
            Log.e("LiveSessionManager", "Error starting live session", e)
            _sessionState.value = SessionState.DISCONNECTED
            false
        }
    }
    
    fun stopLiveSession() {
        try {
            _sessionState.value = SessionState.DISCONNECTING
            
            // Stop processing jobs
            frameProcessingJob?.cancel()
            audioProcessingJob?.cancel()
            audioStreamingJob?.cancel()
            
            // Stop audio manager
            audioManager.stopRecording()
            audioManager.stopPlayback()
            audioManager.cleanup()
            
            // Disconnect from API
            geminiLiveClient.disconnect()
            
            // Reset state
            _aiInsights.value = ""
            _detectedObjects.value = emptyList()
            _emotionalContext.value = EmotionalContext.NEUTRAL
            _isProcessing.value = false
            _processingQueue.value = 0
            _isRecording.value = false
            _isPlayingAudio.value = false
            _audioLevel.value = 0f
            
            Log.d("LiveSessionManager", "Live session stopped")
        } catch (e: Exception) {
            Log.e("LiveSessionManager", "Error stopping live session", e)
        }
    }
    
    fun captureAndAnalyzeFrame(imageCapture: ImageCapture?) {
        if (_isProcessing.value || _sessionState.value != SessionState.CONNECTED) {
            return
        }
        
        viewModelScope.launch {
            _isProcessing.value = true
            _processingQueue.value += 1
            val startTime = System.currentTimeMillis()
            
            try {
                imageCapture?.let { capture ->
                    val photoFile = File(
                        context.externalCacheDir,
                        "analysis_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}.jpg"
                    )
                    
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    
                    capture.takePicture(
                        outputFileOptions,
                        { it.run() }, // Use main thread executor
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exception: ImageCaptureException) {
                                Log.e("LiveSessionManager", "Frame capture failed", exception)
                                _isProcessing.value = false
                                _processingQueue.value = (_processingQueue.value - 1).coerceAtLeast(0)
                            }
                            
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModelScope.launch {
                                    try {
                                        // Read the captured image
                                        val frameData = photoFile.readBytes()
                                        
                                        // Send to Gemini Live API with context
                                        val analysisPrompt = buildAnalysisPrompt()
                                        geminiLiveClient.sendMultimodalData(
                                            audioData = null,
                                            videoFrame = frameData,
                                            textPrompt = analysisPrompt
                                        )
                                        
                                        // Clean up temp file
                                        photoFile.delete()
                                        
                                    } catch (e: Exception) {
                                        Log.e("LiveSessionManager", "Frame analysis failed", e)
                                    } finally {
                                        _isProcessing.value = false
                                        _processingQueue.value = (_processingQueue.value - 1).coerceAtLeast(0)
                                        _responseTime.value = System.currentTimeMillis() - startTime
                                    }
                                }
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("LiveSessionManager", "Frame capture setup failed", e)
                _isProcessing.value = false
                _processingQueue.value = (_processingQueue.value - 1).coerceAtLeast(0)
            }
        }
    }
    
    fun startAudioInput(): Boolean {
        return if (_sessionState.value == SessionState.CONNECTED) {
            audioManager.startRecording()
        } else false
    }
    
    fun stopAudioInput() {
        audioManager.stopRecording()
    }
    
    fun sendAudioData(audioData: ByteArray) {
        if (_sessionState.value == SessionState.CONNECTED) {
            geminiLiveClient.sendAudioData(audioData, "audio/pcm")
        }
    }
    
    fun sendTextMessage(message: String) {
        if (_sessionState.value == SessionState.CONNECTED) {
            geminiLiveClient.sendTextMessage(message)
        }
    }
    
    private fun startFrameProcessing() {
        frameProcessingJob = viewModelScope.launch {
            while (_sessionState.value == SessionState.CONNECTED) {
                delay(2000) // Process frames every 2 seconds for real-time analysis
                
                // Auto-capture frames if not currently processing
                if (!_isProcessing.value && _processingQueue.value == 0) {
                    // This would be triggered by the UI component
                    // captureAndAnalyzeFrame(imageCapture)
                }
            }
        }
    }
    
    private fun startAudioStreaming() {
        audioStreamingJob = viewModelScope.launch {
            // Stream audio chunks from microphone to Gemini Live API
            audioManager.audioChunks.collect { audioChunk ->
                if (_sessionState.value == SessionState.CONNECTED && !_isPlayingAudio.value) {
                    try {
                        geminiLiveClient.sendAudioData(audioChunk, "audio/pcm")
                        _audioChunks.tryEmit(audioChunk) // Emit for UI visualization
                        Log.d("LiveSessionManager", "Sent audio chunk: ${audioChunk.size} bytes")
                    } catch (e: Exception) {
                        Log.e("LiveSessionManager", "Error sending audio chunk", e)
                    }
                }
            }
        }
    }
    
    private fun handleGeminiResponse(response: GeminiLiveApiClient.GeminiLiveResponse) {
        when (response) {
            is GeminiLiveApiClient.GeminiLiveResponse.TextResponse -> {
                processTextResponse(response.text)
            }
            is GeminiLiveApiClient.GeminiLiveResponse.AudioResponse -> {
                // Stop user input while playing AI response
                stopAudioInput()
                
                // Play audio response through AudioTrack
                viewModelScope.launch {
                    try {
                        audioManager.playAudio(response.audioData)
                        Log.d("LiveSessionManager", "Playing audio response: ${response.audioData.size} bytes")
                    } catch (e: Exception) {
                        Log.e("LiveSessionManager", "Error playing audio response", e)
                    }
                }
            }
            is GeminiLiveApiClient.GeminiLiveResponse.Error -> {
                Log.e("LiveSessionManager", "Gemini API error: ${response.message}")
                _aiInsights.value = "Analysis temporarily unavailable: ${response.message}"
            }
            GeminiLiveApiClient.GeminiLiveResponse.SessionStarted -> {
                Log.d("LiveSessionManager", "Gemini Live session started")
            }
            GeminiLiveApiClient.GeminiLiveResponse.SessionEnded -> {
                Log.d("LiveSessionManager", "Gemini Live session ended")
            }
        }
    }
    
    private fun processTextResponse(text: String) {
        try {
            // Parse the AI response for insights and detected objects
            _aiInsights.value = text
            
            // Extract detected objects from response (basic parsing)
            val detectedObjs = mutableListOf<DetectedObject>()
            val confidence = 0.8f + (Math.random() * 0.2f).toFloat() // Simulate confidence
            
            // Look for object mentions in the response
            val commonObjects = listOf("hand", "tool", "ingredient", "utensil", "container", "person", "face")
            commonObjects.forEach { obj ->
                if (text.lowercase().contains(obj)) {
                    detectedObjs.add(DetectedObject(
                        name = obj.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        confidence = confidence,
                        location = "Center" // This would come from actual vision API
                    ))
                }
            }
            
            _detectedObjects.value = detectedObjs
            
            // Analyze emotional context from response tone
            _emotionalContext.value = when {
                text.contains("great", ignoreCase = true) || text.contains("excellent", ignoreCase = true) -> EmotionalContext.CONFIDENT
                text.contains("careful", ignoreCase = true) || text.contains("watch", ignoreCase = true) -> EmotionalContext.FOCUSED
                text.contains("try", ignoreCase = true) || text.contains("consider", ignoreCase = true) -> EmotionalContext.CONFUSED
                text.contains("good", ignoreCase = true) || text.contains("nice", ignoreCase = true) -> EmotionalContext.EXCITED
                else -> EmotionalContext.NEUTRAL
            }
            
        } catch (e: Exception) {
            Log.e("LiveSessionManager", "Error processing text response", e)
        }
    }
    
    private fun buildAnalysisPrompt(): String {
        return """
        Analyze this video frame in real-time. Provide:
        1. What objects or activities you can see
        2. Any technique feedback or suggestions
        3. Safety considerations if applicable
        4. Step-by-step guidance if the user appears to be following a process
        
        Keep your response concise and actionable. Focus on what's most important for the user to know right now.
        """.trimIndent()
    }
    
    /**
     * Get audio capabilities of the device
     */
    fun getAudioCapabilities(): AudioCapabilities {
        return audioManager.getAudioCapabilities()
    }
    
    /**
     * Resume audio input after AI response is complete
     */
    fun resumeAudioInput() {
        if (_sessionState.value == SessionState.CONNECTED && !_isPlayingAudio.value) {
            startAudioInput()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLiveSession()
        audioManager.cleanup()
        geminiLiveClient.cleanup()
    }
}

// Enhanced data classes for better type safety
enum class SessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

data class DetectedObject(
    val name: String,
    val confidence: Float,
    val location: String
)

enum class EmotionalContext(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: androidx.compose.ui.graphics.Color
) {
    NEUTRAL("Neutral", Icons.Rounded.Face, Color.Gray),
    FOCUSED("Focused", Icons.Rounded.Visibility, Color(0xFF2196F3)),
    EXCITED("Excited", Icons.Rounded.EmojiEmotions, Color(0xFFFF9800)),
    CONFUSED("Confused", Icons.Rounded.QuestionMark, Color(0xFF9C27B0)),
    CONFIDENT("Confident", Icons.Rounded.ThumbUp, Color(0xFF4CAF50))
}