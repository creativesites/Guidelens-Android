package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Enhanced Voice Animations for GuideLens
 * Provides visual feedback for voice interactions with beautiful animations
 */

@Composable
fun EnhancedVoiceWaveAnimation(
    isUserSpeaking: Boolean,
    isAISpeaking: Boolean,
    voiceActivityLevel: Float = 0.5f,
    primaryColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Wave animation parameters
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isUserSpeaking || isAISpeaking) 1000 else 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isUserSpeaking || isAISpeaking) 800 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 6
        val maxRadius = size.minDimension / 3
        
        when {
            isUserSpeaking -> {
                drawUserSpeakingAnimation(
                    center = center,
                    baseRadius = baseRadius,
                    maxRadius = maxRadius,
                    activityLevel = voiceActivityLevel,
                    phase = wavePhase,
                    pulseScale = pulseScale,
                    color = androidx.compose.ui.graphics.Color(0xFF10B981) // Green for user
                )
            }
            isAISpeaking -> {
                drawAISpeakingAnimation(
                    center = center,
                    baseRadius = baseRadius,
                    maxRadius = maxRadius,
                    activityLevel = voiceActivityLevel,
                    phase = wavePhase,
                    pulseScale = pulseScale,
                    color = primaryColor
                )
            }
            else -> {
                drawIdleAnimation(
                    center = center,
                    baseRadius = baseRadius,
                    phase = wavePhase,
                    color = primaryColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun DrawScope.drawUserSpeakingAnimation(
    center: Offset,
    baseRadius: Float,
    maxRadius: Float,
    activityLevel: Float,
    phase: Float,
    pulseScale: Float,
    color: androidx.compose.ui.graphics.Color
) {
    // Draw concentric circles with voice activity
    for (i in 0..4) {
        val radius = baseRadius + (i * 20.dp.toPx()) * activityLevel * pulseScale
        val alpha = (0.8f - i * 0.15f) * activityLevel
        
        if (alpha > 0f) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
    
    // Draw audio bars around the circle
    val barCount = 12
    for (i in 0 until barCount) {
        val angle = (i * 2 * PI / barCount) + phase
        val barHeight = 15.dp.toPx() + (sin(angle + phase) * 10.dp.toPx() * activityLevel)
        
        val startRadius = baseRadius + 30.dp.toPx()
        val endRadius = startRadius + barHeight
        
        val startX = center.x + (cos(angle.toDouble()) * startRadius).toFloat()
        val startY = center.y + (sin(angle.toDouble()) * startRadius).toFloat()
        val endX = center.x + (cos(angle.toDouble()) * endRadius).toFloat()
        val endY = center.y + (sin(angle.toDouble()) * endRadius).toFloat()
        
        drawLine(
            color = color.copy(alpha = 0.7f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    
    // Central pulse
    drawCircle(
        color = color.copy(alpha = 0.6f),
        radius = baseRadius * pulseScale,
        center = center
    )
}

private fun DrawScope.drawAISpeakingAnimation(
    center: Offset,
    baseRadius: Float,
    maxRadius: Float,
    activityLevel: Float,
    phase: Float,
    pulseScale: Float,
    color: androidx.compose.ui.graphics.Color
) {
    // AI speaking uses flowing wave patterns
    val waveCount = 8
    for (i in 0 until waveCount) {
        val waveRadius = baseRadius + (i * 15.dp.toPx()) + 
                        (sin(phase + i * 0.5f) * 20.dp.toPx() * activityLevel)
        val alpha = (0.9f - i * 0.1f) * activityLevel
        
        if (alpha > 0f) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = waveRadius * pulseScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
    
    // Draw particle-like dots that move in spiral
    val particleCount = 16
    for (i in 0 until particleCount) {
        val angle = (i * 2 * PI / particleCount) + phase
        val radius = baseRadius + 40.dp.toPx() + (sin(phase * 2 + i) * 20.dp.toPx())
        
        val x = center.x + (cos(angle.toDouble()) * radius).toFloat()
        val y = center.y + (sin(angle.toDouble()) * radius).toFloat()
        
        val particleSize = 3.dp.toPx() + (sin(phase + i) * 2.dp.toPx())
        
        drawCircle(
            color = color.copy(alpha = 0.8f),
            radius = particleSize,
            center = Offset(x, y)
        )
    }
    
    // Central glow
    drawCircle(
        color = color.copy(alpha = 0.4f),
        radius = baseRadius * pulseScale * 1.5f,
        center = center,
        style = Stroke(width = 6.dp.toPx())
    )
}

private fun DrawScope.drawIdleAnimation(
    center: Offset,
    baseRadius: Float,
    phase: Float,
    color: androidx.compose.ui.graphics.Color
) {
    // Gentle breathing animation
    val breathScale = 1f + sin(phase * 0.5f) * 0.1f
    
    drawCircle(
        color = color,
        radius = baseRadius * breathScale,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // Subtle inner glow
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = baseRadius * breathScale * 0.7f,
        center = center
    )
}

@Composable
fun VoiceActivityIndicator(
    isUserSpeaking: Boolean,
    isAISpeaking: Boolean,
    activityLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val density = LocalDensity.current
    
    val indicatorScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val barHeight = with(density) {
                if (isUserSpeaking || isAISpeaking) {
                    val basePx = 20.dp.toPx()
                    val variationPx = sin((index * 0.5f) + (infiniteTransition.animateFloat(
                        0f, 2 * PI.toFloat(),
                        infiniteRepeatable(tween(800, easing = LinearEasing))
                    ).value)).toFloat() * 15.dp.toPx() * activityLevel
                    (basePx + variationPx).toDp()
                } else {
                    8.dp
                }
            }
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .scale(if (isUserSpeaking || isAISpeaking) indicatorScale else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            isUserSpeaking -> androidx.compose.ui.graphics.Color(0xFF10B981)
                            isAISpeaking -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

@Composable
fun SpeechBubbleAnimation(
    isVisible: Boolean,
    text: String,
    isUserSpeech: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
    ) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = if (isUserSpeech) {
                androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            },
            shadowElevation = 8.dp
        ) {
            androidx.compose.material3.Text(
                text = text,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun VoiceModeBackground(
    isUserSpeaking: Boolean,
    isAISpeaking: Boolean,
    primaryColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isUserSpeaking || isAISpeaking) 3000 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val colors = when {
            isUserSpeaking -> listOf(
                androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f),
                androidx.compose.ui.graphics.Color(0xFF059669).copy(alpha = 0.05f),
                androidx.compose.ui.graphics.Color.Transparent
            )
            isAISpeaking -> listOf(
                primaryColor.copy(alpha = 0.1f),
                primaryColor.copy(alpha = 0.05f),
                androidx.compose.ui.graphics.Color.Transparent
            )
            else -> listOf(
                primaryColor.copy(alpha = 0.03f),
                androidx.compose.ui.graphics.Color.Transparent,
                androidx.compose.ui.graphics.Color.Transparent
            )
        }
        
        drawRect(
            brush = Brush.radialGradient(
                colors = colors,
                center = Offset(size.width * 0.3f + gradientOffset % size.width, size.height * 0.5f),
                radius = size.maxDimension * 0.8f
            )
        )
        
        drawRect(
            brush = Brush.radialGradient(
                colors = colors.reversed(),
                center = Offset(size.width * 0.7f - gradientOffset % size.width, size.height * 0.7f),
                radius = size.maxDimension * 0.6f
            )
        )
    }
}