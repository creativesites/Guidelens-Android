package com.craftflowtechnologies.guidelens.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    settingsManager: SettingsManager,
    zambianLocalizationManager: ZambianLocalizationManager,
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val notificationSettings by settingsManager.notificationSettings.collectAsStateWithLifecycle()
    val zambianColors = com.craftflowtechnologies.guidelens.ui.ZambianColorScheme.default()
    
    var showTimePickerDialog by remember { mutableStateOf<TimePickerType?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Notifications",
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
            // Notification Status Header
            item {
                NotificationStatusCard(
                    notificationSettings = notificationSettings,
                    onToggleNotifications = {
                        settingsManager.updateNotificationSettings(
                            notificationSettings.copy(enabled = !notificationSettings.enabled)
                        )
                    },
                    zambianColors = zambianColors,
                    isDarkTheme = isDarkTheme
                )
            }
            
            // General Notifications
            item {
                SettingsGroupCard(
                    title = "General Notifications",
                    icon = Icons.Rounded.Notifications,
                    isDarkTheme = isDarkTheme
                ) {
                    ToggleSettingsItem(
                        title = "Push Notifications",
                        subtitle = "Receive notifications on this device",
                        icon = Icons.Rounded.NotificationsActive,
                        checked = notificationSettings.pushNotifications,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(pushNotifications = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    ToggleSettingsItem(
                        title = "Email Notifications",
                        subtitle = "Receive notifications via email",
                        icon = Icons.Rounded.Email,
                        checked = notificationSettings.emailNotifications,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(emailNotifications = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    ToggleSettingsItem(
                        title = "System Notifications",
                        subtitle = "App updates and system messages",
                        icon = Icons.Rounded.SystemUpdate,
                        checked = notificationSettings.systemNotifications,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(systemNotifications = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Chat Notifications
            item {
                SettingsGroupCard(
                    title = "Chat Notifications",
                    icon = Icons.Rounded.Chat,
                    isDarkTheme = isDarkTheme
                ) {
                    ToggleSettingsItem(
                        title = "Chat Messages",
                        subtitle = "Notifications for new messages and responses",
                        icon = Icons.Rounded.Message,
                        checked = notificationSettings.chatNotifications,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(chatNotifications = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Quiet Hours
            item {
                SettingsGroupCard(
                    title = "Quiet Hours",
                    icon = Icons.Rounded.Bedtime,
                    isDarkTheme = isDarkTheme
                ) {
                    ToggleSettingsItem(
                        title = "Enable Quiet Hours",
                        subtitle = "Disable notifications during specified times",
                        icon = Icons.Rounded.DoNotDisturb,
                        checked = notificationSettings.quietHoursEnabled,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(quietHoursEnabled = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    if (notificationSettings.quietHoursEnabled) {
                        QuietHoursSettings(
                            startTime = notificationSettings.quietHoursStart,
                            endTime = notificationSettings.quietHoursEnd,
                            weekendsOnly = notificationSettings.weekendsOnly,
                            onStartTimeClick = { showTimePickerDialog = TimePickerType.START },
                            onEndTimeClick = { showTimePickerDialog = TimePickerType.END },
                            onWeekendsOnlyChange = { enabled ->
                                settingsManager.updateNotificationSettings(
                                    notificationSettings.copy(weekendsOnly = enabled)
                                )
                            },
                            zambianColors = zambianColors,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
            
            // Sound & Vibration
            item {
                SettingsGroupCard(
                    title = "Sound & Vibration",
                    icon = Icons.Rounded.VolumeUp,
                    isDarkTheme = isDarkTheme
                ) {
                    ToggleSettingsItem(
                        title = "Sound",
                        subtitle = "Play notification sounds",
                        icon = Icons.Rounded.NotificationImportant,
                        checked = notificationSettings.sound,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(sound = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    ToggleSettingsItem(
                        title = "Vibration",
                        subtitle = "Vibrate for notifications",
                        icon = Icons.Rounded.Vibration,
                        checked = notificationSettings.vibration,
                        onCheckedChange = { enabled ->
                            settingsManager.updateNotificationSettings(
                                notificationSettings.copy(vibration = enabled)
                            )
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                    
                    if (notificationSettings.sound) {
                        SettingsItem(
                            icon = Icons.Rounded.MusicNote,
                            title = "Notification Sound",
                            subtitle = notificationSettings.notificationSound.replaceFirstChar { it.uppercase() },
                            onClick = { /* TODO: Open sound picker */ },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
            
            // Preview Section
            item {
                NotificationPreviewCard(
                    notificationSettings = notificationSettings,
                    zambianColors = zambianColors,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
    
    // Time Picker Dialogs
    showTimePickerDialog?.let { timePickerType ->
        TimePickerDialog(
            title = when (timePickerType) {
                TimePickerType.START -> "Quiet Hours Start"
                TimePickerType.END -> "Quiet Hours End"
            },
            initialTime = when (timePickerType) {
                TimePickerType.START -> notificationSettings.quietHoursStart
                TimePickerType.END -> notificationSettings.quietHoursEnd
            },
            onTimeSelected = { time ->
                settingsManager.updateNotificationSettings(
                    when (timePickerType) {
                        TimePickerType.START -> notificationSettings.copy(quietHoursStart = time)
                        TimePickerType.END -> notificationSettings.copy(quietHoursEnd = time)
                    }
                )
                showTimePickerDialog = null
            },
            onDismiss = { showTimePickerDialog = null },
            isDarkTheme = isDarkTheme
        )
    }
}

enum class TimePickerType {
    START, END
}

@Composable
private fun NotificationStatusCard(
    notificationSettings: SettingsManager.NotificationSettings,
    onToggleNotifications: () -> Unit,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (notificationSettings.enabled) 
                                zambianColors.emeraldGreen.copy(alpha = 0.2f)
                            else 
                                Color.Gray.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (notificationSettings.enabled) Icons.Rounded.NotificationsActive 
                        else Icons.Rounded.NotificationsOff,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (notificationSettings.enabled) zambianColors.emeraldGreen else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (notificationSettings.enabled) "Notifications Enabled" else "Notifications Disabled",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    
                    Text(
                        text = if (notificationSettings.enabled) 
                            "You'll receive notifications based on your preferences"
                        else 
                            "Turn on notifications to stay updated",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                    )
                }
                
                Switch(
                    checked = notificationSettings.enabled,
                    onCheckedChange = { onToggleNotifications() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = zambianColors.emeraldGreen,
                        checkedTrackColor = zambianColors.emeraldGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun QuietHoursSettings(
    startTime: String,
    endTime: String,
    weekendsOnly: Boolean,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onWeekendsOnlyChange: (Boolean) -> Unit,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeSettingButton(
                label = "Start Time",
                time = startTime,
                onClick = onStartTimeClick,
                modifier = Modifier.weight(1f),
                isDarkTheme = isDarkTheme
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            TimeSettingButton(
                label = "End Time",
                time = endTime,
                onClick = onEndTimeClick,
                modifier = Modifier.weight(1f),
                isDarkTheme = isDarkTheme
            )
        }
        
        ToggleSettingsItem(
            title = "Weekends Only",
            subtitle = "Apply quiet hours only on weekends",
            icon = Icons.Rounded.Weekend,
            checked = weekendsOnly,
            onCheckedChange = onWeekendsOnlyChange,
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun TimeSettingButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
            )
            
            Text(
                text = time,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkTheme) Color.White else Color.Black
            )
        }
    }
}

@Composable
private fun NotificationPreviewCard(
    notificationSettings: SettingsManager.NotificationSettings,
    zambianColors: com.craftflowtechnologies.guidelens.ui.ZambianColorScheme,
    isDarkTheme: Boolean
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
                text = "Notification Preview",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mock notification
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(zambianColors.emeraldGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = zambianColors.emeraldGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GuideLens • Cooking Assistant",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.6f)
                        )
                        
                        Text(
                            text = "Your nshima is ready! Check if it's cooked properly.",
                            fontSize = 14.sp,
                            color = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                    
                    if (notificationSettings.sound || notificationSettings.vibration) {
                        Row {
                            if (notificationSettings.sound) {
                                Icon(
                                    Icons.Rounded.VolumeUp,
                                    contentDescription = "Sound",
                                    modifier = Modifier.size(16.dp),
                                    tint = zambianColors.sunYellow
                                )
                            }
                            if (notificationSettings.vibration) {
                                Icon(
                                    Icons.Rounded.Vibration,
                                    contentDescription = "Vibration",
                                    modifier = Modifier.size(16.dp),
                                    tint = zambianColors.copperOrange
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This is how notifications will appear based on your settings",
                fontSize = 12.sp,
                color = if (isDarkTheme) Color.White.copy(0.5f) else Color.Black.copy(0.5f)
            )
        }
    }
}

@Composable
private fun TimePickerDialog(
    title: String,
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    // TODO: Implement proper time picker dialog
    // For now, using a simple dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text("Time picker implementation needed. Current time: $initialTime")
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(initialTime) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}