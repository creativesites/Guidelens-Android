package com.craftflowtechnologies.guidelens.audio

import android.content.Context
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Real-time audio manager for Gemini Live API
 * Handles microphone capture, audio streaming, and playback
 */
class RealTimeAudioManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RealTimeAudioManager"
        
        // Audio configuration for Gemini Live API
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2
        
        // Chunk size for streaming (recommended by Gemini Live API)
        private const val CHUNK_SIZE_MS = 100 // 100ms chunks
        private const val CHUNK_SIZE_BYTES = (SAMPLE_RATE * 2 * CHUNK_SIZE_MS) / 1000 // 16-bit = 2 bytes per sample
    }
    
    // Audio recording components
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    
    // Audio queue for sequential playback
    private val audioQueue = mutableListOf<ByteArray>()
    private val queueMutex = kotlinx.coroutines.sync.Mutex()
    
    // State flows
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()
    
    private val _audioChunks = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val audioChunks: SharedFlow<ByteArray> = _audioChunks.asSharedFlow()
    
    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()
    
    // Audio session management
    private var audioManager: AudioManager? = null
    private var originalAudioMode: Int = AudioManager.MODE_NORMAL
    
    init {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    /**
     * Initialize audio components
     */
    fun initializeAudio(): Boolean {
        return try {
            val minBufferSizeIn = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT
            )
            val minBufferSizeOut = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT
            )
            
            if (minBufferSizeIn == AudioRecord.ERROR || minBufferSizeIn == AudioRecord.ERROR_BAD_VALUE ||
                minBufferSizeOut == AudioTrack.ERROR || minBufferSizeOut == AudioTrack.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size for audio recording/playback")
                return false
            }
            
            val bufferSizeIn = maxOf(minBufferSizeIn * BUFFER_SIZE_FACTOR, CHUNK_SIZE_BYTES)
            val bufferSizeOut = maxOf(minBufferSizeOut * BUFFER_SIZE_FACTOR, CHUNK_SIZE_BYTES)
            
            // Initialize AudioRecord for capturing
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSizeIn
            )
            
            // Initialize AudioTrack for playback
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AUDIO_FORMAT)
                .setChannelMask(CHANNEL_CONFIG_OUT)
                .build()
            
            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSizeOut,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            
            Log.i(TAG, "Audio components initialized successfully")
            Log.i(TAG, "Recording buffer size: $bufferSizeIn, Playback buffer size: $bufferSizeOut")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio components", e)
            _recordingError.value = "Audio initialization failed: ${e.message}"
            false
        }
    }
    
    /**
     * Start recording audio and streaming chunks
     */
    fun startRecording(): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Recording already in progress")
            return true
        }
        
        return try {
            val record = audioRecord ?: run {
                Log.e(TAG, "AudioRecord not initialized")
                return false
            }
            
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not properly initialized")
                return false
            }
            
            // Configure audio session for voice communication
            configureAudioSession()
            
            record.startRecording()
            _isRecording.value = true
            _recordingError.value = null
            
            recordingJob = CoroutineScope(Dispatchers.Default).launch {
                captureAudioChunks(record)
            }
            
            Log.i(TAG, "Audio recording started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recordingError.value = "Recording start failed: ${e.message}"
            false
        }
    }
    
    /**
     * Stop recording audio
     */
    fun stopRecording() {
        try {
            recordingJob?.cancel()
            recordingJob = null
            
            audioRecord?.stop()
            _isRecording.value = false
            
            // Restore original audio session
            restoreAudioSession()
            
            Log.i(TAG, "Audio recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    /**
     * Play audio data from AI response (queue-based to prevent crashes)
     */
    fun playAudio(audioData: ByteArray) {
        if (audioData.isEmpty()) {
            Log.w(TAG, "Received empty audio data")
            return
        }
        
        CoroutineScope(Dispatchers.Default).launch {
            queueMutex.lock()
            try {
                audioQueue.add(audioData)
                Log.d(TAG, "Queued audio chunk: ${audioData.size} bytes, queue size: ${audioQueue.size}")
                
                // Start playback if not already playing
                if (!_isPlaying.value) {
                    startQueuedPlayback()
                }
            } finally {
                queueMutex.unlock()
            }
        }
    }
    
    /**
     * Stop current audio playback and clear queue
     */
    fun stopPlayback() {
        try {
            playbackJob?.cancel()
            playbackJob = null
            
            // Clear the audio queue
            CoroutineScope(Dispatchers.Default).launch {
                queueMutex.withLock {
                    audioQueue.clear()
                    Log.d(TAG, "Audio queue cleared")
                }
            }
            
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.stop()
                }
            }
            
            _isPlaying.value = false
            Log.i(TAG, "Audio playback stopped and queue cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        }
    }
    
    /**
     * Configure audio session for voice communication
     */
    private fun configureAudioSession() {
        try {
            audioManager?.let { manager ->
                originalAudioMode = manager.mode
                manager.mode = AudioManager.MODE_IN_COMMUNICATION
                manager.isSpeakerphoneOn = false
                manager.isBluetoothScoOn = true
                
                Log.d(TAG, "Audio session configured for voice communication")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure audio session", e)
        }
    }
    
    /**
     * Restore original audio session settings
     */
    private fun restoreAudioSession() {
        try {
            audioManager?.let { manager ->
                manager.mode = originalAudioMode
                manager.isSpeakerphoneOn = false
                manager.isBluetoothScoOn = false
                
                Log.d(TAG, "Audio session restored")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to restore audio session", e)
        }
    }
    
    /**
     * Capture audio in chunks and emit via flow
     */
    private suspend fun captureAudioChunks(audioRecord: AudioRecord) {
        val buffer = ByteArray(CHUNK_SIZE_BYTES)
        val audioLevelBuffer = ShortArray(CHUNK_SIZE_BYTES / 2) // 16-bit samples
        
        while (_isRecording.value && currentCoroutineContext().isActive) {
            try {
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                
                if (bytesRead > 0) {
                    // Calculate audio level for visualization
                    val audioLevel = calculateAudioLevel(buffer, bytesRead)
                    _audioLevel.value = audioLevel
                    
                    // Emit audio chunk for streaming
                    val chunk = buffer.copyOf(bytesRead)
                    _audioChunks.tryEmit(chunk)
                    
                    // Small delay to prevent overwhelming the system
                    delay(10)
                } else {
                    Log.w(TAG, "AudioRecord read returned: $bytesRead")
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading audio data", e)
                _recordingError.value = "Audio capture error: ${e.message}"
                break
            }
        }
    }
    
    /**
     * Start queued audio playback (sequential processing to prevent crashes)
     */
    private fun startQueuedPlayback() {
        if (playbackJob?.isActive == true) {
            Log.d(TAG, "Playback job already active")
            return
        }
        
        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            _isPlaying.value = true
            
            try {
                val track = audioTrack ?: run {
                    Log.e(TAG, "AudioTrack not available for playback")
                    return@launch
                }
                
                if (track.state == AudioTrack.STATE_UNINITIALIZED) {
                    Log.e(TAG, "AudioTrack not properly initialized")
                    return@launch
                }
                
                // Start AudioTrack
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                    Log.d(TAG, "AudioTrack playback started")
                }
                
                // Process queued audio chunks sequentially
                while (true) {
                    val audioData = queueMutex.withLock {
                        if (audioQueue.isNotEmpty()) {
                            audioQueue.removeAt(0)
                        } else null
                    }
                    
                    if (audioData == null) {
                        Log.d(TAG, "Audio queue empty, stopping playback")
                        break
                    }
                    
                    Log.d(TAG, "Playing audio chunk: ${audioData.size} bytes")
                    playAudioChunkSafely(track, audioData)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in queued playback", e)
            } finally {
                _isPlaying.value = false
                audioTrack?.pause()
                Log.d(TAG, "Audio playback completed")
            }
        }
    }
    
    /**
     * Safely write audio data to AudioTrack
     */
    private suspend fun playAudioChunkSafely(track: AudioTrack, audioData: ByteArray) {
        try {
            var offset = 0
            while (offset < audioData.size && _isPlaying.value) {
                val remainingBytes = audioData.size - offset
                val chunkSize = minOf(CHUNK_SIZE_BYTES, remainingBytes)
                
                val bytesWritten = track.write(audioData, offset, chunkSize)
                
                if (bytesWritten < 0) {
                    Log.e(TAG, "AudioTrack write error: $bytesWritten")
                    break
                }
                
                if (bytesWritten == 0) {
                    Log.w(TAG, "AudioTrack write returned 0 bytes, retrying...")
                    delay(10)
                    continue
                }
                
                offset += bytesWritten
                
                // Small delay for smooth playback
                if (offset < audioData.size) {
                    delay(5)
                }
            }
            
            Log.d(TAG, "Successfully played audio chunk: $offset/${audioData.size} bytes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to AudioTrack", e)
        }
    }
    
    /**
     * Calculate audio level for visualization
     */
    private fun calculateAudioLevel(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        val shortBuffer = ShortArray(length / 2)
        
        // Convert byte array to short array
        for (i in shortBuffer.indices) {
            val byteIndex = i * 2
            if (byteIndex + 1 < length) {
                shortBuffer[i] = ((buffer[byteIndex + 1].toInt() and 0xFF) shl 8 or 
                                 (buffer[byteIndex].toInt() and 0xFF)).toShort()
            }
        }
        
        // Calculate RMS
        for (sample in shortBuffer) {
            sum += (sample * sample).toDouble()
        }
        
        val rms = kotlin.math.sqrt(sum / shortBuffer.size)
        
        // Normalize to 0.0 - 1.0 range
        return (rms / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }
    
    /**
     * Get current audio capabilities
     */
    fun getAudioCapabilities(): AudioCapabilities {
        val hasLowLatency = context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_AUDIO_LOW_LATENCY
        )
        val hasPro = context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_AUDIO_PRO
        )
        
        return AudioCapabilities(
            supportsLowLatency = hasLowLatency,
            supportsPro = hasPro,
            sampleRate = SAMPLE_RATE,
            channelCount = 1,
            bitDepth = 16
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            stopRecording()
            stopPlayback()
            
            // Wait a bit for async operations to complete
            Thread.sleep(100)
            
            audioRecord?.release()
            audioRecord = null
            
            audioTrack?.release()
            audioTrack = null
            
            // Clear any remaining queue items
            CoroutineScope(Dispatchers.Default).launch {
                queueMutex.withLock {
                    audioQueue.clear()
                }
            }
            
            Log.i(TAG, "Audio resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

/**
 * Audio capabilities information
 */
data class AudioCapabilities(
    val supportsLowLatency: Boolean,
    val supportsPro: Boolean,
    val sampleRate: Int,
    val channelCount: Int,
    val bitDepth: Int
)

/**
 * Audio chunk data with metadata
 */
data class AudioChunk(
    val data: ByteArray,
    val timestamp: Long,
    val sequenceNumber: Int,
    val audioLevel: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioChunk

        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (audioLevel != other.audioLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + audioLevel.hashCode()
        return result
    }
}

/**
 * Audio session state
 */
enum class AudioSessionState {
    IDLE,
    INITIALIZING,
    RECORDING,
    PLAYING,
    BOTH,
    ERROR
}