package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.chat.ChatSessionManager
import com.craftflowtechnologies.guidelens.storage.ArtifactRepository
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactContent
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class SidebarTab {
    CHATS, ARTIFACTS, PROFILE, SETTINGS
}

@Composable
fun EnhancedChatSessionSidebar(
    isVisible: Boolean,
    selectedAgent: Agent,
    currentUser: User?,
    sessionManager: ChatSessionManager,
    toneManager: com.craftflowtechnologies.guidelens.personalization.ToneManager,
    localizationManager: com.craftflowtechnologies.guidelens.localization.GeneralLocalizationManager,
    zambianLocalizationManager: ZambianLocalizationManager,
    artifactRepository: ArtifactRepository,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionRename: (String, String) -> Unit,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    onApiTest: () -> Unit = {},
    onSettingsOpen: () -> Unit,
    onArtifactClick: (Artifact) -> Unit,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sessions by sessionManager.chatSessions.collectAsStateWithLifecycle()
    val currentSession by sessionManager.currentSession.collectAsStateWithLifecycle()
    val artifacts by remember { mutableStateOf(emptyList<ArtifactContent>()) }
    
    var selectedTab by remember { mutableStateOf(SidebarTab.CHATS) }
    val scope = rememberCoroutineScope()

    val userSessions = remember(sessions, currentUser, selectedAgent) {
        if (currentUser != null) {
            sessions.filter { it.userId == currentUser.id && it.agentId == selectedAgent.id }
        } else emptyList()
    }

    // Filter artifacts for current agent (eventually will be loaded from repository)
    val agentArtifacts = remember(artifacts, selectedAgent) {
        // TODO: Replace with actual artifact loading from artifactRepository
        emptyList<Artifact>()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(250)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDarkTheme) Color.Black.copy(alpha = 0.4f)
                        else Color(0xFF888888).copy(alpha = 0.2f)
                    )
                    .clickable { onClose() }
            )

            // Enhanced sidebar with tabbed interface
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                        spotColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
                    ),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                color = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFBFBFD),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color(0xFF1C1C1E),
                                        Color(0xFF2C2C2E),
                                        Color(0xFF1C1C1E)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFBFBFD),
                                        Color(0xFFF2F2F7),
                                        Color(0xFFFBFBFD)
                                    )
                                }
                            )
                        )
                ) {
                    // Header with agent info and close button
                    SidebarHeader(
                        selectedAgent = selectedAgent,
                        zambianLocalizationManager = zambianLocalizationManager,
                        onClose = onClose,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // Tab navigation
                    TabNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        chatCount = userSessions.size,
                        artifactCount = agentArtifacts.size,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // Tab content
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            SidebarTab.CHATS -> {
                                ChatsTabContent(
                                    sessions = userSessions,
                                    currentSession = currentSession,
                                    selectedAgent = selectedAgent,
                                    zambianLocalizationManager = zambianLocalizationManager,
                                    onNewChat = onNewChat,
                                    onSessionClick = onSessionClick,
                                    onSessionDelete = onSessionDelete,
                                    onSessionRename = onSessionRename,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                            SidebarTab.ARTIFACTS -> {
                                ArtifactsTabContent(
                                    artifacts = agentArtifacts,
                                    selectedAgent = selectedAgent,
                                    onArtifactClick = onArtifactClick,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                            SidebarTab.PROFILE -> {
                                ProfileTabContent(
                                    currentUser = currentUser,
                                    zambianLocalizationManager = zambianLocalizationManager,
                                    onThemeToggle = onThemeToggle,
                                    onSignOut = onSignOut,
                                    isDarkTheme = isDarkTheme
                                )
                            }
                            SidebarTab.SETTINGS -> {
                                SettingsTabContent(
                                    zambianLocalizationManager = zambianLocalizationManager,
                                    onSettingsOpen = onSettingsOpen,
                                    onApiTest = onApiTest,
                                    isDarkTheme = isDarkTheme
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
private fun SidebarHeader(
    selectedAgent: Agent,
    zambianLocalizationManager: ZambianLocalizationManager,
    onClose: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            selectedAgent.primaryColor.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (selectedAgent.id) {
                            "cooking" -> "👨‍🍳"
                            "crafting" -> "🎨" 
                            "diy" -> "🔧"
                            "buddy" -> "🤖"
                            else -> "🤖"
                        },
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    Text(
                        text = zambianLocalizationManager.getTraditionalGreeting(),
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                    )
                }
            }
            
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                )
            }
        }
    }
}

@Composable
private fun TabNavigation(
    selectedTab: SidebarTab,
    onTabSelected: (SidebarTab) -> Unit,
    chatCount: Int,
    artifactCount: Int,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TabButton(
                icon = Icons.Rounded.Chat,
                label = "Chats",
                count = chatCount,
                isSelected = selectedTab == SidebarTab.CHATS,
                onClick = { onTabSelected(SidebarTab.CHATS) },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            TabButton(
                icon = Icons.Rounded.Folder,
                label = "Files",
                count = artifactCount,
                isSelected = selectedTab == SidebarTab.ARTIFACTS,
                onClick = { onTabSelected(SidebarTab.ARTIFACTS) },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            TabButton(
                icon = Icons.Rounded.Person,
                label = "Profile",
                isSelected = selectedTab == SidebarTab.PROFILE,
                onClick = { onTabSelected(SidebarTab.PROFILE) },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            
            TabButton(
                icon = Icons.Rounded.Settings,
                label = "Settings",
                isSelected = selectedTab == SidebarTab.SETTINGS,
                onClick = { onTabSelected(SidebarTab.SETTINGS) },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabButton(
    icon: ImageVector,
    label: String,
    count: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF007AFF)
        } else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else {
                        if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                    },
                    modifier = Modifier.size(16.sp.value.dp)
                )
                
                if (count > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = if (count > 99) "99+" else count.toString(),
                            fontSize = 8.sp,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else {
                    if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                }
            )
        }
    }
}