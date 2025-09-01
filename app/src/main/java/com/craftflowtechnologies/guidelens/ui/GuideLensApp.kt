package com.craftflowtechnologies.guidelens.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.craftflowtechnologies.guidelens.ai.ArtifactContextManager
import com.craftflowtechnologies.guidelens.api.EnhancedGeminiClient
import com.craftflowtechnologies.guidelens.api.EnhancedResponse
import com.craftflowtechnologies.guidelens.auth.SupabaseAuthManager
import com.craftflowtechnologies.guidelens.chat.ChatSessionManager
import com.craftflowtechnologies.guidelens.ai.OfflineModelManager
import com.craftflowtechnologies.guidelens.cooking.Recipe
import com.craftflowtechnologies.guidelens.cooking.InteractiveCookingOverlay
import com.craftflowtechnologies.guidelens.cooking.EnhancedCookingSessionManager
import com.craftflowtechnologies.guidelens.storage.*
import com.craftflowtechnologies.guidelens.media.ArtifactImageGenerator
import com.craftflowtechnologies.guidelens.ai.ProgressAnalysisSystem
import com.craftflowtechnologies.guidelens.credits.CreditsManager
import com.craftflowtechnologies.guidelens.api.XAIImageClient
import com.craftflowtechnologies.guidelens.cooking.CookingSessionManager
import com.craftflowtechnologies.guidelens.cooking.EnhancedCookingSession
import com.craftflowtechnologies.guidelens.localization.GeneralLocalizationManager
import com.craftflowtechnologies.guidelens.storage.ArtifactDatabase
import com.craftflowtechnologies.guidelens.media.VoiceVideoModeManager
import com.craftflowtechnologies.guidelens.permissions.RequestVoiceAndVideoPermissions
import com.craftflowtechnologies.guidelens.personalization.ToneManager
import com.craftflowtechnologies.guidelens.personalization.ZambianLocalizationManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Helper function to convert a Recipe to an Artifact for backward compatibility
 */
private fun Recipe.toArtifact(userId: String): Artifact {
    return Artifact(
        id = this.id,
        type = com.craftflowtechnologies.guidelens.storage.ArtifactType.RECIPE,
        title = this.title,
        description = this.description,
        agentType = "cooking",
        userId = userId,
        contentData = ArtifactContent.RecipeContent(recipe = this),
        stageImages = emptyList(), // Will be populated during session
        mainImage = null, // Will be populated during session
        tags = listOf("recipe", this.cuisine ?: "international"),
        difficulty = this.difficulty,
        estimatedDuration = this.prepTime + this.cookTime,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        generationMetadata = GenerationMetadata(
            model = "legacy_recipe",
            prompt = "Converted from legacy Recipe format",
            generationTime = System.currentTimeMillis(),
            tokensUsed = 0,
            estimatedCost = 0.0f,
            qualityScore = 1.0f
        ),
        currentProgress = ArtifactProgress(
            currentStageIndex = 0,
            completedStages = emptySet(),
            sessionStartTime = System.currentTimeMillis(),
            sessionPaused = false,
            userNotes = emptyList(),
            stageStates = emptyMap(),
            contextData = emptyMap()
        )
    )
}

@Composable
fun GuideLensApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeController = remember { ThemeController() }

    // Initialize Supabase auth manager
    val authManager = remember { SupabaseAuthManager(context, coroutineScope) }
    val authState by authManager.authState.collectAsStateWithLifecycle()
    var showOnboarding by remember { mutableStateOf(false) }

    // Get current user ID from Supabase
    val currentUserId = authState.user?.id

    // Initialize user data manager  
    val userDataManager = remember { UserDataManager(context) }
    
    // Initialize chat session manager
    val sessionManager = remember { ChatSessionManager(context, userDataManager) }
    val currentSession by sessionManager.currentSession.collectAsStateWithLifecycle()

    // Initialize offline model manager and enhanced Gemini client
    val offlineModelManager = remember { OfflineModelManager(context) }
    val enhancedGeminiClient = remember { EnhancedGeminiClient(context, offlineModelManager) }
    val toneManager = remember { ToneManager() }
    val localizationManager = remember { GeneralLocalizationManager() }
    val zambianLocalizationManager = remember { ZambianLocalizationManager(context) }
    val settingsManager = remember { SettingsManager(userDataManager = userDataManager, zambianLocalizationManager = zambianLocalizationManager,
        context = context
    ) }

    // Apply Zambian localization by default
    LaunchedEffect(Unit) {
        zambianLocalizationManager.setLanguage(ZambianLocalizationManager.ZambianLanguage.ENGLISH)
        zambianLocalizationManager.setRegion(ZambianLocalizationManager.ZambianRegion.LUSAKA)
    }

    // Initialize artifact system dependencies
    val artifactRepository = remember {
        ArtifactRepository(context, ArtifactDatabase.getDatabase(context))
    }
    val xaiImageClient = remember { XAIImageClient(context) }
    val artifactImageGenerator = remember {
        ArtifactImageGenerator(xaiImageClient, artifactRepository)
    }
    val progressAnalysisSystem = remember {
        ProgressAnalysisSystem(enhancedGeminiClient, artifactRepository)
    }
    val creditsManager = remember {
        CreditsManager(artifactRepository)
    }
    val contextManager = remember {
        ArtifactContextManager(artifactRepository, enhancedGeminiClient)
    }

    // Initialize enhanced cooking session manager
    val enhancedCookingSessionManager = remember {
        EnhancedCookingSessionManager(
            artifactRepository = artifactRepository,
            contextManager = contextManager,
            progressAnalysisSystem = progressAnalysisSystem,
            imageGenerator = artifactImageGenerator,
            creditsManager = creditsManager,
            coroutineScope = coroutineScope
        )
    }

    // Initialize Gemini Live components
    val geminiLiveClient = remember { com.craftflowtechnologies.guidelens.api.GeminiLiveApiClient() }
    val liveSessionManager: com.craftflowtechnologies.guidelens.api.GeminiLiveSessionManager = remember {
        com.craftflowtechnologies.guidelens.api.GeminiLiveSessionManager(context, geminiLiveClient)
    }
    val currentCookingSession by enhancedCookingSessionManager.currentSession.collectAsStateWithLifecycle()
    val isOnline by offlineModelManager.isOnline.collectAsStateWithLifecycle()

    // Initialize voice/video mode manager
    val voiceVideoManager = remember { VoiceVideoModeManager(context) }
    val voiceVideoState by voiceVideoManager.state.collectAsStateWithLifecycle()
    val transcriptionStateFromManager by voiceVideoManager.transcriptionState.collectAsStateWithLifecycle()

    // Authentication state
    val isAuthenticated = authState.isLoggedIn && !showOnboarding

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
    }

    var userInput by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var selectedAgent by remember { mutableStateOf(AGENTS[0]) }
    var showAgentSelector by remember { mutableStateOf(false) }
    var showVideoCall by remember { mutableStateOf(false) }

    // Video call state management
    var videoCallState by remember {
        mutableStateOf(
            VideoCallState(
                isUserMuted = false,
                isVideoEnabled = true,
                isSessionActive = false,
                isRecording = false,
                callDuration = "00:00"
            )
        )
    }
    var isLiveSessionActive by remember { mutableStateOf(false) }
    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var transcriptionState by remember { mutableStateOf(TranscriptionState("", false)) }
    val chatMessages = currentSession?.messages ?: emptyList()
    var isConnected by remember { mutableStateOf(true) }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Connected") }
    var showSideMenu by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var selectedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCookingSession by remember { mutableStateOf(false) }
    var showApiTestScreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPersonalization by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showArtifacts by remember { mutableStateOf(false) }
    var showNotificationSettings by remember { mutableStateOf(false) }
    var showUserProfileSettings by remember { mutableStateOf(false) }

    // Handle agent change - create new session for new agent
    LaunchedEffect(selectedAgent, currentUserId) {
        if (currentUserId != null && authState.isLoggedIn) {
            if (currentSession?.agentId != selectedAgent.id) {
                val sessionName = "New ${selectedAgent.name} Chat"
                sessionManager.createNewSession(
                    name = sessionName,
                    agentId = selectedAgent.id,
                    userId = currentUserId
                )
            }
            enhancedGeminiClient.clearHistory()
        }
    }

    // Check permissions when voice or video mode is enabled
    LaunchedEffect(isVoiceMode, isVideoMode) {
        if ((isVoiceMode || isVideoMode) && !hasPermissions) {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }
    }

    // Cleanup VoiceVideoManager when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            voiceVideoManager.cleanup()
        }
    }

    GuideLensTheme(
        themeController = themeController
    ) {
        if (!isAuthenticated) {
            AuthenticationScreen(
                authState = AuthState(
                    isLoading = authState.isLoading,
                    isLoggedIn = authState.isLoggedIn,
                    error = authState.error,
                    successMessage = authState.successMessage,
                    user = authState.user,
                    showOnboarding = showOnboarding,
                    currentStep = AuthStep.WELCOME
                ),
                onLogin = { email, password ->
                    coroutineScope.launch {
                        authManager.login(email, password)
                    }
                },
                onRegister = { name, email, password ->
                    coroutineScope.launch {
                        authManager.register(name, email, password)
                    }
                },
                onGoogleLogin = {
                    coroutineScope.launch {
                        val credentialManager = CredentialManager.create(context)
                        // Generate a nonce and hash it with sha-256
                        // Providing a nonce is optional but recommended
                        val rawNonce = UUID.randomUUID().toString() // Generate a random String. UUID should be sufficient, but can also be any other random string.
                        val bytes = rawNonce.toString().toByteArray()
                        val md = MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(bytes)
                        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) } // Hashed nonce to be passed to Google sign-in
                        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId("WEB_GOOGLE_CLIENT_ID")
                            .setNonce(hashedNonce) // Provide the nonce if you have one
                            .build()
                        val request: GetCredentialRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        coroutineScope.launch {
                            try {
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = context,
                                )
                                // Use our SupabaseAuthManager Google OAuth instead
                                authManager.signInWithGoogle()
                                // Handle successful sign-in
                            } catch (e: Exception) {
                                // Handle authentication errors
                                // Handle RestException thrown by Supabase
                            } catch (e: Exception) {
                                // Handle unknown exceptions
                            }
                        }
                    }
                },
                onForgotPassword = { email ->
                    coroutineScope.launch {
                        authManager.resetPassword(email)
                    }
                },
                onOnboardingComplete = { agent ->
                    coroutineScope.launch {
                        selectedAgent = agent
                        authManager.updateUserProfile(
                            onboardingCompleted = true,
                            preferredAgentId = agent.id
                        )
                        if (authState.user != null) {
                            val initialSession = sessionManager.createNewSession(
                                name = "Welcome to ${agent.name}",
                                agentId = agent.id,
                                userId = authState.user!!.id
                            )
                        }
                        showOnboarding = false
                        if (!hasPermissions) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA
                                )
                            )
                        }
                    }
                },
                onSkipOnboarding = {
                    coroutineScope.launch {
                        authManager.updateUserProfile(
                            onboardingCompleted = true,
                            preferredAgentId = selectedAgent.id
                        )
                        showOnboarding = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showApiTestScreen) {
                    ApiTestScreen(
                        onBackPressed = { showApiTestScreen = false },
                        isDarkTheme = themeController.isDarkTheme,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (showSettings) {
                    MainSettingsScreen(
                        currentUser = authState.user,
                        zambianLocalizationManager = zambianLocalizationManager,
                        userDataManager = userDataManager,
                        onNavigateBack = { showSettings = false },
                        onNavigateToProfile = {
                            showUserProfileSettings = true
                            showSettings = false
                        },
                        onNavigateToPersonalization = {
                            showPersonalization = true
                            showSettings = false
                                                      },
                        onNavigateToPrivacy = {
                            showPrivacy = true
                            showSettings = false
                                              },
                        onNavigateToNotifications = {
                            showNotificationSettings = true
                            showSettings = false
                                                    },
                        onNavigateToApiTest = { showApiTestScreen = true },
                        onThemeToggle = { themeController.toggleTheme() },
                        onSignOut = {
                            coroutineScope.launch {
                                authManager.signOut()
                            }
                        },
                        isDarkTheme = themeController.isDarkTheme
                    )
                } else if (showPersonalization) {
                    PersonalizationSettingsScreen(
                        zambianLocalizationManager = zambianLocalizationManager,
//                        toneManager = toneManager,
//                        localizationManager = localizationManager,
                        onNavigateBack = {
                            showPersonalization = false
                            showSettings = true
                                         },
                        isDarkTheme = themeController.isDarkTheme
                    )
                } else if (showPrivacy) {
                    PrivacySettingsScreen(
                        userDataManager = userDataManager,
                        onBack = {
                            showPrivacy = false
                            showSettings = true
                        }
                    )
                }
                else if (showNotificationSettings) {
                    NotificationSettingsScreen(
                        settingsManager = settingsManager,
                        zambianLocalizationManager = zambianLocalizationManager,
                        onNavigateBack = {
                            showNotificationSettings = false
                            showSettings = true
                                         },
                        isDarkTheme = themeController.isDarkTheme
                    )
                }
                    else if(showUserProfileSettings) {
                        UserProfileSettingsScreen(
                            currentUser = authState.user,
                            zambianLocalizationManager = zambianLocalizationManager,
                            onNavigateBack = {
                                showUserProfileSettings = false
                                showSettings = true
                                             },
                            onSignOut = {
                                coroutineScope.launch {
                                    authManager.signOut()
                                }
                            },
                            onDeleteAccount = {/* Implement */},
                            onUpdateProfile = { /* Implement */ },
                            onPrivacySettingsClick = { showPrivacy = true },
                            onDataExportClick = { /* Implement */ },
                            onNotificationSettingsClick = { showNotificationSettings = true },
                            isDarkTheme = themeController.isDarkTheme


                        )
                }
                else if (showArtifacts) {
                    ArtifactsScreen(
                        currentUser = authState.user,
                        artifactRepository = artifactRepository,
                        chatSessionManager = sessionManager,
                        cookingSessionManager = enhancedCookingSessionManager,
                        imageGenerator = artifactImageGenerator,
                        onNavigateBack = { showArtifacts = false },
                        onSendMessage = { message -> /* Handle message sending from artifacts screen */ },
                        isDarkTheme = themeController.isDarkTheme
                    )
                }
                else {
                    var showPermissionDialog by remember { mutableStateOf(false) }
                    var permissionsNeeded by remember { mutableStateOf<List<String>>(emptyList()) }

                    if ((isVoiceMode || isVideoMode) && !hasPermissions) {
                        RequestVoiceAndVideoPermissions(
                            onPermissionsGranted = {
                                hasPermissions = true
                                showPermissionDialog = false
                            },
                            onPermissionsDenied = { deniedPermissions ->
                                permissionsNeeded = deniedPermissions
                                showPermissionDialog = true
                                isVoiceMode = false
                                isVideoMode = false
                                showVideoCall = false
                                voiceVideoManager.switchToTextMode()
                            }
                        )
                    }

                    if (showPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            title = { Text("Permissions Required") },
                            text = {
                                Text(
                                    "GuideLens needs ${permissionsNeeded.joinToString(" and ")} " +
                                            "permissions to provide voice and video guidance. " +
                                            "Please grant these permissions in your device settings."
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showPermissionDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    MainAppContent(
                        themeController = themeController,
                        userInput = userInput,
                        onInputChange = { userInput = it },
                        selectedImages = selectedImages,
                        onImageSelectionChange = { selectedImages = it },
                        isVoiceMode = isVoiceMode,
                        isVideoMode = isVideoMode,
                        selectedAgent = selectedAgent,
                        showAgentSelector = showAgentSelector,
                        showVideoCall = showVideoCall,
                        chatMessages = chatMessages,
                        isConnected = isConnected,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        connectionStatus = connectionStatus,
                        showSideMenu = showSideMenu,
                        isThinking = isThinking,
                        showCookingSession = showCookingSession,
                        currentCookingSession = currentCookingSession,
                        enhancedCookingSessionManager = enhancedCookingSessionManager,
                        artifactImageGenerator = artifactImageGenerator,
                        authState = authState,
                        coroutineScope = coroutineScope,
                        liveSessionManager = liveSessionManager,
                        videoCallState = videoCallState,
                        isLiveSessionActive = isLiveSessionActive,
                        zambianLocalizationManager = zambianLocalizationManager,
                        onVoiceToggle = {
                            coroutineScope.launch {
                                if (voiceVideoManager.isInVoiceMode()) {
                                    voiceVideoManager.switchToTextMode()
                                    isVoiceMode = false
                                } else {
                                    val success =
                                        voiceVideoManager.switchToVoiceMode(selectedAgent.id)
                                    if (success) {
                                        isVoiceMode = true
                                        isVideoMode = false
                                        showVideoCall = false
                                    }
                                }
                            }
                        },
                        onVideoToggle = {
                            coroutineScope.launch {
                                if (voiceVideoManager.isInVideoMode()) {
                                    voiceVideoManager.switchToTextMode()
                                    isVideoMode = false
                                    showVideoCall = false
                                } else {
                                    val success =
                                        voiceVideoManager.switchToVideoMode(selectedAgent.id)
                                    if (success) {
                                        isVideoMode = true
                                        isVoiceMode = false
                                        showVideoCall = true
                                    }
                                }
                            }
                        },
                        onAgentClick = { showAgentSelector = true },
                        onMenuClick = { showSideMenu = true },
                        onCloseCookingSession = { showCookingSession = false },
                        onSendMessage = { messageText ->
                            if (messageText.isNotBlank() && currentUserId != null && authState.isLoggedIn) {
                                val session = currentSession ?: sessionManager.createNewSession(
                                    name = sessionManager.generateSessionName(
                                        messageText,
                                        selectedAgent.id
                                    ),
                                    agentId = selectedAgent.id,
                                    userId = currentUserId
                                )

                                val newMessage = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    text = messageText,
                                    isFromUser = true,
                                    timestamp = SimpleDateFormat(
                                        "HH:mm",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    images = selectedImages,
                                    hasAttachment = selectedImages.isNotEmpty()
                                )

                                sessionManager.addMessageToCurrentSession(newMessage)

                                val messageToSend = messageText
                                val imagesToSend = selectedImages
                                userInput = ""
                                selectedImages = emptyList()
                                isThinking = true

                                coroutineScope.launch {
                                    try {
                                        delay(500) // Simulate thinking delay for UX

                                        if (!isOnline && session.isOffline != true) {
                                            sessionManager.addMessageToCurrentSession(
                                                ChatMessage(
                                                    text = "🔷 Switching to offline mode...",
                                                    isFromUser = false,
                                                    timestamp = SimpleDateFormat(
                                                        "HH:mm",
                                                        Locale.getDefault()
                                                    ).format(Date())
                                                )
                                            )
                                        }

                                        // Generate personalization context for AI
                                        val personalizationContext = sessionManager.generateEnhancedAIContext(authState.user)
                                        val enhancedPrompt = if (personalizationContext.isNotBlank()) {
                                            "Context: $personalizationContext\n\nUser message: $messageToSend"
                                        } else {
                                            messageToSend
                                        }
                                        
                                        val result = enhancedGeminiClient.generateContent(
                                            prompt = enhancedPrompt,
                                            agentType = selectedAgent.id,
                                            includeHistory = true,
                                            forceOffline = !isOnline,
                                            images = imagesToSend
                                        )
                                        isThinking = false

                                        val response = result.fold(
                                            onSuccess = { it },
                                            onFailure = { throwable ->
                                                EnhancedResponse("❌ Error: ${throwable.message ?: "Unknown error occurred"}")
                                            }
                                        )

                                        val botResponse = ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            text = response.text,
                                            isFromUser = false,
                                            timestamp = SimpleDateFormat(
                                                "HH:mm",
                                                Locale.getDefault()
                                            ).format(Date()),
                                            generatedImage = response.image
                                        )

                                        sessionManager.addMessageToCurrentSession(botResponse)

                                        if (session.messages.isEmpty()) {
                                            sessionManager.renameSession(
                                                session.id,
                                                sessionManager.generateSessionName(
                                                    messageToSend,
                                                    selectedAgent.id
                                                )
                                            )
                                        }
                                    } catch (e: Exception) {
                                        isThinking = false
                                        val errorMessage = ChatMessage(
                                            text = "❌ App Crash Prevented: ${e.message ?: "Unexpected error"}",
                                            isFromUser = false,
                                            timestamp = SimpleDateFormat(
                                                "HH:mm",
                                                Locale.getDefault()
                                            ).format(Date())
                                        )
                                        sessionManager.addMessageToCurrentSession(errorMessage)
                                    }
                                }
                            }
                        },
                        onVoiceRecord = {
                            if (voiceVideoManager.isInVoiceMode() || voiceVideoManager.isInVideoMode()) {
                                val newListeningState = voiceVideoManager.toggleMute()
                                isListening = newListeningState
                            }
                        },
                        onAgentSelected = { agent ->
                            selectedAgent = agent
                            showAgentSelector = false
                            enhancedGeminiClient.clearHistory()
                        },
                        onDismissAgentSelector = { showAgentSelector = false },
                        onCloseVideoCall = {
                            showVideoCall = false
                            isVideoMode = false
                        },
                        onCloseSideMenu = { showSideMenu = false },
                        onSignOut = {
                            coroutineScope.launch {
                                authManager.signOut()
                            }
                        },
                        transcriptionState = transcriptionStateFromManager,
                        cookingSessionManager = null,
                        onStartCookingSession = { recipe ->
                            coroutineScope.launch {
                                if (currentUserId != null) {
                                    val artifact = recipe.toArtifact(currentUserId)
                                    val result = enhancedCookingSessionManager.startEnhancedSession(
                                        userId = currentUserId,
                                        artifact = artifact
                                    )
                                    if (result.isSuccess) {
                                        showCookingSession = true
                                    }
                                }
                            }
                        },
                        onFeatureClick = { feature ->
                            when (feature) {
                                is CookingFeature -> {
                                    userInput = feature.action
                                    if (userInput.isNotBlank() && currentUserId != null && authState.isLoggedIn) {
                                        val session = currentSession ?: sessionManager.createNewSession(
                                            name = sessionManager.generateSessionName(
                                                userInput,
                                                selectedAgent.id
                                            ),
                                            agentId = selectedAgent.id,
                                            userId = currentUserId
                                        )

                                        val newMessage = ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            text = userInput,
                                            isFromUser = true,
                                            timestamp = SimpleDateFormat(
                                                "HH:mm",
                                                Locale.getDefault()
                                            ).format(Date()),
                                            images = selectedImages,
                                            hasAttachment = selectedImages.isNotEmpty()
                                        )

                                        sessionManager.addMessageToCurrentSession(newMessage)

                                        val messageToSend = userInput
                                        val imagesToSend = selectedImages
                                        userInput = ""
                                        selectedImages = emptyList()
                                        isThinking = true

                                        coroutineScope.launch {
                                            try {
                                                delay(300)

                                                val result = enhancedGeminiClient.generateContent(
                                                    prompt = messageToSend,
                                                    agentType = selectedAgent.id,
                                                    includeHistory = true,
                                                    forceOffline = !isOnline,
                                                    images = imagesToSend
                                                )
                                                isThinking = false

                                                val response = result.fold(
                                                    onSuccess = { it },
                                                    onFailure = { throwable ->
                                                        EnhancedResponse("❌ Error: ${throwable.message ?: "Unknown error occurred"}")
                                                    }
                                                )

                                                val botResponse = ChatMessage(
                                                    id = UUID.randomUUID().toString(),
                                                    text = response.text,
                                                    isFromUser = false,
                                                    timestamp = SimpleDateFormat(
                                                        "HH:mm",
                                                        Locale.getDefault()
                                                    ).format(Date()),
                                                    generatedImage = response.image
                                                )

                                                sessionManager.addMessageToCurrentSession(botResponse)
                                            } catch (e: Exception) {
                                                isThinking = false
                                                val errorMessage = ChatMessage(
                                                    text = "❌ Error: ${e.message ?: "Unexpected error"}",
                                                    isFromUser = false,
                                                    timestamp = SimpleDateFormat(
                                                        "HH:mm",
                                                        Locale.getDefault()
                                                    ).format(Date())
                                                )
                                                sessionManager.addMessageToCurrentSession(errorMessage)
                                            }
                                        }
                                    }
                                }
                                is CraftingProject -> {
                                    userInput = "Help me with ${feature.title}: ${feature.difficulty} level project"
                                }
                                is DIYCategory -> {
                                    userInput = "I need help with ${feature.title}: ${feature.description}"
                                }
                                is FriendshipTool -> {
                                    userInput = feature.description
                                }
                                else -> {
                                    val featureText = when {
                                        feature.toString().contains("name") -> {
                                            try {
                                                val nameField = feature::class.java.getDeclaredField("name")
                                                nameField.isAccessible = true
                                                "Tell me about ${nameField.get(feature)}"
                                            } catch (e: Exception) {
                                                "Tell me about this: $feature"
                                            }
                                        }
                                        else -> "Help me with: $feature"
                                    }
                                    userInput = featureText
                                }
                            }
                        },
                        onVideoCallStateChange = { newState ->
                            videoCallState = newState
                        },
                        onLiveSessionActiveChange = { active ->
                            isLiveSessionActive = active
                        }
                    )

                    ChatSessionSidebar(
                        isVisible = showSideMenu,
                        selectedAgent = selectedAgent,
                        currentUser = authState.user,
                        sessionManager = sessionManager,
                        toneManager = toneManager,
                        localizationManager = localizationManager,
                        zambianLocalizationManager = zambianLocalizationManager,
                        onClose = { showSideMenu = false },
                        onNewChat = {
                            if (currentUserId != null) {
                                val sessionName = "New ${selectedAgent.name} Chat"
                                sessionManager.createNewSession(
                                    name = sessionName,
                                    agentId = selectedAgent.id,
                                    userId = currentUserId
                                )
                                enhancedGeminiClient.clearHistory()
                            }
                            showSideMenu = false
                        },
                        onSessionClick = { sessionId ->
                            sessionManager.switchToSession(sessionId)
                            enhancedGeminiClient.clearHistory()
                            showSideMenu = false
                        },
                        onSessionDelete = { sessionId ->
                            sessionManager.deleteSession(sessionId)
                        },
                        onSessionRename = { sessionId, newName ->
                            sessionManager.renameSession(sessionId, newName)
                        },
                        onThemeToggle = {
                            themeController.toggleTheme()
                        },
                        onSignOut = {
                            coroutineScope.launch {
                                authManager.signOut()
                            }
                            showSideMenu = false
                        },
                        onApiTest = {
                            showApiTestScreen = true
                            showSideMenu = false
                        },
                        isDarkTheme = themeController.isDarkTheme,
                        onSettingsOpen = {
                            showSettings = true
                            showSideMenu = false
                        },
                        onArtifactsOpen = {
                            showArtifacts = true
                            showSideMenu = false
                        }
                    )
                }
            }
        }
    }
}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ArtifactsScreen(
//    artifactRepository: ArtifactRepository,
//    userId: String,
//    onBack: () -> Unit,
//    isDarkTheme: Boolean
//) {
//    val artifacts by artifactRepository.getUserArtifacts(userId).collectAsStateWithLifecycle(emptyList())
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Artifacts") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        LazyColumn(
//            modifier = Modifier.padding(padding)
//        ) {
//            items(artifacts) { artifact ->
//                Text(artifact.title)
//            }
//        }
//    }
//}
@Composable
private fun MainAppContent(
    themeController: ThemeController,
    userInput: String,
    onInputChange: (String) -> Unit,
    selectedImages: List<String>,
    onImageSelectionChange: (List<String>) -> Unit,
    isVoiceMode: Boolean,
    isVideoMode: Boolean,
    selectedAgent: Agent,
    showAgentSelector: Boolean,
    showVideoCall: Boolean,
    chatMessages: List<ChatMessage>,
    isConnected: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    connectionStatus: String,
    showSideMenu: Boolean,
    isThinking: Boolean,
    showCookingSession: Boolean,
    currentCookingSession: EnhancedCookingSession?,
    enhancedCookingSessionManager: EnhancedCookingSessionManager,
    artifactImageGenerator: ArtifactImageGenerator,
    authState: AuthState,
    coroutineScope: CoroutineScope,
    liveSessionManager: com.craftflowtechnologies.guidelens.api.GeminiLiveSessionManager,
    videoCallState: VideoCallState,
    isLiveSessionActive: Boolean,
    zambianLocalizationManager: ZambianLocalizationManager,
    onVoiceToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onAgentClick: () -> Unit,
    onMenuClick: () -> Unit,
    onCloseCookingSession: () -> Unit,
    onSendMessage: (String) -> Unit,
    onVoiceRecord: () -> Unit,
    onAgentSelected: (Agent) -> Unit,
    onDismissAgentSelector: () -> Unit,
    onCloseVideoCall: () -> Unit,
    onCloseSideMenu: () -> Unit,
    onSignOut: () -> Unit,
    transcriptionState: TranscriptionState,
    cookingSessionManager: CookingSessionManager?,
    onStartCookingSession: (Recipe) -> Unit,
    onFeatureClick: (Any) -> Unit,
    onVideoCallStateChange: (VideoCallState) -> Unit,
    onLiveSessionActiveChange: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val scrollState = rememberLazyListState()
    val ime = WindowInsets.ime

    val isKeyboardVisible by remember {
        derivedStateOf {
            ime.getBottom(density) > 0
        }
    }

    val isScrolled by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0
        }
    }

    val stickyOffset by remember(scrollState) {
        derivedStateOf {
            val visibleItems = scrollState.layoutInfo.visibleItemsInfo
            val stickyHeaderInfo = visibleItems.find {
                it.contentType == "sticky"
            }

            if (stickyHeaderInfo != null && stickyHeaderInfo.offset < statusBarTop) {
                statusBarTop - stickyHeaderInfo.offset
            } else {
                0
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (themeController.isDarkTheme) {
                        listOf(
                            Color(0xFF0F0F1E),
                            Color(0xFF1A1A2E)
                        )
                    } else {
                        listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE8ECF1)
                        )
                    }
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(100.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "background")
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 100f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offsetX"
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = offsetX.dp, y = 100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                selectedAgent.primaryColor.copy(alpha = 0.033f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Main content with padding for TopBar
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it }
                ) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopBar(
                    selectedAgent = selectedAgent,
                    isVoiceMode = isVoiceMode,
                    isVideoMode = isVideoMode,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isConnected = isConnected,
                    connectionStatus = connectionStatus,
                    onAgentClick = onAgentClick,
                    onVoiceToggle = onVoiceToggle,
                    onVideoToggle = onVideoToggle,
                    onMenuClick = onMenuClick,
                    isDarkTheme = themeController.isDarkTheme,
                    isScrolled = isScrolled,
                    stickyOffset = stickyOffset,
                    zambianLocalizationManager = zambianLocalizationManager
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp) // Space for sticky TopBar
                    .imePadding()
            ) {
                EnhancedChatMessagesArea(
                    messages = chatMessages,
                    selectedAgent = selectedAgent,
                    isThinking = isThinking,
                    modifier = Modifier.weight(1f),
                    isVoiceMode = isVoiceMode,
                    isDarkTheme = themeController.isDarkTheme,
                    onFeatureClick = onFeatureClick,
                    scrollState = scrollState,
                    cookingSessionManager = cookingSessionManager,
                    onStartCookingSession = onStartCookingSession
                )

                AnimatedVisibility(
                    visible = !isVoiceMode && !isVideoMode,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it }
                    ) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = if (themeController.isDarkTheme) {
                            Color(0xFF2A2A3E).copy(alpha = 0.96f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        },
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (themeController.isDarkTheme) Color(0xFF2A2A3E).copy(
                                                alpha = 0.96f
                                            ) else Color.White.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            EnhancedInputArea(
                                userInput = userInput,
                                onInputChange = onInputChange,
                                onSendMessage = { onSendMessage(userInput) },
                                isConnected = isConnected,
                                selectedAgent = selectedAgent,
                                isDarkTheme = themeController.isDarkTheme,
                                isThinking = isThinking,
                                onImageSelected = { images ->
                                    onImageSelectionChange(images)
                                }
                            )
                        }
                    }
                }


            }



            if (showAgentSelector) {
                AgentSelectorDialog(
                    agents = AGENTS,
                    selectedAgent = selectedAgent,
                    onAgentSelected = onAgentSelected,
                    onDismiss = onDismissAgentSelector
                )
            }

            if (showVideoCall) {
                GeminiVideoCallOverlay(
                    onClose = {
                        onCloseVideoCall()
                        onVideoCallStateChange(videoCallState.copy(isSessionActive = false))
                        onLiveSessionActiveChange(false)
                    },
                    selectedAgent = selectedAgent,
                    onSendMessage = onSendMessage,
                    onStartLiveSession = {
                        onVideoCallStateChange(videoCallState.copy(isSessionActive = true))
                        onLiveSessionActiveChange(true)
                        coroutineScope.launch {
                            liveSessionManager.startLiveSession(selectedAgent.id)
                        }
                    },
                    onStopLiveSession = {
                        onVideoCallStateChange(videoCallState.copy(isSessionActive = false))
                        onLiveSessionActiveChange(false)
                        liveSessionManager.stopLiveSession()
                    },
                    liveSessionManager = liveSessionManager,
                    isLiveSessionActive = isLiveSessionActive,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    voiceActivityLevel = 0.5f,
                    sessionDuration = 0
                )
            }

            if (isVoiceMode && !showVideoCall) {
                GeminiLiveVoiceOverlay(
                    selectedAgent = selectedAgent,
                    onClose = {
                        onVoiceToggle()
                    },
                    onSwitchToVideo = {
                        onVideoToggle()
                    },
                    onSendMessage = onSendMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showCookingSession && currentCookingSession != null) {
                val artifact = try {
                    val artifactField = currentCookingSession.javaClass.getDeclaredField("artifact")
                    artifactField.isAccessible = true
                    artifactField.get(currentCookingSession) as? Artifact
                } catch (e: Exception) {
                    null
                }

                if (artifact != null) {
                    InteractiveCookingOverlay(
                        artifact = artifact,
                        sessionManager = enhancedCookingSessionManager,
                        imageGenerator = artifactImageGenerator,
                        onSendMessage = { message ->
                            onInputChange(message)
                            coroutineScope.launch {
                                delay(50)
                                onSendMessage(message)
                            }
                        },
                        onRequestImage = { prompt, stageIndex ->
                            coroutineScope.launch {
                                try {
                                    enhancedCookingSessionManager.requestSessionImage(
                                        prompt,
                                        stageIndex
                                    )
                                } catch (e: Exception) {
                                    onInputChange(prompt)
                                    delay(50)
                                    onSendMessage(prompt)
                                }
                            }
                        },
                        onCaptureProgress = {
                            val message =
                                "I'd like to capture my cooking progress. Can you help me analyze how I'm doing with this step?"
                            onInputChange(message)
                            coroutineScope.launch {
                                delay(50)
                                onSendMessage(message)
                            }
                        },
                        onBackPressed = onCloseCookingSession,
                        onDismiss = onCloseCookingSession,
                        isKeyboardVisible = isKeyboardVisible,
                        modifier = Modifier.fillMaxSize(),
                        themeController = themeController
                    )
                }
            }
        }
    }
}



/**
 * Generate a personalized welcome message based on the selected agent
 */
private fun getWelcomeMessage(agent: Agent): String {
    return when (agent.id) {
        "chef" -> "Welcome to your culinary journey! I'm Chef, your AI cooking companion. " +
                "I'm here to help you create delicious meals, learn new techniques, and discover amazing recipes. " +
                "What would you like to cook today?"

        "craftsperson" -> "Hello there, creative soul! I'm your Crafting Assistant, ready to help you bring your " +
                "artistic visions to life. Whether you're into DIY projects, handmade crafts, or learning new " +
                "techniques, I've got you covered. What shall we create together?"

        "friend" -> "Hey there! I'm your AI Friend, here to chat, listen, and support you through whatever's on your mind. " +
                "Whether you need someone to talk to, want advice, or just want to have a fun conversation, " +
                "I'm here for you. What's going on today?"

        "general" -> "Hello! I'm your General Assistant, equipped to help you with a wide range of tasks and questions. " +
                "From problem-solving to learning new things, I'm here to make your life easier. " +
                "What can I help you with today?"

        else -> "Welcome to GuideLens! I'm ${agent.name}, your AI assistant. " +
                "I'm excited to help you with whatever you need. How can I assist you today?"
    }
}

// You'll also need to extend your AuthManager to support updating user profiles:
// Add this method to your SupabaseAuthManager class:
