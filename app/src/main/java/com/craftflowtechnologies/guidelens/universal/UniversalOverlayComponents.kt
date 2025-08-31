package com.craftflowtechnologies.guidelens.universal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.Dispatchers

/**
 * Universal metric card that adapts to different agent types
 */
@Composable
fun UniversalMetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Universal progress indicator for minimized state
 */
@Composable
fun UniversalProgressIndicator(
    artifact: Artifact,
    contentAdapter: ContentAdapter,
    progress: ArtifactProgress?,
    isDarkTheme: Boolean,
    onExpand: () -> Unit
) {
    val currentStepIndex = progress?.currentStageIndex ?: 0
    val totalSteps = contentAdapter.getTotalSteps(artifact)
    val progressPercentage = ((currentStepIndex + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

    Surface(
        onClick = onExpand,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) {
            Color(0xFF2C2C2E).copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        border = BorderStroke(
            1.dp,
            contentAdapter.primaryColor.copy(alpha = 0.15f)
        ),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Agent-themed circular progress indicator
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier.fillMaxSize(),
                    color = contentAdapter.primaryColor,
                    strokeWidth = 3.dp,
                    trackColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )

                // Agent icon in center
                Icon(
                    contentAdapter.icon,
                    contentDescription = contentAdapter.agentType,
                    tint = contentAdapter.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Artifact info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = artifact.title,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${currentStepIndex + 1} of $totalSteps",
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Progress percentage chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = contentAdapter.primaryColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            color = contentAdapter.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expand indicator with agent theme
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Expand",
                tint = contentAdapter.primaryColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Universal artifact content area that adapts to different content types
 */
@Composable
fun UniversalArtifactContent(
    artifact: Artifact,
    contentAdapter: ContentAdapter,
    progress: ArtifactProgress?,
    sessionManager: UniversalArtifactSessionManager,
    onSendMessage: (String) -> Unit,
    onRequestImage: (String, Int) -> Unit,
    onCaptureProgress: () -> Unit,
    onCompleteStep: (String) -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onTogglePreviewMode: (Boolean) -> Unit,
    onPauseSession: () -> Unit,
    onResetSession: () -> Unit,
    isPreviewMode: Boolean,
    previewStepIndex: Int,
    isSessionPaused: Boolean,
    isDarkTheme: Boolean,
    showImagePicker: Boolean,
    onShowImagePicker: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStepIndex = if (isPreviewMode) previewStepIndex else (progress?.currentStageIndex ?: 0)
    val totalSteps = contentAdapter.getTotalSteps(artifact)
    val completedSteps = progress?.completedStages ?: emptySet()
    val progressPercentage = (completedSteps.size.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
    
    // Get universal step data from adapter
    val stepData = UniversalStepData.fromAdapter(contentAdapter, artifact, currentStepIndex)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Universal progress header
        item {
            UniversalProgressHeader(
                artifact = artifact,
                contentAdapter = contentAdapter,
                progressPercentage = progressPercentage,
                currentStep = currentStepIndex + 1,
                totalSteps = totalSteps,
                completedSteps = completedSteps,
                isDarkTheme = isDarkTheme
            )
        }

        // Universal navigation controls
        item {
            UniversalStepNavigation(
                currentStep = currentStepIndex + 1,
                totalSteps = totalSteps,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                isPreviewMode = isPreviewMode,
                onTogglePreviewMode = onTogglePreviewMode,
                isSessionPaused = isSessionPaused,
                onPauseSession = onPauseSession,
                onResetSession = onResetSession,
                contentAdapter = contentAdapter,
                isDarkTheme = isDarkTheme
            )
        }

        // Universal step card
        item {
            UniversalStepCard(
                stepData = stepData,
                stepIndex = currentStepIndex,
                completedSteps = completedSteps,
                contentAdapter = contentAdapter,
                onCompleteStep = {
                    Log.d("UniversalOverlay", "Completing step: ${currentStepIndex + 1}")
                    onCompleteStep((currentStepIndex + 1).toString())
                },
                onSendMessage = onSendMessage,
                onAutoProgress = {
                    val progressMessage = buildString {
                        appendLine("I've completed step ${stepData.title}.")
                        appendLine("Please check my progress and guide me for the next step.")
                    }
                    onSendMessage(progressMessage)
                },
                onNeedHelp = {
                    val helpMessage = buildString {
                        appendLine("I need help with step ${currentStepIndex + 1}: ${stepData.title}")
                        appendLine("Description: ${stepData.description}")
                        stepData.techniques.takeIf { it.isNotEmpty() }?.let {
                            appendLine("Techniques involved: ${it.joinToString(", ")}")
                        }
                        appendLine("Please provide specific guidance.")
                    }
                    onSendMessage(helpMessage)
                },
                onRequestImage = {
                    val imagePrompt = "Generate a visual reference for step ${currentStepIndex + 1}: ${stepData.title}"
                    onRequestImage(imagePrompt, currentStepIndex)
                },
                onCaptureStepPhoto = {
                    onShowImagePicker(true)
                },
                isPreviewMode = isPreviewMode,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

/**
 * Universal progress header with agent-specific theming
 */
@Composable
private fun UniversalProgressHeader(
    artifact: Artifact,
    contentAdapter: ContentAdapter,
    progressPercentage: Float,
    currentStep: Int,
    totalSteps: Int,
    completedSteps: Set<String>,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artifact.title,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Agent type badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = contentAdapter.primaryColor.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, contentAdapter.primaryColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        contentAdapter.icon,
                        contentDescription = contentAdapter.agentType,
                        tint = contentAdapter.primaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = contentAdapter.agentType.capitalize(),
                        color = contentAdapter.primaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.Black.copy(alpha = 0.06f)
                }
            ) {
                Text(
                    text = "$currentStep/$totalSteps",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Progress visualization with agent theming
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                // Background
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Black.copy(alpha = 0.04f)
                    }
                ) {}

                // Agent-themed progress fill
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(
                            animateFloatAsState(
                                targetValue = progressPercentage,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ).value
                        )
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        contentAdapter.primaryColor,
                                        contentAdapter.secondaryColor,
                                        contentAdapter.primaryColor.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }

            // Step completion indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(totalSteps.coerceAtMost(10)) { index ->
                    val stepNumber = (index + 1).toString()
                    val isCompleted = stepNumber in completedSteps
                    val isCurrent = index == currentStep - 1

                    UniversalStepIndicator(
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        stepNumber = index + 1,
                        primaryColor = contentAdapter.primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun UniversalStepIndicator(
    isCompleted: Boolean,
    isCurrent: Boolean,
    stepNumber: Int,
    primaryColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.7f)
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> primaryColor
            isCurrent -> primaryColor.copy(alpha = 0.7f)
            else -> Color.Transparent
        },
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .scale(scale)
            .background(backgroundColor, CircleShape)
            .border(
                width = if (isCurrent && !isCompleted) 2.dp else 0.dp,
                color = primaryColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        } else if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}