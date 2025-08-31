package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.craftflowtechnologies.guidelens.ai.OfflineModelManager
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class EnhancedGeminiClient(
    private val context: Context,
    private val offlineModelManager: OfflineModelManager,
    private val xaiImageClient: XAIImageClient = XAIImageClient(context) // Added default instantiation
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true // Ensure default values are serialized
    }

    private val prefs = context.getSharedPreferences("image_generation", Context.MODE_PRIVATE)
    private val DAILY_IMAGE_LIMIT = 30 // For testing
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    private val conversationHistory = mutableListOf<GeminiContent>()

    companion object {
        private const val TAG = "EnhancedGeminiClient"

        // Enhanced image generation keywords for all agents
        private val IMAGE_KEYWORDS = listOf(
            // Cooking keywords
            "recipe", "food", "cooking", "dish", "ingredient", "meal", "cuisine", "bake", "roast", "grill",
            // Crafting keywords
            "craft", "project", "tool", "material", "design", "pattern", "handmade", "diy craft", "arts", "creative",
            // DIY keywords
            "diy", "repair", "build", "construction", "home improvement", "workshop", "tools", "fix",
            // General keywords
            "friendship", "activity", "game", "art", "learning", "show me", "visualize", "example", "demonstration",
            "generate image", "create image", "show image", "make image", "illustration", "picture"
        )
    }

    suspend fun generateContent(
        prompt: String,
        agentType: String = "buddy",
        includeHistory: Boolean = true,
        forceOffline: Boolean = false,
        images: List<String> = emptyList(),
        enableImageGeneration: Boolean = false
    ): Result<EnhancedResponse> = withContext(Dispatchers.IO) {
        offlineModelManager.updateNetworkStatus()
        val shouldUseOffline = offlineModelManager.shouldUseOfflineMode() || forceOffline
        val isOfflineModeAvailable = offlineModelManager.isOfflineModeAvailable()

        Log.d(TAG, "Network status: online=${offlineModelManager.isOnline.value}, " +
                "shouldUseOffline=$shouldUseOffline, offlineAvailable=$isOfflineModeAvailable")

        if (shouldUseOffline && isOfflineModeAvailable) {
            Log.d(TAG, "Using offline model")
            generateOfflineResponse(prompt, agentType, includeHistory, images)
                .map { EnhancedResponse(it, null, 1.0f) }
        } else {
            Log.d(TAG, "Attempting online Gemini API")
            var lastError: Exception? = null
            repeat(2) { attempt ->
                val result = generateOnlineResponse(prompt, agentType, includeHistory, images, enableImageGeneration)
                if (result.isSuccess) return@withContext result
                lastError = result.exceptionOrNull() as Exception?
                Log.w(TAG, "Online attempt ${attempt + 1} failed: ${lastError?.message}")
                if (lastError?.message?.contains("Bad request") == true) {
                    return@withContext Result.failure(lastError!!)
                }
                delay(1000)
            }
            Log.w(TAG, "All online attempts failed", lastError)
            if (isOfflineModeAvailable && !offlineModelManager.isOnline.value) {
                Log.w(TAG, "Falling back to offline mode")
                generateOfflineResponse(prompt, agentType, includeHistory, images)
                    .map { EnhancedResponse(it, null, 1.0f) }
            } else {
                Result.failure(Exception("Failed to get response: ${lastError?.message}"))
            }
        }
    }

    private suspend fun generateOnlineResponse(
        prompt: String,
        agentType: String,
        includeHistory: Boolean,
        images: List<String>,
        enableImageGeneration: Boolean = false
    ): Result<EnhancedResponse> = withContext(Dispatchers.IO)  {
        try {
            if (GeminiConfig.GEMINI_API_KEY.isBlank()) {
                Log.e(TAG, "API key is not configured")
                return@withContext Result.failure(Exception("Gemini API key is not configured"))
            }

            if (prompt.length > 10000) {
                Log.e(TAG, "Prompt too long: ${prompt.length} characters")
                return@withContext Result.failure(Exception("Prompt is too long"))
            }

            val systemPrompt = getSystemPromptForAgent(agentType)
            val contents = mutableListOf<GeminiContent>()

            // Add system instructions
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt)),
                    role = "user"
                )
            )

            // Add conversation history
            if (includeHistory && conversationHistory.isNotEmpty()) {
                val validHistory = conversationHistory.takeLast(10).filter {
                    it.role == "user" || it.role == "model"
                }
                contents.addAll(validHistory)
                Log.d(TAG, "Added ${validHistory.size} valid history items")
            }

            // Add current user message with images
            val parts = mutableListOf<GeminiPart>()
            if (prompt.isNotBlank()) {
                parts.add(GeminiPart(text = prompt))
            }
            images.forEachIndexed { index, image ->
                try {
                    val decoded = android.util.Base64.decode(image, android.util.Base64.DEFAULT)
                    if (decoded.size > 20 * 1024 * 1024) {
                        Log.e(TAG, "Image $index exceeds 20MB limit")
                        return@withContext Result.failure(Exception("Image $index is too large (max 20MB)"))
                    }
                    val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    if (bitmap == null) {
                        Log.e(TAG, "Invalid image format at index $index")
                        return@withContext Result.failure(Exception("Invalid image format at index $index"))
                    }
                    parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = image)))
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid base64 image at index $index", e)
                    return@withContext Result.failure(Exception("Invalid image format at index $index"))
                }
            }
            if (parts.isEmpty()) {
                Log.e(TAG, "No valid content to send")
                return@withContext Result.failure(Exception("No valid prompt or images provided"))
            }
            contents.add(GeminiContent(parts = parts, role = "user"))

            // Log contents before serialization
            Log.d(TAG, "Contents to send: $contents")

            val requestBody = GeminiRequest(
                contents = contents,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 2048,
                    stopSequences = emptyList()
                ),
                safetySettings = listOf(
                    GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
                )
            )

            val requestJson = json.encodeToString(requestBody)
            Log.d(TAG, "Request JSON: $requestJson")

            val request = Request.Builder()
                .url("$baseUrl/gemini-1.5-flash:generateContent?key=${GeminiConfig.GEMINI_API_KEY}")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Response: HTTP ${response.code}, Body: $responseBody")
            if (response.isSuccessful) {
                val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
                val candidate = geminiResponse.candidates.firstOrNull()?.content
                var responseText: String? = null
                var generatedImage: String? = null

                if (candidate != null) {
                    candidate.parts.forEach { part ->
                        if (part.text != null) {
                            responseText = part.text
                        }
                        if (part.inlineData != null && part.inlineData.mimeType.startsWith("image/")) {
                            generatedImage = part.inlineData.data
                        }
                    }
                }

                if (responseText != null) {
                    // Enhanced image generation using xAI for all agents
                    if (generatedImage == null && enableImageGeneration && shouldGenerateImage(responseText!!)) {
                        val imageDescription = extractImageDescription(responseText!!, agentType)
                        if (imageDescription.isNotEmpty() && !hasReachedDailyLimit()) {
                            try {
                                val imageResult = generateXAIImage(imageDescription, agentType)
                                if (imageResult.isSuccess) {
                                    generatedImage = imageResult.getOrNull()
                                    incrementDailyImageCount()
                                    Log.d(TAG, "Successfully generated xAI image: $imageDescription")
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to generate xAI image: ${e.message}")
                            }
                        }
                    }

                    // Remove [Image: description] tag if no image was generated
                    val imageMatch = Regex("\\[Image:([^\\]]*?)\\]").find(responseText!!)
                    if (imageMatch != null && generatedImage == null) {
                        responseText = responseText!!.replace(imageMatch.value, "")
                        Log.w(TAG, "No image generated for [Image: ${imageMatch.groupValues[1].trim()}]; tag removed")
                    }

                    conversationHistory.add(
                        GeminiContent(
                            parts = parts,
                            role = "user"
                        )
                    )
                    conversationHistory.add(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = responseText)),
                            role = "model"
                        )
                    )
                    Result.success(EnhancedResponse(responseText!!, generatedImage, 1.0f))
                } else {
                    Log.e(TAG, "No response text received: $responseBody")
                    geminiResponse.promptFeedback?.blockReason?.let { reason ->
                        Log.e(TAG, "Prompt blocked: $reason")
                        return@withContext Result.failure(Exception("Prompt blocked due to safety: $reason"))
                    }
                    return@withContext Result.failure(Exception("No response text received"))
                }
            } else {
                Log.e(TAG, "API Error: HTTP ${response.code}, Body: $responseBody")
                when (response.code) {
                    400 -> {
                        try {
                            val errorResponse = json.decodeFromString<GeminiErrorResponse>(responseBody)
                            Log.e(TAG, "Error details: ${errorResponse.error.message}")
                            Result.failure(Exception("Bad request: ${errorResponse.error.message}"))
                        } catch (e: Exception) {
                            Result.failure(Exception("Bad request: Check prompt or image format"))
                        }
                    }
                    401 -> Result.failure(Exception("Invalid API key"))
                    403 -> Result.failure(Exception("API access forbidden: Check key permissions"))
                    429 -> Result.failure(Exception("Rate limit exceeded: Try again later"))
                    500 -> Result.failure(Exception("Gemini server error: Try again"))
                    else -> Result.failure(Exception("API Error ${response.code}: $responseBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating content online", e)
            Result.failure(Exception("Failed to generate response: ${e.message}"))
        }
    }

    private suspend fun generateXAIImage(description: String, agentType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Enhanced prompt based on agent type
            val enhancedPrompt = when (agentType) {
                "cooking" -> "Professional food photography: $description, well-lit, appetizing, restaurant quality"
                "crafting" -> "High-quality craft photography: $description, well-lit, detailed, creative workspace"
                "diy" -> "Clear instructional photography: $description, workshop setting, tools visible, step-by-step guide style"
                else -> "High-quality photography: $description, professional lighting, clear details"
            }

            Log.d(TAG, "Generating xAI image with prompt: $enhancedPrompt")
            val result = xaiImageClient.generateImage(enhancedPrompt)

            result.fold(
                onSuccess = { imageUrl ->
                    Log.d(TAG, "xAI image generated successfully: $imageUrl")
                    Result.success(imageUrl)
                },
                onFailure = { error ->
                    Log.w(TAG, "xAI image generation failed: ${error.message}")
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in xAI image generation", e)
            Result.failure(e)
        }
    }

    // Legacy Gemini image generation (keeping as fallback)
    private suspend fun generateImage(description: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Use the standard generateContent endpoint with proper image generation request
            val requestBody = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = "Generate an image: $description")),
                        role = "user"
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 2048,
                    stopSequences = emptyList()
                ),
                safetySettings = listOf(
                    GeminiSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
                    GeminiSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
                )
            )

            val requestJson = json.encodeToString(requestBody)
            val imageRequest = Request.Builder()
                .url("$baseUrl/gemini-1.5-flash:generateContent?key=${GeminiConfig.GEMINI_API_KEY}")
                .post(requestJson.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(imageRequest).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Image Response: HTTP ${response.code}, Body: $responseBody")

            if (response.isSuccessful) {
                val geminiResponse = json.decodeFromString<GeminiResponse>(responseBody)
                val candidate = geminiResponse.candidates.firstOrNull()?.content

                if (candidate != null) {
                    candidate.parts.forEach { part ->
                        if (part.inlineData != null && part.inlineData.mimeType.startsWith("image/")) {
                            return@withContext Result.success(part.inlineData.data)
                        }
                    }
                }

                // If no image was found in the response, return a descriptive failure
                Log.w(TAG, "No image data found in Gemini response. Image generation may not be available.")
                Result.failure(Exception("Image generation not available with current API configuration"))
            } else {
                Log.e(TAG, "Image API Error: HTTP ${response.code}, Body: $responseBody")
                Result.failure(Exception("Failed to generate image: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image", e)
            Result.failure(Exception("Failed to generate image: ${e.message}"))
        }
    }

    private suspend fun generateOfflineResponse(
        prompt: String,
        agentType: String,
        includeHistory: Boolean,
        images: List<String>
    ): Result<String> {
        val historyContext = if (includeHistory && conversationHistory.isNotEmpty()) {
            conversationHistory.takeLast(6).joinToString("\n") { content ->
                "${content.role}: ${content.parts.firstOrNull()?.text}"
            }
        } else ""

        return offlineModelManager.generateOfflineResponse(
            prompt = prompt,
            agentType = agentType,
            historyContext = historyContext
        ).onSuccess { response ->
            conversationHistory.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt)),
                    role = "user"
                )
            )
            conversationHistory.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = response)),
                    role = "model"
                )
            )
        }
    }

    private fun getSystemPromptForAgent(agentType: String): String {
        val basePrompt = when (agentType) {
            "cooking" -> """
                You are a Cooking Assistant in GuideLens, an AI-powered guidance app. Your role is to provide helpful, practical cooking advice.
                
                Key responsibilities:
                - Provide clear, step-by-step cooking instructions
                - Suggest ingredient substitutions when asked
                - Offer cooking tips for technique improvement
                - Prioritize food safety in all recommendations
                - Be encouraging and supportive
                
                Communication style:
                - Be friendly and encouraging
                - Use clear, simple language
                - Provide practical, actionable advice
                - Always prioritize safety
                
                Formatting guidelines:
                - Use Markdown for clear formatting
                - Use # for main headings, ## for subheadings
                - Use - for unordered lists, numbers. for ordered lists
                - Use > for blockquotes to highlight important notes
                - Use ``` for code blocks when sharing recipes or structured data
                - Use [Tip: text] for helpful tips
                - Use [Action: label] for interactive actions (e.g., [Action: View Recipe])
                - Use [Image: description] to indicate a generated image of the finished dish for complete recipes only
                - Use **bold** for emphasis, *italic* for subtle highlights
                - Avoid inconsistent formatting like "* **text**"
                
                Remember: You're helping someone learn and improve their cooking skills. Only include [Image: description] for complete recipes with steps, not for general advice or partial instructions.
            """.trimIndent()

            "crafting" -> """
                You are a Crafting Guru in GuideLens, specializing in arts, crafts, and creative projects.
                
                Key responsibilities:
                - Guide users through craft project steps
                - Suggest materials and tools for projects
                - Provide technique tips and troubleshooting
                - Encourage creativity and experimentation
                - Help with project planning and organization
                
                Communication style:
                - Be patient and detailed
                - Celebrate creativity and effort
                - Provide clear step-by-step guidance
                - Encourage experimentation
                
                Formatting guidelines:
                - Use Markdown for clear formatting
                - Use # for main headings, ## for subheadings
                - Use - for unordered lists, numbers. for ordered lists
                - Use > for blockquotes to highlight important notes
                - Use ``` for code blocks when sharing patterns or instructions
                - Use [Tip: text] for helpful tips
                - Use [Action: label] for interactive actions (e.g., [Action: View Tutorial])
                - Use [Image: description] to indicate a generated image of the finished item for complete tutotials only
                - Use **bold** for emphasis, *italic* for subtle highlights
                - Avoid inconsistent formatting like "* **text**"
                
                Remember: Crafting is about creativity and personal expression.
            """.trimIndent()

            "diy" -> """
                You are a DIY Helper in GuideLens, focused on home improvement and repair projects.
                
                Key responsibilities:
                - Provide safe, practical DIY guidance
                - Emphasize safety procedures and proper tool use
                - Help with project planning and preparation
                - Troubleshoot common DIY problems
                - Recommend when to call professionals
                
                Communication style:
                - Always prioritize safety first
                - Be methodical and systematic
                - Emphasize proper preparation
                - Know when to recommend professional help
                
                Formatting guidelines:
                - Use Markdown for clear formatting
                - Use # for main headings, ## for subheadings
                - Use - for unordered lists, numbers. for ordered lists
                - Use > for blockquotes to highlight safety warnings
                - Use ``` for code blocks when sharing plans or specifications
                - Use [Tip: text] for helpful tips
                - Use [Image: description] to indicate a generated image of the finished item for complete plans only
                - Use [Action: label] for interactive actions (e.g., [Action: View Diagram])
                - Use **bold** for emphasis, *italic* for subtle highlights
                - Avoid inconsistent formatting like "* **text**"
                
                Remember: Safety is paramount in all DIY activities.
            """.trimIndent()

            "buddy", "companion" -> """
                You are Buddy, a friendly general assistant in GuideLens. You help with any task or learning goal.
                
                Key responsibilities:
                - Provide helpful guidance on various topics
                - Be supportive and encouraging
                - Adapt to different types of questions and needs
                - Maintain a friendly, approachable personality
                - Help users learn and accomplish their goals
                
                Communication style:
                - Be warm and supportive
                - Show enthusiasm for helping
                - Adapt your communication to the user's needs
                - Be encouraging and positive
                
                Formatting guidelines:
                - Use Markdown for clear formatting
                - Use # for main headings, ## for subheadings
                - Use - for unordered lists, numbers. for ordered lists
                - Use > for blockquotes to highlight key points
                - Use ``` for code blocks when sharing examples or data
                - Use [Tip: text] for helpful tips
                - Use [Action: label] for interactive actions (e.g., [Action: Learn More])
                - Use [Image: description] to indicate a generated image of the finished activity for complete activities only
                - Use **bold** for emphasis, *italic* for subtle highlights
                - Avoid inconsistent formatting like "* **text**"
                
                Remember: You're a helpful companion for any learning or task.
            """.trimIndent()

            else -> """
                You are a helpful AI assistant in GuideLens. Provide clear, helpful responses to user queries.
                
                Formatting guidelines:
                - Use Markdown for clear formatting
                - Use # for main headings, ## for subheadings
                - Use - for unordered lists, numbers. for ordered lists
                - Use > for blockquotes to highlight key points
                - Use ``` for code blocks when sharing examples or data
                - Use [Tip: text] for helpful tips
                - Use [Action: label] for interactive actions (e.g., [Action: Learn More])
                - Use **bold** for emphasis, *italic* for subtle highlights
                - Avoid inconsistent formatting like "* **text**"
            """.trimIndent()
        }

        return "$basePrompt\n\nProvide helpful, accurate responses in a conversational tone."
    }

    fun clearHistory() {
        conversationHistory.clear()
        Log.d(TAG, "Conversation history cleared")
    }

    fun getConversationLength(): Int = conversationHistory.size

    suspend fun isOnline(): Boolean {
        offlineModelManager.updateNetworkStatus()
        return offlineModelManager.isOnline.value
    }

    fun getOfflineStatus(): String {
        return offlineModelManager.getOfflineCapabilityStatus()
    }

    private fun shouldGenerateImage(text: String): Boolean {
        val lowerText = text.lowercase()
        // Check if text contains image-worthy content
        return IMAGE_KEYWORDS.any { keyword -> lowerText.contains(keyword) } ||
                lowerText.length > 100 // Generate images for longer, detailed responses
    }

    private fun extractImageDescription(text: String, agentType: String): String {
        // Enhanced image description extraction with better context understanding

        // First check for explicit image tags
        val imageTagMatch = Regex("\\[Image:([^\\]]*?)\\]").find(text)
        if (imageTagMatch != null) {
            val description = imageTagMatch.groupValues[1].trim()
            return enhanceDescriptionForAgent(description, agentType)
        }

        // Extract key phrases and context
        val keyPhrases = extractKeyPhrases(text, agentType)
        val context = keyPhrases.joinToString(", ").take(100)

        return when (agentType) {
            "cooking" -> {
                when {
                    text.contains(Regex("recipe|dish|meal|food", RegexOption.IGNORE_CASE)) ->
                        "Professional food photography of $context, appetizing, well-lit, restaurant quality presentation"
                    text.contains(Regex("ingredient|spice|herb", RegexOption.IGNORE_CASE)) ->
                        "Fresh ingredients beautifully arranged: $context, clean white background, food photography style"
                    text.contains(Regex("cooking|prepare|bake|roast|grill", RegexOption.IGNORE_CASE)) ->
                        "Cooking process showing $context, kitchen photography, step-by-step style"
                    text.contains(Regex("step|instruction", RegexOption.IGNORE_CASE)) ->
                        "Step-by-step cooking tutorial: $context, clear instructional photography"
                    else -> "Delicious food presentation: $context, professional culinary photography, appetizing"
                }
            }
            "crafting" -> {
                when {
                    text.contains(Regex("project|craft|handmade", RegexOption.IGNORE_CASE)) ->
                        "Beautiful craft project: $context, well-lit workspace, creative and inspiring"
                    text.contains(Regex("material|supply|tool", RegexOption.IGNORE_CASE)) ->
                        "Craft materials and supplies: $context, organized crafting table, colorful and neat"
                    text.contains(Regex("pattern|design|template", RegexOption.IGNORE_CASE)) ->
                        "Craft pattern or design: $context, clear detailed illustration, artistic style"
                    text.contains(Regex("tutorial|guide|step", RegexOption.IGNORE_CASE)) ->
                        "Craft tutorial showing $context, step-by-step photography, well-organized workspace"
                    else -> "Creative craft work: $context, artistic photography, inspiring and colorful"
                }
            }
            "diy" -> {
                when {
                    text.contains(Regex("repair|fix|maintenance", RegexOption.IGNORE_CASE)) ->
                        "DIY repair project: $context, workshop setting, tools visible, instructional style"
                    text.contains(Regex("build|construct|assemble", RegexOption.IGNORE_CASE)) ->
                        "Construction project: $context, clean workshop, professional tools, step-by-step guide"
                    text.contains(Regex("tool|equipment|hardware", RegexOption.IGNORE_CASE)) ->
                        "DIY tools and equipment: $context, organized workshop, professional lighting"
                    text.contains(Regex("home improvement|renovation", RegexOption.IGNORE_CASE)) ->
                        "Home improvement project: $context, before and after style, professional results"
                    else -> "DIY project guide: $context, workshop photography, practical and helpful"
                }
            }
            "buddy" -> {
                when {
                    text.contains(Regex("learn|education|study", RegexOption.IGNORE_CASE)) ->
                        "Educational concept: $context, bright classroom style, engaging and informative"
                    text.contains(Regex("activity|game|fun", RegexOption.IGNORE_CASE)) ->
                        "Fun activity: $context, colorful and engaging, friendly atmosphere"
                    text.contains(Regex("help|support|guide", RegexOption.IGNORE_CASE)) ->
                        "Helpful illustration: $context, clear and friendly, supportive style"
                    else -> "Friendly helpful concept: $context, warm and welcoming, positive atmosphere"
                }
            }
            else -> "Clear illustration: $context, professional and informative style"
        }
    }

    private fun extractKeyPhrases(text: String, agentType: String): List<String> {
        val phrases = mutableListOf<String>()

        // Extract nouns and important phrases based on agent type
        val keywords = when (agentType) {
            "cooking" -> listOf("recipe", "dish", "ingredient", "cooking", "baking", "food", "meal", "cuisine")
            "crafting" -> listOf("craft", "project", "handmade", "creative", "art", "design", "material", "pattern")
            "diy" -> listOf("build", "repair", "tool", "construction", "home", "improvement", "workshop", "fix")
            else -> listOf("help", "learn", "guide", "activity", "support", "education", "fun")
        }

        // Find sentences containing keywords
        val sentences = text.split(".", "!", "?")
        for (sentence in sentences.take(3)) {
            for (keyword in keywords) {
                if (sentence.contains(keyword, ignoreCase = true)) {
                    phrases.add(sentence.trim().take(50))
                    break
                }
            }
        }

        // If no keyword matches, use first meaningful sentence
        if (phrases.isEmpty()) {
            phrases.add(text.split(".", "!", "?").firstOrNull()?.trim()?.take(50) ?: text.take(50))
        }

        return phrases.distinct()
    }

    private fun enhanceDescriptionForAgent(description: String, agentType: String): String {
        val baseStyle = when (agentType) {
            "cooking" -> "professional food photography, appetizing, well-lit"
            "crafting" -> "creative craft photography, colorful, inspiring workspace"
            "diy" -> "clear instructional photography, workshop setting, professional tools"
            "buddy" -> "friendly illustration, bright and welcoming"
            else -> "professional photography, clear and informative"
        }

        return "$description, $baseStyle, high quality"
    }

    private fun hasReachedDailyLimit(): Boolean {
        val today = dateFormat.format(Date())
        val currentCount = prefs.getInt("images_$today", 0)
        return currentCount >= DAILY_IMAGE_LIMIT
    }

    private fun incrementDailyImageCount() {
        val today = dateFormat.format(Date())
        val currentCount = prefs.getInt("images_$today", 0)
        prefs.edit().putInt("images_$today", currentCount + 1).apply()
        Log.d(TAG, "Daily image count: ${currentCount + 1}/$DAILY_IMAGE_LIMIT")
    }

    private fun getDailyImageCount(): Int {
        val today = dateFormat.format(Date())
        return prefs.getInt("images_$today", 0)
    }

    fun getRemainingDailyImages(): Int {
        return DAILY_IMAGE_LIMIT - getDailyImageCount()
    }

    /**
     * Send a text message and get response
     * Convenience method for PersonalizedAIClient compatibility
     */
    suspend fun sendMessage(message: String, agentType: String = "buddy"): Result<EnhancedResponse> {
        return generateContent(
            prompt = message,
            agentType = agentType,
            includeHistory = true,
            forceOffline = false,
            images = emptyList()
        )
    }

    /**
     * Send a message with images and get response
     * Convenience method for PersonalizedAIClient compatibility
     */
    suspend fun sendMessageWithImages(message: String, images: List<String>, agentType: String = "buddy"): Result<EnhancedResponse> {
        return generateContent(
            prompt = message,
            agentType = agentType,
            includeHistory = true,
            forceOffline = false,
            images = images
        )
    }
    
    /**
     * Analyze image with context using XAI
     */
    suspend fun analyzeImageWithContext(
        imageUri: Uri,
        prompt: String,
        contextData: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create XAI client instance
            val xaiClient = XAIClient(GeminiConfig.GEMINI_API_KEY)
            
            val result = xaiClient.analyzeImageWithContext(
                imageUri = imageUri,
                prompt = prompt,
                contextData = contextData,
                systemPrompt = "You are an expert cooking instructor analyzing student progress. Provide detailed, encouraging feedback about their cooking technique and progress."
            )
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image with context", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate contextual response using XAI
     */
    suspend fun generateContextualResponse(
        prompt: String,
        contextData: Map<String, String> = emptyMap(),
        agentType: String = "buddy"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create XAI client instance
            val xaiClient = XAIClient(GeminiConfig.GEMINI_API_KEY)
            
            val systemPrompt = getSystemPromptForAgent(agentType)
            
            val result = xaiClient.generateContextualResponse(
                prompt = prompt,
                contextData = contextData,
                systemPrompt = systemPrompt,
                model = XAIClient.MODEL_GROK_3_MINI_FAST
            )
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating contextual response", e)
            Result.failure(e)
        }
    }
}