package com.craftflowtechnologies.guidelens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactRepository
import com.craftflowtechnologies.guidelens.storage.ArtifactType
import com.craftflowtechnologies.guidelens.storage.RecipeExtractionManager
import com.craftflowtechnologies.guidelens.chat.ChatSessionManager
import com.craftflowtechnologies.guidelens.cooking.EnhancedCookingSessionManager
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsScreen(
    currentUser: User?,
    artifactRepository: ArtifactRepository,
    chatSessionManager: ChatSessionManager,
    cookingSessionManager: EnhancedCookingSessionManager,
    imageGenerator: ArtifactImageGenerator,
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    isDarkTheme: Boolean = true
) {
    var viewMode by remember { mutableStateOf(ViewMode.GRID) }
    var selectedFilter by remember { mutableStateOf(ArtifactFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedArtifact by remember { mutableStateOf<Artifact?>(null) }
    
    var artifacts by remember { mutableStateOf<List<Artifact>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isExtracting by remember { mutableStateOf(false) }
    var showExtractionResult by remember { mutableStateOf(false) }
    var extractionResultMessage by remember { mutableStateOf("") }
    
    // Create recipe extraction manager
    val recipeExtractionManager = remember {
        RecipeExtractionManager(
            context = context,
            chatSessionManager = chatSessionManager,
            artifactRepository = artifactRepository
        )
    }
    
    // Load artifacts
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                artifacts = artifactRepository.getAllArtifactsForUser(currentUser.id)
            } catch (e: Exception) {
                // Handle error - maybe show a snackbar
            }
        }
    }
    
    // Function to toggle favorite status
    val onToggleFavorite: (Artifact) -> Unit = { artifact ->
        scope.launch {
            if (currentUser != null) {
                try {
                    val result = artifactRepository.toggleArtifactFavorite(artifact.id, currentUser.id)
                    if (result.isSuccess) {
                        // Reload artifacts to reflect changes
                        artifacts = artifactRepository.getAllArtifactsForUser(currentUser.id)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
    
    val userArtifacts = remember(artifacts, currentUser, selectedFilter, searchQuery) {
        if (currentUser != null) {
            var filteredArtifacts = artifacts.filter { it.userId == currentUser.id }
            
            // Apply category filter
            filteredArtifacts = when (selectedFilter) {
                ArtifactFilter.RECIPES -> filteredArtifacts.filter { it.type == ArtifactType.RECIPE }
                ArtifactFilter.GUIDES -> filteredArtifacts.filter { it.type == ArtifactType.DIY_GUIDE || it.type == ArtifactType.SKILL_TUTORIAL }
                ArtifactFilter.FAVORITES -> filteredArtifacts.filter { it.usageStats.bookmarked }
                ArtifactFilter.ALL -> filteredArtifacts
            }
            
            // Apply search filter
            if (searchQuery.isNotBlank()) {
                filteredArtifacts = filteredArtifacts.filter { artifact ->
                    artifact.title.contains(searchQuery, ignoreCase = true) ||
                    artifact.description.contains(searchQuery, ignoreCase = true) ||
                    // Simple search in tags and description for now
                    artifact.tags.any { it.contains(searchQuery, ignoreCase = true) }
                }
            }
            
            filteredArtifacts.sortedByDescending { it.updatedAt }
        } else emptyList()
    }
    
    // Recipe extraction function
    val extractRecipesFromChats: () -> Unit = {
        scope.launch {
            if (currentUser != null && !isExtracting) {
                isExtracting = true
                try {
                    val result = recipeExtractionManager.extractRecipesFromAllChatSessions(currentUser)
                    if (result.isSuccess) {
                        val extractedRecipes = result.getOrNull() ?: emptyList()
                        extractionResultMessage = if (extractedRecipes.isNotEmpty()) {
                            "Successfully extracted ${extractedRecipes.size} recipe${if (extractedRecipes.size == 1) "" else "s"} from your chat history!"
                        } else {
                            "No new recipes found in your chat history."
                        }
                        // Reload artifacts to show new recipes
                        artifacts = artifactRepository.getAllArtifactsForUser(currentUser.id)
                    } else {
                        extractionResultMessage = "Failed to extract recipes: ${result.exceptionOrNull()?.message}"
                    }
                    showExtractionResult = true
                } catch (e: Exception) {
                    extractionResultMessage = "Error during extraction: ${e.message}"
                    showExtractionResult = true
                } finally {
                    isExtracting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "My Recipes & Guides",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // View mode toggle
                    IconButton(
                        onClick = { 
                            viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID 
                        }
                    ) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode",
                            tint = if (isDarkTheme) Color.White else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
                    titleContentColor = if (isDarkTheme) Color.White else Color.Black
                )
            )
        },
        floatingActionButton = {
            if (userArtifacts.isEmpty()) {
                FloatingActionButton(
                    onClick = extractRecipesFromChats,
                    containerColor = Color(0xFF007AFF),
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Extract recipes from chats",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (isDarkTheme) Color(0xFF000000) else Color(0xFFF2F2F7)
                )
        ) {
            // Search and Filter Section
            SearchAndFilterSection(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it },
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Artifacts Content
            if (userArtifacts.isEmpty()) {
                EmptyArtifactsState(
                    selectedFilter = selectedFilter,
                    searchQuery = searchQuery,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (viewMode) {
                    ViewMode.GRID -> {
                        ArtifactsGrid(
                            artifacts = userArtifacts,
                            onArtifactClick = { selectedArtifact = it },
                            onToggleFavorite = onToggleFavorite,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ViewMode.LIST -> {
                        ArtifactsList(
                            artifacts = userArtifacts,
                            onArtifactClick = { selectedArtifact = it },
                            onToggleFavorite = onToggleFavorite,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    
    // Interactive Cooking Overlay
    selectedArtifact?.let { artifact ->
        // TODO: Re-enable InteractiveCookingOverlay when parameters are fixed
        /*
        if (artifact.type == ArtifactType.RECIPE) {
            InteractiveCookingOverlay(
                artifact = artifact,
                sessionManager = cookingSessionManager,
                imageGenerator = imageGenerator,
                onSendMessage = onSendMessage
            )
        } else {
        */
            // For all artifacts, show a simple detailed view for now
            ArtifactDetailDialog(
                artifact = artifact,
                onDismiss = { selectedArtifact = null },
                isDarkTheme = isDarkTheme
            )
        //}
    }
    
    // Recipe extraction result dialog
    if (showExtractionResult) {
        AlertDialog(
            onDismissRequest = { showExtractionResult = false },
            title = {
                Text("Recipe Extraction")
            },
            text = {
                Text(extractionResultMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = { showExtractionResult = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: ArtifactFilter,
    onFilterChange: (ArtifactFilter) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFE5E5EA),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search recipes, ingredients, guides...",
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color(0xFF3C3C43).copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                        unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArtifactFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = filter.displayName,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isDarkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                        selectedContainerColor = if (isDarkTheme) Color(0xFF007AFF) else Color(0xFF007AFF),
                        labelColor = if (isDarkTheme) Color.White else Color.Black,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun ArtifactsGrid(
    artifacts: List<Artifact>,
    onArtifactClick: (Artifact) -> Unit,
    onToggleFavorite: (Artifact) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artifacts) { artifact ->
            ArtifactGridItem(
                artifact = artifact,
                onClick = { onArtifactClick(artifact) },
                onToggleFavorite = { onToggleFavorite(artifact) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun ArtifactsList(
    artifacts: List<Artifact>,
    onArtifactClick: (Artifact) -> Unit,
    onToggleFavorite: (Artifact) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(artifacts) { artifact ->
            ArtifactListItem(
                artifact = artifact,
                onClick = { onArtifactClick(artifact) },
                onToggleFavorite = { onToggleFavorite(artifact) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun ArtifactGridItem(
    artifact: Artifact,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Icon and Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = getArtifactColor(artifact.type).copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getArtifactIcon(artifact.type),
                        contentDescription = null,
                        tint = getArtifactColor(artifact.type),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (artifact.usageStats.bookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (artifact.usageStats.bookmarked) "Remove from favorites" else "Add to favorites",
                        tint = if (artifact.usageStats.bookmarked) Color(0xFFFF3B30) else (if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Title
            Text(
                text = artifact.title,
                color = if (isDarkTheme) Color.White else Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description or content info
            Text(
                text = when (artifact.type) {
                    ArtifactType.RECIPE -> "Recipe • ${artifact.estimatedDuration ?: 30} min"
                    ArtifactType.DIY_GUIDE -> "DIY Guide • ${artifact.difficulty}"
                    ArtifactType.CRAFT_PROJECT -> "Craft • ${artifact.difficulty}"
                    else -> artifact.description
                },
                color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Last accessed date
            Text(
                text = formatArtifactDate(artifact.usageStats.lastAccessedAt ?: artifact.createdAt),
                color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ArtifactListItem(
    artifact: Artifact,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF1C1C1E) else Color.White,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getArtifactColor(artifact.type).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getArtifactIcon(artifact.type),
                    contentDescription = null,
                    tint = getArtifactColor(artifact.type),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artifact.title,
                    color = if (isDarkTheme) Color.White else Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = when (artifact.type) {
                        ArtifactType.RECIPE -> "Recipe • ${artifact.estimatedDuration ?: 30} min"
                        ArtifactType.DIY_GUIDE -> "DIY Guide • ${artifact.difficulty}"
                        ArtifactType.CRAFT_PROJECT -> "Craft • ${artifact.difficulty}"
                        else -> artifact.description
                    },
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (artifact.usageStats.bookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (artifact.usageStats.bookmarked) "Remove from favorites" else "Add to favorites",
                        tint = if (artifact.usageStats.bookmarked) Color(0xFFFF3B30) else (if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = formatArtifactDate(artifact.usageStats.lastAccessedAt ?: artifact.createdAt),
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyArtifactsState(
    selectedFilter: ArtifactFilter,
    searchQuery: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color(0xFFC6C6C8),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when {
                searchQuery.isNotEmpty() -> "No results found"
                selectedFilter != ArtifactFilter.ALL -> "No ${selectedFilter.displayName.lowercase()} found"
                else -> "No artifacts yet"
            },
            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color(0xFF3C3C43),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when {
                searchQuery.isNotEmpty() -> "Try adjusting your search terms"
                selectedFilter != ArtifactFilter.ALL -> "Create some ${selectedFilter.displayName.lowercase()} by chatting with your AI assistants"
                else -> "Start cooking or crafting with your AI assistants to create your first recipes and guides"
            },
            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF3C3C43).copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ArtifactDetailDialog(
    artifact: Artifact,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(artifact.title)
        },
        text = {
            Column {
                Text(artifact.description)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Type: ${artifact.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Medium
                )
                
                if (artifact.estimatedDuration != null) {
                    Text(
                        text = "Duration: ${artifact.estimatedDuration} minutes",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                
                Text(
                    text = "Difficulty: ${artifact.difficulty}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Helper functions and enums

private enum class ViewMode {
    GRID, LIST
}

private enum class ArtifactFilter(
    val displayName: String,
    val icon: ImageVector
) {
    ALL("All", Icons.AutoMirrored.Filled.ViewList),
    RECIPES("Recipes", Icons.Default.Restaurant),
    GUIDES("Guides", Icons.AutoMirrored.Filled.MenuBook),
    FAVORITES("Favorites", Icons.Default.Favorite)
}

private fun getArtifactIcon(type: ArtifactType): ImageVector {
    return when (type) {
        ArtifactType.RECIPE -> Icons.Default.Restaurant
        ArtifactType.DIY_GUIDE -> Icons.AutoMirrored.Filled.MenuBook
        ArtifactType.CRAFT_PROJECT -> Icons.Default.Brush
        ArtifactType.SKILL_TUTORIAL -> Icons.Default.School
        ArtifactType.LEARNING_MODULE -> Icons.Default.Book
    }
}

private fun getArtifactColor(type: ArtifactType): Color {
    return when (type) {
        ArtifactType.RECIPE -> Color(0xFFFF9500)
        ArtifactType.DIY_GUIDE -> Color(0xFF007AFF)
        ArtifactType.CRAFT_PROJECT -> Color(0xFF34C759)
        ArtifactType.SKILL_TUTORIAL -> Color(0xFFAF52DE)
        ArtifactType.LEARNING_MODULE -> Color(0xFF8E8E93)
    }
}

private fun formatArtifactDate(timestamp: Long): String {
    return try {
        val date = Date(timestamp)
        val now = Date()
        val diff = now.time - date.time
        
        when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        "Unknown"
    }
}