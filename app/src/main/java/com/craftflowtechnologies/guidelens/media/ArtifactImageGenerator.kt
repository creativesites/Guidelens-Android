package com.craftflowtechnologies.guidelens.media

import android.util.Log
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.CookingStep
import com.craftflowtechnologies.guidelens.api.XAIImageClient
import com.craftflowtechnologies.guidelens.media.ImageGenerationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class ImageGenerationRequest(
    val prompt: String,
    val style: ImageStyle = ImageStyle.REALISTIC,
    val aspectRatio: AspectRatio = AspectRatio.SQUARE,
    val quality: ImageQuality = ImageQuality.STANDARD,
    val creditsRequired: Int = 1
)

@Serializable
enum class ImageStyle {
    REALISTIC, ILLUSTRATION, SKETCH, CARTOON, PHOTOGRAPHIC, ARTISTIC
}

@Serializable
enum class AspectRatio {
    SQUARE, LANDSCAPE, PORTRAIT
}

@Serializable
enum class ImageQuality {
    DRAFT, STANDARD, HIGH, PREMIUM
}


@Serializable
data class BatchImageGenerationResult(
    val artifactId: String,
    val mainImage: GeneratedImage? = null,
    val stageImages: List<StageImage> = emptyList(),
    val totalCreditsUsed: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val errors: List<String> = emptyList()
)

class ArtifactImageGenerator(
    private val xaiImageClient: XAIImageClient,
    private val artifactRepository: ArtifactRepository
) {
    companion object {
        private const val TAG = "ArtifactImageGenerator"
        
        // Prompt templates for different artifact types
        private const val RECIPE_MAIN_PROMPT_TEMPLATE = """
            Professional food photography of finished %s, beautifully plated and garnished, 
            appetizing presentation, natural lighting, restaurant quality, high detail, 
            %s style cuisine, served on elegant dinnerware
        """
        
        private const val RECIPE_STEP_PROMPT_TEMPLATE = """
            Step-by-step cooking photography showing %s, %s, clear view of the cooking technique, 
            professional kitchen setting, natural lighting, instructional style, 
            showing hands and cooking utensils in action
        """
        
        private const val CRAFT_MAIN_PROMPT_TEMPLATE = """
            Beautiful finished %s craft project, professional product photography, 
            clean background, well-lit, showing fine details and craftsmanship, 
            high quality materials, artistic composition
        """
        
        private const val CRAFT_STEP_PROMPT_TEMPLATE = """
            Crafting tutorial photography showing %s, step %d of the process, 
            hands working with materials, clear view of technique being demonstrated, 
            craft supplies visible, instructional lighting, detailed close-up
        """
        
        private const val DIY_MAIN_PROMPT_TEMPLATE = """
            Professional photo of completed %s DIY project, clean finish, 
            well-constructed, showing the final result in its intended environment, 
            good lighting showcasing the quality of work
        """
        
        private const val DIY_STEP_PROMPT_TEMPLATE = """
            DIY tutorial photography showing %s, step %d, hands using tools, 
            safety equipment visible when needed, clear view of the construction technique, 
            workshop or construction setting, instructional style
        """
    }
    
    /**
     * Generate all images for a recipe artifact (main image + stage images)
     */
    suspend fun generateRecipeImages(
        recipe: Recipe,
        userId: String,
        includeAllSteps: Boolean = true
    ): BatchImageGenerationResult = withContext(Dispatchers.IO) {
        
        // Check user limits first
        val canGenerate = checkGenerationLimits(userId, recipe.steps.size + 1)
        if (!canGenerate.getOrDefault(false)) {
            return@withContext BatchImageGenerationResult(
                artifactId = recipe.id,
                errors = listOf("Image generation limit exceeded")
            )
        }
        
        val results = mutableListOf<ImageGenerationResult>()
        val stageImages = mutableListOf<StageImage>()
        var mainImageResult: GeneratedImage? = null
        var totalCredits = 0
        
        coroutineScope {
            try {
                // Generate main recipe image
                val mainImageJob = async {
                    generateMainRecipeImage(recipe)
                }
                
                // Generate step images
                val stepImageJobs = if (includeAllSteps) {
                    recipe.steps.mapIndexed { index, step ->
                        async {
                            generateRecipeStepImage(recipe, step, index)
                        }
                    }
                } else {
                    // Generate images only for key steps (critical steps or those with visual cues)
                    recipe.steps.filter { it.criticalStep || it.visualCues.isNotEmpty() }
                        .mapIndexed { index, step ->
                            async {
                                generateRecipeStepImage(recipe, step, step.stepNumber - 1)
                            }
                        }
                }
                
                // Await all results
                val mainResult = mainImageJob.await()
                val stepResults = stepImageJobs.awaitAll()
                
                // Process main image result
                if (mainResult.success && mainResult.image != null) {
                    mainImageResult = mainResult.image
                    totalCredits += mainResult.creditsUsed
                }
                
                // Process step results
                stepResults.forEachIndexed { index, result ->
                    if (result.success && result.image != null) {
                        val stepIndex = if (includeAllSteps) index else {
                            recipe.steps.indexOfFirst { 
                                it.criticalStep || it.visualCues.isNotEmpty() 
                            }
                        }
                        
                        stageImages.add(
                            StageImage(
                                stageNumber = stepIndex + 1,
                                stepId = recipe.steps.getOrNull(stepIndex)?.id,
                                image = result.image,
                                description = recipe.steps.getOrNull(stepIndex)?.description ?: "",
                                isKeyMilestone = recipe.steps.getOrNull(stepIndex)?.criticalStep == true
                            )
                        )
                        totalCredits += result.creditsUsed
                    }
                }
                
                results.addAll(listOf(mainResult) + stepResults)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating recipe images", e)
            }
        }
        
        // Update user credits
        updateUserCredits(userId, totalCredits)
        
        BatchImageGenerationResult(
            artifactId = recipe.id,
            mainImage = mainImageResult,
            stageImages = stageImages,
            totalCreditsUsed = totalCredits,
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            errors = results.mapNotNull { it.error }
        )
    }
    
    /**
     * Generate images for craft project artifact
     */
    suspend fun generateCraftImages(
        craftContent: ArtifactContent.CraftContent,
        title: String,
        userId: String
    ): BatchImageGenerationResult = withContext(Dispatchers.IO) {
        
        val totalImages = craftContent.steps.size + 1
        val canGenerate = checkGenerationLimits(userId, totalImages)
        if (!canGenerate.getOrDefault(false)) {
            return@withContext BatchImageGenerationResult(
                artifactId = title,
                errors = listOf("Image generation limit exceeded")
            )
        }
        
        val results = mutableListOf<ImageGenerationResult>()
        val stageImages = mutableListOf<StageImage>()
        var mainImageResult: GeneratedImage? = null
        var totalCredits = 0
        
        coroutineScope {
            try {
                // Generate main craft image
                val mainImageJob = async {
                    generateMainCraftImage(title)
                }
                
                // Generate step images
                val stepImageJobs = craftContent.steps.map { step ->
                    async {
                        generateCraftStepImage(title, step)
                    }
                }
                
                // Await all results
                val mainResult = mainImageJob.await()
                val stepResults = stepImageJobs.awaitAll()
                
                // Process results
                if (mainResult.success && mainResult.image != null) {
                    mainImageResult = mainResult.image
                    totalCredits += mainResult.creditsUsed
                }
                
                stepResults.forEachIndexed { index, result ->
                    if (result.success && result.image != null) {
                        stageImages.add(
                            StageImage(
                                stageNumber = index + 1,
                                stepId = craftContent.steps[index].stepNumber.toString(),
                                image = result.image,
                                description = craftContent.steps[index].description,
                                isKeyMilestone = craftContent.steps[index].techniques.isNotEmpty()
                            )
                        )
                        totalCredits += result.creditsUsed
                    }
                }
                
                results.addAll(listOf(mainResult) + stepResults)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating craft images", e)
            }
        }
        
        updateUserCredits(userId, totalCredits)
        
        BatchImageGenerationResult(
            artifactId = title,
            mainImage = mainImageResult,
            stageImages = stageImages,
            totalCreditsUsed = totalCredits,
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            errors = results.mapNotNull { it.error }
        )
    }
    
    /**
     * Generate images for DIY project artifact
     */
    suspend fun generateDIYImages(
        diyContent: ArtifactContent.DIYContent,
        title: String,
        userId: String
    ): BatchImageGenerationResult = withContext(Dispatchers.IO) {
        
        val totalImages = diyContent.steps.size + 1
        val canGenerate = checkGenerationLimits(userId, totalImages)
        if (!canGenerate.getOrDefault(false)) {
            return@withContext BatchImageGenerationResult(
                artifactId = title,
                errors = listOf("Image generation limit exceeded")
            )
        }
        
        val results = mutableListOf<ImageGenerationResult>()
        val stageImages = mutableListOf<StageImage>()
        var mainImageResult: GeneratedImage? = null
        var totalCredits = 0
        
        coroutineScope {
            try {
                // Generate main DIY project image
                val mainImageJob = async {
                    generateMainDIYImage(title)
                }
                
                // Generate step images
                val stepImageJobs = diyContent.steps.map { step ->
                    async {
                        generateDIYStepImage(title, step)
                    }
                }
                
                // Await all results
                val mainResult = mainImageJob.await()
                val stepResults = stepImageJobs.awaitAll()
                
                // Process results
                if (mainResult.success && mainResult.image != null) {
                    mainImageResult = mainResult.image
                    totalCredits += mainResult.creditsUsed
                }
                
                stepResults.forEachIndexed { index, result ->
                    if (result.success && result.image != null) {
                        stageImages.add(
                            StageImage(
                                stageNumber = index + 1,
                                stepId = diyContent.steps[index].stepNumber.toString(),
                                image = result.image,
                                description = diyContent.steps[index].description,
                                isKeyMilestone = diyContent.steps[index].safetyWarnings.isNotEmpty()
                            )
                        )
                        totalCredits += result.creditsUsed
                    }
                }
                
                results.addAll(listOf(mainResult) + stepResults)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating DIY images", e)
            }
        }
        
        updateUserCredits(userId, totalCredits)
        
        BatchImageGenerationResult(
            artifactId = title,
            mainImage = mainImageResult,
            stageImages = stageImages,
            totalCreditsUsed = totalCredits,
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            errors = results.mapNotNull { it.error }
        )
    }
    
    /**
     * Generate a single on-demand image for interactive sessions
     */
    suspend fun generateOnDemandImage(
        prompt: String,
        userId: String,
        artifactId: String,
        stageIndex: Int
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        
        // Check if user can generate on-demand images
        val userLimits = artifactRepository.getUserLimits(userId)
        val tierConfig = userLimits?.tier?.let { TierLimits.limits[it] }
        
        if (tierConfig?.canGenerateOnDemand != true) {
            return@withContext ImageGenerationResult(
                success = false,
                error = "On-demand image generation not available for your tier"
            )
        }
        
        val canGenerate = checkGenerationLimits(userId, 1)
        if (!canGenerate.getOrDefault(false)) {
            return@withContext ImageGenerationResult(
                success = false,
                error = "Image generation limit exceeded"
            )
        }
        
        try {
            val request = ImageGenerationRequest(
                prompt = enhancePromptForInstructional(prompt),
                style = ImageStyle.REALISTIC,
                quality = ImageQuality.STANDARD
            )
            
            val result = generateSingleImage(request)
            if (result.success) {
                updateUserCredits(userId, result.creditsUsed)
            }
            
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating on-demand image", e)
            return@withContext ImageGenerationResult(
                success = false,
                error = "Failed to generate image: ${e.message}"
            )
        }
    }
    
    // Private helper methods
    private suspend fun generateMainRecipeImage(recipe: Recipe): ImageGenerationResult {
        val prompt = RECIPE_MAIN_PROMPT_TEMPLATE.format(
            recipe.title,
            recipe.cuisine ?: "international"
        )
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.PHOTOGRAPHIC,
            quality = ImageQuality.HIGH
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateRecipeStepImage(
        recipe: Recipe,
        step: CookingStep,
        stepIndex: Int
    ): ImageGenerationResult {
        val prompt = RECIPE_STEP_PROMPT_TEMPLATE.format(
            step.description,
            step.visualCues.joinToString(", ").ifEmpty { step.techniques.joinToString(", ") }
        )
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.REALISTIC,
            quality = ImageQuality.STANDARD
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateMainCraftImage(title: String): ImageGenerationResult {
        val prompt = CRAFT_MAIN_PROMPT_TEMPLATE.format(title)
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.ARTISTIC,
            quality = ImageQuality.HIGH
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateCraftStepImage(title: String, step: CraftStep): ImageGenerationResult {
        val prompt = CRAFT_STEP_PROMPT_TEMPLATE.format(
            step.description,
            step.stepNumber
        )
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.ILLUSTRATION,
            quality = ImageQuality.STANDARD
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateMainDIYImage(title: String): ImageGenerationResult {
        val prompt = DIY_MAIN_PROMPT_TEMPLATE.format(title)
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.REALISTIC,
            quality = ImageQuality.HIGH
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateDIYStepImage(title: String, step: DIYStep): ImageGenerationResult {
        val prompt = DIY_STEP_PROMPT_TEMPLATE.format(
            step.description,
            step.stepNumber
        )
        
        val request = ImageGenerationRequest(
            prompt = prompt,
            style = ImageStyle.REALISTIC,
            quality = ImageQuality.STANDARD
        )
        
        return generateSingleImage(request)
    }
    
    private suspend fun generateSingleImage(request: ImageGenerationRequest): ImageGenerationResult {
        return try {
            // Using XAI image client for generation
            val result = xaiImageClient.generateImage(request.prompt)
            
            if (result.isSuccess) {
                val imageUrl = result.getOrNull()
                if (imageUrl != null) {
                    val generatedImage = GeneratedImage(
                        url = imageUrl,
                        prompt = request.prompt,
                        model = "xai-image-generator",
                        generatedAt = System.currentTimeMillis(),
                        costCredits = request.creditsRequired
                    )
                    
                    ImageGenerationResult(
                        success = true,
                        image = generatedImage,
                        creditsUsed = request.creditsRequired
                    )
                } else {
                    ImageGenerationResult(
                        success = false,
                        error = "No image URL returned"
                    )
                }
            } else {
                ImageGenerationResult(
                    success = false,
                    error = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating single image", e)
            ImageGenerationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }
    
    private suspend fun checkGenerationLimits(userId: String, imageCount: Int): Result<Boolean> {
        return artifactRepository.checkImageGenerationLimits(userId, imageCount)
    }
    
    private suspend fun updateUserCredits(userId: String, creditsUsed: Int) {
        try {
            val currentLimits = artifactRepository.getUserLimits(userId)
            if (currentLimits != null) {
                val updatedLimits = currentLimits.copy(
                    creditsRemaining = (currentLimits.creditsRemaining - creditsUsed).coerceAtLeast(0),
                    imagesGeneratedToday = currentLimits.imagesGeneratedToday + creditsUsed
                )
                artifactRepository.updateUserLimits(updatedLimits)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user credits", e)
        }
    }
    
    private fun enhancePromptForInstructional(basePrompt: String): String {
        return "$basePrompt, instructional photography, clear and detailed, good lighting, " +
               "educational style, showing proper technique, step-by-step tutorial format"
    }
    
    /**
     * Batch update artifact with generated images
     */
    suspend fun updateArtifactWithImages(
        artifactId: String,
        batchResult: BatchImageGenerationResult,
        userId: String
    ): Result<Artifact> {
        return try {
            val artifact = artifactRepository.getArtifactById(artifactId, userId)
            if (artifact != null) {
                val updatedArtifact = artifact.copy(
                    mainImage = batchResult.mainImage ?: artifact.mainImage,
                    stageImages = batchResult.stageImages.ifEmpty { artifact.stageImages },
                    updatedAt = System.currentTimeMillis(),
                    generationMetadata = GenerationMetadata(
                        model = "xai-image-generator",
                        prompt = "Batch generation for ${artifact.type}",
                        generationTime = System.currentTimeMillis(),
                        tokensUsed = 0, // Images don't use tokens
                        estimatedCost = batchResult.totalCreditsUsed.toFloat() * 0.01f, // $0.01 per credit
                        qualityScore = if (batchResult.successCount > 0) 0.8f else 0.0f
                    )
                )
                
                artifactRepository.saveArtifact(updatedArtifact)
            } else {
                Result.failure(Exception("Artifact not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating artifact with images", e)
            Result.failure(e)
        }
    }
}