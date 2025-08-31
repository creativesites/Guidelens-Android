package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    currentUser: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    userDataManager: UserDataManager,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPersonalization: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToApiTest: () -> Unit,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val zambianColors = com.craftflowtechnologies.guidelens.ui.ZambianColorScheme.default()
    val culturalSettings by zambianLocalizationManager.culturalSettings.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
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
            // Welcome Header
            item {
                WelcomeHeaderCard(
                    user = currentUser,
                    zambianLocalizationManager = zambianLocalizationManager,
                    isDarkTheme = isDarkTheme
                )
            }
            
            // Quick Actions
            item {
                QuickActionsSection(
                    onThemeToggle = onThemeToggle,
                    onNotificationToggle = { 
                        // Toggle notifications through cultural settings
                        scope.launch {
                            zambianLocalizationManager.updateCulturalSettings(
                                culturalSettings.copy(
                                    // Add notification toggle to cultural settings if needed
                                )
                            )
                        }
                    },
                    onNavigateToPersonalization = onNavigateToPersonalization,
                    isDarkTheme = isDarkTheme,
                    zambianColors = zambianColors
                )
            }
            
            // Account & Profile
            item {
                MainSettingsSection(
                    title = "Account & Profile",
                    icon = Icons.Rounded.Person
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.AccountCircle,
                        title = "Profile Settings",
                        subtitle = "Manage your profile and account information",
                        onClick = onNavigateToProfile,
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Language,
                        title = "Personalization",
                        subtitle = "Language, region, and cultural preferences",
                        onClick = onNavigateToPersonalization,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Privacy & Security
            item {
                MainSettingsSection(
                    title = "Privacy & Security",
                    icon = Icons.Rounded.Security
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "Privacy Settings",
                        subtitle = "Control your data and privacy preferences",
                        onClick = onNavigateToPrivacy,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // TODO: Implement data protection info
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.Shield,
                        title = "Data Protection",
                        subtitle = "View our privacy policy and data handling",
                        onClick = { /* TODO: Navigate to data protection info */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                    
                    // TODO: Implement data export functionality
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.Download,
                        title = "Export Data",
                        subtitle = "Download your conversations and saved content",
                        onClick = { /* TODO: Navigate to data export */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                }
            }
            
            // App Preferences
            item {
                MainSettingsSection(
                    title = "App Preferences",
                    icon = Icons.Rounded.Tune
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Notifications",
                        subtitle = "Manage notification preferences and quiet hours",
                        onClick = onNavigateToNotifications,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // Theme toggle inline
                    SettingsItem(
                        icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        title = "Theme",
                        subtitle = if (isDarkTheme) "Dark mode enabled" else "Light mode enabled",
                        onClick = onThemeToggle,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // TODO: Implement accessibility settings
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.Accessibility,
                        title = "Accessibility",
                        subtitle = "Voice controls, text size, and accessibility options",
                        onClick = { /* TODO: Navigate to accessibility settings */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                    
                    // TODO: Implement storage management
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.Storage,
                        title = "Storage",
                        subtitle = "Manage app storage and cache",
                        onClick = { /* TODO: Navigate to storage settings */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                }
            }
            
            // AI & Features - Only functional items
            item {
                MainSettingsSection(
                    title = "AI & Features",
                    icon = Icons.Rounded.Psychology
                ) {
                    // API Test is functional
                    SettingsItem(
                        icon = Icons.Rounded.Api,
                        title = "API Test",
                        subtitle = "Test API connections and responses",
                        onClick = onNavigateToApiTest,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // TODO: Implement agent preferences
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.SmartToy,
                        title = "Agent Preferences",
                        subtitle = "Customize AI agents and conversation styles",
                        onClick = { /* TODO: Navigate to agent preferences */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                    
                    // TODO: Implement voice settings
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.VoiceChat,
                        title = "Voice & Audio",
                        subtitle = "Configure voice interaction and audio settings",
                        onClick = { /* TODO: Navigate to voice settings */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                    
                    // TODO: Implement video settings
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.VideoCall,
                        title = "Video Features",
                        subtitle = "Camera permissions and video call settings",
                        onClick = { /* TODO: Navigate to video settings */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                }
            }
            
            // Support & About - Simplified version info only
            item {
                MainSettingsSection(
                    title = "About",
                    icon = Icons.Rounded.Info
                ) {
                    // Simple version display
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "GuideLens MVP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.White else Color.Black
                            )
                            Text(
                                text = "Version 1.0.0",
                                fontSize = 14.sp,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Real-time AI skills guidance",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    // TODO: Implement help and support
                    /*
                    SettingsItem(
                        icon = Icons.Rounded.HelpCenter,
                        title = "Help & Support",
                        subtitle = "Get help, contact support, and view FAQs",
                        onClick = { /* TODO: Navigate to help */ },
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.Feedback,
                        title = "Send Feedback",
                        subtitle = "Share your thoughts and suggestions",
                        onClick = { /* TODO: Navigate to feedback */ },
                        isDarkTheme = isDarkTheme
                    )
                    */
                }
            }
            
            // Developer Options - Removed as API Test is now in AI & Features section
            // TODO: Add debug options when needed
            
            // Account Actions
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeaderCard(
    user: User?,
    zambianLocalizationManager: ZambianLocalizationManager,
    isDarkTheme: Boolean
) {
    val greeting = zambianLocalizationManager.getTraditionalGreeting()
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
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            zambianColors.emeraldGreen.copy(alpha = 0.1f),
                            zambianColors.copperOrange.copy(alpha = 0.1f),
                            zambianColors.sunYellow.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    zambianColors.sunYellow.copy(alpha = 0.3f),
                                    zambianColors.emeraldGreen.copy(alpha = 0.1f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.avatarUrl != null || user?.profilePicture != null) {
                        // TODO: Load actual profile image
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = zambianColors.emeraldGreen
                        )
                    } else {
                        Text(
                            text = user?.name?.take(1)?.uppercase() ?: "U",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = zambianColors.emeraldGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                    )
                    
                    Text(
                        text = user?.name ?: "Welcome!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    
                    Text(
                        text = "Manage your GuideLens preferences",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onThemeToggle: () -> Unit,
    onNotificationToggle: () -> Unit,
    onNavigateToPersonalization: () -> Unit,
    isDarkTheme: Boolean,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    label = if (isDarkTheme) "Dark" else "Light",
                    onClick = onThemeToggle,
                    color = zambianColors.sunYellow,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                QuickActionButton(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Notifications",
                    onClick = onNotificationToggle,
                    color = zambianColors.riverBlue,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                QuickActionButton(
                    icon = Icons.Rounded.Language,
                    label = "Personalization",
                    onClick = onNavigateToPersonalization,
                    color = zambianColors.emeraldGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MainSettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Column {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
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
            
            // Section content
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1C1C1E), // Use dark theme color consistently
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    content()
                }
            }
        }
    }
}