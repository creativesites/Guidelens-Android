package com.craftflowtechnologies.guidelens.cooking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.Bitmap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.Dp
import com.craftflowtechnologies.guidelens.charts.NutritionChart
import com.craftflowtechnologies.guidelens.ui.theme.GuideLensColors
import com.craftflowtechnologies.guidelens.ui.theme.CookingColors
import com.craftflowtechnologies.guidelens.ui.StageImageDisplay
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactContent
import com.craftflowtechnologies.guidelens.storage.ArtifactProgress
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import com.craftflowtechnologies.guidelens.storage.AIAnalysis
import com.craftflowtechnologies.guidelens.storage.StageImage
import com.craftflowtechnologies.guidelens.ui.ThemeController
import com.craftflowtechnologies.guidelens.api.GeminiTextClient
import android.media.MediaPlayer
import android.content.Context
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Persistent timer state for steps
//@Serializable
data class PersistentTimerState(
    val remainingTime: Long,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val originalDuration: Long
)

@Composable
fun InteractiveCookingOverlay(
    artifact: Artifact,
    sessionManager: EnhancedCookingSessionManager,
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

    var isGeneratingImages by remember { mutableStateOf(false) }
    var generatedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Session state management
    var isSessionPaused by rememberSaveable { mutableStateOf(false) }
    var isSessionReset by remember { mutableStateOf(false) }
    
    // Persistent timer states for each step
    val persistentTimers = remember { mutableMapOf<String, PersistentTimerState>() }
    
    // Alarm sound player
    val mediaPlayer = remember {
        context?.let { ctx ->
            MediaPlayer().apply {
                try {
                    // Use system notification sound instead of specific resource
                    setDataSource(ctx, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    prepare()
                } catch (e: Exception) {
                    Log.e("CookingOverlay", "Failed to prepare alarm sound", e)
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }


    // Premium animation curves and timing
    val premiumSpring: SpringSpec<Dp> = spring(
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

    // Extract recipe content
    val recipeContent = artifact.contentData as? ArtifactContent.RecipeContent
    val recipe = recipeContent?.recipe
    
    // Create progress object from currentSession state
    val progress = currentSession?.let { session ->
        ArtifactProgress(
            currentStageIndex = session.currentStageIndex,
            completedStages = session.sessionContext.completedStages,
            sessionStartTime = session.startTime,
            sessionPaused = session.isPaused
        )
    }

    if (recipe == null) return

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
                    PremiumDragHandle(
                        isMinimized = isMinimized,
                        isDarkTheme = themeController.isDarkTheme,
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
                            // Premium recipe info with dynamic metrics
                            PremiumRecipeInfoRow(
                                prepTime = recipe.prepTime,
                                cookTime = recipe.cookTime,
                                servings = recipe.servings,
                                isDarkTheme = themeController.isDarkTheme
                            )

                            // Enhanced cooking content with fluid interactions
                            PremiumCookingContent(
                                recipe = recipe,
                                artifact = artifact,
                                progress = progress,
                                sessionManager = sessionManager,
                                onSendMessage = onSendMessage,
                                onRequestImage = onRequestImage,
                                onCaptureProgress = onCaptureProgress,
                                onCompleteStep = { stepId ->
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        sessionManager.completeStep(stepId)
                                        // Auto-advance to next step after completion
                                        val currentStep = progress?.currentStageIndex ?: 0
//                                        if (!isPreviewMode && currentStep < recipe.steps.size - 1) {
//                                            delay(500) // Brief pause for user feedback
//                                            sessionManager.nextStep()
//                                        }
                                        delay(500) // Brief pause for user feedback
                                        sessionManager.nextStep()
                                    }
                                },
                                onNextStep = {
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        if (isPreviewMode) {
                                            if (previewStepIndex < recipe.steps.size - 1) {
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
                                    persistentTimers.clear()
                                    val scope = CoroutineScope(Dispatchers.Main)
                                    scope.launch {
                                        // Reset session through manager
                                        val currentStep = progress?.currentStageIndex ?: 0
                                        repeat(currentStep) {
                                            sessionManager.previousStep()
                                        }
                                    }
                                },
                                isPreviewMode = isPreviewMode,
                                previewStepIndex = previewStepIndex,
                                isSessionPaused = isSessionPaused,
                                persistentTimers = persistentTimers,
                                mediaPlayer = mediaPlayer,
                                isDarkTheme = themeController.isDarkTheme,
                                showImagePicker = showImagePicker,
                                onShowImagePicker = { show -> showImagePicker = show },
                                modifier = Modifier.weight(1f),
                                generatedImages = generatedImages,
                                geminiClient = GeminiTextClient()
//                                showNutritionChart = showNutritionChart,
//                                userProfile = recipe.userProfile
                            )
                        }
                    }

                    // Elegant minimized state with smart preview
                    if (isMinimized) {
                        PremiumProgressIndicator(
                            recipe = recipe,
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
}

@Composable
private fun PremiumDragHandle(
    isMinimized: Boolean,
    isDarkTheme: Boolean,
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
        // Contextual expand action
        AnimatedVisibility(
            visible = isMinimized,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                onClick = onExpand,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.ExpandLess,
                        contentDescription = "Expand",
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
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
private fun PremiumRecipeInfoRow(
    prepTime: Int,
    cookTime: Int,
    servings: Int,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PremiumMetricCard(
            icon = Icons.Rounded.Schedule,
            label = "Prep",
            value = "${prepTime}m",
            color = Color(0xFF32D74B),
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )

        PremiumMetricCard(
            icon = Icons.Rounded.LocalFireDepartment,
            label = "Cook",
            value = "${cookTime}m",
            color = Color(0xFFFF9500),
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )

        PremiumMetricCard(
            icon = Icons.Rounded.People,
            label = "Serves",
            value = "$servings",
            color = Color(0xFF007AFF),
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PremiumMetricCard(
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

@Composable
private fun PremiumCookingContent(
    recipe: Recipe,
    artifact: Artifact,
    progress: ArtifactProgress?,
    sessionManager: EnhancedCookingSessionManager,
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
    persistentTimers: MutableMap<String, PersistentTimerState>,
    mediaPlayer: MediaPlayer?,
    isDarkTheme: Boolean,
    showImagePicker: Boolean,
    onShowImagePicker: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    generatedImages: List<String> = emptyList(),
    geminiClient: GeminiTextClient
//    showNutritionChart: Boolean,
//    userProfile: UserCookingProfile,

) {
    val currentStepIndex = if (isPreviewMode) previewStepIndex else (progress?.currentStageIndex ?: 0)
    val currentStep = recipe.steps.getOrNull(currentStepIndex)
    val totalSteps = recipe.steps.size
    val completedSteps = progress?.completedStages ?: emptySet()
    val progressPercentage = (completedSteps.size.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

    // Image capture launchers with enhanced UX and proper image attachment
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { capturedBitmap ->
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    // Save bitmap and get URI for attachment
                    val currentStep = progress?.currentStageIndex ?: 0
                    val imageUri = saveBitmapToCache(capturedBitmap, artifact.id, currentStep)
                    
                    val photoMessage = buildString {
                        val currentStepIndex = progress?.currentStageIndex ?: 0
                        appendLine("📸 Here's a photo of my progress on step ${currentStepIndex + 1}:")
                        recipe.steps.getOrNull(currentStepIndex)?.let { step ->
                            appendLine("🍳 Step: ${step.description}")
                        }
                        appendLine("📋 Recipe: ${recipe.title}")
                        appendLine("")
                        appendLine("🔍 Please analyze my progress and provide specific feedback on:")
                        appendLine("• How well I'm executing this step")
                        appendLine("• Any corrections or improvements needed")
                        appendLine("• What to focus on for the next step")
                        appendLine("• Cooking tips specific to what you see")
                        appendLine("")
                        appendLine("💡 Image details: ${capturedBitmap.width}x${capturedBitmap.height}")
                    }
                    
                    // Process image with session manager first
                    val messageResult = sessionManager.processUserMessage(
                        message = photoMessage,
                        messageType = MessageType.IMAGE,
                        attachments = listOfNotNull(imageUri)
                    )
                    
                    // Then send to main chat if successful
                    if (messageResult.isSuccess) {
                        CoroutineScope(Dispatchers.Main).launch {
                            onSendMessage(photoMessage)
                        }
                        Log.d("CookingOverlay", "Camera photo captured and processed successfully")
                    } else {
                        Log.e("CookingOverlay", "Failed to process camera photo: ${messageResult.exceptionOrNull()}")
                    }
                } catch (e: Exception) {
                    Log.e("CookingOverlay", "Failed to handle camera photo", e)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val photoMessage = buildString {
                        val currentStepIndex = progress?.currentStageIndex ?: 0
                        appendLine("🖼️ Here's a photo from my gallery showing my progress on step ${currentStepIndex + 1}:")
                        recipe.steps.getOrNull(currentStepIndex)?.let { step ->
                            appendLine("🍳 Step: ${step.description}")
                        }
                        appendLine("📋 Recipe: ${recipe.title}")
                        appendLine("")
                        appendLine("🔍 Please provide detailed feedback on what you see and guide me forward!")
                        appendLine("• Analyze the visual progress")
                        appendLine("• Suggest improvements or corrections")
                        appendLine("• Guide me to the next step")
                        appendLine("")
                        appendLine("📎 Image source: Gallery selection")
                    }
                    
                    // Process image with session manager first
                    val messageResult = sessionManager.processUserMessage(
                        message = photoMessage,
                        messageType = MessageType.IMAGE,
                        attachments = listOf(selectedUri.toString())
                    )
                    
                    // Then send to main chat if successful
                    if (messageResult.isSuccess) {
                        CoroutineScope(Dispatchers.Main).launch {
                            onSendMessage(photoMessage)
                        }
                        Log.d("CookingOverlay", "Gallery photo processed successfully")
                    } else {
                        Log.e("CookingOverlay", "Failed to process gallery photo: ${messageResult.exceptionOrNull()}")
                    }
                } catch (e: Exception) {
                    Log.e("CookingOverlay", "Failed to handle gallery photo", e)
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced progress header with fluid animations
        item {
            PremiumProgressHeader(
                recipe = recipe,
                progressPercentage = progressPercentage,
                currentStep = currentStepIndex + 1,
                totalSteps = totalSteps,
                completedSteps = completedSteps,
                isDarkTheme = isDarkTheme
            )
        }

        // Enhanced navigation with preview mode and session controls
        item {
            EnhancedStepNavigation(
                currentStep = currentStepIndex + 1,
                totalSteps = totalSteps,
                onPreviousStep = onPreviousStep,
                onNextStep = onNextStep,
                isPreviewMode = isPreviewMode,
                onTogglePreviewMode = onTogglePreviewMode,
                isSessionPaused = isSessionPaused,
                onPauseSession = onPauseSession,
                onResetSession = onResetSession,
                isDarkTheme = isDarkTheme,
                sessionManager = sessionManager
            )
        }

        // Premium step card with advanced interactions
        item {
            currentStep?.let { step ->
                PremiumStepCard(
                    step = step,
                    stepIndex = currentStepIndex,
                    completedSteps = completedSteps,
                    onCompleteStep = {
                        Log.d("CookingOverlay", "Completing step: ${step.id}")
                        onCompleteStep(step.id)
                    },
                    onSendMessage = onSendMessage,
                    onAutoProgress = {
                        Log.d("CookingOverlay", "onAutoProgress called - currentStepIndex: $currentStepIndex, totalSteps: $totalSteps")
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            // Send progress message to AI with silent recipe context
                            val progressMessage = buildString {
                                // Silent context (not displayed to user)
                                appendLine("RECIPE_CONTEXT: ${recipe.title}")
                                appendLine("TOTAL_STEPS: $totalSteps")
                                appendLine("CURRENT_STEP: ${currentStepIndex + 1}")
                                appendLine("COMPLETED_STEPS: ${completedSteps.size}")
                                appendLine("RECIPE_DIFFICULTY: ${recipe.difficulty}")
                                if (recipe.cuisine != null) appendLine("CUISINE: ${recipe.cuisine}")
                                
                                // User message
                                appendLine("I've completed step ${step.stepNumber}: ${step.description}.")
                                if (currentStepIndex < totalSteps - 1) {
                                    appendLine("Please check my progress and guide me for the next step.")
                                } else {
                                    appendLine("I've completed all cooking steps! Please provide final guidance and congratulations.")
                                }
                            }
                            
                            onSendMessage(progressMessage)
                            
                            if (currentStepIndex >= totalSteps - 1) {
                                Log.d("CookingOverlay", "Recipe completed! All steps done.")
                            }
                        }
                    },
                    onNeedHelp = {
                        val contextualHelp = buildString {
                            // Silent context (not displayed to user)
                            appendLine("RECIPE_CONTEXT: ${recipe.title}")
                            appendLine("TOTAL_STEPS: $totalSteps")
                            appendLine("CURRENT_STEP: ${currentStepIndex + 1}")
                            appendLine("COMPLETED_STEPS: ${completedSteps.size}")
                            appendLine("RECIPE_DIFFICULTY: ${recipe.difficulty}")
                            if (recipe.cuisine != null) appendLine("CUISINE: ${recipe.cuisine}")
                            if (step.techniques.isNotEmpty()) appendLine("REQUIRED_TECHNIQUES: ${step.techniques.joinToString(", ")}")
                            if (step.requiredEquipment.isNotEmpty()) appendLine("REQUIRED_EQUIPMENT: ${step.requiredEquipment.joinToString(", ")}")
                            
                            // User message
                            appendLine("I need help with cooking step ${step.stepNumber}: ${step.description}")
                            if (step.duration != null) appendLine("Expected duration: ${step.duration} minutes")
                            if (step.temperature != null) appendLine("Temperature: ${step.temperature}")
                            appendLine("Progress: ${completedSteps.size}/$totalSteps steps completed (${(progressPercentage * 100).toInt()}%)")
                            appendLine("Please provide specific guidance and tips for this step.")
                        }
                        onSendMessage(contextualHelp)
                    },
                    onRequestImage = {
                        // Use Gemini API for image generation
                        val scope = CoroutineScope(Dispatchers.Main)
                        scope.launch {
                            val imagePrompt = "Generate a visual reference showing exactly how step ${step.stepNumber} should look when completed. Recipe: ${recipe.title}. Step: ${step.description}"
                            try {
                                val result = geminiClient.generateContent(
                                    prompt = "Create an image description for: $imagePrompt",
                                    agentType = "cooking"
                                )
                                result.getOrNull()?.let { description ->
                                    // This would be passed to an image generation service
                                    val currentStep = progress?.currentStageIndex ?: 0
                                onRequestImage(description, currentStep)
                                }
                            } catch (e: Exception) {
                                Log.e("CookingOverlay", "Failed to generate image prompt", e)
                                onRequestImage(imagePrompt, currentStepIndex)
                            }
                        }
                    },
                    onCaptureStepPhoto = { photoUri ->
                        onShowImagePicker(true)
                    },
                    persistentTimers = persistentTimers,
                    mediaPlayer = mediaPlayer,
                    isPreviewMode = isPreviewMode,
                    isDarkTheme = isDarkTheme,
                    generatedImages = generatedImages,
                    recipe = recipe,
                    geminiClient = geminiClient
//                    showNutritionChart = showNutritionChart,
//                    userProfile = userProfile
                )
            }
        }

    }

    // Premium image picker modal
    if (showImagePicker) {
        PremiumImagePickerDialog(
            onDismiss = { onShowImagePicker(false) },
            onCamera = {
                cameraLauncher.launch(null)
                onShowImagePicker(false)
            },
            onGallery = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                onShowImagePicker(false)
            },
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun PremiumProgressHeader(
    recipe: Recipe,
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
                text = recipe.title,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            PremiumDifficultyChip(
                difficulty = recipe.difficulty,
                isDarkTheme = isDarkTheme
            )

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

        // Sophisticated progress visualization
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                // Background with subtle gradient
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        Color.Black.copy(alpha = 0.04f)
                    }
                ) {}

                // Dynamic progress fill with gradient
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
                                        Color(0xFF32D74B),
                                        Color(0xFF007AFF),
                                        Color(0xFF5856D6)
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

                    AnimatedStepIndicator(
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        stepNumber = index + 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStepIndicator(
    isCompleted: Boolean,
    isCurrent: Boolean,
    stepNumber: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.7f)
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFF32D74B)
            isCurrent -> Color(0xFF007AFF)
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
                color = Color(0xFF007AFF),
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

@Composable
private fun EnhancedStepNavigation(
    currentStep: Int,
    totalSteps: Int,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    isPreviewMode: Boolean,
    onTogglePreviewMode: (Boolean) -> Unit,
    isSessionPaused: Boolean,
    onPauseSession: () -> Unit,
    onResetSession: () -> Unit,
    isDarkTheme: Boolean,
    sessionManager: EnhancedCookingSessionManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Main navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous step button with enhanced visual feedback
            PremiumNavigationButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                enabled = true,
                onClick = {
                    Log.d("CookingOverlay", "Previous button clicked - currentStep: $currentStep, totalSteps: $totalSteps, isPreviewMode: $isPreviewMode")
                    val scope = CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        if (isPreviewMode) {
                            Log.d("CookingOverlay", "Preview mode - calling onPreviousStep")
                            onPreviousStep()
                        } else {
                            if (currentStep > 1) {
                                Log.d("CookingOverlay", "Going to previous step")
                                sessionManager.previousStep()
                            } else {
                                Log.d("CookingOverlay", "Cycling to last step")
                                // Cycle to last step - go forward to end
                                repeat(totalSteps - 1) {
                                    sessionManager.nextStep()
                                }
                            }
                        }
                    }
                },
                isDarkTheme = isDarkTheme
            )

            // Current step indicator with premium styling and mode indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isPreviewMode) {
                    Color(0xFF007AFF).copy(alpha = 0.1f)
                } else if (isDarkTheme) {
                    Color.White.copy(alpha = 0.08f)
                } else {
                    Color.Black.copy(alpha = 0.04f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isPreviewMode) {
                        Color(0xFF007AFF).copy(alpha = 0.3f)
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
                            Color(0xFF007AFF)
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
                            color = Color(0xFF007AFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Next step button
            PremiumNavigationButton(
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                enabled = true,
                onClick = {
                    Log.d("CookingOverlay", "Next button clicked - currentStep: $currentStep, totalSteps: $totalSteps, isPreviewMode: $isPreviewMode")
                    val scope = CoroutineScope(Dispatchers.Main)
                    scope.launch {
                        if (isPreviewMode) {
                            Log.d("CookingOverlay", "Preview mode - calling onNextStep")
                            onNextStep()
                        } else {
                            if (currentStep < totalSteps) {
                                Log.d("CookingOverlay", "Going to next step")
                                sessionManager.nextStep()
                            } else {
                                Log.d("CookingOverlay", "Cycling to first step")
                                // Cycle to first step - go back to beginning
                                repeat(totalSteps - 1) {
                                    sessionManager.previousStep()
                                }
                            }
                        }
                    }
                },
                isDarkTheme = isDarkTheme
            )
        }
        
        // Session controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview mode toggle
            SessionControlButton(
                icon = if (isPreviewMode) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                text = if (isPreviewMode) "Live" else "Preview",
                onClick = { onTogglePreviewMode(!isPreviewMode) },
                color = if (isPreviewMode) Color(0xFF007AFF) else Color(0xFF30D158),
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            // Pause/Resume button
            SessionControlButton(
                icon = if (isSessionPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                text = if (isSessionPaused) "Resume" else "Pause",
                onClick = onPauseSession,
                color = if (isSessionPaused) Color(0xFF32D74B) else Color(0xFFFF9F0A),
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            // Reset button
            SessionControlButton(
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
private fun SessionControlButton(
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

@Composable
private fun PremiumNavigationButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) {
            if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
        } else {
            if (isDarkTheme) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
        },
        animationSpec = tween(200)
    )

    Surface(
        onClick = {
            Log.d("CookingOverlay", "PremiumNavigationButton clicked - enabled: $enabled")
            onClick()
        },
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = backgroundColor,
        border = if (enabled) BorderStroke(
            1.dp,
            if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
        ) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) {
                    if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
                } else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PremiumStepCard(
    step: CookingStep,
    stepIndex: Int,
    completedSteps: Set<String>,
    onCompleteStep: () -> Unit,
    onAutoProgress: () -> Unit,
    onNeedHelp: () -> Unit,
    onRequestImage: () -> Unit,
    onCaptureStepPhoto: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    persistentTimers: MutableMap<String, PersistentTimerState>,
    mediaPlayer: MediaPlayer?,
    isPreviewMode: Boolean,
    isDarkTheme: Boolean,
    generatedImages: List<String> = emptyList(),
    recipe: Recipe,
    geminiClient: GeminiTextClient
//    showNutritionChart: Boolean,
//    userProfile: UserCookingProfile,
) {
    val isCompleted = step.id in completedSteps
    var showNutritionChart by remember { mutableStateOf(false) }
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
            if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium step header
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
                            text = step.title,
                            color = if (isDarkTheme) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (step.criticalStep) {
                            PremiumCriticalBadge(isDarkTheme = isDarkTheme)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.description,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            // Enhanced info chips with better spacing
            if (step.duration != null || step.temperature != null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    step.duration?.let { duration ->
                        item {
                            PremiumInfoChip(
                                icon = Icons.Rounded.Schedule,
                                text = "${duration}m",
                                color = Color(0xFF32D74B),
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                    step.temperature?.let { temp ->
                        item {
                            PremiumInfoChip(
                                icon = Icons.Rounded.LocalFireDepartment,
                                text = temp,
                                color = Color(0xFFFF9500),
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }

            // Premium action grid with enhanced UX
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UltraPremiumDoneButton(
                        isCompleted = isCompleted,
                        onMarkDone = onCompleteStep,
                        onAutoProgress = onAutoProgress,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(2f)
                    )

                    EnhancedCookingTimer(
                        duration = step.duration ?: 10,
                        stepId = step.id,
                        stepDescription = step.description,
                        persistentTimers = persistentTimers,
                        mediaPlayer = mediaPlayer,
                        isPreviewMode = isPreviewMode,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Secondary actions grid
                data class ActionItem(
                    val icon: ImageVector,
                    val text: String,
                    val color: Color,
                    val action: () -> Unit
                )

                val actions = listOf(
                    ActionItem(
                        icon = Icons.Rounded.PhotoCamera, 
                        text = "Photo", 
                        color = Color(0xFF00C7BE)
                    ) { 
                        Log.d("CookingOverlay", "Photo button clicked")
                        onCaptureStepPhoto("step_${step.id}")
                    },
                    ActionItem(
                        icon = Icons.AutoMirrored.Rounded.Help, 
                        text = "AI Help", 
                        color = Color(0xFFBF5AF2)
                    ) { 
                        Log.d("CookingOverlay", "AI Help button clicked")
                        onNeedHelp() 
                    },
                    ActionItem(
                        icon = Icons.Rounded.Visibility, 
                        text = "Visual", 
                        color = Color(0xFF30D158)
                    ) { 
                        Log.d("CookingOverlay", "Visual guide button clicked")
                        onRequestImage() 
                    },
                    ActionItem(
                        icon = Icons.Rounded.Info, 
                        text = "Details", 
                        color = Color(0xFFFF9F0A)
                    ) { 
                        Log.d("CookingOverlay", "Step details button clicked")
                        // Show comprehensive step details
                        val detailsMessage = buildString {
                            appendLine("📔 Complete Details for Step ${step.stepNumber}:")
                            appendLine("🍳 ${step.description}")
                            appendLine("")
                            
                            if (step.techniques.isNotEmpty()) {
                                appendLine("🔪 Techniques required:")
                                step.techniques.forEach { technique ->
                                    appendLine("• $technique")
                                }
                                appendLine("")
                            }
                            
                            if (step.visualCues.isNotEmpty()) {
                                appendLine("👀 Visual cues to look for:")
                                step.visualCues.forEach { cue ->
                                    appendLine("• $cue")
                                }
                                appendLine("")
                            }
                            
                            if (step.tips.isNotEmpty()) {
                                appendLine("💡 Pro tips:")
                                step.tips.forEach { tip ->
                                    appendLine("• $tip")
                                }
                                appendLine("")
                            }
                            
                            if (step.requiredEquipment.isNotEmpty()) {
                                appendLine("🍴️ Equipment needed:")
                                step.requiredEquipment.forEach { equipment ->
                                    appendLine("• $equipment")
                                }
                                appendLine("")
                            }
                            
                            if (step.duration != null) {
                                appendLine("⏱️ Expected duration: ${step.duration} minutes")
                            }
                            if (step.temperature != null) {
                                appendLine("🌡️ Temperature: ${step.temperature}")
                            }
                            if (step.criticalStep) {
                                appendLine("")
                                appendLine("⚠️ Critical step - extra attention required!")
                            }
                            if (step.canSkip) {
                                appendLine("🔄 This step is optional and can be skipped if needed")
                            }
                        }
                        onSendMessage(detailsMessage)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.forEach { (icon, text, color, action) ->
                        UltraPremiumActionButton(
                            icon = icon,
                            text = text,
                            onClick = action,
                            color = color,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            // Generated Images Gallery (if available)
            if (generatedImages.isNotEmpty()) {
                RecipeImageGallery(
                    images = generatedImages,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            // Nutrition Chart Toggle
            if (recipe.nutritionData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = showNutritionChart,
                        onCheckedChange = { showNutritionChart = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDarkTheme) GuideLensColors.DarkPrimary else GuideLensColors.LightPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Show Nutrition Information",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(
                    visible = showNutritionChart,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        NutritionChart(
                            nutritionData = recipe.nutritionData!!,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Personalized message with enhanced styling
//            userProfile?.let { profile ->
//                EnhancedPersonalizedMessage(
//                    profile = profile,
//                    recipe = recipe,
//                    isDarkTheme = isDarkTheme
//                )
//                Spacer(modifier = Modifier.height(20.dp))
//            }
        }
    }
}

@Composable
private fun PremiumCriticalBadge(isDarkTheme: Boolean) {
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
private fun PremiumInfoChip(
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

@Composable
private fun UltraPremiumDoneButton(
    isCompleted: Boolean,
    onMarkDone: () -> Unit,
    onAutoProgress: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) Color(0xFF30D158) else Color(0xFF007AFF),
        animationSpec = spring(dampingRatio = 0.8f)
    )

    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f)
    )

    Surface(
        onClick = {
            Log.d("CookingOverlay", "Mark Done button clicked - isCompleted: $isCompleted")
            if (!isCompleted) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                Log.d("CookingOverlay", "Calling onMarkDone and onAutoProgress")
                onMarkDone()
                onAutoProgress()
            } else {
                Log.d("CookingOverlay", "Step already completed, ignoring click")
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

@Composable
private fun EnhancedCookingTimer(
    duration: Int,
    stepId: String,
    stepDescription: String,
    persistentTimers: MutableMap<String, PersistentTimerState>,
    mediaPlayer: MediaPlayer?,
    isPreviewMode: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // Get or create persistent timer state
    val timerKey = "${stepId}_timer"
    val timerState = persistentTimers.getOrPut(timerKey) {
        PersistentTimerState(
            remainingTime = (duration * 60 * 1000).toLong(),
            originalDuration = (duration * 60 * 1000).toLong()
        )
    }
    
    var isRunning by remember(timerKey) { mutableStateOf(timerState.isRunning && !isPreviewMode) }
    var timeLeft by remember(timerKey) { mutableLongStateOf(timerState.remainingTime) }
    var isPaused by remember(timerKey) { mutableStateOf(timerState.isPaused) }
    var showTimeUpDialog by remember { mutableStateOf(false) }
    var showAlarmAnimation by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Cooking-themed animations
    val pulsatingScale by animateFloatAsState(
        targetValue = if (showAlarmAnimation) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val steamRotation by animateFloatAsState(
        targetValue = if (isRunning) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val progress = if (timerState.originalDuration > 0) {
        1f - (timeLeft.toFloat() / timerState.originalDuration.toFloat())
    } else 0f

    val timerColor = when {
        timeLeft == 0L -> Color(0xFFFF453A) // Red - time's up
        timeLeft < 60000L -> Color(0xFFFF9F0A) // Orange - less than 1 minute
        isRunning -> Color(0xFF30D158) // Green - running
        isPaused -> Color(0xFF007AFF) // Blue - paused
        else -> Color(0xFF8E8E93) // Gray - stopped
    }

    // Timer countdown effect
    LaunchedEffect(isRunning, isPreviewMode) {
        if (!isPreviewMode) {
            while (isRunning && timeLeft > 0) {
                delay(1000)
                timeLeft = (timeLeft - 1000).coerceAtLeast(0)
                // Update persistent state
                persistentTimers[timerKey] = timerState.copy(
                    remainingTime = timeLeft,
                    isRunning = isRunning
                )
            }
            if (timeLeft == 0L && isRunning) {
                isRunning = false
                showTimeUpDialog = true
                showAlarmAnimation = true
                // Play alarm sound
                try {
                    mediaPlayer?.start()
                } catch (e: Exception) {
                    Log.e("CookingTimer", "Failed to play alarm sound", e)
                }
                // Update persistent state
                persistentTimers[timerKey] = timerState.copy(
                    remainingTime = 0,
                    isRunning = false
                )
            }
        }
    }

    Surface(
        onClick = {
            if (!isPreviewMode) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                when {
                    timeLeft == 0L -> {
                        // Reset timer
                        timeLeft = timerState.originalDuration
                        isRunning = false
                        isPaused = false
                        showAlarmAnimation = false
                    }
                    isRunning -> {
                        // Pause timer
                        isRunning = false
                        isPaused = true
                    }
                    isPaused -> {
                        // Resume timer
                        isRunning = true
                        isPaused = false
                    }
                    else -> {
                        // Start timer
                        isRunning = true
                        isPaused = false
                    }
                }
                // Update persistent state
                persistentTimers[timerKey] = timerState.copy(
                    remainingTime = timeLeft,
                    isRunning = isRunning,
                    isPaused = isPaused
                )
            }
        },
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = pulsatingScale
                scaleY = pulsatingScale
            },
        shape = RoundedCornerShape(16.dp),
        color = timerColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, timerColor.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Animated progress background with cooking theme
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
                // Cooking-themed icon with animation
                Icon(
                    when {
                        timeLeft == 0L -> Icons.Rounded.NotificationImportant
                        isRunning -> Icons.Rounded.LocalFireDepartment
                        isPaused -> Icons.Rounded.Pause
                        else -> Icons.Rounded.Schedule
                    },
                    contentDescription = "Timer",
                    tint = timerColor,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            if (isRunning) rotationZ = steamRotation * 0.1f
                        }
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
                            isRunning -> "Cooking"
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

    // Enhanced time-up dialog with cooking theme
    if (showTimeUpDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTimeUpDialog = false
                showAlarmAnimation = false
            },
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Cooking Step Complete!", fontWeight = FontWeight.Bold)
                }
            },
            text = { 
                Text("Your ${duration}-minute timer for step '${stepDescription}' has finished. Check your cooking progress!") 
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showTimeUpDialog = false
                        showAlarmAnimation = false
                        mediaPlayer?.pause()
                    }
                ) {
                    Text("Got it!", color = Color(0xFF30D158))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun UltraPremiumActionButton(
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

@Composable
private fun PremiumProgressIndicator(
    recipe: Recipe,
    progress: ArtifactProgress?,
    isDarkTheme: Boolean,
    onExpand: () -> Unit
) {
    val currentStepIndex = progress?.currentStageIndex ?: 0
    val totalSteps = recipe.steps.size
    val progressPercentage = ((currentStepIndex + 1).toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

    Surface(
        onClick = onExpand,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp), // Reduced height for more compact design
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) {
            Color(0xFF2C2C2E).copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
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
            // Compact circular progress indicator with centered text
            Box(
                modifier = Modifier.size(40.dp), // Slightly smaller container
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF007AFF),
                    strokeWidth = 3.dp, // Thinner stroke for elegance
                    trackColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                )

                // Perfectly centered text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentStepIndex + 1}",
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 11.sp, // Slightly smaller for better proportion
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Recipe info - more compact layout
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp) // Tighter spacing
            ) {
                Text(
                    text = recipe.title,
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

                    // Progress percentage as a chip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF007AFF).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            color = Color(0xFF007AFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Expand indicator with subtle animation
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Expand",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp) // Smaller icon
            )
        }
    }
}

@Composable
private fun PremiumImagePickerDialog(
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    isDarkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Capture Step Progress",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Take a photo of your cooking progress to get personalized AI feedback and guidance.",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumImageOption(
                        icon = Icons.Rounded.PhotoCamera,
                        title = "Camera",
                        description = "Take Photo",
                        color = Color(0xFF00C7BE),
                        onClick = onCamera,
                        modifier = Modifier.weight(1f)
                    )

                    PremiumImageOption(
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

// Helper function to save bitmap to cache and return URI
private fun saveBitmapToCache(bitmap: Bitmap, artifactId: String, stepIndex: Int): String? {
    return try {
        // For production, implement actual file saving to cache directory
        // This would save to app-specific cache directory and return file:// URI
        val fileName = "${artifactId}_step_${stepIndex}_${System.currentTimeMillis()}.jpg"
        
        // In production, you would:
        // 1. Get app cache directory: context.cacheDir
        // 2. Create file and save bitmap
        // 3. Return file URI: File(cacheDir, fileName).toURI().toString()
        
        // For now, return a structured cache URI that includes bitmap metadata
        "cache://$fileName?width=${bitmap.width}&height=${bitmap.height}&config=${bitmap.config}"
    } catch (e: Exception) {
        Log.e("CookingOverlay", "Failed to save bitmap to cache", e)
        null
    }
}

@Composable
private fun PremiumImageOption(
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