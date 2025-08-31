package com.craftflowtechnologies.guidelens.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.craftflowtechnologies.guidelens.cooking.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer



@Composable
fun EnhancedChatMessagesArea(
    messages: List<ChatMessage>,
    selectedAgent: Agent,
    isThinking: Boolean,
    modifier: Modifier = Modifier,
    isVoiceMode: Boolean,
    isDarkTheme: Boolean,
    onFeatureClick: (Any) -> Unit,
    scrollState: LazyListState,
    onMessageInteraction: (String, String) -> Unit = { _, _ -> },
    onRegenerateMessage: (String) -> Unit = {},
    cookingSessionManager: CookingSessionManager? = null,
    onStartCookingSession: (Recipe) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    // Improved scroll-to-bottom detection
    val isScrolledToBottom by remember {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) return@derivedStateOf true

            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()

            // Check if we're at the bottom
            lastVisibleItem?.index == layoutInfo.totalItemsCount - 1 &&
                    lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset + 50 // 50px tolerance
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            ChatToolsSection(
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme,
                onToolClick = onFeatureClick
            )

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(
                    top = 80.dp,
                    bottom = 16.dp
                )
            ) {
                // Welcome message
                if (messages.isEmpty() && !isThinking) {
                    item {
                        WelcomeCard(
                            agent = selectedAgent,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                // Group messages by date and add date separators
                val groupedMessages = groupMessagesByDate(messages)

                groupedMessages.forEach { (date, messagesForDate) ->
                    // Date separator
                    item(key = "date_$date") {
                        DateSeparator(
                            date = date,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // Messages for this date
                    items(
                        items = messagesForDate,
                        key = { it.id }
                    ) { message ->
                        AnimatedMessage( // Changed from AnimatedMessage to remove animations
                            message = message,
                            selectedAgent = selectedAgent,
                            isDarkTheme = isDarkTheme,
                            onMessageInteraction = onMessageInteraction,
                            onRegenerateMessage = onRegenerateMessage,
                            cookingSessionManager = cookingSessionManager,
                            onStartCookingSession = onStartCookingSession
                        )
                    }
                }

                // Thinking indicator
                if (isThinking) {
                    item {
                        ThinkingIndicator(
                            selectedAgent = selectedAgent,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                // Add an extra item at the end for better scrolling
                item(key = "bottom_padding") {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Subtle scroll to bottom button
        if (!isScrolledToBottom && messages.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp) // Position above input field
            ) {
                // Subtle background for better visibility
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0xCC1E1E1E) else Color(0xCCFFFFFF),
                    modifier = Modifier.size(40.dp),
                    contentColor = selectedAgent.primaryColor,
                    onClick = {
                        coroutineScope.launch {
                            // Scroll to the actual bottom
                            scrollState.animateScrollToItem(
                                index = scrollState.layoutInfo.totalItemsCount - 1,
                                scrollOffset = 0
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(8.dp),
                        tint = selectedAgent.primaryColor
                    )
                }
            }
        }
    }
}

// Alternative: Even more minimal version
@Composable
fun MinimalScrollToBottomButton(
    isVisible: Boolean,
    onClick: () -> Unit,
    agentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Box(
            modifier = modifier
                .size(36.dp)
                .background(
                    color = if (isDarkTheme) Color(0xCC1E1E1E) else Color(0xCCFFFFFF),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isDarkTheme) Color(0x26FFFFFF) else Color(0x0D000000),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Scroll to bottom",
                modifier = Modifier.size(18.dp),
                tint = agentColor
            )
        }
    }
}

@Composable
fun DateSeparator(
    date: String,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) {
                Color(0xFF2A2A3E).copy(alpha = 0.6f)
            } else {
                Color.White.copy(alpha = 0.9f)
            },
            shadowElevation = 2.dp
        ) {
            Text(
                text = formatDateForSeparator(date),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF4A5568),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun AnimatedMessage(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onMessageInteraction: (String, String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    cookingSessionManager: CookingSessionManager? = null,
    onStartCookingSession: (Recipe) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        EnhancedMessageBubble(
            message = message,
            selectedAgent = selectedAgent,
            isDarkTheme = isDarkTheme,
            isFromUser = message.isFromUser,
            shouldStartTyping = !message.isFromUser,
            onMessageInteraction = onMessageInteraction,
            onRegenerateMessage = onRegenerateMessage,
            cookingSessionManager = cookingSessionManager,
            onStartCookingSession = onStartCookingSession
        )
    }
}

// Alternative: Minimal Animation Version
@Composable
fun MinimalAnimatedMessage(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onMessageInteraction: (String, String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    cookingSessionManager: CookingSessionManager? = null,
    onStartCookingSession: (Recipe) -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message.id) {
        delay(if (message.isFromUser) 0 else 50) // Tiny delay for AI messages
        isVisible = true
    }

    // Simple fade animation only
//    val alpha by animateFloatAsState(
//        targetValue = if (isVisible) 1f else 0f,
//        animationSpec = tween(
//            durationMillis = 200,
//            easing = LinearOutSlowInEasing
//        ),
//        label = "minimalAlpha"
//    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearOutSlowInEasing
        ),
        label = "minimalAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .graphicsLayer { alpha = animatedAlpha },
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        EnhancedMessageBubble(
            message = message,
            selectedAgent = selectedAgent,
            isDarkTheme = isDarkTheme,
            isFromUser = message.isFromUser,
            shouldStartTyping = isVisible && !message.isFromUser,
            onMessageInteraction = onMessageInteraction,
            onRegenerateMessage = onRegenerateMessage,
            cookingSessionManager = cookingSessionManager,
            onStartCookingSession = onStartCookingSession
        )
    }
}

// Alternative: No Animation Version (Cleanest)
@Composable
fun StaticMessage(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onMessageInteraction: (String, String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    cookingSessionManager: CookingSessionManager? = null,
    onStartCookingSession: (Recipe) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        EnhancedMessageBubble(
            message = message,
            selectedAgent = selectedAgent,
            isDarkTheme = isDarkTheme,
            isFromUser = message.isFromUser,
            shouldStartTyping = true,
            onMessageInteraction = onMessageInteraction,
            onRegenerateMessage = onRegenerateMessage,
            cookingSessionManager = cookingSessionManager,
            onStartCookingSession = onStartCookingSession
        )
    }
}

@Composable
fun EnhancedMessageBubble(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    isFromUser: Boolean,
    shouldStartTyping: Boolean = true,
    onMessageInteraction: (String, String) -> Unit,
    onRegenerateMessage: (String) -> Unit,
    cookingSessionManager: CookingSessionManager? = null,
    onStartCookingSession: (Recipe) -> Unit = {}
) {
    val context = LocalContext.current
    var showThinking by remember { mutableStateOf(false) }
    val messageContent = remember(message.text) {
        parseEnhancedMessage(message.text)
    }

    // Smooth shadow animation
    val shadowElevation by animateDpAsState(
        targetValue = if (isFromUser) 2.dp else 4.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "shadowAnimation"
    )

    Column(
        modifier = Modifier.widthIn(max = if (isFromUser) 300.dp else 600.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isFromUser) 20.dp else 4.dp,
                bottomEnd = if (isFromUser) 4.dp else 20.dp
            ),
            color = if (isFromUser) {
                if (isDarkTheme) selectedAgent.primaryColor.copy(alpha = 0.1f)
                else Color(0xFFCBDAE7)
            } else {
                if (isDarkTheme) Color(0xFF2A2A3E).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.95f)
            },
//            shadowElevation = shadowElevation,
            modifier = Modifier.clip(RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Image attachments with smooth fade-in
                if (message.images.isNotEmpty() || message.generatedImage != null) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300))
                    ) {
                        ImageGrid(
                            images = message.images,
                            generatedImage = message.generatedImage,
                            isDarkTheme = isDarkTheme,
                            agentColor = selectedAgent.primaryColor
                        )
                    }
                    if (message.text.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Main message content
                if (message.text.isNotEmpty()) {
                    SelectionContainer {
                        if (!isFromUser && shouldStartTyping) {
//                            SmoothTypewriterText(
//                                content = messageContent,
//                                generatedImage = message.generatedImage,
//                                isFromUser = isFromUser,
//                                isDarkTheme = isDarkTheme,
//                                agentColor = selectedAgent.primaryColor
//                            )
                            EnhancedFormattedText(
                                content = messageContent,
                                generatedImage = message.generatedImage,
                                isFromUser = isFromUser,
                                isDarkTheme = isDarkTheme,
                                agentColor = selectedAgent.primaryColor
                            )
                        } else {
                            EnhancedFormattedText(
                                content = messageContent,
                                generatedImage = message.generatedImage,
                                isFromUser = isFromUser,
                                isDarkTheme = isDarkTheme,
                                agentColor = selectedAgent.primaryColor
                            )
                        }
                    }
                } else if (!isFromUser && (message.images.isNotEmpty() || message.generatedImage != null)) {
                    Text(
                        text = "Unable to generate response for the provided images.",
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 14.sp
                    )
                }

                // Thinking steps with smooth animation
                if (messageContent.thinkingSteps != null && !isFromUser) {
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            ThinkingSection(
                                thinkingSteps = messageContent.thinkingSteps,
                                showThinking = showThinking,
                                onToggle = { showThinking = !showThinking },
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }

        // Message interaction buttons with staggered animation
        if (!isFromUser) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                MessageInteractionButtons(
                    message = message,
                    selectedAgent = selectedAgent,
                    isDarkTheme = isDarkTheme,
                    onCopy = {
                        copyToClipboard(context, message.text)
                        onMessageInteraction(message.id, "copy")
                    },
                    onLike = { onMessageInteraction(message.id, "like") },
                    onDislike = { onMessageInteraction(message.id, "dislike") },
                    onRegenerate = { onRegenerateMessage(message.id) }
                )

                // Cooking Tools
                if (selectedAgent.id == "cooking" && !isFromUser) {
                    val parsedRecipe = RecipeParser.parseRecipeFromText(message.text)
                    if (parsedRecipe != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CookingToolsCard(
                            recipe = parsedRecipe,
                            isDarkTheme = isDarkTheme,
                            cookingSessionManager = cookingSessionManager,
                            onStartInteractiveCooking = onStartCookingSession
                        )
                    }
                }
            }
        }

        // Timestamp with fade-in
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = if (isFromUser) 0 else 400
                )
            )
        ) {
            Text(
                text = message.timestamp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color(0xFF718096),
                fontSize = 11.sp,
                modifier = Modifier.padding(
                    top = 4.dp,
                    start = if (isFromUser) 0.dp else 8.dp,
                    end = if (isFromUser) 8.dp else 0.dp
                )
            )
        }
    }
}

// Smooth typewriter effect component
@Composable
fun SmoothTypewriterText(
    content: EnhancedMessageContent,
    generatedImage: String?,
    isFromUser: Boolean,
    isDarkTheme: Boolean,
    agentColor: Color
) {
    var displayedText by remember { mutableStateOf("") }

    // Extract full text from list of ContentElements (e.g., by joining all Text elements)
    val fullText = content.mainContent
        .filterIsInstance<ContentElement.Text>()
        .joinToString(" ") { it.text }

    LaunchedEffect(fullText) {
        displayedText = ""
        val words = fullText.split(" ")
        val totalDuration = minOf(2000L, fullText.length * 15L)
        val delayPerWord = if (words.isNotEmpty()) totalDuration / words.size else 0L

        words.forEachIndexed { index, _ ->
            displayedText = words.take(index + 1).joinToString(" ")
            delay(delayPerWord)
        }
    }

    Crossfade(
        targetState = displayedText,
        animationSpec = tween(100),
        label = "textCrossfade"
    ) { text ->
        EnhancedFormattedText(
            content = content.copy(mainContent = listOf(ContentElement.Text(text))), // ✅ FIXED
            generatedImage = generatedImage,
            isFromUser = isFromUser,
            isDarkTheme = isDarkTheme,
            agentColor = agentColor
        )
    }
}

// Animation state enum
enum class AnimationState {
    HIDDEN,
    ENTERING,
    VISIBLE
}


@Composable
fun MessageInteractionButtons(
    message: ChatMessage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onCopy: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onRegenerate: () -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Copy button
        InteractionButton(
            icon = Icons.Default.ContentCopy,
            isActive = message.userInteractions.isCopied,
            onClick = onCopy,
            contentDescription = "Copy",
            isDarkTheme = isDarkTheme,
            activeColor = selectedAgent.primaryColor
        )

        // Like button
        InteractionButton(
            icon = Icons.Default.ThumbUp,
            isActive = message.userInteractions.isLiked,
            onClick = {
                Log.d("MessageInteractions", "Like button clicked - current state: ${message.userInteractions.isLiked}")
                onLike()
            },
            contentDescription = "Like",
            isDarkTheme = isDarkTheme,
            activeColor = Color(0xFF10B981)
        )

        // Dislike button
        InteractionButton(
            icon = Icons.Default.ThumbDown,
            isActive = message.userInteractions.isDisliked,
            onClick = {
                Log.d("MessageInteractions", "Dislike button clicked - current state: ${message.userInteractions.isDisliked}")
                onDislike()
            },
            contentDescription = "Dislike",
            isDarkTheme = isDarkTheme,
            activeColor = Color(0xFFEF4444)
        )

        // Regenerate button - commented out for now
        // InteractionButton(
        //     icon = Icons.Default.Refresh,
        //     isActive = false,
        //     onClick = onRegenerate,
        //     contentDescription = "Regenerate",
        //     isDarkTheme = isDarkTheme,
        //     activeColor = selectedAgent.secondaryColor,
        //     showBadge = message.userInteractions.regenerationCount > 0,
        //     badgeText = message.userInteractions.regenerationCount.toString()
        // )
    }
}

@Composable
fun InteractionButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    isDarkTheme: Boolean,
    activeColor: Color,
    showBadge: Boolean = false,
    badgeText: String = ""
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    // Animated properties with smooth transitions
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.85f
            isActive -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = { isPressed = false }
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.4f)  // Increased alpha for better visibility
                     else if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.6f)
                     else Color(0xFFF3F4F6),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val animatedIconColor by animateColorAsState(
        targetValue = if (isActive) {
            // Make active icons brighter and more saturated
            when (activeColor) {
                Color(0xFF10B981) -> Color(0xFF059669) // Darker green for like
                Color(0xFFEF4444) -> Color(0xFFDC2626) // Darker red for dislike  
                else -> activeColor
            }
        } else if (isDarkTheme) Color.White.copy(alpha = 0.6f)
        else Color(0xFF6B7280),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isActive) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    Box {
        Surface(
            onClick = {
                isPressed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Log.d("InteractionButton", "Button clicked - description: $contentDescription, isActive: $isActive")
                onClick()
            },
            shape = CircleShape,
            color = animatedBackgroundColor,
            shadowElevation = animatedElevation,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Add a subtle glow effect when active
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        activeColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                    radius = 20f
                                ),
                                shape = CircleShape
                            )
                    )
                }
                
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = animatedIconColor,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            // Add subtle rotation animation for active state
                            if (isActive && (contentDescription == "Like" || contentDescription == "Dislike")) {
                                rotationZ = if (contentDescription == "Like") 10f else -10f
                            }
                        }
                )
            }
        }
        
        // Badge for regeneration count
        if (showBadge && badgeText.isNotEmpty()) {
            Surface(
                shape = CircleShape,
                color = activeColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

@Composable
fun ImageGrid(
    images: List<String>,
    generatedImage: String?,
    isDarkTheme: Boolean,
    agentColor: Color
) {
    val allImages = buildList {
        addAll(images)
        if (generatedImage != null) add(generatedImage)
    }

    when (allImages.size) {
        1 -> {
            SingleImageDisplay(allImages[0], isDarkTheme, agentColor)
        }
        2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allImages.forEach { image ->
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(image, isDarkTheme, agentColor)
                    }
                }
            }
        }
        3 -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleImageDisplay(allImages[0], isDarkTheme, agentColor)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(allImages[1], isDarkTheme, agentColor)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(allImages[2], isDarkTheme, agentColor)
                    }
                }
            }
        }
        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(allImages[0], isDarkTheme, agentColor)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(allImages[1], isDarkTheme, agentColor)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SingleImageDisplay(allImages[2], isDarkTheme, agentColor)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (allImages.size > 3) {
                            SingleImageDisplay(allImages[3], isDarkTheme, agentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SingleImageDisplay(
    imageData: String,
    isDarkTheme: Boolean,
    agentColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF374151) else Color(0xFFF3F4F6),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
    ) {
        URLImageDisplay(
            imageSource = imageData,
            contentDescription = "Generated image",
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { saveImageToGallery(context, imageData, "Image") },
                    modifier = Modifier
                        .size(32.dp)
                        .background(agentColor.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save to Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(agentColor.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand Image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        ZoomableImageDialog(
            imageData = imageData,
            description = "Image",
            isDarkTheme = isDarkTheme,
            onDismiss = { showDialog = false }
        )
    }
}

// Helper functions
fun groupMessagesByDate(messages: List<ChatMessage>): Map<String, List<ChatMessage>> {
    return messages.groupBy { message ->
        val fullDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(message.fullTimestamp)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(fullDate ?: Date())
    }
}

fun formatDateForSeparator(dateString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val messageDate = Calendar.getInstance().apply { time = date ?: Date() }
        
        when {
            isSameDay(messageDate, today) -> "Today"
            isSameDay(messageDate, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
        }
    } catch (e: Exception) {
        dateString
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Message", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

// Include all other components from the original file with enhancements
@Composable
fun WelcomeCard(
    agent: Agent,
    isDarkTheme: Boolean
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
fun ThinkingIndicator(
    selectedAgent: Agent,
    isDarkTheme: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Dot animations with smoother timing
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.8f at 0 with FastOutSlowInEasing
                1.2f at 400 with FastOutSlowInEasing
                0.8f at 800 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.8f at 200 with FastOutSlowInEasing
                1.2f at 600 with FastOutSlowInEasing
                0.8f at 1000 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.8f at 400 with FastOutSlowInEasing
                1.2f at 800 with FastOutSlowInEasing
                0.8f at 1200 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    // Background pulse animation
    val backgroundAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backgroundPulse"
    )

    // Subtle glow effect
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // Premium agent avatar with subtle glow
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = selectedAgent.primaryColor.copy(alpha = glowIntensity),
                    spotColor = selectedAgent.primaryColor.copy(alpha = glowIntensity * 0.7f)
                )
        ) {
            AgentAvatarIndicator(selectedAgent = selectedAgent, isDarkTheme = isDarkTheme)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Premium thinking bubble with glass morphism effect
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDarkTheme) {
                Color(0x4D2A2A3E)
            } else {
                Color(0x4DF7F7F9)
            },
            modifier = Modifier
                .widthIn(min = 140.dp, max = 200.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                },
            border = BorderStroke(
                1.dp,
                selectedAgent.primaryColor.copy(alpha = 0.15f + glowIntensity * 0.2f)
            ),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                selectedAgent.primaryColor.copy(alpha = backgroundAlpha),
                                Color.Transparent
                            ),
                            center = Offset.Zero,
                            radius = 200f
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated AI icon with subtle rotation
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Thinking",
                        tint = selectedAgent.primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = glowIntensity * 15f // Subtle wobble
                            }
                    )

                    // Premium animated dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dot 1
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer {
                                    scaleX = dot1Scale
                                    scaleY = dot1Scale
                                }
                                .background(
                                    color = selectedAgent.primaryColor,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = selectedAgent.primaryColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )

                        // Dot 2
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer {
                                    scaleX = dot2Scale
                                    scaleY = dot2Scale
                                }
                                .background(
                                    color = selectedAgent.primaryColor,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = selectedAgent.primaryColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )

                        // Dot 3
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer {
                                    scaleX = dot3Scale
                                    scaleY = dot3Scale
                                }
                                .background(
                                    color = selectedAgent.primaryColor,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = selectedAgent.primaryColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

// Premium version of AgentAvatar to match the new design
@Composable
fun AgentAvatarIndicator(selectedAgent: Agent, isDarkTheme: Boolean) {
    Surface(
        shape = CircleShape,
        color = selectedAgent.primaryColor.copy(alpha = 0.1f),
        border = BorderStroke(1.5.dp, selectedAgent.primaryColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = selectedAgent.icon,
                contentDescription = selectedAgent.name,
                tint = selectedAgent.primaryColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AgentAvatar(
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
fun UserAvatar(isDarkTheme: Boolean) {
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

//data class EnhancedMessageContent(
//    val mainContent: String,
//    val thinkingSteps: String? = null,
//    val actions: List<Pair<String, String>> = emptyList(), // Action label, action text
//    val tips: List<String> = emptyList()
//)

fun parseEnhancedMessage(text: String): EnhancedMessageContent {
    val mainContent = mutableListOf<ContentElement>()
    var thinkingSteps: String? = null
    val actions = mutableListOf<Pair<String, String>>()
    val tips = mutableListOf<String>()
    val lines = text.split("\n").map { it.trim() }

    var currentText = StringBuilder()
    var inCodeBlock = false
    var codeBlockContent = StringBuilder()

    lines.forEach { line ->
        when {
            // Thinking steps
            line.matches(Regex("\\[Thinking:(.*?)\\]", RegexOption.DOT_MATCHES_ALL)) -> {
                thinkingSteps = Regex("\\[Thinking:(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
                    .find(line)?.groupValues?.get(1)?.trim()
            }
            // Actions
            line.matches(Regex("\\[Action:([^\\]]*?)\\]")) -> {
                val actionText = Regex("\\[Action:([^\\]]*?)\\]").find(line)?.groupValues?.get(1)?.trim()
                if (actionText != null) {
                    actions.add(Pair(actionText, actionText))
                }
            }
            // Tips
            line.matches(Regex("\\[Tip:([^\\]]*?)\\]")) -> {
                val tipText = Regex("\\[Tip:([^\\]]*?)\\]").find(line)?.groupValues?.get(1)?.trim()
                if (tipText != null) {
                    tips.add(tipText)
                }
            }
            // Code block
            line.startsWith("```") -> {
                if (inCodeBlock) {
                    mainContent.add(ContentElement.CodeBlock(codeBlockContent.toString().trim()))
                    codeBlockContent = StringBuilder()
                    inCodeBlock = false
                } else {
                    if (currentText.isNotEmpty()) {
                        mainContent.add(ContentElement.Text(currentText.toString().trim()))
                        currentText = StringBuilder()
                    }
                    inCodeBlock = true
                }
            }
            inCodeBlock -> {
                codeBlockContent.append(line).append("\n")
            }
            // Headings
            line.startsWith("# ") -> {
                if (currentText.isNotEmpty()) {
                    mainContent.add(ContentElement.Text(currentText.toString().trim()))
                    currentText = StringBuilder()
                }
                mainContent.add(ContentElement.Heading(line.removePrefix("# ").trim(), 1))
            }
            line.startsWith("## ") -> {
                if (currentText.isNotEmpty()) {
                    mainContent.add(ContentElement.Text(currentText.toString().trim()))
                    currentText = StringBuilder()
                }
                mainContent.add(ContentElement.Heading(line.removePrefix("## ").trim(), 2))
            }
            // Blockquote
            line.startsWith("> ") -> {
                if (currentText.isNotEmpty()) {
                    mainContent.add(ContentElement.Text(currentText.toString().trim()))
                    currentText = StringBuilder()
                }
                mainContent.add(ContentElement.Blockquote(line.removePrefix("> ").trim()))
            }
            // Ordered list
            line.matches(Regex("\\d+\\.\\s.*")) -> {
                if (currentText.isNotEmpty()) {
                    mainContent.add(ContentElement.Text(currentText.toString().trim()))
                    currentText = StringBuilder()
                }
                mainContent.add(ContentElement.ListItem(line.replace(Regex("\\d+\\.\\s"), ""), true, 0))
            }
            // Unordered list
            line.startsWith("- ") || line.startsWith("* ") -> {
                if (currentText.isNotEmpty()) {
                    mainContent.add(ContentElement.Text(currentText.toString().trim()))
                    currentText = StringBuilder()
                }
                mainContent.add(ContentElement.ListItem(line.removePrefix("- ").removePrefix("* ").trim(), false, 0))
            }
            // Regular text
            else -> {
                currentText.append(line).append("\n")
            }
        }
    }

    if (currentText.isNotEmpty()) {
        mainContent.add(ContentElement.Text(currentText.toString().trim()))
    }

    return EnhancedMessageContent(
        mainContent = mainContent,
        thinkingSteps = thinkingSteps,
        actions = actions,
        tips = tips
    )
}

@Composable
fun TypewriterText(
    content: EnhancedMessageContent,
    generatedImage: String?,
    isFromUser: Boolean,
    isDarkTheme: Boolean,
    agentColor: Color,
    onActionClick: (String) -> Unit = {}
) {
    val fullText = content.mainContent.filterIsInstance<ContentElement.Text>()
        .joinToString("\n") { it.text }
    
    var displayedText by remember { mutableStateOf("") }
    var isTypingComplete by remember { mutableStateOf(false) }
    
    LaunchedEffect(content) {
        displayedText = ""
        isTypingComplete = false
        
        // Typing animation
        val typingSpeed = 20L // milliseconds per character
        fullText.forEachIndexed { index, char ->
            delay(typingSpeed)
            displayedText = fullText.substring(0, index + 1)
        }
        isTypingComplete = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Animated typing text
        AnimatedVisibility(
            visible = displayedText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300))
        ) {
            MarkdownText(
                text = displayedText,
                isDarkTheme = isDarkTheme,
                isFromUser = isFromUser,
                agentColor = agentColor
            )
        }
        
        // Show cursor while typing
        if (!isTypingComplete && displayedText.isNotEmpty()) {
            Row {
                Text(
                    text = "|",
                    color = agentColor,
                    fontSize = 15.sp,
                    modifier = Modifier.animateContentSize()
                )
            }
        }
        
        // Show full content with enhanced formatting once typing is complete
        if (isTypingComplete) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { it / 8 },
                            animationSpec = tween(500, delayMillis = 200)
                        )
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Display other content elements (headings, lists, etc.)
                    content.mainContent.filter { it !is ContentElement.Text }.forEach { element ->
                        when (element) {
                            is ContentElement.Heading -> {
                                Text(
                                    text = element.text,
                                    color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                                    fontSize = if (element.level == 1) 20.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(agentColor.copy(alpha = 0.2f), Color.Transparent)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            is ContentElement.ListItem -> {
                                Row(
                                    modifier = Modifier.padding(start = (element.indentLevel * 16).dp)
                                ) {
                                    Text(
                                        text = if (element.isOrdered) "${element.indentLevel + 1}. " else "• ",
                                        color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                                        fontSize = 15.sp
                                    )
                                    MarkdownText(
                                        text = element.text,
                                        isDarkTheme = isDarkTheme,
                                        isFromUser = isFromUser,
                                        agentColor = agentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            is ContentElement.Blockquote -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFF3F4F6),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = agentColor.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = element.text,
                                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF4A5568),
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is ContentElement.CodeBlock -> {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = element.text,
                                        color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF2D2D2D),
                                        fontSize = 13.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    // Generated Image
                    if (content.mainContent.any { it is ContentElement.Text && it.text.contains("[Image:") }) {
                        if (generatedImage != null) {
                            GeneratedImageDisplay(
                                imageData = generatedImage,
                                description = content.mainContent
                                    .filterIsInstance<ContentElement.Text>()
                                    .firstOrNull { it.text.contains("[Image:") }
                                    ?.let { Regex("\\[Image:([^\\]]*?)\\]").find(it.text)?.groupValues?.get(1)?.trim() }
                                    ?: "Generated Image",
                                isDarkTheme = isDarkTheme,
                                agentColor = agentColor
                            )
                        }
                    }
                    
                    // Tips in expandable sections
                    content.tips.forEachIndexed { index, tip ->
                        var expanded by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = agentColor.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = "Tip",
                                            tint = agentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tip ${index + 1}",
                                            color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Text(
                                        text = tip,
                                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF4A5568),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Actions as interactive buttons
                    content.actions.forEachIndexed { index, (label, action) ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300, delayMillis = index * 100)) +
                                    slideInVertically(initialOffsetY = { it / 2 })
                        ) {
                            Button(
                                onClick = { onActionClick(action) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = agentColor,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedFormattedText(
    content: EnhancedMessageContent,
    generatedImage: String?, // Base64-encoded image
    isFromUser: Boolean,
    isDarkTheme: Boolean,
    agentColor: Color,
    onActionClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Render main content
        content.mainContent.forEach { element ->
            when (element) {
                is ContentElement.Text -> {
                    MarkdownText(
                        text = element.text,
                        isDarkTheme = isDarkTheme,
                        isFromUser = isFromUser,
                        agentColor = agentColor
                    )
                }
                is ContentElement.Heading -> {
                    Text(
                        text = element.text,
                        color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                        fontSize = if (element.level == 1) 20.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(agentColor.copy(alpha = 0.2f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                is ContentElement.ListItem -> {
                    Row(
                        modifier = Modifier.padding(start = (element.indentLevel * 16).dp)
                    ) {
                        Text(
                            text = if (element.isOrdered) "${element.indentLevel + 1}. " else "• ",
                            color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                            fontSize = 15.sp
                        )
                        MarkdownText(
                            text = element.text,
                            isDarkTheme = isDarkTheme,
                            isFromUser = isFromUser,
                            agentColor = agentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is ContentElement.Blockquote -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFF3F4F6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = agentColor.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = element.text,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF4A5568),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                is ContentElement.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = element.text,
                            color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF2D2D2D),
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Generated Image
        if (content.mainContent.any { it is ContentElement.Text && it.text.contains("[Image:") }) {
            if (generatedImage != null) {
                GeneratedImageDisplay(
                    imageData = generatedImage,
                    description = content.mainContent
                        .filterIsInstance<ContentElement.Text>()
                        .firstOrNull { it.text.contains("[Image:") }
                        ?.let { Regex("\\[Image:([^\\]]*?)\\]").find(it.text)?.groupValues?.get(1)?.trim() }
                        ?: "Generated Image",
                    isDarkTheme = isDarkTheme,
                    agentColor = agentColor
                )
            } else {
                Text(
                    text = "Image generation unavailable for this recipe.",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Tips in expandable sections
        content.tips.forEachIndexed { index, tip ->
            var expanded by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = agentColor.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Tip",
                                tint = agentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tip ${index + 1}",
                                color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = tip,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF4A5568),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Actions as interactive buttons
        content.actions.forEachIndexed { index, (label, action) ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300, delayMillis = index * 100)) +
                        slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Button(
                    onClick = { onActionClick(action) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = agentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedImageDisplay(
    imageData: String,
    description: String,
    isDarkTheme: Boolean,
    agentColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFF3F4F6),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            URLImageDisplay(
                imageSource = imageData,
                contentDescription = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4 / 3f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        saveImageToGallery(context, imageData, description)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = agentColor.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save to Gallery",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 12.sp)
                }
                Button(
                    onClick = { showDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = agentColor.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand Image",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Expand", fontSize = 12.sp)
                }
            }
        }
    }

    if (showDialog) {
        ZoomableImageDialog(
            imageData = imageData,
            description = description,
            isDarkTheme = isDarkTheme,
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun ZoomableImageDialog(
    imageData: String,
    description: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                URLImageDisplay(
                    imageSource = imageData,
                    contentDescription = description,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .offset(x = offsetX.dp, y = offsetY.dp),
                    contentScale = ContentScale.Fit
                )
//                } catch (e: Exception) {
//                    Icon(
//                        imageVector = Icons.Default.Image,
//                        contentDescription = "Failed to load image",
//                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF9CA3AF),
//                        modifier = Modifier.size(64.dp)
//                    )
//                }
            }
        }
    }
}

fun saveImageToGallery(context: Context, imageData: String, description: String) {
    try {
        val decodedBytes = android.util.Base64.decode(imageData, android.util.Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        if (bitmap != null) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${description}_${UUID.randomUUID()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                //SnackbarHostState().showSnackbar("Image saved to gallery")
            } ?: throw Exception("Failed to create media store entry")
        } else {
            throw Exception("Invalid image data")
        }
    } catch (e: Exception) {
        //SnackbarHostState().showSnackbar("Failed to save image: ${e.message}")
    }
}

@Composable
fun MarkdownText(
    text: String,
    isDarkTheme: Boolean,
    isFromUser: Boolean,
    agentColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
        val italicRegex = Regex("\\*(.+?)\\*")
        val inlineCodeRegex = Regex("`(.+?)`")

        while (currentIndex < text.length) {
            // Find the next Markdown element
            val boldMatch = boldRegex.find(text, currentIndex)
            val italicMatch = italicRegex.find(text, currentIndex)
            val codeMatch = inlineCodeRegex.find(text, currentIndex)

            val nextMatch = listOfNotNull(boldMatch, italicMatch, codeMatch)
                .minByOrNull { it.range.first } ?: run {
                append(text.substring(currentIndex))
                return@buildAnnotatedString
            }

            val matchStart = nextMatch.range.first
            val matchEnd = nextMatch.range.last + 1
            val matchText = nextMatch.value
            val contentText = nextMatch.groupValues[1]

            // Append text before the match
            if (currentIndex < matchStart) {
                append(text.substring(currentIndex, matchStart))
            }

            // Apply styling based on the match type
            when (nextMatch) {
                boldMatch -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(contentText)
                    }
                }
                italicMatch -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(contentText)
                    }
                }
                codeMatch -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            background = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
                        )
                    ) {
                        append(contentText)
                    }
                }
            }

            currentIndex = matchEnd
        }
    }

    Text(
        text = annotatedString,
        color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = modifier
    )
}
@Composable
fun ThinkingSection(
    thinkingSteps: String,
    showThinking: Boolean,
    onToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.2f) else Color(0xFFF3F4F6),
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
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How I got there",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF6B7280),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showThinking) "Collapse" else "Expand",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    AnimatedVisibility(
        visible = showThinking,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF2A2A3E).copy(alpha = 0.3f) else Color(0xFFF9FAFB),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            MarkdownText(
                text = thinkingSteps,
                isDarkTheme = isDarkTheme,
                isFromUser = false
            )
        }
    }
}



@Composable
fun ChatToolsSection(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onToolClick: (Any) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(selectedAgent.features) { feature ->
            ToolCard(
                feature = feature,
                isDarkTheme = isDarkTheme,
                onClick = { onToolClick(feature) }
            )
        }
    }
}

@Composable
fun ToolCard(
    feature: Any,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val (title, icon) = when (feature) {
        is CookingFeature -> feature.title to feature.icon
        is CraftingProject -> feature.title to feature.icon
        is FriendshipTool -> feature.title to feature.icon
        is DIYCategory -> feature.title to feature.icon
        else -> "" to Icons.Default.Build
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.3f) else Color(0xFFF3F4F6),
        modifier = Modifier.width(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDarkTheme) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun CookingToolsCard(
    recipe: Recipe,
    isDarkTheme: Boolean,
    cookingSessionManager: CookingSessionManager?,
    onStartInteractiveCooking: (Recipe) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFF1C1C2E)
            } else {
                Color(0xFFFFFFFE)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            // Premium gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (isDarkTheme) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF2D1B69).copy(alpha = 0.8f),
                                    Color(0xFF1C1C2E).copy(alpha = 0.9f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8F9FA),
                                    Color(0xFFFFFFFF)
                                )
                            )
                        }
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Premium Header with gradient accent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFFD93D)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Restaurant,
                            contentDescription = "Cooking Tools",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Interactive Cooking",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
                        )
                        Text(
                            text = "Premium Chef Experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDarkTheme)
                                Color.White.copy(alpha = 0.7f)
                            else
                                Color(0xFF6B7280)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Enhanced Recipe Overview with glassmorphism
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) {
                            Color.White.copy(alpha = 0.1f)
                        } else {
                            Color(0xFFF8F9FA).copy(alpha = 0.8f)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme)
                            Color.White.copy(alpha = 0.1f)
                        else
                            Color(0xFFE5E7EB)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            ),
                            color = if (isDarkTheme) Color.White else Color(0xFF111827)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PremiumInfoItem(
                                icon = Icons.Rounded.Schedule,
                                text = "${recipe.prepTime + recipe.cookTime} min",
                                label = "Total Time",
                                isDarkTheme = isDarkTheme
                            )
                            PremiumInfoItem(
                                icon = Icons.Rounded.People,
                                text = "${recipe.servings}",
                                label = "Servings",
                                isDarkTheme = isDarkTheme
                            )
                            PremiumInfoItem(
                                icon = Icons.Rounded.Star,
                                text = recipe.difficulty,
                                label = "Level",
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Premium Features Section
                Text(
                    text = "✨ What Makes This Special",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isDarkTheme) {
                        Color(0xFFFFD93D)
                    } else {
                        Color(0xFFFF6B6B)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val premiumFeatures = listOf(
                    FeatureItemNew("🎯", "Step-by-step guidance", "Smart cooking flow"),
                    FeatureItemNew("⏰", "Adaptive timers", "Perfect timing every time"),
                    FeatureItemNew("🤖", "AI cooking assistant", "Instant help & tips"),
                    FeatureItemNew("📊", "Progress tracking", "Visual cooking journey"),
                    FeatureItemNew("🎨", "Beautiful visuals", "AI-generated imagery")
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(premiumFeatures) { feature ->
                        PremiumFeatureRow(
                            feature = feature,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Premium CTA Button with advanced animations
                Button(
                    onClick = {
                        isPressed = true
                        onStartInteractiveCooking(recipe)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(if (isPressed) 0.96f else 1f)
                        .shadow(
                            elevation = if (isPressed) 8.dp else 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = Color(0xFFFF6B6B).copy(alpha = 0.3f)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFFD93D)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Start Cooking Experience",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.sp
                                )
                                Text(
                                    text = "Let's create something amazing! ✨",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                LaunchedEffect(isPressed) {
                    if (isPressed) {
                        delay(150)
                        isPressed = false
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    label: String,
    isDarkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isDarkTheme) {
                        Color(0xFFFF6B6B).copy(alpha = 0.2f)
                    } else {
                        Color(0xFFFF6B6B).copy(alpha = 0.1f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (isDarkTheme) Color.White else Color(0xFF111827)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDarkTheme)
                Color.White.copy(alpha = 0.6f)
            else
                Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PremiumFeatureRow(
    feature: FeatureItemNew,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color.White.copy(alpha = 0.05f)
            } else {
                Color(0xFFF8F9FA)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            0.5.dp,
            if (isDarkTheme)
                Color.White.copy(alpha = 0.1f)
            else
                Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = feature.emoji,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isDarkTheme) Color.White else Color(0xFF111827)
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.7f)
                    else
                        Color(0xFF6B7280)
                )
            }
        }
    }
}

data class FeatureItemNew(
    val emoji: String,
    val title: String,
    val description: String
)

fun compressBitmap(bitmap: Bitmap): ByteArray {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return outputStream.toByteArray()
}