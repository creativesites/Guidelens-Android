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
 * xAI API Testing Utility
 * Tests connectivity and model access for xAI's Grok image generation
 */
class XAIApiTester(private val context: Context) {
    
    companion object {
        private const val TAG = "XAIApiTester"
        private const val BASE_URL = "https://api.x.ai/v1"
        private const val IMAGE_GENERATION_ENDPOINT = "$BASE_URL/images/generations"
        private const val MODELS_ENDPOINT = "$BASE_URL/models"
        private const val DEFAULT_TIMEOUT = 30000L // 30 seconds for testing
        
        // Updated model information based on xAI documentation
        private const val LATEST_MODEL = "grok-2-image-1212"
        private val SUPPORTED_MODELS = listOf(
            "grok-2-image-1212",
            "grok-2-image",
            "grok-2-image-latest"
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
        prettyPrint = true
    }

    // Your xAI API key
    private val apiKey = "YOUR_XAI_API_KEY_HERE" // Add your xAI API key here

    @Serializable
    data class TestImageRequest(
        val model: String,
        val prompt: String,
        val n: Int = 1,
        val response_format: String = "url"
    )

    @Serializable
    data class TestImageResponse(
        val data: List<TestImageData>
    )

    @Serializable
    data class TestImageData(
        val url: String? = null,
        val b64_json: String? = null,
        val revised_prompt: String? = null
    )

    @Serializable
    data class ApiError(
        val error: ErrorDetail
    )

    @Serializable
    data class ErrorDetail(
        val message: String,
        val type: String,
        val code: String? = null
    )

    data class TestResult(
        val success: Boolean,
        val message: String,
        val details: String = "",
        val imageUrl: String? = null,
        val errorCode: String? = null,
        val responseTime: Long = 0
    )

    /**
     * Comprehensive API connectivity test
     */
    suspend fun runFullApiTest(): List<TestResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TestResult>()
        
        Log.i(TAG, "🚀 Starting xAI API Connectivity Tests")
        
        // Test 1: API Key Validation
        results.add(testApiKeyValidation())
        
        // Test 2: Basic Connectivity
        results.add(testBasicConnectivity())
        
        // Test 3: Model Access for each supported model
        SUPPORTED_MODELS.forEach { model ->
            results.add(testModelAccess(model))
        }
        
        // Test 4: Simple Image Generation
        results.add(testSimpleImageGeneration())
        
        // Test 5: Complex Prompt Test
        results.add(testComplexPrompt())
        
        // Test 6: Multiple Images Test
        results.add(testMultipleImages())
        
        // Test 7: Error Handling
        results.add(testErrorHandling())
        
        Log.i(TAG, "✅ xAI API Tests Completed: ${results.count { it.success }}/${results.size} passed")
        
        return@withContext results
    }

    private suspend fun testApiKeyValidation(): TestResult {
        Log.d(TAG, "🔑 Testing API Key Validation...")
        
        return if (apiKey.isBlank() || apiKey == "YOUR_XAI_API_KEY_HERE") {
            TestResult(
                success = false,
                message = "API Key Validation Failed",
                details = "API key is not configured or still using placeholder value"
            )
        } else if (!apiKey.startsWith("xai-")) {
            TestResult(
                success = false,
                message = "API Key Format Invalid",
                details = "xAI API keys should start with 'xai-'"
            )
        } else {
            TestResult(
                success = true,
                message = "API Key Validation Passed",
                details = "API key format appears correct: ${apiKey.take(8)}...${apiKey.takeLast(4)}"
            )
        }
    }

    private suspend fun testBasicConnectivity(): TestResult {
        Log.d(TAG, "🌐 Testing Basic Connectivity...")
        val startTime = System.currentTimeMillis()
        
        return try {
            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime
            
            TestResult(
                success = response.isSuccessful || response.code in 400..499, // 4xx means we reached the API
                message = "Basic Connectivity Test",
                details = "HTTP ${response.code} - Connection established in ${responseTime}ms",
                responseTime = responseTime
            )
        } catch (e: IOException) {
            TestResult(
                success = false,
                message = "Connectivity Failed",
                details = "Network error: ${e.message}",
                responseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun testModelAccess(model: String): TestResult {
        Log.d(TAG, "🤖 Testing Model Access: $model...")
        val startTime = System.currentTimeMillis()
        
        return try {
            val requestBody = TestImageRequest(
                model = model,
                prompt = "A simple test image: red apple on white background",
                n = 1,
                response_format = "url"
            )

            val jsonBody = json.encodeToString(TestImageRequest.serializer(), requestBody)
            
            val request = Request.Builder()
                .url(IMAGE_GENERATION_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful && responseBody != null) {
                val imageResponse = json.decodeFromString(TestImageResponse.serializer(), responseBody)
                val imageUrl = imageResponse.data.firstOrNull()?.url
                
                TestResult(
                    success = true,
                    message = "Model Access: $model ✅",
                    details = "Successfully generated image in ${responseTime}ms",
                    imageUrl = imageUrl,
                    responseTime = responseTime
                )
            } else {
                val errorMessage = if (responseBody != null) {
                    try {
                        val error = json.decodeFromString(ApiError.serializer(), responseBody)
                        error.error.message
                    } catch (e: Exception) {
                        responseBody.take(200)
                    }
                } else {
                    "No response body"
                }
                
                TestResult(
                    success = false,
                    message = "Model Access Failed: $model",
                    details = "HTTP ${response.code}: $errorMessage",
                    errorCode = response.code.toString(),
                    responseTime = responseTime
                )
            }
        } catch (e: Exception) {
            TestResult(
                success = false,
                message = "Model Test Error: $model",
                details = "Exception: ${e.message}",
                responseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun testSimpleImageGeneration(): TestResult {
        Log.d(TAG, "🎨 Testing Simple Image Generation...")
        return generateTestImage(
            prompt = "A delicious chocolate chip cookie, professional food photography",
            testName = "Simple Image Generation"
        )
    }

    private suspend fun testComplexPrompt(): TestResult {
        Log.d(TAG, "🎭 Testing Complex Prompt...")
        return generateTestImage(
            prompt = "Professional food photography of a beautifully plated pasta dish with marinara sauce, fresh basil, and parmesan cheese, restaurant quality presentation, well-lit, appetizing",
            testName = "Complex Prompt Test"
        )
    }

    private suspend fun testMultipleImages(): TestResult {
        Log.d(TAG, "🎯 Testing Multiple Images...")
        val startTime = System.currentTimeMillis()
        
        return try {
            val requestBody = TestImageRequest(
                model = LATEST_MODEL,
                prompt = "Step-by-step cooking process: mixing ingredients in a bowl",
                n = 3, // Generate 3 images
                response_format = "url"
            )

            val jsonBody = json.encodeToString(TestImageRequest.serializer(), requestBody)
            
            val request = Request.Builder()
                .url(IMAGE_GENERATION_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful && responseBody != null) {
                val imageResponse = json.decodeFromString(TestImageResponse.serializer(), responseBody)
                val imageCount = imageResponse.data.size
                
                TestResult(
                    success = imageCount >= 2, // At least 2 images should be generated
                    message = "Multiple Images Test",
                    details = "Generated $imageCount images in ${responseTime}ms",
                    imageUrl = imageResponse.data.firstOrNull()?.url,
                    responseTime = responseTime
                )
            } else {
                TestResult(
                    success = false,
                    message = "Multiple Images Failed",
                    details = "HTTP ${response.code}: ${responseBody?.take(200)}",
                    responseTime = responseTime
                )
            }
        } catch (e: Exception) {
            TestResult(
                success = false,
                message = "Multiple Images Error",
                details = "Exception: ${e.message}",
                responseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun testErrorHandling(): TestResult {
        Log.d(TAG, "❌ Testing Error Handling...")
        val startTime = System.currentTimeMillis()
        
        return try {
            // Test with invalid model
            val requestBody = TestImageRequest(
                model = "invalid-model-name",
                prompt = "Test prompt",
                n = 1
            )

            val jsonBody = json.encodeToString(TestImageRequest.serializer(), requestBody)
            
            val request = Request.Builder()
                .url(IMAGE_GENERATION_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseTime = System.currentTimeMillis() - startTime

            // We expect this to fail gracefully
            TestResult(
                success = !response.isSuccessful, // Success means error handling worked
                message = "Error Handling Test",
                details = "Expected error received: HTTP ${response.code}",
                responseTime = responseTime
            )
        } catch (e: Exception) {
            TestResult(
                success = true, // Exception handling worked
                message = "Error Handling Test",
                details = "Exception properly caught: ${e.message}",
                responseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    private suspend fun generateTestImage(prompt: String, testName: String): TestResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            val requestBody = TestImageRequest(
                model = LATEST_MODEL,
                prompt = prompt,
                n = 1,
                response_format = "url"
            )

            val jsonBody = json.encodeToString(TestImageRequest.serializer(), requestBody)
            
            val request = Request.Builder()
                .url(IMAGE_GENERATION_ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            val responseTime = System.currentTimeMillis() - startTime

            if (response.isSuccessful && responseBody != null) {
                val imageResponse = json.decodeFromString(TestImageResponse.serializer(), responseBody)
                val imageUrl = imageResponse.data.firstOrNull()?.url
                val revisedPrompt = imageResponse.data.firstOrNull()?.revised_prompt
                
                TestResult(
                    success = true,
                    message = "$testName ✅",
                    details = "Generated in ${responseTime}ms${if (revisedPrompt != null) ", revised prompt available" else ""}",
                    imageUrl = imageUrl,
                    responseTime = responseTime
                )
            } else {
                val errorMessage = if (responseBody != null) {
                    try {
                        val error = json.decodeFromString(ApiError.serializer(), responseBody)
                        error.error.message
                    } catch (e: Exception) {
                        responseBody.take(200)
                    }
                } else {
                    "No response body"
                }
                
                TestResult(
                    success = false,
                    message = "$testName Failed",
                    details = "HTTP ${response.code}: $errorMessage",
                    errorCode = response.code.toString(),
                    responseTime = responseTime
                )
            }
        } catch (e: Exception) {
            TestResult(
                success = false,
                message = "$testName Error",
                details = "Exception: ${e.message}",
                responseTime = System.currentTimeMillis() - startTime
            )
        }
    }

    /**
     * Quick connectivity test for UI use
     */
    suspend fun quickConnectivityTest(): TestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "⚡ Running Quick Connectivity Test...")
        
        return@withContext generateTestImage(
            prompt = "Simple test: red apple",
            testName = "Quick Test"
        )
    }

    /**
     * Print detailed test report
     */
    fun printTestReport(results: List<TestResult>) {
        Log.i(TAG, "📊 xAI API Test Report")
//        Log.i(TAG, "=" * 50)
        
        results.forEachIndexed { index, result ->
            val status = if (result.success) "✅ PASS" else "❌ FAIL"
            Log.i(TAG, "${index + 1}. $status | ${result.message}")
            if (result.details.isNotEmpty()) {
                Log.i(TAG, "   Details: ${result.details}")
            }
            if (result.responseTime > 0) {
                Log.i(TAG, "   Response Time: ${result.responseTime}ms")
            }
            if (result.imageUrl != null) {
                Log.i(TAG, "   Image URL: ${result.imageUrl}")
            }
            Log.i(TAG, "")
        }
        
        val passed = results.count { it.success }
        val total = results.size
        val passRate = (passed.toFloat() / total * 100).toInt()
        
        Log.i(TAG, "📈 Summary: $passed/$total tests passed ($passRate%)")
//        Log.i(TAG, "=" * 50)
    }
}

/**
 * Utility function to run tests from UI
 */
suspend fun runXAIApiTests(context: Context): List<XAIApiTester.TestResult> {
    val tester = XAIApiTester(context)
    val results = tester.runFullApiTest()
    tester.printTestReport(results)
    return results
}