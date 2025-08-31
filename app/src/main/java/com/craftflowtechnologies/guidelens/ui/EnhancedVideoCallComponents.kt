package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.draw.scale





@Composable
fun AgentVideoAvatar(
    agent: Agent,
    isListening: Boolean,
    isSpeaking: Boolean,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening || isSpeaking) 1000 else 2000,
                easing = androidx.compose.animation.core.EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )
    
    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isSpeaking) 800 else 1500,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (isListening || isSpeaking) pulseScale else 1f)
            .offset(y = 8.dp), // Adjust this value to lower more or less
        contentAlignment = Alignment.Center
    ) {
        // Background animation when active
        if (isListening || isSpeaking) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = androidx.compose.ui.geometry.Offset(this.size.width / 2, this.size.height / 2)
                val radius = minOf(this.size.width, this.size.height) / 4
                
                repeat(3) { index ->
                    val animatedRadius = radius * (1f + 0.3f * kotlin.math.sin(waveAnimation + index * 0.5f))
                    drawCircle(
                        color = agent.primaryColor.copy(alpha = 0.2f - (index * 0.05f)),
                        radius = animatedRadius,
                        center = center
                    )
                }
            }
        }
        
        // Main avatar
        Box(
            modifier = Modifier
                .size(size * 0.7f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            agent.primaryColor,
                            agent.secondaryColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = agent.icon,
                contentDescription = agent.name,
                tint = MaterialTheme.guideLensColors.textPrimary,
                modifier = Modifier.size(size * 0.4f)
            )
        }
    }
}