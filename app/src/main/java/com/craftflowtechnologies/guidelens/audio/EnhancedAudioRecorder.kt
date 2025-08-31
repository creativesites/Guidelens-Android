package com.craftflowtechnologies.guidelens.audio

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EnhancedAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Audio configuration for optimal Gemini Live API compatibility
    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz for speech recognition
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_MULTIPLIER = 4
    }
    
    private val bufferSize = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
    ) * BUFFER_SIZE_MULTIPLIER
    
    // Audio data streams
    private val _audioChunks = MutableSharedFlow<ByteArray>(extraBufferCapacity = 100)
    val audioChunks: SharedFlow<ByteArray> = _audioChunks.asSharedFlow()
    
    private val _recordingState = MutableStateFlow(AudioRecordingState())
    val recordingState: StateFlow<AudioRecordingState> = _recordingState.asStateFlow()
    
    private val audioBuffer = ByteArrayOutputStream()
    
    data class AudioRecordingState(
        val isRecording: Boolean = false,
        val volumeLevel: Float = 0f,
        val duration: Long = 0L,
        val quality: AudioQuality = AudioQuality.GOOD,
        val errorMessage: String? = null
    )
    
    enum class AudioQuality {
        EXCELLENT, GOOD, FAIR, POOR
    }
    
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w("EnhancedAudioRecorder", "Already recording")
            return true
        }
        
        return try {
            audioRecord = createAudioRecord()
            audioRecord?.startRecording()
            
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e("EnhancedAudioRecorder", "Failed to start recording")
                return false
            }
            
            isRecording = true
            audioBuffer.reset()
            
            _recordingState.value = _recordingState.value.copy(
                isRecording = true,
                errorMessage = null
            )
            
            // Start recording in background coroutine
            recordingJob = scope.launch {
                recordAudio()
            }
            
            Log.d("EnhancedAudioRecorder", "Audio recording started successfully")
            true
            
        } catch (e: Exception) {
            Log.e("EnhancedAudioRecorder", "Failed to start audio recording", e)
            _recordingState.value = _recordingState.value.copy(
                errorMessage = "Failed to start recording: ${e.message}"
            )
            false
        }
    }
    
    private suspend fun recordAudio() = withContext(Dispatchers.IO) {
        val buffer = ShortArray(bufferSize / 2) // 16-bit samples
        val byteBuffer = ByteArray(bufferSize)
        val startTime = System.currentTimeMillis()
        
        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                val samplesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                
                if (samplesRead > 0) {
                    // Convert shorts to bytes
                    val byteBuffer = ByteBuffer.allocate(samplesRead * 2).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.take(samplesRead).forEach { sample ->
                        byteBuffer.putShort(sample)
                    }
                    
                    val audioChunk = byteBuffer.array()
                    
                    // Calculate volume level for UI feedback
                    val volumeLevel = calculateVolumeLevel(buffer, samplesRead)
                    val quality = assessAudioQuality(volumeLevel, samplesRead)
                    
                    // Update state
                    val currentTime = System.currentTimeMillis()
                    _recordingState.value = _recordingState.value.copy(
                        volumeLevel = volumeLevel,
                        duration = currentTime - startTime,
                        quality = quality
                    )
                    
                    // Store audio data
                    audioBuffer.write(audioChunk)
                    
                    // Emit audio chunk for real-time processing
                    _audioChunks.tryEmit(audioChunk)
                }
                
                delay(10) // Small delay to prevent overwhelming the stream
                
            } catch (e: Exception) {
                Log.e("EnhancedAudioRecorder", "Error during recording", e)
                break
            }
        }
    }
    
    fun stopRecording(): ByteArray? {
        if (!isRecording) {
            return null
        }
        
        isRecording = false
        recordingJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            _recordingState.value = _recordingState.value.copy(
                isRecording = false,
                volumeLevel = 0f
            )
            
            val recordedAudio = audioBuffer.toByteArray()
            audioBuffer.reset()
            
            Log.d("EnhancedAudioRecorder", "Recording stopped. Captured ${recordedAudio.size} bytes")
            return recordedAudio
            
        } catch (e: Exception) {
            Log.e("EnhancedAudioRecorder", "Error stopping recording", e)
            _recordingState.value = _recordingState.value.copy(
                errorMessage = "Error stopping recording: ${e.message}"
            )
            return null
        }
    }
    
    private fun createAudioRecord(): AudioRecord {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        }
    }
    
    private fun calculateVolumeLevel(buffer: ShortArray, samplesRead: Int): Float {
        var sum = 0.0
        for (i in 0 until samplesRead) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        
        val rms = Math.sqrt(sum / samplesRead)
        val amplitude = rms / 32767.0 // Normalize to 0-1
        
        return amplitude.toFloat().coerceIn(0f, 1f)
    }
    
    private fun assessAudioQuality(volumeLevel: Float, samplesRead: Int): AudioQuality {
        return when {
            volumeLevel < 0.01f -> AudioQuality.POOR // Too quiet
            volumeLevel > 0.8f -> AudioQuality.POOR  // Too loud/clipping
            volumeLevel in 0.1f..0.6f -> AudioQuality.EXCELLENT
            volumeLevel in 0.05f..0.1f -> AudioQuality.GOOD
            else -> AudioQuality.FAIR
        }
    }
    
    fun pauseRecording() {
        if (isRecording) {
            isRecording = false
            recordingJob?.cancel()
            
            _recordingState.value = _recordingState.value.copy(
                isRecording = false
            )
        }
    }
    
    fun resumeRecording() {
        if (!_recordingState.value.isRecording && audioRecord != null) {
            isRecording = true
            recordingJob = scope.launch {
                recordAudio()
            }
            
            _recordingState.value = _recordingState.value.copy(
                isRecording = true
            )
        }
    }
    
    // Audio processing utilities
    fun applyNoiseReduction(audioData: ByteArray): ByteArray {
        // Simple noise gate implementation
        val threshold = 0.02f
        val buffer = ShortArray(audioData.size / 2)
        val byteBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in buffer.indices) {
            buffer[i] = byteBuffer.short
        }
        
        // Apply noise gate
        for (i in buffer.indices) {
            val normalized = buffer[i] / 32767.0f
            if (Math.abs(normalized) < threshold) {
                buffer[i] = 0
            }
        }
        
        // Convert back to bytes
        val outputBuffer = ByteBuffer.allocate(audioData.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.forEach { sample ->
            outputBuffer.putShort(sample)
        }
        
        return outputBuffer.array()
    }
    
    fun amplifyAudio(audioData: ByteArray, gainFactor: Float): ByteArray {
        val buffer = ShortArray(audioData.size / 2)
        val byteBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in buffer.indices) {
            buffer[i] = byteBuffer.short
        }
        
        // Apply gain
        for (i in buffer.indices) {
            val amplified = (buffer[i] * gainFactor).toInt()
            buffer[i] = amplified.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        // Convert back to bytes
        val outputBuffer = ByteBuffer.allocate(audioData.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.forEach { sample ->
            outputBuffer.putShort(sample)
        }
        
        return outputBuffer.array()
    }
    
    fun cleanup() {
        stopRecording()
        scope.cancel()
    }
}