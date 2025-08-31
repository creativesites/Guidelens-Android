package com.craftflowtechnologies.guidelens.universal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal done button that adapts to different agent types
 */
@Composable
fun UniversalDoneButton(
    isCompleted: Boolean,
    onMarkDone: () -> Unit,
    onAutoProgress: () -> Unit,
    contentAdapter: ContentAdapter,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) contentAdapter.secondaryColor else contentAdapter.primaryColor,
        animationSpec = spring(dampingRatio = 0.8f)
    )

    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )

    Surface(
        onClick = {
            if (!isCompleted) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onMarkDone()
                onAutoProgress()
            }
        },
        enabled = !isCompleted,
        modifier = modifier
            .height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isCompleted) 4.dp else 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.8f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }

                AnimatedVisibility(
                    visible = !isCompleted,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Mark Done",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }

                Text(
                    text = if (isCompleted) "Completed" else "Mark Done",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Universal timer button that adapts to different agent types
 */
@Composable
fun UniversalTimerButton(
    duration: Int,
    agentType: String,
    primaryColor: Color,
    isPreviewMode: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember(duration) { mutableLongStateOf((duration * 60 * 1000).toLong()) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    
    val progress = if (duration > 0) {
        1f - (timeLeft.toFloat() / (duration * 60 * 1000).toFloat())
    } else 0f

    val timerColor = when {
        timeLeft == 0L -> Color(0xFFFF453A) // Red - time's up
        timeLeft < 60000L -> Color(0xFFFF9F0A) // Orange - less than 1 minute
        isRunning -> primaryColor // Agent color - running
        isPaused -> Color(0xFF007AFF) // Blue - paused
        else -> Color(0xFF8E8E93) // Gray - stopped
    }

    // Timer countdown effect
    LaunchedEffect(isRunning, isPreviewMode) {
        if (!isPreviewMode) {
            while (isRunning && timeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                timeLeft = (timeLeft - 1000).coerceAtLeast(0)
            }
            if (timeLeft == 0L && isRunning) {
                isRunning = false
                // Could trigger notification here
            }
        }
    }

    Surface(
        onClick = {
            if (!isPreviewMode) {
                when {
                    timeLeft == 0L -> {
                        timeLeft = (duration * 60 * 1000).toLong()
                        isRunning = false
                        isPaused = false
                    }
                    isRunning -> {
                        isRunning = false
                        isPaused = true
                    }
                    isPaused -> {
                        isRunning = true
                        isPaused = false
                    }
                    else -> {
                        isRunning = true
                        isPaused = false
                    }
                }
            }
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = timerColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, timerColor.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Progress background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                timerColor.copy(alpha = 0.2f * progress),
                                timerColor.copy(alpha = 0.1f * progress),
                                Color.Transparent
                            ),
                            radius = maxOf(1f, 100f * progress)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Agent-specific timer icon
                Icon(
                    when {
                        timeLeft == 0L -> Icons.Rounded.NotificationImportant
                        isRunning -> getAgentTimerIcon(agentType)
                        isPaused -> Icons.Rounded.Pause
                        else -> Icons.Rounded.Schedule
                    },
                    contentDescription = "Timer",
                    tint = timerColor,
                    modifier = Modifier.size(18.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val minutes = timeLeft / 60000
                    val seconds = (timeLeft % 60000) / 1000
                    
                    Text(
                        text = "${minutes}:${seconds.toString().padStart(2, '0')}",
                        color = timerColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = when {
                            isPreviewMode -> "Preview"
                            timeLeft == 0L -> "Done!"
                            isRunning -> getAgentTimerLabel(agentType)
                            isPaused -> "Paused"
                            else -> "Ready"
                        },
                        color = timerColor.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Universal action button that adapts to different agent types
 */
@Composable
fun UniversalActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f)),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Universal image picker dialog
 */
@Composable
fun UniversalImagePickerDialog(
    agentType: String,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    isDarkTheme: Boolean
) {
    val agentAction = when (agentType) {
        "cooking" -> "cooking progress"
        "crafting" -> "craft progress" 
        "diy" -> "project progress"
        else -> "progress"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Capture Progress",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Take a photo of your $agentAction to get personalized AI feedback and guidance.",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UniversalImageOption(
                        icon = Icons.Rounded.PhotoCamera,
                        title = "Camera",
                        description = "Take Photo",
                        color = primaryColor,
                        onClick = onCamera,
                        modifier = Modifier.weight(1f)
                    )

                    UniversalImageOption(
                        icon = Icons.Rounded.PhotoLibrary,
                        title = "Gallery",
                        description = "Choose Photo",
                        color = Color(0xFF30D158),
                        onClick = onGallery,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                )
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun UniversalImageOption(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = color
            )

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

// Helper functions for agent-specific UI elements
private fun getAgentTimerIcon(agentType: String): ImageVector {
    return when (agentType) {
        "cooking" -> Icons.Rounded.LocalFireDepartment
        "crafting" -> Icons.Rounded.Palette
        "diy" -> Icons.Rounded.Build
        "buddy" -> Icons.Rounded.School
        else -> Icons.Rounded.Schedule
    }
}

private fun getAgentTimerLabel(agentType: String): String {
    return when (agentType) {
        "cooking" -> "Cooking"
        "crafting" -> "Creating"
        "diy" -> "Building"
        "buddy" -> "Learning"
        else -> "Running"
    }
}

/**
 * Universal agent theme colors
 */
object UniversalAgentTheme {
    val Cooking = AgentColors(
        primary = Color(0xFF32D74B),
        secondary = Color(0xFFFF9500),
        accent = Color(0xFF007AFF)
    )
    
    val Crafting = AgentColors(
        primary = Color(0xFFBF5AF2),
        secondary = Color(0xFF00C7BE),
        accent = Color(0xFF30D158)
    )
    
    val DIY = AgentColors(
        primary = Color(0xFFFF9F0A),
        secondary = Color(0xFF007AFF),
        accent = Color(0xFF32D74B)
    )
    
    val Buddy = AgentColors(
        primary = Color(0xFF30D158),
        secondary = Color(0xFF5856D6),
        accent = Color(0xFF007AFF)
    )
    
    fun getColors(agentType: String): AgentColors {
        return when (agentType.lowercase()) {
            "cooking" -> Cooking
            "crafting" -> Crafting
            "diy" -> DIY
            "buddy" -> Buddy
            else -> Cooking // Default fallback
        }
    }
}

data class AgentColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color
)