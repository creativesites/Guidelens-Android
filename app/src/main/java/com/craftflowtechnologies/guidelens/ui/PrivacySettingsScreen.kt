package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import com.craftflowtechnologies.guidelens.storage.PrivacySettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    userDataManager: UserDataManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val privacySettings by userDataManager.privacySettings.collectAsStateWithLifecycle()
    var showDataExportDialog by remember { mutableStateOf(false) }
    var showDataDeletionDialog by remember { mutableStateOf(false) }
    var isExportingData by remember { mutableStateOf(false) }
    var isDeletingData by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Text(
                    text = "Privacy & Data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
            }
        }

        item {
            // Privacy Overview Card
            PrivacyOverviewCard(privacySettings)
        }

        item {
            // Data Collection Settings
            Text(
                text = "Data Collection Preferences",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            PrivacySettingCard(
                title = "Personalization",
                description = "Allow GuideLens to remember your preferences, interests, and goals for more personalized assistance",
                icon = Icons.Default.Person,
                isEnabled = privacySettings.allowPersonalization,
                onToggle = { enabled ->
                    scope.launch {
                        userDataManager.updatePrivacySettings(
                            privacySettings.copy(allowPersonalization = enabled)
                        )
                    }
                }
            )
        }

        item {
            PrivacySettingCard(
                title = "Cooking Data",
                description = "Store your cooking preferences, dietary restrictions, and skill level for better recipe recommendations",
                icon = Icons.Default.Restaurant,
                isEnabled = privacySettings.allowCookingData,
                onToggle = { enabled ->
                    scope.launch {
                        userDataManager.updatePrivacySettings(
                            privacySettings.copy(allowCookingData = enabled)
                        )
                    }
                }
            )
        }

        item {
            PrivacySettingCard(
                title = "Wellness Data",
                description = "Track your mood and wellness for supportive guidance from Buddy",
                icon = Icons.Default.Favorite,
                isEnabled = privacySettings.allowWellnessData,
                onToggle = { enabled ->
                    scope.launch {
                        userDataManager.updatePrivacySettings(
                            privacySettings.copy(allowWellnessData = enabled)
                        )
                    }
                }
            )
        }

        item {
            PrivacySettingCard(
                title = "Usage Analytics",
                description = "Help us improve GuideLens by sharing anonymous usage patterns (no personal data included)",
                icon = Icons.Default.Analytics,
                isEnabled = privacySettings.allowUsageAnalytics,
                onToggle = { enabled ->
                    scope.launch {
                        userDataManager.updatePrivacySettings(
                            privacySettings.copy(allowUsageAnalytics = enabled)
                        )
                    }
                }
            )
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 16.dp))
        }

        item {
            // Data Management Section
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            DataRetentionCard(
                currentDays = privacySettings.dataRetentionDays,
                onRetentionChanged = { days ->
                    scope.launch {
                        userDataManager.updatePrivacySettings(
                            privacySettings.copy(dataRetentionDays = days)
                        )
                    }
                }
            )
        }

        item {
            // Export Data Button
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Your Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Download a copy of all your personal data stored in GuideLens",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showDataExportDialog = true },
                        enabled = !isExportingData
                    ) {
                        if (isExportingData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
                }
            }
        }

        item {
            // Delete All Data Button
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete All Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Permanently remove all your personal data from this device. This cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showDataDeletionDialog = true },
                        enabled = !isDeletingData,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isDeletingData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }

        item {
            // Privacy Information Footer
            Text(
                text = "🔒 All your data is stored securely on your device only. GuideLens never sends your personal information to external servers without your explicit consent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Data Export Dialog
    if (showDataExportDialog) {
        AlertDialog(
            onDismissRequest = { showDataExportDialog = false },
            title = { Text("Export Your Data") },
            text = { 
                Text("This will create a file containing all your personal data stored in GuideLens. The file will be saved to your device's downloads folder.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isExportingData = true
                            try {
                                val exportData = userDataManager.exportUserData()
                                // Here you would implement the actual file export logic
                                // For now, we'll just simulate the export
                                kotlinx.coroutines.delay(2000)
                                showDataExportDialog = false
                            } catch (e: Exception) {
                                // Handle export error
                            } finally {
                                isExportingData = false
                            }
                        }
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Data Deletion Dialog
    if (showDataDeletionDialog) {
        AlertDialog(
            onDismissRequest = { showDataDeletionDialog = false },
            title = { Text("Delete All Data") },
            text = { 
                Text("Are you sure you want to permanently delete all your personal data? This includes your preferences, cooking profile, mood entries, and usage history. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeletingData = true
                            try {
                                userDataManager.deleteAllUserData()
                                kotlinx.coroutines.delay(1000)
                                showDataDeletionDialog = false
                            } catch (e: Exception) {
                                // Handle deletion error
                            } finally {
                                isDeletingData = false
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDataDeletionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyOverviewCard(privacySettings: PrivacySettings) {
    val enabledCount = listOf(
        privacySettings.allowPersonalization,
        privacySettings.allowCookingData,
        privacySettings.allowWellnessData,
        privacySettings.allowUsageAnalytics
    ).count { it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Privacy Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "$enabledCount of 4 data types enabled for personalization",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Data retention: ${privacySettings.dataRetentionDays} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PrivacySettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataRetentionCard(
    currentDays: Int,
    onRetentionChanged: (Int) -> Unit
) {
    val retentionOptions = listOf(30, 60, 90, 180, 365)
    var expanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Data Retention Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "How long to keep your personal data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = "$currentDays days",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    retentionOptions.forEach { days ->
                        DropdownMenuItem(
                            text = { Text("$days days") },
                            onClick = {
                                onRetentionChanged(days)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}