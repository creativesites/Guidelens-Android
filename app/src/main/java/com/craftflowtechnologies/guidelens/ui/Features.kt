package com.craftflowtechnologies.guidelens.ui


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow

// First, let's update the name from "Mental Health Buddy" to something more approachable
val WELLNESS_AGENT_NAME = "Mindful Companion" // More approachable name

// Add this data class for feature selection state
data class FeatureSelectionState(
    val selectedFeature: Any? = null,
    val isExpanded: Boolean = false,
    val showVideoButton: Boolean = false
)

// Main composable that handles feature selection
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FeatureSelectionHandler(
    selectedAgent: Agent,
    featureSelectionState: FeatureSelectionState,
    onFeatureClick: (Any) -> Unit,
    onBackClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = featureSelectionState,
        transitionSpec = {
            if (targetState.isExpanded) {
                slideInVertically { height -> height } + fadeIn() with
                        slideOutVertically { height -> -height } + fadeOut()
            } else {
                slideInVertically { height -> -height } + fadeIn() with
                        slideOutVertically { height -> height } + fadeOut()
            }.using(SizeTransform(clip = false))
        },
        modifier = modifier
    ) { state ->
        when {
            state.isExpanded && state.selectedFeature != null -> {
                ExpandedFeatureView(
                    feature = state.selectedFeature,
                    agent = selectedAgent,
                    showVideoButton = state.showVideoButton,
                    onBackClick = onBackClick,
                    onVideoModeClick = onVideoModeClick
                )
            }
            else -> {
                AgentWelcomeCard(
                    agent = selectedAgent,
                    onFeatureClick = onFeatureClick
                )
            }
        }
    }
}

@Composable
fun AgentWelcomeCard(
    agent: Agent,
    onFeatureClick: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        // Main welcome card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(agent.primaryColor, agent.secondaryColor)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = agent.icon,
                        contentDescription = agent.name,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hi! I'm ${agent.name}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = agent.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick actions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(agent.quickActions) { action ->
                        QuickActionChip(
                            text = action,
                            color = agent.primaryColor,
                            onClick = { onFeatureClick(action) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Agent-specific features grid
        AgentFeaturesGrid(
            agent = agent,
            onFeatureClick = onFeatureClick
        )
    }
}

// Expanded view when a feature is selected
@Composable
fun ExpandedFeatureView(
    feature: Any,
    agent: Agent,
    showVideoButton: Boolean,
    onBackClick: () -> Unit,
    onVideoModeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        agent.primaryColor.copy(alpha = 0.1f),
                        MaterialTheme.guideLensColors.gradientEnd
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Back button removed - handled by parent voice overlay to avoid duplication

        // Feature-specific content
        when (feature) {
            is CookingFeature -> ExpandedCookingFeature(feature, agent)
            is CraftingProject -> ExpandedCraftingProject(feature, agent)
            is FriendshipTool -> ExpandedFriendshipTool(feature, agent)
            is DIYCategory -> ExpandedDIYCategory(feature, agent)
            is String -> ExpandedQuickAction(feature, agent)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Video mode button if applicable
        if (showVideoButton) {
            Button(
                onClick = onVideoModeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = agent.primaryColor,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video Mode",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch to Video Mode", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Expanded cooking feature view
@Composable
fun ExpandedCookingFeature(feature: CookingFeature, agent: Agent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(pulse)
                .background(
                    color = agent.primaryColor.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = agent.primaryColor.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                tint = agent.primaryColor,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = feature.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = feature.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            FeatureDetailRow("Difficulty", feature.difficulty)
            FeatureDetailRow("Time", feature.estimatedTime)

            if (feature.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tags: ${feature.tags.joinToString(", ")}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action button
        Button(
            onClick = { /* Start this feature */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(feature.action)
        }
    }
}

// Expanded crafting project view
@Composable
fun ExpandedCraftingProject(project: CraftingProject, agent: Agent) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = project.icon,
                contentDescription = project.title,
                tint = agent.primaryColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = project.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Project details
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FeatureDetailRow("Difficulty", project.difficulty)
                FeatureDetailRow("Time Required", project.timeRequired)
                FeatureDetailRow("Category", project.category)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Materials Needed:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                    project.materials.forEach { material ->
                        Text(
                            text = "• $material",
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start project button
        Button(
            onClick = { /* Start project */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start This Project")
        }
    }
}

// Expanded friendship tool view
@Composable
fun ExpandedFriendshipTool(tool: FriendshipTool, agent: Agent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Calming animation
        val infiniteTransition = rememberInfiniteTransition()
        val breatheIn by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(200.dp)
                .scale(breatheIn)
                .background(
                    color = agent.primaryColor.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = agent.primaryColor.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = agent.primaryColor,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (breatheIn > 1.1f) "Breathe In" else "Breathe Out",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = tool.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tool.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Benefits list
        if (tool.benefits.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Benefits:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    tool.benefits.forEach { benefit ->
                        Text(
                            text = "• $benefit",
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start session button
        Button(
            onClick = { /* Start session */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Begin ${tool.duration} Session")
        }
    }
}

// Expanded DIY category view
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedDIYCategory(category: DIYCategory, agent: Agent) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with urgency indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = agent.primaryColor,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = category.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Urgency: ${category.urgency}",
                    color = when (category.urgency) {
                        "High" -> Color.Red
                        "Medium" -> Color.Yellow
                        else -> Color.Green
                    },
                    fontSize = 14.sp
                )
            }
        }

        // Category details
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = category.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                FeatureDetailRow("Safety Level", category.safetyLevel)
                FeatureDetailRow("Average Cost", category.averageCost)

                if (category.toolsRequired.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tools Required:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        category.toolsRequired.forEach { tool ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = agent.primaryColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, agent.primaryColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = tool,
                                    color = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { /* Start guide */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = agent.primaryColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Step-by-Step Guide")
            }

            if (category.urgency == "High") {
                Button(
                    onClick = { /* Emergency help */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Emergency Help")
                }
            }
        }
    }
}

// Expanded quick action view
@Composable
fun ExpandedQuickAction(action: String, agent: Agent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Quick Action",
            tint = agent.primaryColor,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = action,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Context-specific content based on action
        when (action) {
            "Find Recipe" -> QuickActionRecipeContent(agent)
            "Set Timer" -> QuickActionTimerContent(agent)
            "Quick Check-in" -> QuickActionCheckInContent(agent)
            // Add more cases as needed
            else -> DefaultQuickActionContent(action, agent)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* Execute action */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start $action")
        }
    }
}

// Helper composable for feature detail rows
@Composable
fun FeatureDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = Color.White
        )
    }
}

// Voice controls at the bottom
@Composable
fun MinimalVoiceControls(
    voiceState: VoiceState,
    onMicClick: () -> Unit,
    onMuteClick: () -> Unit,
    onBotMuteClick: () -> Unit,
    onVideoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Mic button - primary control
        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = if (voiceState.isListening) Color.Red.copy(alpha = 0.3f)
                    else Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (voiceState.isListening) Color.Red
                    else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (voiceState.isListening) Icons.Default.Mic
                else Icons.Default.MicOff,
                contentDescription = if (voiceState.isListening) "Stop Listening"
                else "Start Listening",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        // User mute button
        IconButton(
            onClick = onMuteClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (voiceState.isMuted) Color.Red.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (voiceState.isMuted) Icons.Default.VolumeOff
                else Icons.Default.VolumeUp,
                contentDescription = if (voiceState.isMuted) "Unmute" else "Mute",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Bot mute button
        IconButton(
            onClick = onBotMuteClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (voiceState.isBotMuted) Color.Red.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (voiceState.isBotMuted) Icons.Default.HeadsetOff
                else Icons.Default.Headset,
                contentDescription = if (voiceState.isBotMuted) "Unmute bot" else "Mute bot",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Video button
        IconButton(
            onClick = onVideoClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Mode",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


// Quick Action Content Components
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionRecipeContent(agent: Agent) {
    val ingredients = remember { mutableStateListOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Find Recipes by Ingredients",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Ingredient input
        ingredients.forEachIndexed { index, ingredient ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = ingredient,
                    onValueChange = { ingredients[index] = it },
                    placeholder = { Text("Enter ingredient", color = Color.White.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = agent.primaryColor,
                        unfocusedIndicatorColor = agent.primaryColor.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (index > 0) {
                    IconButton(
                        onClick = { ingredients.removeAt(index) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Add ingredient button
        Button(
            onClick = { ingredients.add("") },
            colors = ButtonDefaults.buttonColors(
                containerColor = agent.primaryColor.copy(alpha = 0.3f),
                contentColor = agent.primaryColor
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add ingredient")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Ingredient")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dietary preferences
        val dietaryOptions = listOf("Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free")
        val selectedOptions = remember { mutableStateListOf<String>() }

        Text(
            text = "Dietary Preferences:",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

//        FlowRow(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            dietaryOptions.forEach { option ->
//                FilterChip(
//                    selected = selectedOptions.contains(option),
//                    onClick = {
//                        if (selectedOptions.contains(option)) {
//                            selectedOptions.remove(option)
//                        } else {
//                            selectedOptions.add(option)
//                        }
//                    },
//                    colors = FilterChipDefaults.filterChipColors(
//                        selectedContainerColor = agent.primaryColor,
//                        selectedLabelColor = Color.White,
//                        labelColor = Color.White.copy(alpha = 0.8f)
//                    ),
//                    border = FilterChipDefaults.filterChipBorder(
//                        borderColor = agent.primaryColor.copy(alpha = 0.5f),
//                        selectedBorderColor = agent.primaryColor,
//                        borderWidth = 1.dp
//                    ),
//                    modifier = Modifier.height(36.dp) // Added height modifier for consistency
//                ) {
//                    Text(option, fontSize = 12.sp)
//                }
//            }
//        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionTimerContent(agent: Agent) {
    var timerDuration by remember { mutableStateOf(5) }
    var timerLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Set Cooking Timer",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Timer duration selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Duration:",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.width(100.dp)
            )

            Slider(
                value = timerDuration.toFloat(),
                onValueChange = { timerDuration = it.toInt() },
                valueRange = 1f..120f,
                steps = 119,
                colors = SliderDefaults.colors(
                    thumbColor = agent.primaryColor,
                    activeTrackColor = agent.primaryColor,
                    inactiveTrackColor = agent.primaryColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$timerDuration min",
                color = Color.White,
                modifier = Modifier.width(60.dp)
            )
        }

        // Timer label
        OutlinedTextField(
            value = timerLabel,
            onValueChange = { timerLabel = it },
            placeholder = { Text("Timer label (optional)", color = Color.White.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = agent.primaryColor,
                unfocusedIndicatorColor = agent.primaryColor.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.8f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Preset buttons
        val presets = listOf(5, 10, 15, 30, 45, 60)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.forEach { minutes ->
                Button(
                    onClick = { timerDuration = minutes },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timerDuration == minutes)
                            agent.primaryColor
                        else
                            agent.primaryColor.copy(alpha = 0.2f),
                        contentColor = if (timerDuration == minutes)
                            Color.White
                        else
                            agent.primaryColor
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("$minutes min")
                }
            }
        }
    }
}

@Composable
fun QuickActionCheckInContent(agent: Agent) {
    var currentMood by remember { mutableIntStateOf(3) } // 1-5 scale
    var journalEntry by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "How are you feeling, friend?",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Mood selector
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            for (i in 1..5) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (currentMood == i)
                                agent.primaryColor.copy(alpha = 0.5f)
                            else
                                Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { currentMood = i }
                ) {
                    Icon(
                        imageVector = when (i) {
                            1 -> Icons.Default.SentimentVeryDissatisfied
                            2 -> Icons.Default.SentimentDissatisfied
                            3 -> Icons.Default.SentimentNeutral
                            4 -> Icons.Default.SentimentSatisfied
                            else -> Icons.Default.SentimentVerySatisfied
                        },
                        contentDescription = "Mood $i",
                        tint = when (i) {
                            1 -> Color.Red
                            2 -> Color(0xFFFFA726)
                            3 -> Color.Yellow
                            4 -> Color(0xFF66BB6A)
                            else -> Color(0xFF43A047)
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Journal entry
        Text(
            text = "Optional notes:",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = journalEntry,
            onValueChange = { journalEntry = it },
            placeholder = { Text("How's your day going?", color = Color.White.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = agent.primaryColor,
                unfocusedIndicatorColor = agent.primaryColor.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.8f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Suggested activities based on mood
        val suggestions = when (currentMood) {
            1 -> listOf("Let's breathe together", "I'm here to listen", "Virtual hug from me")
            2 -> listOf("Let's take a walk", "I'll play you music", "Tell me what's wrong")
            3 -> listOf("Let's plan something fun", "Want to try a new recipe?", "I'll read with you")
            4 -> listOf("I'm so happy for you!", "Let's be creative together", "You're awesome!")
            else -> listOf("You're amazing!", "I'm grateful for you", "Let's celebrate!")
        }

        Text(
            text = "What I suggest as your friend:",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = agent.primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            suggestions.forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = agent.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = suggestion,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DefaultQuickActionContent(action: String, agent: Agent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Quick Action",
            tint = agent.primaryColor,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ready for $action?",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This quick action will help you with $action in no time!",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Voice prompt suggestion
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = agent.primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice command",
                tint = agent.primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Try saying: \"Help me with $action\"",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}


@Composable
fun QuickActionChip(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AgentFeaturesGrid(
    agent: Agent,
    onFeatureClick: (Any) -> Unit
) {
    when (agent.id) {
        "cooking" -> CookingFeaturesGrid(
            features = agent.features as List<CookingFeature>,
            primaryColor = agent.primaryColor,
            onFeatureClick = onFeatureClick
        )
        "crafting" -> CraftingFeaturesGrid(
            features = agent.features as List<CraftingProject>,
            primaryColor = agent.primaryColor,
            onFeatureClick = onFeatureClick
        )
        "companion" -> FriendshipFeaturesGrid(
            features = agent.features as List<FriendshipTool>,
            primaryColor = agent.primaryColor,
            onFeatureClick = onFeatureClick
        )
        "diy" -> DIYFeaturesGrid(
            features = agent.features as List<DIYCategory>,
            primaryColor = agent.primaryColor,
            onFeatureClick = onFeatureClick
        )
    }
}

@Composable
fun CookingFeaturesGrid(
    features: List<CookingFeature>,
    primaryColor: Color,
    onFeatureClick: (Any) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(features) { feature ->
            FeatureCard(
                title = feature.title,
                description = feature.description,
                icon = feature.icon,
                color = primaryColor,
                onClick = { onFeatureClick(feature) }
            )
        }
    }
}

@Composable
fun CraftingFeaturesGrid(
    features: List<CraftingProject>,
    primaryColor: Color,
    onFeatureClick: (Any) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(features) { project ->
            ProjectCard(
                project = project,
                color = primaryColor,
                onClick = { onFeatureClick(project) }
            )
        }
    }
}

@Composable
fun FriendshipFeaturesGrid(
    features: List<FriendshipTool>,
    primaryColor: Color,
    onFeatureClick: (Any) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(features) { tool ->
            FriendshipToolCard(
                tool = tool,
                color = primaryColor,
                onClick = { onFeatureClick(tool) }
            )
        }
    }
}

@Composable
fun DIYFeaturesGrid(
    features: List<DIYCategory>,
    primaryColor: Color,
    onFeatureClick: (Any) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(features) { category ->
            DIYCategoryCard(
                category = category,
                color = primaryColor,
                onClick = { onFeatureClick(category) }
            )
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: CraftingProject,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = project.icon,
                    contentDescription = project.title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = project.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.difficulty,
                color = when (project.difficulty) {
                    "Easy" -> Color.Green
                    "Medium" -> Color.Yellow
                    else -> Color.Red
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = project.timeRequired,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Materials: ${project.materials.take(2).joinToString(", ")}${if (project.materials.size > 2) "..." else ""}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FriendshipToolCard(
    tool: FriendshipTool,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tool.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = tool.duration,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DIYCategoryCard(
    category: DIYCategory,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (category.urgency) {
                    "High" -> Color.Red.copy(alpha = 0.3f)
                    "Medium" -> Color.Yellow.copy(alpha = 0.3f)
                    else -> Color.Green.copy(alpha = 0.3f)
                }
            ) {
                Text(
                    text = category.urgency,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}