package com.craftflowtechnologies.guidelens.cooking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.craftflowtechnologies.guidelens.ui.theme.GuideLensColors
import com.craftflowtechnologies.guidelens.ui.theme.CookingColors
import com.craftflowtechnologies.guidelens.api.XAIImageClient
import androidx.compose.material.icons.filled.AttachMoney

/**
 * Interactive cooking tools and features for enhanced chat experience
 */

data class CookingTool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val action: String,
    val category: CookingToolCategory
)

enum class CookingToolCategory {
    MEASUREMENT,
    TIMING,
    TEMPERATURE,
    TECHNIQUE,
    PLANNING,
    INSPIRATION
}

@Composable
fun CookingToolsPanel(
    onToolClick: (CookingTool) -> Unit,
    onGenerateImages: (String) -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tools = remember { getCookingTools() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = CookingColors.CookingPhase,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Cooking Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) {
                        GuideLensColors.DarkOnSurface
                    } else {
                        GuideLensColors.LightOnSurface
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tools Grid
            val groupedTools = tools.groupBy { it.category }
            
            groupedTools.forEach { (category, categoryTools) ->
                CookingToolCategory(
                    category = category,
                    tools = categoryTools,
                    onToolClick = onToolClick,
                    onGenerateImages = onGenerateImages,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CookingToolCategory(
    category: CookingToolCategory,
    tools: List<CookingTool>,
    onToolClick: (CookingTool) -> Unit,
    onGenerateImages: (String) -> Unit,
    isDarkTheme: Boolean
) {
    Column {
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) {
                GuideLensColors.DarkOnSurfaceVariant
            } else {
                GuideLensColors.LightOnSurfaceVariant
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tools) { tool ->
                CookingToolChip(
                    tool = tool,
                    onClick = { 
                        onToolClick(tool)
                        if (tool.category == CookingToolCategory.INSPIRATION) {
                            onGenerateImages(tool.action)
                        }
                    },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun CookingToolChip(
    tool: CookingTool,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = tool.color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, tool.color.copy(alpha = 0.3f)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                tool.icon,
                contentDescription = null,
                tint = tool.color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tool.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tool.color
            )
        }
    }
}

@Composable
fun SmartCookingTimer(
    timerName: String,
    durationMinutes: Int,
    onTimerComplete: () -> Unit,
    onCancel: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    var timeLeftSeconds by remember { mutableStateOf(durationMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    
    LaunchedEffect(isRunning) {
        while (isRunning && timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
        if (timeLeftSeconds <= 0) {
            onTimerComplete()
            isRunning = false
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                CookingColors.TimerRunning.copy(alpha = 0.1f)
            } else {
                CookingColors.TimerRunning.copy(alpha = 0.05f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(timeLeftSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            timeLeftSeconds < 60 -> CookingColors.TimerCritical
                            timeLeftSeconds < 300 -> CookingColors.TimerWarning
                            else -> CookingColors.TimerRunning
                        }
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isRunning) {
                        Button(
                            onClick = { isRunning = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CookingColors.TimerRunning
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                        }
                    } else {
                        Button(
                            onClick = { 
                                isRunning = false
                                isPaused = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CookingColors.TimerPaused
                            ),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onCancel,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Cancel")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            val progress = if (durationMinutes * 60 > 0) {
                (durationMinutes * 60 - timeLeftSeconds).toFloat() / (durationMinutes * 60).toFloat()
            } else 0f
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    timeLeftSeconds < 60 -> CookingColors.TimerCritical
                    timeLeftSeconds < 300 -> CookingColors.TimerWarning
                    else -> CookingColors.TimerRunning
                },
                trackColor = if (isDarkTheme) {
                    GuideLensColors.DarkSurfaceVariant
                } else {
                    GuideLensColors.LightSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun CookingTipCard(
    tip: String,
    category: String = "General",
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CookingColors.TipBackground.copy(
                alpha = if (isDarkTheme) 0.1f else 0.8f
            )
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, CookingColors.TipBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = CookingColors.TipIcon.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = CookingColors.TipIcon,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CookingColors.TipIcon
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) {
                        GuideLensColors.DarkOnSurface
                    } else {
                        Color(0xFF8D4E00)
                    },
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun getCookingTools(): List<CookingTool> {
    return listOf(
        // Measurement Tools
        CookingTool(
            id = "measurement_converter",
            name = "Convert Units",
            description = "Convert between different measurement units",
            icon = Icons.Default.Balance,
            color = Color(0xFF2196F3),
            action = "Help me convert measurements for cooking",
            category = CookingToolCategory.MEASUREMENT
        ),
        CookingTool(
            id = "portion_calculator",
            name = "Scale Recipe",
            description = "Scale recipe up or down for different servings",
            icon = Icons.Default.AttachMoney,
            color = Color(0xFF2196F3),
            action = "Help me scale this recipe for different servings",
            category = CookingToolCategory.MEASUREMENT
        ),
        
        // Timing Tools
        CookingTool(
            id = "cooking_timer",
            name = "Timer",
            description = "Set cooking timers",
            icon = Icons.Default.Timer,
            color = Color(0xFFFF9800),
            action = "Set a cooking timer",
            category = CookingToolCategory.TIMING
        ),
        CookingTool(
            id = "meal_planner",
            name = "Plan Timing",
            description = "Plan when to start each dish",
            icon = Icons.Default.Schedule,
            color = Color(0xFFFF9800),
            action = "Help me plan the timing for this meal",
            category = CookingToolCategory.TIMING
        ),
        
        // Temperature Tools
        CookingTool(
            id = "temp_guide",
            name = "Temperatures",
            description = "Cooking temperature guide",
            icon = Icons.Default.Thermostat,
            color = Color(0xFFE91E63),
            action = "Show me cooking temperature guidelines",
            category = CookingToolCategory.TEMPERATURE
        ),
        CookingTool(
            id = "doneness_check",
            name = "Doneness",
            description = "Check if food is properly cooked",
            icon = Icons.Default.CheckCircle,
            color = Color(0xFFE91E63),
            action = "How do I know when this is properly cooked?",
            category = CookingToolCategory.TEMPERATURE
        ),
        
        // Technique Tools
        CookingTool(
            id = "knife_skills",
            name = "Knife Skills",
            description = "Learn cutting techniques",
            icon = Icons.Default.ContentCut,
            color = Color(0xFF4CAF50),
            action = "Teach me proper knife techniques",
            category = CookingToolCategory.TECHNIQUE
        ),
        CookingTool(
            id = "cooking_methods",
            name = "Techniques",
            description = "Learn cooking methods",
            icon = Icons.Default.AutoAwesome,
            color = Color(0xFF4CAF50),
            action = "Explain different cooking techniques",
            category = CookingToolCategory.TECHNIQUE
        ),
        
        // Planning Tools
        CookingTool(
            id = "ingredient_sub",
            name = "Substitutions",
            description = "Find ingredient substitutions",
            icon = Icons.Default.SwapHoriz,
            color = Color(0xFF9C27B0),
            action = "What can I substitute for ingredients I don't have?",
            category = CookingToolCategory.PLANNING
        ),
        CookingTool(
            id = "shopping_list",
            name = "Shopping List",
            description = "Generate shopping list",
            icon = Icons.Default.ShoppingCart,
            color = Color(0xFF9C27B0),
            action = "Create a shopping list for this recipe",
            category = CookingToolCategory.PLANNING
        ),
        
        // Inspiration Tools
        CookingTool(
            id = "recipe_ideas",
            name = "Recipe Ideas",
            description = "Get recipe suggestions",
            icon = Icons.Default.Lightbulb,
            color = Color(0xFFFF5722),
            action = "Suggest recipes with ingredients I have",
            category = CookingToolCategory.INSPIRATION
        ),
        CookingTool(
            id = "generate_images",
            name = "Visualize",
            description = "Generate recipe images",
            icon = Icons.Default.Image,
            color = Color(0xFFFF5722),
            action = "Generate images for this recipe",
            category = CookingToolCategory.INSPIRATION
        )
    )
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}