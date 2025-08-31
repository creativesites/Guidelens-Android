package com.craftflowtechnologies.guidelens.api

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GeminiLiveApiClient {
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _responses = MutableSharedFlow<GeminiLiveResponse>(extraBufferCapacity = 100)
    val responses: SharedFlow<GeminiLiveResponse> = _responses.asSharedFlow()
    
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errors: SharedFlow<String> = _errors.asSharedFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _voiceActivityLevel = MutableStateFlow(0f)
    val voiceActivityLevel: StateFlow<Float> = _voiceActivityLevel.asStateFlow()
    
    private var currentSessionContext: GeminiSessionContext = GeminiSessionContext()
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        object Error : ConnectionState()
    }
    
    sealed class GeminiLiveResponse {
        data class TextResponse(val text: String, val isComplete: Boolean = true) : GeminiLiveResponse()
        data class AudioResponse(val audioData: ByteArray, val isComplete: Boolean = false) : GeminiLiveResponse()
        data class Error(val message: String, val code: String? = null) : GeminiLiveResponse()
        object SessionStarted : GeminiLiveResponse()
        object SessionEnded : GeminiLiveResponse()
    }
    
    fun updateSessionContext(context: GeminiSessionContext) {
        currentSessionContext = context
    }
    
    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        if (_connectionState.value == ConnectionState.Connected) {
            continuation.resume(true)
            return@suspendCoroutine
        }
        
        _connectionState.value = ConnectionState.Connecting
        
        val request = Request.Builder()
            .url("${GeminiConfig.GEMINI_LIVE_ENDPOINT}?key=${GeminiConfig.GEMINI_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLive", "WebSocket connection opened")
                _connectionState.value = ConnectionState.Connected
                _responses.tryEmit(GeminiLiveResponse.SessionStarted)
                
                // Send setup message immediately after connection
                sendSetupMessage("buddy")
                
                continuation.resume(true)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("GeminiLive", "Received message: $text")
                handleMessage(text)
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                Log.d("GeminiLive", "Received binary message: ${bytes.size} bytes")
                _responses.tryEmit(GeminiLiveResponse.AudioResponse(bytes.toByteArray()))
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("GeminiLive", "WebSocket closing: $code $reason")
                _connectionState.value = ConnectionState.Disconnected
                _responses.tryEmit(GeminiLiveResponse.SessionEnded)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("GeminiLive", "WebSocket closed: $code $reason")
                _connectionState.value = ConnectionState.Disconnected
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLive", "WebSocket error", t)
                _connectionState.value = ConnectionState.Error
                _errors.tryEmit("Connection failed: ${t.message}")
                _responses.tryEmit(GeminiLiveResponse.Error("Connection failed: ${t.message}"))
                continuation.resume(false)
            }
        }
        
        webSocket = okHttpClient.newWebSocket(request, listener)
    }
    
    private fun handleMessage(message: String) {
        try {
            // Parse the Gemini Live API response
            val response = json.decodeFromString<Map<String, Any>>(message)
            
            when {
                // Handle setupComplete message
                response.containsKey("setupComplete") -> {
                    Log.d("GeminiLive", "Setup completed successfully")
                    // Session is ready for input
                }
                
                // Handle serverContent responses
                response.containsKey("serverContent") -> {
                    val serverContent = response["serverContent"] as? Map<String, Any>
                    val modelTurn = serverContent?.get("modelTurn") as? Map<String, Any>
                    val parts = modelTurn?.get("parts") as? List<Map<String, Any>>
                    
                    parts?.forEach { part ->
                        // Handle text responses
                        val text = part["text"] as? String
                        text?.let {
                            _responses.tryEmit(GeminiLiveResponse.TextResponse(it))
                        }
                        
                        // Handle audio responses
                        val inlineData = part["inlineData"] as? Map<String, Any>
                        if (inlineData?.get("mimeType") as? String == "audio/pcm") {
                            val audioBase64 = inlineData["data"] as? String
                            audioBase64?.let { base64 ->
                                val audioData = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                                _responses.tryEmit(GeminiLiveResponse.AudioResponse(audioData))
                            }
                        }
                    }
                }
                
                // Handle turnComplete messages
                response.containsKey("turnComplete") -> {
                    Log.d("GeminiLive", "Turn completed")
                    _isSpeaking.value = false
                }
                
                // Handle error responses
                response.containsKey("error") -> {
                    val error = response["error"] as? Map<String, Any>
                    val errorMessage = error?.get("message") as? String ?: "Unknown error"
                    val code = error?.get("code") as? String
                    _responses.tryEmit(GeminiLiveResponse.Error(errorMessage, code))
                }
                
                else -> {
                    Log.d("GeminiLive", "Received unknown message type: $message")
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLive", "Failed to parse message: $message", e)
            _responses.tryEmit(GeminiLiveResponse.Error("Failed to parse response: ${e.message}"))
        }
    }
    
    fun sendSetupMessage(agentType: String = "buddy") {
        if (_connectionState.value != ConnectionState.Connected) {
            _errors.tryEmit("Not connected to Gemini Live API")
            return
        }
        
        val systemPrompt = getSystemPromptForAgent(agentType)
        val setupMessage = LiveSetupMessage(
            setup = SetupData(
                model = "models/gemini-2.0-flash-exp",
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    maxOutputTokens = getMaxTokensForTier(currentSessionContext.userTier)
                ),
                systemInstruction = SystemInstruction(
                    parts = listOf(InstructionPart(text = systemPrompt))
                )
            )
        )
        
        try {
            val jsonMessage = json.encodeToString(setupMessage)
            webSocket?.send(jsonMessage)
            Log.d("GeminiLive", "Sent setup message: $jsonMessage")
        } catch (e: Exception) {
            Log.e("GeminiLive", "Failed to send setup message", e)
            _errors.tryEmit("Failed to send setup message: ${e.message}")
        }
    }
    
    fun sendTextMessage(message: String) {
        if (_connectionState.value != ConnectionState.Connected) {
            _errors.tryEmit("Not connected to Gemini Live API")
            return
        }
        
        val clientContentMessage = LiveClientContentMessage(
            clientContent = ClientContent(
                turns = listOf(
                    Turn(
                        role = "user",
                        parts = listOf(TurnPart(text = message))
                    )
                ),
                turnComplete = true
            )
        )
        
        try {
            val jsonMessage = json.encodeToString(clientContentMessage)
            webSocket?.send(jsonMessage)
            Log.d("GeminiLive", "Sent text message: $jsonMessage")
        } catch (e: Exception) {
            Log.e("GeminiLive", "Failed to send text message", e)
            _errors.tryEmit("Failed to send text message: ${e.message}")
        }
    }
    
    fun sendAudioData(audioData: ByteArray, mimeType: String = "audio/pcm") {
        if (_connectionState.value != ConnectionState.Connected) {
            _errors.tryEmit("Not connected to Gemini Live API")
            return
        }
        
        val audioBase64 = android.util.Base64.encodeToString(audioData, android.util.Base64.NO_WRAP)
        val realtimeInputMessage = LiveRealtimeInputMessage(
            realtimeInput = RealtimeInput(
                mediaChunks = listOf(
                    MediaChunk(
                        mimeType = "audio/pcm;rate=16000",
                        data = audioBase64
                    )
                )
            )
        )
        
        try {
            val jsonMessage = json.encodeToString(realtimeInputMessage)
            webSocket?.send(jsonMessage)
            Log.d("GeminiLive", "Sent audio data: ${audioData.size} bytes")
        } catch (e: Exception) {
            Log.e("GeminiLive", "Failed to send audio data", e)
            _errors.tryEmit("Failed to send audio: ${e.message}")
        }
    }
    
    fun sendVideoFrame(frameData: ByteArray, mimeType: String = "image/jpeg") {
        if (_connectionState.value != ConnectionState.Connected) {
            _errors.tryEmit("Not connected to Gemini Live API")
            return
        }
        
        val frameBase64 = android.util.Base64.encodeToString(frameData, android.util.Base64.NO_WRAP)
        val realtimeInputMessage = LiveRealtimeInputMessage(
            realtimeInput = RealtimeInput(
                mediaChunks = listOf(
                    MediaChunk(
                        mimeType = mimeType,
                        data = frameBase64
                    )
                )
            )
        )
        
        try {
            val jsonMessage = json.encodeToString(realtimeInputMessage)
            webSocket?.send(jsonMessage)
            Log.d("GeminiLive", "Sent video frame: ${frameData.size} bytes")
        } catch (e: Exception) {
            Log.e("GeminiLive", "Failed to send video frame", e)
            _errors.tryEmit("Failed to send video: ${e.message}")
        }
    }
    
    fun sendMultimodalData(audioData: ByteArray?, videoFrame: ByteArray?, textPrompt: String? = null) {
        if (_connectionState.value != ConnectionState.Connected) {
            _errors.tryEmit("Not connected to Gemini Live API")
            return
        }
        
        val mediaChunks = mutableListOf<MediaChunk>()
        
        // Add audio data if provided
        audioData?.let { audio ->
            val audioBase64 = android.util.Base64.encodeToString(audio, android.util.Base64.NO_WRAP)
            mediaChunks.add(
                MediaChunk(
                    mimeType = "audio/pcm;rate=16000",
                    data = audioBase64
                )
            )
        }
        
        // Add video frame if provided
        videoFrame?.let { frame ->
            val frameBase64 = android.util.Base64.encodeToString(frame, android.util.Base64.NO_WRAP)
            mediaChunks.add(
                MediaChunk(
                    mimeType = "image/jpeg",
                    data = frameBase64
                )
            )
        }
        
        // Send text as client content if provided
        textPrompt?.let { text ->
            sendTextMessage(text)
        }
        
        // Send media chunks if any
        if (mediaChunks.isNotEmpty()) {
            val realtimeInputMessage = LiveRealtimeInputMessage(
                realtimeInput = RealtimeInput(
                    mediaChunks = mediaChunks
                )
            )
            
            try {
                val jsonMessage = json.encodeToString(realtimeInputMessage)
                webSocket?.send(jsonMessage)
                Log.d("GeminiLive", "Sent multimodal data: audio=${audioData?.size}, video=${videoFrame?.size}, text=${textPrompt != null}")
            } catch (e: Exception) {
                Log.e("GeminiLive", "Failed to send multimodal data", e)
                _errors.tryEmit("Failed to send multimodal data: ${e.message}")
            }
        }
    }
    
    private fun getSystemPromptForAgent(agentType: String): String {
        return when (agentType) {
            "cooking" -> """You are the Cooking Assistant agent in GuideLens. 
                Provide real-time cooking guidance by analyzing what the user is doing through their camera.
                Focus on: technique correction, timing, safety, ingredient substitutions.
                Be encouraging and specific. Always prioritize food safety."""
            
            "crafting" -> """You are the Crafting Guru agent in GuideLens.
                Guide users through craft projects with real-time visual feedback.
                Focus on: technique improvement, tool usage, project troubleshooting.
                Be patient and detail-oriented. Celebrate progress and creativity."""
            
            "diy" -> """You are the DIY Helper agent in GuideLens.
                Assist with home improvement and repair projects through visual guidance.
                Focus on: safety first, proper tool use, step-by-step guidance.
                Be safety-conscious and methodical. Always emphasize proper safety gear."""
            
            "buddy" -> """You are Buddy, the friendly general assistant in GuideLens.
                Help with any skill or learning task not covered by specialized agents.
                Focus on: encouragement, learning support, general guidance.
                Be supportive, adaptable, and enthusiastic about learning."""
            
            else -> "You are a helpful AI assistant providing guidance and support."
        }
    }
    
    private fun getMaxTokensForTier(userTier: String): Int {
        return when (userTier) {
            "free" -> GeminiConfig.SessionLimits.FREE_TOKEN_LIMIT
            "basic" -> GeminiConfig.SessionLimits.BASIC_TOKEN_LIMIT
            "pro" -> GeminiConfig.SessionLimits.PRO_TOKEN_LIMIT
            else -> GeminiConfig.SessionLimits.FREE_TOKEN_LIMIT
        }
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        _connectionState.value = ConnectionState.Disconnected
    }
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}