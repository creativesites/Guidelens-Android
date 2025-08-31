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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.chat.ChatSessionManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatSessionSidebar(
    isVisible: Boolean,
    selectedAgent: Agent,
    currentUser: User?,
    sessionManager: ChatSessionManager,
    toneManager: com.craftflowtechnologies.guidelens.personalization.ToneManager,
    localizationManager: com.craftflowtechnologies.guidelens.localization.GeneralLocalizationManager,
    zambianLocalizationManager: com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionRename: (String, String) -> Unit,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    onApiTest: () -> Unit = {},
    onSettingsOpen: () -> Unit, // New callback for opening settings
    onArtifactsOpen: () -> Unit, // New callback for opening artifacts page
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sessions by sessionManager.chatSessions.collectAsStateWithLifecycle()
    val currentSession by sessionManager.currentSession.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val userSessions = remember(sessions, currentUser, selectedAgent, searchQuery) {
        if (currentUser != null) {
            val filteredSessions = sessions.filter { it.userId == currentUser.id && it.agentId == selectedAgent.id }
            if (searchQuery.isBlank()) {
                filteredSessions
            } else {
                filteredSessions.filter { session ->
                    session.name.contains(searchQuery, ignoreCase = true) ||
                    session.messages.any { message -> 
                        message.text.contains(searchQuery, ignoreCase = true) 
                    }
                }
            }
        } else emptyList()
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
            // Subtle background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDarkTheme) Color.Black.copy(alpha = 0.4f)
                        else Color(0xFF888888).copy(alpha = 0.2f)
                    )
                    .clickable { onClose() }
            )

            // Premium sidebar panel with glass morphism effect
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        spotColor = if (isDarkTheme) Color(0xFF3A3A3C) else Color(0xFFC6C6C8)
                    ),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = if (isDarkTheme) Color(0xFF1C1C1E) else Color(0xFFFBFBFD),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color(0xFF1C1C1E),
                                        Color(0xFF2C2C2E)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFBFBFD),
                                        Color(0xFFF2F2F7)
                                    )
                                }
                            )
                        )
                ) {
                    // Search bar at the top
                    SearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // Artifacts link section
                    ArtifactsSection(
                        onClick = onArtifactsOpen,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    // New chat button
                    NewChatButton(
                        onClick = onNewChat,
                        agentName = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
                        isDarkTheme = isDarkTheme
                    )

                    // Sessions list with fixed height container
                    Text(
                        text = "RECENT CHATS",
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )

                    // Fixed height scrollable container for chat history
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        if (userSessions.isEmpty()) {
                            EmptySessionsState(
                                agentName = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
                                isDarkTheme = isDarkTheme
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(userSessions, key = { it.id }) { session ->
                                    ChatSessionItem(
                                        session = session,
                                        isSelected = currentSession?.id == session.id,
                                        onClick = { onSessionClick(session.id) },
                                        onDelete = { onSessionDelete(session.id) },
                                        onRename = { newName -> onSessionRename(session.id, newName) },
                                        isDarkTheme = isDarkTheme,
                                        modifier = Modifier
                                    )
                                }
                            }
                        }
                    }

                    // User profile section at the bottom
                    UserProfileSection(
                        currentUser = currentUser,
                        onSettingsOpen = onSettingsOpen,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search conversations...",
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color(0xFF3C3C43).copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                    unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )
            
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactsSection(
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (isDarkTheme) Color(0xFF64D2FF).copy(alpha = 0.15f) else Color(0xFF007AFF).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Artifacts",
                    tint = if (isDarkTheme) Color(0xFF64D2FF) else Color(0xFF007AFF),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Recipes & Guides",
                    color = if (isDarkTheme) Color.White else Color(0xFF000000),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "View saved recipes and cooking guides",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View artifacts",
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NewChatButton(
    onClick: () -> Unit,
    agentName: String,
    isDarkTheme: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Chat",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "New Chat",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun UserProfileSection(
    currentUser: User?,
    onSettingsOpen: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF64D2FF),
                                Color(0xFF5E5CE6)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser?.name?.take(1)?.uppercase() ?: "U",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User name and email
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentUser?.name ?: "Unknown User",
                    color = if (isDarkTheme) Color.White else Color(0xFF000000),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (currentUser?.email != null) {
                    Text(
                        text = currentUser.email,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Settings button
            IconButton(
                onClick = onSettingsOpen,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                        else Color.Black.copy(alpha = 0.06f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (isDarkTheme) Color.White else Color(0xFF000000),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatSessionItem(
    session: ChatSession,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(session.name) }

    // Apple-style list item
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = when {
            isSelected && isDarkTheme -> Color(0xFF3A3A3C)
            isSelected && !isDarkTheme -> Color(0xFFE5E5EA)
            else -> Color.Transparent
        },
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat bubble icon
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Session content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isRenaming) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                } else {
                    Text(
                        text = session.name,
                        color = if (isDarkTheme) Color.White else Color(0xFF000000),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Session metadata in a row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${session.messages.size} messages",
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )

                        Text(
                            text = formatRelativeTime(session.updatedAt),
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!isRenaming) {
                // Options menu
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(
                            if (isDarkTheme) Color(0xFF3A3A3C) else Color.White
                        )
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Rename",
                                    fontSize = 13.sp,
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                            },
                            onClick = {
                                isRenaming = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    fontSize = 13.sp,
                                    color = if (isDarkTheme) Color.White else Color.Black
                                )
                            },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = if (isDarkTheme) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            } else {
                // Save button during rename
                IconButton(
                    onClick = {
                        onRename(renameText)
                        isRenaming = false
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySessionsState(
    agentName: String,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Forum,
            contentDescription = null,
            tint = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFFC6C6C8),
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Conversations",
            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF3C3C43),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start a new chat with your $agentName assistant to begin",
            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatRelativeTime(timestamp: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val date = format.parse(timestamp) ?: return timestamp
        val now = Date()
        val diff = now.time - date.time

        when {
            diff < 60_000 -> "Now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        timestamp
    }
}