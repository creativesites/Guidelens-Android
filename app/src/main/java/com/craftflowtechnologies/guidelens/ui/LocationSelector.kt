package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.localization.*
import kotlinx.coroutines.launch

/**
 * Location and cultural preferences selector for the sidebar
 * Allows users to select country, region, and tribal language preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelector(
    localizationManager: LocalizationManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentLocale by localizationManager.currentLocale.collectAsStateWithLifecycle()
    val selectedTribalLanguage by localizationManager.selectedTribalLanguage.collectAsStateWithLifecycle()
    
    var showLocationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Location Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    IconButton(
                        onClick = { showLocationDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Country flag and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CountryFlag(countryCode = currentLocale.countryCode)
                    
                    Column {
                        Text(
                            text = currentLocale.countryName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentLocale.culturalRegion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Key locale info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LocaleInfoChip(
                        icon = Icons.Default.AttachMoney,
                        text = currentLocale.currency,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    LocaleInfoChip(
                        icon = Icons.Default.Schedule,
                        text = currentLocale.timeZone.split("/").last(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    
                    LocaleInfoChip(
                        icon = Icons.Default.WbSunny,
                        text = getCurrentSeason(currentLocale),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Tribal Language Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cultural Language",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    IconButton(
                        onClick = { showLanguageDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Select language",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (selectedTribalLanguage != null) {
                    // Show selected language
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Column {
                            Text(
                                text = selectedTribalLanguage!!.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedTribalLanguage!!.tribe,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Show a cultural greeting
                    if (selectedTribalLanguage!!.commonGreetings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val greeting = selectedTribalLanguage!!.commonGreetings.first()
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = greeting.first,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = greeting.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    // Encourage language selection
                    Text(
                        text = "Select a local language to add cultural expressions and traditional wisdom to your conversations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // Quick Cultural Insights
        CulturalInsightsCard(currentLocale, selectedTribalLanguage)
    }
    
    // Location Selection Dialog
    if (showLocationDialog) {
        LocationSelectionDialog(
            currentLocale = currentLocale,
            availableLocales = LocalizationManager.LOCALES,
            onLocationSelected = { locale ->
                scope.launch {
                    localizationManager.setLocale(locale, selectedTribalLanguage)
                }
                showLocationDialog = false
            },
            onDismiss = { showLocationDialog = false }
        )
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        TribalLanguageSelectionDialog(
            currentLocale = currentLocale,
            selectedLanguage = selectedTribalLanguage,
            availableLanguages = LocalizationManager.TRIBAL_LANGUAGES.values
                .filter { it.countryCode == currentLocale.countryCode },
            onLanguageSelected = { language ->
                scope.launch {
                    localizationManager.setLocale(currentLocale, language)
                }
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun CountryFlag(countryCode: String) {
    // Simple flag representation using colors and symbols
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when (countryCode) {
                    "ZM" -> Color(0xFF4CAF50) // Green for Zambia
                    "ZW" -> Color(0xFFFFD700) // Gold for Zimbabwe
                    else -> Color.Gray
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (countryCode) {
                "ZM" -> "🦅" // Eagle (on Zambian flag)
                "ZW" -> "🦅" // Bird (on Zimbabwean flag)
                else -> countryCode
            },
            fontSize = 20.sp
        )
    }
}

@Composable
private fun LocaleInfoChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CulturalInsightsCard(
    locale: LocaleInfo,
    tribalLanguage: TribalLanguage?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Cultural Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Climate info
            InsightRow(
                icon = Icons.Default.WbSunny,
                title = "Climate",
                description = locale.climate,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Common ingredients
            if (locale.commonIngredients.isNotEmpty()) {
                InsightRow(
                    icon = Icons.Default.Restaurant,
                    title = "Local Foods",
                    description = locale.commonIngredients.take(3).joinToString(", "),
                    color = MaterialTheme.colorScheme.tertiary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Cultural values
            InsightRow(
                icon = Icons.Default.Favorite,
                title = "Values",
                description = locale.culturalNotes.take(50) + "...",
                color = MaterialTheme.colorScheme.tertiary
            )
            
            // Tribal language insight
            if (tribalLanguage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                InsightRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Cultural Wisdom",
                    description = tribalLanguage.culturalNotes.take(50) + "...",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun LocationSelectionDialog(
    currentLocale: LocaleInfo,
    availableLocales: List<LocaleInfo>,
    onLocationSelected: (LocaleInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Your Location")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableLocales) { locale ->
                    LocationOptionItem(
                        locale = locale,
                        isSelected = locale.countryCode == currentLocale.countryCode,
                        onClick = { onLocationSelected(locale) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LocationOptionItem(
    locale: LocaleInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryFlag(countryCode = locale.countryCode)
            
            Column {
                Text(
                    text = locale.countryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${locale.culturalRegion} • ${locale.currency}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = locale.primaryLanguage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TribalLanguageSelectionDialog(
    currentLocale: LocaleInfo,
    selectedLanguage: TribalLanguage?,
    availableLanguages: List<TribalLanguage>,
    onLanguageSelected: (TribalLanguage?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Cultural Language")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option for no tribal language
                item {
                    LanguageOptionItem(
                        language = null,
                        isSelected = selectedLanguage == null,
                        onClick = { onLanguageSelected(null) }
                    )
                }
                
                items(availableLanguages) { language ->
                    LanguageOptionItem(
                        language = language,
                        isSelected = language.name == selectedLanguage?.name,
                        onClick = { onLanguageSelected(language) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LanguageOptionItem(
    language: TribalLanguage?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (language != null) Icons.Default.RecordVoiceOver else Icons.Default.Language,
                contentDescription = null,
                tint = if (language != null) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = language?.name ?: "English Only",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language?.tribe ?: "No cultural language expressions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (language != null && language.commonGreetings.isNotEmpty()) {
                    Text(
                        text = "\"${language.commonGreetings.first().first}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getCurrentSeason(locale: LocaleInfo): String {
    val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
    return when (locale.countryCode) {
        "ZM", "ZW" -> when (month) {
            12, 1, 2, 3, 4 -> "Rainy"
            5, 6, 7, 8 -> "Dry"
            else -> "Hot"
        }
        else -> "Spring"
    }
}