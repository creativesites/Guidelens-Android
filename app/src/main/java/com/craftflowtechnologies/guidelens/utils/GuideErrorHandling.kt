package com.craftflowtechnologies.guidelens.utils

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Enterprise-grade error handling system for GuideLens
 * Provides comprehensive error tracking, user-friendly messaging, and recovery strategies
 */
class GuideErrorManager private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuideErrorManager()
        
        private const val TAG = "GuideErrorManager"
        private const val MAX_ERROR_HISTORY = 100
    }
    
    // Error state management
    private val _currentError = MutableStateFlow<GuideError?>(null)
    val currentError: StateFlow<GuideError?> = _currentError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<GuideError>>(emptyList())
    val errorHistory: StateFlow<List<GuideError>> = _errorHistory.asStateFlow()
    
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()
    
    private val errorQueue = ConcurrentLinkedQueue<GuideError>()
    
    // Error logging and reporting
    fun reportError(
        exception: Throwable,
        context: String = "",
        severity: GuideErrorSeverity = GuideErrorSeverity.ERROR,
        category: GuideErrorCategory = GuideErrorCategory.GENERAL,
        recoveryAction: GuideRecoveryAction? = null
    ) {
        val error = GuideError(
            id = generateErrorId(),
            exception = exception,
            context = context,
            severity = severity,
            category = category,
            timestamp = LocalDateTime.now(),
            recoveryAction = recoveryAction
        )
        
        handleError(error)
    }
    
    fun reportError(
        errorMessage: String,
        context: String = "",
        severity: GuideErrorSeverity = GuideErrorSeverity.WARNING,
        category: GuideErrorCategory = GuideErrorCategory.GENERAL,
        recoveryAction: GuideRecoveryAction? = null
    ) {
        val error = GuideError(
            id = generateErrorId(),
            errorMessage = errorMessage,
            context = context,
            severity = severity,
            category = category,
            timestamp = LocalDateTime.now(),
            recoveryAction = recoveryAction
        )
        
        handleError(error)
    }
    
    private fun handleError(error: GuideError) {
        // Log the error
        logError(error)
        
        // Add to history
        addToHistory(error)
        
        // Update current error if it's severe enough
        if (shouldDisplayError(error)) {
            _currentError.value = error
        }
        
        // Attempt automatic recovery if available
        error.recoveryAction?.let { recovery ->
            if (recovery.isAutomatic) {
                CoroutineScope(Dispatchers.IO).launch {
                    attemptRecovery(error, recovery)
                }
            }
        }
    }
    
    private fun logError(error: GuideError) {
        val logLevel = when (error.severity) {
            GuideErrorSeverity.DEBUG -> Log.DEBUG
            GuideErrorSeverity.INFO -> Log.INFO
            GuideErrorSeverity.WARNING -> Log.WARN
            GuideErrorSeverity.ERROR -> Log.ERROR
            GuideErrorSeverity.CRITICAL -> Log.ERROR
        }
        
        val message = buildString {
            append("[${error.category.name}] ")
            append(error.getMessage())
            if (error.context.isNotEmpty()) {
                append(" | Context: ${error.context}")
            }
        }
        
        Log.println(logLevel, TAG, message)
        
        error.exception?.let { exception ->
            Log.println(logLevel, TAG, getStackTrace(exception))
        }
    }
    
    private fun addToHistory(error: GuideError) {
        val currentHistory = _errorHistory.value.toMutableList()
        currentHistory.add(0, error) // Add to beginning
        
        // Limit history size
        if (currentHistory.size > MAX_ERROR_HISTORY) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _errorHistory.value = currentHistory
    }
    
    private fun shouldDisplayError(error: GuideError): Boolean {
        return error.severity in listOf(
            GuideErrorSeverity.ERROR,
            GuideErrorSeverity.CRITICAL
        )
    }
    
    fun clearCurrentError() {
        _currentError.value = null
    }
    
    fun retryLastAction() {
        _currentError.value?.recoveryAction?.let { recovery ->
            val error = _currentError.value!!
            CoroutineScope(Dispatchers.IO).launch {
                attemptRecovery(error, recovery)
            }
        }
    }
    
    private suspend fun attemptRecovery(error: GuideError, recovery: GuideRecoveryAction) {
        _isRecovering.value = true
        
        try {
            val success = recovery.action()
            if (success) {
                Log.i(TAG, "Recovery successful for error: ${error.id}")
                if (error == _currentError.value) {
                    clearCurrentError()
                }
            } else {
                Log.w(TAG, "Recovery failed for error: ${error.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recovery attempt threw exception for error: ${error.id}", e)
            reportError(e, "Recovery attempt failed", GuideErrorSeverity.ERROR)
        } finally {
            _isRecovering.value = false
        }
    }
    
    // Error analytics
    fun getErrorStatistics(): GuideErrorStatistics {
        val history = _errorHistory.value
        return GuideErrorStatistics(
            totalErrors = history.size,
            errorsBySeverity = history.groupBy { it.severity }.mapValues { it.value.size },
            errorsByCategory = history.groupBy { it.category }.mapValues { it.value.size },
            recentErrors = history.take(10),
            criticalErrorCount = history.count { it.severity == GuideErrorSeverity.CRITICAL }
        )
    }
    
    // Utility functions
    private fun generateErrorId(): String {
        return "ERR-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun getStackTrace(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}

// Data classes
data class GuideError(
    val id: String,
    val exception: Throwable? = null,
    val errorMessage: String? = null,
    val context: String = "",
    val severity: GuideErrorSeverity,
    val category: GuideErrorCategory,
    val timestamp: LocalDateTime,
    val recoveryAction: GuideRecoveryAction? = null,
    val isRecovered: Boolean = false
) {
    fun getMessage(): String {
        return errorMessage ?: exception?.message ?: "Unknown error occurred"
    }
    
    fun getUserFriendlyMessage(): String {
        return when (category) {
            GuideErrorCategory.NETWORK -> "Network connection issue. Please check your internet connection."
            GuideErrorCategory.API -> "Service temporarily unavailable. Please try again in a moment."
            GuideErrorCategory.AUTHENTICATION -> "Authentication failed. Please sign in again."
            GuideErrorCategory.PERMISSION -> "Permission required to continue. Please grant the necessary permissions."
            GuideErrorCategory.CAMERA -> "Camera access issue. Please check camera permissions."
            GuideErrorCategory.MICROPHONE -> "Microphone access issue. Please check microphone permissions."
            GuideErrorCategory.STORAGE -> "Storage access issue. Please check storage permissions."
            GuideErrorCategory.COOKING_SESSION -> "Cooking session error. Attempting to recover..."
            GuideErrorCategory.VOICE_PROCESSING -> "Voice processing error. Please try speaking again."
            GuideErrorCategory.VIDEO_PROCESSING -> "Video processing error. Attempting to reconnect..."
            GuideErrorCategory.AI_SERVICE -> "AI service temporarily unavailable. Trying alternative approach..."
            GuideErrorCategory.DATA_SYNC -> "Data synchronization issue. Your progress has been saved locally."
            GuideErrorCategory.GENERAL -> getMessage()
        }
    }
}

enum class GuideErrorSeverity(val displayName: String, val color: Color) {
    DEBUG("Debug", Color.Gray),
    INFO("Info", Color.Blue),
    WARNING("Warning", Color(0xFFFF9800)),
    ERROR("Error", Color(0xFFF44336)),
    CRITICAL("Critical", Color(0xFF9C27B0))
}

enum class GuideErrorCategory {
    GENERAL,
    NETWORK,
    API,
    AUTHENTICATION,
    PERMISSION,
    CAMERA,
    MICROPHONE,
    STORAGE,
    COOKING_SESSION,
    VOICE_PROCESSING,
    VIDEO_PROCESSING,
    AI_SERVICE,
    DATA_SYNC
}

data class GuideRecoveryAction(
    val name: String,
    val description: String,
    val isAutomatic: Boolean = false,
    val action: suspend () -> Boolean
)

data class GuideErrorStatistics(
    val totalErrors: Int,
    val errorsBySeverity: Map<GuideErrorSeverity, Int>,
    val errorsByCategory: Map<GuideErrorCategory, Int>,
    val recentErrors: List<GuideError>,
    val criticalErrorCount: Int
)

// Composable error handling utilities
@Composable
fun rememberGuideErrorState(): State<GuideError?> {
    return GuideErrorManager.instance.currentError.collectAsState()
}

@Composable
fun rememberGuideRecoveryState(): State<Boolean> {
    return GuideErrorManager.instance.isRecovering.collectAsState()
}

// Common recovery actions
object GuideRecoveryActions {
    
    fun retryNetworkRequest(request: suspend () -> Boolean) = GuideRecoveryAction(
        name = "Retry Network Request",
        description = "Attempting to reconnect and retry the request",
        isAutomatic = true,
        action = request
    )
    
    fun refreshAuthentication(authAction: suspend () -> Boolean) = GuideRecoveryAction(
        name = "Refresh Authentication", 
        description = "Refreshing authentication tokens",
        isAutomatic = true,
        action = authAction
    )
    
    fun fallbackToOfflineMode(fallbackAction: suspend () -> Boolean) = GuideRecoveryAction(
        name = "Offline Mode",
        description = "Switching to offline mode to continue",
        isAutomatic = true,
        action = fallbackAction
    )
    
    fun restartSession(restartAction: suspend () -> Boolean) = GuideRecoveryAction(
        name = "Restart Session",
        description = "Restarting the current session",
        isAutomatic = false,
        action = restartAction
    )
    
    fun requestPermissions(permissionAction: suspend () -> Boolean) = GuideRecoveryAction(
        name = "Grant Permissions",
        description = "Please grant the required permissions to continue",
        isAutomatic = false,
        action = permissionAction
    )
}

// Coroutine exception handler for global error catching
val GuideGlobalExceptionHandler = CoroutineExceptionHandler { context, exception ->
    GuideErrorManager.instance.reportError(
        exception = exception,
        context = context.toString(),
        severity = GuideErrorSeverity.ERROR,
        category = GuideErrorCategory.GENERAL
    )
}

// Extension functions for easy error reporting
fun Throwable.reportToGuide(
    context: String = "",
    severity: GuideErrorSeverity = GuideErrorSeverity.ERROR,
    category: GuideErrorCategory = GuideErrorCategory.GENERAL,
    recoveryAction: GuideRecoveryAction? = null
) {
    GuideErrorManager.instance.reportError(
        exception = this,
        context = context,
        severity = severity,
        category = category,
        recoveryAction = recoveryAction
    )
}

// Helper for handling network errors
fun handleNetworkError(
    exception: Throwable,
    retryAction: suspend () -> Boolean
) {
    exception.reportToGuide(
        context = "Network operation failed",
        category = GuideErrorCategory.NETWORK,
        recoveryAction = GuideRecoveryActions.retryNetworkRequest(retryAction)
    )
}

// Helper for handling API errors  
fun handleApiError(
    exception: Throwable,
    endpoint: String,
    fallbackAction: suspend () -> Boolean = { false }
) {
    exception.reportToGuide(
        context = "API call to $endpoint failed",
        category = GuideErrorCategory.API,
        recoveryAction = GuideRecoveryActions.fallbackToOfflineMode(fallbackAction)
    )
}

// Helper for handling permission errors
fun handlePermissionError(
    permissionType: String,
    requestAction: suspend () -> Boolean
) {
    GuideErrorManager.instance.reportError(
        errorMessage = "$permissionType permission required",
        context = "Permission check failed",
        severity = GuideErrorSeverity.WARNING,
        category = GuideErrorCategory.PERMISSION,
        recoveryAction = GuideRecoveryActions.requestPermissions(requestAction)
    )
}