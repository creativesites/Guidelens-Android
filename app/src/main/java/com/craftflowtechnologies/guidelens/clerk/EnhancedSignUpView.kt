package com.craftflowtechnologies.guidelens.clerk

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craftflowtechnologies.guidelens.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSignUpView(
    onGoogleSignIn: () -> Unit,
    isDarkTheme: Boolean = false,
    signUpViewModel: SignUpViewModel = viewModel(),
    onBackToSignIn: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    onSignUpSuccess: () -> Unit = {}
) {
    val signUpState by signUpViewModel.uiState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showEmailForm by remember { mutableStateOf(false) }
    
    // Handle successful signup
    LaunchedEffect(signUpState) {
        if (signUpState == SignUpViewModel.SignUpUiState.Success) {
            onSignUpSuccess()
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onBackToSignIn,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("← Sign In")
            }
            
            TextButton(
                onClick = onBackPressed,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("← Back")
            }
        }
        
        Text(
            text = "Create Account", 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color.Black
        )
        
        when (signUpState) {
            is SignUpViewModel.SignUpUiState.NeedsVerification -> {
                var code by remember { mutableStateOf("") }
                
                // Back button for verification screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(
                        onClick = { signUpViewModel.clearError() }, // Go back to sign up form
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("← Back to Sign Up")
                    }
                }
                
                Text(
                    text = "Enter the verification code sent to your email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray
                )
                
                OutlinedTextField(
                    value = code, 
                    onValueChange = { code = it },
                    label = { Text("Verification Code") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
                    )
                )

                Button(
                    onClick = { signUpViewModel.verify(code) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = code.isNotBlank()
                ) { 
                    Text("Verify", fontSize = 16.sp, fontWeight = FontWeight.Medium) 
                }
            }
            
            is SignUpViewModel.SignUpUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (signUpState as SignUpViewModel.SignUpUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Show the same UI as SignedOut after error
                SignUpContent(
                    onGoogleSignIn = onGoogleSignIn,
                    isDarkTheme = isDarkTheme,
                    email = email,
                    onEmailChange = { 
                        email = it
                        signUpViewModel.clearError()
                    },
                    password = password,
                    onPasswordChange = { 
                        password = it
                        signUpViewModel.clearError()
                    },
                    showEmailForm = showEmailForm,
                    onShowEmailForm = { showEmailForm = it },
                    onSignUp = { signUpViewModel.signUp(email, password) },
                    onBackToSignIn = onBackToSignIn
                )
            }
            
            else -> {
                SignUpContent(
                    onGoogleSignIn = onGoogleSignIn,
                    isDarkTheme = isDarkTheme,
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    showEmailForm = showEmailForm,
                    onShowEmailForm = { showEmailForm = it },
                    onSignUp = { signUpViewModel.signUp(email, password) },
                    onBackToSignIn = onBackToSignIn
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpContent(
    onGoogleSignIn: () -> Unit,
    isDarkTheme: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showEmailForm: Boolean,
    onShowEmailForm: (Boolean) -> Unit,
    onSignUp: () -> Unit,
    onBackToSignIn: () -> Unit = {}
) {
    // Primary Google Sign-In Button
    Button(
        onClick = onGoogleSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_icon),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "Sign up with Google",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
    
    // Divider
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
        )
        Text(
            text = "or",
            modifier = Modifier.padding(horizontal = 16.dp),
            color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray,
            fontSize = 14.sp
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
        )
    }
    
    // Email/Password Option
    if (!showEmailForm) {
        OutlinedButton(
            onClick = { onShowEmailForm(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Sign up with Email",
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        // Email/Password Form
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = email, 
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.5f)
                )
            )

            Button(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = email.isNotBlank() && password.isNotBlank()
            ) { 
                Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Medium) 
            }
            
            TextButton(
                onClick = { onShowEmailForm(false) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Back to Google Sign-Up",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}