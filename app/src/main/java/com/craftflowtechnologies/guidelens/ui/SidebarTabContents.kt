package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactType
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatsTabContent(
    sessions: List<ChatSession>,
    currentSession: ChatSession?,
    selectedAgent: Agent,
    zambianLocalizationManager: ZambianLocalizationManager,
    onNewChat: () -> Unit,
    onSessionClick: (String) -> Unit,
    onSessionDelete: (String) -> Unit,
    onSessionRename: (String, String) -> Unit,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Search bar
        SearchBar(isDarkTheme = isDarkTheme)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // New chat button
        NewChatButton(
            onClick = onNewChat,
            agentName = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
            isDarkTheme = isDarkTheme
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Section header
        Text(
            text = zambianLocalizationManager.getLocalizedString("recent_chats").uppercase(),
            color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Chat sessions list
        if (sessions.isEmpty()) {
            EmptySessionsState(
                agentName = zambianLocalizationManager.getLocalizedAgentName(selectedAgent.id),
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    MainChatSessionItem(
                        session = session,
                        isSelected = currentSession?.id == session.id,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { onSessionDelete(session.id) },
                        onRename = { newName -> onSessionRename(session.id, newName) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun ArtifactsTabContent(
    artifacts: List<Artifact>,
    selectedAgent: Agent,
    onArtifactClick: (Artifact) -> Unit,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (selectedAgent.id) {
                    "cooking" -> Icons.Rounded.Restaurant
                    "crafting" -> Icons.Rounded.Build
                    "diy" -> Icons.Rounded.Handyman
                    else -> Icons.Rounded.Folder
                },
                contentDescription = null,
                tint = selectedAgent.primaryColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = when (selectedAgent.id) {
                        "cooking" -> "Recipes & Cooking Guides"
                        "crafting" -> "Projects & Patterns"
                        "diy" -> "Blueprints & Instructions"
                        else -> "Documents & Files"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                
                Text(
                    text = "${artifacts.size} items",
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                )
            }
        }
        
        if (artifacts.isEmpty()) {
            EmptyArtifactsState(
                agentType = selectedAgent.id,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(artifacts) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        onClick = { onArtifactClick(artifact) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileTabContent(
    currentUser: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    isDarkTheme: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // User profile card
            ProfileCard(
                user = currentUser,
                zambianLocalizationManager = zambianLocalizationManager,
                isDarkTheme = isDarkTheme
            )
        }
        
        item {
            // Quick settings
            QuickSettingsSection(
                zambianLocalizationManager = zambianLocalizationManager,
                onThemeToggle = onThemeToggle,
                isDarkTheme = isDarkTheme
            )
        }
        
        item {
            // Statistics
            UserStatsSection(
                zambianLocalizationManager = zambianLocalizationManager,
                isDarkTheme = isDarkTheme
            )
        }
        
        item {
            // Sign out button
            Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    zambianLocalizationManager: ZambianLocalizationManager,
    onSettingsOpen: () -> Unit,
    onApiTest: () -> Unit,
    isDarkTheme: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSection(
                title = "Personalization",
                icon = Icons.Rounded.Person
            ) {
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = "Language & Region",
                    subtitle = zambianLocalizationManager.currentLanguage.value.displayName,
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = "Cultural Settings",
                    subtitle = "Traditional greetings, local features",
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        
        item {
            SettingsSection(
                title = "App Settings",
                icon = Icons.Rounded.Settings
            ) {
                SettingsItem(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your notification preferences",
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Security,
                    title = "Privacy & Security",
                    subtitle = "Data and privacy controls",
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Storage,
                    title = "Data Management",
                    subtitle = "Manage your data and exports",
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        
        item {
            SettingsSection(
                title = "Developer",
                icon = Icons.Rounded.DeveloperMode
            ) {
                SettingsItem(
                    icon = Icons.Rounded.Api,
                    title = "API Test",
                    subtitle = "Test API connections",
                    onClick = onApiTest,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        
        item {
            SettingsSection(
                title = "About",
                icon = Icons.Rounded.Info
            ) {
                SettingsItem(
                    icon = Icons.Rounded.Help,
                    title = "Help & Support",
                    subtitle = "Get help and contact support",
                    onClick = onSettingsOpen,
                    isDarkTheme = isDarkTheme
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Update,
                    title = "Version",
                    subtitle = "GuideLens MVP v1.0.0",
                    onClick = { },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

// Helper components
@Composable
private fun SearchBar(isDarkTheme: Boolean) {
    var searchText by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        placeholder = { Text("Search conversations...") },
        leadingIcon = { 
            Icon(Icons.Default.Search, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF007AFF),
            unfocusedBorderColor = if (isDarkTheme) Color.White.copy(0.2f) else Color.Black.copy(0.2f)
        )
    )
}

@Composable
private fun NewChatButton(
    onClick: () -> Unit,
    agentName: String,
    isDarkTheme: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF007AFF)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("New Chat with $agentName")
    }
}

@Composable
private fun EmptySessionsState(
    agentName: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ChatBubble,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (isDarkTheme) Color.White.copy(0.3f) else Color.Black.copy(0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No conversations yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
        )
        
        Text(
            text = "Start a chat with $agentName to begin",
            fontSize = 14.sp,
            color = if (isDarkTheme) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
        )
    }
}

@Composable
private fun EmptyArtifactsState(
    agentType: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (agentType) {
                "cooking" -> Icons.Rounded.Restaurant
                "crafting" -> Icons.Rounded.Build
                "diy" -> Icons.Rounded.Handyman
                else -> Icons.Rounded.Folder
            },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (isDarkTheme) Color.White.copy(0.3f) else Color.Black.copy(0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (agentType) {
                "cooking" -> "No recipes saved yet"
                "crafting" -> "No projects saved yet"
                "diy" -> "No instructions saved yet"
                else -> "No files saved yet"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
        )
        
        Text(
            text = when (agentType) {
                "cooking" -> "Chat about recipes to start building your collection"
                "crafting" -> "Chat about projects to start building your collection"
                "diy" -> "Chat about repairs to start building your collection"
                else -> "Chat to create your first document"
            },
            fontSize = 14.sp,
            color = if (isDarkTheme) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
        )
    }
}