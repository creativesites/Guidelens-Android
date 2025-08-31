package com.craftflowtechnologies.guidelens.api

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GeminiTextClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    // Store conversation history for context
    private val conversationHistory = mutableListOf<GeminiTextContent>()

    suspend fun generateContent(
        prompt: String,
        agentType: String = "buddy",
        includeHistory: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = getSystemPromptForAgent(agentType)

            // Build conversation with proper structure
            val contents = mutableListOf<GeminiTextContent>()

            // Add system instructions as the first message
            contents.add(
                GeminiTextContent(
                    parts = listOf(GeminiTextPart(text = systemPrompt)),
                    role = "user"
                )
            )

            // Add conversation history if requested and available
            if (includeHistory && conversationHistory.isNotEmpty()) {
                contents.addAll(conversationHistory.takeLast(10)) // Keep last 10 exchanges
            }

            // Add current user message
            contents.add(
                GeminiTextContent(
                    parts = listOf(GeminiTextPart(text = prompt)),
                    role = "user"
                )
            )

            val request = GeminiTextRequest(
                contents = contents,
                generationConfig = GeminiTextGenerationConfig(
                    temperature = 0.8, // Increased for more creativity
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 2048, // Increased for longer responses
                    candidateCount = 1,
                    stopSequences = emptyList()
                ),
                safetySettings = listOf(
                    GeminiTextSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH"),
                    GeminiTextSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH"),
                    GeminiTextSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH"),
                    GeminiTextSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH")
                )
            )

            val requestBody = json.encodeToString(request)
                .toRequestBody("application/json".toMediaType())

            // Use correct model name
            val httpRequest = Request.Builder()
                .url("$baseUrl/gemini-1.5-flash:generateContent?key=${GeminiConfig.GEMINI_API_KEY}")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            Log.d("GeminiText", "Sending request to Gemini API")
            Log.d("GeminiText", "Request contents count: ${contents.size}")

            val response = client.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.d("GeminiText", "Received response length: ${responseBody.length}")

                    try {
                        val geminiResponse = json.decodeFromString<GeminiTextResponse>(responseBody)

                        val candidate = geminiResponse.candidates.firstOrNull()
                        val text = candidate?.content?.parts?.firstOrNull()?.text

                        if (text != null && text.isNotBlank()) {
//                            // sava history
                            if (includeHistory) {
                                conversationHistory.add(
                                    GeminiTextContent(
                                        parts = listOf(GeminiTextPart(text = prompt)),
                                        role = "user"
                                    )
                                )
                                conversationHistory.add(
                                    GeminiTextContent(
                                        parts = listOf(GeminiTextPart(text = text)),
                                        role = "model"
                                    )
                                )
                            }
                            Result.success(text.trim())
                        } else {
                            Log.e("GeminiText", "Empty or null text in response")
                            Log.e("GeminiText", "Full response: $responseBody")

                            // Check for safety blocks or other issues
                            candidate?.safetyRatings?.let { ratings ->
                                Log.w("GeminiText", "Safety ratings: $ratings")
                            }

                            Result.failure(Exception("No text content in response. Response may have been blocked."))
                        }
                    } catch (e: Exception) {
                        Log.e("GeminiText", "Failed to parse response: $responseBody", e)
                        Result.failure(Exception("Failed to parse API response: ${e.message}"))
                    }
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("GeminiText", "API Error: ${response.code} - $errorBody")
                Log.e("GeminiText", "Response headers: ${response.headers}")

                when (response.code) {
                    400 -> Result.failure(Exception("Bad request - check your prompt format"))
                    401 -> Result.failure(Exception("Invalid API key"))
                    403 -> Result.failure(Exception("API access forbidden - check your key permissions"))
                    429 -> Result.failure(Exception("Rate limit exceeded - try again later"))
                    500 -> Result.failure(Exception("Gemini server error - try again"))
                    else -> Result.failure(Exception("API Error ${response.code}: $errorBody"))
                }
            }
        } catch (e: IOException) {
            Log.e("GeminiText", "Network error", e)
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("GeminiText", "Unexpected error", e)
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
        Log.d("GeminiText", "Conversation history cleared")
    }

    fun getHistorySize(): Int = conversationHistory.size

    private fun getSystemPromptForAgent(agentType: String): String {
        val basePrompt = when (agentType) {
            "cooking" -> """You are an expert Cooking Assistant in the GuideLens app. You're passionate about helping people become better cooks through personalized guidance and encouragement.

Your expertise includes:
- Recipe guidance and modifications
- Cooking techniques and timing
- Food safety and kitchen safety
- Ingredient substitutions and alternatives
- Troubleshooting cooking problems
- Equipment recommendations

Your communication style:
- Be encouraging and supportive
- Give specific, actionable advice
- Always prioritize food safety
- Use clear, step-by-step instructions
- Share helpful tips and tricks
- Ask clarifying questions when needed

Remember: Every interaction should help the user feel more confident in the kitchen while ensuring they cook safely and successfully."""

            "crafting" -> """You are an experienced Crafting Guru in the GuideLens app. You're enthusiastic about helping people explore their creativity through various craft projects.

Your expertise includes:
- Project planning and design
- Technique instruction and improvement
- Tool selection and proper usage
- Material recommendations
- Problem-solving and troubleshooting
- Finishing techniques

Your communication style:
- Be patient and encouraging
- Celebrate creativity and personal expression
- Provide clear, detailed instructions
- Offer alternatives for different skill levels
- Share inspirational ideas
- Help troubleshoot when things go wrong

Remember: Focus on helping users develop their skills while enjoying the creative process. Every project is a learning opportunity."""

            "diy" -> """You are a knowledgeable DIY Helper in the GuideLens app. You're dedicated to helping people tackle home improvement and repair projects safely and successfully.

Your expertise includes:
- Home repair and maintenance
- Tool selection and proper usage
- Safety protocols and protective equipment
- Project planning and preparation
- Building codes and best practices
- Troubleshooting common problems

Your communication style:
- Always prioritize safety first
- Be methodical and detail-oriented
- Provide step-by-step guidance
- Emphasize proper preparation
- Recommend when to call professionals
- Use clear safety warnings

Remember: Safety is paramount. Help users understand not just how to do something, but how to do it safely and correctly."""

            "buddy" -> """You are Buddy, the friendly and versatile assistant in the GuideLens app. You're here to help users learn and grow in any skill or area they're interested in.

Your personality:
- Enthusiastic and supportive
- Adaptable to any topic
- Patient with beginners
- Encouraging during challenges
- Celebrates progress and achievements
- Genuinely interested in helping people learn

Your approach:
- Ask thoughtful questions to understand their needs
- Break down complex topics into manageable steps
- Provide encouragement and motivation
- Offer multiple approaches when possible
- Connect learning to real-world applications
- Make learning enjoyable and engaging

Remember: You're not just providing information - you're being a supportive companion on their learning journey."""

            else -> """You are a helpful AI assistant in the GuideLens app. Provide clear, accurate, and supportive guidance on any topic. Be encouraging, specific, and always prioritize safety when relevant."""
        }
        return """
        $basePrompt
        
        Response Format:
        - Use markdown for structured responses:
          - **Bold** for headings and emphasis
          - *Italics* for subtle emphasis
          - - Bullet points for lists
          - 1. Numbered lists for steps
          - ```code``` for code snippets
          - > Blockquotes for important notes or tips
          - Use emojis for engagement (e.g., ✅, 🍳, 🎨)
        - Include interactive elements when relevant:
          - Suggest clickable actions using [Action: Text]
          - Provide quick tips in a [Tip: Text] format
          - Include thinking steps in [Thinking: Steps] when complex reasoning is involved
        - Keep responses concise yet detailed, with a friendly and engaging tone.
        - Always end with a question or call-to-action to keep the conversation going.
    """.trimIndent()
    }
}

// Updated data classes with proper structure
@kotlinx.serialization.Serializable
data class GeminiTextRequest(
    val contents: List<GeminiTextContent>,
    val generationConfig: GeminiTextGenerationConfig,
    val safetySettings: List<GeminiTextSafetySetting>
)

@kotlinx.serialization.Serializable
data class GeminiTextContent(
    val parts: List<GeminiTextPart>,
    val role: String? = null
)

@kotlinx.serialization.Serializable
data class GeminiTextPart(
    val text: String
)

@kotlinx.serialization.Serializable
data class GeminiTextGenerationConfig(
    val temperature: Double,
    val topK: Int,
    val topP: Double,
    val maxOutputTokens: Int,
    val candidateCount: Int? = null,
    val stopSequences: List<String>? = null
)

@kotlinx.serialization.Serializable
data class GeminiTextSafetySetting(
    val category: String,
    val threshold: String
)

@kotlinx.serialization.Serializable
data class GeminiTextResponse(
    val candidates: List<GeminiTextCandidate>,
    val promptFeedback: GeminiTextPromptFeedback? = null
)

@kotlinx.serialization.Serializable
data class GeminiTextCandidate(
    val content: GeminiTextContent,
    val finishReason: String? = null,
    val safetyRatings: List<GeminiTextSafetyRating>? = null
)

@kotlinx.serialization.Serializable
data class GeminiTextSafetyRating(
    val category: String,
    val probability: String
)

@kotlinx.serialization.Serializable
data class GeminiTextPromptFeedback(
    val safetyRatings: List<GeminiTextSafetyRating>? = null,
    val blockReason: String? = null
)
