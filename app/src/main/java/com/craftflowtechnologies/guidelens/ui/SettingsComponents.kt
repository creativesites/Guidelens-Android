package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.items

data class SettingsSection(
    val title: String,
    val items: List<SettingsItem>
)

data class SettingsItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val action: SettingsAction,
    val enabled: Boolean = true
)

sealed class SettingsAction {
    object Navigation : SettingsAction()
    data class Toggle(val isEnabled: Boolean, val onToggle: (Boolean) -> Unit) : SettingsAction()
    data class Selection(val currentValue: String, val onSelect: () -> Unit) : SettingsAction()
    data class Action(val onClick: () -> Unit) : SettingsAction()
}

@Composable
fun SettingsScreen(
    user: User,
    onUserUpdate: (User) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    val sections = remember(user) {
        listOf(
            SettingsSection(
                title = "Account",
                items = listOf(
                    SettingsItem(
                        title = "Profile",
                        subtitle = "Edit your profile information",
                        icon = Icons.Default.Person,
                        action = SettingsAction.Action { onNavigateToProfile() }
                    ),
                    SettingsItem(
                        title = "Notifications",
                        subtitle = if (user.preferences.notifications) "Enabled" else "Disabled",
                        icon = Icons.Default.Notifications,
                        action = SettingsAction.Toggle(
                            isEnabled = user.preferences.notifications,
                            onToggle = { enabled ->
                                onUserUpdate(
                                    user.copy(
                                        preferences = user.preferences.copy(notifications = enabled)
                                    )
                                )
                            }
                        )
                    ),
                    SettingsItem(
                        title = "Voice Settings",
                        subtitle = if (user.preferences.voiceEnabled) "Enabled" else "Disabled",
                        icon = Icons.Default.Mic,
                        action = SettingsAction.Toggle(
                            isEnabled = user.preferences.voiceEnabled,
                            onToggle = { enabled ->
                                onUserUpdate(
                                    user.copy(
                                        preferences = user.preferences.copy(voiceEnabled = enabled)
                                    )
                                )
                            }
                        )
                    )
                )
            ),
            SettingsSection(
                title = "Preferences",
                items = listOf(
                    SettingsItem(
                        title = "Default Assistant",
                        subtitle = AGENTS.find { it.id == user.preferences.favoriteAgent }?.name ?: "Unknown",
                        icon = Icons.Default.SmartToy,
                        action = SettingsAction.Selection(
                            currentValue = user.preferences.favoriteAgent,
                            onSelect = { /* Handle agent selection */ }
                        )
                    ),
                    SettingsItem(
                        title = "Theme",
                        subtitle = user.preferences.theme.capitalize(),
                        icon = Icons.Default.Palette,
                        action = SettingsAction.Selection(
                            currentValue = user.preferences.theme,
                            onSelect = { /* Handle theme selection */ }
                        )
                    ),
                    SettingsItem(
                        title = "Language",
                        subtitle = "English (US)",
                        icon = Icons.Default.Language,
                        action = SettingsAction.Selection(
                            currentValue = "en_US",
                            onSelect = { /* Handle language selection */ }
                        )
                    )
                )
            ),
            SettingsSection(
                title = "Privacy & Security",
                items = listOf(
                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "View our privacy policy",
                        icon = Icons.Default.PrivacyTip,
                        action = SettingsAction.Action { onNavigateToPrivacy() }
                    ),
                    SettingsItem(
                        title = "Data & Storage",
                        subtitle = "Manage your data",
                        icon = Icons.Default.Storage,
                        action = SettingsAction.Action { /* Handle data settings */ }
                    ),
                    SettingsItem(
                        title = "Permissions",
                        subtitle = "App permissions",
                        icon = Icons.Default.Security,
                        action = SettingsAction.Action { /* Handle permissions */ }
                    )
                )
            ),
            SettingsSection(
                title = "Support",
                items = listOf(
                    SettingsItem(
                        title = "Help Center",
                        subtitle = "Get help and support",
                        icon = Icons.Default.Help,
                        action = SettingsAction.Action { /* Handle help */ }
                    ),
                    SettingsItem(
                        title = "Contact Us",
                        subtitle = "Reach out to our team",
                        icon = Icons.Default.ContactSupport,
                        action = SettingsAction.Action { /* Handle contact */ }
                    ),
                    SettingsItem(
                        title = "About",
                        subtitle = "App information",
                        icon = Icons.Default.Info,
                        action = SettingsAction.Action { onNavigateToAbout() }
                    )
                )
            ),
            SettingsSection(
                title = "Account Actions",
                items = listOf(
                    SettingsItem(
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        icon = Icons.Default.Logout,
                        action = SettingsAction.Action { showSignOutDialog = true }
                    ),
                    SettingsItem(
                        title = "Delete Account",
                        subtitle = "Permanently delete your account",
                        icon = Icons.Default.DeleteForever,
                        action = SettingsAction.Action { showDeleteAccountDialog = true }
                    )
                )
            )
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                SettingsHeader(user = user)
            }
            
            // Settings sections
            items(sections) { section ->
                SettingsSection(
                    section = section,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Sign out dialog
        if (showSignOutDialog) {
            ConfirmationDialog(
                title = "Sign Out",
                message = "Are you sure you want to sign out?",
                confirmText = "Sign Out",
                onConfirm = {
                    showSignOutDialog = false
                    onSignOut()
                },
                onDismiss = { showSignOutDialog = false }
            )
        }
        
        // Delete account dialog
        if (showDeleteAccountDialog) {
            ConfirmationDialog(
                title = "Delete Account",
                message = "Are you sure you want to permanently delete your account? This action cannot be undone.",
                confirmText = "Delete Account",
                isDestructive = true,
                onConfirm = {
                    showDeleteAccountDialog = false
                    onDeleteAccount()
                },
                onDismiss = { showDeleteAccountDialog = false }
            )
        }
    }
}

@Composable
fun SettingsHeader(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile picture
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            Color(0xFF1E40AF)
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
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // User info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = user.name,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user.email,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        // Settings icon
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SettingsSection(
    section: SettingsSection,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Section title
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        // Section items
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                section.items.forEachIndexed { index, item ->
                    SettingsItem(
                        item = item,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (index < section.items.size - 1) {
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    item: SettingsItem,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (item.enabled) 1f else 0.7f
    )
    
    Row(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = item.enabled) {
                when (val action = item.action) {
                    is SettingsAction.Action -> action.onClick()
                    is SettingsAction.Selection -> action.onSelect()
                    is SettingsAction.Toggle -> action.onToggle(!action.isEnabled)
                    else -> {}
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (item.enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                color = if (item.enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    color = if (item.enabled) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f),
                    fontSize = 14.sp
                )
            }
        }
        
        // Action indicator
        when (val action = item.action) {
            is SettingsAction.Navigation -> {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            is SettingsAction.Toggle -> {
                Switch(
                    checked = action.isEnabled,
                    onCheckedChange = null, // Handled by row click
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF3B82F6),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }
            is SettingsAction.Selection -> {
                Text(
                    text = action.currentValue,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
            is SettingsAction.Action -> {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Action",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color.Red else Color(0xFF3B82F6),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = confirmText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ProfileEditScreen(
    user: User,
    onUserUpdate: (User) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var isEditing by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(
                    onClick = {
                        if (isEditing) {
                            onUserUpdate(user.copy(name = name, email = email))
                        }
                        isEditing = !isEditing
                    }
                ) {
                    Text(
                        text = if (isEditing) "Save" else "Edit",
                        color = Color(0xFF3B82F6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Profile picture
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF1E40AF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(enabled = isEditing) {
                        // Handle profile picture change
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
                
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .background(
                                color = Color(0xFF3B82F6),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Color.White.copy(alpha = 0.7f)) },
                enabled = isEditing,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    disabledTextColor = Color.White.copy(alpha = 0.8f),
                    disabledBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                enabled = isEditing,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    disabledTextColor = Color.White.copy(alpha = 0.8f),
                    disabledBorderColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Additional info
            if (!isEditing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Account Information",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ProfileInfoRow(
                            label = "Member since",
                            value = "January 2024"
                        )
                        
                        ProfileInfoRow(
                            label = "Conversations",
                            value = "127"
                        )
                        
                        ProfileInfoRow(
                            label = "Favorite assistant",
                            value = AGENTS.find { it.id == user.preferences.favoriteAgent }?.name ?: "Unknown"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PersonalizationSection(
    toneManager: com.craftflowtechnologies.guidelens.personalization.ToneManager,
    localizationManager: com.craftflowtechnologies.guidelens.localization.GeneralLocalizationManager,
    modifier: Modifier = Modifier
) {
    val tonePreferences by toneManager.tonePreferences.collectAsState()
    val localizationPreferences by localizationManager.preferences.collectAsState()
    
    var showToneSelector by remember { mutableStateOf(false) }
    var showCountrySelector by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Section Header
        Text(
            text = "Personalization",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tone Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showToneSelector = !showToneSelector },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = tonePreferences.selectedTone.icon,
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "Personality",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tonePreferences.selectedTone.displayName,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (showToneSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle tone selector",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                AnimatedVisibility(visible = showToneSelector) {
                    ToneSelectionGrid(
                        currentTone = tonePreferences.selectedTone,
                        onToneSelected = { tone ->
                            toneManager.setTone(tone)
                            showToneSelector = false
                        }
                    )
                }
                
                Divider(color = Color.White.copy(alpha = 0.1f))
                
                // Country/Language Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCountrySelector = !showCountrySelector },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = localizationPreferences.selectedCountry.flag,
                            fontSize = 20.sp
                        )
                        Column {
                            Text(
                                text = "Location & Language",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${localizationPreferences.selectedCountry.name} • ${localizationPreferences.selectedLanguage.name}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = if (showCountrySelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle country selector",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                AnimatedVisibility(visible = showCountrySelector) {
                    CountrySelectionList(
                        currentCountry = localizationPreferences.selectedCountry,
                        onCountrySelected = { country ->
                            localizationManager.setCountry(country)
                            showCountrySelector = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneSelectionGrid(
    currentTone: com.craftflowtechnologies.guidelens.personalization.AgentTone,
    onToneSelected: (com.craftflowtechnologies.guidelens.personalization.AgentTone) -> Unit,
    modifier: Modifier = Modifier
) {
    val tones = com.craftflowtechnologies.guidelens.personalization.AgentTone.values()
    
    LazyColumn(
        modifier = modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tones) { tone ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToneSelected(tone) },
                colors = CardDefaults.cardColors(
                    containerColor = if (tone == currentTone) 
                        Color(0xFF3B82F6).copy(alpha = 0.3f) 
                    else 
                        Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tone.icon,
                        fontSize = 24.sp
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tone.displayName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = tone.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    
                    if (tone == currentTone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountrySelectionList(
    currentCountry: com.craftflowtechnologies.guidelens.localization.GeneralCountry,
    onCountrySelected: (com.craftflowtechnologies.guidelens.localization.GeneralCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Popular countries section
        item {
            Text(
                text = "Popular",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        items(com.craftflowtechnologies.guidelens.localization.GeneralCountries.POPULAR) { country ->
            CountryItem(
                country = country,
                isSelected = country == currentCountry,
                onSelected = { onCountrySelected(country) }
            )
        }
        
        // African countries section
        item {
            Text(
                text = "African Countries",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        items(com.craftflowtechnologies.guidelens.localization.GeneralCountries.AFRICAN) { country ->
            CountryItem(
                country = country,
                isSelected = country == currentCountry,
                onSelected = { onCountrySelected(country) }
            )
        }
    }
}

@Composable
private fun CountryItem(
    country: com.craftflowtechnologies.guidelens.localization.GeneralCountry,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .background(
                color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = country.flag,
            fontSize = 20.sp
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = country.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${country.language.name} • ${country.currency}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}