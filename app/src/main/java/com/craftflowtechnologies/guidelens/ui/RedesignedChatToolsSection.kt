package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
 * Redesigned ChatToolsSection focused on clear artifact generation
 * Each agent gets 3-4 focused tools that reliably create artifacts
 */
@Composable
fun RedesignedChatToolsSection(
    selectedAgent: Agent,
    isDarkTheme: Boolean,
    onArtifactRequest: (ArtifactRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Generate ${selectedAgent.name} Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = selectedAgent.primaryColor
                    )
                    Text(
                        text = "Create step-by-step artifacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    imageVector = selectedAgent.icon,
                    contentDescription = null,
                    tint = selectedAgent.primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Agent-specific artifact tools
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(getArtifactToolsForAgent(selectedAgent)) { tool ->
                    ArtifactToolCard(
                        tool = tool,
                        agentColor = selectedAgent.primaryColor,
                        onClick = {
                            onArtifactRequest(
                                ArtifactRequest(
                                    type = tool.artifactType,
                                    agentId = selectedAgent.id,
                                    prompt = tool.generatePrompt(),
                                    parameters = tool.parameters
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactToolCard(
    tool: ArtifactTool,
    agentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = agentColor.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(agentColor.copy(alpha = 0.3f))
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = agentColor,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = tool.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 14.sp
            )
        }
    }
}

// Data classes for artifact tools
data class ArtifactTool(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val artifactType: String,
    val parameters: Map<String, Any> = emptyMap(),
    val promptTemplate: String
) {
    fun generatePrompt(): String = promptTemplate
}

// Agent-specific artifact tools
private fun getArtifactToolsForAgent(agent: Agent): List<ArtifactTool> {
    return when (agent.id) {
        "cooking" -> listOf(
            ArtifactTool(
                title = "Recipe",
                description = "Complete recipe guide",
                icon = Icons.Filled.Restaurant,
                artifactType = "recipe",
                promptTemplate = "Create a complete recipe with ingredients, steps, and cooking tips. Format as a structured recipe artifact."
            ),
            ArtifactTool(
                title = "Meal Plan",
                description = "Weekly meal planning",
                icon = Icons.Filled.CalendarMonth,
                artifactType = "meal_plan",
                promptTemplate = "Create a 7-day meal plan with breakfast, lunch, and dinner recipes. Include shopping list and prep instructions."
            ),
            ArtifactTool(
                title = "Technique",
                description = "Cooking technique guide",
                icon = Icons.Filled.School,
                artifactType = "cooking_technique",
                promptTemplate = "Create a detailed cooking technique guide with step-by-step instructions, tips, and common mistakes to avoid."
            )
        )
        
        "crafting" -> listOf(
            ArtifactTool(
                title = "Craft Project",
                description = "Complete project guide",
                icon = Icons.Filled.Palette,
                artifactType = "craft_project",
                promptTemplate = "Create a detailed craft project guide with materials list, tools needed, step-by-step instructions, and finishing techniques. Format as a structured craft artifact."
            ),
            ArtifactTool(
                title = "Pattern Guide",
                description = "Template & patterns",
                icon = Icons.Filled.GridOn,
                artifactType = "craft_pattern",
                promptTemplate = "Create a craft pattern guide with templates, cutting instructions, assembly steps, and variations. Include material requirements and difficulty level."
            ),
            ArtifactTool(
                title = "Technique",
                description = "Crafting technique",
                icon = Icons.Filled.Build,
                artifactType = "craft_technique",
                promptTemplate = "Create a crafting technique tutorial with detailed instructions, required tools, practice exercises, and troubleshooting tips."
            ),
            ArtifactTool(
                title = "Gift Ideas",
                description = "Handmade gift guide",
                icon = Icons.Filled.CardGiftcard,
                artifactType = "craft_gift_guide",
                promptTemplate = "Create a handmade gift guide with multiple project options, difficulty levels, time estimates, and personalization ideas."
            )
        )
        
        "diy" -> listOf(
            ArtifactTool(
                title = "DIY Project",
                description = "Home improvement",
                icon = Icons.Filled.HomeRepairService,
                artifactType = "diy_project",
                promptTemplate = "Create a comprehensive DIY project guide with materials list, tools required, safety precautions, step-by-step instructions, and troubleshooting tips. Format as a structured DIY artifact."
            ),
            ArtifactTool(
                title = "Repair Guide",
                description = "Fix-it instructions",
                icon = Icons.Filled.Build,
                artifactType = "repair_guide",
                promptTemplate = "Create a repair guide with problem diagnosis, required tools, safety warnings, detailed repair steps, and prevention tips."
            ),
            ArtifactTool(
                title = "Installation",
                description = "Setup instructions",
                icon = Icons.Filled.Construction,
                artifactType = "installation_guide",
                promptTemplate = "Create an installation guide with preparation steps, required tools, safety requirements, installation process, and final testing procedures."
            ),
            ArtifactTool(
                title = "Maintenance",
                description = "Care & upkeep",
                icon = Icons.Filled.Schedule,
                artifactType = "maintenance_schedule",
                promptTemplate = "Create a maintenance schedule guide with regular tasks, seasonal maintenance, troubleshooting, and care instructions."
            )
        )
        
        "buddy" -> listOf(
            ArtifactTool(
                title = "Learning Plan",
                description = "Skill development",
                icon = Icons.Filled.School,
                artifactType = "learning_plan",
                promptTemplate = "Create a structured learning plan with modules, objectives, practice exercises, resources, and progress tracking. Format as an educational artifact."
            ),
            ArtifactTool(
                title = "Tutorial",
                description = "Step-by-step guide",
                icon = Icons.Filled.PlayLesson,
                artifactType = "tutorial",
                promptTemplate = "Create a comprehensive tutorial with learning objectives, prerequisites, step-by-step instructions, examples, and practice exercises."
            ),
            ArtifactTool(
                title = "Checklist",
                description = "Task completion",
                icon = Icons.Filled.Checklist,
                artifactType = "checklist",
                promptTemplate = "Create an organized checklist with categories, priority levels, completion tracking, and helpful tips for each item."
            ),
            ArtifactTool(
                title = "Resource Guide",
                description = "Curated resources",
                icon = Icons.Filled.LibraryBooks,
                artifactType = "resource_guide",
                promptTemplate = "Create a resource guide with categorized links, tools, references, and recommendations organized by skill level and topic."
            )
        )
        
        else -> emptyList()
    }
}