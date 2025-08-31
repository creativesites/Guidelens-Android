package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Enterprise-grade Gemini Video Client for real-time video understanding
 * Supports:
 * - Real-time video frame analysis
 * - Video file upload and processing
 * - Timestamped video analysis
 * - Custom frame rate sampling
 * - YouTube URL processing
 * - Live video streaming analysis
 */
class GeminiVideoClient(
    private val context: Context,
    private val apiKey: String = GeminiConfig.GEMINI_API_KEY
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://generativelanguage.googleapis.com"
    
    companion object {
        private const val TAG = "GeminiVideoClient"
        private const val MAX_FILE_SIZE_MB = 20
        private const val DEFAULT_FPS = 1
    }
    
    /**
     * Analyze a video frame with custom prompt
     */
    suspend fun analyzeVideoFrame(
        imageFile: File,
        prompt: String,
        customFps: Int = DEFAULT_FPS
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        try {
            if (!imageFile.exists() || imageFile.length() == 0L) {
                return@withContext VideoAnalysisResult.Error("Image file does not exist or is empty")
            }
            
            // Check file size
            val fileSizeMB = imageFile.length() / (1024 * 1024)
            if (fileSizeMB > MAX_FILE_SIZE_MB) {
                Log.w(TAG, "File size ($fileSizeMB MB) exceeds recommended limit ($MAX_FILE_SIZE_MB MB)")
            }
            
            // Convert image to base64
            val imageBytes = imageFile.readBytes()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("inline_data", buildJsonObject {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            add(buildJsonObject {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.4)
                    put("topK", 32)
                    put("topP", 1.0)
                    put("maxOutputTokens", 2048)
                })
                put("safetySettings", buildJsonArray {
                    add(buildJsonObject {
                        put("category", "HARM_CATEGORY_HARASSMENT")
                        put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                    })
                    add(buildJsonObject {
                        put("category", "HARM_CATEGORY_HATE_SPEECH")
                        put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                    })
                    add(buildJsonObject {
                        put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                        put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                    })
                    add(buildJsonObject {
                        put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                        put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                    })
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = suspendCoroutine<Response> { continuation ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(500)
                            .message(e.message ?: "Unknown error")
                            .body(ResponseBody.create(null, ""))
                            .build())
                    }
                    
                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }
                })
            }
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "Video analysis response: $responseBody")
                parseVideoAnalysisResponse(responseBody)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Video analysis failed: ${response.code} - $errorBody")
                VideoAnalysisResult.Error("API Error: ${response.code} - ${response.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing video frame", e)
            VideoAnalysisResult.Error("Analysis failed: ${e.message}")
        }
    }
    
    /**
     * Upload and analyze a video file using the Files API
     */
    suspend fun analyzeVideoFile(
        videoFile: File,
        prompt: String,
        customFps: Int = DEFAULT_FPS,
        startTimeSeconds: Int? = null,
        endTimeSeconds: Int? = null
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: Upload video file
            val fileUri = uploadVideoFile(videoFile)
            if (fileUri == null) {
                return@withContext VideoAnalysisResult.Error("Failed to upload video file")
            }
            
            // Step 2: Wait for processing (if needed)
            waitForFileProcessing(fileUri)
            
            // Step 3: Generate content with video
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("file_data", buildJsonObject {
                                    put("file_uri", fileUri)
                                    put("mime_type", "video/mp4")
                                })
                                // Add video metadata for custom processing
                                put("video_metadata", buildJsonObject {
                                    put("fps", customFps)
                                    startTimeSeconds?.let { put("start_offset", "${it}s") }
                                    endTimeSeconds?.let { put("end_offset", "${it}s") }
                                })
                            })
                            add(buildJsonObject {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.4)
                    put("topK", 32)
                    put("topP", 1.0)
                    put("maxOutputTokens", 4096) // Higher limit for video analysis
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                parseVideoAnalysisResponse(responseBody)
            } else {
                VideoAnalysisResult.Error("Video analysis failed: ${response.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing video file", e)
            VideoAnalysisResult.Error("Video analysis failed: ${e.message}")
        }
    }
    
    /**
     * Analyze YouTube video with timestamp
     */
    suspend fun analyzeYouTubeVideo(
        youtubeUrl: String,
        prompt: String,
        timestampSeconds: Int? = null
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val enhancedPrompt = if (timestampSeconds != null) {
                val minutes = timestampSeconds / 60
                val seconds = timestampSeconds % 60
                "$prompt At timestamp ${String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)}, please focus on that specific moment."
            } else {
                prompt
            }
            
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", enhancedPrompt)
                            })
                            add(buildJsonObject {
                                put("file_data", buildJsonObject {
                                    put("file_uri", youtubeUrl)
                                })
                            })
                        })
                    })
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                parseVideoAnalysisResponse(responseBody)
            } else {
                VideoAnalysisResult.Error("YouTube analysis failed: ${response.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing YouTube video", e)
            VideoAnalysisResult.Error("YouTube analysis failed: ${e.message}")
        }
    }
    
    /**
     * Real-time video analysis for live streaming
     */
    suspend fun analyzeVideoStream(
        frameFiles: List<File>,
        prompt: String,
        contextualPrompt: String = ""
    ): VideoAnalysisResult = withContext(Dispatchers.IO) {
        try {
            if (frameFiles.isEmpty()) {
                return@withContext VideoAnalysisResult.Error("No frames provided for analysis")
            }
            
            val combinedPrompt = buildString {
                append(prompt)
                if (contextualPrompt.isNotEmpty()) {
                    append("\n\nAdditional context: $contextualPrompt")
                }
                append("\n\nAnalyze these sequential frames as a continuous video stream.")
            }
            
            // Take the most recent frames (max 5 for performance)
            val recentFrames = frameFiles.takeLast(5)
            
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            // Add frames
                            recentFrames.forEach { frame ->
                                add(buildJsonObject {
                                    put("inline_data", buildJsonObject {
                                        put("mime_type", "image/jpeg")
                                        put("data", Base64.encodeToString(frame.readBytes(), Base64.NO_WRAP))
                                    })
                                })
                            }
                            // Add prompt
                            add(buildJsonObject {
                                put("text", combinedPrompt)
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("temperature", 0.3) // Lower temperature for more consistent streaming analysis
                    put("maxOutputTokens", 1024) // Shorter responses for real-time
                })
            }
            
            val request = Request.Builder()
                .url("$baseUrl/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                parseVideoAnalysisResponse(responseBody)
            } else {
                VideoAnalysisResult.Error("Stream analysis failed: ${response.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing video stream", e)
            VideoAnalysisResult.Error("Stream analysis failed: ${e.message}")
        }
    }
    
    private suspend fun uploadVideoFile(file: File): String? = withContext(Dispatchers.IO) {
        try {
            // First, initiate the upload
            val initRequest = Request.Builder()
                .url("$baseUrl/upload/v1beta/files?key=$apiKey")
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", file.length().toString())
                .header("X-Goog-Upload-Header-Content-Type", "video/mp4")
                .header("Content-Type", "application/json")
                .post(buildJsonObject {
                    put("file", buildJsonObject {
                        put("display_name", file.name)
                    })
                }.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val initResponse = client.newCall(initRequest).execute()
            val uploadUrl = initResponse.header("X-Goog-Upload-URL")
            
            if (uploadUrl == null) {
                Log.e(TAG, "Failed to get upload URL")
                return@withContext null
            }
            
            // Upload the file
            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .header("Content-Length", file.length().toString())
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .post(file.asRequestBody("video/mp4".toMediaTypeOrNull()))
                .build()
            
            val uploadResponse = client.newCall(uploadRequest).execute()
            
            if (uploadResponse.isSuccessful) {
                val responseBody = uploadResponse.body?.string() ?: ""
                val response = json.decodeFromString<FileUploadResponse>(responseBody)
                response.file?.uri
            } else {
                Log.e(TAG, "File upload failed: ${uploadResponse.message}")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading video file", e)
            null
        }
    }
    
    private suspend fun waitForFileProcessing(fileUri: String) = withContext(Dispatchers.IO) {
        // In a real implementation, you might want to poll the file status
        // For now, we'll just wait a bit to ensure processing is complete
        kotlinx.coroutines.delay(2000)
    }
    
    private fun parseVideoAnalysisResponse(responseBody: String): VideoAnalysisResult {
        return try {
            val jsonResponse = json.parseToJsonElement(responseBody) as JsonObject
            val candidates = jsonResponse["candidates"]?.let { candidates ->
                json.decodeFromJsonElement<List<Candidate>>(candidates)
            } ?: emptyList()
            
            if (candidates.isNotEmpty()) {
                val content = candidates[0].content
                val text = content?.parts?.firstOrNull { it.text != null }?.text ?: "No analysis available"
                
                // Extract confidence and detected objects (mock implementation)
                val confidence = extractConfidence(text)
                val detectedObjects = extractDetectedObjects(text)
                
                VideoAnalysisResult.Success(
                    analysis = text,
                    confidence = confidence,
                    detectedObjects = detectedObjects,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                VideoAnalysisResult.Error("No analysis candidates returned")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            VideoAnalysisResult.Error("Failed to parse response: ${e.message}")
        }
    }
    
    private fun extractConfidence(text: String): Float {
        // Mock confidence extraction - in a real implementation, 
        // this might be based on response metadata or content analysis
        return when {
            text.contains("clearly", ignoreCase = true) || text.contains("definitely", ignoreCase = true) -> 0.9f
            text.contains("appears", ignoreCase = true) || text.contains("seems", ignoreCase = true) -> 0.7f
            text.contains("might", ignoreCase = true) || text.contains("possibly", ignoreCase = true) -> 0.5f
            else -> 0.8f
        }
    }
    
    private fun extractDetectedObjects(text: String): List<String> {
        // Mock object detection - in a real implementation,
        // this might use NLP or structured response parsing
        val commonObjects = listOf("person", "hand", "tool", "ingredient", "bowl", "knife", "pan", "food")
        return commonObjects.filter { obj ->
            text.contains(obj, ignoreCase = true)
        }.take(4)
    }
}

// Data models
@Serializable
sealed class VideoAnalysisResult {
    @Serializable
    data class Success(
        val analysis: String,
        val confidence: Float,
        val detectedObjects: List<String> = emptyList(),
        val timestamp: Long
    ) : VideoAnalysisResult()
    
    @Serializable
    data class Error(val message: String) : VideoAnalysisResult()
}

@Serializable
data class FileUploadResponse(
    val file: FileInfo? = null
)

@Serializable
data class FileInfo(
    val name: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: String,
    val createTime: String,
    val updateTime: String,
    val expirationTime: String,
    val sha256Hash: String,
    val uri: String,
    val state: String,
    val error: ErrorInfo? = null
)

@Serializable
data class ErrorInfo(
    val code: Int,
    val message: String,
    val status: String
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class Content(
    val parts: List<Part>? = null,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)