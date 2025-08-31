package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * xAI Image Generation Client for GuideLens
 * Implements xAI's Grok image generation API
 */
class XAIImageClient(private val context: Context) {
    
    companion object {
        private const val TAG = "XAIImageClient"
        private const val BASE_URL = "https://api.x.ai/v1"
        private const val IMAGE_GENERATION_ENDPOINT = "$BASE_URL/images/generations"
        
        // Updated model names based on xAI documentation
        private const val MODEL = "grok-2-image-1212" // Latest specific version
        private const val MODEL_ALIAS = "grok-2-image" // Alias version
        private const val MODEL_LATEST = "grok-2-image-latest" // Always latest
        
        private const val DEFAULT_TIMEOUT = 60000L // 60 seconds
        
        // Supported models for fallback
        private val SUPPORTED_MODELS = listOf(
            MODEL, // Try specific version first
            MODEL_LATEST, // Then latest
            MODEL_ALIAS // Finally alias
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(DEFAULT_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .writeTimeout(DEFAULT_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .readTimeout(DEFAULT_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // TODO: Replace with your actual xAI API key
    private val apiKey = "YOUR_XAI_API_KEY_HERE" // Add your xAI API key here

    @Serializable
    data class ImageGenerationRequest(
        val model: String = MODEL,
        val prompt: String,
        val n: Int = 1,
        val response_format: String = "url" // "url" or "b64_json"
    )

    @Serializable
    data class ImageGenerationResponse(
        val data: List<ImageData>
    )

    @Serializable
    data class ImageData(
        val url: String? = null,
        val b64_json: String? = null,
        val revised_prompt: String? = null
    )

    @Serializable
    data class ErrorResponse(
        val error: ErrorDetail
    )

    @Serializable
    data class ErrorDetail(
        val message: String,
        val type: String,
        val code: String? = null
    )

    /**
     * Generate a single image from a text prompt with model fallback
     * @param prompt The text description of the image to generate
     * @param modelName Specific model to use (null for automatic selection)
     */
    suspend fun generateImage(prompt: String, modelName: String? = null): Result<String> {
        return generateImages(prompt, 1, false, modelName).map { images ->
            images.firstOrNull() ?: throw Exception("No image generated")
        }
    }

    /**
     * Generate multiple images from a text prompt with model fallback
     * @param prompt The text description of the image to generate
     * @param count Number of images to generate (1-10)
     * @param useBase64 Whether to return base64 encoded images instead of URLs
     * @param modelName Specific model to use (null for automatic selection)
     * @return List of image URLs or base64 encoded images
     */
    suspend fun generateImages(
        prompt: String,
        count: Int = 1,
        useBase64: Boolean = false,
        modelName: String? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        
        if (apiKey == "YOUR_XAI_API_KEY_HERE") {
            return@withContext Result.failure(
                Exception("xAI API key not configured. Please set your API key in XAIImageClient.kt")
            )
        }

        try {
            // Use specified model or try fallback models
            val modelsToTry = if (modelName != null) {
                listOf(modelName)
            } else {
                SUPPORTED_MODELS
            }
            
            var lastError: Exception? = null
            
            for (model in modelsToTry) {
                try {
                    Log.d(TAG, "Attempting image generation with model: $model")
                    
                    val requestBody = ImageGenerationRequest(
                        model = model,
                        prompt = prompt,
                        n = count.coerceIn(1, 10),
                        response_format = if (useBase64) "b64_json" else "url"
                    )

                    val jsonBody = json.encodeToString(ImageGenerationRequest.serializer(), requestBody)
                    Log.d(TAG, "Generating $count image(s) with model $model and prompt: $prompt")

                    val request = Request.Builder()
                        .url(IMAGE_GENERATION_ENDPOINT)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .post(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.w(TAG, "Model $model failed: ${response.code} - $responseBody")
                        
                        val errorResponse = try {
                            json.decodeFromString(ErrorResponse.serializer(), responseBody ?: "")
                        } catch (e: Exception) {
                            null
                        }
                        
                        val errorMessage = errorResponse?.error?.message ?: "HTTP ${response.code}: ${response.message}"
                        lastError = Exception("xAI API Error with $model: $errorMessage")
                        
                        // If this is a model-specific error, try next model
                        if (response.code == 404 || response.code == 400) {
                            continue
                        } else {
                            // For other errors, don't try other models
                            break
                        }
                    }

                    if (responseBody == null) {
                        lastError = Exception("Empty response body from $model")
                        continue
                    }

                    val imageResponse = json.decodeFromString(ImageGenerationResponse.serializer(), responseBody)
                    
                    val images = imageResponse.data.mapNotNull { imageData ->
                        when {
                            useBase64 && !imageData.b64_json.isNullOrBlank() -> imageData.b64_json
                            !useBase64 && !imageData.url.isNullOrBlank() -> imageData.url
                            else -> null
                        }
                    }

                    if (images.isEmpty()) {
                        lastError = Exception("No valid images in response from $model")
                        continue
                    }

                    Log.d(TAG, "Successfully generated ${images.size} image(s) using model: $model")
                    return@withContext Result.success(images)
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Error with model $model: ${e.message}")
                    lastError = e
                    continue
                }
            }
            
            // All models failed
            return@withContext Result.failure(
                lastError ?: Exception("All image generation models failed")
            )

        } catch (e: IOException) {
            Log.e(TAG, "Network error during image generation", e)
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during image generation", e)
            Result.failure(Exception("Image generation failed: ${e.message}"))
        }
    }

    /**
     * Generate cooking-specific images for recipes
     * Automatically generates multiple images for different aspects of cooking
     */
    suspend fun generateCookingImages(
        recipeName: String,
        recipeType: String = "dish",
        includeSteps: Boolean = true
    ): Result<CookingImageSet> = withContext(Dispatchers.IO) {
        try {
            val prompts = mutableListOf<String>()
            
            // Main dish image
            prompts.add("A beautifully plated $recipeName $recipeType, professional food photography, appetizing, well-lit, restaurant quality presentation")
            
            if (includeSteps) {
                // Cooking process images
                prompts.add("Ingredients for $recipeName laid out and organized on a clean kitchen counter, food photography style")
                prompts.add("Cooking process of $recipeName, showing the dish being prepared in a pan or pot, action shot, kitchen photography")
                prompts.add("Final plating and garnishing of $recipeName, chef's hands adding finishing touches, professional kitchen photography")
            }

            val imageResults = mutableMapOf<String, String>()
            var hasErrors = false

            // Generate images for each prompt
            prompts.forEachIndexed { index, prompt ->
                val result = generateImage(prompt)
                result.fold(
                    onSuccess = { imageUrl ->
                        val key = when (index) {
                            0 -> "final_dish"
                            1 -> "ingredients"
                            2 -> "cooking_process"  
                            3 -> "plating"
                            else -> "extra_$index"
                        }
                        imageResults[key] = imageUrl
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Failed to generate image for prompt: $prompt", error)
                        hasErrors = true
                    }
                )
            }

            if (imageResults.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to generate any cooking images"))
            }

            val cookingImageSet = CookingImageSet(
                recipeName = recipeName,
                finalDish = imageResults["final_dish"],
                ingredients = imageResults["ingredients"],
                cookingProcess = imageResults["cooking_process"],
                plating = imageResults["plating"],
                extraImages = imageResults.filterKeys { it.startsWith("extra_") }.values.toList(),
                hasPartialFailures = hasErrors
            )

            Result.success(cookingImageSet)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating cooking image set", e)
            Result.failure(e)
        }
    }

    /**
     * Generate step-by-step cooking images for a recipe
     */
    suspend fun generateRecipeStepImages(
        recipeName: String,
        steps: List<String>
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val stepImages = mutableListOf<String>()
            
            steps.forEachIndexed { index, step ->
                val prompt = "Step ${index + 1} of cooking $recipeName: $step. Kitchen photography, clear view of the cooking process, professional food photography"
                
                val result = generateImage(prompt)
                result.fold(
                    onSuccess = { imageUrl ->
                        stepImages.add(imageUrl)
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Failed to generate image for step ${index + 1}: $step", error)
                        // Continue with other steps even if one fails
                    }
                )
            }

            if (stepImages.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to generate any step images"))
            }

            Result.success(stepImages)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating recipe step images", e)
            Result.failure(e)
        }
    }

    /**
     * Generate ingredient-specific images
     */
    suspend fun generateIngredientImages(ingredients: List<String>): Result<List<String>> {
        return generateImages(
            prompt = "Fresh ingredients for cooking: ${ingredients.joinToString(", ")}. Clean, organized layout on white background, food photography style",
            count = 1
        )
    }

    data class CookingImageSet(
        val recipeName: String,
        val finalDish: String? = null,
        val ingredients: String? = null,
        val cookingProcess: String? = null,
        val plating: String? = null,
        val extraImages: List<String> = emptyList(),
        val hasPartialFailures: Boolean = false
    )
}

/**
 * Helper function to create enhanced cooking prompts
 */
fun createCookingPrompt(
    dish: String,
    style: String = "professional food photography",
    lighting: String = "natural lighting",
    angle: String = "overhead view",
    additional: String = ""
): String {
    return buildString {
        append(dish)
        append(", ")
        append(style)
        append(", ")
        append(lighting)
        append(", ")
        append(angle)
        if (additional.isNotBlank()) {
            append(", ")
            append(additional)
        }
        append(", high quality, appetizing, restaurant presentation")
    }
}