package com.craftflowtechnologies.guidelens.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.craftflowtechnologies.guidelens.api.GeminiConfig
import com.craftflowtechnologies.guidelens.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Gemini Image Generation Client using Gemini 2.5 Flash Image (Nano Banana)
 */
class GeminiImageGenerator(
    private val context: Context
) {
    companion object {
        private const val TAG = "GeminiImageGenerator"
        private const val GEMINI_IMAGE_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image-preview:generateContent"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 // 1MB
        private const val IMAGE_QUALITY = 85
        private val json = Json { ignoreUnknownKeys = true }
    }
    
    private val httpClient = OkHttpClient()
    
    /**
     * Generate image from text prompt only
     */
    suspend fun generateImage(
        prompt: String,
        agentType: String = "buddy",
        artifactId: String? = null,
        stepIndex: Int? = null
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        
        try {
            val enhancedPrompt = enhancePromptForAgent(prompt, agentType)
            
            val requestBody = buildTextToImageRequest(enhancedPrompt)
            val request = buildHttpRequest(requestBody)
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val imageData = extractImageFromResponse(responseBody)
                
                if (imageData != null) {
                    val savedImage = saveGeneratedImage(imageData, artifactId, stepIndex, agentType)
                    ImageGenerationResult(
                        success = true,
                        image = savedImage
                    )
                } else {
                    ImageGenerationResult(
                        success = false,
                        error = "Failed to extract image from response"
                    )
                }
            } else {
                Log.e(TAG, "Image generation failed: ${response.code} - ${response.message}")
                ImageGenerationResult(
                    success = false,
                    error = "API request failed: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating image", e)
            ImageGenerationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Generate image with input image for editing/enhancement
     */
    suspend fun generateImageWithInput(
        prompt: String,
        inputImageUri: Uri,
        agentType: String = "buddy",
        artifactId: String? = null,
        stepIndex: Int? = null
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        
        try {
            val enhancedPrompt = enhancePromptForAgent(prompt, agentType)
            val inputImageBase64 = encodeImageToBase64(inputImageUri)
            
            if (inputImageBase64 == null) {
                return@withContext ImageGenerationResult(
                    success = false,
                    error = "Failed to encode input image"
                )
            }
            
            val requestBody = buildImageEditRequest(enhancedPrompt, inputImageBase64)
            val request = buildHttpRequest(requestBody)
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val imageData = extractImageFromResponse(responseBody)
                
                if (imageData != null) {
                    val savedImage = saveGeneratedImage(imageData, artifactId, stepIndex, agentType)
                    ImageGenerationResult(
                        success = true,
                        image = savedImage
                    )
                } else {
                    ImageGenerationResult(
                        success = false,
                        error = "Failed to extract image from response"
                    )
                }
            } else {
                Log.e(TAG, "Image editing failed: ${response.code} - ${response.message}")
                ImageGenerationResult(
                    success = false,
                    error = "API request failed: ${response.code}"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error editing image", e)
            ImageGenerationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    /**
     * Generate multiple step images for an artifact
     */
    suspend fun generateStepImages(
        artifact: Artifact,
        maxImages: Int = 4
    ): List<StageImage> = withContext(Dispatchers.IO) {
        
        val stageImages = mutableListOf<StageImage>()
        
        try {
            val stepsWithImages = identifyStepsForImages(artifact, maxImages)
            
            stepsWithImages.forEachIndexed { index, stepInfo ->
                val prompt = buildStepImagePrompt(artifact, stepInfo.stepIndex, stepInfo.description)
                val result = generateImage(
                    prompt = prompt,
                    agentType = artifact.agentType,
                    artifactId = artifact.id,
                    stepIndex = stepInfo.stepIndex
                )
                
                if (result.success && result.image != null) {
                    stageImages.add(
                        StageImage(
                            stageNumber = stepInfo.stepIndex,
                            stepId = stepInfo.stepId,
                            image = result.image,
                            description = stepInfo.description,
                            isKeyMilestone = stepInfo.isKeyMilestone
                        )
                    )
                }
                
                // Add delay between requests to avoid rate limiting
                if (index < stepsWithImages.size - 1) {
                    kotlinx.coroutines.delay(2000)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating step images", e)
        }
        
        stageImages
    }
    
    // Private helper methods
    private fun enhancePromptForAgent(prompt: String, agentType: String): String {
        val agentStyle = when (agentType.lowercase()) {
            "cooking" -> "high-quality food photography, professional kitchen lighting, appetizing, clean presentation"
            "crafting" -> "craft project photography, good lighting, clear details, creative and inspiring"
            "diy" -> "clear instructional photography, good lighting, safety-focused, step-by-step visual guide"
            "buddy" -> "educational illustration, clear and friendly, informative visual"
            else -> "clear, professional, well-lit photography"
        }
        
        return "Create a $agentStyle image: $prompt. The image should be clear, well-composed, and helpful for learning."
    }
    
    private fun buildTextToImageRequest(prompt: String): String {
        val request = GeminiImageRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart.TextPart(prompt)
                    )
                )
            )
        )
        return json.encodeToString(GeminiImageRequest.serializer(), request)
    }
    
    private fun buildImageEditRequest(prompt: String, imageBase64: String): String {
        val request = GeminiImageRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart.TextPart(prompt),
                        GeminiPart.ImagePart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = imageBase64
                            )
                        )
                    )
                )
            )
        )
        return json.encodeToString(GeminiImageRequest.serializer(), request)
    }
    
    private fun buildHttpRequest(requestBody: String): Request {
        return Request.Builder()
            .url(GEMINI_IMAGE_API_URL)
            .addHeader("x-goog-api-key", GeminiConfig.GEMINI_API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    private fun extractImageFromResponse(responseBody: String?): ByteArray? {
        return try {
            if (responseBody == null) return null
            
            // Extract base64 image data using regex (similar to curl example)
            val dataRegex = """"data":\s*"([^"]+)"""".toRegex()
            val matchResult = dataRegex.find(responseBody)
            
            matchResult?.let { match ->
                val base64Data = match.groupValues[1]
                Base64.decode(base64Data, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting image from response", e)
            null
        }
    }
    
    private fun encodeImageToBase64(imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, outputStream)
                val imageBytes = outputStream.toByteArray()
                
                // Compress if too large
                val finalBytes = if (imageBytes.size > MAX_IMAGE_SIZE) {
                    compressImage(bitmap, MAX_IMAGE_SIZE)
                } else {
                    imageBytes
                }
                
                Base64.encodeToString(finalBytes, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding image to base64", e)
            null
        }
    }
    
    private fun compressImage(bitmap: Bitmap, maxSize: Int): ByteArray {
        var quality = IMAGE_QUALITY
        var imageBytes: ByteArray
        
        do {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            imageBytes = outputStream.toByteArray()
            quality -= 10
        } while (imageBytes.size > maxSize && quality > 10)
        
        return imageBytes
    }
    
    private suspend fun saveGeneratedImage(
        imageData: ByteArray,
        artifactId: String?,
        stepIndex: Int?,
        agentType: String
    ): GeneratedImage {
        val imageId = UUID.randomUUID().toString()
        val fileName = "${artifactId ?: agentType}_${stepIndex ?: "general"}_$imageId.png"
        val imageFile = File(context.filesDir, "generated_images/$fileName")
        
        // Ensure directory exists
        imageFile.parentFile?.mkdirs()
        
        // Save image to local storage
        FileOutputStream(imageFile).use { fos ->
            fos.write(imageData)
        }
        
        return GeneratedImage(
            id = imageId,
            localPath = imageFile.absolutePath,
            prompt = "Generated for ${agentType} step ${stepIndex ?: 0}",
            model = "gemini-2.5-flash-image",
            width = 1024, // Default Gemini image size
            height = 1024,
            costCredits = 1, // Will be calculated based on actual token usage
            isDownloaded = true
        )
    }
    
    private fun identifyStepsForImages(artifact: Artifact, maxImages: Int): List<StepImageInfo> {
        return when (val content = artifact.contentData) {
            is ArtifactContent.RecipeContent -> {
                identifyRecipeStepsForImages(content.recipe, maxImages)
            }
            is ArtifactContent.CraftContent -> {
                identifyCraftStepsForImages(content.steps, maxImages)
            }
            is ArtifactContent.DIYContent -> {
                identifyDIYStepsForImages(content.steps, maxImages)
            }
            is ArtifactContent.TutorialContent -> {
                identifyTutorialStepsForImages(content.modules, maxImages)
            }
            else -> emptyList()
        }
    }
    
    private fun identifyRecipeStepsForImages(recipe: com.craftflowtechnologies.guidelens.cooking.Recipe, maxImages: Int): List<StepImageInfo> {
        val keywordPriority = mapOf(
            "mix" to 1, "combine" to 1, "whisk" to 1,
            "cook" to 3, "bake" to 3, "fry" to 3, "grill" to 3,
            "prepare" to 2, "chop" to 2, "dice" to 2,
            "final" to 4, "serve" to 4, "plate" to 4, "garnish" to 4
        )
        
        return recipe.steps
            .mapIndexed { index, step ->
                val priority = keywordPriority.entries.find { 
                    step.description.contains(it.key, ignoreCase = true) 
                }?.value ?: 0
                
                StepImageInfo(
                    stepIndex = index,
                    stepId = step.id,
                    description = step.description,
                    priority = priority,
                    isKeyMilestone = priority >= 3
                )
            }
            .filter { it.priority > 0 }
            .sortedByDescending { it.priority }
            .take(maxImages)
    }
    
    private fun identifyCraftStepsForImages(steps: List<CraftStep>, maxImages: Int): List<StepImageInfo> {
        val keywordPriority = mapOf(
            "cut" to 2, "shape" to 2, "form" to 2,
            "paint" to 3, "glue" to 2, "attach" to 2,
            "finish" to 4, "complete" to 4, "final" to 4
        )
        
        return steps
            .mapIndexed { index, step ->
                val priority = keywordPriority.entries.find { 
                    step.description.contains(it.key, ignoreCase = true) 
                }?.value ?: 0
                
                StepImageInfo(
                    stepIndex = index,
                    stepId = step.stepNumber.toString(),
                    description = step.description,
                    priority = priority,
                    isKeyMilestone = priority >= 3
                )
            }
            .filter { it.priority > 0 }
            .sortedByDescending { it.priority }
            .take(maxImages)
    }
    
    private fun identifyDIYStepsForImages(steps: List<DIYStep>, maxImages: Int): List<StepImageInfo> {
        val keywordPriority = mapOf(
            "measure" to 1, "mark" to 1,
            "cut" to 3, "drill" to 3, "install" to 3,
            "assemble" to 2, "attach" to 2,
            "finish" to 4, "complete" to 4, "test" to 4
        )
        
        return steps
            .mapIndexed { index, step ->
                val priority = keywordPriority.entries.find { 
                    step.description.contains(it.key, ignoreCase = true) 
                }?.value ?: 0
                
                StepImageInfo(
                    stepIndex = index,
                    stepId = step.stepNumber.toString(),
                    description = step.description,
                    priority = priority,
                    isKeyMilestone = priority >= 3
                )
            }
            .filter { it.priority > 0 }
            .sortedByDescending { it.priority }
            .take(maxImages)
    }
    
    private fun identifyTutorialStepsForImages(modules: List<LearningModule>, maxImages: Int): List<StepImageInfo> {
        return modules
            .take(maxImages)
            .mapIndexed { index, module ->
                StepImageInfo(
                    stepIndex = index,
                    stepId = index.toString(),
                    description = module.description,
                    priority = 2,
                    isKeyMilestone = false
                )
            }
    }
    
    private fun buildStepImagePrompt(artifact: Artifact, stepIndex: Int, description: String): String {
        val agentContext = when (artifact.agentType.lowercase()) {
            "cooking" -> "cooking step showing $description for ${artifact.title}"
            "crafting" -> "craft project step showing $description for ${artifact.title}"
            "diy" -> "DIY project step showing $description for ${artifact.title}"
            "buddy" -> "educational diagram showing $description for ${artifact.title}"
            else -> "step-by-step image showing $description"
        }
        
        return agentContext
    }
}

// Data classes for Gemini API
@Serializable
data class GeminiImageRequest(
    val contents: List<GeminiContent>
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
sealed class GeminiPart {
    @Serializable
    data class TextPart(
        val text: String
    ) : GeminiPart()
    
    @Serializable
    data class ImagePart(
        val inlineData: GeminiInlineData
    ) : GeminiPart()
}

@Serializable
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

// Result and helper classes
data class ImageGenerationResult(
    val success: Boolean,
    val image: GeneratedImage? = null,
    val error: String? = null,
    val creditsUsed: Int = 0
)

data class StepImageInfo(
    val stepIndex: Int,
    val stepId: String,
    val description: String,
    val priority: Int,
    val isKeyMilestone: Boolean
)