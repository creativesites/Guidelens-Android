package com.craftflowtechnologies.guidelens.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

data class VideoCaptureState(
    val isCapturing: Boolean = false,
    val frameRate: Int = 15,
    val resolution: String = "720p",
    val cameraFacing: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
    val isAnalyzing: Boolean = false,
    val lastFrameTimestamp: Long = 0L,
    val errorMessage: String? = null
)

class VideoCaptureManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    
    // State management
    private val _state = MutableStateFlow(VideoCaptureState())
    val state: StateFlow<VideoCaptureState> = _state.asStateFlow()
    
    // Frame streams
    private val _frameData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 10)
    val frameData: SharedFlow<ByteArray> = _frameData.asSharedFlow()
    
    private val _frameAnalysis = MutableSharedFlow<FrameAnalysis>(extraBufferCapacity = 10)
    val frameAnalysis: SharedFlow<FrameAnalysis> = _frameAnalysis.asSharedFlow()
    
    data class FrameAnalysis(
        val timestamp: Long,
        val brightness: Float,
        val sharpness: Float,
        val motion: Float,
        val objects: List<DetectedObject> = emptyList()
    )
    
    data class DetectedObject(
        val label: String,
        val confidence: Float,
        val boundingBox: Rect
    )
    
    suspend fun startVideoCapture(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        enableFrameAnalysis: Boolean = true
    ): Boolean {
        return try {
            Log.d("VideoCaptureManager", "Starting video capture")
            
            // Get camera provider
            cameraProvider = ProcessCameraProvider.getInstance(context).get()
            
            // Setup preview
            preview = Preview.Builder()
                .setTargetResolution(android.util.Size(1280, 720)) // 720p
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Setup image analysis for frame processing
            if (enableFrameAnalysis) {
                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(640, 480)) // Lower resolution for analysis
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                            processFrame(imageProxy)
                        }
                    }
            }
            
            // Select camera
            val cameraSelector = _state.value.cameraFacing
            
            // Unbind use cases before rebinding
            cameraProvider?.unbindAll()
            
            // Bind use cases to camera
            camera = if (enableFrameAnalysis && imageAnalysis != null) {
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } else {
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            }
            
            _state.value = _state.value.copy(
                isCapturing = true,
                isAnalyzing = enableFrameAnalysis,
                errorMessage = null
            )
            
            Log.d("VideoCaptureManager", "Video capture started successfully")
            true
            
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Failed to start video capture", e)
            _state.value = _state.value.copy(
                errorMessage = "Failed to start video capture: ${e.message}"
            )
            false
        }
    }
    
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // Convert ImageProxy to ByteArray for transmission
            val frameData = imageProxyToByteArray(imageProxy)
            frameData?.let { data ->
                _frameData.tryEmit(data)
            }
            
            // Perform frame analysis
            if (_state.value.isAnalyzing) {
                val analysis = analyzeFrame(imageProxy, currentTime)
                _frameAnalysis.tryEmit(analysis)
            }
            
            _state.value = _state.value.copy(
                lastFrameTimestamp = currentTime
            )
            
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Error processing frame", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun imageProxyToByteArray(imageProxy: ImageProxy): ByteArray? {
        return try {
            val buffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Error converting ImageProxy to ByteArray", e)
            null
        }
    }
    
    private fun analyzeFrame(imageProxy: ImageProxy, timestamp: Long): FrameAnalysis {
        // Simple frame analysis - can be enhanced with ML models
        val brightness = calculateBrightness(imageProxy)
        val sharpness = calculateSharpness(imageProxy)
        val motion = calculateMotion(imageProxy, timestamp)
        
        return FrameAnalysis(
            timestamp = timestamp,
            brightness = brightness,
            sharpness = sharpness,
            motion = motion,
            objects = emptyList() // Would be populated by object detection model
        )
    }
    
    private fun calculateBrightness(imageProxy: ImageProxy): Float {
        // Simple brightness calculation from Y channel
        try {
            val yBuffer = imageProxy.planes[0].buffer
            val ySize = yBuffer.remaining()
            val yArray = ByteArray(ySize)
            yBuffer.get(yArray)
            
            var sum = 0L
            yArray.forEach { byte ->
                sum += (byte.toInt() and 0xFF)
            }
            
            return (sum.toFloat() / ySize) / 255f
            
        } catch (e: Exception) {
            Log.w("VideoCaptureManager", "Failed to calculate brightness", e)
            return 0.5f
        }
    }
    
    private fun calculateSharpness(imageProxy: ImageProxy): Float {
        // Simple sharpness calculation using Laplacian variance
        try {
            val yBuffer = imageProxy.planes[0].buffer
            val width = imageProxy.width
            val height = imageProxy.height
            val yArray = ByteArray(yBuffer.remaining())
            yBuffer.get(yArray)
            
            // Simplified Laplacian calculation on a subset
            var variance = 0.0
            val step = 10 // Sample every 10th pixel for performance
            
            for (y in step until height - step step step) {
                for (x in step until width - step step step) {
                    val idx = y * width + x
                    if (idx + width < yArray.size && idx - width >= 0) {
                        val center = yArray[idx].toInt() and 0xFF
                        val top = yArray[idx - width].toInt() and 0xFF
                        val bottom = yArray[idx + width].toInt() and 0xFF
                        val left = yArray[idx - 1].toInt() and 0xFF
                        val right = yArray[idx + 1].toInt() and 0xFF
                        
                        val laplacian = Math.abs(4 * center - top - bottom - left - right)
                        variance += laplacian * laplacian
                    }
                }
            }
            
            return (variance / (width * height / (step * step))).toFloat().coerceIn(0f, 1f)
            
        } catch (e: Exception) {
            Log.w("VideoCaptureManager", "Failed to calculate sharpness", e)
            return 0.5f
        }
    }
    
    private var previousFrameTime: Long = 0
    private var previousBrightness: Float = 0f
    
    private fun calculateMotion(imageProxy: ImageProxy, timestamp: Long): Float {
        val currentBrightness = calculateBrightness(imageProxy)
        
        val motion = if (previousFrameTime > 0) {
            val timeDiff = timestamp - previousFrameTime
            val brightnessDiff = Math.abs(currentBrightness - previousBrightness)
            
            if (timeDiff > 0) {
                (brightnessDiff / (timeDiff / 1000f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        } else {
            0f
        }
        
        previousFrameTime = timestamp
        previousBrightness = currentBrightness
        
        return motion
    }
    
    fun switchCamera() {
        try {
            val newCameraFacing = if (_state.value.cameraFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            
            _state.value = _state.value.copy(cameraFacing = newCameraFacing)
            
            // Re-bind camera with new selector
            cameraProvider?.let { provider ->
                provider.unbindAll()
                
                val useCases = mutableListOf<UseCase>()
                preview?.let { useCases.add(it) }
                imageAnalysis?.let { useCases.add(it) }
                
                camera = provider.bindToLifecycle(
                    context as LifecycleOwner,
                    newCameraFacing,
                    *useCases.toTypedArray()
                )
            }
            
            Log.d("VideoCaptureManager", "Switched to ${if (newCameraFacing == CameraSelector.DEFAULT_FRONT_CAMERA) "front" else "back"} camera")
            
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Failed to switch camera", e)
            _state.value = _state.value.copy(
                errorMessage = "Failed to switch camera: ${e.message}"
            )
        }
    }
    
    fun setFrameRate(frameRate: Int) {
        _state.value = _state.value.copy(frameRate = frameRate)
        
        // Update image analysis frame rate if applicable
        imageAnalysis?.clearAnalyzer()
        imageAnalysis?.setAnalyzer(cameraExecutor) { imageProxy ->
            // Add frame rate limiting
            val now = System.currentTimeMillis()
            val frameInterval = 1000L / frameRate
            
            if (now - _state.value.lastFrameTimestamp >= frameInterval) {
                processFrame(imageProxy)
            } else {
                imageProxy.close()
            }
        }
    }
    
    fun enableFrameAnalysis(enable: Boolean) {
        _state.value = _state.value.copy(isAnalyzing = enable)
    }
    
    fun captureStillFrame(): Bitmap? {
        return try {
            // This would typically use ImageCapture use case
            // For now, return null as we'd need to restructure to support ImageCapture
            Log.d("VideoCaptureManager", "Still frame capture requested")
            null
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Failed to capture still frame", e)
            null
        }
    }
    
    fun stopVideoCapture() {
        try {
            Log.d("VideoCaptureManager", "Stopping video capture")
            
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalysis = null
            
            _state.value = _state.value.copy(
                isCapturing = false,
                isAnalyzing = false,
                lastFrameTimestamp = 0L
            )
            
            Log.d("VideoCaptureManager", "Video capture stopped")
            
        } catch (e: Exception) {
            Log.e("VideoCaptureManager", "Error stopping video capture", e)
            _state.value = _state.value.copy(
                errorMessage = "Error stopping video capture: ${e.message}"
            )
        }
    }
    
    fun cleanup() {
        stopVideoCapture()
        cameraExecutor.shutdown()
        scope.cancel()
    }
    
    // Utility functions
    fun isCapturing(): Boolean = _state.value.isCapturing
    fun getCurrentFrameRate(): Int = _state.value.frameRate
    fun getLastFrameTime(): Long = _state.value.lastFrameTimestamp
    fun hasError(): Boolean = _state.value.errorMessage != null
    fun getErrorMessage(): String? = _state.value.errorMessage
    
    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}