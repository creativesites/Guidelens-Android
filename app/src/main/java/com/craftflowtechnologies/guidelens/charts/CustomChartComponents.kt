package com.craftflowtechnologies.guidelens.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.formatting.CostBreakdownData
import com.craftflowtechnologies.guidelens.formatting.MoodDataPoint
import com.craftflowtechnologies.guidelens.formatting.NutritionData
import com.craftflowtechnologies.guidelens.formatting.ProgressData
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Custom chart components specifically designed for GuideLens data visualization
 */

@Composable
fun NutritionChart(
    nutritionData: NutritionData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Nutrition Breakdown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Donut chart for macronutrients
            NutritionDonutChart(
                proteins = nutritionData.proteins,
                carbs = nutritionData.carbohydrates,
                fats = nutritionData.fats,
                modifier = Modifier.height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nutrition details
            NutritionDetails(nutritionData)
        }
    }
}

@Composable
private fun NutritionDonutChart(
    proteins: Float,
    carbs: Float,
    fats: Float,
    modifier: Modifier = Modifier
) {
    val total = proteins + carbs + fats
    val proteinAngle = (proteins / total) * 360f
    val carbsAngle = (carbs / total) * 360f
    val fatsAngle = (fats / total) * 360f
    
    val animatedProteinAngle by animateFloatAsState(
        targetValue = proteinAngle,
        animationSpec = tween(1000, easing = EaseInOut), label = ""
    )
    val animatedCarbsAngle by animateFloatAsState(
        targetValue = carbsAngle,
        animationSpec = tween(1000, delayMillis = 200, easing = EaseInOut), label = ""
    )
    val animatedFatsAngle by animateFloatAsState(
        targetValue = fatsAngle,
        animationSpec = tween(1000, delayMillis = 400, easing = EaseInOut), label = ""
    )
    
    val proteinColor = Color(0xFF4CAF50) // Green
    val carbsColor = Color(0xFF2196F3)   // Blue  
    val fatsColor = Color(0xFFFF9800)    // Orange
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chart
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                // Background circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(strokeWidth)
                )
                
                var startAngle = -90f
                
                // Proteins arc
                if (animatedProteinAngle > 0) {
                    drawArc(
                        color = proteinColor,
                        startAngle = startAngle,
                        sweepAngle = animatedProteinAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += animatedProteinAngle
                }
                
                // Carbs arc
                if (animatedCarbsAngle > 0) {
                    drawArc(
                        color = carbsColor,
                        startAngle = startAngle,
                        sweepAngle = animatedCarbsAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += animatedCarbsAngle
                }
                
                // Fats arc
                if (animatedFatsAngle > 0) {
                    drawArc(
                        color = fatsColor,
                        startAngle = startAngle,
                        sweepAngle = animatedFatsAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(
                            center.x - radius,
                            center.y - radius
                        ),
                        size = Size(radius * 2, radius * 2)
                    )
                }
            }
            
            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${total.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartLegendItem("Proteins", "${proteins.toInt()}g", proteinColor)
            ChartLegendItem("Carbs", "${carbs.toInt()}g", carbsColor)
            ChartLegendItem("Fats", "${fats.toInt()}g", fatsColor)
        }
    }
}

@Composable
private fun ChartLegendItem(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NutritionDetails(nutritionData: NutritionData) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            NutritionDetailCard(
                title = "Calories",
                value = "${nutritionData.calories.toInt()}",
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFF5722)
            )
        }
        item {
            NutritionDetailCard(
                title = "Fiber",
                value = "${nutritionData.fiber.toInt()}",
                unit = "g",
                icon = Icons.Default.Grass,
                color = Color(0xFF4CAF50)
            )
        }
        item {
            NutritionDetailCard(
                title = "Sugar",
                value = "${nutritionData.sugar.toInt()}",
                unit = "g",
                icon = Icons.Default.Icecream,
                color = Color(0xFFE91E63)
            )
        }
        item {
            NutritionDetailCard(
                title = "Sodium",
                value = "${nutritionData.sodium.toInt()}",
                unit = "mg",
                icon = Icons.Default.Grain,
                color = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
private fun NutritionDetailCard(
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.size(100.dp, 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MoodTrackingChart(
    moodData: List<MoodDataPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Mood Tracking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MoodLineChart(
                moodData = moodData,
                modifier = Modifier.height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MoodSummary(moodData)
        }
    }
}

@Composable
private fun MoodLineChart(
    moodData: List<MoodDataPoint>,
    modifier: Modifier = Modifier
) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1500, easing = EaseInOut), label = ""
    )
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        if (moodData.isNotEmpty()) {
            val maxValue = 5f
            val minValue = 1f
            val valueRange = maxValue - minValue
            
            val xStep = size.width / (moodData.size - 1).coerceAtLeast(1)
            val yScale = size.height / valueRange
            
            val path = Path()
            val points = mutableListOf<Offset>()
            
            moodData.forEachIndexed { index, dataPoint ->
                val x = index * xStep
                val y = size.height - (dataPoint.value - minValue) * yScale
                val point = Offset(x, y)
                points.add(point)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Apply animation
            val animatedPath = Path()
            val pathMeasure = android.graphics.PathMeasure(path.asAndroidPath(), false)
            pathMeasure.getSegment(
                0f,
                pathMeasure.length * animationProgress,
                animatedPath.asAndroidPath(),
                true
            )
            
            // Draw the line
            drawPath(
                path = animatedPath,
                color = Color(0xFF2196F3),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw data points
            points.take((points.size * animationProgress).toInt()).forEach { point ->
                val moodColor = when {
                    point.y > size.height * 0.8f -> Color(0xFFE53E3E) // Very sad
                    point.y > size.height * 0.6f -> Color(0xFFFF9500) // Sad
                    point.y > size.height * 0.4f -> Color(0xFFFFD60A) // Neutral
                    point.y > size.height * 0.2f -> Color(0xFF30D158) // Good
                    else -> Color(0xFF007AFF) // Very happy
                }
                
                drawCircle(
                    color = moodColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
private fun MoodSummary(moodData: List<MoodDataPoint>) {
    if (moodData.isNotEmpty()) {
        val averageMood = moodData.map { it.value }.average()
        val moodTrend = if (moodData.size >= 2) {
            moodData.last().value - moodData[moodData.size - 2].value
        } else 0f
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MoodSummaryCard(
                title = "Average",
                value = String.format("%.1f", averageMood),
                icon = Icons.Default.Analytics,
                color = getMoodColor(averageMood.toFloat())
            )
            
            MoodSummaryCard(
                title = "Trend",
                value = when {
                    moodTrend > 0 -> "↗ Better"
                    moodTrend < 0 -> "↘ Lower"
                    else -> "→ Stable"
                },
                icon = when {
                    moodTrend > 0 -> Icons.Default.TrendingUp
                    moodTrend < 0 -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                },
                color = when {
                    moodTrend > 0 -> Color(0xFF4CAF50)
                    moodTrend < 0 -> Color(0xFFFF5722)
                    else -> Color(0xFF9E9E9E)
                }
            )
        }
    }
}

@Composable
private fun MoodSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.size(120.dp, 80.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProgressChart(
    progressData: ProgressData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = progressData.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bars for different skills/categories
            progressData.categories.forEach { category ->
                SkillProgressBar(
                    skillName = category.name,
                    progress = category.progress,
                    maxProgress = category.maxProgress,
                    color = category.color
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Overall completion percentage
            val overallProgress = progressData.categories.map { it.progress / it.maxProgress }.average()
            LinearProgressIndicator(
                progress = { overallProgress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Overall Progress: ${(overallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SkillProgressBar(
    skillName: String,
    progress: Float,
    maxProgress: Float,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / maxProgress,
        animationSpec = tween(1000, easing = EaseOutCubic), label = ""
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = skillName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${progress.toInt()}/${maxProgress.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

@Composable
fun CostBreakdownChart(
    costData: CostBreakdownData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cost Breakdown",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horizontal bar chart
            costData.items.forEach { item ->
                CostBarItem(
                    name = item.name,
                    amount = item.amount,
                    percentage = item.amount / costData.total,
                    color = item.color,
                    currency = costData.currency
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Divider()
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${costData.currency}${String.format("%.2f", costData.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CostBarItem(
    name: String,
    amount: Float,
    percentage: Float,
    color: Color,
    currency: String
) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(1000, easing = EaseOutCubic), label = ""
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$currency${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color.copy(alpha = 0.2f),
                    RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPercentage)
                    .fillMaxHeight()
                    .background(
                        color,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

private fun getMoodColor(value: Float): Color {
    return when {
        value <= 1.5f -> Color(0xFFE53E3E) // Very sad - Red
        value <= 2.5f -> Color(0xFFFF9500) // Sad - Orange  
        value <= 3.5f -> Color(0xFFFFD60A) // Neutral - Yellow
        value <= 4.5f -> Color(0xFF30D158) // Good - Light Green
        else -> Color(0xFF007AFF) // Very happy - Blue
    }
}

// Data classes for chart data
//@Serializable
//data class NutritionData(
//    val calories: Float,
//    val proteins: Float,
//    val carbohydrates: Float,
//    val fats: Float,
//    val fiber: Float,
//    val sugar: Float,
//    val sodium: Float
//)

//@Serializable
//data class MoodDataPoint(
//    val date: String,
//    val value: Float,
//    val note: String = ""
//)

//data class ProgressData(
//    val title: String,
//    val categories: List<ProgressCategory>
//)
//
//data class ProgressCategory(
//    val name: String,
//    val progress: Float,
//    val maxProgress: Float,
//    val color: androidx.compose.ui.graphics.Color
//)

//data class CostBreakdownData(
//    val total: Float,
//    val currency: String,
//    val items: List<CostItem>
//)

//data class CostItem(
//    val name: String,
//    val amount: Float,
//    val color: androidx.compose.ui.graphics.Color
//)