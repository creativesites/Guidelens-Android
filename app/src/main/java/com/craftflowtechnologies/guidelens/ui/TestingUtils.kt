package com.craftflowtechnologies.guidelens.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import com.craftflowtechnologies.guidelens.storage.Artifact
import com.craftflowtechnologies.guidelens.storage.ArtifactType
import com.craftflowtechnologies.guidelens.storage.ArtifactContent

/**
 * Testing and debugging utilities for GuideLens
 * Provides mock data, debugging helpers, and testing components
 */
object TestingUtils {
    
    // Mock Data for Testing and Previews
    object MockData {
        
        fun createMockUser(id: String = "test_user_1"): User {
            return User(
                id = id,
                name = "Mwansa Kabwe",
                email = "mwansa.kabwe@example.com",
                profilePicture = null,
                preferences = UserPreferences(
                    favoriteAgent = "cooking",
                    voiceEnabled = true,
                    notifications = true,
                    theme = "dark"
                ),
                avatarUrl = null,
                createdAt = "2024-01-15",
                onboardingCompleted = true,
                preferredAgentId = "cooking"
            )
        }
        
        fun createMockAgents(): List<Agent> {
            return listOf(
                Agent(
                    id = "cooking",
                    name = "Cooking Assistant",
                    description = "Expert in Zambian cuisine and cooking techniques",
                    emoji = "👨‍🍳",
                    primaryColor = Color(0xFFFF6B35),
                    isActive = true
                ),
                Agent(
                    id = "crafting",
                    name = "Crafting Guru",
                    description = "Master of traditional and modern crafts",
                    emoji = "🎨",
                    primaryColor = Color(0xFF4ECDC4),
                    isActive = true
                ),
                Agent(
                    id = "diy",
                    name = "DIY Helper",
                    description = "Your reliable home improvement companion",
                    emoji = "🔧",
                    primaryColor = Color(0xFF45B7D1),
                    isActive = true
                ),
                Agent(
                    id = "buddy",
                    name = "Buddy",
                    description = "Your friendly general assistant",
                    emoji = "🤖",
                    primaryColor = Color(0xFF9B59B6),
                    isActive = true
                )
            )
        }
        
        fun createMockChatSessions(userId: String = "test_user_1"): List<ChatSession> {
            val now = System.currentTimeMillis()
            return listOf(
                ChatSession(
                    id = "session_1",
                    name = "Traditional Nshima Recipe",
                    messages = createMockMessages(),
                    agentId = "cooking",
                    userId = userId,
                    createdAt = "2024-01-15 10:30",
                    updatedAt = "2024-01-15 11:30"
                ),
                ChatSession(
                    id = "session_2",
                    name = "Basket Weaving Project",
                    messages = emptyList(),
                    agentId = "crafting",
                    userId = userId,
                    createdAt = "2024-01-14 09:15",
                    updatedAt = "2024-01-14 10:15"
                ),
                ChatSession(
                    id = "session_3",
                    name = "Fixing Kitchen Sink",
                    messages = emptyList(),
                    agentId = "diy",
                    userId = userId,
                    createdAt = "2024-01-13 14:45",
                    updatedAt = "2024-01-13 15:45"
                )
            )
        }
        
        private fun createMockMessages(): List<ChatMessage> {
            return listOf(
                ChatMessage(
                    id = "msg_1",
                    text = "Hello! I'd like to learn how to make traditional nshima.",
                    isFromUser = true,
                    timestamp = "10:30"
                ),
                ChatMessage(
                    id = "msg_2",
                    text = "Mwapoleni! I'm excited to help you make authentic Zambian nshima. This is our staple food and I'll guide you through the traditional method. Do you have mealie meal ready?",
                    isFromUser = false,
                    timestamp = "10:31"
                ),
                ChatMessage(
                    id = "msg_3",
                    text = "Yes, I have fine mealie meal. What's the first step?",
                    isFromUser = true,
                    timestamp = "10:32"
                )
            )
        }
        
        fun createMockArtifacts(): List<Artifact> {
            val now = System.currentTimeMillis()
            return listOf(
                Artifact(
                    id = "recipe_1",
                    type = ArtifactType.RECIPE,
                    title = "Traditional Nshima",
                    description = "Step-by-step guide to making perfect nshima",
                    agentType = "cooking",
                    userId = "test_user_1",
                    createdAt = now - 86400000,
                    updatedAt = now - 86400000,
                    tags = listOf("zambian", "staple", "traditional"),
                    contentData = ArtifactContent.TextContent("# Traditional Nshima Recipe\n\n## Ingredients\n- 2 cups fine mealie meal\n- 4 cups water\n- Salt (optional)\n\n## Instructions\n1. Boil water in a heavy-bottomed pot...")
                ),
                Artifact(
                    id = "project_1",
                    type = ArtifactType.CRAFT_PROJECT,
                    title = "Chitenge Pillow Cover",
                    description = "Sewing project using traditional chitenge fabric",
                    agentType = "crafting",
                    userId = "test_user_1",
                    createdAt = now - 172800000,
                    updatedAt = now - 172800000,
                    tags = listOf("sewing", "chitenge", "home-decor"),
                    contentData = ArtifactContent.TextContent("# Chitenge Pillow Cover\n\n## Materials\n- 1 yard chitenge fabric\n- Pillow insert\n- Sewing machine\n- Thread")
                ),
                Artifact(
                    id = "instruction_1",
                    type = ArtifactType.DIY_GUIDE,
                    title = "Solar Water Heater",
                    description = "DIY solar water heating system for rural areas",
                    agentType = "diy",
                    userId = "test_user_1",
                    createdAt = now - 259200000,
                    updatedAt = now - 259200000,
                    tags = listOf("solar", "water", "sustainable"),
                    contentData = ArtifactContent.TextContent("# DIY Solar Water Heater\n\n## Materials\n- Black plastic bottles\n- Clear plastic sheeting\n- Insulation materials")
                )
            )
        }
        
        fun createMockZambianColors(): ZambianColorScheme {
            return ZambianColorScheme.default()
        }
        
        fun createMockLocalizationManager(): com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager.CulturalSettings {
            return com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager.CulturalSettings(
                useTraditionalGreetings = true,
                showLocalPrayers = true,
                useLocalTimeFormat = true,
                showSeasonalGuidance = true,
                includeFamilyContext = true,
                respectElders = true,
                communityOriented = true,
                useLocalMeasurements = true,
                showLocalFestivals = true,
                useUbuntuPhilosophy = true
            )
        }
        
        fun createMockLocalFeatures(): com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager.LocalFeatures {
            return com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager.LocalFeatures(
                enableLocalCuisine = true,
                enableTraditionalCrafts = true,
                enableLocalFarming = true,
                enableMiningSupport = true,
                enableLocalBusinessSupport = true,
                enableEducationSupport = true,
                enableCommunityHelp = true
            )
        }
        
        fun createMockErrors(): List<AppError> {
            val colors = createMockZambianColors()
            return listOf(
                AppError.NetworkError(colors),
                AppError.ServerError(colors),
                AppError.AuthError(colors),
                AppError.PermissionError(colors),
                AppError.GeneralError("Something went wrong during testing", colors)
            )
        }
        
        // Format timestamp for display
        fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }
    
    // Debug Helpers
    object DebugHelpers {
        
        @Composable
        fun isInPreviewMode(): Boolean {
            return LocalInspectionMode.current
        }
        
        @Composable
        fun DebugBorder(
            color: Color = Color.Red,
            width: androidx.compose.ui.unit.Dp = 1.dp,
            content: @Composable () -> Unit
        ) {
            if (isInPreviewMode()) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.drawBehind {
                        drawRect(
                            color = color,
                            style = Stroke(width = width.toPx())
                        )
                    }
                ) {
                    content()
                }
            } else {
                content()
            }
        }
        
        fun logCompositionInfo(tag: String, info: String) {
            if (TestingUtils.BuildConfig.DEBUG) {
                android.util.Log.d("Compose_$tag", info)
            }
        }
        
        @Composable
        fun CompositionLogger(tag: String) {
            val composition = rememberUpdatedState(Unit)
            LaunchedEffect(composition.value) {
                logCompositionInfo(tag, "Composition occurred")
            }
        }
        
        // Performance monitoring
        @Composable
        fun PerformanceMonitor(
            tag: String,
            content: @Composable () -> Unit
        ) {
            val startTime = remember { System.currentTimeMillis() }
            
            DisposableEffect(Unit) {
                onDispose {
                    val endTime = System.currentTimeMillis()
                    logCompositionInfo(tag, "Composition time: ${endTime - startTime}ms")
                }
            }
            
            content()
        }
    }
    
    // Test Scenarios
    object TestScenarios {
        
        data class TestScenario(
            val name: String,
            val description: String,
            val mockData: () -> Any,
            val expectedBehavior: String
        )
        
        val allScenarios = listOf(
            TestScenario(
                name = "Empty Chat State",
                description = "User has no chat sessions",
                mockData = { emptyList<ChatSession>() },
                expectedBehavior = "Should show empty state with new chat option"
            ),
            TestScenario(
                name = "Network Error",
                description = "Network connection is unavailable",
                mockData = { AppError.NetworkError(MockData.createMockZambianColors()) },
                expectedBehavior = "Should show network error with retry option"
            ),
            TestScenario(
                name = "Long Message List",
                description = "Chat with many messages for performance testing",
                mockData = { 
                    (1..100).map { index ->
                        ChatMessage(
                            id = "msg_$index",
                            text = "This is test message number $index for performance testing",
                            isFromUser = index % 2 == 0,
                            timestamp = "${10 + index % 12}:${index % 60}"
                        )
                    }
                },
                expectedBehavior = "Should handle large lists efficiently with lazy loading"
            ),
            TestScenario(
                name = "Accessibility Mode",
                description = "User with accessibility needs",
                mockData = { 
                    AccessibilityUtils.AccessibilitySettings(
                        largeText = true,
                        highContrast = true,
                        screenReaderEnabled = true,
                        fontSizeMultiplier = 1.5f,
                        buttonSizeMultiplier = 1.3f
                    )
                },
                expectedBehavior = "Should adapt UI for accessibility requirements"
            ),
            TestScenario(
                name = "Offline Mode",
                description = "No internet connection available",
                mockData = { false }, // isConnected
                expectedBehavior = "Should show cached content and offline indicators"
            ),
            TestScenario(
                name = "New User Onboarding",
                description = "First time user experience",
                mockData = { 
                    MockData.createMockUser().copy(onboardingCompleted = false)
                },
                expectedBehavior = "Should guide user through onboarding flow"
            )
        )
        
        fun getScenario(name: String): TestScenario? {
            return allScenarios.find { it.name == name }
        }
    }
    
    // UI Test Helpers
    object UITestHelpers {
        
        const val CHAT_INPUT_FIELD = "chat_input_field"
        const val SEND_BUTTON = "send_button"
        const val AGENT_SELECTOR = "agent_selector"
        const val SETTINGS_BUTTON = "settings_button"
        const val SIDEBAR_TOGGLE = "sidebar_toggle"
        const val MESSAGE_LIST = "message_list"
        const val VOICE_BUTTON = "voice_button"
        const val CAMERA_BUTTON = "camera_button"
        
        // Common test tags
        fun messageItem(messageId: String) = "message_item_$messageId"
        fun agentButton(agentId: String) = "agent_button_$agentId"
        fun sessionItem(sessionId: String) = "session_item_$sessionId"
        fun settingsItem(itemName: String) = "settings_item_$itemName"
        
        // Test utilities
        fun waitForComposition(timeoutMs: Long = 5000L) {
            // TODO: Implement proper composition waiting
        }
        
        fun simulateNetworkDelay(delayMs: Long = 1000L) {
            // TODO: Implement network delay simulation
        }
    }
    
    // Mock API Responses
    object MockApiResponses {
        
        val successfulChatResponse = """
        {
            "message": "Mwapoleni! I'd be happy to help you with that recipe.",
            "confidence": 0.95,
            "suggestions": [
                "Would you like me to show you the traditional method?",
                "Do you have all the ingredients ready?",
                "Should we start with the preparation steps?"
            ]
        }
        """.trimIndent()
        
        val errorResponse = """
        {
            "error": "rate_limit_exceeded",
            "message": "Too many requests. Please try again later.",
            "retry_after": 60
        }
        """.trimIndent()
        
        val authErrorResponse = """
        {
            "error": "authentication_required",
            "message": "Please sign in to continue.",
            "redirect_url": "/auth/login"
        }
        """.trimIndent()
        
        // Helper to create random delays for realistic testing
        fun randomDelay(): Long {
            return (500L..3000L).random()
        }
    }
    
    // Build configuration helpers
    object BuildConfig {
        const val DEBUG = true // This would typically come from actual BuildConfig
        const val VERSION_NAME = "1.0.0"
        const val VERSION_CODE = 1
    }
}

/**
 * Testing extensions for better test readability
 */
fun String.asTestTag(): String = "test_$this"

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(this))
    }
}

/**
 * Preview parameter providers for Compose previews
 */
class UserPreviewParameterProvider : androidx.compose.ui.tooling.preview.PreviewParameterProvider<User> {
    override val values = sequenceOf(
        TestingUtils.MockData.createMockUser(),
        TestingUtils.MockData.createMockUser("user_2").copy(
            name = "Chipo Mwamba",
            email = "chipo.mwamba@example.com"
        )
    )
}

class ZambianColorsPreviewParameterProvider : androidx.compose.ui.tooling.preview.PreviewParameterProvider<ZambianColorScheme> {
    override val values = sequenceOf(
        ZambianColorScheme.default(),
        ZambianColorScheme.highContrast(),
        ZambianColorScheme.muted()
    )
}