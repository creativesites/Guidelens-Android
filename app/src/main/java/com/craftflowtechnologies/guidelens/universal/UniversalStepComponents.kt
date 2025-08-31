package com.craftflowtechnologies.guidelens.universal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log

/**
 * Universal step navigation that adapts to different agent types
 */
@Composable
fun UniversalStepNavigation(
    currentStep: Int,
    totalSteps: Int,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    isPreviewMode: Boolean,
    onTogglePreviewMode: (Boolean) -> Unit,
    isSessionPaused: Boolean,
    onPauseSession: () -> Unit,
    onResetSession: () -> Unit,
    contentAdapter: ContentAdapter,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous step button
            UniversalNavigationButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                enabled = true,
                onClick = {
                    Log.d("UniversalStepNav", "Previous button clicked")
                    onPreviousStep()
                },
                contentAdapter = contentAdapter,
                isDarkTheme = isDarkTheme
            )

            // Current step indicator with agent theming
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isPreviewMode) {
                    contentAdapter.primaryColor.copy(alpha = 0.1f)
                } else if (isDarkTheme) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.Black.copy(alpha = 0.04f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isPreviewMode) {
                        contentAdapter.primaryColor.copy(alpha = 0.3f)
                    } else if (isDarkTheme) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color.Black.copy(alpha = 0.08f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Step $currentStep of $totalSteps",
                        color = if (isPreviewMode) {
                            contentAdapter.primaryColor
                        } else if (isDarkTheme) {
                            Color.White.copy(alpha = 0.9f)
                        } else {
                            Color.Black.copy(alpha = 0.8f)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isPreviewMode) {
                        Text(
                            text = "Preview Mode",
                            color = contentAdapter.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Next step button
            UniversalNavigationButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                enabled = true,
                onClick = {
                    Log.d("UniversalStepNav", "Next button clicked")
                    onNextStep()
                },
                contentAdapter = contentAdapter,
                isDarkTheme = isDarkTheme
            )
        }
        
        // Session controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview mode toggle
            UniversalSessionControlButton(
                icon = if (isPreviewMode) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                text = if (isPreviewMode) "Live" else "Preview",
                onClick = { onTogglePreviewMode(!isPreviewMode) },
                color = if (isPreviewMode) contentAdapter.primaryColor else contentAdapter.secondaryColor,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            // Pause/Resume button
            UniversalSessionControlButton(
                icon = if (isSessionPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                text = if (isSessionPaused) "Resume" else "Pause",
                onClick = onPauseSession,
                color = if (isSessionPaused) Color(0xFF32D74B) else Color(0xFFFF9F0A),
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            // Reset button
            UniversalSessionControlButton(
                icon = Icons.Rounded.RestartAlt,
                text = "Reset",
                onClick = onResetSession,
                color = Color(0xFFFF453A),
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun UniversalNavigationButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    contentAdapter: ContentAdapter,
    isDarkTheme: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) {
            contentAdapter.primaryColor.copy(alpha = 0.1f)
        } else {
            if (isDarkTheme) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
        },
        animationSpec = tween(200)
    )

    Surface(
        onClick = {
            Log.d("UniversalNavigationButton", "Navigation button clicked - enabled: $enabled")
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = backgroundColor,
        border = if (enabled) BorderStroke(
            1.dp,
            contentAdapter.primaryColor.copy(alpha = 0.3f)
        ) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) {
                    contentAdapter.primaryColor
                } else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UniversalSessionControlButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Universal step card that adapts to different content types
 */
@Composable
fun UniversalStepCard(
    stepData: UniversalStepData,
    stepIndex: Int,
    completedSteps: Set<String>,
    contentAdapter: ContentAdapter,
    onCompleteStep: () -> Unit,
    onSendMessage: (String) -> Unit,
    onAutoProgress: () -> Unit,
    onNeedHelp: () -> Unit,
    onRequestImage: () -> Unit,
    onCaptureStepPhoto: (String) -> Unit,
    isPreviewMode: Boolean,
    isDarkTheme: Boolean
) {
    val isCompleted = (stepIndex + 1).toString() in completedSteps
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) {
            Color(0xFF2C2C2E).copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        border = BorderStroke(
            1.dp,
            contentAdapter.primaryColor.copy(alpha = 0.08f)
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step header with agent theming
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stepData.title,
                            color = if (isDarkTheme) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Agent-specific badges
                        if (stepData.specificData.containsKey("criticalStep") && 
                            stepData.specificData["criticalStep"] == "true") {
                            UniversalCriticalBadge(contentAdapter.primaryColor, isDarkTheme)
                        }
                        
                        if (stepData.specificData.containsKey("safetyWarnings") &&
                            contentAdapter.agentType == "diy") {
                            UniversalSafetyBadge(isDarkTheme)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stepData.description,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Enhanced info chips
            if (stepData.duration != null || stepData.techniques.isNotEmpty() || stepData.requiredItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stepData.duration?.let { duration ->
                        item {
                            UniversalInfoChip(
                                icon = Icons.Rounded.Schedule,
                                text = "${duration}m",
                                color = contentAdapter.secondaryColor,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                    
                    if (stepData.techniques.isNotEmpty()) {
                        item {
                            UniversalInfoChip(
                                icon = Icons.Rounded.Psychology,
                                text = "${stepData.techniques.size} techniques",
                                color = contentAdapter.primaryColor,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                    
                    if (stepData.requiredItems.isNotEmpty()) {
                        item {
                            UniversalInfoChip(
                                icon = when (contentAdapter.agentType) {
                                    "cooking" -> Icons.Rounded.Kitchen
                                    "crafting" -> Icons.Rounded.Palette
                                    "diy" -> Icons.Rounded.Build
                                    else -> Icons.Rounded.List
                                },
                                text = "${stepData.requiredItems.size} items",
                                color = contentAdapter.secondaryColor,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }

            // Action buttons with agent-specific adaptations
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UniversalDoneButton(
                        isCompleted = isCompleted,
                        onMarkDone = onCompleteStep,
                        onAutoProgress = onAutoProgress,
                        contentAdapter = contentAdapter,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(2f)
                    )

                    // Agent-specific timer or duration display
                    stepData.duration?.let { duration ->
                        UniversalTimerButton(
                            duration = duration,
                            agentType = contentAdapter.agentType,
                            primaryColor = contentAdapter.primaryColor,
                            isPreviewMode = isPreviewMode,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Secondary actions - agent-aware
                val actions = getAgentSpecificActions(contentAdapter.agentType, stepData)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEach { action ->
                        UniversalActionButton(
                            icon = action.icon,
                            text = action.text,
                            onClick = when (action.type) {
                                "photo" -> { { onCaptureStepPhoto("step_${stepIndex}") } }
                                "help" -> { { onNeedHelp() } }
                                "visual" -> { { onRequestImage() } }
                                "details" -> { { 
                                    val detailsMessage = buildAgentSpecificDetails(stepData, contentAdapter.agentType)
                                    onSendMessage(detailsMessage)
                                } }
                                else -> { {} }
                            },
                            color = action.color,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UniversalCriticalBadge(primaryColor: Color, isDarkTheme: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF453A).copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color(0xFFFF453A).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = Color(0xFFFF453A),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "Critical",
                color = Color(0xFFFF453A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UniversalSafetyBadge(isDarkTheme: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF9F0A).copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color(0xFFFF9F0A).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Rounded.Security,
                contentDescription = null,
                tint = Color(0xFFFF9F0A),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "Safety",
                color = Color(0xFFFF9F0A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UniversalInfoChip(
    icon: ImageVector,
    text: String,
    color: Color,
    isDarkTheme: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class ActionItem(
    val icon: ImageVector,
    val text: String,
    val color: Color,
    val type: String
)

private fun getAgentSpecificActions(agentType: String, stepData: UniversalStepData): List<ActionItem> {
    val baseActions = mutableListOf<ActionItem>()
    
    // Common actions for visual agents
    if (agentType in listOf("cooking", "crafting", "diy")) {
        baseActions.add(
            ActionItem(
                icon = Icons.Rounded.PhotoCamera,
                text = "Photo",
                color = Color(0xFF00C7BE),
                type = "photo"
            )
        )
    }
    
    // All agents have help
    baseActions.add(
        ActionItem(
            icon = Icons.AutoMirrored.Rounded.Help,
            text = "AI Help",
            color = Color(0xFFBF5AF2),
            type = "help"
        )
    )
    
    // Visual guides for physical tasks
    if (agentType in listOf("cooking", "crafting", "diy")) {
        baseActions.add(
            ActionItem(
                icon = Icons.Rounded.Visibility,
                text = "Visual",
                color = Color(0xFF30D158),
                type = "visual"
            )
        )
    }
    
    // Details button
    baseActions.add(
        ActionItem(
            icon = Icons.Rounded.Info,
            text = "Details",
            color = Color(0xFFFF9F0A),
            type = "details"
        )
    )
    
    return baseActions
}

private fun buildAgentSpecificDetails(stepData: UniversalStepData, agentType: String): String {
    return buildString {
        appendLine("📔 Complete Details for ${stepData.title}:")
        appendLine("${getAgentEmoji(agentType)} ${stepData.description}")
        appendLine("")
        
        if (stepData.techniques.isNotEmpty()) {
            appendLine("🔧 Techniques required:")
            stepData.techniques.forEach { technique ->
                appendLine("• $technique")
            }
            appendLine("")
        }
        
        if (stepData.tips.isNotEmpty()) {
            appendLine("💡 Tips:")
            stepData.tips.forEach { tip ->
                appendLine("• $tip")
            }
            appendLine("")
        }
        
        if (stepData.requiredItems.isNotEmpty()) {
            appendLine("📝 Required items:")
            stepData.requiredItems.forEach { item ->
                appendLine("• $item")
            }
            appendLine("")
        }
        
        stepData.duration?.let { duration ->
            appendLine("⏱️ Expected duration: $duration minutes")
        }
        
        // Agent-specific details
        when (agentType) {
            "cooking" -> {
                stepData.specificData["temperature"]?.takeIf { it.isNotEmpty() }?.let {
                    appendLine("🌡️ Temperature: $it")
                }
                if (stepData.specificData["criticalStep"] == "true") {
                    appendLine("⚠️ Critical step - extra attention required!")
                }
            }
            "diy" -> {
                stepData.specificData["safetyWarnings"]?.takeIf { it.isNotEmpty() }?.let {
                    appendLine("⚠️ Safety warnings: $it")
                }
            }
            "crafting" -> {
                stepData.specificData["materials"]?.takeIf { it.isNotEmpty() }?.let {
                    appendLine("🎨 Materials needed: $it")
                }
            }
        }
    }
}

private fun getAgentEmoji(agentType: String): String {
    return when (agentType) {
        "cooking" -> "🍳"
        "crafting" -> "🎨"
        "diy" -> "🔨"
        "buddy" -> "📚"
        else -> "📋"
    }
}