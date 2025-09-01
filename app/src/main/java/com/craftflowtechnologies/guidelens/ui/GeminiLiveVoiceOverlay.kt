package com.craftflowtechnologies.guidelens.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope  
import androidx.compose.foundation.layout.RowScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craftflowtechnologies.guidelens.api.GeminiLiveSessionManager
import com.craftflowtechnologies.guidelens.api.GeminiLiveApiClient
import com.craftflowtechnologies.guidelens.api.SessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Redesigned Gemini Live Voice Overlay with real-time AI streaming
 * Features:
 * - Direct Gemini Live API integration for native audio processing
 * - Real-time voice activity detection and visualization
 * - Smart session management with cost controls
 * - Modern glassmorphism UI design
 * - Emotional context awareness
 * - Multi-agent support with specialized prompts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLiveVoiceOverlay(
    selectedAgent: Agent,
    onClose: () -> Unit,
    onSwitchToVideo: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Gemini Live Session Manager with real audio streaming
    val geminiLiveClient = remember { GeminiLiveApiClient() }
    val liveSessionManager: GeminiLiveSessionManager = viewModel {
        GeminiLiveSessionManager(context, geminiLiveClient)
    }
    
    // State management
    var isMicEnabled by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentTranscription by remember { mutableStateOf("") }
    
    // Collect state from Gemini Live Session Manager with real audio streaming
    val sessionState by liveSessionManager.sessionState.collectAsState()
    val aiInsights by liveSessionManager.aiInsights.collectAsState()
    val emotionalContext by liveSessionManager.emotionalContext.collectAsState()
    val isProcessing by liveSessionManager.isProcessing.collectAsState()
    val isRecording by liveSessionManager.isRecording.collectAsState()
    val isPlayingAudio by liveSessionManager.isPlayingAudio.collectAsState()
    val audioLevel by liveSessionManager.audioLevel.collectAsState()
    val responseTime by liveSessionManager.responseTime.collectAsState()
    
    // Derived state for UI
    val isListening = isRecording
    val isSpeaking = isPlayingAudio
    val voiceActivityLevel = audioLevel
    
    // Audio visualization state
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 800 else 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val waveRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 3000 else 5000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    
    // Handle AI responses from audio streaming
    LaunchedEffect(aiInsights) {
        if (aiInsights.isNotEmpty()) {
            onSendMessage("🎤 AI: $aiInsights")
            currentTranscription = aiInsights
        }
    }
    
    // Auto-resume audio input after AI stops speaking
    LaunchedEffect(isSpeaking) {
        if (!isSpeaking && sessionState == SessionState.CONNECTED && isMicEnabled) {
            delay(500) // Brief pause after AI finishes speaking
            liveSessionManager.resumeAudioInput()
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && sessionState != SessionState.CONNECTED) {
            delay(5000)
            showControls = false
        }
    }
    
    // Gemini Live session management functions with real audio streaming
    fun startLiveAudioSession() {
        coroutineScope.launch {
            try {
                onSendMessage("🔄 Initializing AI voice session...")
                
                // Start the live session with agent-specific configuration
                val sessionStarted = liveSessionManager.startLiveSession(
                    agentType = selectedAgent.id,
                    userTier = "pro" // TODO: Get from user preferences
                )
                
                if (sessionStarted) {
                    onSendMessage("🎤 Started live voice session with ${selectedAgent.name}")
                    onSendMessage("💬 Say something to start the conversation!")
                    
                    // Start audio input automatically
                    delay(1000)
                    if (liveSessionManager.startAudioInput()) {
                        onSendMessage("🎙️ Microphone is active - listening...")
                    }
                } else {
                    onSendMessage("❌ Failed to start voice session. Please check your connection.")
                }
            } catch (e: Exception) {
                onSendMessage("❌ Error starting session: ${e.message}")
            }
        }
    }
    
    fun stopLiveAudioSession() {
        liveSessionManager.stopLiveSession()
        onSendMessage("🔇 Voice session ended")
    }
    
    fun toggleMicrophone() {
        isMicEnabled = !isMicEnabled
        if (isMicEnabled && sessionState == SessionState.CONNECTED) {
            liveSessionManager.startAudioInput()
        } else {
            liveSessionManager.stopAudioInput()
        }
    }
    
    fun toggleAudioSession() {
        when (sessionState) {
            SessionState.CONNECTED -> {
                if (isListening) {
                    liveSessionManager.stopAudioInput()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else {
                    liveSessionManager.startAudioInput()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            SessionState.DISCONNECTED -> {
                startLiveAudioSession()
            }
            else -> {
                // Session is connecting/processing - show feedback
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
    
    // Helper function to get appropriate voice for agent
    fun getVoiceForAgent(agentId: String): String {
        return when (agentId) {
            "cooking" -> "PUCK"      // Friendly, helpful voice for cooking
            "crafting" -> "FENRIR"   // Creative, inspiring voice for crafting
            "diy" -> "AOEDE"         // Professional, clear voice for DIY
            else -> "PUCK"           // Default friendly voice for Buddy
        }
    }
    
    BackHandler { onClose() }
    
    // Main UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = when {
                    sessionState == SessionState.CONNECTED -> Brush.radialGradient(
                        colors = listOf(
                            selectedAgent.primaryColor.copy(alpha = 0.3f),
                            selectedAgent.secondaryColor.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        radius = 800f
                    )
                    isListening -> Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.4f),
                            Color(0xFF059669).copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        radius = 600f
                    )
                    isSpeaking -> Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.4f),
                            Color(0xFF1E40AF).copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        radius = 600f
                    )
                    sessionState == SessionState.CONNECTING -> Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.3f),
                            Color(0xFFD97706).copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        radius = 500f
                    )
                    else -> Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F23),
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Close button
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Session status indicator
        if (sessionState == SessionState.CONNECTED || sessionState == SessionState.CONNECTING) {
            GeminiLiveSessionStatus(
                sessionState = sessionState,
                connectionQuality = "Excellent", // Would come from actual connection metrics
                isListening = isListening,
                isSpeaking = isSpeaking,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        
        // AI Insights Panel
        if (aiInsights.isNotEmpty() || emotionalContext != com.craftflowtechnologies.guidelens.api.EmotionalContext.NEUTRAL) {
            AnimatedVisibility(
                visible = showControls || sessionState == SessionState.CONNECTED,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                GeminiLiveVoiceInsights(
                    insights = aiInsights,
                    emotionalContext = emotionalContext,
                    isProcessing = isProcessing,
                    selectedAgent = selectedAgent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
        
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Voice mode background with animations
            VoiceModeBackground(
                isUserSpeaking = isListening,
                isAISpeaking = isSpeaking,
                primaryColor = selectedAgent.primaryColor,
                modifier = Modifier.fillMaxSize()
            )
            
            // Central voice interface
            GeminiLiveVoiceInterface(
                agent = selectedAgent,
                sessionState = sessionState,
                isListening = isListening,
                isSpeaking = isSpeaking,
                voiceActivityLevel = voiceActivityLevel,
                pulseScale = pulseScale,
                waveRotation = waveRotation,
                onToggleListening = { 
                    if (isListening) {
                        liveSessionManager.stopAudioInput()
                    } else {
                        liveSessionManager.startAudioInput()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            // Transcription display
            if (currentTranscription.isNotEmpty()) {
                GeminiLiveTranscription(
                    text = currentTranscription,
                    isUserSpeaking = isListening,
                    isSpeaking = isSpeaking,
                    agent = selectedAgent,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            
            // Agent welcome message when idle
            if (sessionState == SessionState.DISCONNECTED && currentTranscription.isEmpty()) {
                VoiceWelcomeMessage(
                    agent = selectedAgent,
                    onStartSession = { startLiveAudioSession() },
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        
        // Bottom controls
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GeminiLiveVoiceControls(
                sessionState = sessionState,
                isListening = isListening,
                isMicEnabled = isMicEnabled,
                onToggleListening = { toggleAudioSession() },
                onToggleMic = { toggleMicrophone() },
                onSwitchToVideo = onSwitchToVideo,
                onEndSession = { 
                    when (sessionState) {
                        SessionState.CONNECTED, 
                        SessionState.CONNECTING -> stopLiveAudioSession()
                        else -> onClose()
                    }
                },
                selectedAgent = selectedAgent,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun GeminiLiveVoiceInterface(
    agent: Agent,
    sessionState: SessionState,
    isListening: Boolean,
    isSpeaking: Boolean,
    voiceActivityLevel: Float,
    pulseScale: Float,
    waveRotation: Float,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Enhanced voice wave animation
        if (isListening || isSpeaking || sessionState == SessionState.CONNECTED) {
            EnhancedVoiceWaveAnimation(
                isUserSpeaking = isListening,
                isAISpeaking = isSpeaking,
                voiceActivityLevel = voiceActivityLevel,
                primaryColor = agent.primaryColor,
                modifier = Modifier.size(300.dp)
            )
        }
        
        // Central agent avatar
        Surface(
            onClick = onToggleListening,
            modifier = Modifier
                .size(160.dp)
                .scale(if (isListening || isSpeaking) pulseScale else 1f),
            shape = CircleShape,
            color = when (sessionState) {
                SessionState.CONNECTED -> agent.primaryColor
                SessionState.CONNECTING -> agent.primaryColor.copy(alpha = 0.7f)
                SessionState.DISCONNECTING -> Color(0xFFFF9800)
                SessionState.DISCONNECTED -> Color.White.copy(alpha = 0.15f)
            },
            shadowElevation = if (sessionState == SessionState.CONNECTED) 20.dp else 8.dp,
            border = BorderStroke(
                width = 2.dp,
                color = when {
                    isListening -> Color(0xFF10B981)
                    isSpeaking -> agent.secondaryColor
                    else -> Color.White.copy(alpha = 0.2f)
                }
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                when (sessionState) {
                    SessionState.CONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = when {
                                isListening -> Icons.Rounded.Mic
                                isSpeaking -> Icons.Rounded.RecordVoiceOver
                                sessionState == SessionState.CONNECTED -> Icons.Rounded.Hearing
                                else -> Icons.Rounded.PlayArrow
                            },
                            contentDescription = when {
                                isListening -> "Listening"
                                isSpeaking -> "Speaking"
                                sessionState == SessionState.CONNECTED -> "Connected"
                                else -> "Start Session"
                            },
                            tint = if (sessionState == SessionState.CONNECTED) Color.White else agent.primaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        
        // Session state indicator
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = agent.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when (sessionState) {
                    SessionState.CONNECTING -> "Connecting to Gemini Live..."
                    SessionState.CONNECTED -> when {
                        isListening -> "I'm listening..."
                        isSpeaking -> "Speaking..."
                        else -> "Live session active - Tap to speak"
                    }
                    SessionState.DISCONNECTING -> "Ending session..."
                    SessionState.DISCONNECTED -> "Tap to start live session"
                },
                color = when (sessionState) {
                    SessionState.CONNECTED -> Color.White.copy(alpha = 0.9f)
                    SessionState.CONNECTING -> Color.Yellow
                    SessionState.DISCONNECTING -> Color(0xFFFF9800)
                    else -> Color.White.copy(alpha = 0.6f)
                },
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GeminiLiveAudioVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    activityLevel: Float,
    rotation: Float,
    agent: Agent,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 4
        
        // Draw multiple animated rings
        for (i in 0 until 5) {
            val ringRadius = baseRadius + (i * 15.dp.toPx())
            val alpha = (0.6f - i * 0.1f) * activityLevel
            
            if (alpha > 0f) {
                val color = when {
                    isListening -> Color(0xFF10B981).copy(alpha = alpha)
                    isSpeaking -> agent.primaryColor.copy(alpha = alpha)
                    else -> Color.White.copy(alpha = alpha * 0.3f)
                }
                
                // Rotating wave effect
                val waveOffset = rotation + (i * 45f)
                val radiusVariation = sin(Math.toRadians(waveOffset.toDouble())).toFloat() * 10.dp.toPx()
                
                drawCircle(
                    color = color,
                    radius = ringRadius + radiusVariation,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Draw activity bars around the circle
        if (isListening || isSpeaking) {
            val barCount = 12
            for (i in 0 until barCount) {
                val angle = (i * 360f / barCount) + rotation
                val barLength = 15.dp.toPx() * (0.3f + activityLevel * 0.7f)
                val startRadius = baseRadius + 40.dp.toPx()
                val endRadius = startRadius + barLength
                
                val startX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * startRadius
                val startY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * startRadius
                val endX = center.x + cos(Math.toRadians(angle.toDouble())).toFloat() * endRadius
                val endY = center.y + sin(Math.toRadians(angle.toDouble())).toFloat() * endRadius
                
                val color = when {
                    isListening -> Color(0xFF10B981)
                    isSpeaking -> agent.primaryColor
                    else -> Color.White.copy(alpha = 0.3f)
                }
                
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun GeminiLiveSessionStatus(
    sessionState: SessionState,
    connectionQuality: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 1.dp,
            color = when (sessionState) {
                SessionState.CONNECTED -> Color(0xFF10B981).copy(alpha = pulseAlpha)
                SessionState.CONNECTING -> Color.Yellow.copy(alpha = pulseAlpha)
                else -> Color.Gray.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when (sessionState) {
                            SessionState.CONNECTED -> Color(0xFF10B981)
                            SessionState.CONNECTING -> Color.Yellow
                            else -> Color.Gray
                        },
                        shape = CircleShape
                    )
            )
            
            Column {
                Text(
                    text = when (sessionState) {
                        SessionState.CONNECTED -> "GEMINI LIVE"
                        SessionState.CONNECTING -> "CONNECTING..."
                        else -> "DISCONNECTED"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when {
                        sessionState == SessionState.CONNECTED && isListening -> "Listening"
                        sessionState == SessionState.CONNECTED && isSpeaking -> "Speaking"
                        sessionState == SessionState.CONNECTED -> connectionQuality
                        else -> "Tap center to connect"
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun GeminiLiveVoiceInsights(
    insights: String,
    emotionalContext: com.craftflowtechnologies.guidelens.api.EmotionalContext,
    isProcessing: Boolean,
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
            color = selectedAgent.primaryColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = "AI Insights",
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "Gemini Live Insights",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Emotional context indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = emotionalContext.color.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, emotionalContext.color.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = emotionalContext.icon,
                            contentDescription = emotionalContext.displayName,
                            tint = emotionalContext.color,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = emotionalContext.displayName,
                            color = emotionalContext.color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Processing indicator
            if (isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = selectedAgent.primaryColor,
                        strokeWidth = 1.dp
                    )
                    Text(
                        text = "Processing...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Insights content
            if (insights.isNotEmpty()) {
                Text(
                    text = insights,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun GeminiLiveTranscription(
    text: String,
    isUserSpeaking: Boolean,
    isSpeaking: Boolean,
    agent: Agent,
    modifier: Modifier = Modifier
) {
    var displayText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        if (text.isEmpty()) {
            displayText = ""
        } else if (isSpeaking) {
            // Typing animation for AI responses
            displayText = ""
            text.forEachIndexed { index, _ ->
                delay(30)
                displayText = text.substring(0, index + 1)
            }
        } else {
            displayText = text
        }
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isUserSpeaking) Color(0xFF10B981) else agent.primaryColor
        )
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
                    imageVector = if (isUserSpeaking) Icons.Default.Person else Icons.Rounded.SmartToy,
                    contentDescription = if (isUserSpeaking) "You" else agent.name,
                    tint = if (isUserSpeaking) Color(0xFF10B981) else agent.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isUserSpeaking) "You" else agent.name,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Transcription text
            Text(
                text = displayText + if (isSpeaking && displayText.isNotEmpty() && displayText.length < text.length) "▌" else "",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun VoiceWelcomeMessage(
    agent: Agent,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hi! I'm ${agent.name}",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = agent.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onStartSession,
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Start Voice Session",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Live Session",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GeminiLiveVoiceControls(
    sessionState: SessionState,
    isListening: Boolean,
    isMicEnabled: Boolean,
    onToggleListening: () -> Unit,
    onToggleMic: () -> Unit,
    onSwitchToVideo: () -> Unit,
    onEndSession: () -> Unit,
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
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic toggle
            GeminiLiveControlButton(
                icon = if (isMicEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                isActive = isMicEnabled && isListening,
                color = if (isMicEnabled) Color(0xFF10B981) else Color.Red,
                onClick = onToggleMic,
                label = if (isMicEnabled) "Mic On" else "Muted",
                enabled = sessionState == SessionState.CONNECTED
            )
            
            // Main listening toggle
            GeminiLiveControlButton(
                icon = when (sessionState) {
                    SessionState.CONNECTED -> 
                        if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic
                    SessionState.CONNECTING -> Icons.Rounded.HourglassEmpty
                    else -> Icons.Rounded.PlayArrow
                },
                isActive = isListening,
                color = when (sessionState) {
                    SessionState.CONNECTED -> selectedAgent.primaryColor
                    SessionState.CONNECTING -> Color(0xFFF59E0B)
                    SessionState.DISCONNECTING -> Color.Red
                    else -> selectedAgent.primaryColor.copy(alpha = 0.6f)
                },
                onClick = onToggleListening,
                label = when (sessionState) {
                    SessionState.CONNECTED -> if (isListening) "Stop" else "Listen"
                    SessionState.CONNECTING -> "Connecting..."
                    SessionState.DISCONNECTING -> "Retry"
                    else -> "Start"
                },
                size = 64.dp,
                enabled = sessionState != SessionState.CONNECTING
            )
            
            // Video mode
            GeminiLiveControlButton(
                icon = Icons.Rounded.Videocam,
                isActive = false,
                color = Color(0xFF2196F3),
                onClick = onSwitchToVideo,
                label = "Video",
                enabled = sessionState == SessionState.CONNECTED
            )
            
            // End session
            GeminiLiveControlButton(
                icon = Icons.Rounded.CallEnd,
                isActive = false,
                color = Color.Red,
                onClick = onEndSession,
                label = "End"
            )
        }
    }
}

@Composable
private fun GeminiLiveControlButton(
    icon: ImageVector,
    isActive: Boolean,
    color: Color,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = if (enabled) onClick else { {} },
            modifier = modifier
                .size(size)
                .scale(scale),
            shape = CircleShape,
            color = when {
                !enabled -> Color.Gray.copy(alpha = 0.3f)
                isActive -> color
                else -> Color.White.copy(alpha = 0.15f)
            },
            shadowElevation = if (isActive && enabled) 8.dp else 2.dp,
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
                        !enabled -> Color.Gray
                        isActive -> Color.White
                        else -> color
                    },
                    modifier = Modifier.size((size.value * 0.4f).dp)
                )
            }
        }
        
        Text(
            text = label,
            color = if (enabled) Color.White.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}