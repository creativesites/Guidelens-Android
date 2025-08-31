package com.craftflowtechnologies.guidelens.clerk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craftflowtechnologies.guidelens.R

@Composable
fun EnhancedClerkAuthScreen(
    isDarkTheme: Boolean = false,
    onAuthSuccess: () -> Unit = {},
    onBackPressed: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { EnhancedMainViewModel(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Listen for successful authentication
    LaunchedEffect(state) {
        if (state == EnhancedAuthUiState.SignedIn) {
            onAuthSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            Color(0xFF0F0F1E),
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    } else {
                        listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE8ECF1),
                            Color(0xFFD6E8F5)
                        )
                    }
                )
            )
    ) {
        // Background pattern/blur effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
        ) {
            // Decorative elements
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-100).dp, y = 200.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6C5CE7).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .offset(x = 200.dp, y = (-50).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00B894).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        when (state) {
            EnhancedAuthUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Signing in...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }
            }
            
            EnhancedAuthUiState.SignedOut, is EnhancedAuthUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App Logo and Branding
                    Card(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 32.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.guidelens_logo),
                                contentDescription = "GuideLens Logo",
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    
                    Text(
                        text = "GuideLens",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    
                    Text(
                        text = "Real-time AI skills guidance",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        modifier = Modifier.padding(bottom = 48.dp)
                    )
                    
                    // Show error if present
                    if (state is EnhancedAuthUiState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = (state as EnhancedAuthUiState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    // Authentication form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) {
                                Color.Black.copy(alpha = 0.3f)
                            } else {
                                Color.White.copy(alpha = 0.9f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            EnhancedSignInOrUpView(
                                onGoogleSignIn = { viewModel.signInWithGoogle() },
                                isDarkTheme = isDarkTheme,
                                onBackPressed = onBackPressed,
                                onAuthSuccess = onAuthSuccess
                            )
                        }
                    }
                }
            }
            
            EnhancedAuthUiState.SignedIn -> {
                // This state is handled by LaunchedEffect above
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Welcome to GuideLens!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedSignInOrUpView(
    onGoogleSignIn: () -> Unit,
    isDarkTheme: Boolean = false,
    onBackPressed: () -> Unit = {},
    onAuthSuccess: () -> Unit = {}
) {
    var isSignUp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (isSignUp) {
            EnhancedSignUpView(
                onGoogleSignIn = onGoogleSignIn,
                isDarkTheme = isDarkTheme,
                onBackToSignIn = { isSignUp = false },
                onBackPressed = onBackPressed,
                onSignUpSuccess = onAuthSuccess
            )
        } else {
            EnhancedSignInView(
                onGoogleSignIn = onGoogleSignIn,
                isDarkTheme = isDarkTheme,
                onBackPressed = onBackPressed,
                onSignInSuccess = onAuthSuccess
            )
        }

        OutlinedButton(
            onClick = { isSignUp = !isSignUp },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isSignUp) {
                    "Already have an account? Sign in"
                } else {
                    "Don't have an account? Sign up"
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}