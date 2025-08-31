package com.craftflowtechnologies.guidelens.api

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GeminiLiveViewModel(context: Context) : ViewModel() {
    
    private val geminiClient = GeminiLiveApiClient()
    private val audioManager = AudioStreamManager(context)
    
    private val _uiState = MutableStateFlow(GeminiLiveUiState())
    val uiState: StateFlow<GeminiLiveUiState> = _uiState.asStateFlow()
    
    private val _responses = MutableSharedFlow<String>(extraBufferCapacity = 100)
    val responses: SharedFlow<String> = _responses.asSharedFlow()
    
    init {
        observeGeminiResponses()
        observeConnectionState()
        observeAudioData()
    }
    
    private fun observeGeminiResponses() {
        viewModelScope.launch {
            geminiClient.responses.collect { response ->
                when (response) {
                    is GeminiLiveApiClient.GeminiLiveResponse.TextResponse -> {
                        _responses.tryEmit(response.text)
                        updateSessionStats(response.text.length)
                    }
                    is GeminiLiveApiClient.GeminiLiveResponse.AudioResponse -> {
                        audioManager.playAudio(response.audioData)
                    }
                    is GeminiLiveApiClient.GeminiLiveResponse.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = response.message,
                            isLoading = false
                        )
                    }
                    is GeminiLiveApiClient.GeminiLiveResponse.SessionStarted -> {
                        _uiState.value = _uiState.value.copy(
                            isConnected = true,
                            isLoading = false,
                            error = null
                        )
                    }
                    is GeminiLiveApiClient.GeminiLiveResponse.SessionEnded -> {
                        _uiState.value = _uiState.value.copy(
                            isConnected = false,
                            isVoiceMode = false,
                            isVideoMode = false
                        )
                    }
                }
            }
        }
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            geminiClient.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = when (state) {
                        is GeminiLiveApiClient.ConnectionState.Disconnected -> "Disconnected"
                        is GeminiLiveApiClient.ConnectionState.Connecting -> "Connecting"
                        is GeminiLiveApiClient.ConnectionState.Connected -> "Connected"
                        is GeminiLiveApiClient.ConnectionState.Error -> "Error"
                    },
                    isConnected = state is GeminiLiveApiClient.ConnectionState.Connected,
                    isLoading = state is GeminiLiveApiClient.ConnectionState.Connecting
                )
            }
        }
    }
    
    private fun observeAudioData() {
        viewModelScope.launch {
            audioManager.audioData
                .sample(500) // Sample every 500ms to avoid overwhelming the API
                .collect { audioData ->
                    if (_uiState.value.isVoiceMode || _uiState.value.isVideoMode) {
                        geminiClient.sendAudioData(audioData)
                    }
                }
        }
        
        viewModelScope.launch {
            audioManager.recordingState.collect { isRecording ->
                _uiState.value = _uiState.value.copy(isListening = isRecording)
            }
        }
        
        viewModelScope.launch {
            audioManager.playbackState.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isSpeaking = isPlaying)
            }
        }
    }
    
    fun connectToGemini(agentType: String = "buddy", userTier: String = "free") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val sessionContext = GeminiSessionContext(
                userTier = userTier,
                mode = when {
                    _uiState.value.isVideoMode -> "video"
                    _uiState.value.isVoiceMode -> "voice"
                    else -> "text"
                },
                sessionType = agentType
            )
            
            geminiClient.updateSessionContext(sessionContext)
            
            val success = geminiClient.connect()
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to connect to Gemini Live API"
                )
            }
        }
    }

    fun startVoiceMode(agentType: String = "buddy") {
        if (!audioManager.hasAudioPermission()) {
            _uiState.value = _uiState.value.copy(error = "Audio permission required")
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isVoiceMode = true,
            isVideoMode = false
        )
        
        // Connect to Gemini if not already connected
        if (!_uiState.value.isConnected) {
            connectToGemini(agentType)
        }
        
        // Start audio recording
        if (audioManager.startRecording()) {
            audioManager.startPlayback() // For playing AI responses
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Failed to start audio recording",
                isVoiceMode = false
            )
        }
    }
    
    fun startVideoMode(agentType: String = "buddy") {
        _uiState.value = _uiState.value.copy(
            isVideoMode = true,
            isVoiceMode = false
        )
        
        // Video mode includes voice, so start voice functionality
        startVoiceMode(agentType)
    }
    
    fun stopVoiceMode() {
        _uiState.value = _uiState.value.copy(
            isVoiceMode = false,
            isListening = false,
            isSpeaking = false
        )
        audioManager.stopRecording()
        audioManager.stopPlayback()
    }
    
    fun stopVideoMode() {
        _uiState.value = _uiState.value.copy(
            isVideoMode = false
        )
        stopVoiceMode()
    }
    
    fun toggleMute() {
        if (_uiState.value.isListening) {
            audioManager.stopRecording()
        } else {
            audioManager.startRecording()
        }
    }
    
    fun disconnect() {
        stopVoiceMode()
        stopVideoMode()
        geminiClient.disconnect()
    }
    
    private fun updateSessionStats(tokenCount: Int) {
        val currentStats = _uiState.value.sessionStats
        _uiState.value = _uiState.value.copy(
            sessionStats = currentStats.copy(
                totalTokens = currentStats.totalTokens + tokenCount,
                messageCount = currentStats.messageCount + 1,
                estimatedCost = calculateCost(currentStats.totalTokens + tokenCount)
            )
        )
    }
    
    private fun calculateCost(tokens: Int): Double {
        // Rough cost calculation based on token usage
        // This should be updated with actual Gemini pricing
        val costPerToken = 0.000001 // $0.000001 per token (example)
        return tokens * costPerToken
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        geminiClient.cleanup()
        audioManager.cleanup()
    }
}

data class GeminiLiveUiState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val isVoiceMode: Boolean = false,
    val isVideoMode: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val connectionState: String = "Disconnected",
    val error: String? = null,
    val sessionStats: SessionStats = SessionStats()
)

data class SessionStats(
    val totalTokens: Int = 0,
    val messageCount: Int = 0,
    val sessionDuration: Long = 0,
    val estimatedCost: Double = 0.0
)