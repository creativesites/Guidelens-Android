package com.craftflowtechnologies.guidelens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaMetadataRetriever
import android.util.Half.abs
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

object MediaUtils {
    
    /**
     * Convert ImageProxy to JPEG byte array for Gemini API
     */
    fun imageProxyToJpeg(imageProxy: ImageProxy, quality: Int = 85): ByteArray? {
        return try {
            when (imageProxy.format) {
                ImageFormat.YUV_420_888 -> yuvToJpeg(imageProxy, quality)
                ImageFormat.JPEG -> {
                    // Direct JPEG format
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    bytes
                }
                else -> {
                    Log.w("MediaUtils", "Unsupported image format: ${imageProxy.format}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error converting ImageProxy to JPEG", e)
            null
        }
    }
    
    private fun yuvToJpeg(imageProxy: ImageProxy, quality: Int): ByteArray? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), quality, out)
            out.toByteArray()
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error converting YUV to JPEG", e)
            null
        }
    }
    
    /**
     * Resize image to optimal size for Gemini API (reduce bandwidth)
     */
    fun resizeImageForApi(originalBytes: ByteArray, maxWidth: Int = 1280, maxHeight: Int = 720): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
            val scaledBitmap = resizeBitmap(bitmap, maxWidth, maxHeight)
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            scaledBitmap.recycle()
            bitmap.recycle()
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e("MediaUtils", "Error resizing image", e)
            originalBytes // Return original if resize fails
        }
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val aspectRatio = width.toFloat() / height.toFloat()
        
        val (newWidth, newHeight) = when {
            width > height -> {
                if (width > maxWidth) {
                    Pair(maxWidth, (maxWidth / aspectRatio).toInt())
                } else {
                    Pair(width, height)
                }
            }
            height > width -> {
                if (height > maxHeight) {
                    Pair((maxHeight * aspectRatio).toInt(), maxHeight)
                } else {
                    Pair(width, height)
                }
            }
            else -> {
                val maxSize = minOf(maxWidth, maxHeight)
                if (width > maxSize) {
                    Pair(maxSize, maxSize)
                } else {
                    Pair(width, height)
                }
            }
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Convert audio PCM data to optimal format for Gemini API
     */
    fun optimizeAudioForApi(pcmData: ByteArray, sampleRate: Int = 16000): ByteArray {
        // For now, return as-is. Could add noise reduction, normalization, etc.
        return pcmData
    }
    
    /**
     * Calculate audio quality metrics
     */
    fun analyzeAudioQuality(pcmData: ByteArray): AudioQualityMetrics {
        val samples = pcmDataToShortArray(pcmData)
        
        // Calculate RMS (volume level)
        var sumSquares = 0.0
        samples.forEach { sample ->
            sumSquares += (sample * sample)
        }
        val rms = Math.sqrt(sumSquares / samples.size)
        val volume = (rms / Short.MAX_VALUE).coerceIn(0.0, 1.0)
        
        // Simple clipping detection
        val clippedSamples = samples.count { abs(it) > (Short.MAX_VALUE * 0.95) }
        val clippingRatio = clippedSamples.toFloat() / samples.size

        val silentSamples = samples.count { abs(it) < (Short.MAX_VALUE * 0.01) }
        val silenceRatio = silentSamples.toFloat() / samples.size
        
        return AudioQualityMetrics(
            volume = volume.toFloat(),
            clippingRatio = clippingRatio,
            silenceRatio = silenceRatio,
            isGoodQuality = volume > 0.05 && clippingRatio < 0.01 && silenceRatio < 0.8
        )
    }
    
    private fun pcmDataToShortArray(pcmData: ByteArray): ShortArray {
        val shortArray = ShortArray(pcmData.size / 2)
        val buffer = ByteBuffer.wrap(pcmData)
        buffer.asShortBuffer().get(shortArray)
        return shortArray
    }
    
    /**
     * Format duration for display
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60)) % 24
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Check if device has good network conditions for video streaming
     */
    fun isNetworkSuitableForVideo(context: Context): Boolean {
        // Implementation would check network speed, connection type, etc.
        // For now, return true
        return true
    }
    
    /**
     * Estimate bandwidth usage
     */
    fun estimateBandwidthUsage(
        audioEnabled: Boolean,
        videoEnabled: Boolean,
        videoQuality: VideoQuality = VideoQuality.MEDIUM
    ): BandwidthEstimate {
        val audioKbps = if (audioEnabled) 64 else 0 // 64 kbps for audio
        val videoKbps = when (videoQuality) {
            VideoQuality.LOW -> 500    // 500 kbps
            VideoQuality.MEDIUM -> 1000 // 1 Mbps
            VideoQuality.HIGH -> 2000   // 2 Mbps
        }
        
        val totalKbps = audioKbps + (if (videoEnabled) videoKbps else 0)
        
        return BandwidthEstimate(
            audioKbps = audioKbps,
            videoKbps = if (videoEnabled) videoKbps else 0,
            totalKbps = totalKbps,
            estimatedMBperHour = (totalKbps * 3600) / (8 * 1024) // Convert to MB/hour
        )
    }
}

data class AudioQualityMetrics(
    val volume: Float,
    val clippingRatio: Float,
    val silenceRatio: Float,
    val isGoodQuality: Boolean
)

enum class VideoQuality {
    LOW, MEDIUM, HIGH
}

data class BandwidthEstimate(
    val audioKbps: Int,
    val videoKbps: Int,
    val totalKbps: Int,
    val estimatedMBperHour: Int
)