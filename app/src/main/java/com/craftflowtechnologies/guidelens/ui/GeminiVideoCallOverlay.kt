package com.craftflowtechnologies.guidelens.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.media.MediaMetadataRetriever
import android.util.Log
import android.media.MediaRecorder
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState

/**
 * Next-generation Gemini Live video chatbot overlay with real-time AI streaming
 * Features:
 * - Gemini Live API integration for real-time video/audio processing
 * - Native audio generation with emotion-aware dialogue
 * - Session management with context compression
 * - Modern glassmorphism UI design
 * - Advanced voice activity detection
 * - Real-time video frame analysis
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiVideoCallOverlay(
    onClose: () -> Unit,
    selectedAgent: Agent,
    onSendMessage: (String) -> Unit,
    onStartLiveSession: () -> Unit,
    onStopLiveSession: () -> Unit,
    liveSessionManager: com.craftflowtechnologies.guidelens.api.GeminiLiveSessionManager,
    isLiveSessionActive: Boolean = false,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    voiceActivityLevel: Float = 0f,
    sessionDuration: Int = 0,
    modifier: Modifier = Modifier
) {
    // Enhanced state management for Gemini Live
    var isVideoEnabled by rememberSaveable { mutableStateOf(true) }
    var isMicEnabled by rememberSaveable { mutableStateOf(true) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var showControls by rememberSaveable { mutableStateOf(true) }
    var currentAnalysisMode by rememberSaveable { mutableStateOf(AnalysisMode.REAL_TIME) }
    var videoQuality by rememberSaveable { mutableStateOf(VideoQuality.HD) }
    var audioQuality by rememberSaveable { mutableStateOf(AudioQuality.HIGH) }

    // Live session state from the manager
    val sessionState by liveSessionManager.sessionState.collectAsState()
    val connectionQuality by remember { mutableStateOf(ConnectionQuality.EXCELLENT) }
    val lastResponseTime by liveSessionManager.responseTime.collectAsState()
    val aiInsights by liveSessionManager.aiInsights.collectAsState()
    val detectedObjects by liveSessionManager.detectedObjects.collectAsState()
    val emotionalContext by liveSessionManager.emotionalContext.collectAsState()
    val isProcessingFrame by liveSessionManager.isProcessing.collectAsState()
    val frameProcessingQueue by liveSessionManager.processingQueue.collectAsState()

    // Camera and recording enhanced state
    var currentCameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_FRONT_CAMERA) }
    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var recordingStartTime by remember { mutableStateOf(0L) }
    var recordingDuration by remember { mutableStateOf(0) }

    // Audio visualization
    var audioWaveform by remember { mutableStateOf(floatArrayOf()) }
    var currentAudioLevel by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }

    // Enhanced permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Auto-hide controls with modern timing
//    LaunchedEffect(showControls) {
//        if (showControls) {
//            delay(7000) // Extended timing for better UX
//            if (!isLiveSessionActive && !isRecording) {
//                showControls = false
//            }
//        }
//    }

    // Initialize camera with enhanced capabilities
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()

            cameraController = LifecycleCameraController(context).apply {
                setEnabledUseCases(
                    CameraController.VIDEO_CAPTURE or
                            CameraController.IMAGE_CAPTURE or
                            CameraController.IMAGE_ANALYSIS
                )
                cameraSelector = currentCameraSelector
                bindToLifecycle(lifecycleOwner)
            }
        }
    }

    // Enhanced session duration tracking
    LaunchedEffect(isLiveSessionActive) {
        if (isLiveSessionActive) {
            recordingStartTime = System.currentTimeMillis()
            while (isLiveSessionActive) {
                recordingDuration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                delay(1000)
            }
        } else {
            recordingDuration = 0
        }
    }

    // Real-time audio level simulation for visualization
    LaunchedEffect(isListening || isSpeaking) {
        while (isListening || isSpeaking) {
            currentAudioLevel = voiceActivityLevel + (Math.random().toFloat() * 0.3f - 0.15f)
            currentAudioLevel = currentAudioLevel.coerceIn(0f, 1f)
            delay(50) // 20 FPS for smooth animation
        }
    }

    // Enhanced functions
    fun startLiveSession() {
        if (!isLiveSessionActive) {
            coroutineScope.launch {
                onSendMessage("🚀 Starting Gemini Live session with ${selectedAgent.name}...")
                val success = liveSessionManager.startLiveSession(selectedAgent.id)
                if (success) {
                    onStartLiveSession()
                } else {
                    onSendMessage("❌ Failed to start Gemini Live session. Please try again.")
                }
            }
        }
    }

    fun stopLiveSession() {
        if (isLiveSessionActive) {
            liveSessionManager.stopLiveSession()
            onStopLiveSession()
        }
    }

    fun switchCamera() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        cameraController?.cameraSelector = currentCameraSelector
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun captureAnalysisFrame() {
        if (!isProcessingFrame) {
            // Use the live session manager to capture and analyze frames
            liveSessionManager.captureAndAnalyzeFrame(imageCapture)
        }
    }

    BackHandler { onClose() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF0A0A0A),
                        Color.Black
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Enhanced video feed with modern overlay effects
        if (hasCameraPermission && isVideoEnabled) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        controller = cameraController
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isLiveSessionActive) {
                            Modifier.clip(RoundedCornerShape(16.dp))
                        } else Modifier
                    ),
                update = { previewView ->
                    cameraController?.let { controller ->
                        previewView.controller = controller
                    }
                }
            )

            // Modern gradient overlay for better UI visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        } else {
            // Enhanced placeholder with modern design
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E),
                                Color(0xFF16213E),
                                Color(0xFF0F0F23)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        selectedAgent.primaryColor.copy(alpha = 0.3f),
                                        selectedAgent.secondaryColor.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = "Camera Off",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = if (!hasCameraPermission) "Camera Access Required" else "Camera Paused",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (!hasCameraPermission)
                            "Enable camera permission to start video analysis"
                        else
                            "Tap the video button to resume",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }

        // Enhanced AI Insights Panel with glassmorphism
        AnimatedVisibility(
            visible = (aiInsights.isNotEmpty() || detectedObjects.isNotEmpty()) && showControls,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            GeminiLiveInsightsPanel(
                insights = aiInsights,
                detectedObjects = detectedObjects,
                emotionalContext = emotionalContext,
                processingQueue = frameProcessingQueue,
                responseTime = lastResponseTime,
                selectedAgent = selectedAgent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Enhanced session status indicator
        if (isLiveSessionActive) {
            ModernSessionIndicator(
                duration = recordingDuration,
                quality = connectionQuality,
                audioLevel = currentAudioLevel,
                isListening = isListening,
                isSpeaking = isSpeaking,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(20.dp)
            )
        }

        // Enhanced agent status with modern design
        AnimatedVisibility(
            visible = showControls,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            ModernAgentCard(
                agent = selectedAgent,
                sessionState = sessionState,
                isListening = isListening,
                isSpeaking = isSpeaking,
                analysisMode = currentAnalysisMode,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Enhanced analysis mode selector with modern design
        AnimatedVisibility(
            visible = showControls && !isLiveSessionActive,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            ModernAnalysisModeSelector(
                currentMode = currentAnalysisMode,
                onModeChange = { mode ->
                    currentAnalysisMode = mode
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier.padding(16.dp)
            )
        }

        // Audio visualization overlay when listening/speaking
        if (isListening || isSpeaking) {
            ModernAudioVisualizer(
                audioLevel = currentAudioLevel,
                isListening = isListening,
                isSpeaking = isSpeaking,
                selectedAgent = selectedAgent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp)
            )
        }

        // Enhanced video controls with modern glassmorphism design
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GeminiLiveControls(
                isVideoEnabled = isVideoEnabled,
                isMicEnabled = isMicEnabled,
                isLiveSessionActive = isLiveSessionActive,
                sessionState = sessionState,
                isListening = isListening,
                isSpeaking = isSpeaking,
                sessionDuration = recordingDuration,
                isProcessing = isProcessingFrame,
                videoQuality = videoQuality,
                audioQuality = audioQuality,
                onVideoToggle = {
                    isVideoEnabled = !isVideoEnabled
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onMicToggle = {
                    isMicEnabled = !isMicEnabled
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onSessionToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isLiveSessionActive) {
                        stopLiveSession()
                    } else {
                        startLiveSession()
                    }
                },
                onSwitchCamera = { switchCamera() },
                onClose = onClose,
                onCaptureFrame = { captureAnalysisFrame() },
                onQualitySettings = {
                    // TODO: Implement quality settings dialog
                },
                selectedAgent = selectedAgent,
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
}

/**
 * Modern Gemini Live controls with glassmorphism design
 */
@Composable
private fun GeminiLiveControls(
    isVideoEnabled: Boolean,
    isMicEnabled: Boolean,
    isLiveSessionActive: Boolean,
    sessionState: SessionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    sessionDuration: Int,
    isProcessing: Boolean,
    videoQuality: VideoQuality,
    audioQuality: AudioQuality,
    onVideoToggle: () -> Unit,
    onMicToggle: () -> Unit,
    onSessionToggle: () -> Unit,
    onClose: () -> Unit,
    onCaptureFrame: () -> Unit,
    onSwitchCamera: () -> Unit,
    onQualitySettings: () -> Unit,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shadowElevation = 16.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frame capture
                ModernControlButton(
                    icon = Icons.Rounded.CameraAlt,
                    isActive = false,
                    color = Color(0xFF4CAF50),
                    onClick = onCaptureFrame,
                    label = "Capture",
                    isEnabled = !isProcessing
                )

                // Microphone
                ModernControlButton(
                    icon = if (isMicEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    isActive = isMicEnabled && (isListening || isSpeaking),
                    color = selectedAgent.primaryColor,
                    onClick = onMicToggle,
                    label = if (isMicEnabled) "Mic" else "Muted"
                )

                // Live session (main button)
                ModernSessionButton(
                    isActive = isLiveSessionActive,
                    sessionState = sessionState,
                    duration = sessionDuration,
                    onClick = onSessionToggle,
                    selectedAgent = selectedAgent
                )

                // Video toggle
                ModernControlButton(
                    icon = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    isActive = isVideoEnabled,
                    color = Color(0xFF2196F3),
                    onClick = onVideoToggle,
                    label = if (isVideoEnabled) "Video" else "Off"
                )

                // Camera switch
                ModernControlButton(
                    icon = Icons.Filled.FlipCameraAndroid,
                    isActive = false,
                    color = Color(0xFFFF9800),
                    onClick = onSwitchCamera,
                    label = "Flip",
                    isEnabled = isVideoEnabled
                )
            }

            // Secondary controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    onClick = onQualitySettings
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = "Quality",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = videoQuality.label,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // End session
                ModernControlButton(
                    icon = Icons.Rounded.CallEnd,
                    isActive = false,
                    color = Color(0xFFE53E3E),
                    onClick = onClose,
                    label = "End",
                    size = 48.dp
                )
            }
        }
    }
}

@Composable
private fun ModernControlButton(
    icon: ImageVector,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    size: Dp = 52.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Color.White.copy(alpha = 0.1f)
            isActive -> color
            else -> Color.White.copy(alpha = 0.15f)
        },
        animationSpec = tween(300)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = (if (isEnabled) onClick else { }) as () -> Unit,
            modifier = modifier
                .size(size)
                .scale(scale),
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = if (isActive && isEnabled) 8.dp else 2.dp,
            border = if (isActive) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = when {
                        !isEnabled -> Color.White.copy(alpha = 0.3f)
                        isActive -> Color.White
                        else -> color
                    },
                    modifier = Modifier.size((size.value * 0.4f).dp)
                )
            }
        }

        Text(
            text = label,
            color = if (isEnabled) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ModernSessionButton(
    isActive: Boolean,
    sessionState: SessionState,
    duration: Int,
    onClick: () -> Unit,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isActive) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(80.dp)
        ) {
            // Glow effect for active session
            if (isActive) {
                Canvas(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(pulseScale)
                ) {
                    drawCircle(
                        color = selectedAgent.primaryColor.copy(alpha = glowAlpha),
                        radius = size.minDimension / 2,
                        center = center
                    )
                }
            }
//            val Orange = Color(0xFFFF9800) // material orange 500
            // Main button
            Surface(
                onClick = onClick,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = when (sessionState) {
                    SessionState.CONNECTING -> selectedAgent.primaryColor.copy(alpha = 0.7f)
                    SessionState.CONNECTED -> selectedAgent.primaryColor
                    SessionState.DISCONNECTING -> Color(0xFFFF9800)
                    SessionState.DISCONNECTED -> Color.White.copy(alpha = 0.15f)
                },
                shadowElevation = 16.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = if (isActive) Color.White.copy(alpha = 0.3f) else Color.Transparent
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (sessionState) {
                        SessionState.CONNECTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                        SessionState.DISCONNECTING -> {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stopping",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = if (isActive) Icons.Rounded.VideoCall else Icons.Rounded.PlayArrow,
                                contentDescription = if (isActive) "End Live Session" else "Start Live Session",
                                tint = if (isActive) Color.White else selectedAgent.primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = when (sessionState) {
                SessionState.CONNECTING -> "Connecting..."
                SessionState.CONNECTED -> "Live ${formatDuration(duration)}"
                SessionState.DISCONNECTING -> "Ending..."
                SessionState.DISCONNECTED -> "Start Live"
            },
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GeminiLiveInsightsPanel(
    insights: String,
    detectedObjects: List<DetectedObject>,
    emotionalContext: EmotionalContext,
    processingQueue: Int,
    responseTime: Long,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.8f),
        shadowElevation = 12.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with live indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = "Live AI Analysis",
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(24.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini Live Analysis",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Processing queue indicator
                        if (processingQueue > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF9800).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Processing $processingQueue",
                                    color = Color(0xFFFF9800),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Response time
                        if (responseTime > 0) {
                            val responseMs = System.currentTimeMillis() - responseTime
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    responseMs < 500 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    responseMs < 1000 -> Color.Yellow.copy(alpha = 0.2f)
                                    else -> Color.Red.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = "${responseMs}ms",
                                    color = when {
                                        responseMs < 500 -> Color(0xFF4CAF50)
                                        responseMs < 1000 -> Color.Yellow
                                        else -> Color.Red
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Emotional context indicator
                EmotionalContextIndicator(
                    context = emotionalContext,
                    selectedAgent = selectedAgent
                )
            }

            // Detected objects with enhanced visualization
            if (detectedObjects.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(detectedObjects) { obj ->
                        DetectedObjectChip(
                            detectedObject = obj,
                            selectedAgent = selectedAgent
                        )
                    }
                }
            }

            // AI insights with enhanced formatting
            if (insights.isNotEmpty()) {
                Text(
                    text = insights,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun DetectedObjectChip(
    detectedObject: DetectedObject,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = selectedAgent.primaryColor.copy(alpha = 0.2f),
        border = BorderStroke(
            width = 1.dp,
            color = selectedAgent.primaryColor.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = detectedObject.name,
                color = selectedAgent.primaryColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    detectedObject.confidence > 0.8f -> Color(0xFF4CAF50)
                    detectedObject.confidence > 0.6f -> Color.Yellow
                    else -> Color.Red
                }.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "${(detectedObject.confidence * 100).toInt()}%",
                    color = when {
                        detectedObject.confidence > 0.8f -> Color(0xFF4CAF50)
                        detectedObject.confidence > 0.6f -> Color.Yellow
                        else -> Color.Red
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EmotionalContextIndicator(
    context: EmotionalContext,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = context.color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, context.color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = context.icon,
                contentDescription = context.name,
                tint = context.color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = context.displayName,
                color = context.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModernAgentCard(
    agent: Agent,
    sessionState: SessionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    analysisMode: AnalysisMode,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enhanced agent avatar with status ring
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor, agent.secondaryColor)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = agent.name.take(1),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status indicator
                if (sessionState == SessionState.CONNECTED) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .background(
                                color = when {
                                    isSpeaking -> Color(0xFF4CAF50)
                                    isListening -> Color(0xFF2196F3)
                                    else -> Color.Gray
                                },
                                shape = CircleShape
                            )
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }

            Column {
                Text(
                    text = agent.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = when {
                        sessionState == SessionState.CONNECTING -> "Connecting to Live API..."
                        sessionState == SessionState.DISCONNECTING -> "Ending session..."
                        isSpeaking -> "Speaking with native audio"
                        isListening -> "Listening & analyzing..."
                        sessionState == SessionState.CONNECTED -> "Live session active"
                        else -> analysisMode.displayName
                    },
                    color = when (sessionState) {
                        SessionState.CONNECTED -> agent.primaryColor
                        SessionState.CONNECTING -> Color.Yellow
                        SessionState.DISCONNECTING -> Color(0xFFFF9800)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModernAnalysisModeSelector(
    currentMode: AnalysisMode,
    onModeChange: (AnalysisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Analysis Mode",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        AnalysisMode.values().forEach { mode ->
            val isSelected = currentMode == mode

            Surface(
                onClick = { onModeChange(mode) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = mode.displayName,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )

                    Column {
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )

                        Text(
                            text = mode.description,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSessionIndicator(
    duration: Int,
    quality: ConnectionQuality,
    audioLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.8f),
        border = BorderStroke(
            width = 1.dp,
            color = quality.color.copy(alpha = pulseAlpha)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = quality.color.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )

            Column {
                Text(
                    text = "LIVE ${formatDuration(duration)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${quality.label} • ${if (isListening) "Listening" else if (isSpeaking) "Speaking" else "Ready"}",
                    color = quality.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModernAudioVisualizer(
    audioLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 3

            // Outer ring
            drawCircle(
                color = selectedAgent.primaryColor.copy(alpha = 0.3f),
                radius = radius + 20.dp.toPx(),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Audio level visualization
            for (i in 0 until 12) {
                val angle = (i * 30f + rotation) * Math.PI / 180
                val startRadius = radius
                val endRadius = radius + (audioLevel * 30.dp.toPx())

                val startX = center.x + (startRadius * Math.cos(angle)).toFloat()
                val startY = center.y + (startRadius * Math.sin(angle)).toFloat()
                val endX = center.x + (endRadius * Math.cos(angle)).toFloat()
                val endY = center.y + (endRadius * Math.sin(angle)).toFloat()

                drawLine(
                    color = if (isListening) Color(0xFF2196F3) else selectedAgent.primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center content
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Hearing else Icons.Rounded.RecordVoiceOver,
                    contentDescription = if (isListening) "Listening" else "Speaking",
                    tint = if (isListening) Color(0xFF2196F3) else selectedAgent.primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// Using SessionState from GeminiLiveSessionManager
// Type aliases for backward compatibility
typealias SessionState = com.craftflowtechnologies.guidelens.api.SessionState
typealias DetectedObject = com.craftflowtechnologies.guidelens.api.DetectedObject
typealias EmotionalContext = com.craftflowtechnologies.guidelens.api.EmotionalContext

enum class ConnectionQuality(val label: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF4CAF50)),
    GOOD("Good", Color(0xFF8BC34A)),
    FAIR("Fair", Color(0xFFFF9800)),
    POOR("Poor", Color(0xFFE53E3E))
}

enum class VideoQuality(val label: String) {
    HD("HD"),
    FHD("FHD"),
    UHD("4K")
}

enum class AudioQuality(val label: String) {
    STANDARD("Standard"),
    HIGH("High"),
    LOSSLESS("Lossless")
}



// Enhanced analysis modes
enum class AnalysisMode(
    val displayName: String,
    val icon: ImageVector,
    val description: String
) {
    REAL_TIME("Live Feedback", Icons.Rounded.Visibility, "Continuous real-time analysis"),
    STEP_BY_STEP("Step Guide", Icons.Rounded.LinearScale, "Detailed step-by-step guidance"),
    SAFETY_FOCUS("Safety Check", Icons.Rounded.Security, "Focus on safety and best practices"),
    TECHNIQUE("Technique", Icons.Rounded.Psychology, "Analyze technique and form"),
    TROUBLESHOOT("Debug Mode", Icons.Rounded.BugReport, "Identify and solve problems"),
    LEARNING("Learning", Icons.Rounded.School, "Educational explanations"),
    CREATIVE("Creative", Icons.Rounded.Brush, "Creative and artistic guidance")
}

// Helper functions
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

private fun bindCameraPreview(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onImageCaptureReady: (ImageCapture) -> Unit
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()

    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        onImageCaptureReady(imageCapture)
    } catch (exc: Exception) {
        Log.e("GeminiLiveOverlay", "Camera binding failed", exc)
    }
}

private fun captureFrameForAnalysis(
    context: Context,
    imageCapture: ImageCapture?,
    onAnalysis: (File) -> Unit
) {
    imageCapture?.let { capture ->
        val photoFile = File(
            context.externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e("GeminiLiveOverlay", "Image capture failed", exception)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onAnalysis(photoFile)
                }
            }
        )
    }
}

private fun buildAnalysisPrompt(mode: AnalysisMode): String {
    return when (mode) {
        AnalysisMode.REAL_TIME -> "Analyze this video frame in real-time and provide immediate feedback on what you see. Focus on the current action and any guidance needed."

        AnalysisMode.STEP_BY_STEP -> "Break down this step in detail. Explain exactly what should be happening, what comes next, and provide specific step-by-step guidance."

        AnalysisMode.SAFETY_FOCUS -> "Focus on safety in this frame. Identify potential safety concerns, proper safety equipment usage, and safety best practices."

        AnalysisMode.TECHNIQUE -> "Analyze the technique shown. Provide detailed feedback on form, posture, positioning, and suggest improvements."

        AnalysisMode.TROUBLESHOOT -> "Act as a troubleshooting expert. Identify any problems, mistakes, or issues and provide specific solutions."

        AnalysisMode.LEARNING -> "Provide educational explanations about what's happening in this frame. Focus on learning and understanding."

        AnalysisMode.CREATIVE -> "Provide creative and artistic guidance based on what you see. Focus on aesthetics, composition, and creative expression."
    }
}