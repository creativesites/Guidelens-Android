package com.craftflowtechnologies.guidelens.api

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import android.util.Base64
import java.io.InputStream

@Serializable
data class XAIMessage(
    val role: String, // "system", "user", "assistant"
    val content: XAIContent,
    val reasoning_content: String? = null
)

@Serializable
sealed class XAIContent {
    @Serializable
    data class TextContent(val text: String) : XAIContent()
    
    @Serializable
    data class MultipartContent(val parts: List<ContentPart>) : XAIContent()
}

@Serializable
data class ContentPart(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String,
    val detail: String = "high" // "low", "high", "auto"
)

@Serializable
data class XAIRequest(
    val model: String,
    val messages: List<XAIMessage>,
    val temperature: Float = 0.7f,
    val max_completion_tokens: Int? = null,
    val reasoning_effort: String? = null, // "low" or "high", not supported by grok-4
    val stream: Boolean = false,
    val response_format: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(
    val type: String // "text", "json_object", "json_schema"
)

@Serializable
data class XAIResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val system_fingerprint: String? = null
)
@Serializable
data class LogProbs(
    val tokens: List<String>? = null,
    val probabilities: List<Double>? = null
    // Add fields as per actual data
)
@Serializable
data class Choice(
    val index: Int,
    val message: XAIMessage,
    val finish_reason: String,
    val logprobs: LogProbs? = null
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
    val reasoning_tokens: Int? = null
)

class XAIClient(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "XAIClient"
        private const val BASE_URL = "https://api.x.ai/v1"
        private const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
        
        // Model constants
        const val MODEL_GROK_4 = "grok-4"
        const val MODEL_GROK_3_MINI = "grok-3-mini"
        const val MODEL_GROK_3_MINI_FAST = "grok-3-mini-fast"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // Longer timeout for reasoning models
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Generate contextual response using XAI chat models
     */
    suspend fun generateContextualResponse(
        prompt: String,
        contextData: Map<String, String> = emptyMap(),
        model: String = MODEL_GROK_3_MINI,
        systemPrompt: String? = null,
        conversationHistory: List<Pair<String, String>> = emptyList(), // role to content pairs
        reasoningEffort: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            val messages = buildMessages(
                systemPrompt = systemPrompt,
                conversationHistory = conversationHistory,
                currentPrompt = prompt,
                contextData = contextData
            )
            
            val request = XAIRequest(
                model = model,
                messages = messages,
                temperature = 0.7f,
                reasoning_effort = if (model != MODEL_GROK_4) reasoningEffort else null,
                max_completion_tokens = 2000
            )
            
            val response = makeRequest(request)
            
            if (response.isSuccess) {
                val xaiResponse = response.getOrNull()
                val content = xaiResponse?.choices?.firstOrNull()?.message?.content
                
                when (content) {
                    is XAIContent.TextContent -> Result.success(content.text)
                    is XAIContent.MultipartContent -> {
                        val textParts = content.parts.filter { it.type == "text" }
                        val combinedText = textParts.joinToString("\n") { it.text ?: "" }
                        Result.success(combinedText)
                    }
                    null -> Result.failure(Exception("No content in response"))
                }
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating contextual response", e)
            Result.failure(e)
        }
    }
    
    /**
     * Analyze image with context using XAI vision capabilities
     */
    suspend fun analyzeImageWithContext(
        imageUri: Uri,
        prompt: String,
        contextData: Map<String, String> = emptyMap(),
        model: String = MODEL_GROK_4,
        systemPrompt: String? = null,
        imageDetail: String = "high"
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            // Convert image to base64
            val base64Image = encodeImageToBase64(imageUri)
            if (base64Image.isFailure) {
                return@withContext Result.failure(
                    base64Image.exceptionOrNull() ?: Exception("Failed to encode image")
                )
            }
            
            val messages = buildImageAnalysisMessages(
                systemPrompt = systemPrompt,
                prompt = prompt,
                base64Image = base64Image.getOrNull()!!,
                contextData = contextData,
                imageDetail = imageDetail
            )
            
            val request = XAIRequest(
                model = model,
                messages = messages,
                temperature = 0.3f, // Lower temperature for more consistent analysis
                max_completion_tokens = 1500
            )
            
            val response = makeRequest(request)
            
            if (response.isSuccess) {
                val xaiResponse = response.getOrNull()
                val content = xaiResponse?.choices?.firstOrNull()?.message?.content
                
                when (content) {
                    is XAIContent.TextContent -> Result.success(content.text)
                    is XAIContent.MultipartContent -> {
                        val textParts = content.parts.filter { it.type == "text" }
                        val combinedText = textParts.joinToString("\n") { it.text ?: "" }
                        Result.success(combinedText)
                    }
                    null -> Result.failure(Exception("No content in response"))
                }
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image with context", e)
            Result.failure(e)
        }
    }
    
    /**
     * Analyze multiple images with context
     */
    suspend fun analyzeMultipleImagesWithContext(
        imageUris: List<Uri>,
        prompt: String,
        contextData: Map<String, String> = emptyMap(),
        model: String = MODEL_GROK_4,
        systemPrompt: String? = null,
        imageDetail: String = "high"
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            // Convert all images to base64
            val base64Images = mutableListOf<String>()
            imageUris.forEach { uri ->
                val base64Result = encodeImageToBase64(uri)
                if (base64Result.isSuccess) {
                    base64Images.add(base64Result.getOrNull()!!)
                } else {
                    Log.w(TAG, "Failed to encode image: $uri")
                }
            }
            
            if (base64Images.isEmpty()) {
                return@withContext Result.failure(Exception("No images could be processed"))
            }
            
            val messages = buildMultiImageAnalysisMessages(
                systemPrompt = systemPrompt,
                prompt = prompt,
                base64Images = base64Images,
                contextData = contextData,
                imageDetail = imageDetail
            )
            
            val request = XAIRequest(
                model = model,
                messages = messages,
                temperature = 0.3f,
                max_completion_tokens = 2000
            )
            
            val response = makeRequest(request)
            
            if (response.isSuccess) {
                val xaiResponse = response.getOrNull()
                val content = xaiResponse?.choices?.firstOrNull()?.message?.content
                
                when (content) {
                    is XAIContent.TextContent -> Result.success(content.text)
                    is XAIContent.MultipartContent -> {
                        val textParts = content.parts.filter { it.type == "text" }
                        val combinedText = textParts.joinToString("\n") { it.text ?: "" }
                        Result.success(combinedText)
                    }
                    null -> Result.failure(Exception("No content in response"))
                }
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing multiple images", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate structured JSON response
     */
    suspend fun generateStructuredResponse(
        prompt: String,
        jsonSchema: String? = null,
        contextData: Map<String, String> = emptyMap(),
        model: String = MODEL_GROK_3_MINI,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        
        try {
            val messages = buildMessages(
                systemPrompt = systemPrompt,
                conversationHistory = emptyList(),
                currentPrompt = prompt,
                contextData = contextData
            )
            
            val responseFormat = if (jsonSchema != null) {
                ResponseFormat(type = "json_schema")
            } else {
                ResponseFormat(type = "json_object")
            }
            
            val request = XAIRequest(
                model = model,
                messages = messages,
                temperature = 0.3f,
                response_format = responseFormat,
                max_completion_tokens = 1500
            )
            
            val response = makeRequest(request)
            
            if (response.isSuccess) {
                val xaiResponse = response.getOrNull()
                val content = xaiResponse?.choices?.firstOrNull()?.message?.content
                
                when (content) {
                    is XAIContent.TextContent -> Result.success(content.text)
                    is XAIContent.MultipartContent -> {
                        val textParts = content.parts.filter { it.type == "text" }
                        val combinedText = textParts.joinToString("\n") { it.text ?: "" }
                        Result.success(combinedText)
                    }
                    null -> Result.failure(Exception("No content in response"))
                }
            } else {
                Result.failure(response.exceptionOrNull() ?: Exception("Unknown error"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating structured response", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private suspend fun makeRequest(request: XAIRequest): Result<XAIResponse> {
        return try {
            val requestBody = json.encodeToString(XAIRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())
            
            val httpRequest = Request.Builder()
                .url(CHAT_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            val response = httpClient.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val xaiResponse = json.decodeFromString(XAIResponse.serializer(), responseBody)
                Result.success(xaiResponse)
            } else {
                val errorMessage = "XAI API error: ${response.code} - ${responseBody ?: "Unknown error"}"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Network error calling XAI API", e)
            Result.failure(e)
        }
    }
    
    private fun buildMessages(
        systemPrompt: String?,
        conversationHistory: List<Pair<String, String>>,
        currentPrompt: String,
        contextData: Map<String, String>
    ): List<XAIMessage> {
        val messages = mutableListOf<XAIMessage>()
        
        // Add system message if provided
        if (systemPrompt != null) {
            val enhancedSystemPrompt = if (contextData.isNotEmpty()) {
                "$systemPrompt\n\nContext Information:\n${contextData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}"
            } else {
                systemPrompt
            }
            
            messages.add(
                XAIMessage(
                    role = "system",
                    content = XAIContent.TextContent(enhancedSystemPrompt)
                )
            )
        }
        
        // Add conversation history
        conversationHistory.forEach { (role, content) ->
            messages.add(
                XAIMessage(
                    role = role,
                    content = XAIContent.TextContent(content)
                )
            )
        }
        
        // Add current prompt
        messages.add(
            XAIMessage(
                role = "user",
                content = XAIContent.TextContent(currentPrompt)
            )
        )
        
        return messages
    }
    
    private fun buildImageAnalysisMessages(
        systemPrompt: String?,
        prompt: String,
        base64Image: String,
        contextData: Map<String, String>,
        imageDetail: String
    ): List<XAIMessage> {
        val messages = mutableListOf<XAIMessage>()
        
        // Add system message if provided
        if (systemPrompt != null) {
            val enhancedSystemPrompt = if (contextData.isNotEmpty()) {
                "$systemPrompt\n\nContext Information:\n${contextData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}"
            } else {
                systemPrompt
            }
            
            messages.add(
                XAIMessage(
                    role = "system",
                    content = XAIContent.TextContent(enhancedSystemPrompt)
                )
            )
        }
        
        // Add user message with image and text
        val contentParts = listOf(
            ContentPart(
                type = "image_url",
                image_url = ImageUrl(
                    url = "data:image/jpeg;base64,$base64Image",
                    detail = imageDetail
                )
            ),
            ContentPart(
                type = "text",
                text = prompt
            )
        )
        
        messages.add(
            XAIMessage(
                role = "user",
                content = XAIContent.MultipartContent(contentParts)
            )
        )
        
        return messages
    }
    
    private fun buildMultiImageAnalysisMessages(
        systemPrompt: String?,
        prompt: String,
        base64Images: List<String>,
        contextData: Map<String, String>,
        imageDetail: String
    ): List<XAIMessage> {
        val messages = mutableListOf<XAIMessage>()
        
        // Add system message if provided
        if (systemPrompt != null) {
            val enhancedSystemPrompt = if (contextData.isNotEmpty()) {
                "$systemPrompt\n\nContext Information:\n${contextData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}"
            } else {
                systemPrompt
            }
            
            messages.add(
                XAIMessage(
                    role = "system",
                    content = XAIContent.TextContent(enhancedSystemPrompt)
                )
            )
        }
        
        // Build content parts with all images and text
        val contentParts = mutableListOf<ContentPart>()
        
        // Add all images
        base64Images.forEach { base64Image ->
            contentParts.add(
                ContentPart(
                    type = "image_url",
                    image_url = ImageUrl(
                        url = "data:image/jpeg;base64,$base64Image",
                        detail = imageDetail
                    )
                )
            )
        }
        
        // Add text prompt
        contentParts.add(
            ContentPart(
                type = "text",
                text = prompt
            )
        )
        
        messages.add(
            XAIMessage(
                role = "user",
                content = XAIContent.MultipartContent(contentParts)
            )
        )
        
        return messages
    }
    
    private suspend fun encodeImageToBase64(imageUri: Uri): Result<String> {
        return try {
            // This is a simplified implementation
            // In a real app, you'd need to handle different URI schemes and get the actual image data
            // For now, we'll assume we can read from the URI as a file
            
            val inputStream: InputStream = when {
                imageUri.scheme == "content" -> {
                    // Handle content:// URIs (camera, gallery, etc.)
                    // This would require a ContentResolver
                    throw NotImplementedError("Content URI handling not implemented in this example")
                }
                imageUri.scheme == "file" -> {
                    // Handle file:// URIs
                    java.io.FileInputStream(imageUri.path!!)
                }
                imageUri.scheme?.startsWith("http") == true -> {
                    // Handle network URLs
                    throw NotImplementedError("Network URI handling not implemented in this example")
                }
                else -> {
                    throw IllegalArgumentException("Unsupported URI scheme: ${imageUri.scheme}")
                }
            }
            
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success(base64String)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to base64", e)
            Result.failure(e)
        }
    }
}