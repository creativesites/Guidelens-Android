package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Production-ready loading states with Zambian theming
 */
@Composable
fun ZambianLoadingIndicator(
    modifier: Modifier = Modifier,
    size: LoadingSize = LoadingSize.MEDIUM,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    message: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.dimension)
                .rotate(rotation)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            zambianColors.emeraldGreen,
                            zambianColors.sunYellow,
                            zambianColors.copperOrange,
                            zambianColors.riverBlue,
                            zambianColors.emeraldGreen
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size.dimension * 0.7f)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )
        }
        
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = size.textSize,
                color = zambianColors.emeraldGreen,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class LoadingSize(val dimension: androidx.compose.ui.unit.Dp, val textSize: androidx.compose.ui.unit.TextUnit) {
    SMALL(24.dp, 12.sp),
    MEDIUM(48.dp, 14.sp),
    LARGE(72.dp, 16.sp)
}

/**
 * Production-ready error states with retry functionality
 */
@Composable
fun ZambianErrorState(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error icon with animation
            val pulseAnimation = rememberInfiniteTransition(label = "pulse")
            val pulse by pulseAnimation.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        error.color.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    error.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { 
                            scaleX = pulse
                            scaleY = pulse
                        },
                    tint = error.color
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = error.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error.description,
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                onDismiss?.let {
                    OutlinedButton(
                        onClick = it,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) Color.White else Color.Black
                        )
                    ) {
                        Text("Dismiss")
                    }
                }
                
                onRetry?.let {
                    Button(
                        onClick = it,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = zambianColors.emeraldGreen
                        )
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

/**
 * App error types with Zambian color theming
 */
sealed class AppError(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    data class NetworkError(val zambianColors: ZambianColorScheme) : AppError(
        title = "Connection Problem",
        description = "Please check your internet connection and try again.",
        icon = Icons.Rounded.CloudOff,
        color = zambianColors.eagleRed
    )
    
    data class ServerError(val zambianColors: ZambianColorScheme) : AppError(
        title = "Server Issue",
        description = "Our servers are experiencing issues. Please try again in a few minutes.",
        icon = Icons.Rounded.Storage,
        color = zambianColors.copperOrange
    )
    
    data class AuthError(val zambianColors: ZambianColorScheme) : AppError(
        title = "Authentication Required",
        description = "Please sign in to continue using GuideLens.",
        icon = Icons.Rounded.Lock,
        color = zambianColors.riverBlue
    )
    
    data class PermissionError(val zambianColors: ZambianColorScheme) : AppError(
        title = "Permission Required",
        description = "GuideLens needs permission to access your camera and microphone.",
        icon = Icons.Rounded.Security,
        color = zambianColors.sunYellow
    )
    
    data class GeneralError(val message: String, val zambianColors: ZambianColorScheme) : AppError(
        title = "Something went wrong",
        description = message.ifEmpty { "An unexpected error occurred. Please try again." },
        icon = Icons.Rounded.Error,
        color = zambianColors.eagleRed
    )
}

/**
 * Empty state components with Zambian theming
 */
@Composable
fun ZambianEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    zambianColors.emeraldGreen.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = zambianColors.emeraldGreen.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            fontSize = 14.sp,
            color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = zambianColors.emeraldGreen
                )
            ) {
                Text(actionText)
            }
        }
    }
}

/**
 * Success state with celebration animation
 */
@Composable
fun ZambianSuccessState(
    title: String,
    description: String? = null,
    onDismiss: (() -> Unit)? = null,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    autoHide: Boolean = true,
    autoHideDelay: Long = 3000L
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Auto hide effect
    if (autoHide) {
        LaunchedEffect(Unit) {
            delay(autoHideDelay)
            isVisible = false
            delay(300) // Wait for animation
            onDismiss?.invoke()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(tween(300))
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = zambianColors.emeraldGreen.copy(alpha = 0.9f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated success icon
                val bounceAnimation = rememberInfiniteTransition(label = "bounce")
                val bounce by bounceAnimation.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bounce"
                )
                
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = bounce
                            scaleY = bounce
                        },
                    tint = Color.White
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    description?.let {
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = Color.White.copy(0.9f)
                        )
                    }
                }
                
                onDismiss?.let {
                    IconButton(
                        onClick = { 
                            isVisible = false
                            it()
                        }
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced toast-like notifications
 */
@Composable
fun ZambianToast(
    message: String,
    type: ToastType = ToastType.INFO,
    duration: ToastDuration = ToastDuration.SHORT,
    onDismiss: () -> Unit,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(duration.millis)
        isVisible = false
        delay(300)
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = type.backgroundColor(zambianColors),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    type.icon,
                    contentDescription = null,
                    tint = type.iconColor(zambianColors),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = type.textColor(zambianColors)
                )
            }
        }
    }
}

enum class ToastType(
    val icon: ImageVector,
    val backgroundColor: (ZambianColorScheme) -> Color,
    val iconColor: (ZambianColorScheme) -> Color,
    val textColor: (ZambianColorScheme) -> Color
) {
    SUCCESS(
        icon = Icons.Rounded.CheckCircle,
        backgroundColor = { it.emeraldGreen.copy(alpha = 0.9f) },
        iconColor = { Color.White },
        textColor = { Color.White }
    ),
    ERROR(
        icon = Icons.Rounded.Error,
        backgroundColor = { it.eagleRed.copy(alpha = 0.9f) },
        iconColor = { Color.White },
        textColor = { Color.White }
    ),
    WARNING(
        icon = Icons.Rounded.Warning,
        backgroundColor = { it.sunYellow.copy(alpha = 0.9f) },
        iconColor = { Color.Black },
        textColor = { Color.Black }
    ),
    INFO(
        icon = Icons.Rounded.Info,
        backgroundColor = { it.riverBlue.copy(alpha = 0.9f) },
        iconColor = { Color.White },
        textColor = { Color.White }
    )
}

enum class ToastDuration(val millis: Long) {
    SHORT(2000L),
    LONG(4000L),
    EXTRA_LONG(6000L)
}

/**
 * Production-ready skeleton loading components
 */
@Composable
fun ZambianSkeleton(
    modifier: Modifier = Modifier,
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    isDarkTheme: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier
            .background(
                color = if (isDarkTheme) 
                    Color.White.copy(alpha = alpha * 0.1f)
                else 
                    Color.Black.copy(alpha = alpha * 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
    )
}

@Composable
fun ChatMessageSkeleton(
    zambianColors: ZambianColorScheme = ZambianColorScheme.default(),
    isDarkTheme: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ZambianSkeleton(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            ZambianSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp),
                zambianColors = zambianColors,
                isDarkTheme = isDarkTheme
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ZambianSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                zambianColors = zambianColors,
                isDarkTheme = isDarkTheme
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            ZambianSkeleton(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp),
                zambianColors = zambianColors,
                isDarkTheme = isDarkTheme
            )
        }
    }
}