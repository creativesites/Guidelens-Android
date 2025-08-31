package com.craftflowtechnologies.guidelens.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.craftflowtechnologies.guidelens.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Import Agent from Models.kt - no need to redefine

// Predefined agents
object AvailableAgents {
    val COOKING_ASSISTANT = Agent(
        id = "cooking",
        name = "Cooking Assistant",
        description = "Your culinary companion for recipes, techniques, and kitchen guidance",
        icon = Icons.Rounded.Restaurant,
        primaryColor = Color(0xFFFF6B6B),
        secondaryColor = Color(0xFFF2994A),
        features = listOf("Recipes", "Cooking Techniques", "Kitchen Safety", "Meal Planning"),
        quickActions = listOf("Start Recipe", "Set Timer", "Ask Question"),
        specialCapabilities = listOf("Visual Recipe Analysis", "Cooking Tips", "Safety Alerts"),
        image = "cooking_assistant.png"
    )

    val CRAFTING_GURU = Agent(
        id = "crafting",
        name = "Crafting Guru",
        description = "Creative expert for DIY projects, arts & crafts, and handmade creations",
        icon = Icons.Rounded.Palette,
        primaryColor = Color(0xFF4ECDC4),
        secondaryColor = Color(0xFF26A69A),
        features = listOf("DIY Projects", "Art & Crafts", "Creative Ideas", "Material Guidance"),
        quickActions = listOf("Start Project", "Material List", "Get Ideas"),
        specialCapabilities = listOf("Project Planning", "Material Identification", "Technique Guidance"),
        image = "crafting_guru.png"
    )

    val DIY_HELPER = Agent(
        id = "diy",
        name = "DIY Helper",
        description = "Hands-on assistant for home improvement and repair projects",
        icon = Icons.Rounded.Build,
        primaryColor = Color(0xFF45B7D1),
        secondaryColor = Color(0xFF26A69A),
        features = listOf("Home Repair", "Tools & Equipment", "Project Planning", "Safety"),
        quickActions = listOf("Start Repair", "Tool Help", "Safety Check"),
        specialCapabilities = listOf("Tool Identification", "Safety Monitoring", "Step Guidance"),
        image = "diy_helper.png"
    )

    val BUDDY = Agent(
        id = "buddy",
        name = "Buddy",
        description = "Your friendly all-around assistant for everyday tasks and learning",
        icon = Icons.Rounded.EmojiPeople,
        primaryColor = Color(0xFF96CEB4),
        secondaryColor = Color(0xFF66BB6A),
        features = listOf("General Help", "Learning", "Daily Tasks", "Conversation"),
        quickActions = listOf("Ask Question", "Get Help", "Learn Something"),
        specialCapabilities = listOf("General Assistance", "Learning Support", "Task Management"),
        image = "buddy.png"
    )

    fun getAll() = listOf(COOKING_ASSISTANT, CRAFTING_GURU, DIY_HELPER, BUDDY)
    fun getById(id: String) = getAll().find { it.id == id }
}

// Enhanced Auth State
data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val user: User? = null,
    val showOnboarding: Boolean = false,
    val currentStep: AuthStep = AuthStep.WELCOME
)

enum class AuthStep {
    WELCOME,
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD,
    ONBOARDING,
    COMPLETE
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun AuthenticationScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onForgotPassword: (String) -> Unit,
    onOnboardingComplete: (Agent) -> Unit,
    onSkipOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(AuthStep.WELCOME) }

    // Update step based on auth state
    LaunchedEffect(authState) {
        when {
            authState.isLoggedIn && authState.showOnboarding -> currentStep = AuthStep.ONBOARDING
            authState.isLoggedIn -> currentStep = AuthStep.COMPLETE
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F23),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // Animated background elements
        PremiumBackground()

        // Main content with smooth transitions
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn() with slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut()
            },
            label = "auth_step_transition"
        ) { step ->
            when (step) {
                AuthStep.WELCOME -> WelcomeScreen(
                    onGetStarted = { currentStep = AuthStep.LOGIN },
                    onSignUp = { currentStep = AuthStep.REGISTER }
                )
                AuthStep.LOGIN -> LoginScreen(
                    authState = authState,
                    onLogin = onLogin,
                    onGoogleLogin = onGoogleLogin,
                    onSwitchToRegister = { currentStep = AuthStep.REGISTER },
                    onForgotPassword = { currentStep = AuthStep.FORGOT_PASSWORD },
                    onBack = { currentStep = AuthStep.WELCOME }
                )
                AuthStep.REGISTER -> RegisterScreen(
                    authState = authState,
                    onRegister = onRegister,
                    onGoogleLogin = onGoogleLogin,
                    onSwitchToLogin = { currentStep = AuthStep.LOGIN },
                    onBack = { currentStep = AuthStep.WELCOME }
                )
                AuthStep.FORGOT_PASSWORD -> ForgotPasswordScreen(
                    authState = authState,
                    onResetPassword = onForgotPassword,
                    onBack = { currentStep = AuthStep.LOGIN }
                )
                AuthStep.ONBOARDING -> IntegratedOnboardingScreen(
                    onComplete = onOnboardingComplete,
                    onSkip = onSkipOnboarding
                )
                AuthStep.COMPLETE -> {
                    // This would typically navigate to main app
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Welcome to GuideLens!",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient orbs
        val infiniteTransition = rememberInfiniteTransition(label = "background")

        val offsetX1 by infiniteTransition.animateFloat(
            initialValue = -200f,
            targetValue = 200f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb1_x"
        )

        val offsetY1 by infiniteTransition.animateFloat(
            initialValue = 100f,
            targetValue = 300f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb1_y"
        )

        val offsetX2 by infiniteTransition.animateFloat(
            initialValue = 300f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(25000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb2_x"
        )

        // First orb
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = offsetX1.dp, y = offsetY1.dp)
                .blur(80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Second orb
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = offsetX2.dp, y = 500.dp)
                .blur(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Third static orb
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 50.dp, y = 700.dp)
                .blur(60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF06B6D4).copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSignUp: () -> Unit
) {
    val scale by rememberInfiniteTransition(label = "logo_animation").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Logo with breathing animation
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4F46E5).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.guide_lens_logo),
                contentDescription = "GuideLens Logo",
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Welcome text with typewriter effect
        Text(
            text = "Welcome to",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "GuideLens",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your AI-powered visual assistant for life's everyday challenges",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary CTA - Get Started
            PremiumButton(
                onClick = onGetStarted,
                text = "Get Started",
                isPrimary = true,
                icon = Icons.Rounded.ArrowForward
            )

            // Secondary CTA - Sign Up
            PremiumButton(
                onClick = onSignUp,
                text = "Create Account",
                isPrimary = false,
                icon = Icons.Rounded.PersonAdd
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LoginScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showValidation by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Validation
    val isEmailValid = email.contains("@") && email.contains(".")
    val isFormValid = isEmailValid && password.length >= 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding()
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Placeholder for symmetry
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Glass card container
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Column {
                    Text(
                        text = "Welcome back",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Sign in to continue your journey",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Form fields
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Email field
                    PremiumTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            showValidation = false
                        },
                        label = "Email address",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = showValidation && !isEmailValid,
                        errorMessage = if (showValidation && !isEmailValid) "Please enter a valid email" else null
                    )

                    // Password field
                    PremiumTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showValidation = false
                        },
                        label = "Password",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (isFormValid) {
                                    onLogin(email, password)
                                } else {
                                    showValidation = true
                                }
                            }
                        ),
                        isError = showValidation && password.length < 6,
                        errorMessage = if (showValidation && password.length < 6) "Password must be at least 6 characters" else null
                    )
                }

                // Forgot password
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Forgot password?",
                        color = Color(0xFF60A5FA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Error message
                authState.error?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Sign in button
                    PremiumButton(
                        onClick = {
                            if (isFormValid) {
                                onLogin(email, password)
                            } else {
                                showValidation = true
                            }
                        },
                        text = "Sign In",
                        isPrimary = true,
                        isLoading = authState.isLoading,
                        enabled = !authState.isLoading
                    )

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "or",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                    }

                    // Google sign in
                    PremiumSocialButton(
                        onClick = onGoogleLogin,
                        text = "Continue with Google",
                        icon = R.drawable.google_icon
                    )
                }

                // Switch to register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onSwitchToRegister,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign up",
                            color = Color(0xFF60A5FA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterScreen(
    authState: AuthState,
    onRegister: (String, String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showValidation by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Validation
    val isNameValid = name.trim().length >= 2
    val isEmailValid = email.contains("@") && email.contains(".")
    val isPasswordValid = password.length >= 6
    val isFormValid = isNameValid && isEmailValid && isPasswordValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding()
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Create Account",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Placeholder for symmetry
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Glass card container
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title
                Column {
                    Text(
                        text = "Join GuideLens",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Create your account to get started",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Form fields
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Name field
                    PremiumTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showValidation = false
                        },
                        label = "Full name",
                        placeholder = "Enter your full name",
                        leadingIcon = Icons.Rounded.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        isError = showValidation && !isNameValid,
                        errorMessage = if (showValidation && !isNameValid) "Name must be at least 2 characters" else null
                    )

                    // Email field
                    PremiumTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            showValidation = false
                        },
                        label = "Email address",
                        placeholder = "Enter your email",
                        leadingIcon = Icons.Rounded.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        isError = showValidation && !isEmailValid,
                        errorMessage = if (showValidation && !isEmailValid) "Please enter a valid email" else null
                    )

                    // Password field
                    PremiumTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showValidation = false
                        },
                        label = "Password",
                        placeholder = "Create a password",
                        leadingIcon = Icons.Rounded.Lock,
                        trailingIcon = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        onTrailingIconClick = { passwordVisible = !passwordVisible },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (isFormValid) {
                                    onRegister(name, email, password)
                                } else {
                                    showValidation = true
                                }
                            }
                        ),
                        isError = showValidation && !isPasswordValid,
                        errorMessage = if (showValidation && !isPasswordValid) "Password must be at least 6 characters" else null
                    )
                }

                // Password strength indicator
                if (password.isNotEmpty()) {
                    PasswordStrengthIndicator(password = password)
                }

                // Error message
                authState.error?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Create account button
                    PremiumButton(
                        onClick = {
                            if (isFormValid) {
                                onRegister(name, email, password)
                            } else {
                                showValidation = true
                            }
                        },
                        text = "Create Account",
                        isPrimary = true,
                        isLoading = authState.isLoading,
                        enabled = !authState.isLoading
                    )

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Text(
                            text = "or",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                    }

                    // Google sign up
                    PremiumSocialButton(
                        onClick = onGoogleLogin,
                        text = "Sign up with Google",
                        icon = R.drawable.google_icon
                    )
                }

                // Switch to login
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    TextButton(
                        onClick = onSwitchToLogin,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign in",
                            color = Color(0xFF60A5FA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordScreen(
    authState: AuthState,
    onResetPassword: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val isEmailValid = email.contains("@") && email.contains(".")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .systemBarsPadding()
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Reset Password",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Placeholder for symmetry
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Glass card container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title and description
                Column {
                    Text(
                        text = "Forgot password?",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Email field
                PremiumTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        showValidation = false
                    },
                    label = "Email address",
                    placeholder = "Enter your email",
                    leadingIcon = Icons.Rounded.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (isEmailValid) {
                                onResetPassword(email)
                            } else {
                                showValidation = true
                            }
                        }
                    ),
                    isError = showValidation && !isEmailValid,
                    errorMessage = if (showValidation && !isEmailValid) "Please enter a valid email" else null
                )

                // Success message
                authState.successMessage?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF059669).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = message,
                            color = Color(0xFF6EE7B7),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Error message
                authState.error?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Send reset link button
                PremiumButton(
                    onClick = {
                        if (isEmailValid) {
                            onResetPassword(email)
                        } else {
                            showValidation = true
                        }
                    },
                    text = "Send Reset Link",
                    isPrimary = true,
                    isLoading = authState.isLoading,
                    enabled = !authState.isLoading
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntegratedOnboardingScreen(
    onComplete: (Agent) -> Unit,
    onSkip: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageAuth(
                title = "Welcome Back to GuideLens",
                description = "Your AI-powered assistant for cooking, crafting, DIY projects, and more. Let's catch up on what's new!",
                icon = Icons.Rounded.AutoAwesome,
                primaryColor = Color(0xFF3B82F6),
                secondaryColor = Color(0xFF1E40AF)
            ),
            OnboardingPageAuth(
                title = "Enhanced Voice & Video",
                description = "Experience improved real-time conversations with better voice recognition and video guidance",
                icon = Icons.Rounded.VideoCall,
                primaryColor = Color(0xFF10B981),
                secondaryColor = Color(0xFF059669)
            ),
            OnboardingPageAuth(
                title = "Smarter Visual Analysis",
                description = "Your AI now understands images better than ever, providing more accurate guidance and tips",
                icon = Icons.Rounded.RemoveRedEye,
                primaryColor = Color(0xFF8B5CF6),
                secondaryColor = Color(0xFF7C3AED)
            ),
            OnboardingPageAuth(
                title = "New Features & Updates",
                description = "Discover improved session management, persistent timers, and enhanced cooking guidance",
                icon = Icons.Rounded.NewReleases,
                primaryColor = Color(0xFFFF6B35),
                secondaryColor = Color(0xFFE55100)
            ),
            OnboardingPageAuth(
                title = "Choose Your Assistant",
                description = "Select or update your preferred AI companion to personalize your experience",
                icon = Icons.Rounded.Person,
                primaryColor = Color(0xFFF59E0B),
                secondaryColor = Color(0xFFEF4444)
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedAgent by remember { mutableStateOf<Agent?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F23),
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / pages.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = pages[pagerState.currentPage].primaryColor,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        // Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContentAuth(
                page = pages[page],
                isAgentSelectionPage = page == pages.size - 1,
                selectedAgent = selectedAgent,
                onAgentSelect = { agent: Agent -> selectedAgent = agent }
            )
        }

        // Bottom navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "indicator_width"
                    )

                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .background(
                                color = if (isSelected) {
                                    pages[pagerState.currentPage].primaryColor
                                } else {
                                    Color.White.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button
                TextButton(onClick = onSkip) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }

                // Next/Complete button
                if (pagerState.currentPage < pages.size - 1) {
                    PremiumButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        text = "Continue",
                        isPrimary = true,
                        icon = Icons.Rounded.ArrowForward,
                        modifier = Modifier.width(140.dp)
                    )
                } else {
                    PremiumButton(
                        onClick = {
                            selectedAgent?.let { agent ->
                                onComplete(agent)
                            }
                        },
                        text = "Get Started",
                        isPrimary = true,
                        icon = Icons.Rounded.RocketLaunch,
                        enabled = selectedAgent != null,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }
    }
}

// Onboarding page data
data class OnboardingPageAuth(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color
)

@Composable
private fun OnboardingPageContentAuth(
    page: OnboardingPageAuth,
    isAgentSelectionPage: Boolean,
    selectedAgent: Agent?,
    onAgentSelect: (Agent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icon with animated background
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            page.primaryColor.copy(alpha = 0.2f),
                            page.secondaryColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = page.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = page.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Agent selection for the last page
        if (isAgentSelectionPage) {
            AgentSelectionGrid(
                agents = AvailableAgents.getAll(),
                selectedAgent = selectedAgent,
                onAgentSelect = onAgentSelect
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AgentSelectionGrid(
    agents: List<Agent>,
    selectedAgent: Agent?,
    onAgentSelect: (Agent) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(agents) { agent ->
            AgentCardAuth(
                agent = agent,
                isSelected = selectedAgent?.id == agent.id,
                onClick = { onAgentSelect(agent) }
            )
        }
    }
}

@Composable
private fun AgentCardAuth(
    agent: Agent,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "agent_card_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            agent.primaryColor.copy(alpha = 0.2f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                agent.primaryColor
            } else {
                Color.White.copy(alpha = 0.2f)
            }
        ),
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Agent icon with color background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = agent.primaryColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    agent.icon,
                    contentDescription = null,
                    tint = agent.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Agent name
            Text(
                text = agent.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Agent features
            Text(
                text = agent.features.take(2).joinToString(" • ") { it.toString() },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f)
                )
            },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    IconButton(onClick = onTrailingIconClick ?: {}) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            isError = isError,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color(0xFFEF4444) else Color(0xFF60A5FA),
                unfocusedBorderColor = if (isError) Color(0xFFEF4444) else Color.White.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedLabelColor = if (isError) Color(0xFFEF4444) else Color(0xFF60A5FA),
                unfocusedLabelColor = if (isError) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                cursorColor = Color(0xFF60A5FA),
                errorBorderColor = Color(0xFFEF4444),
                errorLabelColor = Color(0xFFEF4444)
            ),
            shape = RoundedCornerShape(16.dp)
        )

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PremiumButton(
    onClick: () -> Unit,
    text: String,
    isPrimary: Boolean,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) {
                Color(0xFF4F46E5)
            } else {
                Color.Transparent
            },
            contentColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.1f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        border = if (!isPrimary) {
            BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        } else null,
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPrimary) 4.dp else 0.dp,
            pressedElevation = if (isPrimary) 2.dp else 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                icon?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumSocialButton(
    onClick: () -> Unit,
    text: String,
    icon: Int
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "social_button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(password: String) {
    val strength = calculatePasswordStrength(password)
    val strengthText = when (strength) {
        0 -> "Very Weak"
        1 -> "Weak"
        2 -> "Fair"
        3 -> "Good"
        4 -> "Strong"
        else -> "Very Strong"
    }

    val strengthColor = when (strength) {
        0, 1 -> Color(0xFFEF4444)
        2 -> Color(0xFFF59E0B)
        3 -> Color(0xFF10B981)
        else -> Color(0xFF059669)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index <= strength) strengthColor else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Text(
            text = "Password strength: $strengthText",
            color = strengthColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun calculatePasswordStrength(password: String): Int {
    var score = 0

    if (password.length >= 8) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return score
}