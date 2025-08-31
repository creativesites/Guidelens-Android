package com.craftflowtechnologies.guidelens.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.craftflowtechnologies.guidelens.api.RealtimeApiManager
import com.craftflowtechnologies.guidelens.api.GeminiTextClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.plus


// Empty initial messages - start with clean chat
fun getSampleMessages(): List<ChatMessage> {
    return emptyList()
}

suspend fun getBotResponse(userMessage: String, agent: Agent): String {
    return try {
        val geminiClient = GeminiTextClient()
        val result = geminiClient.generateContent(userMessage, agent.id)
        
        result.getOrElse { throwable ->
            // Return the actual error message for debugging
            "❌ API Error: ${throwable.message}"
        }
    } catch (e: Exception) {
        // Return actual exception details for debugging
        "❌ Exception: ${e.message ?: e.javaClass.simpleName}"
    }
}


// Enhanced video call state for better UI testing


// TODO: Screen capture functionality will be implemented here
// This will take silent screenshots at intervals for the vision model
// to analyze what the user is doing and provide contextual help
object VideoCallScreenCapture {
    // TODO: Implement screen capture with proper permissions
    // fun startScreenCapture(context: Context, interval: Long = 5000L)
    // fun stopScreenCapture()
    // fun processScreenshot(bitmap: Bitmap): String // Vision model analysis
}

// TODO: Integration points for actual implementation
object VideoCallIntegration {
    // TODO: Connect to RealtimeApiManager for actual voice processing
    // fun connectToRealtimeApi(apiManager: RealtimeApiManager)

    // TODO: Implement vision model integration for screen analysis
    // fun analyzeScreenContent(screenshot: Bitmap): String

    // TODO: Handle actual video call functionality
    // fun startVideoCall(agent: Agent)
    // fun endVideoCall()
}

// Helper functions for sample responses
fun getSampleUserQuery(agent: Agent): String {
    return when (agent.id) {
        "cooking" -> "Can you suggest a quick dinner recipe?"
        "crafting" -> "How do I make a simple paper craft?"
        "companion" -> "I'm feeling a bit stressed, can you help?"
        "diy" -> "How do I fix a leaky faucet?"
        else -> "Can you help me with something?"
    }
}

fun getSampleBotResponse(agent: Agent): String {
    return when (agent.id) {
        "cooking" -> "Here's a quick 15-minute pasta recipe: Cook spaghetti, sauté garlic and cherry tomatoes in olive oil, add fresh basil and parmesan. Toss together and serve!"
        "crafting" -> "Try making an origami crane: Fold a square paper diagonally, follow these steps [simplified instructions] for a beautiful result."
        "companion" -> "Of course! Let's try this together: breathe in for 4 seconds, hold for 7, breathe out for 8. I'll do it with you!"
        "diy" -> "To fix a leaky faucet, turn off water supply, remove handle, replace the O-ring or washer, and reassemble. Test for leaks."
        else -> "Sure, I'm here to help! What do you need?"
    }
}

fun getFeatureResponse(feature: Any, agent: Agent): String {
    return when (agent.id) {
        "cooking" -> {
            val cookingFeature = feature as CookingFeature
            when (cookingFeature.id) {
                "recipe_finder" -> "Let's find a recipe! What ingredients do you have on hand?"
                "meal_planner" -> "I can help plan your weekly meals. What's your favorite cuisine?"
                "nutrition_tracker" -> "Let's track nutrition! What food would you like to analyze?"
                "cooking_timer" -> "I'll set a cooking timer for you. How long do you need?"
                "technique_guide" -> "Want to learn a cooking technique? Try knife skills or perfect boiling!"
                "substitutions" -> "Need an ingredient substitute? What are you looking to replace?"
                else -> "How can I assist with your cooking needs?"
            }
        }
        "crafting" -> {
            val craftingProject = feature as CraftingProject
            when (craftingProject.id) {
                "beginner_projects" -> "Let's start with an easy paper craft. Got scissors and paper?"
                "home_decor" -> "Create stunning home decor! Try a painted mason jar vase."
                "holiday_crafts" -> "Holiday crafts are fun! Let's make a festive wreath."
                "upcycling" -> "Upcycle old items into treasures! Try turning jars into lanterns."
                else -> "What craft would you like to create today?"
            }
        }
        "companion" -> {
            val friendshipTool = feature as FriendshipTool
            when (friendshipTool.id) {
                "breathing_exercise" -> "Let's take some deep breaths together. I'll guide you through it, friend!"
                "mindfulness" -> "Let's be present in this moment together. I'm right here with you."
                "mood_tracker" -> "How are you feeling today? I'm here to listen, no matter what."
                "gratitude_journal" -> "Let's share some good things! I'll start: I'm grateful for our friendship!"
                else -> "I'm here as your friend. What can we do together?"
            }
        }
        "diy" -> {
            val diyCategory = feature as DIYCategory
            when (diyCategory.id) {
                "plumbing" -> "Fix that leak! Here's how to replace a faucet washer..."
                "electrical" -> "Electrical fix? Let's safely replace an outlet."
                "carpentry" -> "Build a shelf! Here's a simple woodworking guide."
                "painting" -> "Time to paint! Here's how to prep and paint a room."
                else -> "What DIY project would you like to tackle?"
            }
        }
        else -> "How can I assist you today?"
    }
}
