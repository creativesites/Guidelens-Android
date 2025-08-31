package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.craftflowtechnologies.guidelens.api.XAIApiTester
import com.craftflowtechnologies.guidelens.api.runXAIApiTests
import com.craftflowtechnologies.guidelens.ui.theme.GuideLensColors

/**
 * Test screen for xAI API connectivity and functionality
 */
@Composable
fun ApiTestScreen(
    onBackPressed: () -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isRunningTests by remember { mutableStateOf(false) }
    var testResults by remember { mutableStateOf<List<XAIApiTester.TestResult>>(emptyList()) }
    var quickTestResult by remember { mutableStateOf<XAIApiTester.TestResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (isDarkTheme) {
                    GuideLensColors.DarkBackground
                } else {
                    GuideLensColors.LightBackground
                }
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDarkTheme) {
                        GuideLensColors.DarkOnBackground
                    } else {
                        GuideLensColors.LightOnBackground
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "xAI API Tests",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) {
                    GuideLensColors.DarkOnBackground
                } else {
                    GuideLensColors.LightOnBackground
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Test Button
            Button(
                onClick = {
                    if (!isRunningTests) {
                        isRunningTests = true
                        coroutineScope.launch {
                            try {
                                val tester = XAIApiTester(context)
                                quickTestResult = tester.quickConnectivityTest()
                            } finally {
                                isRunningTests = false
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isRunningTests,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                if (isRunningTests) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Speed, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Test")
            }
            
            // Full Test Suite Button
            Button(
                onClick = {
                    if (!isRunningTests) {
                        isRunningTests = true
                        testResults = emptyList()
                        coroutineScope.launch {
                            try {
                                testResults = runXAIApiTests(context)
                            } finally {
                                isRunningTests = false
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isRunningTests,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) {
                        GuideLensColors.DarkPrimary
                    } else {
                        GuideLensColors.LightPrimary
                    }
                )
            ) {
                if (isRunningTests) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Full Tests")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Results
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quick Test Result
            quickTestResult?.let { result ->
                item {
                    TestResultCard(
                        result = result,
                        isDarkTheme = isDarkTheme,
                        isQuickTest = true
                    )
                }
            }
            
            // Full Test Results
            if (testResults.isNotEmpty()) {
                item {
                    TestSummaryCard(
                        results = testResults,
                        isDarkTheme = isDarkTheme
                    )
                }
                
                items(testResults) { result ->
                    TestResultCard(
                        result = result,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
            
            // Running indicator
            if (isRunningTests && testResults.isEmpty() && quickTestResult == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) {
                                GuideLensColors.DarkSurface
                            } else {
                                GuideLensColors.LightSurface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = if (isDarkTheme) {
                                    GuideLensColors.DarkPrimary
                                } else {
                                    GuideLensColors.LightPrimary
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Running xAI API Tests...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isDarkTheme) {
                                    GuideLensColors.DarkOnSurface
                                } else {
                                    GuideLensColors.LightOnSurface
                                }
                            )
                        }
                    }
                }
            }
            
            // Instructions
            if (!isRunningTests && testResults.isEmpty() && quickTestResult == null) {
                item {
                    InstructionsCard(isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}

@Composable
private fun TestResultCard(
    result: XAIApiTester.TestResult,
    isDarkTheme: Boolean,
    isQuickTest: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSurface
            } else {
                GuideLensColors.LightSurface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (result.success) Color(0xFF4CAF50) else Color(0xFFE91E63)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) {
                            GuideLensColors.DarkOnSurface
                        } else {
                            GuideLensColors.LightOnSurface
                        }
                    )
                }
                
                if (isQuickTest) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2196F3).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "QUICK",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) {
                        GuideLensColors.DarkOnSurfaceVariant
                    } else {
                        GuideLensColors.LightOnSurfaceVariant
                    },
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (result.responseTime > 0) {
                    Text(
                        text = "${result.responseTime}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
                
                if (result.imageUrl != null) {
                    Text(
                        text = "🖼️ Image Generated",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                if (result.errorCode != null) {
                    Text(
                        text = "Error: ${result.errorCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE91E63)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestSummaryCard(
    results: List<XAIApiTester.TestResult>,
    isDarkTheme: Boolean
) {
    val passed = results.count { it.success }
    val total = results.size
    val passRate = if (total > 0) (passed.toFloat() / total * 100).toInt() else 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (passRate >= 80) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else if (passRate >= 50) {
                Color(0xFFFF9800).copy(alpha = 0.1f)
            } else {
                Color(0xFFE91E63).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = if (passRate >= 80) Color(0xFF4CAF50) else if (passRate >= 50) Color(0xFFFF9800) else Color(0xFFE91E63)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Test Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$passed/$total tests passed ($passRate%)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionsCard(isDarkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                GuideLensColors.DarkSecondaryContainer.copy(alpha = 0.3f)
            } else {
                GuideLensColors.LightSecondaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDarkTheme) {
                        GuideLensColors.DarkPrimary
                    } else {
                        GuideLensColors.LightPrimary
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "xAI API Testing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = """
                    This screen tests connectivity and functionality with xAI's image generation API.
                    
                    • Quick Test: Fast connectivity check with simple image generation
                    • Full Tests: Comprehensive suite testing all features and models
                    
                    Tests include:
                    - API key validation
                    - Network connectivity  
                    - Model access (grok-2-image-1212, grok-2-image, grok-2-image-latest)
                    - Image generation with various prompts
                    - Error handling and fallback mechanisms
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkTheme) {
                    GuideLensColors.DarkOnSurfaceVariant
                } else {
                    GuideLensColors.LightOnSurfaceVariant
                }
            )
        }
    }
}