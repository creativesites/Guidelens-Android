package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager

@Composable
fun LanguageSelectionDialog(
    currentLanguage: ZambianLocalizationManager.ZambianLanguage,
    onLanguageSelected: (ZambianLocalizationManager.ZambianLanguage) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Language",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ZambianLocalizationManager.ZambianLanguage.values()) { language ->
                        LanguageItem(
                            language = language,
                            isSelected = language == currentLanguage,
                            onClick = { onLanguageSelected(language) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: ZambianLocalizationManager.ZambianLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            Color(0xFF007AFF).copy(alpha = 0.1f)
        } else Color.Transparent,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF007AFF))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                
                Text(
                    text = language.localName,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                )
                
                Text(
                    text = language.code,
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.5f) else Color.Black.copy(0.5f)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RegionSelectionDialog(
    currentRegion: ZambianLocalizationManager.ZambianRegion,
    onRegionSelected: (ZambianLocalizationManager.ZambianRegion) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Region",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ZambianLocalizationManager.ZambianRegion.values()) { region ->
                        RegionItem(
                            region = region,
                            isSelected = region == currentRegion,
                            onClick = { onRegionSelected(region) },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun RegionItem(
    region: ZambianLocalizationManager.ZambianRegion,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            Color(0xFF007AFF).copy(alpha = 0.1f)
        } else Color.Transparent,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF007AFF))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = region.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                
                Text(
                    text = region.province,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
                )
                
                Text(
                    text = "Languages: ${region.majorLanguages.joinToString(", ") { it.displayName }}",
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(0.5f) else Color.Black.copy(0.5f),
                    lineHeight = 16.sp
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ZambianColorThemeCard(
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
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF007AFF)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Zambian Color Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Colors inspired by Zambian culture and heritage",
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White.copy(0.7f) else Color.Black.copy(0.7f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color palette display
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColorRow(
                    colors = listOf(
                        ColorInfo("Emerald Green", zambianColors.emeraldGreen, "Zambian flag"),
                        ColorInfo("Copper Orange", zambianColors.copperOrange, "Mining heritage"),
                        ColorInfo("Eagle Red", zambianColors.eagleRed, "National bird")
                    )
                )
                
                ColorRow(
                    colors = listOf(
                        ColorInfo("Sun Yellow", zambianColors.sunYellow, "African sun"),
                        ColorInfo("River Blue", zambianColors.riverBlue, "Zambezi river"),
                        ColorInfo("Earth Brown", zambianColors.earthBrown, "African soil")
                    )
                )
                
                ColorRow(
                    colors = listOf(
                        ColorInfo("Maize Gold", zambianColors.maizeGold, "Staple crop"),
                        ColorInfo("Forest Green", zambianColors.forestGreen, "Rich forests"),
                        ColorInfo("Sky Blue", zambianColors.skyBlue, "Clear skies")
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorRow(colors: List<ColorInfo>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colors.forEach { colorInfo ->
            ColorSwatch(colorInfo)
        }
    }
}

@Composable
private fun ColorSwatch(colorInfo: ColorInfo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(colorInfo.color, CircleShape)
                .border(2.dp, Color.White.copy(0.3f), CircleShape)
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = colorInfo.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(0.8f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = colorInfo.meaning,
            fontSize = 8.sp,
            color = Color.White.copy(0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SeasonalGuidanceCard(
    zambianLocalizationManager: ZambianLocalizationManager,
    isDarkTheme: Boolean
) {
    val seasonalGuidance = zambianLocalizationManager.getSeasonalGuidance()
    
    if (seasonalGuidance != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                zambianLocalizationManager.getZambianColors().sunYellow.copy(alpha = 0.1f),
                                zambianLocalizationManager.getZambianColors().skyBlue.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = zambianLocalizationManager.getZambianColors().sunYellow
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Seasonal Guidance",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = seasonalGuidance,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White.copy(0.8f) else Color.Black.copy(0.8f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

data class ColorInfo(
    val name: String,
    val color: Color,
    val meaning: String
)