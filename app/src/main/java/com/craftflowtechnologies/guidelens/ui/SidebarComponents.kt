package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactContent
import com.craftflowtechnologies.guidelens.storage.ArtifactType
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainChatSessionItem(
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
    var newName by remember { mutableStateOf(session.name) }
    
    Surface(
        onClick = { if (!isRenaming) onClick() },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            if (isDarkTheme) Color(0xFF007AFF).copy(alpha = 0.2f) else Color(0xFF007AFF).copy(alpha = 0.1f)
        } else Color.Transparent,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF007AFF).copy(alpha = 0.3f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRenaming) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = if (isDarkTheme) Color.White.copy(0.3f) else Color.Black.copy(0.3f)
                        )
                    )
                    
                    Row {
                        IconButton(
                            onClick = {
                                onRename(newName)
                                isRenaming = false
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Green)
                        }
                        
                        IconButton(
                            onClick = {
                                isRenaming = false
                                newName = session.name
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Red)
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White else Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = session.createdAt,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                        )
                    }
                    
                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu }
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    isRenaming = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
            
            if (session.messages.isNotEmpty() && !isRenaming) {
                val lastMessage = session.messages.lastOrNull()
                Text(
                    text = lastMessage?.text?.take(100) + if ((lastMessage?.text?.length ?: 0) > 100) "..." else "",
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.5f) else Color.Black.copy(0.5f),
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ArtifactCard(
    artifact: Artifact,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
            // Artifact type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (artifact.type) {
                            ArtifactType.RECIPE -> Color(0xFFFF6B35).copy(alpha = 0.2f)
                            ArtifactType.CRAFT_PROJECT -> Color(0xFF4ECDC4).copy(alpha = 0.2f)
                            ArtifactType.DIY_GUIDE -> Color(0xFF45B7D1).copy(alpha = 0.2f)
                            ArtifactType.LEARNING_MODULE -> Color(0xFF9B59B6).copy(alpha = 0.2f)
                            ArtifactType.SKILL_TUTORIAL -> Color(0xFF9B59B6).copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (artifact.type) {
                        ArtifactType.RECIPE -> Icons.Rounded.Restaurant
                        ArtifactType.CRAFT_PROJECT -> Icons.Rounded.Build
                        ArtifactType.DIY_GUIDE -> Icons.Rounded.Handyman
                        ArtifactType.LEARNING_MODULE -> Icons.Rounded.School
                        ArtifactType.SKILL_TUTORIAL -> Icons.Rounded.Psychology
                    },
                    contentDescription = null,
                    tint = when (artifact.type) {
                        ArtifactType.RECIPE -> Color(0xFFFF6B35)
                        ArtifactType.CRAFT_PROJECT -> Color(0xFF4ECDC4)
                        ArtifactType.DIY_GUIDE -> Color(0xFF45B7D1)
                        ArtifactType.LEARNING_MODULE -> Color(0xFF9B59B6)
                        ArtifactType.SKILL_TUTORIAL -> Color(0xFF9B59B6)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artifact.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = artifact.description ?: "No description",
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatDate(artifact.createdAt),
                    fontSize = 10.sp,
                    color = if (isDarkTheme) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
                )
            }
            
            if (artifact.tags.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF007AFF).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = artifact.tags.first(),
                        fontSize = 10.sp,
                        color = Color(0xFF007AFF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    user: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            zambianLocalizationManager.getZambianColors().emeraldGreen.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                zambianLocalizationManager.getZambianColors().sunYellow.copy(alpha = 0.3f),
                                zambianLocalizationManager.getZambianColors().emeraldGreen.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user?.profilePicture != null || user?.avatarUrl != null) {
                    // TODO: Load actual profile image
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = zambianLocalizationManager.getZambianColors().emeraldGreen
                    )
                } else {
                    Text(
                        text = user?.name?.take(1)?.uppercase() ?: "U",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = zambianLocalizationManager.getZambianColors().emeraldGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // User name
            Text(
                text = user?.name ?: "Anonymous User",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )
            
            // User location/region
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = zambianLocalizationManager.getZambianColors().copperOrange
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = zambianLocalizationManager.currentRegion.value.displayName,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                )
            }
            
            // Language
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Language,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = zambianLocalizationManager.getZambianColors().riverBlue
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = zambianLocalizationManager.currentLanguage.value.localName,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickSettingsSection(
    zambianLocalizationManager: ZambianLocalizationManager,
    onThemeToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Theme toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = zambianLocalizationManager.getZambianColors().sunYellow
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = if (isDarkTheme) "Dark Theme" else "Light Theme",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                }
                
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onThemeToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = zambianLocalizationManager.getZambianColors().emeraldGreen,
                        checkedTrackColor = zambianLocalizationManager.getZambianColors().emeraldGreen.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Traditional greetings toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EmojiEmotions,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = zambianLocalizationManager.getZambianColors().maizeGold
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Traditional Greetings",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                }
                
                Switch(
                    checked = zambianLocalizationManager.culturalSettings.value.useTraditionalGreetings,
                    onCheckedChange = { enabled ->
                        zambianLocalizationManager.updateCulturalSettings(
                            zambianLocalizationManager.culturalSettings.value.copy(
                                useTraditionalGreetings = enabled
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = zambianLocalizationManager.getZambianColors().emeraldGreen,
                        checkedTrackColor = zambianLocalizationManager.getZambianColors().emeraldGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun UserStatsSection(
    zambianLocalizationManager: ZambianLocalizationManager,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Activity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Chats",
                    value = "24",
                    icon = Icons.Rounded.Chat,
                    color = zambianLocalizationManager.getZambianColors().riverBlue,
                    isDarkTheme = isDarkTheme
                )
                
                StatCard(
                    title = "Recipes",
                    value = "8",
                    icon = Icons.Rounded.Restaurant,
                    color = zambianLocalizationManager.getZambianColors().copperOrange,
                    isDarkTheme = isDarkTheme
                )
                
                StatCard(
                    title = "Projects",
                    value = "12",
                    icon = Icons.Rounded.Build,
                    color = zambianLocalizationManager.getZambianColors().emeraldGreen,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isDarkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color.Black
        )
        
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF007AFF)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF007AFF)
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isDarkTheme) Color.White.copy(0.4f) else Color.Black.copy(0.4f)
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}