package com.craftflowtechnologies.guidelens.media

import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import com.craftflowtechnologies.guidelens.api.AudioStreamManager
import com.craftflowtechnologies.guidelens.api.GeminiLiveApiClient
import com.craftflowtechnologies.guidelens.api.GeminiSessionContext
import com.craftflowtechnologies.guidelens.audio.EnhancedAudioRecorder
import com.craftflowtechnologies.guidelens.camera.CameraManager
import com.craftflowtechnologies.guidelens.ui.TranscriptionState
import com.craftflowtechnologies.guidelens.video.VideoCaptureManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class MediaMode {
    TEXT_ONLY,
    VOICE,
    VIDEO
}

data class VoiceVideoState(
    val currentMode: MediaMode = MediaMode.TEXT_ONLY,
    val isRecording: Boolean = false,
    val isPlayingAudio: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val isCameraOn: Boolean = false,
    val isConnectedToGeminiLive: Boolean = false,
    val hasPermissions: Boolean = false,
    val errorMessage: String? = null,
    val transcriptionState: TranscriptionState = TranscriptionState(),
    val sessionDuration: Long = 0L
)

class VoiceVideoModeManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Core components
    private val audioStreamManager = AudioStreamManager(context)
    private val enhancedAudioRecorder = EnhancedAudioRecorder(context)
    private val geminiLiveClient = GeminiLiveApiClient()
    private var cameraManager: CameraManager? = null
    private var videoCaptureManager: VideoCaptureManager? = null
    
    // State management
    private val _state = MutableStateFlow(VoiceVideoState())
    val state: StateFlow<VoiceVideoState> = _state.asStateFlow()
    
    private val _transcriptionState = MutableStateFlow(TranscriptionState())
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()
    
    private var sessionStartTime: Long = 0L
    private var sessionTimer: Job? = null
    
    init {
        initializeComponents()
    }
    
    private fun initializeComponents() {
        scope.launch {
            // Monitor audio stream from AudioStreamManager
            audioStreamManager.audioData.collect { audioData ->
                if (_state.value.isConnectedToGeminiLive) {
                    geminiLiveClient.sendAudioData(audioData)
                }
            }
        }
        
        scope.launch {
            // Monitor enhanced audio recorder
            enhancedAudioRecorder.audioChunks.collect { audioChunk ->
                if (_state.value.isConnectedToGeminiLive && _state.value.currentMode in listOf(MediaMode.VOICE, MediaMode.VIDEO)) {
                    geminiLiveClient.sendAudioData(audioChunk)
                }
            }
        }
        
        scope.launch {
            // Monitor Gemini Live responses
            geminiLiveClient.responses.collect { response ->
                handleGeminiResponse(response)
            }
        }
        
        scope.launch {
            // Monitor connection state
            geminiLiveClient.connectionState.collect { connectionState ->
                _state.value = _state.value.copy(
                    isConnectedToGeminiLive = connectionState == GeminiLiveApiClient.ConnectionState.Connected
                )
            }
        }
        
        scope.launch {
            // Monitor audio recording state
            audioStreamManager.recordingState.collect { isRecording ->
                _state.value = _state.value.copy(isRecording = isRecording)
                
                // Update transcription state when recording changes
                _transcriptionState.value = _transcriptionState.value.copy(
                    isUserSpeaking = isRecording
                )
            }
        }
        
        scope.launch {
            // Monitor audio playback state
            audioStreamManager.playbackState.collect { isPlaying ->
                _state.value = _state.value.copy(isPlayingAudio = isPlaying)
            }
        }
    }
    
    suspend fun switchToVoiceMode(agentType: String, userTier: String = "free"): Boolean {
        return try {
            Log.d("VoiceVideoManager", "Switching to voice mode")
            
            // Check permissions
            if (!audioStreamManager.hasAudioPermission()) {
                _state.value = _state.value.copy(
                    errorMessage = "Microphone permission required for voice mode"
                )
                return false
            }
            
            // Connect to Gemini Live API
            val sessionContext = GeminiSessionContext(
                mode = "voice",
                sessionType = agentType,
                userTier = userTier
            )
            geminiLiveClient.updateSessionContext(sessionContext)
            
            val connected = geminiLiveClient.connect()
            if (!connected) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to connect to Gemini Live API"
                )
                return false
            }
            
            // Update state
            _state.value = _state.value.copy(
                currentMode = MediaMode.VOICE,
                hasPermissions = true,
                errorMessage = null
            )
            
            startSession()
            Log.d("VoiceVideoManager", "Voice mode activated successfully")
            true
            
        } catch (e: Exception) {
            Log.e("VoiceVideoManager", "Error switching to voice mode", e)
            _state.value = _state.value.copy(
                errorMessage = "Error activating voice mode: ${e.message}"
            )
            false
        }
    }
    
    suspend fun switchToVideoMode(
        agentType: String, 
        userTier: String = "free",
        lifecycleOwner: LifecycleOwner? = null
    ): Boolean {
        return try {
            Log.d("VoiceVideoManager", "Switching to video mode")
            
            // Check permissions for both camera and audio
            if (!audioStreamManager.hasAudioPermission()) {
                _state.value = _state.value.copy(
                    errorMessage = "Microphone and camera permissions required for video mode"
                )
                return false
            }
            
            // Initialize camera and video capture managers
            cameraManager = CameraManager(context)
            videoCaptureManager = VideoCaptureManager(context)
            
            // Monitor video frames if in video mode
            videoCaptureManager?.let { videoManager ->
                scope.launch {
                    videoManager.frameData.collect { frameData ->
                        if (_state.value.isConnectedToGeminiLive && _state.value.currentMode == MediaMode.VIDEO) {
                            // Send video frames to Gemini Live API at reduced rate for efficiency
                            geminiLiveClient.sendVideoFrame(frameData)
                        }
                    }
                }
            }
            
            // Connect to Gemini Live API with video support
            val sessionContext = GeminiSessionContext(
                mode = "video",
                sessionType = agentType,
                userTier = userTier
            )
            geminiLiveClient.updateSessionContext(sessionContext)
            
            val connected = geminiLiveClient.connect()
            if (!connected) {
                _state.value = _state.value.copy(
                    errorMessage = "Failed to connect to Gemini Live API for video mode"
                )
                return false
            }
            
            // Update state
            _state.value = _state.value.copy(
                currentMode = MediaMode.VIDEO,
                isVideoEnabled = true,
                isCameraOn = true,
                hasPermissions = true,
                errorMessage = null
            )
            
            startSession()
            Log.d("VoiceVideoManager", "Video mode activated successfully")
            true
            
        } catch (e: Exception) {
            Log.e("VoiceVideoManager", "Error switching to video mode", e)
            _state.value = _state.value.copy(
                errorMessage = "Error activating video mode: ${e.message}"
            )
            false
        }
    }
    
    fun switchToTextMode() {
        Log.d("VoiceVideoManager", "Switching to text mode")
        stopSession()
        
        _state.value = _state.value.copy(
            currentMode = MediaMode.TEXT_ONLY,
            isVideoEnabled = false,
            isCameraOn = false
        )
        
        // Disconnect from Gemini Live
        geminiLiveClient.disconnect()
    }
    
    fun startVoiceRecording(): Boolean {
        return if (_state.value.currentMode in listOf(MediaMode.VOICE, MediaMode.VIDEO)) {
            Log.d("VoiceVideoManager", "Starting voice recording")
            val success = audioStreamManager.startRecording()
            if (success) {
                _transcriptionState.value = _transcriptionState.value.copy(
                    isUserSpeaking = true,
                    currentText = ""
                )
            }
            success
        } else {
            Log.w("VoiceVideoManager", "Cannot record audio: not in voice/video mode")
            false
        }
    }
    
    fun stopVoiceRecording() {
        Log.d("VoiceVideoManager", "Stopping voice recording")
        audioStreamManager.stopRecording()
        _transcriptionState.value = _transcriptionState.value.copy(
            isUserSpeaking = false
        )
    }
    
    fun toggleMute(): Boolean {
        return if (_state.value.isRecording) {
            stopVoiceRecording()
            false
        } else {
            startVoiceRecording()
        }
    }
    
    fun sendTextMessage(message: String, agentType: String = "buddy") {
        if (_state.value.isConnectedToGeminiLive) {
            geminiLiveClient.sendTextMessage(message)
        }
    }
    
    private fun handleGeminiResponse(response: GeminiLiveApiClient.GeminiLiveResponse) {
        when (response) {
            is GeminiLiveApiClient.GeminiLiveResponse.TextResponse -> {
                Log.d("VoiceVideoManager", "Received text response: ${response.text}")
                _transcriptionState.value = _transcriptionState.value.copy(
                    currentText = response.text,
                    isUserSpeaking = false
                )
            }
            
            is GeminiLiveApiClient.GeminiLiveResponse.AudioResponse -> {
                Log.d("VoiceVideoManager", "Received audio response: ${response.audioData.size} bytes")
                // Play the audio response
                audioStreamManager.playAudio(response.audioData)
            }
            
            is GeminiLiveApiClient.GeminiLiveResponse.Error -> {
                Log.e("VoiceVideoManager", "Gemini Live error: ${response.message}")
                _state.value = _state.value.copy(
                    errorMessage = response.message
                )
            }
            
            is GeminiLiveApiClient.GeminiLiveResponse.SessionStarted -> {
                Log.d("VoiceVideoManager", "Gemini Live session started")
            }
            
            is GeminiLiveApiClient.GeminiLiveResponse.SessionEnded -> {
                Log.d("VoiceVideoManager", "Gemini Live session ended")
                stopSession()
            }
        }
    }
    
    private fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionTimer?.cancel()
        sessionTimer = scope.launch {
            while (isActive) {
                delay(1000) // Update every second
                val duration = System.currentTimeMillis() - sessionStartTime
                _state.value = _state.value.copy(sessionDuration = duration)
            }
        }
    }
    
    private fun stopSession() {
        sessionTimer?.cancel()
        audioStreamManager.stopRecording()
        audioStreamManager.stopPlayback()
        
        _state.value = _state.value.copy(
            isRecording = false,
            isPlayingAudio = false,
            sessionDuration = 0L
        )
        
        _transcriptionState.value = TranscriptionState()
    }
    
    fun cleanup() {
        Log.d("VoiceVideoManager", "Cleaning up VoiceVideoModeManager")
        stopSession()
        geminiLiveClient.cleanup()
        audioStreamManager.cleanup()
        enhancedAudioRecorder.cleanup()
        cameraManager?.cleanup()
        videoCaptureManager?.cleanup()
        scope.cancel()
    }
    
    // Utility functions
    fun isInVoiceMode(): Boolean = _state.value.currentMode == MediaMode.VOICE
    fun isInVideoMode(): Boolean = _state.value.currentMode == MediaMode.VIDEO
    fun isInTextMode(): Boolean = _state.value.currentMode == MediaMode.TEXT_ONLY
    
    fun getCurrentMode(): MediaMode = _state.value.currentMode
    fun getSessionDuration(): Long = _state.value.sessionDuration
    
    fun hasError(): Boolean = _state.value.errorMessage != null
    fun getErrorMessage(): String? = _state.value.errorMessage
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}