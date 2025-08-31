package com.craftflowtechnologies.guidelens.api

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import com.craftflowtechnologies.guidelens.ui.Agent
import com.craftflowtechnologies.guidelens.ui.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RealtimeApiManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "RealtimeApiManager"
        private const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY_HERE" // Add your OpenAI API key here
        private const val REALTIME_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01"

        // Audio configuration - Fixed values
        private const val SAMPLE_RATE = 24000 // OpenAI Realtime API expects 24kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE = 1024
        private const val OUTPUT_SAMPLE_RATE = 24000 // For output audio
    }

    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlayingAudio = false
    private var currentAgent: Agent? = null

    // Audio processing
    private val audioQueue: Queue<ByteArray> = LinkedList()
    private var fullAnswerText = StringBuilder()
    private var audioExecutor = Executors.newSingleThreadExecutor()

    // State flows
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun connect(selectedAgent: Agent) {
        if (_isConnected.value) {
            Log.w(TAG, "Already connected")
            return
        }

        currentAgent = selectedAgent
        _connectionStatus.value = "Connecting..."

        val request = Request.Builder()
            .url(REALTIME_URL)
            .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _isConnected.value = true
                _connectionStatus.value = "Connected"
                this@RealtimeApiManager.webSocket = webSocket
                // Session will be updated when session.created event is received
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                handleWebSocketMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed", t)
                _isConnected.value = false
                _connectionStatus.value = "Connection failed: ${t.message}"
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                _isConnected.value = false
                _connectionStatus.value = "Disconnecting"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _isConnected.value = false
                _connectionStatus.value = "Disconnected"
                cleanup()
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val eventJson = JSONObject(text)
            val eventType = eventJson.optString("type")

            Log.d(TAG, "Handling event: $eventType")

            when (eventType) {
                "session.created" -> {
                    Log.d(TAG, "Session created. Sending session update.")
                    sendSessionUpdate()
                }

                "session.updated" -> {
                    Log.d(TAG, "Session updated. Ready for audio.")
                    _connectionStatus.value = "Ready"
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val transcript = eventJson.optString("transcript", "")
                    Log.d(TAG, "User said: $transcript")

                    if (transcript.isNotEmpty()) {
                        addMessage(ChatMessage(
                            text = transcript,
                            isFromUser = true,
                            timestamp = getCurrentTime(),
                            isVoiceMessage = true
                        ))
                    }
                }

                "response.created" -> {
                    Log.d(TAG, "Response created")
                    fullAnswerText.clear()
                }

                "response.text.delta" -> {
                    val deltaText = eventJson.optString("delta", "")
                    Log.d(TAG, "Text delta: $deltaText")
                    fullAnswerText.append(deltaText)
                    updateOrAddAIMessage(fullAnswerText.toString())
                }

                "response.audio_transcript.delta" -> {
                    val deltaText = eventJson.optString("delta", "")
                    Log.d(TAG, "Audio transcript delta: $deltaText")
                    fullAnswerText.append(deltaText)
                    updateOrAddAIMessage(fullAnswerText.toString())
                }

                "input_audio_buffer.speech_started" -> {
                    Log.d(TAG, "Speech started")
                    _isListening.value = true
                    stopAudioPlayback()
                    clearAudioQueue()
                }

                "input_audio_buffer.speech_stopped" -> {
                    Log.d(TAG, "Speech stopped")
                    _isListening.value = false
                    // Create response when speech stops
                    createResponse()
                }

                "response.audio.delta" -> {
                    val audioData = eventJson.optString("delta", "")
                    if (audioData.isNotEmpty()) {
                        try {
                            val decodedAudio = Base64.decode(audioData, Base64.DEFAULT)
                            synchronized(audioQueue) {
                                audioQueue.add(decodedAudio)
                            }
                            processAudioQueue()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding audio", e)
                        }
                    }
                }

                "response.audio.done" -> {
                    Log.d(TAG, "Audio response completed")
                    // Don't set speaking to false here, let audio playback finish
                }

                "response.done" -> {
                    Log.d(TAG, "Response completed")
                    // Response is complete, but audio might still be playing
                }

                "error" -> {
                    val error = eventJson.optJSONObject("error")
                    val errorMessage = error?.optString("message", "Unknown error") ?: "Unknown error"
                    Log.e(TAG, "Server error: $errorMessage")
                    _connectionStatus.value = "Error: $errorMessage"

                    // Add error message to chat
                    addMessage(ChatMessage(
                        text = "Error: $errorMessage",
                        isFromUser = false,
                        timestamp = getCurrentTime(),
                        isVoiceMessage = false
                    ))
                }

                else -> {
                    Log.d(TAG, "Unhandled event type: $eventType")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WebSocket message", e)
        }
    }

    private fun sendSessionUpdate() {
        val instructions = currentAgent?.let { agent ->
            "You are ${agent.name}, a ${agent.description.lowercase()}. " +
                    "You are a helpful, witty, and friendly AI assistant. " +
                    "Act like a human, but remember that you aren't a human and that you can't do human things in the real world. " +
                    "Your voice and personality should be warm and engaging, with a lively and playful tone. " +
                    "Keep responses concise and conversational."
        } ?: "You are a helpful, witty, and friendly AI assistant."

        val sessionConfig = JSONObject().apply {
            put("type", "session.update")
            put("session", JSONObject().apply {
                put("instructions", instructions)
                put("voice", "alloy")
                put("temperature", 0.8)
                put("max_response_output_tokens", 4096)
                put("modalities", JSONArray().apply {
                    put("text")
                    put("audio")
                })
                put("input_audio_format", "pcm16")
                put("output_audio_format", "pcm16")
                put("input_audio_transcription", JSONObject().apply {
                    put("model", "whisper-1")
                })
                put("turn_detection", JSONObject().apply {
                    put("type", "server_vad")
                    put("threshold", 0.5)
                    put("prefix_padding_ms", 300)
                    put("silence_duration_ms", 500)
                })
                put("tool_choice", "auto")
            })
        }

        Log.d(TAG, "Sending session update: ${sessionConfig.toString()}")
        webSocket?.send(sessionConfig.toString())
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (!_isConnected.value) {
            Log.w(TAG, "Not connected to start listening")
            return
        }

        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        _isListening.value = true
        startAudioRecording()
    }

    fun stopListening() {
        _isListening.value = false
        if (isRecording) {
            stopAudioRecording()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startAudioRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = Math.max(minBufferSize, BUFFER_SIZE * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            Log.d(TAG, "Started audio recording with buffer size: $bufferSize")

            audioExecutor.execute {
                val audioBuffer = ByteArray(bufferSize)

                try {
                    while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val readBytes = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                        if (readBytes > 0) {
                            val audioData = audioBuffer.copyOf(readBytes)
                            val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
                            sendAudioData(base64Audio)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during audio recording", e)
                } finally {
                    audioRecord?.stop()
                    audioRecord?.release()
                    audioRecord = null
                    Log.d(TAG, "Audio recording stopped")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            isRecording = false
            _isListening.value = false
        }
    }

    private fun stopAudioRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "Stopped audio recording")
    }

    private fun sendAudioData(base64Audio: String) {
        val json = JSONObject().apply {
            put("type", "input_audio_buffer.append")
            put("audio", base64Audio)
        }
        webSocket?.send(json.toString())
    }

    private fun createResponse() {
        val responseJson = JSONObject().apply {
            put("type", "response.create")
            put("response", JSONObject().apply {
                put("modalities", JSONArray().apply {
                    put("text")
                    put("audio")
                })
            })
        }
        webSocket?.send(responseJson.toString())
    }

    private fun processAudioQueue() {
        if (!isPlayingAudio) {
            audioExecutor.execute {
                isPlayingAudio = true
                _isSpeaking.value = true

                try {
                    while (synchronized(audioQueue) { audioQueue.isNotEmpty() }) {
                        val audioData = synchronized(audioQueue) {
                            audioQueue.poll()
                        }
                        if (audioData != null) {
                            playAudio(audioData)
                        }
                    }
                } finally {
                    isPlayingAudio = false
                    _isSpeaking.value = false
                }
            }
        }
    }

    private fun playAudio(audioData: ByteArray) {
        try {
            if (audioTrack == null) {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    OUTPUT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    OUTPUT_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                    AudioTrack.MODE_STREAM
                )
                audioTrack?.play()
            }

            audioTrack?.write(audioData, 0, audioData.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        }
    }

    private fun stopAudioPlayback() {
        audioTrack?.stop()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
        isPlayingAudio = false
        _isSpeaking.value = false
    }

    private fun clearAudioQueue() {
        synchronized(audioQueue) {
            audioQueue.clear()
        }
    }

    fun sendTextMessage(text: String) {
        if (!_isConnected.value) {
            Log.w(TAG, "Not connected to send text message")
            return
        }

        // Add user message to chat
        addMessage(ChatMessage(
            text = text,
            isFromUser = true,
            timestamp = getCurrentTime(),
            isVoiceMessage = false
        ))

        // Send message to server
        val messageJson = JSONObject().apply {
            put("type", "conversation.item.create")
            put("item", JSONObject().apply {
                put("type", "message")
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "input_text")
                        put("text", text)
                    })
                })
            })
        }

        webSocket?.send(messageJson.toString())
        Log.d(TAG, "Sent text message: $text")

        // Request response
        val responseJson = JSONObject().apply {
            put("type", "response.create")
            put("response", JSONObject().apply {
                put("modalities", JSONArray().apply {
                    put("text")
                    put("audio")
                })
            })
        }

        webSocket?.send(responseJson.toString())
    }

    private fun addMessage(message: ChatMessage) {
        coroutineScope.launch {
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(message)
            _messages.value = currentMessages
        }
    }

    private fun updateOrAddAIMessage(text: String) {
        coroutineScope.launch {
            val currentMessages = _messages.value.toMutableList()

            // Find the last AI message or create a new one
            val lastMessageIndex = currentMessages.indexOfLast { !it.isFromUser }

            if (lastMessageIndex != -1 &&
                currentMessages[lastMessageIndex].timestamp == getCurrentTime()) {
                // Update existing AI message from same timeframe
                currentMessages[lastMessageIndex] = currentMessages[lastMessageIndex].copy(text = text)
            } else {
                // Add new AI message
                currentMessages.add(ChatMessage(
                    text = text,
                    isFromUser = false,
                    timestamp = getCurrentTime(),
                    isVoiceMessage = true
                ))
            }

            _messages.value = currentMessages
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
        fullAnswerText.clear()
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting")
        cleanup()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _isConnected.value = false
        _connectionStatus.value = "Disconnected"
    }

    private fun cleanup() {
        stopAudioRecording()
        stopAudioPlayback()
        clearAudioQueue()
        _isListening.value = false
        _isSpeaking.value = false
        audioExecutor.shutdown()
        audioExecutor = Executors.newSingleThreadExecutor()
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}