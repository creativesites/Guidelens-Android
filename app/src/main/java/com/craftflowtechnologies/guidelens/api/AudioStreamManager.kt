package com.craftflowtechnologies.guidelens.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

class AudioStreamManager(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _audioData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 100)
    val audioData: SharedFlow<ByteArray> = _audioData.asSharedFlow()
    
    private val _recordingState = MutableStateFlow(false)
    val recordingState: StateFlow<Boolean> = _recordingState.asStateFlow()
    
    private val _playbackState = MutableStateFlow(false)
    val playbackState: StateFlow<Boolean> = _playbackState.asStateFlow()
    
    // Audio configuration
    companion object {
        const val SAMPLE_RATE = 16000 // 16kHz for good quality and efficiency
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_FACTOR = 2
    }
    
    private val bufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    }
    
    private val playbackBufferSize: Int by lazy {
        AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR
    }
    
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun startRecording(): Boolean {
        if (!hasAudioPermission()) {
            Log.e("AudioStream", "Audio permission not granted")
            return false
        }
        
        if (isRecording) {
            Log.w("AudioStream", "Already recording")
            return true
        }
        
        try {
            audioRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG_IN)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_IN,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }
            
            audioRecord?.startRecording()
            isRecording = true
            _recordingState.value = true
            
            // Start recording in background
            scope.launch {
                recordAudio()
            }
            
            Log.d("AudioStream", "Started recording")
            return true
            
        } catch (e: Exception) {
            Log.e("AudioStream", "Failed to start recording", e)
            return false
        }
    }
    
    private suspend fun recordAudio() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(bufferSize)
        
        while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            
            if (bytesRead > 0) {
                // Copy only the actual data read
                val audioChunk = buffer.copyOf(bytesRead)
                _audioData.tryEmit(audioChunk)
            }
            
            // Small delay to prevent overwhelming the stream
            delay(10)
        }
    }
    
    fun stopRecording() {
        if (!isRecording) {
            return
        }
        
        isRecording = false
        _recordingState.value = false
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d("AudioStream", "Stopped recording")
        } catch (e: Exception) {
            Log.e("AudioStream", "Error stopping recording", e)
        }
    }
    
    fun startPlayback(): Boolean {
        if (isPlaying) {
            Log.w("AudioStream", "Already playing")
            return true
        }
        
        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_CONFIG_OUT)
                            .build()
                    )
                    .setBufferSizeInBytes(playbackBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG_OUT,
                    AUDIO_FORMAT,
                    playbackBufferSize,
                    AudioTrack.MODE_STREAM
                )
            }
            
            audioTrack?.play()
            isPlaying = true
            _playbackState.value = true
            
            Log.d("AudioStream", "Started playback")
            return true
            
        } catch (e: Exception) {
            Log.e("AudioStream", "Failed to start playback", e)
            return false
        }
    }
    
    fun playAudio(audioData: ByteArray) {
        if (!isPlaying) {
            if (!startPlayback()) {
                return
            }
        }
        
        scope.launch {
            try {
                audioTrack?.write(audioData, 0, audioData.size)
            } catch (e: Exception) {
                Log.e("AudioStream", "Error playing audio", e)
            }
        }
    }
    
    fun stopPlayback() {
        if (!isPlaying) {
            return
        }
        
        isPlaying = false
        _playbackState.value = false
        
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            Log.d("AudioStream", "Stopped playback")
        } catch (e: Exception) {
            Log.e("AudioStream", "Error stopping playback", e)
        }
    }
    
    fun cleanup() {
        stopRecording()
        stopPlayback()
        scope.cancel()
    }
    
    // Utility functions for audio processing
    fun convertToMono(stereoData: ByteArray): ByteArray {
        val monoData = ByteArray(stereoData.size / 2)
        for (i in monoData.indices) {
            val leftSample = ByteBuffer.wrap(stereoData, i * 4, 2).short
            val rightSample = ByteBuffer.wrap(stereoData, i * 4 + 2, 2).short
            val monoSample = ((leftSample + rightSample) / 2).toShort()
            
            monoData[i * 2] = (monoSample.toInt() and 0xFF).toByte()
            monoData[i * 2 + 1] = ((monoSample.toInt() shr 8) and 0xFF).toByte()
        }
        return monoData
    }
    
    fun adjustVolume(audioData: ByteArray, volumeFactor: Float): ByteArray {
        val adjustedData = ByteArray(audioData.size)
        for (i in audioData.indices step 2) {
            val sample = ByteBuffer.wrap(audioData, i, 2).short
            val adjustedSample = (sample * volumeFactor).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            
            adjustedData[i] = (adjustedSample.toInt() and 0xFF).toByte()
            adjustedData[i + 1] = ((adjustedSample.toInt() shr 8) and 0xFF).toByte()
        }
        return adjustedData
    }
}