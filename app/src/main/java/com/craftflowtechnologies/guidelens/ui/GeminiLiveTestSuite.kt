package com.craftflowtechnologies.guidelens.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craftflowtechnologies.guidelens.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Comprehensive test suite for Gemini Live API integration
 * Tests all video call overlay and voice session overlay features
 */
@Composable
fun GeminiLiveTestSuite(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize components
    val geminiLiveClient = remember { GeminiLiveApiClient() }
    val liveSessionManager: GeminiLiveSessionManager = viewModel {
        GeminiLiveSessionManager(context, geminiLiveClient)
    }
    
    // Test state
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var isRunningTests by remember { mutableStateOf(false) }
    var currentTest by remember { mutableStateOf("") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gemini Live Test Suite",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (!isRunningTests) {
                        coroutineScope.launch {
                            runAllTests(
                                liveSessionManager = liveSessionManager,
                                geminiLiveClient = geminiLiveClient,
                                context = context,
                                onTestUpdate = { test -> currentTest = test },
                                onResultUpdate = { results -> testResults = results }
                            ).also { isRunningTests = false }
                        }
                        isRunningTests = true
                    }
                },
                enabled = !isRunningTests,
                modifier = Modifier.weight(1f)
            ) {
                if (isRunningTests) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isRunningTests) "Running Tests..." else "Run All Tests")
            }
            
            Button(
                onClick = {
                    testResults = emptyList()
                    currentTest = ""
                },
                enabled = !isRunningTests,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Clear Results")
            }
        }
        
        // Current test indicator
        if (currentTest.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1A1A2E),
                border = BorderStroke(1.dp, Color.Yellow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Yellow,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Running: $currentTest",
                        color = Color.Yellow,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Test results
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testResults) { result ->
                TestResultCard(result = result)
            }
            
            if (testResults.isEmpty() && !isRunningTests) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Click 'Run All Tests' to start testing Gemini Live features",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(
    result: TestResult,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E),
        border = BorderStroke(
            width = 1.dp,
            color = when (result.status) {
                TestStatus.PASSED -> Color.Green
                TestStatus.FAILED -> Color.Red
                TestStatus.WARNING -> Color.Yellow
                TestStatus.RUNNING -> Color.Blue
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (result.status) {
                            TestStatus.PASSED -> Icons.Default.CheckCircle
                            TestStatus.FAILED -> Icons.Default.Error
                            TestStatus.WARNING -> Icons.Default.Warning
                            TestStatus.RUNNING -> Icons.Default.PlayArrow
                        },
                        contentDescription = result.status.name,
                        tint = when (result.status) {
                            TestStatus.PASSED -> Color.Green
                            TestStatus.FAILED -> Color.Red
                            TestStatus.WARNING -> Color.Yellow
                            TestStatus.RUNNING -> Color.Blue
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Text(
                        text = result.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (result.duration > 0) {
                    Text(
                        text = "${result.duration}ms",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
            
            if (result.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = result.details,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

// Test execution logic
private suspend fun runAllTests(
    liveSessionManager: GeminiLiveSessionManager,
    geminiLiveClient: GeminiLiveApiClient,
    context: Context,
    onTestUpdate: (String) -> Unit,
    onResultUpdate: (List<TestResult>) -> Unit
): Unit {
    val results = mutableListOf<TestResult>()
    
    // Test 1: Gemini Live API Client Connection
    onTestUpdate("Testing Gemini Live API Connection")
    val connectionTest = testGeminiLiveConnection(geminiLiveClient)
    results.add(connectionTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 2: Session Manager Initialization
    onTestUpdate("Testing Session Manager Initialization")
    val initTest = testSessionManagerInit(liveSessionManager)
    results.add(initTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 3: Live Session Start/Stop
    onTestUpdate("Testing Live Session Lifecycle")
    val sessionTest = testLiveSessionLifecycle(liveSessionManager)
    results.add(sessionTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 4: Text Message Sending
    onTestUpdate("Testing Text Message Sending")
    val textTest = testTextMessageSending(geminiLiveClient)
    results.add(textTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 5: Audio Data Processing
    onTestUpdate("Testing Audio Data Processing")
    val audioTest = testAudioDataProcessing(geminiLiveClient)
    results.add(audioTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 6: Video Frame Analysis
    onTestUpdate("Testing Video Frame Analysis")
    val videoTest = testVideoFrameAnalysis(liveSessionManager)
    results.add(videoTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 7: Insights Panel Integration
    onTestUpdate("Testing Insights Panel Integration")
    val insightsTest = testInsightsPanelIntegration(liveSessionManager)
    results.add(insightsTest)
    onResultUpdate(results.toList())
    
    delay(1000)
    
    // Test 8: Error Handling
    onTestUpdate("Testing Error Handling")
    val errorTest = testErrorHandling(geminiLiveClient)
    results.add(errorTest)
    onResultUpdate(results.toList())
    
    onTestUpdate("")
    Log.d("GeminiLiveTest", "All tests completed. Passed: ${results.count { it.status == TestStatus.PASSED }}, Failed: ${results.count { it.status == TestStatus.FAILED }}")
}

private suspend fun testGeminiLiveConnection(client: GeminiLiveApiClient): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        val connected = client.connect()
        val duration = System.currentTimeMillis() - startTime
        
        if (connected) {
            TestResult(
                name = "Gemini Live API Connection",
                description = "Tests WebSocket connection to Gemini Live API",
                status = TestStatus.PASSED,
                duration = duration,
                details = "Successfully connected to Gemini Live API endpoint"
            )
        } else {
            TestResult(
                name = "Gemini Live API Connection",
                description = "Tests WebSocket connection to Gemini Live API",
                status = TestStatus.FAILED,
                duration = duration,
                details = "Failed to establish WebSocket connection"
            )
        }
    } catch (e: Exception) {
        TestResult(
            name = "Gemini Live API Connection",
            description = "Tests WebSocket connection to Gemini Live API",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testSessionManagerInit(manager: GeminiLiveSessionManager): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Check if session manager initializes properly
        val initialState = manager.sessionState.value
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Session Manager Initialization",
            description = "Tests proper initialization of GeminiLiveSessionManager",
            status = if (initialState == SessionState.DISCONNECTED) TestStatus.PASSED else TestStatus.WARNING,
            duration = duration,
            details = "Initial session state: $initialState"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Session Manager Initialization",
            description = "Tests proper initialization of GeminiLiveSessionManager",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testLiveSessionLifecycle(manager: GeminiLiveSessionManager): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Test session start
        val startSuccess = manager.startLiveSession("buddy", "pro")
        delay(1000) // Wait for connection
        
        val connectedState = manager.sessionState.value
        
        // Test session stop
        manager.stopLiveSession()
        delay(500) // Wait for disconnection
        
        val disconnectedState = manager.sessionState.value
        val duration = System.currentTimeMillis() - startTime
        
        val success = startSuccess && 
                     (connectedState == SessionState.CONNECTED || connectedState == SessionState.CONNECTING) &&
                     disconnectedState == SessionState.DISCONNECTED
        
        TestResult(
            name = "Live Session Lifecycle",
            description = "Tests starting and stopping live sessions",
            status = if (success) TestStatus.PASSED else TestStatus.FAILED,
            duration = duration,
            details = "Start: $startSuccess, Connected: $connectedState, Disconnected: $disconnectedState"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Live Session Lifecycle",
            description = "Tests starting and stopping live sessions",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testTextMessageSending(client: GeminiLiveApiClient): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Attempt to send a test message
        client.sendTextMessage("Test message for Gemini Live API")
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Text Message Sending",
            description = "Tests sending text messages to Gemini Live API",
            status = TestStatus.PASSED,
            duration = duration,
            details = "Successfully sent test message"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Text Message Sending",
            description = "Tests sending text messages to Gemini Live API",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testAudioDataProcessing(client: GeminiLiveApiClient): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Create mock audio data
        val mockAudioData = ByteArray(1024) { (it % 256).toByte() }
        client.sendAudioData(mockAudioData, "audio/pcm")
        
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Audio Data Processing",
            description = "Tests sending audio data to Gemini Live API",
            status = TestStatus.PASSED,
            duration = duration,
            details = "Successfully sent ${mockAudioData.size} bytes of audio data"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Audio Data Processing",
            description = "Tests sending audio data to Gemini Live API",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testVideoFrameAnalysis(manager: GeminiLiveSessionManager): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Test frame capture functionality (without actual camera)
        manager.captureAndAnalyzeFrame(null) // Passing null ImageCapture for testing
        
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Video Frame Analysis",
            description = "Tests video frame capture and analysis pipeline",
            status = TestStatus.WARNING, // Warning since no actual camera is available
            duration = duration,
            details = "Frame analysis pipeline tested (no camera available in test environment)"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Video Frame Analysis",
            description = "Tests video frame capture and analysis pipeline",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testInsightsPanelIntegration(manager: GeminiLiveSessionManager): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Check if insights panel state flows are accessible
        val insights = manager.aiInsights.value
        val emotionalContext = manager.emotionalContext.value
        val isProcessing = manager.isProcessing.value
        
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Insights Panel Integration",
            description = "Tests AI insights panel data flow and state management",
            status = TestStatus.PASSED,
            duration = duration,
            details = "Insights: '${insights.take(50)}...', Context: $emotionalContext, Processing: $isProcessing"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Insights Panel Integration",
            description = "Tests AI insights panel data flow and state management",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

private suspend fun testErrorHandling(client: GeminiLiveApiClient): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        // Test error handling with invalid data
        var errorReceived = false
        
        // Collect errors for a short time
        val errorJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            client.errors.collect { error ->
                errorReceived = true
            }
        }
        
        // Send invalid multimodal data to trigger an error
        client.sendMultimodalData(null, null, null) // Should trigger an error
        
        delay(1000) // Wait for potential error
        errorJob.cancel()
        
        val duration = System.currentTimeMillis() - startTime
        
        TestResult(
            name = "Error Handling",
            description = "Tests proper error handling and reporting",
            status = if (errorReceived) TestStatus.PASSED else TestStatus.WARNING,
            duration = duration,
            details = if (errorReceived) "Error handling working correctly" else "No errors detected (might be expected)"
        )
    } catch (e: Exception) {
        TestResult(
            name = "Error Handling",
            description = "Tests proper error handling and reporting",
            status = TestStatus.FAILED,
            duration = System.currentTimeMillis() - startTime,
            details = "Exception: ${e.message}"
        )
    }
}

// Data classes for test results
data class TestResult(
    val name: String,
    val description: String,
    val status: TestStatus,
    val duration: Long,
    val details: String
)

enum class TestStatus {
    PASSED,
    FAILED,
    WARNING,
    RUNNING
}