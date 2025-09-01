package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Feature-specific pages for enhanced functionality
 */
@Composable
fun FeaturePageDialog(
    featurePageId: String,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) 
                    MaterialTheme.colorScheme.surface 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            when (featurePageId) {
                // Cooking Feature Pages
                "nutrition_calculator" -> NutritionCalculatorPage(selectedAgent, isDarkTheme, onDismiss, onArtifactRequest)
                "cooking_timer" -> CookingTimerPage(selectedAgent, isDarkTheme, onDismiss)
                "unit_converter" -> UnitConverterPage(selectedAgent, isDarkTheme, onDismiss)
                
                // Crafting Feature Pages
                "color_palette" -> ColorPalettePage(selectedAgent, isDarkTheme, onDismiss, onArtifactRequest)
                "size_calculator" -> DefaultFeaturePage("size_calculator", selectedAgent, isDarkTheme, onDismiss)
                "pattern_library" -> DefaultFeaturePage("pattern_library", selectedAgent, isDarkTheme, onDismiss)
                
                // DIY Feature Pages
                "measurement_tools" -> DefaultFeaturePage("measurement_tools", selectedAgent, isDarkTheme, onDismiss)
                "cost_calculator" -> CostCalculatorPage(selectedAgent, isDarkTheme, onDismiss, onArtifactRequest)
                "safety_checker" -> DefaultFeaturePage("safety_checker", selectedAgent, isDarkTheme, onDismiss)
                
                // Buddy Feature Pages
                "skill_assessment" -> SkillAssessmentPage(selectedAgent, isDarkTheme, onDismiss, onArtifactRequest)
                "mood_tracker" -> DefaultFeaturePage("mood_tracker", selectedAgent, isDarkTheme, onDismiss)
                "goal_planner" -> DefaultFeaturePage("goal_planner", selectedAgent, isDarkTheme, onDismiss)
                
                else -> DefaultFeaturePage(featurePageId, selectedAgent, isDarkTheme, onDismiss)
            }
        }
    }
}

// COOKING FEATURE PAGES

@Composable
private fun NutritionCalculatorPage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    var selectedMeal by remember { mutableStateOf("") }
    var servings by remember { mutableStateOf("1") }
    
    FeaturePageLayout(
        title = "Nutrition Calculator",
        subtitle = "Calculate nutritional values for your recipes",
        icon = Icons.Default.MonitorWeight,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = selectedMeal,
                    onValueChange = { selectedMeal = it },
                    label = { Text("Recipe or Meal") },
                    placeholder = { Text("e.g., Spaghetti Carbonara") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Restaurant, contentDescription = null)
                    }
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text("Servings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            if (selectedMeal.isNotEmpty()) {
                                onArtifactRequest(
                                    ArtifactRequest(
                                        type = "nutrition_analysis",
                                        agentId = selectedAgent.id,
                                        prompt = "Calculate detailed nutrition information for $selectedMeal for $servings servings",
                                        parameters = mapOf(
                                            "meal" to selectedMeal,
                                            "servings" to servings
                                        )
                                    )
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.align(Alignment.Bottom),
                        enabled = selectedMeal.isNotEmpty()
                    ) {
                        Text("Calculate")
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = selectedAgent.primaryColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quick Nutrition Tips",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedAgent.primaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val tips = listOf(
                            "Balance macronutrients: 45-65% carbs, 20-35% fats, 10-35% protein",
                            "Aim for colorful plates with variety",
                            "Include fiber-rich foods for better digestion",
                            "Watch portion sizes for calorie control"
                        )
                        tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Circle,
                                    contentDescription = null,
                                    modifier = Modifier.size(6.dp).padding(top = 6.dp),
                                    tint = selectedAgent.primaryColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CookingTimerPage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var timerName by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    
    FeaturePageLayout(
        title = "Cooking Timers",
        subtitle = "Set multiple timers for your cooking",
        icon = Icons.Default.Timer,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = timerName,
                    onValueChange = { timerName = it },
                    label = { Text("Timer Name") },
                    placeholder = { Text("e.g., Pasta boiling") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { seconds = it },
                        label = { Text("Seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Button(
                    onClick = {
                        // Timer functionality would be implemented here
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = minutes.isNotEmpty() || seconds.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Timer")
                }
            }
            
            item {
                Text(
                    text = "Common Cooking Times",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(getCommonCookingTimes()) { time ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            timerName = time.name
                            minutes = time.minutes.toString()
                            seconds = "0"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = time.name)
                        Text(
                            text = "${time.minutes}:00",
                            color = selectedAgent.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitConverterPage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf("cups") }
    var toUnit by remember { mutableStateOf("ml") }
    
    FeaturePageLayout(
        title = "Unit Converter",
        subtitle = "Convert between cooking measurements",
        icon = Icons.Default.SwapHoriz,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UnitDropdown(
                        selectedUnit = fromUnit,
                        onUnitSelected = { fromUnit = it },
                        label = "From",
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = {
                            val temp = fromUnit
                            fromUnit = toUnit
                            toUnit = temp
                        },
                        modifier = Modifier.align(Alignment.Bottom)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap units")
                    }
                    
                    UnitDropdown(
                        selectedUnit = toUnit,
                        onUnitSelected = { toUnit = it },
                        label = "To",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                if (amount.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = selectedAgent.primaryColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Conversion Result",
                                style = MaterialTheme.typography.titleMedium,
                                color = selectedAgent.primaryColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$amount $fromUnit = ${convertUnits(amount, fromUnit, toUnit)} $toUnit",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Quick Reference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(getQuickConversions()) { conversion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = conversion,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// CRAFTING FEATURE PAGES

@Composable
private fun ColorPalettePage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    var selectedColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    
    FeaturePageLayout(
        title = "Color Palette Generator",
        subtitle = "Create beautiful color combinations",
        icon = Icons.Default.Palette,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Popular Color Schemes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(getColorSchemes()) { scheme ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedColors = scheme.colors
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedColors == scheme.colors)
                            selectedAgent.primaryColor.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = scheme.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(scheme.colors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(color, RoundedCornerShape(4.dp))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                if (selectedColors.isNotEmpty()) {
                    Button(
                        onClick = {
                            onArtifactRequest(
                                ArtifactRequest(
                                    type = "color_palette",
                                    agentId = selectedAgent.id,
                                    prompt = "Create a craft project using these colors: ${selectedColors.joinToString(", ") { colorToHex(it) }}",
                                    parameters = mapOf(
                                        "colors" to selectedColors.map { colorToHex(it) }
                                    )
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Project with These Colors")
                    }
                }
            }
        }
    }
}

// DIY FEATURE PAGES

@Composable
private fun CostCalculatorPage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    var projectType by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf(mutableListOf<MaterialCost>()) }
    
    FeaturePageLayout(
        title = "Project Cost Calculator",
        subtitle = "Estimate costs for your DIY projects",
        icon = Icons.Default.AttachMoney,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = projectType,
                    onValueChange = { projectType = it },
                    label = { Text("Project Type") },
                    placeholder = { Text("e.g., Kitchen cabinet renovation") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            items(materials.size) { index ->
                MaterialCostItem(
                    material = materials[index],
                    onUpdate = { updated -> materials[index] = updated },
                    onRemove = { materials.removeAt(index) }
                )
            }
            
            item {
                OutlinedButton(
                    onClick = {
                        materials.add(MaterialCost("", 0.0, 1))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Material")
                }
            }
            
            item {
                val totalCost = materials.sumOf { it.unitCost * it.quantity }
                if (totalCost > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = selectedAgent.primaryColor.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Estimated Total Cost",
                                style = MaterialTheme.typography.titleMedium,
                                color = selectedAgent.primaryColor
                            )
                            Text(
                                text = "$${"%.2f".format(totalCost)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            item {
                if (projectType.isNotEmpty() && materials.isNotEmpty()) {
                    Button(
                        onClick = {
                            onArtifactRequest(
                                ArtifactRequest(
                                    type = "project_budget",
                                    agentId = selectedAgent.id,
                                    prompt = "Create a detailed budget and shopping plan for: $projectType",
                                    parameters = mapOf(
                                        "project" to projectType,
                                        "materials" to materials,
                                        "totalCost" to materials.sumOf { it.unitCost * it.quantity }
                                    )
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Detailed Budget Plan")
                    }
                }
            }
        }
    }
}

// BUDDY FEATURE PAGES

@Composable
private fun SkillAssessmentPage(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onArtifactRequest: (ArtifactRequest) -> Unit
) {
    var selectedSkill by remember { mutableStateOf("") }
    var currentLevel by remember { mutableStateOf("") }
    var targetLevel by remember { mutableStateOf("") }
    
    FeaturePageLayout(
        title = "Skill Assessment",
        subtitle = "Evaluate and plan your learning journey",
        icon = Icons.Default.Assessment,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = selectedSkill,
                    onValueChange = { selectedSkill = it },
                    label = { Text("Skill to Learn") },
                    placeholder = { Text("e.g., Python programming, Guitar playing") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Text(
                    text = "Current Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(listOf("Beginner", "Intermediate", "Advanced", "Expert")) { level ->
                        FilterChip(
                            selected = currentLevel == level,
                            onClick = { currentLevel = level },
                            label = { Text(level) }
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Target Level",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(listOf("Intermediate", "Advanced", "Expert", "Professional")) { level ->
                        FilterChip(
                            selected = targetLevel == level,
                            onClick = { targetLevel = level },
                            label = { Text(level) }
                        )
                    }
                }
            }
            
            item {
                if (selectedSkill.isNotEmpty() && currentLevel.isNotEmpty() && targetLevel.isNotEmpty()) {
                    Button(
                        onClick = {
                            onArtifactRequest(
                                ArtifactRequest(
                                    type = "learning_path",
                                    agentId = selectedAgent.id,
                                    prompt = "Create a personalized learning path for $selectedSkill from $currentLevel to $targetLevel level",
                                    parameters = mapOf(
                                        "skill" to selectedSkill,
                                        "currentLevel" to currentLevel,
                                        "targetLevel" to targetLevel
                                    )
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Learning Path")
                    }
                }
            }
        }
    }
}

// LAYOUT AND UTILITY COMPOSABLES

@Composable
private fun FeaturePageLayout(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedAgent: Agent,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Content
        content()
    }
}

@Composable
private fun DefaultFeaturePage(
    featurePageId: String,
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    FeaturePageLayout(
        title = "Feature Coming Soon",
        subtitle = "This feature is under development",
        icon = Icons.Default.Construction,
        selectedAgent = selectedAgent,
        onDismiss = onDismiss
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = selectedAgent.primaryColor.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Construction,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = selectedAgent.primaryColor
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Feature ID: $featurePageId",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This feature is currently being developed and will be available in a future update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helper composables and functions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val units = listOf("cups", "ml", "oz", "lbs", "tsp", "tbsp", "grams", "kg")
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MaterialCostItem(
    material: MaterialCost,
    onUpdate: (MaterialCost) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Material ${material.name.ifEmpty { "(unnamed)" }}",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                }
            }
            
            OutlinedTextField(
                value = material.name,
                onValueChange = { onUpdate(material.copy(name = it)) },
                label = { Text("Material Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = material.unitCost.toString(),
                    onValueChange = { 
                        onUpdate(material.copy(unitCost = it.toDoubleOrNull() ?: 0.0))
                    },
                    label = { Text("Unit Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = material.quantity.toString(),
                    onValueChange = { 
                        onUpdate(material.copy(quantity = it.toIntOrNull() ?: 1))
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Data classes
data class CookingTime(val name: String, val minutes: Int)
data class ColorScheme(val name: String, val colors: List<Color>)
data class MaterialCost(val name: String, val unitCost: Double, val quantity: Int)

// Helper functions
private fun getCommonCookingTimes(): List<CookingTime> = listOf(
    CookingTime("Pasta (al dente)", 8),
    CookingTime("Rice (white)", 18),
    CookingTime("Eggs (soft boiled)", 6),
    CookingTime("Eggs (hard boiled)", 10),
    CookingTime("Steak (medium)", 7),
    CookingTime("Chicken breast", 25),
    CookingTime("Pizza (homemade)", 15)
)

private fun getQuickConversions(): List<String> = listOf(
    "1 cup = 240ml",
    "1 tablespoon = 15ml",
    "1 teaspoon = 5ml",
    "1 ounce = 28g",
    "1 pound = 454g",
    "350°F = 175°C",
    "400°F = 200°C"
)

private fun getColorSchemes(): List<ColorScheme> = listOf(
    ColorScheme("Warm Autumn", listOf(
        Color(0xFFD2691E), Color(0xFFCD853F), Color(0xFFA0522D), Color(0xFF8B4513)
    )),
    ColorScheme("Cool Ocean", listOf(
        Color(0xFF4682B4), Color(0xFF5F9EA0), Color(0xFF008B8B), Color(0xFF2E8B57)
    )),
    ColorScheme("Spring Garden", listOf(
        Color(0xFF98FB98), Color(0xFF90EE90), Color(0xFF32CD32), Color(0xFF228B22)
    )),
    ColorScheme("Sunset Sky", listOf(
        Color(0xFFFF6347), Color(0xFFFF4500), Color(0xFFFF8C00), Color(0xFFFFA500)
    ))
)

private fun convertUnits(amount: String, fromUnit: String, toUnit: String): String {
    // Simple conversion logic - in a real app, this would be more comprehensive
    val amountDouble = amount.toDoubleOrNull() ?: return "0"
    
    val conversionMap = mapOf(
        "cups" to 240.0,
        "ml" to 1.0,
        "oz" to 29.57,
        "tsp" to 5.0,
        "tbsp" to 15.0
    )
    
    val fromMl = (conversionMap[fromUnit] ?: 1.0) * amountDouble
    val result = fromMl / (conversionMap[toUnit] ?: 1.0)
    
    return "%.2f".format(result)
}

private fun colorToHex(color: Color): String {
    val red = (color.red * 255).toInt()
    val green = (color.green * 255).toInt()
    val blue = (color.blue * 255).toInt()
    return "#%02X%02X%02X".format(red, green, blue)
}