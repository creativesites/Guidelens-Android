package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

// Updated EnhancedVoiceOverlay to handle feature selection
@Composable
fun GuideVoiceSessionOverlay(
    selectedAgent: Agent,
    transcriptionState: TranscriptionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    featureSelectionState: FeatureSelectionState?,
    onClose: () -> Unit,
    onAgentClick: () -> Unit,
    onFeatureClick: (Any) -> Unit,
    onBackClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    isUserMuted: Boolean,
    isBotMuted: Boolean,
    onToggleUserMute: () -> Unit,
    onToggleBotMute: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Animations (same as before)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 300 else 600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 400 else 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    // Background gradient (same as before)
    val backgroundGradient = when {
        isListening -> Brush.radialGradient(
            colors = listOf(
                MaterialTheme.guideLensColors.successColor.copy(alpha = 0.4f),
                MaterialTheme.guideLensColors.successColor.copy(alpha = 0.2f),
                MaterialTheme.guideLensColors.overlayBackground
            ),
            radius = 800f
        )
        isSpeaking -> Brush.radialGradient(
            colors = listOf(
                selectedAgent.primaryColor.copy(alpha = 0.4f),
                selectedAgent.secondaryColor.copy(alpha = 0.2f),
                MaterialTheme.guideLensColors.overlayBackground
            ),
            radius = 800f
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.guideLensColors.gradientStart.copy(alpha = 0.95f),
                MaterialTheme.guideLensColors.gradientEnd.copy(alpha = 0.98f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // Close button - now functional with better visibility

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 8.dp)
        ) {
            Image(
                painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.close),
                contentDescription = "Close Voice Mode",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        }

        // Back button - shown only when a feature is expanded with better visibility
        if (featureSelectionState?.isExpanded == true) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.guideLensColors.cardBackground.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.guideLensColors.borderColor,
                        shape = CircleShape
                    )
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.guideLensColors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Main content area
        when {
            isListening || isSpeaking -> {
                VoiceActiveMode(
                    selectedAgent = selectedAgent,
                    transcriptionState = transcriptionState,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    pulseScale = pulseScale,
                    waveAnimation = waveAnimation,
                    onAgentClick = onAgentClick,
                    featureSelectionState = featureSelectionState,
                    onFeatureClick = onFeatureClick,
                    onBackClick = onBackClick
                )
            }
            else -> {
                VoiceIdleMode(
                    selectedAgent = selectedAgent,
                    onAgentClick = onAgentClick,
                    onFeatureClick = onFeatureClick,
                    featureSelectionState = featureSelectionState,
                    onBackClick = onBackClick
                )
            }
        }

        // Updated MinimalVoiceControls with mute functionality
        MinimalVoiceControls(
            voiceState = VoiceState(
                isListening = isListening,
                isMuted = isUserMuted,
                isBotMuted = isBotMuted
            ),
            onMicClick = { /* Toggle listening */ },
            onMuteClick = onToggleUserMute,
            onBotMuteClick = onToggleBotMute,
            onVideoClick = onVideoModeClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
@Composable
fun GuideLegacyVoiceOverlay(
    selectedAgent: Agent,
    transcriptionState: TranscriptionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    onClose: () -> Unit,
    onAgentClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Different animations for listening vs speaking
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 250 else 400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 500 else 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    // Gradient colors based on state
    val overlayColors = when {
        isListening -> listOf(
            Color(0xFF10B981).copy(alpha = 0.9f),
            Color(0xFF059669).copy(alpha = 0.7f),
            Color(0xFF047857).copy(alpha = 0.5f)
        )
        isSpeaking -> listOf(
            selectedAgent.primaryColor.copy(alpha = 0.9f),
            selectedAgent.secondaryColor.copy(alpha = 0.7f),
            selectedAgent.primaryColor.copy(alpha = 0.5f)
        )
        else -> listOf(
            Color(0xFF1E293B).copy(alpha = 0.9f),
            Color(0xFF334155).copy(alpha = 0.7f),
            Color(0xFF475569).copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = overlayColors
                )
            )
    ) {
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Voice Mode",
                tint = Color.White
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Agent avatar with animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { onAgentClick() },
                contentAlignment = Alignment.Center
            ) {
                // Outer pulse rings
                repeat(3) { index ->
                    val delay = index * 200
                    val ringScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 2000,
                                delayMillis = delay,
                                easing = EaseOut
                            ),
                            repeatMode = RepeatMode.Restart
                        ), label = "ring$index"
                    )

                    val ringAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 2000,
                                delayMillis = delay,
                                easing = EaseOut
                            ),
                            repeatMode = RepeatMode.Restart
                        ), label = "ringAlpha$index"
                    )

                    if (isListening || isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(ringScale)
                                .alpha(ringAlpha)
                                .background(
                                    color = if (isListening) Color.Green else selectedAgent.primaryColor,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Main avatar
                Box(
                    modifier = Modifier
                        .size(if (isListening || isSpeaking) 120.dp else 100.dp)
                        .scale(if (isListening) pulseScale else 1f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    selectedAgent.primaryColor,
                                    selectedAgent.secondaryColor
                                )
                            ),
                            shape = CircleShape
                        )
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedAgent.icon,
                        contentDescription = selectedAgent.name,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Agent name
            Text(
                text = selectedAgent.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status text
            Text(
                text = when {
                    isListening -> "Listening..."
                    isSpeaking -> "Speaking..."
                    else -> "Ready to help"
                },
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Audio visualization
            AudioWaveform(
                isActive = isListening || isSpeaking,
                isListening = isListening,
                color = if (isListening) Color.Green else selectedAgent.primaryColor,
                animationProgress = waveAnimation
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Transcription area
            TranscriptionDisplay(
                transcriptionState = transcriptionState,
                selectedAgent = selectedAgent
            )
        }
    }
}

@Composable
fun AudioWaveform(
    isActive: Boolean,
    isListening: Boolean,
    color: Color,
    animationProgress: Float
) {
    Row(
        modifier = Modifier.height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(7) { index ->
            val baseHeight = 20.dp
            val maxHeight = 50.dp

            val heightMultiplier = if (isActive) {
                val waveOffset = (index * 0.5f) + (animationProgress * 0.5f)
                val wave = (sin(waveOffset) * 0.5f + 0.5f).coerceIn(0f, 1f)
                0.3f + (wave * 0.7f)
            } else {
                0.3f
            }

            val barHeight = baseHeight + (maxHeight - baseHeight) * heightMultiplier

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(barHeight)
                    .background(
                        color = color.copy(alpha = if (isActive) 0.9f else 0.3f),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

@Composable
fun TranscriptionDisplay(
    transcriptionState: TranscriptionState,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = transcriptionState.currentText.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Speaker indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = if (transcriptionState.isUserSpeaking) Icons.Default.Person else selectedAgent.icon,
                        contentDescription = if (transcriptionState.isUserSpeaking) "You" else selectedAgent.name,
                        tint = if (transcriptionState.isUserSpeaking) Color.Green else selectedAgent.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (transcriptionState.isUserSpeaking) "You" else selectedAgent.name,
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Transcription text with typing animation
                AnimatedTranscriptionText(
                    text = transcriptionState.currentText,
                    isUserSpeaking = transcriptionState.isUserSpeaking,
                    selectedAgent = selectedAgent
                )

                // Confidence indicator (for user speech)
                if (transcriptionState.isUserSpeaking) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            val isActive = index < (transcriptionState.confidence * 5).toInt()
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = if (isActive) Color.Green else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )
                            if (index < 4) Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedTranscriptionText(
    text: String,
    isUserSpeaking: Boolean,
    selectedAgent: Agent
) {
    var displayText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        if (text.isEmpty()) {
            displayText = ""
        } else if (!isUserSpeaking) {
            // Typing animation for AI responses
            displayText = ""
            text.forEachIndexed { index, char ->
                delay(30)
                displayText = text.substring(0, index + 1)
            }
        } else {
            // Immediate display for user speech
            displayText = text
        }
    }

    Text(
        text = displayText + if (!isUserSpeaking && displayText.isNotEmpty() && displayText.length < text.length) "▌" else "",
        color = Color.Black.copy(alpha = 0.8f),
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
}

// Updated VoiceActiveMode to include feature selection
@Composable
fun VoiceActiveMode(
    selectedAgent: Agent,
    transcriptionState: TranscriptionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    pulseScale: Float,
    waveAnimation: Float,
    onAgentClick: () -> Unit,
    featureSelectionState: FeatureSelectionState? = null,
    onFeatureClick: (Any) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        // Show feature selection if active
        if (featureSelectionState?.isExpanded == true && featureSelectionState.selectedFeature != null) {
            ExpandedFeatureView(
                feature = featureSelectionState.selectedFeature,
                agent = selectedAgent,
                showVideoButton = featureSelectionState.showVideoButton,
                onBackClick = onBackClick,
                onVideoModeClick = { /* Handle video mode */ },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Default voice mode UI
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .background(
                        color = selectedAgent.primaryColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = selectedAgent.primaryColor.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onAgentClick)
            ) {
                // Audio visualization
                if (isListening || isSpeaking) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 3

                        for (i in 0..7) {
                            val angle = waveAnimation + (i * (2f * PI.toFloat() / 8))
                            val waveRadius = radius * (0.8f + 0.2f * sin(angle))

                            drawCircle(
                                color = selectedAgent.primaryColor.copy(
                                    alpha = 0.3f - (i * 0.03f)
                                ),
                                radius = waveRadius,
                                center = center
                            )
                        }
                    }
                }

                Icon(
                    imageVector = selectedAgent.icon,
                    contentDescription = selectedAgent.name,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Transcription
            Text(
                text = transcriptionState.currentText.ifEmpty {
                    if (isListening) "Listening..." else if (isSpeaking) "Agent is speaking..." else ""
                },
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Confidence indicator
            if (isListening && transcriptionState.confidence < 0.9f) {
                LinearProgressIndicator(
                progress = { transcriptionState.confidence },
                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .padding(horizontal = 32.dp),
                color = when {
                                        transcriptionState.confidence > 0.7f -> Color.Green
                                        transcriptionState.confidence > 0.4f -> Color.Yellow
                                        else -> Color.Red
                                    },
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )

                Text(
                    text = when {
                        transcriptionState.confidence > 0.7f -> "Good recognition"
                        transcriptionState.confidence > 0.4f -> "Fair recognition"
                        else -> "Poor recognition - try speaking clearly"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}


// Updated VoiceIdleMode to handle feature selection
@Composable
fun VoiceIdleMode(
    selectedAgent: Agent,
    onAgentClick: () -> Unit,
    onFeatureClick: (Any) -> Unit,
    featureSelectionState: FeatureSelectionState? = null,
    onBackClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        if (featureSelectionState?.isExpanded == true && featureSelectionState.selectedFeature != null) {
            ExpandedFeatureView(
                feature = featureSelectionState.selectedFeature,
                agent = selectedAgent,
                showVideoButton = featureSelectionState.showVideoButton,
                onBackClick = onBackClick,
                onVideoModeClick = { /* Handle video mode */ },
                modifier = Modifier.weight(1f)
            )
        } else {
            // Default idle mode UI
            AgentWelcomeCard(
                agent = selectedAgent,
                onFeatureClick = onFeatureClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
fun VoiceAgentAvatar(
    agent: Agent,
    isListening: Boolean,
    isSpeaking: Boolean,
    pulseScale: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated rings for active states
        if (isListening || isSpeaking) {
            repeat(3) { index ->
                val delay = index * 300
                val ringScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2000,
                            delayMillis = delay,
                            easing = EaseOut
                        ),
                        repeatMode = RepeatMode.Restart
                    ), label = "ring$index"
                )

                val ringAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2000,
                            delayMillis = delay,
                            easing = EaseOut
                        ),
                        repeatMode = RepeatMode.Restart
                    ), label = "ringAlpha$index"
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha)
                        .border(
                            width = 2.dp,
                            color = if (isListening) Color(0xFF10B981) else agent.primaryColor,
                            shape = CircleShape
                        )
                )
            }
        }

        // Main avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(if (isListening || isSpeaking) pulseScale else 1f)
                .clickable { onClick() }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            agent.primaryColor.copy(alpha = 0.9f),
                            agent.secondaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = agent.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VoiceStatusIndicator(
    agentName: String,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = agentName,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when {
                            isListening -> Color(0xFF10B981)
                            isSpeaking -> Color(0xFF3B82F6)
                            else -> Color.Gray
                        },
                        shape = CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = when {
                    isListening -> "Listening..."
                    isSpeaking -> "Speaking..."
                    else -> "Ready to help"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun EnhancedTranscriptionDisplay(
    transcriptionState: TranscriptionState,
    selectedAgent: Agent,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp, max = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isListening) "You're saying..." else "Response",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                if (isListening || isSpeaking) {
                    LoadingDots(
                        color = if (isListening) Color(0xFF10B981) else selectedAgent.primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transcription content
            when {
                transcriptionState.currentText.isNotEmpty() && isListening -> {
                    Text(
                        text = transcriptionState.currentText,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
                transcriptionState.currentText.isNotEmpty() && isSpeaking -> {
                    Text(
                        text = transcriptionState.currentText,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
                else -> {
                    Text(
                        text = "Tap to start speaking...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceWelcomeSection(
    agent: Agent,
    onAgentClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Agent avatar (smaller for idle mode)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clickable { onAgentClick() }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            agent.primaryColor,
                            agent.secondaryColor
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = agent.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Hi! I'm ${agent.name}",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = agent.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun VoiceFeatureGrid(
    agent: Agent,
    onFeatureClick: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        when (agent.id) {
            "cooking" -> {
                items(agent.features as List<CookingFeature>) { feature ->
                    VoiceFeatureCard(
                        title = feature.title,
                        description = feature.description,
                        icon = feature.icon,
                        color = agent.primaryColor,
                        onClick = { onFeatureClick(feature) }
                    )
                }
            }
            "crafting" -> {
                items(agent.features as List<CraftingProject>) { project ->
                    VoiceFeatureCard(
                        title = project.title,
                        description = project.difficulty,
                        icon = project.icon,
                        color = agent.primaryColor,
                        onClick = { onFeatureClick(project) }
                    )
                }
            }
            "companion" -> {
                items(agent.features as List<FriendshipTool>) { tool ->
                    VoiceFeatureCard(
                        title = tool.title,
                        description = tool.duration,
                        icon = tool.icon,
                        color = agent.primaryColor,
                        onClick = { onFeatureClick(tool) }
                    )
                }
            }
            "diy" -> {
                items(agent.features as List<DIYCategory>) { category ->
                    VoiceFeatureCard(
                        title = category.title,
                        description = category.urgency,
                        icon = category.icon,
                        color = agent.primaryColor,
                        onClick = { onFeatureClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VoicePrompt() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Tap the avatar to start voice conversation",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun LoadingDots(
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
            )
        }
    }
}
