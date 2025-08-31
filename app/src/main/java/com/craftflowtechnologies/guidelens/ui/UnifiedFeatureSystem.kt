package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Enhanced feature system for both voice and video modes
@Composable
fun UnifiedFeatureOverlay(
    selectedAgent: Agent,
    isVideoMode: Boolean = false,
    isVisible: Boolean,
    onClose: () -> Unit,
    onFeatureClick: (Any) -> Unit,
    onVideoModeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
                .clickable { onClose() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable(enabled = false) { }, // Prevent click-through
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                selectedAgent.primaryColor,
                                                selectedAgent.secondaryColor
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = selectedAgent.icon,
                                    contentDescription = selectedAgent.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = "${selectedAgent.name} Features",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = if (isVideoMode) "Video Mode - Enhanced with Vision" else "Voice Mode",
                                    color = selectedAgent.primaryColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.Red.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Feature Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(selectedAgent.features) { feature ->
                            UnifiedFeatureCard(
                                feature = feature,
                                agent = selectedAgent,
                                isVideoMode = isVideoMode,
                                onClick = { onFeatureClick(feature) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mode Switch Button (only show in voice mode)
                    if (!isVideoMode) {
                        Button(
                            onClick = onVideoModeClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedAgent.primaryColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Mode",
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Switch to Video Mode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
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
fun UnifiedFeatureCard(
    feature: Any,
    agent: Agent,
    isVideoMode: Boolean,
    onClick: () -> Unit
) {
    val (title, description, icon) = when (feature) {
        is CookingFeature -> Triple(feature.title, feature.description, feature.icon)
        is CraftingProject -> Triple(feature.title, feature.difficulty, feature.icon)
        is FriendshipTool -> Triple(feature.title, feature.description, feature.icon)
        is DIYCategory -> Triple(feature.title, feature.description, feature.icon)
        else -> Triple("Unknown", "Unknown feature", Icons.Default.Help)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = agent.primaryColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = agent.primaryColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = agent.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            // Video enhancement indicator
            if (isVideoMode && shouldShowVideoEnhancement(feature)) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = agent.primaryColor.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "Vision Enhanced",
                            tint = agent.primaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Vision",
                            color = agent.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Determine which features should show video enhancement
private fun shouldShowVideoEnhancement(feature: Any): Boolean {
    return when (feature) {
        is CookingFeature -> feature.id in listOf("recipe_finder", "technique_guide", "cooking_timer")
        is CraftingProject -> true // All crafting projects benefit from vision
        is DIYCategory -> true // All DIY tasks benefit from vision
        is FriendshipTool -> feature.id in listOf("mood_tracker", "breathing_exercise")
        else -> false
    }
}

// Video-specific enhancements for features
@Composable
fun VideoEnhancedFeatureIndicator(
    feature: Any,
    agent: Agent
) {
    val enhancement = when (feature) {
        is CookingFeature -> when (feature.id) {
            "recipe_finder" -> "I can see your ingredients!"
            "technique_guide" -> "I'll watch your technique"
            "cooking_timer" -> "I'll monitor your cooking"
            else -> "Visual assistance available"
        }
        is CraftingProject -> "I can see your project progress"
        is DIYCategory -> "I can see what you're working on"
        is FriendshipTool -> when (feature.id) {
            "mood_tracker" -> "I can see how you're feeling"
            "breathing_exercise" -> "I'll do this with you"
            else -> "Visual support available"
        }
        else -> "Enhanced with vision"
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = agent.primaryColor.copy(alpha = 0.2f),
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = "Vision Enhanced",
                tint = agent.primaryColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = enhancement,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}