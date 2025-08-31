package com.craftflowtechnologies.guidelens.api

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.liveGenerationConfig

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import javax.inject.Inject

/**
 * Firebase AI Logic Test Helper
 * Use this to test Firebase integration after setup
 */
object FirebaseTestHelper {
    private const val TAG = "FirebaseTestHelper"

    /**
     * Test basic Firebase AI Logic connectivity
     * Call this after Firebase setup to verify integration
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun testFirebaseAIConnection(context: Context): TestResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing Firebase AI Logic connection...")

                // Try to initialize Firebase AI
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
                    modelName = "gemini-2.0-flash-live-preview-04-09",
                    generationConfig = liveGenerationConfig {
                        responseModality = ResponseModality.TEXT
                    }
                )

                Log.d(TAG, "Firebase AI Logic model created successfully")

                // Try to establish connection
                val session = model.connect()
                Log.d(TAG, "Firebase Live session connected successfully")

                // Send a simple test message
                session.send("Hello, this is a test message")
                Log.d(TAG, "Test message sent successfully")

                // Close the test session
                session.close()
                Log.d(TAG, "Test session closed successfully")

                TestResult.Success("Firebase AI Logic integration working correctly!")

            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI Logic test failed", e)
                TestResult.Error(
                    message = "Firebase integration failed: ${e.message}",
                    details = when {
                        e.message?.contains("API key", ignoreCase = true) == true ->
                            "Check your Gemini API key configuration in Firebase Console"
                        e.message?.contains("not found", ignoreCase = true) == true ->
                            "Ensure google-services.json is in the correct location"
                        e.message?.contains("permission", ignoreCase = true) == true ->
                            "Check Firebase project permissions and enabled services"
                        else -> "Check Firebase Console for project configuration"
                    }
                )
            }
        }
    }

    /**
     * Test Firebase project configuration
     */
    suspend fun testFirebaseProjectConfig(context: Context): TestResult {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get Firebase app instance
                val app = com.google.firebase.FirebaseApp.getInstance()
                
                Log.d(TAG, "Firebase app initialized: ${app.name}")
                Log.d(TAG, "Project ID: ${app.options.projectId}")
                Log.d(TAG, "Application ID: ${app.options.applicationId}")

                if (app.options.projectId.isNullOrEmpty()) {
                    return@withContext TestResult.Error(
                        message = "Firebase project ID not found",
                        details = "Check google-services.json file and Firebase project configuration"
                    )
                }

                TestResult.Success("Firebase project configuration valid")

            } catch (e: Exception) {
                Log.e(TAG, "Firebase project test failed", e)
                TestResult.Error(
                    message = "Firebase project configuration error: ${e.message}",
                    details = "Check google-services.json file and Firebase Console setup"
                )
            }
        }
    }

    /**
     * Comprehensive Firebase integration test
     */
    suspend fun runFullFirebaseTest(context: Context): List<TestResult> {
        val results = mutableListOf<TestResult>()
        
        Log.i(TAG, "Starting comprehensive Firebase integration test...")
        
        // Test 1: Project Configuration
        results.add(testFirebaseProjectConfig(context))
        
        // Test 2: AI Logic Connection (only if project config passed)
        if (results.last() is TestResult.Success) {
            results.add(testFirebaseAIConnection(context))
        } else {
            results.add(TestResult.Skipped("AI Logic test skipped due to project configuration failure"))
        }

        // Log summary
        val successCount = results.count { it is TestResult.Success }
        val totalCount = results.size
        Log.i(TAG, "Firebase test completed: $successCount/$totalCount tests passed")

        return results
    }
}

/**
 * Test result sealed class
 */
sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Error(val message: String, val details: String) : TestResult()
    data class Skipped(val reason: String) : TestResult()
}

/**
 * Extension function for easy result checking
 */
fun List<TestResult>.isAllSuccessful(): Boolean = all { it is TestResult.Success }

/**
 * Extension function to get summary message
 */
fun List<TestResult>.getSummary(): String {
    val successCount = count { it is TestResult.Success }
    val errorCount = count { it is TestResult.Error }
    val skippedCount = count { it is TestResult.Skipped }
    
    return buildString {
        appendLine("Firebase Integration Test Summary:")
        appendLine("✅ Passed: $successCount")
        if (errorCount > 0) appendLine("❌ Failed: $errorCount")
        if (skippedCount > 0) appendLine("⏭️ Skipped: $skippedCount")
        
        this@getSummary.forEach { result ->
            when (result) {
                is TestResult.Success -> appendLine("✅ ${result.message}")
                is TestResult.Error -> {
                    appendLine("❌ ${result.message}")
                    appendLine("   Details: ${result.details}")
                }
                is TestResult.Skipped -> appendLine("⏭️ ${result.reason}")
            }
        }
    }
}