package com.craftflowtechnologies.guidelens.video

import android.content.Context
import android.util.Log
import com.craftflowtechnologies.guidelens.api.GeminiVideoClient
import com.craftflowtechnologies.guidelens.api.VideoAnalysisResult
import com.craftflowtechnologies.guidelens.ui.Agent
import com.craftflowtechnologies.guidelens.ui.AnalysisMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enterprise-grade Video Session Manager for real-time Gemini video analysis
 * Manages:
 * - Real-time video frame processing
 * - Analysis queue and rate limiting
 * - Session state and context
 * - Agent-specific analysis modes
 * - Performance monitoring
 */
class GeminiVideoSessionManager(
    private val context: Context,
    private val videoClient: GeminiVideoClient
) {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisorJob)
    
    // Session state
    private val _sessionState = MutableStateFlow<VideoSessionState>(VideoSessionState.Idle)
    val sessionState: StateFlow<VideoSessionState> = _sessionState.asStateFlow()
    
    private val _currentAnalysis = MutableStateFlow<VideoAnalysisResult?>(null)
    val currentAnalysis: StateFlow<VideoAnalysisResult?> = _currentAnalysis.asStateFlow()
    
    private val _analysisHistory = MutableStateFlow<List<VideoAnalysisEntry>>(emptyList())
    val analysisHistory: StateFlow<List<VideoAnalysisEntry>> = _analysisHistory.asStateFlow()
    
    // Analysis queue and processing
    private val analysisQueue = ConcurrentLinkedQueue<AnalysisRequest>()
    private var analysisJob: Job? = null
    private var lastAnalysisTime = 0L
    private val minAnalysisInterval = 2000L // 2 seconds minimum between analyses
    
    // Session configuration
    private var currentAgent: Agent? = null
    private var analysisMode: AnalysisMode = AnalysisMode.REAL_TIME
    private var sessionId: String? = null
    private var isRealTimeEnabled = false
    
    companion object {
        private const val TAG = "GeminiVideoSessionManager"
        private const val MAX_QUEUE_SIZE = 10
        private const val MAX_HISTORY_SIZE = 50
    }
    
    /**
     * Start a new video analysis session
     */
    fun startSession(agent: Agent, mode: AnalysisMode = AnalysisMode.REAL_TIME): String {
        Log.d(TAG, "Starting video session with ${agent.name} in $mode mode")
        
        // Stop any existing session
        stopSession()
        
        // Initialize new session
        sessionId = "session_${System.currentTimeMillis()}"
        currentAgent = agent
        analysisMode = mode
        isRealTimeEnabled = mode == AnalysisMode.REAL_TIME
        
        _sessionState.value = VideoSessionState.Starting
        
        // Start analysis processing loop
        analysisJob = scope.launch {
            processAnalysisQueue()
        }
        
        _sessionState.value = VideoSessionState.Active(sessionId!!, agent, mode)
        
        return sessionId!!
    }
    
    /**
     * Stop the current session and cleanup
     */
    fun stopSession() {
        Log.d(TAG, "Stopping video session")
        
        analysisJob?.cancel()
        analysisQueue.clear()
        
        _sessionState.value = VideoSessionState.Idle
        _currentAnalysis.value = null
        
        sessionId = null
        currentAgent = null
        isRealTimeEnabled = false
    }
    
    /**
     * Queue a video frame for analysis
     */
    fun analyzeFrame(frameFile: File, customPrompt: String? = null) {
        val agent = currentAgent ?: return
        
        if (analysisQueue.size >= MAX_QUEUE_SIZE) {
            Log.w(TAG, "Analysis queue full, dropping oldest request")
            analysisQueue.poll()
        }
        
        val prompt = customPrompt ?: generateAgentSpecificPrompt(agent, analysisMode)
        
        analysisQueue.offer(
            AnalysisRequest.FrameAnalysis(
                frameFile = frameFile,
                prompt = prompt,
                timestamp = System.currentTimeMillis(),
                agent = agent,
                mode = analysisMode
            )
        )
        
        Log.d(TAG, "Queued frame analysis, queue size: ${analysisQueue.size}")
    }
    
    /**
     * Queue a video file for analysis
     */
    fun analyzeVideo(
        videoFile: File,
        customPrompt: String? = null,
        startTimeSeconds: Int? = null,
        endTimeSeconds: Int? = null,
        fps: Int = 1
    ) {
        val agent = currentAgent ?: return
        val prompt = customPrompt ?: generateAgentSpecificPrompt(agent, analysisMode)
        
        analysisQueue.offer(
            AnalysisRequest.VideoAnalysis(
                videoFile = videoFile,
                prompt = prompt,
                startTimeSeconds = startTimeSeconds,
                endTimeSeconds = endTimeSeconds,
                fps = fps,
                timestamp = System.currentTimeMillis(),
                agent = agent,
                mode = analysisMode
            )
        )
        
        Log.d(TAG, "Queued video analysis")
    }
    
    /**
     * Analyze YouTube video with timestamp
     */
    fun analyzeYouTube(
        youtubeUrl: String,
        customPrompt: String? = null,
        timestampSeconds: Int? = null
    ) {
        val agent = currentAgent ?: return
        val prompt = customPrompt ?: generateAgentSpecificPrompt(agent, analysisMode)
        
        analysisQueue.offer(
            AnalysisRequest.YouTubeAnalysis(
                youtubeUrl = youtubeUrl,
                prompt = prompt,
                timestampSeconds = timestampSeconds,
                timestamp = System.currentTimeMillis(),
                agent = agent,
                mode = analysisMode
            )
        )
        
        Log.d(TAG, "Queued YouTube analysis")
    }
    
    /**
     * Switch analysis mode
     */
    fun switchAnalysisMode(newMode: AnalysisMode) {
        Log.d(TAG, "Switching analysis mode from $analysisMode to $newMode")
        analysisMode = newMode
        isRealTimeEnabled = newMode == AnalysisMode.REAL_TIME
        
        // Update session state
        val currentState = _sessionState.value
        if (currentState is VideoSessionState.Active) {
            _sessionState.value = currentState.copy(analysisMode = newMode)
        }
    }
    
    /**
     * Get current session metrics
     */
    fun getSessionMetrics(): SessionMetrics {
        val history = _analysisHistory.value
        return SessionMetrics(
            sessionId = sessionId,
            totalAnalyses = history.size,
            successfulAnalyses = history.count { it.result is VideoAnalysisResult.Success },
            failedAnalyses = history.count { it.result is VideoAnalysisResult.Error },
            averageAnalysisTime = if (history.isNotEmpty()) {
                history.mapNotNull { it.processingTimeMs }.average()
            } else 0.0,
            queueSize = analysisQueue.size,
            isRealTimeActive = isRealTimeEnabled && _sessionState.value is VideoSessionState.Active
        )
    }
    
    private suspend fun processAnalysisQueue() {
        while (analysisJob?.isActive == true) {
            try {
                val request = analysisQueue.poll()
                if (request != null) {
                    processAnalysisRequest(request)
                } else {
                    delay(100) // Short delay when queue is empty
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing analysis queue", e)
                delay(1000) // Longer delay on error
            }
        }
    }
    
    private suspend fun processAnalysisRequest(request: AnalysisRequest) {
        // Rate limiting
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTime < minAnalysisInterval) {
            delay(minAnalysisInterval - (now - lastAnalysisTime))
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            _sessionState.value = when (val current = _sessionState.value) {
                is VideoSessionState.Active -> current.copy(isProcessing = true)
                else -> current
            }
            
            val result = when (request) {
                is AnalysisRequest.FrameAnalysis -> {
                    videoClient.analyzeVideoFrame(request.frameFile, request.prompt)
                }
                is AnalysisRequest.VideoAnalysis -> {
                    videoClient.analyzeVideoFile(
                        request.videoFile,
                        request.prompt,
                        request.fps,
                        request.startTimeSeconds,
                        request.endTimeSeconds
                    )
                }
                is AnalysisRequest.YouTubeAnalysis -> {
                    videoClient.analyzeYouTubeVideo(
                        request.youtubeUrl,
                        request.prompt,
                        request.timestampSeconds
                    )
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Update current analysis
            _currentAnalysis.value = result
            
            // Add to history
            val entry = VideoAnalysisEntry(
                id = "analysis_${System.currentTimeMillis()}",
                timestamp = request.timestamp,
                result = result,
                prompt = request.prompt,
                agent = request.agent,
                analysisMode = request.mode,
                processingTimeMs = processingTime
            )
            
            addToHistory(entry)
            
            lastAnalysisTime = System.currentTimeMillis()
            
            Log.d(TAG, "Analysis completed in ${processingTime}ms: ${result::class.simpleName}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            _currentAnalysis.value = VideoAnalysisResult.Error("Analysis failed: ${e.message}")
        } finally {
            _sessionState.value = when (val current = _sessionState.value) {
                is VideoSessionState.Active -> current.copy(isProcessing = false)
                else -> current
            }
        }
    }
    
    private fun addToHistory(entry: VideoAnalysisEntry) {
        val currentHistory = _analysisHistory.value.toMutableList()
        currentHistory.add(0, entry) // Add to beginning
        
        // Keep only the most recent entries
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _analysisHistory.value = currentHistory
    }
    
    private fun generateAgentSpecificPrompt(agent: Agent, mode: AnalysisMode): String {
        val basePrompt = when (agent.id) {
            "cooking" -> "As a professional cooking instructor, analyze this cooking scene."
            "crafting" -> "As an expert crafting mentor, analyze this crafting activity."
            "diy" -> "As a skilled DIY and home improvement expert, analyze this project."
            "companion" -> "As a helpful general assistant, analyze what's happening in this scene."
            else -> "Analyze this video scene and provide helpful guidance."
        }
        
        val modeSpecificGuidance = when (mode) {
            AnalysisMode.REAL_TIME -> "Focus on immediate feedback and real-time guidance for what's currently happening."
            AnalysisMode.STEP_BY_STEP -> "Provide detailed step-by-step instructions and break down the process clearly."
            AnalysisMode.SAFETY_FOCUS -> "Prioritize safety observations, proper protective equipment, and safe practices."
            AnalysisMode.TECHNIQUE -> "Focus on technique analysis, form correction, and skill improvement suggestions."
            AnalysisMode.TROUBLESHOOT -> "Identify any problems, errors, or issues and provide specific solutions."
            AnalysisMode.LEARNING -> "Focus on technique analysis, form correction, and skill improvement suggestions."
            AnalysisMode.CREATIVE -> "Focus on technique analysis, form correction, and skill improvement suggestions."
        }
        
        return "$basePrompt $modeSpecificGuidance Be specific, actionable, and encouraging in your response."
    }
    
    fun cleanup() {
        supervisorJob.cancel()
        analysisQueue.clear()
    }
}


// Session state management
sealed class VideoSessionState {
    object Idle : VideoSessionState()
    object Starting : VideoSessionState()
    data class Active(
        val sessionId: String,
        val agent: Agent,
        val analysisMode: AnalysisMode,
        val isProcessing: Boolean = false
    ) : VideoSessionState()
    data class Error(val message: String) : VideoSessionState()
}

// Analysis request types
sealed class AnalysisRequest {
    abstract val timestamp: Long
    abstract val agent: Agent
    abstract val mode: AnalysisMode
    abstract val prompt: String
    
    data class FrameAnalysis(
        val frameFile: File,
        override val prompt: String,
        override val timestamp: Long,
        override val agent: Agent,
        override val mode: AnalysisMode
    ) : AnalysisRequest()
    
    data class VideoAnalysis(
        val videoFile: File,
        override val prompt: String,
        val startTimeSeconds: Int?,
        val endTimeSeconds: Int?,
        val fps: Int,
        override val timestamp: Long,
        override val agent: Agent,
        override val mode: AnalysisMode
    ) : AnalysisRequest()
    
    data class YouTubeAnalysis(
        val youtubeUrl: String,
        override val prompt: String,
        val timestampSeconds: Int?,
        override val timestamp: Long,
        override val agent: Agent,
        override val mode: AnalysisMode
    ) : AnalysisRequest()
}

// Analysis history entry
data class VideoAnalysisEntry(
    val id: String,
    val timestamp: Long,
    val result: VideoAnalysisResult,
    val prompt: String,
    val agent: Agent,
    val analysisMode: AnalysisMode,
    val processingTimeMs: Long
)

// Session metrics
data class SessionMetrics(
    val sessionId: String?,
    val totalAnalyses: Int,
    val successfulAnalyses: Int,
    val failedAnalyses: Int,
    val averageAnalysisTime: Double,
    val queueSize: Int,
    val isRealTimeActive: Boolean
)