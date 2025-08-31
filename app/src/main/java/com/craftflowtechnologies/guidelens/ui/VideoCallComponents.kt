package com.craftflowtechnologies.guidelens.ui


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle


@Composable
fun VideoCallMessageBubble(
    message: ChatMessage,
    selectedAgent: Agent
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isFromUser) 12.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 12.dp
            ),
            color = if (message.isFromUser) {
                selectedAgent.primaryColor.copy(alpha = 0.8f)
            } else {
                Color.White.copy(alpha = 0.1f)
            },
            modifier = Modifier.widthIn(max = 240.dp)
        ) {
            Text(
                text = message.text,
                color = MaterialTheme.guideLensColors.textPrimary,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// TranscriptionDisplay component for consistent transcription UI
// Updated transcription display for video mode (controlled by bot state)
@Composable
fun TranscriptionDisplayVideo(
    transcriptionState: TranscriptionState,
    selectedAgent: Agent,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = transcriptionState.currentText.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.guideLensColors.overlayBackground
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Speaker indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = if (transcriptionState.isUserSpeaking) Icons.Default.Person else selectedAgent.icon,
                        contentDescription = if (transcriptionState.isUserSpeaking) "You" else selectedAgent.name,
                        tint = if (transcriptionState.isUserSpeaking) Color.White else selectedAgent.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (transcriptionState.isUserSpeaking) "You" else selectedAgent.name,
                        color = MaterialTheme.guideLensColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Live indicator for user speech
                    if (transcriptionState.isUserSpeaking) {
                        LiveIndicator()
                    }
                }

                // Transcription text
                Text(
                    text = transcriptionState.currentText,
                    color = MaterialTheme.guideLensColors.textPrimary.copy(alpha = if (transcriptionState.isUserSpeaking) transcriptionState.confidence else 0.95f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.animateContentSize()
                )
            }
        }
    }
}


@Composable
fun AnimatedTranscriptionTextVideo(
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
        color = MaterialTheme.guideLensColors.textPrimary.copy(alpha = if (isUserSpeaking) confidence else 0.9f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.animateContentSize()
    )
}



// Updated VideoCallBottomControls without mute button
@Composable
fun VideoCallBottomControls(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    videoCallState: VideoCallState,
    onToggleVideo: () -> Unit,
    onVoiceRecord: () -> Unit,
    isListening: Boolean,
    selectedAgent: Agent,
    onClose: () -> Unit,
//    onToggleSession: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onToggleChatOverlay: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Call controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video toggle button
//                VideoCallControlButton(
//                    icon = if (videoCallState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
//                    isActive = videoCallState.isVideoEnabled,
//                    activeColor = selectedAgent.primaryColor,
//                    inactiveColor = Color.Red,
//                    onClick = onToggleVideo,
//                    contentDescription = if (videoCallState.isVideoEnabled) "Turn off video" else "Turn on video"
//                )
                Image(
                painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.no_video),
                contentDescription = "Open chat",
                modifier = Modifier
                    .clickable {
                        onToggleVideo()
                    }
                    .size(34.dp),

                )

                // Chat overlay button - now uses agent colors

                Image(
                painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.new_message),
                contentDescription = "Open chat",
                modifier = Modifier
                    .clickable {
                        onToggleChatOverlay()
                    }
                    .size(34.dp),

                )

                // Voice recording button (main interaction)
                VideoCallVoiceButton(
                    isListening = isListening,
                    selectedAgent = selectedAgent,
                    onClick = onVoiceRecord,
                    isSessionActive = videoCallState.isSessionActive,
                    modifier = Modifier.size(64.dp)
                )


                // Session toggle button
//                VideoCallControlButton(
//                    icon = if (videoCallState.isSessionActive) Icons.Default.Pause else Icons.Default.PlayArrow,
//                    isActive = videoCallState.isSessionActive,
//                    activeColor = selectedAgent.primaryColor,
//                    inactiveColor = Color(0xFFFBBF24),
//                    onClick = onToggleSession,
//                    contentDescription = if (videoCallState.isSessionActive) "Pause session" else "Resume session"
//                )

                // End call button

                Image(
                painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.phone_call_end),
                contentDescription = "End call",
                modifier = Modifier
                    .clickable {
                        onClose()
                    }
                    .size(34.dp),

                )

            }

            Spacer(modifier = Modifier.height(16.dp))


        }
    }
}

@Composable
fun VideoCallChatOverlay(
    messages: List<ChatMessage>,
    selectedAgent: Agent,
    transcriptionState: TranscriptionState,
    userInput: String,
    modifier: Modifier = Modifier,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = Color.Black.copy(alpha = 0.45f) // More transparent background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main chat interface
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.80f)
                    .align(Alignment.CenterStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 0.dp
                ),
                color = Color.Black.copy(alpha = 0.9f),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Enhanced chat header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = selectedAgent.primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 20.dp,
                            bottomEnd = 0.dp,
                            bottomStart = 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Agent avatar
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    selectedAgent.primaryColor,
                                                    selectedAgent.secondaryColor
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedAgent.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Chat with ${selectedAgent.name}",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${messages.size} messages",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Message count badge
                            if (messages.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = selectedAgent.primaryColor
                                ) {
                                    Text(
                                        text = messages.size.toString(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Live transcription section (if active)
                    if (transcriptionState.currentText.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            shape = RectangleShape
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Pulsing indicator
                                    val pulseAlpha by animateFloatAsState(
                                        targetValue = if (transcriptionState.isUserSpeaking) 1f else 0.3f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "pulse"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = Color.Red.copy(alpha = pulseAlpha),
                                                shape = CircleShape
                                            )
                                    )

                                    Text(
                                        text = "Live Transcription",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = transcriptionState.currentText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    style = TextStyle(
                                        fontStyle = FontStyle.Italic,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }

                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            color = Color.White.copy(alpha = 0.1f)
                        )
                    }

                    // Messages list
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = true // Latest messages at bottom
                    ) {
                        items(messages.reversed()) { message ->
                            VideoCallMessageBubble(
                                message = message,
                                selectedAgent = selectedAgent
                            )
                        }

                        // Add some bottom padding for better UX
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    //  footer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Text input for backup communication
                            VideoCallTextInput(
                                value = userInput,
                                onValueChange = onInputChange,
                                onSendMessage = onSendMessage,
                                selectedAgent = selectedAgent,
                                placeholder = "Type a message or use voice...",
                                onFocusChange = onFocusChange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCallControlButton(
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) activeColor else inactiveColor
    val contentColor = Color.White

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .background(
                color = backgroundColor.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .clip(CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

    }
}

@Composable
fun VideoCallVoiceButton(
    isListening: Boolean,
    selectedAgent: Agent,
    onClick: () -> Unit,
    isSessionActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening && isSessionActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ), label = "voice_pulse"
    )

    // Outer ripple effect when listening
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        if (isListening && isSessionActive) {
            repeat(2) { index ->
                val delay = index * 400
                val rippleScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1600,
                            delayMillis = delay,
                            easing = EaseOut
                        ),
                        repeatMode = RepeatMode.Restart
                    ), label = "voice_ripple$index"
                )

                val rippleAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 1600,
                            delayMillis = delay,
                            easing = EaseOut
                        ),
                        repeatMode = RepeatMode.Restart
                    ), label = "voice_ripple_alpha$index"
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(rippleScale)
                        .alpha(rippleAlpha)
                        .background(
                            color = selectedAgent.primaryColor.copy(0.02f),
                            shape = CircleShape
                        )
                )
            }
        }

        // Main button
        IconButton(
            onClick = onClick,
            enabled = isSessionActive,
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .background(
                    brush = if (isSessionActive) {
                        Brush.linearGradient(
                            colors = listOf(
                                selectedAgent.primaryColor.copy(0.02f),
                                selectedAgent.secondaryColor.copy(0.02f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.6f),
                                Color.Gray.copy(alpha = 0.4f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clip(CircleShape)
        ) {

            if (isListening && isSessionActive) {
                Image(
                painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.mute),
                contentDescription = "Stop listening",
                modifier = Modifier

                    .size(34.dp),

                )

            }else{
                Image(
                    painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.speaker),
                    contentDescription = "Start listening",
                    modifier = Modifier

                        .size(34.dp),

                    )
            }
        }
    }
}

@Composable
fun VideoCallTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    selectedAgent: Agent,
    placeholder: String,
    onFocusChange: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChange(focusState.isFocused)
                },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = selectedAgent.primaryColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = selectedAgent.primaryColor,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
            singleLine = true,
//            keyboardOptions = KeyboardOptions(
//                imeAction = ImeAction.Send
//            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    onSendMessage()
                }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send button
        IconButton(
            onClick = onSendMessage,
            enabled = value.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (value.isNotBlank()) {
                        selectedAgent.primaryColor
                    } else {
                        Color.Gray.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}





