package com.craftflowtechnologies.guidelens.universal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.ui.ThemeController
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.Bitmap
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.craftflowtechnologies.guidelens.cooking.MessageType

/**
 * Universal artifact overlay that works with all agent types
 * Replaces InteractiveCookingOverlay with a generic implementation
 */
@Composable
fun UniversalArtifactOverlay(
    artifact: Artifact,
    sessionManager: UniversalArtifactSessionManager,
    imageGenerator: ArtifactImageGenerator,
    onSendMessage: (String) -> Unit,
    onRequestImage: (String, Int) -> Unit,
    onCaptureProgress: () -> Unit,
    onBackPressed: () -> Unit = {},
    onDismiss: () -> Unit = {},
    isKeyboardVisible: Boolean = false,
    modifier: Modifier = Modifier,
    themeController: ThemeController,
    context: Context? = null
) {
    // Get the appropriate content adapter for this artifact type
    val contentAdapter = remember(artifact.type) {
        ContentAdapterFactory.createAdapter(artifact)
    }
    
    val currentSession by sessionManager.currentSession.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Enhanced state management with premium animations
    var isDragging by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }
    var dragScale by remember { mutableFloatStateOf(1f) }
    var showImagePicker by remember { mutableStateOf(false) }
    var overlayAlpha by remember { mutableFloatStateOf(1f) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var previewStepIndex by remember { mutableStateOf(0) }
    
    // Session state management
    var isSessionPaused by rememberSaveable { mutableStateOf(false) }
    var isSessionReset by remember { mutableStateOf(false) }
    
    // Premium animation curves and timing
    val premiumSpring: SpringSpec<androidx.compose.ui.unit.Dp> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val premiumSpringFloat: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val fluidTransition = tween<Float>(
        durationMillis = 400,
        easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    )

    // Sophisticated scaling with momentum
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isDragging -> dragScale.coerceIn(0.3f, 1.2f)
            isMinimized -> 0.85f
            else -> 1f
        },
        animationSpec = premiumSpringFloat,
        label = "overlay_scale"
    )

    // Dynamic height adaptation
    val animatedHeight by animateDpAsState(
        targetValue = when {
            isMinimized -> 120.dp
            isKeyboardVisible -> 280.dp
            else -> 380.dp
        },
        animationSpec = premiumSpring,
        label = "overlay_height"
    )

    // Progressive corner radius
    val animatedCornerRadius by animateDpAsState(
        targetValue = when {
            isMinimized -> 24.dp
            isDragging -> 28.dp
            else -> 24.dp
        },
        animationSpec = premiumSpring,
        label = "corner_radius"
    )

    // Create progress object from currentSession state
    val progress = currentSession?.let { session ->
        ArtifactProgress(
            currentStageIndex = session.currentStageIndex,
            completedStages = session.sessionContext.completedStages,
            sessionStartTime = session.startTime,
            sessionPaused = session.isPaused
        )
    }

    // Intelligent keyboard offset with momentum
    val keyboardOffset by animateDpAsState(
        targetValue = if (isKeyboardVisible && !isMinimized) 100.dp else 20.dp,
        animationSpec = premiumSpring,
        label = "keyboard_offset"
    )

    // Premium backdrop with glassmorphism
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Main overlay with premium materials
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = overlayAlpha
                    shadowElevation = if (isMinimized) 12f else 24f
                    spotShadowColor = Color.Black.copy(alpha = 0.25f)
                    ambientShadowColor = Color.Black.copy(alpha = 0.15f)
                }
                .align(Alignment.TopCenter)
                .offset(y = 68.dp + keyboardOffset)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = {
                            isDragging = false
                            val shouldMinimize = dragScale < 0.7f || overlayAlpha < 0.8f
                            isMinimized = shouldMinimize
                            dragScale = 1f
                            overlayAlpha = 1f
                            haptic.performHapticFeedback(
                                if (shouldMinimize) HapticFeedbackType.LongPress
                                else HapticFeedbackType.TextHandleMove
                            )
                        }
                    ) { _, dragAmount ->
                        // Advanced gesture recognition with multi-directional feedback
                        val verticalDelta = -dragAmount.y / 800f
                        val horizontalDelta = abs(dragAmount.x) / 1200f

                        dragScale = (dragScale + verticalDelta).coerceIn(0.3f, 1.2f)
                        overlayAlpha = (overlayAlpha - horizontalDelta).coerceIn(0.3f, 1f)
                    }
                }
                .zIndex(15f),
            shape = RoundedCornerShape(animatedCornerRadius),
            color = Color.Transparent
        ) {
            // Premium glassmorphism background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (themeController.isDarkTheme) {
                                listOf(
                                    Color(0xFF1C1C1E).copy(alpha = 0.95f),
                                    Color(0xFF2C2C2E).copy(alpha = 0.98f),
                                    Color(0xFF1A1A1A).copy(alpha = 0.95f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFFFAFAFA).copy(alpha = 0.98f),
                                    Color(0xFFF5F5F7).copy(alpha = 0.95f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(animatedCornerRadius)
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = if (themeController.isDarkTheme) {
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.02f)
                                )
                            } else {
                                listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.02f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(animatedCornerRadius)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {

                    // Premium drag handle with micro-interactions
                    UniversalDragHandle(
                        isMinimized = isMinimized,
                        isDarkTheme = themeController.isDarkTheme,
                        agentType = contentAdapter.agentType,
                        primaryColor = contentAdapter.primaryColor,
                        onExpand = {
                            isMinimized = false
                            dragScale = 1f
                            overlayAlpha = 1f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDismiss = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        }
                    )

                    // Fluid content transitions
                    AnimatedVisibility(
                        visible = !isMinimized,
                        enter = fadeIn(animationSpec = tween(350, delayMillis = 100)) +
                                slideInVertically(animationSpec = tween(350)) { it / 2 } +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(350)),
                        exit = fadeOut(animationSpec = tween(200)) +
                                slideOutVertically(animationSpec = tween(200)) { it / 2 } +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Universal artifact info row
                            UniversalArtifactInfoRow(
                                artifact = artifact,
                                contentAdapter = contentAdapter,
                                isDarkTheme = themeController.isDarkTheme
                            )

                            // Universal content area
                            UniversalArtifactContent(
                                artifact = artifact,
                                contentAdapter = contentAdapter,
                                progress = progress,
                                sessionManager = sessionManager,
                                onSendMessage = onSendMessage,
                                onRequestImage = onRequestImage,
                                onCaptureProgress = onCaptureProgress,
                                onCompleteStep = { stepId ->
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        sessionManager.completeStep(stepId)
                                        delay(500)
                                        sessionManager.nextStep()
                                    }
                                },
                                onNextStep = {
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        if (isPreviewMode) {
                                            val totalSteps = contentAdapter.getTotalSteps(artifact)
                                            if (previewStepIndex < totalSteps - 1) {
                                                previewStepIndex++
                                            }
                                        } else {
                                            sessionManager.nextStep()
                                        }
                                    }
                                },
                                onPreviousStep = {
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        if (isPreviewMode) {
                                            if (previewStepIndex > 0) {
                                                previewStepIndex--
                                            }
                                        } else {
                                            sessionManager.previousStep()
                                        }
                                    }
                                },
                                onTogglePreviewMode = { isPreview ->
                                    isPreviewMode = isPreview
                                    if (isPreview) {
                                        val currentStep = progress?.currentStageIndex ?: 0
                                        previewStepIndex = currentStep
                                    }
                                },
                                onPauseSession = {
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        if (isSessionPaused) {
                                            sessionManager.resumeSession()
                                            isSessionPaused = false
                                        } else {
                                            sessionManager.pauseSession()
                                            isSessionPaused = true
                                        }
                                    }
                                },
                                onResetSession = {
                                    isSessionReset = true
                                    isSessionPaused = false
                                    isPreviewMode = false
                                    previewStepIndex = 0
                                },
                                isPreviewMode = isPreviewMode,
                                previewStepIndex = previewStepIndex,
                                isSessionPaused = isSessionPaused,
                                isDarkTheme = themeController.isDarkTheme,
                                showImagePicker = showImagePicker,
                                onShowImagePicker = { show -> showImagePicker = show },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Elegant minimized state with smart preview
                    if (isMinimized) {
                        UniversalProgressIndicator(
                            artifact = artifact,
                            contentAdapter = contentAdapter,
                            progress = progress,
                            isDarkTheme = themeController.isDarkTheme,
                            onExpand = {
                                isMinimized = false
                                dragScale = 1f
                                overlayAlpha = 1f
                            }
                        )
                    }
                }
            }
        }
    }

    // Premium image picker modal (if agent supports it)
    if (showImagePicker && supportsImageCapture(contentAdapter.agentType)) {
        UniversalImagePickerDialog(
            agentType = contentAdapter.agentType,
            primaryColor = contentAdapter.primaryColor,
            onDismiss = { showImagePicker = false },
            onCamera = {
                // Handle camera capture
                showImagePicker = false
            },
            onGallery = {
                // Handle gallery selection
                showImagePicker = false
            },
            isDarkTheme = themeController.isDarkTheme
        )
    }
}

@Composable
private fun UniversalDragHandle(
    isMinimized: Boolean,
    isDarkTheme: Boolean,
    agentType: String,
    primaryColor: Color,
    onExpand: () -> Unit,
    onDismiss: () -> Unit
) {
    val handleAlpha by animateFloatAsState(
        targetValue = if (isMinimized) 0.8f else 0.6f,
        animationSpec = tween(300)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contextual expand action with agent-specific styling
        AnimatedVisibility(
            visible = isMinimized,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                onClick = onExpand,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ExpandLess,
                        contentDescription = "Expand",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (!isMinimized) {
            Spacer(modifier = Modifier.width(32.dp))
        }

        // Premium drag handle with subtle animation
        Surface(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clickable { if (isMinimized) onExpand() }
                .graphicsLayer { alpha = handleAlpha },
            shape = RoundedCornerShape(2.dp),
            color = if (isDarkTheme) {
                Color.White.copy(alpha = 0.25f)
            } else {
                Color.Black.copy(alpha = 0.15f)
            }
        ) {}

        // Refined dismiss control
        Surface(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UniversalArtifactInfoRow(
    artifact: Artifact,
    contentAdapter: ContentAdapter,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Agent type indicator
        UniversalMetricCard(
            icon = contentAdapter.icon,
            label = contentAdapter.agentType.capitalize(),
            value = artifact.difficulty,
            color = contentAdapter.primaryColor,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )

        // Duration metric (if available)
        artifact.estimatedDuration?.let { duration ->
            UniversalMetricCard(
                icon = Icons.Rounded.Schedule,
                label = "Duration",
                value = "${duration}m",
                color = contentAdapter.secondaryColor,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        // Steps metric
        UniversalMetricCard(
            icon = Icons.Rounded.List,
            label = "Steps",
            value = contentAdapter.getTotalSteps(artifact).toString(),
            color = Color(0xFF007AFF),
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper functions and other components would continue here...
// This is a substantial component so I'll break it into multiple files for better organization

private fun supportsImageCapture(agentType: String): Boolean {
    return when (agentType.lowercase()) {
        "cooking", "crafting", "diy" -> true
        "buddy" -> false // Tutorials typically don't need image capture
        else -> false
    }
}