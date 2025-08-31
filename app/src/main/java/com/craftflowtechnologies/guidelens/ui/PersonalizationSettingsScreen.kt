package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationSettingsScreen(
    zambianLocalizationManager: ZambianLocalizationManager,
    onNavigateBack: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val currentLanguage by zambianLocalizationManager.currentLanguage.collectAsStateWithLifecycle()
    val currentRegion by zambianLocalizationManager.currentRegion.collectAsStateWithLifecycle()
    val culturalSettings by zambianLocalizationManager.culturalSettings.collectAsStateWithLifecycle()
    val localFeatures by zambianLocalizationManager.localFeatures.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showRegionDialog by remember { mutableStateOf(false) }
    
    val zambianColors = com.craftflowtechnologies.guidelens.ui.ZambianColorScheme.default()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Personalization",
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
            // Greeting Card
            item {
                GreetingCard(
                    zambianLocalizationManager = zambianLocalizationManager,
                    isDarkTheme = isDarkTheme
                )
            }
            
            // Language & Region Settings
            item {
                SettingsGroupCard(
                    title = "Language & Region",
                    icon = Icons.Rounded.Language,
                    isDarkTheme = isDarkTheme
                ) {
                    SettingsItem(
                        icon = Icons.Rounded.Language,
                        title = "Language",
                        subtitle = "${currentLanguage.displayName} (${currentLanguage.localName})",
                        onClick = { showLanguageDialog = true },
                        isDarkTheme = isDarkTheme
                    )
                    
                    SettingsItem(
                        icon = Icons.Rounded.LocationOn,
                        title = "Region",
                        subtitle = "${currentRegion.displayName}, ${currentRegion.province}",
                        onClick = { showRegionDialog = true },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Cultural Settings
            item {
                SettingsGroupCard(
                    title = "Cultural Preferences",
                    icon = Icons.Rounded.Diversity3,
                    isDarkTheme = isDarkTheme
                ) {
                    CulturalSettingsToggles(
                        culturalSettings = culturalSettings,
                        onCulturalSettingsChange = { settings ->
                            scope.launch {
                                zambianLocalizationManager.updateCulturalSettings(settings)
                            }
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Local Features
            item {
                SettingsGroupCard(
                    title = "Local Features",
                    icon = Icons.Rounded.Explore,
                    isDarkTheme = isDarkTheme
                ) {
                    LocalFeaturesToggles(
                        localFeatures = localFeatures,
                        onLocalFeaturesChange = { features ->
                            scope.launch {
                                zambianLocalizationManager.updateLocalFeatures(features)
                            }
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Young Adult Features
            item {
                SettingsGroupCard(
                    title = "Young Adult Features",
                    icon = Icons.Rounded.Psychology,
                    isDarkTheme = isDarkTheme
                ) {
                    YoungAdultFeaturesToggles(
                        localFeatures = localFeatures,
                        onLocalFeaturesChange = { features ->
                            scope.launch {
                                zambianLocalizationManager.updateLocalFeatures(features)
                            }
                        },
                        zambianColors = zambianColors,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Color Theme Preview
            item {
                ZambianColorThemeCard(
                    zambianColors = zambianColors,
                    isDarkTheme = isDarkTheme
                )
            }
            
            // Seasonal Guidance
            item {
                SeasonalGuidanceCard(
                    zambianLocalizationManager = zambianLocalizationManager,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { language ->
                scope.launch {
                    zambianLocalizationManager.setLanguage(language)
                }
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
            isDarkTheme = isDarkTheme
        )
    }
    
    // Region Selection Dialog
    if (showRegionDialog) {
        RegionSelectionDialog(
            currentRegion = currentRegion,
            onRegionSelected = { region ->
                scope.launch {
                    zambianLocalizationManager.setRegion(region)
                }
                showRegionDialog = false
            },
            onDismiss = { showRegionDialog = false },
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun GreetingCard(
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
            Column {
                Text(
                    text = greeting,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Customize GuideLens for your Zambian experience",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF007AFF)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun CulturalSettingsToggles(
    culturalSettings: ZambianLocalizationManager.CulturalSettings,
    onCulturalSettingsChange: (ZambianLocalizationManager.CulturalSettings) -> Unit,
    zambianColors: ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSettingsItem(
            title = "Traditional Greetings",
            subtitle = "Use traditional Zambian greetings based on time of day",
            icon = Icons.Rounded.WavingHand,
            checked = culturalSettings.useTraditionalGreetings,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(useTraditionalGreetings = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Family Context",
            subtitle = "Include family considerations in responses",
            icon = Icons.Rounded.People,
            checked = culturalSettings.includeFamilyContext,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(includeFamilyContext = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Community Oriented",
            subtitle = "Provide community-focused advice and solutions",
            icon = Icons.Rounded.Groups,
            checked = culturalSettings.communityOriented,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(communityOriented = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Ubuntu Philosophy",
            subtitle = "Apply Ubuntu philosophy to interactions",
            icon = Icons.Rounded.Favorite,
            checked = culturalSettings.useUbuntuPhilosophy,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(useUbuntuPhilosophy = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Local Time Format",
            subtitle = "Use 24-hour time format",
            icon = Icons.Rounded.Schedule,
            checked = culturalSettings.useLocalTimeFormat,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(useLocalTimeFormat = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Seasonal Guidance",
            subtitle = "Show season-appropriate tips and advice",
            icon = Icons.Rounded.WbSunny,
            checked = culturalSettings.showSeasonalGuidance,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(showSeasonalGuidance = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Local Measurements",
            subtitle = "Use metric system and ZMW currency",
            icon = Icons.Rounded.Straighten,
            checked = culturalSettings.useLocalMeasurements,
            onCheckedChange = { onCulturalSettingsChange(culturalSettings.copy(useLocalMeasurements = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun LocalFeaturesToggles(
    localFeatures: ZambianLocalizationManager.LocalFeatures,
    onLocalFeaturesChange: (ZambianLocalizationManager.LocalFeatures) -> Unit,
    zambianColors: ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSettingsItem(
            title = "Local Cuisine",
            subtitle = "Zambian recipes and cooking techniques",
            icon = Icons.Rounded.Restaurant,
            checked = localFeatures.enableLocalCuisine,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableLocalCuisine = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Traditional Crafts",
            subtitle = "Local crafting projects and techniques",
            icon = Icons.Rounded.Handyman,
            checked = localFeatures.enableTraditionalCrafts,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableTraditionalCrafts = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Local Farming",
            subtitle = "Zambian farming and agriculture advice",
            icon = Icons.Rounded.Agriculture,
            checked = localFeatures.enableLocalFarming,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableLocalFarming = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Mining Support",
            subtitle = "Mining industry and safety information",
            icon = Icons.Rounded.Engineering,
            checked = localFeatures.enableMiningSupport,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableMiningSupport = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Local Business",
            subtitle = "Support for Zambian businesses and entrepreneurship",
            icon = Icons.Rounded.Business,
            checked = localFeatures.enableLocalBusinessSupport,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableLocalBusinessSupport = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        
        ToggleSettingsItem(
            title = "Education Support",
            subtitle = "Educational resources and learning support",
            icon = Icons.Rounded.School,
            checked = localFeatures.enableEducationSupport,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableEducationSupport = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Community Help",
            subtitle = "Community resources and support networks",
            icon = Icons.Rounded.VolunteerActivism,
            checked = localFeatures.enableCommunityHelp,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableCommunityHelp = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun YoungAdultFeaturesToggles(
    localFeatures: ZambianLocalizationManager.LocalFeatures,
    onLocalFeaturesChange: (ZambianLocalizationManager.LocalFeatures) -> Unit,
    zambianColors: ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToggleSettingsItem(
            title = "Career Guidance",
            subtitle = "Professional development and career advice",
            icon = Icons.Rounded.WorkOutline,
            checked = localFeatures.enableCareerGuidance,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableCareerGuidance = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Christian Tone",
            subtitle = "Christian-friendly language and values",
            icon = Icons.Rounded.Church,
            checked = localFeatures.enableChristianTone,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableChristianTone = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Daily Verses",
            subtitle = "Bible verses and spiritual encouragement",
            icon = Icons.Rounded.MenuBook,
            checked = localFeatures.enableDailyVerses,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableDailyVerses = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Devotionals",
            subtitle = "Daily devotional content and reflections",
            icon = Icons.Rounded.SelfImprovement,
            checked = localFeatures.enableDevotionals,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableDevotionals = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Inspirational Messages",
            subtitle = "Uplifting and encouraging content",
            icon = Icons.Rounded.EmojiEmotions,
            checked = localFeatures.enableInspirationalMessages,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableInspirationalMessages = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Motivational Messages",
            subtitle = "Achievement-focused motivation",
            icon = Icons.Rounded.TrendingUp,
            checked = localFeatures.enableMotivationalMessages,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableMotivationalMessages = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Success Stories",
            subtitle = "Real stories of achievement and progress",
            icon = Icons.Rounded.Star,
            checked = localFeatures.enableSuccessStories,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableSuccessStories = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Dating Advice",
            subtitle = "Relationship guidance for young adults",
            icon = Icons.Rounded.Favorite,
            checked = localFeatures.enableDatingAdvice,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableDatingAdvice = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Social Media Tips",
            subtitle = "Digital presence and online safety",
            icon = Icons.Rounded.Share,
            checked = localFeatures.enableSocialMediaTips,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableSocialMediaTips = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "TikTok Trends",
            subtitle = "Current trends and viral content insights",
            icon = Icons.Rounded.Videocam,
            checked = localFeatures.enableTikTokTrends,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableTikTokTrends = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Hot Topics & Gossip",
            subtitle = "Current events and trending discussions",
            icon = Icons.Rounded.TrendingUp,
            checked = localFeatures.enableGossipAndHotTopics,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableGossipAndHotTopics = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Financial Literacy",
            subtitle = "Money management and financial education",
            icon = Icons.Rounded.AttachMoney,
            checked = localFeatures.enableFinancialLiteracy,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableFinancialLiteracy = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
        
        ToggleSettingsItem(
            title = "Skill Development",
            subtitle = "Personal and professional skill building",
            icon = Icons.Rounded.Build,
            checked = localFeatures.enableSkillDevelopment,
            onCheckedChange = { onLocalFeaturesChange(localFeatures.copy(enableSkillDevelopment = it)) },
            zambianColors = zambianColors,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
fun ToggleSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    zambianColors: ZambianColorScheme,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (checked) zambianColors.emeraldGreen.copy(alpha = 0.2f)
                    else if (isDarkTheme) Color.White.copy(alpha = 0.1f)
                    else Color.Black.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (checked) zambianColors.emeraldGreen 
                else if (isDarkTheme) Color.White.copy(0.6f) 
                else Color.Black.copy(0.6f)
            )
        }
        
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
                lineHeight = 16.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = zambianColors.emeraldGreen,
                checkedTrackColor = zambianColors.emeraldGreen.copy(alpha = 0.5f),
                uncheckedThumbColor = if (isDarkTheme) Color.White.copy(0.6f) else Color.Black.copy(0.3f),
                uncheckedTrackColor = if (isDarkTheme) Color.White.copy(0.2f) else Color.Black.copy(0.1f)
            )
        )
    }
}