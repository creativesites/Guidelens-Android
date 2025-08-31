package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

// Enhanced Chat Messages Area with animations and thinking indicator
@Composable
fun EnhancedChatMessagesAreaOld(
    messages: List<ChatMessage>,
    selectedAgent: Agent,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
    isVoiceMode: Boolean,
    isDarkTheme: Boolean,
    onFeatureClick: (Any) -> Unit,
    scrollState: LazyListState
) {
    // Use the passed scrollState instead of creating a new one
    // Remove: val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty() || isThinking) {
            delay(100)
            scrollState.animateScrollToItem(
                index = if (isThinking) messages.size else messages.size - 1,
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = scrollState, // Use the passed scrollState here
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            top = 80.dp, // Add top padding for sticky header
            bottom = 16.dp
        )
    ) {
        // Welcome message
        if (messages.isEmpty() && !isThinking) {
            item {
                GuideWelcomeCard(
                    agent = selectedAgent,
                    isDarkTheme = isDarkTheme
                )
            }
        }

        // Chat messages with staggered animation
        items(
            items = messages,
            key = { it.id } // Use unique ID instead of timestamp
        ) { message ->
            GuideMessagesAnimated(
                message = message,
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme
            )
        }

        // Thinking indicator
        if (isThinking) {
            item {
                GuideThinkingIndicator(
                    selectedAgent = selectedAgent,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun GuideWelcomeCard(
    agent: Agent,
    isDarkTheme: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "welcomeAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .scale(animatedAlpha),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFF2A2A3E).copy(alpha = 0.7f)
            } else {
                Color.White.copy(alpha = 0.9f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            agent.primaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Agent avatar with glow effect
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glow background
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        agent.primaryColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(agent.primaryColor, agent.secondaryColor)
                                ),
                                shape = CircleShape
                            )
                            .shadow(12.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.general_icon),
                            contentDescription = agent.name,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hi! I'm ${agent.name}",
                    color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = agent.description,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF4A5568),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick start suggestions
                Text(
                    text = "How can I help you today?",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF718096),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GuideMessagesAnimated(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean
) {
    // Removed animations for better performance and stability

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
        ) {


            // Message bubble
            MessageBubbleEnhanced(
                message = message,
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme,
                isFromUser = message.isFromUser
            )

            if (message.isFromUser) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
}

@Composable
fun MessageBubbleEnhanced(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    isFromUser: Boolean
) {
    var showThinking by remember { mutableStateOf(false) }
    val messageContent = remember(message.text) {
        parseMessageWithThinking(message.text)
    }

    Column(
        modifier = Modifier.widthIn(max = if (isFromUser) 280.dp else 600.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isFromUser) 20.dp else 0.dp,
                topEnd = if (isFromUser) 20.dp else 0.dp,
                bottomStart = if (isFromUser) 20.dp else 0.dp,
                bottomEnd = if (isFromUser) 4.dp else 0.dp
            ),

            color = if (isFromUser) {
                if (isDarkTheme) Color(0xFF2A2A3E).copy(alpha = 0.7f) else Color.White

            } else {
//                if (isDarkTheme) Color(0xFF2A2A3E) else Color.White
                Color.Transparent
            },
            shadowElevation = if (isFromUser) 6.dp else 0.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(if (isFromUser) 20.dp else 0.dp))

        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (!isFromUser) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (!isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        }
                        else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (!isDarkTheme) selectedAgent.primaryColor.copy(alpha = 0.03f) else selectedAgent.primaryColor.copy(alpha = 0.01f),
                                    Color.Transparent
                                )
                            )
                        }
                    )

            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Main message text
                    FormattedText(
                        text = messageContent.mainContent,
                        isFromUser = isFromUser,
                        isDarkTheme = isDarkTheme
                    )

                    // Thinking steps (if present)
                    if (messageContent.thinkingSteps != null && !isFromUser) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            onClick = { showThinking = !showThinking },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "Thinking",
                                        tint = if (isFromUser) Color.White.copy(alpha = 0.8f)
                                        else if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                                        else Color(0xFF718096),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Thinking process",
                                        color = if (isFromUser) Color.White.copy(alpha = 0.8f)
                                        else if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                                        else Color(0xFF718096),
                                        fontSize = 12.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showThinking) "Collapse" else "Expand",
                                    tint = if (isFromUser) Color.White.copy(alpha = 0.6f)
                                    else if (isDarkTheme) Color.White.copy(alpha = 0.4f)
                                    else Color(0xFF718096),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (showThinking) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = messageContent.thinkingSteps,
                                    color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF4A5568),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (message.isVoiceMessage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Voice Message",
                                tint = if (isFromUser) Color.White.copy(alpha = 0.7f)
                                else if (isDarkTheme) Color.White.copy(alpha = 0.5f)
                                else Color(0xFF718096),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice",
                                color = if (isFromUser) Color.White.copy(alpha = 0.7f)
                                else if (isDarkTheme) Color.White.copy(alpha = 0.5f)
                                else Color(0xFF718096),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Timestamp
        Text(
            text = message.timestamp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFF718096),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 12.dp)
        )
    }
}

@Composable
fun GuideThinkingIndicator(
    selectedAgent: Agent,
    isDarkTheme: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Subtle enterprise-grade thinking animation
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, delayMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {


        Spacer(modifier = Modifier.width(8.dp))

        // Thinking bubble
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF2A2A3E) else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 120.dp)

        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()

                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subtle pulsing icon
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Thinking",
                        tint = selectedAgent.primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .scale(pulseScale)
                    )

                    // Animated dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = selectedAgent.primaryColor.copy(alpha = dot1Alpha),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = selectedAgent.primaryColor.copy(alpha = dot2Alpha),
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = selectedAgent.primaryColor.copy(alpha = dot3Alpha),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentAvatarOld(
    selectedAgent: Agent,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        selectedAgent.primaryColor.copy(alpha = 0.3f),
                        selectedAgent.secondaryColor.copy(alpha = 0.3f)
                    )
                ),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        selectedAgent.primaryColor.copy(alpha = 0.5f),
                        selectedAgent.secondaryColor.copy(alpha = 0.5f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.general_icon),
            contentDescription = selectedAgent.name,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun UserAvatarOld(isDarkTheme: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF4A5568),
                            Color(0xFF2D3748)
                        )
                    } else {
                        listOf(
                            Color(0xFFE2E8F0),
                            Color(0xFFCBD5E0)
                        )
                    }
                ),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.user_icon),
            contentDescription = "User",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun FormattedText(
    text: String,
    isFromUser: Boolean,
    isDarkTheme: Boolean
) {
    Text(
        text = text,
        color = if (isFromUser) {
            if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
        } else {
            if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
        },
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
}

// Data class for parsed message content
data class ParsedMessage(
    val mainContent: String,
    val thinkingSteps: String? = null
)

// Helper function to parse message with thinking steps
fun parseMessageWithThinking(text: String): ParsedMessage {
    // Check if the message contains thinking steps (indicated by specific markers)
    // This is a simple implementation - adjust based on your API response format
    val thinkingMarker = "<thinking>"
    val thinkingEndMarker = "</thinking>"

    return if (text.contains(thinkingMarker) && text.contains(thinkingEndMarker)) {
        val startIndex = text.indexOf(thinkingMarker)
        val endIndex = text.indexOf(thinkingEndMarker)

        val mainContent = text.substring(0, startIndex).trim() +
                text.substring(endIndex + thinkingEndMarker.length).trim()
        val thinkingContent = text.substring(
            startIndex + thinkingMarker.length,
            endIndex
        ).trim()

        ParsedMessage(mainContent, thinkingContent)
    } else {
        ParsedMessage(text, null)
    }
}