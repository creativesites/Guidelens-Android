package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset


@Composable
fun TopBar(
    selectedAgent: Agent,
    isVoiceMode: Boolean,
    isVideoMode: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isConnected: Boolean,
    connectionStatus: String,
    onAgentClick: () -> Unit,
    onVoiceToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onMenuClick: () -> Unit = {},
    isDarkTheme: Boolean = true,
    isScrolled: Boolean = false, // Add this parameter to track scroll state
    stickyOffset: Int,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
) {
    // Calculate background color based on scroll state and theme
    val backgroundColor = when {
        isScrolled -> {
            if (isDarkTheme) {
                Color.Black.copy(alpha = 0.85f) // Semi-transparent dark
            } else {
                Color.White.copy(alpha = 0.90f) // Semi-transparent light
            }
        }
        else -> Color.Transparent
    }

    // Calculate elevation based on scroll state
    val elevation = if (isScrolled) 8.dp else 0.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),

        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .offset { IntOffset(0, stickyOffset) }
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left - Menu button with theme-aware icon
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.menu_bold),
                        contentDescription = "Menu",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                // Center - Agent selector
                MinimalAgentSelector(
                    selectedAgent = selectedAgent,
                    isConnected = isConnected,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    zambianLocalizationManager = zambianLocalizationManager,
                    onClick = onAgentClick,
                    isDarkTheme = isDarkTheme
                )

                // Right - Mode controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onVoiceToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.voice_mode),
                            contentDescription = "Voice Mode",
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    IconButton(
                        onClick = onVideoToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.video_mode),
                            contentDescription = "Video Mode",
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun MinimalIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.guideLensColors.iconTint
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 20.dp,
                    color = Color.Black.copy(alpha = 0.08f)
                ),
                onClick = onClick
            ),

        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.guideLensColors.textPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MinimalAgentSelector(
    selectedAgent: Agent,
    isConnected: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager,
    onClick: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Status indicator animation
    val statusAlpha by animateFloatAsState(
        targetValue = when {
            isListening || isSpeaking -> 1f
            isConnected -> 0.6f
            else -> 0.3f
        },
        animationSpec = tween(300),
        label = "statusAlpha"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent) // Very light gray
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = selectedAgent.primaryColor.copy(alpha = 0.12f)
                ),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Agent icon with status
        Box {
//            Box(
//                modifier = Modifier
//                    .size(32.dp)
//                    .background(
//                        color = selectedAgent.primaryColor.copy(alpha = 0.4f),
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
////
//
//                Image(
//                    painter = painterResource(id = com.craftflowtechnologies.guidelens.R.drawable.general_icon),
//                    contentDescription = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
//                    modifier = Modifier.size(24.dp)
//                )
//            }

            // Status dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = when {
                                isListening -> Color(0xFF10B981)
                                isSpeaking -> selectedAgent.primaryColor
                                isConnected -> Color(0xFF10B981)
                                else -> Color(0xFFD1D5DB)
                            }.copy(alpha = statusAlpha),
                            shape = CircleShape
                        )
                )
            }
        }

        // Agent name
        Text(
            text = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.guideLensColors.textPrimary
        )

        // Chevron
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.guideLensColors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MinimalToggleButton(
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.guideLensColors.cardBackground else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "bgColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.guideLensColors.textPrimary else MaterialTheme.guideLensColors.textTertiary,
        animationSpec = tween(200),
        label = "iconColor"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isActive) activeIcon else inactiveIcon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

        // Subtle pulse effect when active and in use
        if (isActive && (contentDescription == "Voice Mode" || contentDescription == "Video Mode")) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.guideLensColors.textPrimary.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )
        }
    }
}

// Alternative ultra-minimal design (optional)
@Composable
fun UltraMinimalTopBar(
    selectedAgent: Agent,
    isVoiceMode: Boolean,
    isVideoMode: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isConnected: Boolean,
    connectionStatus: String,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager,
    onAgentClick: () -> Unit,
    onVoiceToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left - Menu
        TextButton(
            onClick = onMenuClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF374151)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                modifier = Modifier.size(24.dp)
            )
        }

        // Center - Agent name with subtle indicator
        TextButton(
            onClick = onAgentClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color(0xFF111827)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection indicator
                Canvas(
                    modifier = Modifier.size(6.dp)
                ) {
                    drawCircle(
                        color = when {
                            isListening || isSpeaking -> Color(0xFF10B981)
                            isConnected -> Color(0xFF10B981).copy(alpha = 0.5f)
                            else -> Color(0xFFE5E7EB)
                        }
                    )
                }

                Text(
                    text = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.02).sp
                )
            }
        }

        // Right - Mode toggles
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isVoiceMode) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = "Voice Mode",
                tint = if (isVoiceMode) Color(0xFF111827) else Color(0xFFD1D5DB),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp),
                        onClick = onVoiceToggle
                    )
            )

            Icon(
                imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = "Video Mode",
                tint = if (isVideoMode) Color(0xFF111827) else Color(0xFFD1D5DB),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 16.dp),
                        onClick = onVideoToggle
                    )
            )
        }
    }
}
// Side Menu Data Classes
data class SideMenuItem(
    val title: String,
    val icon: ImageVector,
    val badge: String? = null,
    val action: SideMenuAction
)

sealed class SideMenuAction {
    object SessionHistory : SideMenuAction()
    object UserProfile : SideMenuAction()
    object Settings : SideMenuAction()
    object ThemeToggle : SideMenuAction()
    object About : SideMenuAction()
    object Feedback : SideMenuAction()
    object SignOut : SideMenuAction()
}

@Composable
fun SideMenu(
    isVisible: Boolean,
    selectedAgent: Agent,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager,
    onClose: () -> Unit,
    onMenuItemClick: (SideMenuAction) -> Unit,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val menuItems = remember(isDarkTheme) {
        listOf(
            SideMenuItem("Session History", Icons.Default.History, action = SideMenuAction.SessionHistory),
            SideMenuItem("Profile", Icons.Default.Person, action = SideMenuAction.UserProfile),
            SideMenuItem("Settings", Icons.Default.Settings, action = SideMenuAction.Settings),
            SideMenuItem(
                title = if (isDarkTheme) "Light Mode" else "Dark Mode",
                icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                action = SideMenuAction.ThemeToggle
            ),
            SideMenuItem("About", Icons.Default.Info, action = SideMenuAction.About),
            SideMenuItem("Feedback", Icons.Default.Feedback, action = SideMenuAction.Feedback),
            SideMenuItem("Sign Out", Icons.Default.Logout, action = SideMenuAction.SignOut)
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.guideLensColors.overlayBackground)
                    .clickable { onClose() }
            )

            // Side menu panel
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    ),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.guideLensColors.cardBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.guideLensColors.cardBackground,
                                    MaterialTheme.guideLensColors.gradientEnd
                                )
                            )
                        )
                ) {
                    // Header
                    SideMenuHeader(
                        selectedAgent = selectedAgent,
                        zambianLocalizationManager = zambianLocalizationManager,
                        onClose = onClose
                    )

                    // Menu items
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(menuItems) { item ->
                            SideMenuItemCard(
                                item = item,
                                selectedAgent = selectedAgent,
                                onClick = { onMenuItemClick(item.action) }
                            )
                        }
                    }

                    // Footer
                    SideMenuFooter()
                }
            }
        }
    }
}

@Composable
private fun SideMenuHeader(
    selectedAgent: Agent,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            selectedAgent.primaryColor.copy(alpha = 0.3f),
                            selectedAgent.secondaryColor.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            // Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile picture placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column {
                    Text(
                        text = zambianLocalizationManager.getLocalizedString("welcome"),
                        color = MaterialTheme.guideLensColors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Using ${zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id)}",
                        color = MaterialTheme.guideLensColors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SideMenuItemCard(
    item: SideMenuItem,
    selectedAgent: Agent,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = selectedAgent.primaryColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = selectedAgent.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title
                Text(
                    text = item.title,
                    color = MaterialTheme.guideLensColors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Badge or chevron
            if (item.badge != null) {
                Surface(
                    shape = CircleShape,
                    color = selectedAgent.primaryColor
                ) {
                    Text(
                        text = item.badge,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun SideMenuFooter() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GuideLens v1.0",
                color = MaterialTheme.guideLensColors.textTertiary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Made with ❤️ for you",
                color = MaterialTheme.guideLensColors.textTertiary,
                fontSize = 12.sp
            )
        }
    }
}