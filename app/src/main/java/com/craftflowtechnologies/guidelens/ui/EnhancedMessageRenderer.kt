package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.charts.*
import com.craftflowtechnologies.guidelens.formatting.*
import com.craftflowtechnologies.guidelens.localization.CulturalExpression
import com.craftflowtechnologies.guidelens.localization.LocalizationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced message renderer that displays formatted messages with charts,
 * localized content, and interactive elements
 */
@Composable
fun EnhancedMessageRenderer(
    formattedMessage: FormattedMessage,
    localizationManager: LocalizationManager,
    modifier: Modifier = Modifier,
    onInstructionToggle: ((Int, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(formattedMessage.elements) { index, element ->
            EnhancedMessageElement(
                element = element,
                localizationManager = localizationManager,
                onInstructionToggle = onInstructionToggle,
                animationDelay = index * 150L
            )
        }

        // Show metadata footer for cultural context
        if (formattedMessage.metadata.tribalLanguage != null ||
            formattedMessage.metadata.locale.countryCode != "US") {
            item {
                CulturalContextFooter(
                    metadata = formattedMessage.metadata,
                    localizationManager = localizationManager
                )
            }
        }
    }
}

@Composable
private fun EnhancedMessageElement(
    element: MessageElement,
    localizationManager: LocalizationManager,
    onInstructionToggle: ((Int, Boolean) -> Unit)?,
    animationDelay: Long = 0L
) {
    var isVisible by remember { mutableStateOf(true) } // Show immediately for testing

    LaunchedEffect(element) {
        delay(animationDelay)
        isVisible = true
    }

    // Show content
    if (isVisible) {
        when (element) {
            is MessageElement.Text -> TextElementRenderer(element)
            is MessageElement.Chart -> ChartElementRenderer(element, localizationManager)
            is MessageElement.Recipe -> RecipeElementRenderer(element, localizationManager)
            is MessageElement.Instructions -> InstructionsElementRenderer(element, onInstructionToggle)
            is MessageElement.CostBreakdown -> CostBreakdownElementRenderer(element)
            is MessageElement.ProgressTracking -> ProgressTrackingElementRenderer(element)
            is MessageElement.LocalizedContent -> LocalizedContentElementRenderer(element, localizationManager)
        }
    }
}
@Composable
private fun TextElementRenderer(element: MessageElement.Text) {
    val textStyle = when (element.style) {
        TextStyle.HEADING -> MaterialTheme.typography.headlineSmall
        TextStyle.BODY -> MaterialTheme.typography.bodyLarge
        TextStyle.CAPTION -> MaterialTheme.typography.bodySmall
        TextStyle.EMPHASIS -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = element.content,
            style = textStyle,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChartElementRenderer(
    element: MessageElement.Chart,
    localizationManager: LocalizationManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    text = element.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    when (element.chartType) {
                        ChartType.NUTRITION -> Icons.Default.Restaurant
                        ChartType.MOOD_TRACKING -> Icons.Default.Mood
                        ChartType.PROGRESS -> Icons.Default.TrendingUp
                        ChartType.COST_BREAKDOWN -> Icons.Default.AttachMoney
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Render appropriate chart based on type
            when (element.chartType) {
                ChartType.NUTRITION -> {
                    val nutritionData = element.data as? NutritionData
                    nutritionData?.let { data ->
                        NutritionChart(
                            nutritionData = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ChartType.MOOD_TRACKING -> {
                    val moodData = element.data as? List<MoodDataPoint>
                    moodData?.let { data ->
                        MoodTrackingChart(
                            moodData = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ChartType.PROGRESS -> {
                    val progressData = element.data as? ProgressData
                    progressData?.let { data ->
                        ProgressChart(
                            progressData = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ChartType.COST_BREAKDOWN -> {
                    val costData = element.data as? CostBreakdownData
                    costData?.let { data ->
                        CostBreakdownChart(
                            costData = data,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeElementRenderer(
    element: MessageElement.Recipe,
    localizationManager: LocalizationManager
) {
    var showLocalizedVersion by remember { mutableStateOf(false) }
    val locale by localizationManager.currentLocale.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with recipe title and localization toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = element.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Traditional ${locale.countryName} Style",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = showLocalizedVersion,
                        onCheckedChange = { showLocalizedVersion = it }
                    )

                    Text(
                        text = "Local",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cultural notes
            if (element.culturalNotes.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = element.culturalNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Ingredients section
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            element.ingredients.forEach { ingredient ->
                IngredientItem(
                    ingredient = ingredient,
                    showLocalizedVersion = showLocalizedVersion
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steps section
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            element.steps.forEachIndexed { index, step ->
                RecipeStepItem(
                    stepNumber = index + 1,
                    instruction = step
                )
            }

            // Localized tips
            if (element.localizedTips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Local Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                element.localizedTips.forEach { tip ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun IngredientItem(
    ingredient: LocalizedIngredient,
    showLocalizedVersion: Boolean
) {
    val displayName = if (showLocalizedVersion && ingredient.localName != null) {
        "${ingredient.localName} (${ingredient.name})"
    } else {
        ingredient.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(6.dp)
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showLocalizedVersion && (ingredient.alternatives.isNotEmpty() || ingredient.availability.isNotEmpty())) {
                Spacer(modifier = Modifier.height(4.dp))

                if (ingredient.alternatives.isNotEmpty()) {
                    Text(
                        text = "Alternatives: ${ingredient.alternatives.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }

                if (ingredient.availability.isNotEmpty()) {
                    Text(
                        text = "Availability: ${ingredient.availability}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeStepItem(
    stepNumber: Int,
    instruction: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun InstructionsElementRenderer(
    element: MessageElement.Instructions,
    onInstructionToggle: ((Int, Boolean) -> Unit)?
) {
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
                    text = element.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (element.allowInteraction) {
                    val completedSteps = element.steps.count { it.isCompleted }
                    val totalSteps = element.steps.size

                    Text(
                        text = "$completedSteps/$totalSteps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            element.steps.forEach { step ->
                InstructionStepItem(
                    step = step,
                    allowInteraction = element.allowInteraction,
                    onToggle = { isCompleted ->
                        onInstructionToggle?.invoke(step.number - 1, isCompleted)
                    }
                )
            }

            // Progress indicator
            if (element.allowInteraction) {
                Spacer(modifier = Modifier.height(16.dp))

                val progress = element.steps.count { it.isCompleted }.toFloat() / element.steps.size
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
            }
        }
    }
}

@Composable
private fun InstructionStepItem(
    step: InstructionStep,
    allowInteraction: Boolean,
    onToggle: (Boolean) -> Unit
) {
    var isCompleted by remember { mutableStateOf(step.isCompleted) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .then(
                if (allowInteraction) {
                    Modifier.clickable {
                        isCompleted = !isCompleted
                        onToggle(isCompleted)
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (allowInteraction) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = {
                        isCompleted = it
                        onToggle(it)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.number.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = step.instruction,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CostBreakdownElementRenderer(element: MessageElement.CostBreakdown) {
    CostBreakdownChart(
        costData = CostBreakdownData(
            total = element.total,
            currency = element.currency,
            items = element.items
        ),
        modifier = Modifier.fillMaxWidth()
    )

    if (element.localizedNotes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = element.localizedNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProgressTrackingElementRenderer(element: MessageElement.ProgressTracking) {
    Column {
        ProgressChart(
            progressData = ProgressData(
                title = element.title,
                categories = element.categories
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (element.motivationalMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = element.motivationalMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalizedContentElementRenderer(
    element: MessageElement.LocalizedContent,
    localizationManager: LocalizationManager
) {
    Column {
        // Main content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Text(
                text = element.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Cultural expressions tooltip
        if (element.culturalExpressions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(element.culturalExpressions) { expression ->
                    CulturalExpressionChip(expression)
                }
            }
        }

        // Localized terms
        if (element.localizedTerms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(element.localizedTerms) { term ->
                    LocalizedTermChip(term)
                }
            }
        }

        // Seasonal context
        if (element.seasonalContext.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = element.seasonalContext,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CulturalExpressionChip(expression: CulturalExpression) {
    var showTooltip by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable { showTooltip = !showTooltip },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = expression.phrase,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (showTooltip) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = expression.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = expression.context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalizedTermChip(term: LocalizedTerm) {
    var showTranslation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.clickable { showTranslation = !showTranslation },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = term.phrase,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            if (showTranslation) {
                Text(
                    text = term.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = term.language,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CulturalContextFooter(
    metadata: MessageMetadata,
    localizationManager: LocalizationManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = buildString {
                    append("Localized for ${metadata.locale.countryName}")
                    metadata.tribalLanguage?.let {
                        append(" • $it expressions included")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
