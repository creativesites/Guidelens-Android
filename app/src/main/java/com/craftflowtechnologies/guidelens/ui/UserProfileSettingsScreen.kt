package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSettingsScreen(
    currentUser: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdateProfile: (User) -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onDataExportClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    isDarkTheme: Boolean = true
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSignOutConfirmation by remember { mutableStateOf(false) }
    
    val zambianColors = com.craftflowtechnologies.guidelens.ui.ZambianColorScheme.default()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Profile Settings",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
                    titleContentColor = if (isDarkTheme) Color.White else Color.Black
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (isDarkTheme) Color(0xFF000000) else Color(0xFFF2F2F7)
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                ProfileHeaderCard(
                    user = currentUser,
                    zambianLocalizationManager = zambianLocalizationManager,
                    onEditClick = { showEditDialog = true },
                    isDarkTheme = isDarkTheme
                )
            }
            
            // Account Settings
            item {
                SettingsGroupCard(
                    title = "Account",
                    icon = Icons.Rounded.Person,
                    isDarkTheme = isDarkTheme
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.Edit,
                        title = "Edit Profile",
                        subtitle = "Update your name and profile information",
                        onClick = { showEditDialog = true },
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Password,
                        title = "Change Password",
                        subtitle = "Update your account password",
                        onClick = { /* TODO: Navigate to change password */ },
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Verified,
                        title = "Account Verification",
                        subtitle = "Verify your email and phone number",
                        onClick = { /* TODO: Navigate to verification */ },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Preferences
            item {
                SettingsGroupCard(
                    title = "Preferences",
                    icon = Icons.Rounded.Tune,
                    isDarkTheme = isDarkTheme
                ) {
                    PreferenceSettingsToggles(
                        user = currentUser,
                        onUserUpdate = onUpdateProfile,
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Privacy & Security
            item {
                SettingsGroupCard(
                    title = "Privacy & Security",
                    icon = Icons.Rounded.Security,
                    isDarkTheme = isDarkTheme
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Settings",
                        subtitle = "Control your data and privacy preferences",
                        onClick = onPrivacySettingsClick,
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Download,
                        title = "Export Data",
                        subtitle = "Download your data and conversation history",
                        onClick = onDataExportClick,
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Delete,
                        title = "Delete Data",
                        subtitle = "Remove your conversations and saved content",
                        onClick = { /* TODO: Navigate to data deletion */ },
                        isDarkTheme = isDarkTheme,
                        isDestructive = true
                    )
                }
            }
            
            // Notifications
            item {
                SettingsGroupCard(
                    title = "Notifications",
                    icon = Icons.Rounded.Notifications,
                    isDarkTheme = isDarkTheme
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notification Settings",
                        subtitle = "Manage your notification preferences",
                        onClick = onNotificationSettingsClick,
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Schedule,
                        title = "Quiet Hours",
                        subtitle = "Set times when notifications are disabled",
                        onClick = { /* TODO: Navigate to quiet hours */ },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Account Activity
            item {
                SettingsGroupCard(
                    title = "Account Activity",
                    icon = Icons.Rounded.History,
                    isDarkTheme = isDarkTheme
                ) {
                    AccountStatsCard(
                        user = currentUser,
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsItem(
                        icon = Icons.Rounded.Analytics,
                        title = "Usage Statistics",
                        subtitle = "View your app usage and activity",
                        onClick = { /* TODO: Navigate to usage stats */ },
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Devices,
                        title = "Active Sessions",
                        subtitle = "Manage your logged-in devices",
                        onClick = { /* TODO: Navigate to active sessions */ },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Account Actions
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sign Out Button
                Button(
                    onClick = { showSignOutConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
                        contentColor = if (isDarkTheme) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Delete Account Button
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF3B30)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Account", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    
    // Edit Profile Dialog
    if (showEditDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { updatedUser ->
                onUpdateProfile(updatedUser)
                showEditDialog = false
            },
            isDarkTheme = isDarkTheme
        )
    }
    
    // Sign Out Confirmation
    if (showSignOutConfirmation) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "Are you sure you want to sign out of your account?",
            confirmText = "Sign Out",
            onConfirm = {
                onSignOut()
                showSignOutConfirmation = false
            },
            onDismiss = { showSignOutConfirmation = false },
            isDestructive = false,
            isDarkTheme = isDarkTheme
        )
    }
    
    // Delete Account Confirmation
    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete Account",
            message = "This action cannot be undone. All your data, conversations, and saved content will be permanently deleted.",
            confirmText = "Delete Account",
            onConfirm = {
                onDeleteAccount()
                showDeleteConfirmation = false
            },
            onDismiss = { showDeleteConfirmation = false },
            isDestructive = true,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    user: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    onEditClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val zambianColors = com.craftflowtechnologies.guidelens.ui.ZambianColorScheme.default()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            zambianColors.emeraldGreen.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    zambianColors.sunYellow.copy(alpha = 0.3f),
                                    zambianColors.emeraldGreen.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(3.dp, zambianColors.emeraldGreen.copy(alpha = 0.3f), CircleShape)
                        .clickable { onEditClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.avatarUrl != null || user?.profilePicture != null) {
                        // TODO: Load actual profile image
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = zambianColors.emeraldGreen
                        )
                    } else {
                        Text(
                            text = user?.name?.take(1)?.uppercase() ?: "U",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = zambianColors.emeraldGreen
                        )
                    }
                    
                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .background(Color(0xFF007AFF), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // User Info
                Text(
                    text = user?.name ?: "Anonymous User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = user?.email ?: "No email",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Member since
                if (user?.createdAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = zambianColors.copperOrange
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = "Member since ${user.createdAt}",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSettingsToggles(
    user: User?,
    onUserUpdate: (User) -> Unit,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSettingsItem(
            title = "Voice Interaction",
            subtitle = "Enable voice commands and responses",
            icon = Icons.Rounded.Mic,
            checked = user?.preferences?.voiceEnabled ?: true,
            onCheckedChange = { enabled ->
                user?.let {
                    onUserUpdate(
                        it.copy(
                            preferences = it.preferences.copy(voiceEnabled = enabled)
                        )
                    )
                }
            },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Push Notifications",
            subtitle = "Receive notifications about updates and tips",
            icon = Icons.Rounded.NotificationsActive,
            checked = user?.preferences?.notifications ?: true,
            onCheckedChange = { enabled ->
                user?.let {
                    onUserUpdate(
                        it.copy(
                            preferences = it.preferences.copy(notifications = enabled)
                        )
                    )
                }
            },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun AccountStatsCard(
    user: User?,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Account Statistics",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Sessions",
                    value = "47",
                    icon = Icons.Rounded.Chat,
                    color = zambianColors.riverBlue,
                    isDarkTheme = isDarkTheme
                )
                
                StatItem(
                    title = "Saved Items",
                    value = "23",
                    icon = Icons.Rounded.Bookmark,
                    color = zambianColors.copperOrange,
                    isDarkTheme = isDarkTheme
                )
                
                StatItem(
                    title = "Days Active",
                    value = "12",
                    icon = Icons.Rounded.CalendarToday,
                    color = zambianColors.emeraldGreen,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun StatItem(
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
                .size(40.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color.Black
        )
        
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EditProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    isDarkTheme: Boolean
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Profile")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    user?.let {
                        onSave(it.copy(name = name, email = email))
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean,
    isDarkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = if (isDestructive) Color(0xFFFF3B30) else 
                    if (isDarkTheme) Color.White else Color.Black
            )
        },
        text = {
            Text(
                text = message,
                color = if (isDarkTheme) Color.White.copy(0.8f) else Color.Black.copy(0.8f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) Color(0xFFFF3B30) else Color(0xFF007AFF)
                )
            ) {
                Text(confirmText, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    isDestructive: Boolean = false
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
                tint = if (isDestructive) Color(0xFFFF3B30) else
                    if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFFF3B30) else
                        if (isDarkTheme) Color.White else Color.Black
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