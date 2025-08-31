package com.craftflowtechnologies.guidelens.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
// Remove the android.R import - this was causing the issue
// import android.R  // REMOVE THIS LINE
import com.craftflowtechnologies.guidelens.R // Add this import for your app's R class



@Composable
fun VideoCallOverlay(
    onClose: () -> Unit,
    selectedAgent: Agent,
    messages: List<ChatMessage>,
    onAgentClick: () -> Unit,
    onSendMessage: (String) -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    videoCallState: VideoCallState,
    onToggleVideo: () -> Unit,
    transcriptionState: TranscriptionState,
//    onToggleSession: () -> Unit,
    onVoiceRecord: () -> Unit,
    onFeatureClick: (Any) -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var isUserVideoMaximized by remember { mutableStateOf(true) } // Start with user video maximized
    var showChatOverlay by remember { mutableStateOf(false) }
    var showFeatureOverlay by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Handle back gesture
    BackHandler(enabled = showFeatureOverlay || isKeyboardVisible || showChatOverlay) {
        when {
            showFeatureOverlay -> showFeatureOverlay = false
            isKeyboardVisible -> {
                focusManager.clearFocus()
                keyboardController?.hide()
                isKeyboardVisible = false
            }
            showChatOverlay -> {
                showChatOverlay = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.guideLensColors.gradientStart,
                        MaterialTheme.guideLensColors.gradientEnd
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main video area (top 2/3 of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(16.dp)
            ) {
                // Primary video feed (user or agent based on maximization)
                MainVideoFeed(
                    selectedAgent = selectedAgent,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onAgentClick = onAgentClick,
                    videoCallState = videoCallState,
                    isUserVideoMaximized = isUserVideoMaximized,
                    onToggleMaximize = { isUserVideoMaximized = !isUserVideoMaximized },
                    onClose = onClose,
                    modifier = Modifier.fillMaxSize()
                )

                // Transcription overlay at bottom of video
                if (transcriptionState.currentText.isNotEmpty()) {
                    TranscriptionDisplayOther(
                        transcriptionState = transcriptionState,
                        selectedAgent = selectedAgent,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // Lower section (bottom 1/3 of screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bot video feed area (left side)
                    BotVideoFeed(
                        selectedAgent = selectedAgent,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        onShowFeatures = { showFeatureOverlay = true },
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    )

                    // Status/Activity panel (right side)
                    AgentSidebarOld(
                        selectedAgent = selectedAgent,
                        videoCallState = videoCallState,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isUserVideoMaximized = isUserVideoMaximized,
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    )
                }
            }

            // Bottom controls
            VideoCallBottomControls(
                userInput = userInput,
                onInputChange = { userInput = it },
                onSendMessage = {
                    if (userInput.isNotBlank()) {
                        onSendMessage(userInput)
                        userInput = ""
                        focusManager.clearFocus()
                        isKeyboardVisible = false
                    }
                },
                videoCallState = videoCallState,
                onToggleVideo = onToggleVideo,
                onVoiceRecord = onVoiceRecord,
                isListening = isListening,
                selectedAgent = selectedAgent,
                onClose = onClose,
//                onToggleSession = onToggleSession,
                onFocusChange = { focused ->
                    isKeyboardVisible = focused
                },
                onToggleChatOverlay = { showChatOverlay = !showChatOverlay }
            )
        }

        // Feature overlay
        UnifiedFeatureOverlay(
            selectedAgent = selectedAgent,
            isVideoMode = true,
            isVisible = showFeatureOverlay,
            onClose = { showFeatureOverlay = false },
            onFeatureClick = { feature ->
                onFeatureClick(feature)
                showFeatureOverlay = false
            }
        )

        // Chat overlay (full screen when active)
        if (showChatOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showChatOverlay = false }
            ) {
                VideoCallChatOverlay(
                    messages = messages,
                    selectedAgent = selectedAgent,
                    transcriptionState = transcriptionState,
                    userInput = userInput,
                    onInputChange = { userInput = it },
                    onSendMessage = {
                        if (userInput.isNotBlank()) {
                            onSendMessage(userInput)
                            userInput = ""
                            focusManager.clearFocus()
                            isKeyboardVisible = false
                        }
                    },
                    onFocusChange = { focused ->
                        isKeyboardVisible = focused
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )

                // Close chat overlay button
                IconButton(
                    onClick = { showChatOverlay = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close chat",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MainVideoFeed(
    selectedAgent: Agent,
    isListening: Boolean,
    isSpeaking: Boolean,
    videoCallState: VideoCallState,
    isUserVideoMaximized: Boolean,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onAgentClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Initialize camera controller
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && isUserVideoMaximized) {
            cameraController = LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.VIDEO_CAPTURE)
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                bindToLifecycle(lifecycleOwner)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 4.dp,
                color = selectedAgent.primaryColor.copy(alpha = 0.2f), // or any color you prefer
                shape = RoundedCornerShape(20.dp)
            )
            .background(Color.Black)
    ) {
        if (isUserVideoMaximized) {
            // User video feed
            if (videoCallState.isVideoEnabled && cameraController != null && hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            controller = cameraController
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { previewView ->
                        previewView.controller = cameraController
                    }
                )
            } else {
                // User placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2D3748)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (!hasCameraPermission) "Camera permission required" else "Camera off",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            // Agent video area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                selectedAgent.primaryColor.copy(alpha = 0.3f),
                                selectedAgent.secondaryColor.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            radius = 800f
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AgentVideoAvatar(
                        agent = selectedAgent,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        size = 120.dp,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = when {
                                isListening -> "Listening to you..."
                                isSpeaking -> "Speaking..."
                                else -> "Ready to help"
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Top overlay with controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: User/Agent info
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = selectedAgent.primaryColor.copy(alpha = 0.2f),
                        spotColor = selectedAgent.secondaryColor.copy(alpha = 0.2f)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(selectedAgent.primaryColor, selectedAgent.secondaryColor),
                                    start = Offset(0f, 0f),
                                    end = Offset(100f, 100f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUserVideoMaximized) "U" else selectedAgent.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (isUserVideoMaximized) "You" else selectedAgent.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Switch Camera (Front/Back) button
                IconButton(
                    onClick = {
                        // Switch between front and back camera
                        cameraController?.let { controller ->
                            val currentSelector = controller.cameraSelector
                            controller.cameraSelector = if (currentSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.1f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color.Black.copy(alpha = 0.2f),
                            spotColor = Color.Black.copy(alpha = 0.2f)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Background glow effect
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        // Camera flip icon

                        Image(
                        painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.resource_switch),
                        contentDescription = "Video Mode",
                        modifier = Modifier.size(34.dp),

                        )
                    }
                }
                // Agent Selection button
                IconButton(
                    onClick = { onAgentClick() },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(selectedAgent.primaryColor.copy(0.3f), selectedAgent.secondaryColor.copy(0.3f)),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = selectedAgent.primaryColor.copy(alpha = 0.3f),
                            spotColor = selectedAgent.secondaryColor.copy(alpha = 0.3f)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Background glow effect
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        // Main icon - using a different icon for agent selection

                        Image(
                        painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.development),
                        contentDescription = "Select Agent",
                        modifier = Modifier.size(34.dp),

                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BotVideoFeed(
    selectedAgent: Agent,
    isListening: Boolean,
    isSpeaking: Boolean,
    onShowFeatures: () -> Unit,
    modifier: Modifier = Modifier,

) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2D3748))
            .border(
                width = 4.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onShowFeatures() }
    ) {
            // Agent video with audio wave
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background image covering the whole surface

                if (selectedAgent.id == "cooking") {
                    Image(
                        painter = painterResource(id = R.drawable.cooking_agent_icon),
                        contentDescription = "Video call placeholder",
                        contentScale = ContentScale.Crop, // Changed from Fit to Crop
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (selectedAgent.id == "crafting"){
                    Image(
                        painter = painterResource(id = R.drawable.crafting_agent),
                        contentDescription = "Video call placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (selectedAgent.id == "companion"){
                    Image(
                        painter = painterResource(id = R.drawable.companion_agent),
                        contentDescription = "Video call placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (selectedAgent.id == "diy"){
                    Image(
                        painter = painterResource(id = R.drawable.diy_agent),
                        contentDescription = "Video call placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        AgentVideoAvatar(
                            agent = selectedAgent,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            size = 60.dp,
                            modifier = Modifier.offset(0.dp, 44.dp)
                        )

                        // Audio wave overlay when speaking
                        if (isSpeaking) {
                            AudioWaveOverlay(
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(58.dp))

                    Text(
                        text = selectedAgent.name,
                        color = selectedAgent.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }



}

@Composable
fun AgentSidebarOld(
    selectedAgent: Agent,
    videoCallState: VideoCallState,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    isUserVideoMaximized: Boolean,
    onShowFeatures: () -> Unit = {}
) {
    var isHovered by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.01f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "hover_scale"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "status_pulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .scale(animatedScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHovered = true
                        tryAwaitRelease()
                        isHovered = false
                    }
                )
            },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            selectedAgent.primaryColor.copy(alpha = 0.15f),
                            selectedAgent.secondaryColor.copy(alpha = 0.25f),
                            Color(0xFF0F172A).copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Animated background particles
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val particleCount = 20
                repeat(particleCount) { i ->
                    val x = (size.width * (i % 5) / 5) + (size.width * 0.1f)
                    val y = (size.height * (i / 5) / 4) + (size.height * 0.1f)
                    drawCircle(
                        color = selectedAgent.primaryColor.copy(alpha = 0.1f),
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Agent Status Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Agent Avatar with Status Ring
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Status ring animation
                        if (isSpeaking || isListening) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        color = if (isSpeaking) selectedAgent.primaryColor.copy(alpha = pulseAlpha)
                                        else selectedAgent.secondaryColor.copy(alpha = pulseAlpha),
                                        shape = CircleShape
                                    )
                            )
                        }

                        // Agent avatar
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            selectedAgent.primaryColor,
                                            selectedAgent.secondaryColor
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .padding(2.dp)
                                .background(
                                    color = Color(0xFF1A202C),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedAgent.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agent name
                    Text(
                        text = selectedAgent.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Status indicator
                    Text(
                        text = when {
                            isSpeaking -> "Speaking..."
                            isListening -> "Listening..."
                            else -> "Ready"
                        },
                        color = when {
                            isSpeaking -> selectedAgent.primaryColor
                            isListening -> selectedAgent.secondaryColor
                            else -> Color.White.copy(alpha = 0.7f)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // Activity Visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSpeaking || isListening) {
                        // Audio wave visualization
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(8) { index ->
                                val animatedHeight by animateFloatAsState(
                                    targetValue = if (isSpeaking || isListening)
                                        (0.4f + (index % 3) * 0.15f) else 0.2f,
                                    animationSpec = tween(
                                        durationMillis = 400 + (index * 50),
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "wave_height_$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(24.dp * animatedHeight)
                                        .background(
                                            color = selectedAgent.primaryColor.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    } else {
                        // Idle state
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tap to speak",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Features Button
                Button(
                    onClick = onShowFeatures,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedAgent.primaryColor.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = selectedAgent.primaryColor.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "Features",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Features",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.VideoCall,
                        value = "HD",
                        label = "Quality",
                        color = selectedAgent.primaryColor
                    )
                    StatItem(
                        icon = Icons.Default.Settings,
                        value = "5/5",
                        label = "Signal",
                        color = selectedAgent.secondaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun AgentSidebar(
    selectedAgent: Agent,
    videoCallState: VideoCallState,
    isListening: Boolean,
    isSpeaking: Boolean,
    isUserVideoMaximized: Boolean,
    modifier: Modifier = Modifier
) {
    // Mock task data - replace with real data from your state
    val currentTask = remember { mutableStateOf("Preparing ingredients") }
    val taskProgress = remember { mutableFloatStateOf(0.3f) }
    val currentStep = remember { mutableIntStateOf(2) }
    val totalSteps = remember { mutableIntStateOf(7) }
    val taskStatus = remember { mutableStateOf(TaskStatus.IN_PROGRESS) }
    val detectedItems = remember { mutableStateOf(listOf("Onions", "Garlic", "Tomatoes")) }
    val aiInsights = remember { mutableStateOf("Great knife technique! Try cutting smaller pieces for even cooking.") }

    // Animation states
    val progressAnimation by animateFloatAsState(
        targetValue = taskProgress.floatValue,
        animationSpec = tween(1000, easing = EaseInOutCubic),
        label = "progress"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isListening || isSpeaking) 1.02f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "pulse"
    )

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .scale(pulseScale),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.4f),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            selectedAgent.primaryColor.copy(alpha = 0.1f),
                            selectedAgent.secondaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with agent status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = when {
                                        isListening -> Color(0xFF10B981)
                                        isSpeaking -> selectedAgent.primaryColor
                                        else -> Color(0xFF6B7280)
                                    },
                                    shape = CircleShape
                                )
                                .then(
                                    if (isListening || isSpeaking) {
                                        Modifier.drawBehind {
                                            drawCircle(
                                                color = when {
                                                    isListening -> Color(0xFF10B981)
                                                    else -> selectedAgent.primaryColor
                                                }.copy(alpha = 0.3f),
                                                radius = size.width * 1.5f
                                            )
                                        }
                                    } else Modifier
                                )
                        )

                        Text(
                            text = when {
                                isListening -> "Listening..."
                                isSpeaking -> "Guiding..."
                                else -> "Watching"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Task type indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = selectedAgent.primaryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "COOKING",
                            color = selectedAgent.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Current task with animated progress
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Task",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = currentTask.value,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Progress bar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Step ${currentStep.intValue}/${totalSteps.intValue}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${(progressAnimation * 100).toInt()}%",
                                color = selectedAgent.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressAnimation)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                selectedAgent.primaryColor,
                                                selectedAgent.secondaryColor
                                            )
                                        ),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // AI Vision Detection
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = selectedAgent.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AI Vision",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(detectedItems.value) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    selectedAgent.primaryColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = item,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = Color.White.copy(alpha = 0.1f)
                )

                // AI Insights
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Live Feedback",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFFBBF24).copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = aiInsights.value,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { /* Handle next step */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = selectedAgent.primaryColor.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = selectedAgent.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Next",
                                color = selectedAgent.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Surface(
                        onClick = { /* Handle help */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Help",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PAUSED,
    ERROR
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}


@Composable
private fun FeatureCard(
    feature: FeatureItem,
    selectedAgent: Agent,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .scale(if (isPressed) 0.95f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A202C)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = selectedAgent.primaryColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = selectedAgent.primaryColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feature.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = feature.description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)
@Composable
fun ChatbotVideoFeedSidebar(
    selectedAgent: Agent,
    videoCallState: VideoCallState,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    isUserVideoMaximized: Boolean,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A202C)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 4.dp,
                    color = Color.White.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Background image covering the whole surface
            Image(
                painter = painterResource(id = R.drawable.cooking_agent_one),
                contentDescription = "Video call placeholder",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Add a subtle overlay when speaking/listening
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(selectedAgent.primaryColor.copy(alpha = 0.2f))
                )
            }

            // Add agent initial in center when not maximized
            if (!isUserVideoMaximized) {
                Text(
                    text = selectedAgent.name.take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Top overlay with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: User/Agent info
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 20.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val backgroundModifier = if (isUserVideoMaximized) {
                            Modifier.background(selectedAgent.primaryColor)
                        } else {
                            Modifier.background(
                                brush = Brush.linearGradient(
                                    colors = listOf(selectedAgent.primaryColor, selectedAgent.secondaryColor)
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .then(backgroundModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isUserVideoMaximized) "U" else selectedAgent.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Text(
                            text = if (isUserVideoMaximized) "You" else selectedAgent.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Right: Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Settings button
                    IconButton(
                        onClick = { /* TODO: Handle settings */ },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItem(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(16.dp)
        )

        Column {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActivityIndicator(
    title: String,
    isActive: Boolean,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isActive) activeColor else Color.Gray,
                    shape = CircleShape
                )
        )

        Text(
            text = title,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun AudioWaveOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500,
                        delayMillis = index * 100,
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ), label = "waveHeight$index"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp * animatedHeight)
                    .background(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            if (index < 4) {
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}


// TranscriptionDisplay component for consistent transcription UI
@Composable
fun TranscriptionDisplayOther(
    transcriptionState: TranscriptionState,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Speaker indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (transcriptionState.isUserSpeaking) "U" else selectedAgent.name.take(1).uppercase(),
                    color = if (transcriptionState.isUserSpeaking) Color.DarkGray else selectedAgent.primaryColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (transcriptionState.isUserSpeaking) "You" else selectedAgent.name,
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Live indicator
                if (transcriptionState.isUserSpeaking) {
                    LiveIndicator()
                }
            }

            // Transcription text with typing animation
            AnimatedTranscriptionText(
                text = transcriptionState.currentText,
                isUserSpeaking = transcriptionState.isUserSpeaking,
                confidence = transcriptionState.confidence
            )
        }
    }
}

@Composable
fun AnimatedTranscriptionText(
    text: String,
    isUserSpeaking: Boolean,
    confidence: Float
) {
    var displayText by remember { mutableStateOf("") }
    var currentIndex by remember { mutableStateOf(0) }

    // Typing animation effect
    LaunchedEffect(text) {
        if (text.isNotEmpty()) {
            displayText = ""
            currentIndex = 0

            // Simulate typing effect for bot responses
            if (!isUserSpeaking) {
                while (currentIndex < text.length) {
                    delay(30) // Typing speed
                    currentIndex++
                    displayText = text.substring(0, currentIndex)
                }
            } else {
                displayText = text
            }
        } else {
            displayText = ""
        }
    }

    Text(
        text = displayText,
        color = Color.Black.copy(alpha = if (isUserSpeaking) confidence else 0.99f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.animateContentSize()
    )
}

@Composable
fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "live_indicator"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Red.copy(alpha = alpha),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = "LIVE",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}