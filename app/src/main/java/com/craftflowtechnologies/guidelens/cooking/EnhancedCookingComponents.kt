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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.craftflowtechnologies.guidelens.ui.theme.GuideLensColors
import com.craftflowtechnologies.guidelens.ui.theme.CookingColors
import com.craftflowtechnologies.guidelens.charts.NutritionChart
import com.craftflowtechnologies.guidelens.api.XAIImageClient
import com.craftflowtechnologies.guidelens.formatting.NutritionData

/**
 * Enhanced cooking components with premium UI, charts integration, and xAI image generation
 */

@Composable
fun EnhancedRecipeCard(
    recipe: Recipe,
    userProfile: UserCookingProfile?,
    onStartCooking: () -> Unit,
    onCustomizeRecipe: () -> Unit,
    onGenerateImages: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val xaiClient = remember { XAIImageClient(context) }
    
    var isGeneratingImages by remember { mutableStateOf(false) }
    var generatedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showNutritionChart by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.95f)
            } else {
                Color.White.copy(alpha = 0.98f)
            }
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Recipe Header with Premium Styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) {
                            GuideLensColors.DarkOnSurface
                        } else {
                            GuideLensColors.LightOnSurface
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkTheme) {
                            GuideLensColors.DarkOnSurfaceVariant
                        } else {
                            GuideLensColors.LightOnSurfaceVariant
                        },
                        lineHeight = 24.sp
                    )
                }
                
                PremiumDifficultyChip(
                    difficulty = recipe.difficulty,
                    isDarkTheme = isDarkTheme
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Enhanced Recipe Info with Glass Morphism
            EnhancedRecipeInfoRow(
                prepTime = recipe.prepTime,
                cookTime = recipe.cookTime,
                servings = recipe.servings,
                isDarkTheme = isDarkTheme
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Generated Images Gallery (if available)
            if (generatedImages.isNotEmpty()) {
                RecipeImageGallery(
                    images = generatedImages,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Nutrition Chart Toggle
            if (recipe.nutritionData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = showNutritionChart,
                        onCheckedChange = { showNutritionChart = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDarkTheme) GuideLensColors.DarkPrimary else GuideLensColors.LightPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Show Nutrition Information",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                AnimatedVisibility(
                    visible = showNutritionChart,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        NutritionChart(
                            nutritionData = recipe.nutritionData!!,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Personalized message with enhanced styling
            userProfile?.let { profile ->
                EnhancedPersonalizedMessage(
                    profile = profile,
                    recipe = recipe,
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Enhanced Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary action - Start Cooking
                Button(
                    onClick = onStartCooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CookingColors.CookingPhase
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Start Cooking!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCustomizeRecipe,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isDarkTheme) GuideLensColors.DarkPrimary else GuideLensColors.LightPrimary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            if (!isGeneratingImages) {
                                isGeneratingImages = true
                                coroutineScope.launch {
                                    try {
                                        val result = xaiClient.generateCookingImages(
                                            recipeName = recipe.title,
                                            recipeType = "dish",
                                            includeSteps = true
                                        )
                                        result.fold(
                                            onSuccess = { cookingImageSet ->
                                                val imageUrls = listOfNotNull(
                                                    cookingImageSet.finalDish,
                                                    cookingImageSet.ingredients,
                                                    cookingImageSet.cookingProcess,
                                                    cookingImageSet.plating
                                                ) + cookingImageSet.extraImages
                                                generatedImages = imageUrls
                                            },
                                            onFailure = { error ->
                                                // Handle error - could show snackbar
                                                println("Failed to generate images: ${error.message}")
                                            }
                                        )
                                    } finally {
                                        isGeneratingImages = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            Color(0xFF9C27B0)
                        ),
                        enabled = !isGeneratingImages
                    ) {
                        if (isGeneratingImages) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF9C27B0)
                            )
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isGeneratingImages) "Generating..." else "AI Images")
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumDifficultyChip(
    difficulty: String,
    isDarkTheme: Boolean
) {
    val (color, icon) = when (difficulty.lowercase()) {
        "easy" -> Pair(Color(0xFF4CAF50), Icons.Default.StarRate)
        "medium" -> Pair(Color(0xFFFF9800), Icons.Default.Stars)
        "hard" -> Pair(Color(0xFFE91E63), Icons.Default.Whatshot)
        else -> Pair(Color(0xFF9E9E9E), Icons.Default.Help)
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = difficulty,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EnhancedRecipeInfoRow(
    prepTime: Int,
    cookTime: Int,
    servings: Int,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        EnhancedRecipeInfoItem(
            icon = Icons.Default.Schedule,
            label = "Prep Time",
            value = "${prepTime}m",
            color = Color(0xFF2196F3),
            isDarkTheme = isDarkTheme
        )
        EnhancedRecipeInfoItem(
            icon = Icons.Default.LocalFireDepartment,
            label = "Cook Time",
            value = "${cookTime}m",
            color = Color(0xFFFF5722),
            isDarkTheme = isDarkTheme
        )
        EnhancedRecipeInfoItem(
            icon = Icons.Default.People,
            label = "Servings",
            value = servings.toString(),
            color = Color(0xFF4CAF50),
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun EnhancedRecipeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    isDarkTheme: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) {
                    GuideLensColors.DarkOnSurfaceVariant
                } else {
                    GuideLensColors.LightOnSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun RecipeImageGallery(
    images: List<String>,
    isDarkTheme: Boolean
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Generated Images",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9C27B0)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(images) { imageUrl ->
                Card(
                    modifier = Modifier
                        .size(120.dp, 90.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Generated cooking image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPersonalizedMessage(
    profile: UserCookingProfile,
    recipe: Recipe,
    isDarkTheme: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSecondaryContainer.copy(alpha = 0.3f)
            } else {
                GuideLensColors.LightSecondaryContainer.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) {
                    GuideLensColors.DarkPrimary.copy(alpha = 0.2f)
                } else {
                    GuideLensColors.LightPrimary.copy(alpha = 0.2f)
                }
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isDarkTheme) {
                        GuideLensColors.DarkPrimary
                    } else {
                        GuideLensColors.LightPrimary
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hi ${profile.name}! 👋",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This ${recipe.difficulty.lowercase()} recipe matches your cooking level perfectly. Let's make something delicious!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) {
                        GuideLensColors.DarkOnSurfaceVariant
                    } else {
                        GuideLensColors.LightOnSurfaceVariant
                    },
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// Extension function to add nutrition data to Recipe
fun Recipe.withNutritionData(nutritionData: NutritionData): Recipe {
    return this.copy(nutritionData = nutritionData)
}

// Update Recipe data class to include nutrition data
data class RecipeWithNutrition(
    val recipe: Recipe,
    val nutritionData: NutritionData? = null
)