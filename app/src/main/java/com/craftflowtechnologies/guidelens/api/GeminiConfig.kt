package com.craftflowtechnologies.guidelens.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object GeminiConfig {
    // Gemini API Configuration
    const val GEMINI_API_KEY = "AIzaSyBB5ZYwktOFI3R3j_vs8U7CxwKgS3XNgM0" // Replace with actual API key
    const val GEMINI_LIVE_ENDPOINT = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    
    // Model configurations based on CLAUDE.md
    const val FREE_MODEL = "gemini-1.5-flash"
    const val STANDARD_MODEL = "gemini-1.5-flash"
    const val VOICE_MODEL = "gemini-1.5-flash"
    const val LIVE_MODEL = "gemini-1.5-flash"

    // Model selection
    const val DEFAULT_MODEL = "gemini-1.5-pro-latest"
    const val FALLBACK_MODEL = "gemini-1.5-flash"

    // API endpoints
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    // Rate limits and session configs
    object RateLimits {
        const val FREE_TIER_RPM = 15
        const val FREE_TIER_TPM = 250000
        const val FREE_TIER_RPD = 1000
        
        const val TIER_1_RPM = 1000
        const val TIER_1_TPM = 1000000
        const val TIER_1_RPD = 10000
    }
    
    object SessionLimits {
        const val FREE_MAX_DURATION = 1800 // 30 minutes
        const val FREE_COST_CAP = 0.05 // USD
        const val FREE_TOKEN_LIMIT = 100000
        
        const val BASIC_MAX_DURATION = 3600 // 60 minutes
        const val BASIC_COST_CAP = 0.10 // USD
        const val BASIC_TOKEN_LIMIT = 200000
        
        const val PRO_MAX_DURATION = -1 // Unlimited
        const val PRO_COST_CAP = 2.00 // USD
        const val PRO_TOKEN_LIMIT = 500000
    }
}

@Serializable
data class GeminiSessionContext(
    val userTier: String = "pro", // free, basic, pro
    val mode: String = "text", // text, voice, video
    val sessionType: String = "buddy", // cooking, crafting, diy, buddy
    val complexity: String = "simple", // simple, moderate, complex
    val budgetRemaining: Double = 1.0
)

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val safetySettings: List<GeminiSafetySetting>? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    @SerialName("role") val role: String // Explicitly name the field "role"
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String // Base64 encoded
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double = 0.7,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 1024,
    val stopSequences: List<String>? = null
)

@Serializable
data class GeminiSafetySetting(
    val category: String,
    val threshold: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>,
    val promptFeedback: GeminiPromptFeedback? = null
)
data class EnhancedResponse(
    val text: String,
    val image: String? = null,
    val confidence: Float? = 1.0f
)
@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String? = null,
    val index: Int = 0,
    val safetyRatings: List<GeminiSafetyRating>? = null
)

@Serializable
data class GeminiPromptFeedback(
    val safetyRatings: List<GeminiSafetyRating>? = null,
    val blockReason: String? = null
)

@Serializable
data class GeminiSafetyRating(
    val category: String,
    val probability: String
)

@kotlinx.serialization.Serializable
data class GeminiErrorResponse(
    val error: GeminiErrorDetails
)

@kotlinx.serialization.Serializable
data class GeminiErrorDetails(
    val code: Int,
    val message: String,
    val status: String
)

// Live API message structures
@Serializable
data class LiveSetupMessage(
    val setup: SetupData
)

@Serializable
data class SetupData(
    val model: String,
    val generationConfig: GeminiGenerationConfig,
    val systemInstruction: SystemInstruction
)

@Serializable
data class SystemInstruction(
    val parts: List<InstructionPart>
)

@Serializable
data class InstructionPart(
    val text: String
)

@Serializable
data class LiveClientContentMessage(
    val clientContent: ClientContent
)

@Serializable
data class ClientContent(
    val turns: List<Turn>,
    val turnComplete: Boolean
)

@Serializable
data class Turn(
    val role: String,
    val parts: List<TurnPart>
)

@Serializable
data class TurnPart(
    val text: String
)

@Serializable
data class LiveRealtimeInputMessage(
    val realtimeInput: RealtimeInput
)

@Serializable
data class RealtimeInput(
    val mediaChunks: List<MediaChunk>
)

@Serializable
data class MediaChunk(
    val mimeType: String,
    val data: String
)