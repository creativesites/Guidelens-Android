package com.craftflowtechnologies.guidelens.ui


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Carpenter
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import java.util.UUID
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageType {
    TEXT,
    SYSTEM,
    WELCOME,
    ERROR
}

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: String,
    val fullTimestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val isVoiceMessage: Boolean = false,
    val hasAttachment: Boolean = false,
    val images: List<String> = emptyList(), // Base64 encoded images
    val userInteractions: MessageInteractions = MessageInteractions(),
    val generatedImage: String? = null,
)

@Serializable
data class MessageInteractions(
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isCopied: Boolean = false,
    val regenerationCount: Int = 0
)

data class EnhancedMessageContent(
    val mainContent: List<ContentElement>,
    val thinkingSteps: String? = null,
    val actions: List<Pair<String, String>> = emptyList(), // Action label, action text
    val tips: List<String> = emptyList()
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val messages: List<ChatMessage>,
    val agentId: String,
    val userId: String,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val updatedAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
    val isOffline: Boolean = false
)

data class Agent(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector = Icons.Default.Psychology,
    val primaryColor: Color,
    val secondaryColor: Color = primaryColor,
    val features: List<Any> = emptyList(),
    val quickActions: List<String> = emptyList(),
    val specialCapabilities: List<String> = emptyList(),
    val image: String = "",
    val emoji: String = "",
    val isActive: Boolean = true
)

data class TranscriptionState(
    val currentText: String = "", // Default to empty string
    val isUserSpeaking: Boolean = false, // Default to false
    val confidence: Float = 1.0f
)

sealed class ContentElement {
    data class Text(val text: String) : ContentElement()
    data class Heading(val text: String, val level: Int) : ContentElement()
    data class ListItem(val text: String, val isOrdered: Boolean, val indentLevel: Int) : ContentElement()
    data class Blockquote(val text: String) : ContentElement()
    data class CodeBlock(val text: String) : ContentElement()
}

data class VideoCallState(
    val isUserMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSessionActive: Boolean = true,
    val isRecording: Boolean = false,
    val callDuration: String = "00:00",
    val connectionQuality: String = "Excellent",
    val isScreenSharing: Boolean = false,
    val participantCount: Int = 2,
    val networkStrength: Int = 5
)


// Enhanced Agent data structures with specific features
data class CookingFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: String,
    val tags: List<String> = emptyList(),
    val difficulty: String = "Easy",
    val estimatedTime: String = "5-10 minutes"
)
data class VoiceState(
    val isListening: Boolean = false,
    val isMuted: Boolean = false,
    val isBotMuted: Boolean = false,
    val isProcessing: Boolean = false,
    val confidence: Float = 0f,
    val lastHeardText: String = ""
)

data class CraftingProject(
    val id: String,
    val title: String,
    val difficulty: String,
    val timeRequired: String,
    val materials: List<String>,
    val icon: ImageVector,
    val steps: List<String> = emptyList(),
    val category: String = "General",
    val skillsLearned: List<String> = emptyList()
)

data class FriendshipTool(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val duration: String,
    val category: String = "Support",
    val benefits: List<String> = emptyList(),
    val difficulty: String = "Easy"
)

data class DIYCategory(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val urgency: String,
    val safetyLevel: String = "Medium",
    val toolsRequired: List<String> = emptyList(),
    val averageCost: String = "$$"
)

// Enhanced Agent with specific features


// Agent-specific feature data
// Enhanced Cooking Features with more detailed actions
val COOKING_FEATURES = listOf(
    CookingFeature(
        id = "recipe_finder",
        title = "Recipe Finder",
        description = "Find recipes based on ingredients you have",
        icon = Icons.Default.Search,
        action = "What ingredients do you have?",
        tags = listOf("ingredients", "search", "discovery")
    ),
    CookingFeature(
        id = "meal_planner",
        title = "Meal Planner",
        description = "Plan your weekly meals and shopping",
        icon = Icons.Default.CalendarToday,
        action = "Let's plan your meals for this week",
        tags = listOf("planning", "weekly", "organization")
    ),
    CookingFeature(
        id = "nutrition_tracker",
        title = "Nutrition Guide",
        description = "Track nutritional information and health goals",
        icon = Icons.Default.MonitorWeight,
        action = "Tell me about nutritional values",
        tags = listOf("health", "nutrition", "tracking")
    ),
    CookingFeature(
        id = "cooking_timer",
        title = "Smart Timer",
        description = "Set multiple cooking timers with voice control",
        icon = Icons.Default.Timer,
        action = "Set a cooking timer",
        tags = listOf("timing", "voice", "alerts")
    ),
    CookingFeature(
        id = "technique_guide",
        title = "Cooking Techniques",
        description = "Learn professional cooking techniques",
        icon = Icons.Default.School,
        action = "Show me cooking techniques",
        tags = listOf("learning", "skills", "techniques")
    ),
    CookingFeature(
        id = "substitutions",
        title = "Smart Substitutions",
        description = "Find ingredient alternatives and ratios",
        icon = Icons.Default.SwapHoriz,
        action = "I need ingredient substitutions",
        tags = listOf("alternatives", "ratios", "flexibility")
    )
)


val CRAFTING_PROJECTS = listOf(
    CraftingProject(
        id = "beginner_projects",
        title = "Beginner Projects",
        difficulty = "Easy",
        timeRequired = "30 min - 2 hours",
        materials = listOf("Paper", "Glue", "Scissors"),
        icon = Icons.Default.Star
    ),
    CraftingProject(
        id = "home_decor",
        title = "Home Decor",
        difficulty = "Medium",
        timeRequired = "2-6 hours",
        materials = listOf("Fabric", "Paint", "Wood"),
        icon = Icons.Default.Home
    ),
    CraftingProject(
        id = "holiday_crafts",
        title = "Holiday Crafts",
        difficulty = "Easy-Medium",
        timeRequired = "1-4 hours",
        materials = listOf("Seasonal items", "Ribbon", "Glitter"),
        icon = Icons.Default.Celebration
    ),
    CraftingProject(
        id = "upcycling",
        title = "Upcycling Projects",
        difficulty = "Medium-Hard",
        timeRequired = "3-8 hours",
        materials = listOf("Old items", "Paint", "Tools"),
        icon = Icons.Default.Recycling
    )
)

val FRIENDSHIP_TOOLS = listOf(
    FriendshipTool(
        id = "breathing_exercise",
        title = "Calm Together",
        description = "Let's take some deep breaths together",
        icon = Icons.Default.Air,
        duration = "5-10 minutes"
    ),
    FriendshipTool(
        id = "mindfulness",
        title = "Present Moment",
        description = "Let's be mindful and present together",
        icon = Icons.Default.Psychology,
        duration = "10-20 minutes"
    ),
    FriendshipTool(
        id = "mood_tracker",
        title = "How Are You?",
        description = "Tell me how you're feeling today",
        icon = Icons.Default.Mood,
        duration = "3-5 minutes"
    ),
    FriendshipTool(
        id = "gratitude_journal",
        title = "Good Things",
        description = "Let's share what we're grateful for",
        icon = Icons.Default.Edit,
        duration = "5-10 minutes"
    )
)

val DIY_CATEGORIES = listOf(
    DIYCategory(
        id = "plumbing",
        title = "Plumbing Fixes",
        description = "Leaks, clogs, and installations",
        icon = Icons.Default.Plumbing,
        urgency = "High"
    ),
    DIYCategory(
        id = "electrical",
        title = "Electrical Work",
        description = "Wiring, outlets, and fixtures",
        icon = Icons.Default.ElectricalServices,
        urgency = "High"
    ),
    DIYCategory(
        id = "carpentry",
        title = "Carpentry Projects",
        description = "Building and woodworking",
        icon = Icons.Default.Carpenter,
        urgency = "Medium"
    ),
    DIYCategory(
        id = "painting",
        title = "Painting & Finishing",
        description = "Interior and exterior painting",
        icon = Icons.Default.FormatPaint,
        urgency = "Low"
    )
)
val FRIENDSHIP_ACTIVITIES = listOf(
    FriendshipTool(
        id = "breathing_exercise",
        title = "Calm Together",
        description = "Let's practice some breathing techniques as friends",
        icon = Icons.Default.Air,
        duration = "5-10 minutes"
    ),
    FriendshipTool(
        id = "mindfulness",
        title = "Present Moment",
        description = "Let's be mindful and present together",
        icon = Icons.Default.Psychology,
        duration = "10-20 minutes"
    ),
    FriendshipTool(
        id = "mood_tracker",
        title = "How Are You?",
        description = "Tell me how you're feeling - I'm here to listen",
        icon = Icons.Default.Mood,
        duration = "3-5 minutes"
    ),
    FriendshipTool(
        id = "gratitude_journal",
        title = "Good Things",
        description = "Let's share the good things in our lives",
        icon = Icons.Default.Edit,
        duration = "5-10 minutes"
    ),
    FriendshipTool(
        id = "stress_relief",
        title = "Feeling Better",
        description = "Let's find ways to feel better together",
        icon = Icons.Default.Spa,
        duration = "5-15 minutes"
    ),
    FriendshipTool(
        id = "energy_boost",
        title = "Pick Me Up",
        description = "Let's boost your energy and mood",
        icon = Icons.Default.Bolt,
        duration = "3-8 minutes"
    )
)
// Enhanced Agent data with features
// Enhanced Agent data with features - Updated Mental Health Agent Name
val AGENTS = listOf(
    Agent(
        id = "cooking",
        name = "Cooking Assistant",
        description = "Culinary expert and recipe helper",
        icon = Icons.Default.Restaurant,
        primaryColor = Color(0xFFD97706), // Muted amber
        secondaryColor = Color(0xFFDC2626), // Muted red
        features = COOKING_FEATURES,
        quickActions = listOf("Find Recipe", "Set Timer", "Convert Units", "Plan Meals"),
        specialCapabilities = listOf("Voice Recipe Reading", "Step-by-step Guidance", "Ingredient Scanner"),
        image = "cooking_agent"
    ),
    Agent(
        id = "crafting",
        name = "Crafting Guru",
        description = "Expert in DIY crafts and creative projects",
        icon = Icons.Default.Build,
        primaryColor = Color(0xFF4F46E5), // Muted indigo
        secondaryColor = Color(0xFF7C3AED), // Muted purple
        features = CRAFTING_PROJECTS,
        quickActions = listOf("Project Ideas", "Material List", "Tutorial", "Skill Level"),
        specialCapabilities = listOf("Visual Tutorials", "Progress Tracking", "Community Projects"),
        image = "crafting_agent"
    ),
    Agent(
        id = "companion",
        name = "Buddy",
        description = "Your caring friend who's always here to listen and support",
        icon = Icons.Default.Favorite,
        primaryColor = Color(0xFF059669), // Muted emerald
        secondaryColor = Color(0xFF0891B2), // Muted cyan
        features = FRIENDSHIP_ACTIVITIES,
        quickActions = listOf("How Are You?", "Calm Together", "Present Moment", "Good Things"),
        specialCapabilities = listOf("Caring Support", "Friendly Chats", "Positive Vibes"),
        image = "companion_agent"
    ),
    Agent(
        id = "diy",
        name = "DIY Helper",
        description = "Home improvement and repair specialist",
        icon = Icons.Default.Settings,
        primaryColor = Color(0xFF9333EA), // Muted violet
        secondaryColor = Color(0xFFEC4899), // Muted pink
        features = DIY_CATEGORIES,
        quickActions = listOf("Emergency Fix", "Tool Guide", "Safety Tips", "Cost Estimate"),
        specialCapabilities = listOf("AR Measurements", "Tool Identification", "Safety Alerts"),
        image = "diy_agent"
    )
)

class ChatViewModel : ViewModel() {
    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _selectedImages = mutableStateListOf<ImageData>()
    val selectedImages: List<ImageData> = _selectedImages

    fun handleMessageInteraction(messageId: String, action: String) {
        _messages.replaceAll { message ->
            if (message.id == messageId) {
                val interactions = message.userInteractions.copy(
                    isLiked = if (action == "like") !message.userInteractions.isLiked else message.userInteractions.isLiked,
                    isDisliked = if (action == "dislike") !message.userInteractions.isDisliked else message.userInteractions.isDisliked,
                    isCopied = if (action == "copy") true else message.userInteractions.isCopied
                )
                message.copy(userInteractions = interactions)
            } else message
        }
    }

    fun regenerateMessage(messageId: String) {
        _messages.replaceAll { message ->
            if (message.id == messageId) {
                val newInteractions = message.userInteractions.copy(
                    regenerationCount = message.userInteractions.regenerationCount + 1
                )
                message.copy(userInteractions = newInteractions)
            } else message
        }
        // Trigger regeneration logic (call API with original prompt)
    }

    fun addMessage(text: String, isFromUser: Boolean, images: List<String> = emptyList(), generatedImage: String? = null) {
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        _messages.add(
            ChatMessage(
                text = text,
                isFromUser = isFromUser,
                timestamp = timestamp,
                images = images,
                generatedImage = generatedImage,
                hasAttachment = images.isNotEmpty() || generatedImage != null
            )
        )
    }

    fun addSelectedImages(images: List<ImageData>) {
        _selectedImages.addAll(images)
    }

    fun handleApiError(errorMessage: String, images: List<String>) {
        addMessage(
            text = "Error: $errorMessage",
            isFromUser = false,
            images = images
        )
    }
}

data class ImageData(
    val base64: String,
    val mimeType: String
)
// Updated Mental Health Tools to Wellness Tools



