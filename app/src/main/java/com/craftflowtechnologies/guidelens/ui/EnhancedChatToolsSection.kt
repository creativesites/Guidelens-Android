package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced ChatToolsSection with artifact creation focus
 */
@Composable
fun EnhancedChatToolsSection(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit,
    onFeaturePageRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showArtifactGrid by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with agent-specific call to action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getAgentCallToAction(selectedAgent.id),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = selectedAgent.primaryColor
                    )
                    Text(
                        text = "Create, learn, and explore",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Toggle view button
                IconButton(
                    onClick = { showArtifactGrid = !showArtifactGrid }
                ) {
                    Icon(
                        imageVector = if (showArtifactGrid) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view",
                        tint = selectedAgent.primaryColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (showArtifactGrid) {
                ArtifactCreationGrid(
                    selectedAgent = selectedAgent,
                    isDarkTheme = isDarkTheme,
                    onArtifactRequest = onArtifactRequest
                )
            } else {
                QuickActionsRow(
                    selectedAgent = selectedAgent,
                    isDarkTheme = isDarkTheme,
                    onArtifactRequest = onArtifactRequest,
                    onFeaturePageRequest = onFeaturePageRequest
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit,
    onFeaturePageRequest: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // Quick artifact creation buttons
        items(getAgentArtifactTypes(selectedAgent.id)) { artifactType ->
            QuickArtifactCard(
                artifactType = artifactType,
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme,
                onArtifactRequest = onArtifactRequest
            )
        }
        
        // Feature pages
        items(getAgentFeaturePages(selectedAgent.id)) { featurePage ->
            FeaturePageCard(
                featurePage = featurePage,
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme,
                onFeaturePageRequest = onFeaturePageRequest
            )
        }
    }
}

@Composable
private fun ArtifactCreationGrid(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(300.dp)
    ) {
        items(getDetailedArtifactOptions(selectedAgent.id)) { option ->
            DetailedArtifactCard(
                option = option,
                selectedAgent = selectedAgent,
                isDarkTheme = isDarkTheme,
                onArtifactRequest = onArtifactRequest
            )
        }
    }
}

@Composable
private fun QuickArtifactCard(
    artifactType: ArtifactType,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { 
                onArtifactRequest(
                    ArtifactRequest(
                        type = artifactType.id,
                        agentId = selectedAgent.id,
                        prompt = artifactType.defaultPrompt,
                        parameters = emptyMap()
                    )
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                selectedAgent.primaryColor.copy(alpha = 0.1f)
            else
                selectedAgent.primaryColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = selectedAgent.primaryColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = artifactType.icon,
                contentDescription = artifactType.title,
                tint = selectedAgent.primaryColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = artifactType.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            if (artifactType.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artifactType.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun FeaturePageCard(
    featurePage: FeaturePage,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onFeaturePageRequest: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onFeaturePageRequest(featurePage.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = if (isDarkTheme) 0.3f else 0.7f
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = featurePage.icon,
                contentDescription = featurePage.title,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = featurePage.title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun DetailedArtifactCard(
    option: DetailedArtifactOption,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onArtifactRequest(
                    ArtifactRequest(
                        type = option.artifactType,
                        agentId = selectedAgent.id,
                        prompt = option.prompt,
                        parameters = option.parameters
                    )
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                selectedAgent.primaryColor.copy(alpha = 0.15f)
            else
                selectedAgent.primaryColor.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    
                    // Tags
                    if (option.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(option.tags.take(3)) { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = selectedAgent.primaryColor.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getAgentCallToAction(agentId: String): String {
    return when (agentId) {
        "cooking" -> "What shall we cook today?"
        "crafting" -> "Let's create something amazing!"
        "diy" -> "Ready to build something?"
        "companion" -> "How can I support you today?"
        else -> "What would you like to explore?"
    }
}

// Data classes for artifact creation
data class ArtifactType(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val defaultPrompt: String
)

data class FeaturePage(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String = ""
)

data class DetailedArtifactOption(
    val id: String,
    val title: String,
    val description: String,
    val artifactType: String,
    val icon: ImageVector,
    val prompt: String,
    val parameters: Map<String, Any> = emptyMap(),
    val tags: List<String> = emptyList(),
    val difficulty: String = "Easy",
    val estimatedTime: String = "5-15 min"
)

data class ArtifactRequest(
    val type: String,
    val agentId: String,
    val prompt: String,
    val parameters: Map<String, Any>
)

// Agent-specific artifact types
private fun getAgentArtifactTypes(agentId: String): List<ArtifactType> {
    return when (agentId) {
        "cooking" -> listOf(
            ArtifactType(
                id = "recipe",
                title = "Recipe",
                subtitle = "Complete recipe",
                icon = Icons.Default.Restaurant,
                defaultPrompt = "Create a recipe for"
            ),
            ArtifactType(
                id = "meal_plan",
                title = "Meal Plan",
                subtitle = "Weekly planner",
                icon = Icons.Default.CalendarToday,
                defaultPrompt = "Create a meal plan for"
            ),
            ArtifactType(
                id = "shopping_list",
                title = "Shopping List",
                subtitle = "Smart list",
                icon = Icons.Default.ShoppingCart,
                defaultPrompt = "Create a shopping list for"
            ),
            ArtifactType(
                id = "cooking_guide",
                title = "Cooking Guide",
                subtitle = "Step-by-step",
                icon = Icons.Default.MenuBook,
                defaultPrompt = "Create a cooking technique guide for"
            )
        )
        
        "crafting" -> listOf(
            ArtifactType(
                id = "craft_project",
                title = "Craft Project",
                subtitle = "Complete tutorial",
                icon = Icons.Default.Brush,
                defaultPrompt = "Create a craft project for"
            ),
            ArtifactType(
                id = "materials_list",
                title = "Materials List",
                subtitle = "What you need",
                icon = Icons.Default.Inventory,
                defaultPrompt = "Create a materials list for"
            ),
            ArtifactType(
                id = "pattern_guide",
                title = "Pattern Guide",
                subtitle = "Templates",
                icon = Icons.Default.GridOn,
                defaultPrompt = "Create a pattern guide for"
            ),
            ArtifactType(
                id = "technique_tutorial",
                title = "Technique",
                subtitle = "Learn skills",
                icon = Icons.Default.School,
                defaultPrompt = "Create a technique tutorial for"
            )
        )
        
        "diy" -> listOf(
            ArtifactType(
                id = "diy_project",
                title = "DIY Project",
                subtitle = "Complete guide",
                icon = Icons.Default.Construction,
                defaultPrompt = "Create a DIY project guide for"
            ),
            ArtifactType(
                id = "tools_list",
                title = "Tools List",
                subtitle = "What you need",
                icon = Icons.Default.Build,
                defaultPrompt = "Create a tools list for"
            ),
            ArtifactType(
                id = "safety_guide",
                title = "Safety Guide",
                subtitle = "Stay safe",
                icon = Icons.Default.Security,
                defaultPrompt = "Create a safety guide for"
            ),
            ArtifactType(
                id = "troubleshooting",
                title = "Troubleshooting",
                subtitle = "Fix problems",
                icon = Icons.Default.Help,
                defaultPrompt = "Create a troubleshooting guide for"
            )
        )
        
        "companion" -> listOf(
            ArtifactType(
                id = "learning_plan",
                title = "Learning Plan",
                subtitle = "Study guide",
                icon = Icons.Default.School,
                defaultPrompt = "Create a learning plan for"
            ),
            ArtifactType(
                id = "practice_exercises",
                title = "Practice Set",
                subtitle = "Exercises",
                icon = Icons.Default.FitnessCenter,
                defaultPrompt = "Create practice exercises for"
            ),
            ArtifactType(
                id = "resource_list",
                title = "Resources",
                subtitle = "Helpful links",
                icon = Icons.Default.Link,
                defaultPrompt = "Create a resource list for"
            ),
            ArtifactType(
                id = "progress_tracker",
                title = "Progress Track",
                subtitle = "Track growth",
                icon = Icons.Default.TrendingUp,
                defaultPrompt = "Create a progress tracker for"
            )
        )
        
        else -> emptyList()
    }
}

private fun getAgentFeaturePages(agentId: String): List<FeaturePage> {
    return when (agentId) {
        "cooking" -> listOf(
            FeaturePage("nutrition_calculator", "Nutrition", Icons.Default.MonitorWeight),
            FeaturePage("cooking_timer", "Timers", Icons.Default.Timer),
            FeaturePage("unit_converter", "Convert", Icons.Default.SwapHoriz)
        )
        
        "crafting" -> listOf(
            FeaturePage("color_palette", "Colors", Icons.Default.Palette),
            FeaturePage("size_calculator", "Sizes", Icons.Default.Straighten),
            FeaturePage("pattern_library", "Patterns", Icons.Default.Apps)
        )
        
        "diy" -> listOf(
            FeaturePage("measurement_tools", "Measure", Icons.Default.Straighten),
            FeaturePage("cost_calculator", "Costs", Icons.Default.AttachMoney),
            FeaturePage("safety_checker", "Safety", Icons.Default.Security)
        )
        
        "companion" -> listOf(
            FeaturePage("skill_assessment", "Assess", Icons.Default.Assessment),
            FeaturePage("mood_tracker", "Mood", Icons.Default.Mood),
            FeaturePage("goal_planner", "Goals", Icons.Default.Flag)
        )
        
        else -> emptyList()
    }
}

private fun getDetailedArtifactOptions(agentId: String): List<DetailedArtifactOption> {
    return when (agentId) {
        "cooking" -> listOf(
            DetailedArtifactOption(
                id = "quick_recipe",
                title = "Quick Recipe",
                description = "Fast and easy recipes for busy days",
                artifactType = "recipe",
                icon = Icons.Default.Timer,
                prompt = "Create a quick 30-minute recipe for",
                tags = listOf("quick", "easy", "30min"),
                difficulty = "Easy",
                estimatedTime = "30 min"
            ),
            DetailedArtifactOption(
                id = "healthy_meal",
                title = "Healthy Recipe",
                description = "Nutritious and balanced meal recipes",
                artifactType = "recipe",
                icon = Icons.Default.FavoriteBorder,
                prompt = "Create a healthy, nutritious recipe for",
                tags = listOf("healthy", "balanced", "nutritious"),
                difficulty = "Easy",
                estimatedTime = "45 min"
            ),
            DetailedArtifactOption(
                id = "comfort_food",
                title = "Comfort Food",
                description = "Soul-warming, cozy recipes",
                artifactType = "recipe",
                icon = Icons.Default.Home,
                prompt = "Create a comfort food recipe for",
                tags = listOf("comfort", "hearty", "cozy"),
                difficulty = "Medium",
                estimatedTime = "1 hour"
            ),
            DetailedArtifactOption(
                id = "baking_recipe",
                title = "Baking Recipe",
                description = "Delicious baked goods and desserts",
                artifactType = "recipe",
                icon = Icons.Default.Cake,
                prompt = "Create a baking recipe for",
                tags = listOf("baking", "dessert", "sweet"),
                difficulty = "Medium",
                estimatedTime = "2 hours"
            )
        )
        
        "crafting" -> listOf(
            DetailedArtifactOption(
                id = "beginner_craft",
                title = "Beginner Project",
                description = "Perfect first projects for new crafters",
                artifactType = "craft_project",
                icon = Icons.Default.Star,
                prompt = "Create a beginner-friendly craft project for",
                tags = listOf("beginner", "easy", "first-time"),
                difficulty = "Easy",
                estimatedTime = "1 hour"
            ),
            DetailedArtifactOption(
                id = "home_decor_craft",
                title = "Home Decor",
                description = "Beautiful projects to decorate your space",
                artifactType = "craft_project",
                icon = Icons.Default.Home,
                prompt = "Create a home decor craft project for",
                tags = listOf("decor", "home", "beautiful"),
                difficulty = "Medium",
                estimatedTime = "3 hours"
            ),
            DetailedArtifactOption(
                id = "gift_craft",
                title = "Handmade Gift",
                description = "Thoughtful gifts made with love",
                artifactType = "craft_project",
                icon = Icons.Default.CardGiftcard,
                prompt = "Create a handmade gift project for",
                tags = listOf("gift", "thoughtful", "personal"),
                difficulty = "Medium",
                estimatedTime = "2 hours"
            ),
            DetailedArtifactOption(
                id = "upcycle_craft",
                title = "Upcycling Project",
                description = "Turn old items into something amazing",
                artifactType = "craft_project",
                icon = Icons.Default.Recycling,
                prompt = "Create an upcycling craft project for",
                tags = listOf("upcycle", "eco", "reuse"),
                difficulty = "Medium",
                estimatedTime = "4 hours"
            )
        )
        
        "diy" -> listOf(
            DetailedArtifactOption(
                id = "quick_fix",
                title = "Quick Fix",
                description = "Emergency repairs and quick solutions",
                artifactType = "diy_project",
                icon = Icons.Default.Build,
                prompt = "Create a quick fix guide for",
                tags = listOf("quick", "repair", "emergency"),
                difficulty = "Easy",
                estimatedTime = "30 min"
            ),
            DetailedArtifactOption(
                id = "home_improvement",
                title = "Home Upgrade",
                description = "Projects that add value to your home",
                artifactType = "diy_project",
                icon = Icons.Default.Home,
                prompt = "Create a home improvement project for",
                tags = listOf("upgrade", "value", "improvement"),
                difficulty = "Medium",
                estimatedTime = "1 day"
            ),
            DetailedArtifactOption(
                id = "organization_project",
                title = "Organization",
                description = "Storage and organization solutions",
                artifactType = "diy_project",
                icon = Icons.Default.Inventory2,
                prompt = "Create an organization project for",
                tags = listOf("organize", "storage", "tidy"),
                difficulty = "Easy",
                estimatedTime = "2 hours"
            ),
            DetailedArtifactOption(
                id = "outdoor_project",
                title = "Outdoor Project",
                description = "Garden and outdoor space improvements",
                artifactType = "diy_project",
                icon = Icons.Default.Yard,
                prompt = "Create an outdoor DIY project for",
                tags = listOf("outdoor", "garden", "landscape"),
                difficulty = "Hard",
                estimatedTime = "1 weekend"
            )
        )
        
        "companion" -> listOf(
            DetailedArtifactOption(
                id = "study_plan",
                title = "Study Plan",
                description = "Structured learning plans for any subject",
                artifactType = "learning_plan",
                icon = Icons.Default.School,
                prompt = "Create a study plan for learning",
                tags = listOf("study", "learn", "structured"),
                difficulty = "Easy",
                estimatedTime = "10 min setup"
            ),
            DetailedArtifactOption(
                id = "skill_practice",
                title = "Skill Building",
                description = "Practice exercises to build specific skills",
                artifactType = "practice_exercises",
                icon = Icons.Default.TrendingUp,
                prompt = "Create skill-building exercises for",
                tags = listOf("skills", "practice", "improve"),
                difficulty = "Medium",
                estimatedTime = "varies"
            ),
            DetailedArtifactOption(
                id = "wellness_routine",
                title = "Wellness Routine",
                description = "Daily practices for mental and physical health",
                artifactType = "learning_plan",
                icon = Icons.Default.FavoriteBorder,
                prompt = "Create a wellness routine for",
                tags = listOf("wellness", "routine", "health"),
                difficulty = "Easy",
                estimatedTime = "15 min daily"
            ),
            DetailedArtifactOption(
                id = "creative_challenge",
                title = "Creative Challenge",
                description = "Fun challenges to spark creativity",
                artifactType = "practice_exercises",
                icon = Icons.Default.Lightbulb,
                prompt = "Create a creative challenge for",
                tags = listOf("creative", "challenge", "fun"),
                difficulty = "Medium",
                estimatedTime = "30 min"
            )
        )
        
        else -> emptyList()
    }
}