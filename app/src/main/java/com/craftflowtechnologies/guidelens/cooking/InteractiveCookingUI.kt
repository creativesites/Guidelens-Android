package com.craftflowtechnologies.guidelens.cooking

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.ui.theme.GuideLensColors
import com.craftflowtechnologies.guidelens.ui.theme.CookingColors

@Composable
fun InteractiveCookingSession(
    sessionManager: CookingSessionManager,
    onSendMessage: (String) -> Unit,
    onBackPressed: () -> Unit = {},
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentSession by sessionManager.currentSession.collectAsStateWithLifecycle()
    val uiState by sessionManager.uiState.collectAsStateWithLifecycle()
    val userProfile by sessionManager.userProfile.collectAsStateWithLifecycle()

    currentSession?.let { session ->
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Main content with premium glass morphism background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    GuideLensColors.DeepNavy,
                                    GuideLensColors.RiverBed.copy(alpha = 0.8f)
                                )
                            } else {
                                listOf(
                                    GuideLensColors.LightBlue.copy(alpha = 0.3f),
                                    Color.White
                                )
                            }
                        )
                    )
            ) {
                when (uiState.currentPhase) {
                    CookingPhase.OVERVIEW -> {
                        RecipeOverviewCard(
                            recipe = session.recipe,
                            userProfile = userProfile,
                            onStartCooking = { sessionManager.beginCooking() },
                            onCustomizeRecipe = { /* TODO */ },
                            isDarkTheme = isDarkTheme
                        )
                    }
                    CookingPhase.COOKING -> {
                        ActiveCookingInterface(
                            session = session,
                            uiState = uiState,
                            userProfile = userProfile,
                            sessionManager = sessionManager,
                            onSendMessage = onSendMessage,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    CookingPhase.COMPLETED -> {
                        CompletedCookingCard(
                            session = session,
                            onRateRecipe = { rating -> /* TODO */ },
                            onSaveNotes = { notes -> /* TODO */ },
                            isDarkTheme = isDarkTheme
                        )
                    }
                    CookingPhase.PAUSED -> {
                        PausedCookingCard(
                            session = session,
                            onResume = { sessionManager.resumeSession() },
                            isDarkTheme = isDarkTheme
                        )
                    }
                    else -> {
                        // Fallback UI
                    }
                }
            }
            
            // Premium floating back button overlay with glass morphism
            BackButtonOverlay(
                onBackPressed = onBackPressed,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
private fun BackButtonOverlay(
    onBackPressed: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onBackPressed,
        modifier = modifier
            .padding(16.dp)
            .size(48.dp),
        shape = CircleShape,
        color = if (isDarkTheme) {
            GuideLensColors.DarkSurface.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.9f)
        },
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                GuideLensColors.PowderBlue.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                GuideLensColors.SkyBlue.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = if (isDarkTheme) {
                    GuideLensColors.DarkOnSurface
                } else {
                    GuideLensColors.LightOnSurface
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeOverviewCard(
    recipe: Recipe,
    userProfile: UserCookingProfile?,
    onStartCooking: () -> Unit,
    onCustomizeRecipe: () -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.95f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Recipe Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DifficultyChip(difficulty = recipe.difficulty)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recipe Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecipeInfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Prep",
                    value = "${recipe.prepTime} min"
                )
                RecipeInfoItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Cook",
                    value = "${recipe.cookTime} min"
                )
                RecipeInfoItem(
                    icon = Icons.Default.People,
                    label = "Serves",
                    value = recipe.servings.toString()
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Personalized message
            userProfile?.let { profile ->
                PersonalizedMessage(profile = profile, recipe = recipe)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartCooking,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CookingColors.CookingPhase
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Cooking!")
                }
                
                OutlinedButton(
                    onClick = onCustomizeRecipe,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp, 
                        if (isDarkTheme) GuideLensColors.DarkPrimary else GuideLensColors.LightPrimary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customize")
                }
            }
        }
    }
}

@Composable
private fun ActiveCookingInterface(
    session: CookingSession,
    uiState: CookingUIState,
    userProfile: UserCookingProfile?,
    sessionManager: CookingSessionManager,
    onSendMessage: (String) -> Unit,
    isDarkTheme: Boolean = false
) {
    val currentStep = session.recipe.steps.getOrNull(session.currentStepIndex)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Progress indicator
        item {
            CookingProgressCard(
                currentStep = session.currentStepIndex + 1,
                totalSteps = session.recipe.steps.size,
                recipe = session.recipe,
                isDarkTheme = isDarkTheme
            )
        }
        
        // Active timers
        if (session.timers.isNotEmpty()) {
            item {
                ActiveTimersCard(
                    timers = session.timers.values.filter { it.isRunning || it.isPaused },
                    onTimerAction = { timerId, action ->
                        when (action) {
                            "pause" -> sessionManager.pauseTimer(timerId)
                            "resume" -> sessionManager.resumeTimer(timerId)
                            "adjust" -> { /* TODO: Show timer adjustment dialog */ }
                        }
                    }
                )
            }
        }
        
        // Current step
        currentStep?.let { step ->
            item {
                CurrentStepCard(
                    step = step,
                    stepState = session.stepStates[step.id],
                    userProfile = userProfile,
                    sessionManager = sessionManager,
                    onSendMessage = onSendMessage
                )
            }
            
            // Smart suggestions
            item {
                val suggestions = sessionManager.getPersonalizedSuggestions(step.id)
                if (suggestions.isNotEmpty()) {
                    SmartSuggestionsCard(
                        suggestions = suggestions,
                        onSuggestionAction = { suggestion ->
                            onSendMessage("Help: ${suggestion.description}")
                        }
                    )
                }
            }
        }
        
        // Session controls
        item {
            CookingControlsCard(
                session = session,
                onPause = { sessionManager.pauseSession() },
                onNeedHelp = {
                    onSendMessage("I need help with cooking step ${session.currentStepIndex + 1}")
                },
                onAddNote = { note ->
                    currentStep?.let { sessionManager.addUserNote(it.id, note) }
                }
            )
        }
    }
}

@Composable
private fun CookingProgressCard(
    currentStep: Int,
    totalSteps: Int,
    recipe: Recipe,
    isDarkTheme: Boolean
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFD97706)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animated progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(1000, easing = EaseOutCubic),
                label = "progress"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFFD97706),
                trackColor = Color(0xFFE5E5E5),
            )
        }
    }
}

@Composable
private fun ActiveTimersCard(
    timers: List<TimerState>,
    onTimerAction: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Timers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            timers.forEach { timer ->
                TimerItem(
                    timer = timer,
                    onAction = { action -> onTimerAction(timer.id, action) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TimerItem(
    timer: TimerState,
    onAction: (String) -> Unit
) {
    val minutes = (timer.remainingTime / 60000).toInt()
    val seconds = ((timer.remainingTime % 60000) / 1000).toInt()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = timer.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (timer.remainingTime < 60000) Color(0xFFD32F2F) else Color(0xFF1976D2)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (timer.isRunning) {
                IconButton(
                    onClick = { onAction("pause") }
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
            } else if (timer.isPaused) {
                IconButton(
                    onClick = { onAction("resume") }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                }
            }
            
            IconButton(
                onClick = { onAction("adjust") }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Adjust")
            }
        }
    }
}

@Composable
private fun CurrentStepCard(
    step: CookingStep,
    stepState: StepState?,
    userProfile: UserCookingProfile?,
    sessionManager: CookingSessionManager,
    onSendMessage: (String) -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Step header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step ${step.stepNumber}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isDarkTheme) {
                            GuideLensColors.DarkPrimary
                        } else {
                            GuideLensColors.LightPrimary
                        }
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (step.criticalStep) {
                    CriticalStepBadge()
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Step description
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            
            // Duration and temperature info
            if (step.duration != null || step.temperature != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    step.duration?.let { duration ->
                        InfoChip(
                            icon = Icons.Default.Timer,
                            text = "${duration} min",
                            color = Color(0xFFE3F2FD)
                        )
                    }
                    step.temperature?.let { temp ->
                        InfoChip(
                            icon = Icons.Default.LocalFireDepartment,
                            text = temp,
                            color = Color(0xFFFFF3E0)
                        )
                    }
                }
            }
            
            // Visual cues
            if (step.visualCues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Look for: ${step.visualCues.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Tips
            if (step.tips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                step.tips.forEach { tip ->
                    TipCard(tip = tip)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step actions
            StepActionButtons(
                step = step,
                stepState = stepState,
                onAction = { action ->
                    when (action) {
                        "complete" -> sessionManager.completeStep(step.id)
                        "timer" -> step.duration?.let { duration ->
                            sessionManager.startTimer(
                                "Step ${step.stepNumber}: ${step.title}",
                                duration * 60 * 1000L,
                                step.id
                            )
                        }
                        "help" -> onSendMessage("I need help with: ${step.description}")
                        "pause" -> sessionManager.pauseSession()
                    }
                }
            )
        }
    }
}

@Composable
private fun StepActionButtons(
    step: CookingStep,
    stepState: StepState?,
    onAction: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary action - Complete step
        Button(
            onClick = { onAction("complete") },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Done!")
        }
        
        // Timer action
        if (step.duration != null) {
            OutlinedButton(
                onClick = { onAction("timer") }
            ) {
                Icon(Icons.Default.Timer, contentDescription = null)
            }
        }
        
        // Help action
        OutlinedButton(
            onClick = { onAction("help") }
        ) {
            Icon(Icons.Default.Help, contentDescription = null)
        }
        
        // Pause action
        OutlinedButton(
            onClick = { onAction("pause") }
        ) {
            Icon(Icons.Default.Pause, contentDescription = null)
        }
    }
}

@Composable
private fun SmartSuggestionsCard(
    suggestions: List<SmartSuggestion>,
    onSuggestionAction: (SmartSuggestion) -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSecondaryContainer.copy(alpha = 0.4f)
            } else {
                GuideLensColors.LightSecondaryContainer.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            suggestions.forEach { suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    onClick = { onSuggestionAction(suggestion) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SmartSuggestion,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
private fun CookingControlsCard(
    session: CookingSession,
    onPause: () -> Unit,
    onNeedHelp: () -> Unit,
    onAddNote: (String) -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Session Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
                
                OutlinedButton(
                    onClick = onNeedHelp,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Help")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Show note dialog */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Note, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Note")
                }
            }
        }
    }
}

// Helper composables
@Composable
private fun DifficultyChip(difficulty: String) {
    val color = when (difficulty.lowercase()) {
        "easy" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFFFF9800)
        "hard" -> Color(0xFFE91E63)
        else -> Color(0xFF9E9E9E)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = difficulty,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecipeInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFD97706)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PersonalizedMessage(
    profile: UserCookingProfile,
    recipe: Recipe
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE8F5E8)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hi ${profile.name}! This ${recipe.difficulty.lowercase()} recipe looks perfect for your cooking level.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun CriticalStepBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Critical",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE91E63),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TipCard(tip: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFF8E1),
        border = BorderStroke(1.dp, Color(0xFFFFE082))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFFF8F00),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100)
            )
        }
    }
}

@Composable
private fun CompletedCookingCard(
    session: CookingSession,
    onRateRecipe: (Int) -> Unit,
    onSaveNotes: (String) -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.95f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Congratulations!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "You've successfully completed ${session.recipe.title}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // TODO: Add rating and notes UI
            
            Button(
                onClick = { /* TODO: Navigate back to chat */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706)
                )
            ) {
                Text("Back to Chat")
            }
        }
    }
}

@Composable
private fun PausedCookingCard(
    session: CookingSession,
    onResume: () -> Unit,
    isDarkTheme: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface.copy(alpha = 0.95f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Pause,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cooking Paused",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Take your time! Resume when you're ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resume Cooking")
            }
        }
    }
}