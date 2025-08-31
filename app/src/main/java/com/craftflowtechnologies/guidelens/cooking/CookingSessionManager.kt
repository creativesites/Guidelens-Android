package com.craftflowtechnologies.guidelens.cooking

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.text.SimpleDateFormat
import java.util.*

class CookingSessionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cooking_sessions", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _currentSession = MutableStateFlow<CookingSession?>(null)
    val currentSession: StateFlow<CookingSession?> = _currentSession.asStateFlow()
    
    private val _uiState = MutableStateFlow(CookingUIState())
    val uiState: StateFlow<CookingUIState> = _uiState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<UserCookingProfile?>(null)
    val userProfile: StateFlow<UserCookingProfile?> = _userProfile.asStateFlow()
    
    private val timerJobs = mutableMapOf<String, Job>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "CookingSessionManager"
        private const val PREF_USER_PROFILE = "user_profile"
        private const val PREF_CURRENT_SESSION = "current_session"
    }
    
    init {
        loadUserProfile()
        loadCurrentSession()
    }
    
    // Session Management
    fun startCookingSession(recipe: Recipe) {
        val session = CookingSession(
            recipe = recipe,
            stepStates = recipe.steps.associate { 
                it.id to StepState(it.id) 
            }.toMutableMap()
        )
        
        _currentSession.value = session
        _uiState.value = _uiState.value.copy(
            currentPhase = CookingPhase.OVERVIEW
        )
        
        saveCurrentSession()
        Log.d(TAG, "Started cooking session for: ${recipe.title}")
    }
    
    fun beginCooking() {
        val session = _currentSession.value ?: return
        val firstStep = session.recipe.steps.firstOrNull() ?: return
        
        _currentSession.value = session.copy(
            currentStepIndex = 0
        )
        
        _uiState.value = _uiState.value.copy(
            currentPhase = CookingPhase.COOKING
        )
        
        activateStep(firstStep.id)
        saveCurrentSession()
    }
    
    fun activateStep(stepId: String) {
        val session = _currentSession.value ?: return
        val step = session.recipe.steps.find { it.id == stepId } ?: return
        
        // Update step state
        session.stepStates[stepId] = StepState(
            stepId = stepId,
            status = StepStatus.ACTIVE,
            startTime = System.currentTimeMillis()
        )
        
        // Auto-start timer if step has duration
        step.duration?.let { duration ->
            startTimer(
                name = "Step ${step.stepNumber}: ${step.title}",
                duration = duration * 60 * 1000L, // Convert to milliseconds
                stepId = stepId
            )
        }
        
        _currentSession.value = session
        saveCurrentSession()
        
        Log.d(TAG, "Activated step: ${step.title}")
    }
    
    fun completeStep(stepId: String, userNotes: String? = null) {
        val session = _currentSession.value ?: return
        val stepIndex = session.recipe.steps.indexOfFirst { it.id == stepId }
        if (stepIndex == -1) return
        
        // Update step state
        session.stepStates[stepId] = session.stepStates[stepId]?.copy(
            status = StepStatus.COMPLETED,
            endTime = System.currentTimeMillis(),
            userNotes = userNotes
        ) ?: return
        
        session.completedSteps.add(stepId)
        
        // Stop any timers for this step
        stopTimersForStep(stepId)
        
        // Move to next step or complete session
        val nextIndex = stepIndex + 1
        if (nextIndex < session.recipe.steps.size) {
            _currentSession.value = session.copy(currentStepIndex = nextIndex)
            activateStep(session.recipe.steps[nextIndex].id)
        } else {
            completeSession()
        }
        
        saveCurrentSession()
    }
    
    fun completeSession() {
        val session = _currentSession.value ?: return
        val profile = _userProfile.value ?: return
        
        // Add to cooking history
        val historyEntry = CookingHistoryEntry(
            recipeId = session.recipe.id,
            recipeName = session.recipe.title,
            completedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
            cookingTime = System.currentTimeMillis() - session.startTime,
            difficulty = session.recipe.difficulty,
            modifications = session.userModifications.toList()
        )
        
        val updatedProfile = profile.copy(
            cookingHistory = profile.cookingHistory + historyEntry
        )
        
        _userProfile.value = updatedProfile
        _uiState.value = _uiState.value.copy(currentPhase = CookingPhase.COMPLETED)
        
        saveUserProfile()
        clearCurrentSession()
        
        Log.d(TAG, "Completed cooking session for: ${session.recipe.title}")
    }
    
    fun pauseSession() {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(isPaused = true)
        _uiState.value = _uiState.value.copy(currentPhase = CookingPhase.PAUSED)
        
        // Pause all timers
        timerJobs.values.forEach { it.cancel() }
        
        saveCurrentSession()
    }
    
    fun resumeSession() {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(isPaused = false)
        _uiState.value = _uiState.value.copy(currentPhase = CookingPhase.COOKING)
        
        // Resume any paused timers
        session.timers.values.filter { it.isPaused }.forEach { timer ->
            startTimerFromState(timer)
        }
        
        saveCurrentSession()
    }
    
    // Timer Management
    fun startTimer(name: String, duration: Long, stepId: String? = null, type: TimerType = TimerType.COOKING): String {
        val timer = TimerState(
            name = name,
            originalDuration = duration,
            remainingTime = duration,
            isRunning = true,
            stepId = stepId,
            type = type
        )
        
        val session = _currentSession.value ?: return timer.id
        session.timers[timer.id] = timer
        
        startTimerFromState(timer)
        
        _currentSession.value = session
        saveCurrentSession()
        
        return timer.id
    }
    
    private fun startTimerFromState(timer: TimerState) {
        timerJobs[timer.id] = coroutineScope.launch {
            var remaining = timer.remainingTime
            val interval = 1000L // 1 second
            
            while (remaining > 0 && isActive) {
                delay(interval)
                remaining -= interval
                
                // Update timer state
                val session = _currentSession.value ?: return@launch
                val updatedTimer = session.timers[timer.id]?.copy(
                    remainingTime = remaining
                ) ?: return@launch
                
                session.timers[timer.id] = updatedTimer
                _currentSession.value = session
                
                // Save periodically (every 30 seconds)
                if (remaining % 30000L == 0L) {
                    saveCurrentSession()
                }
            }
            
            // Timer completed
            if (remaining <= 0) {
                onTimerComplete(timer.id)
            }
        }
    }
    
    private fun onTimerComplete(timerId: String) {
        val session = _currentSession.value ?: return
        val timer = session.timers[timerId] ?: return
        
        // Update timer state
        session.timers[timerId] = timer.copy(
            remainingTime = 0,
            isRunning = false
        )
        
        _currentSession.value = session
        saveCurrentSession()
        
        // Could trigger notification or sound here
        Log.d(TAG, "Timer completed: ${timer.name}")
    }
    
    fun pauseTimer(timerId: String) {
        val session = _currentSession.value ?: return
        val timer = session.timers[timerId] ?: return
        
        timerJobs[timerId]?.cancel()
        session.timers[timerId] = timer.copy(
            isRunning = false,
            isPaused = true
        )
        
        _currentSession.value = session
        saveCurrentSession()
    }
    
    fun resumeTimer(timerId: String) {
        val session = _currentSession.value ?: return
        val timer = session.timers[timerId] ?: return
        
        if (timer.remainingTime > 0) {
            val resumedTimer = timer.copy(
                isRunning = true,
                isPaused = false
            )
            session.timers[timerId] = resumedTimer
            startTimerFromState(resumedTimer)
            
            _currentSession.value = session
            saveCurrentSession()
        }
    }
    
    fun adjustTimer(timerId: String, newDuration: Long) {
        val session = _currentSession.value ?: return
        val timer = session.timers[timerId] ?: return
        
        timerJobs[timerId]?.cancel()
        
        val adjustedTimer = timer.copy(
            originalDuration = newDuration,
            remainingTime = newDuration,
            isRunning = true,
            isPaused = false
        )
        
        session.timers[timerId] = adjustedTimer
        startTimerFromState(adjustedTimer)
        
        _currentSession.value = session
        saveCurrentSession()
        
        // Record user modification
        recordUserModification(
            stepId = timer.stepId ?: "",
            type = ModificationType.TIME_ADJUSTMENT,
            description = "Adjusted timer from ${timer.originalDuration / 60000} to ${newDuration / 60000} minutes"
        )
    }
    
    private fun stopTimersForStep(stepId: String) {
        val session = _currentSession.value ?: return
        session.timers.values.filter { it.stepId == stepId }.forEach { timer ->
            timerJobs[timer.id]?.cancel()
            session.timers[timer.id] = timer.copy(isRunning = false)
        }
    }
    
    // User Modifications and Learning
    fun recordUserModification(stepId: String, type: ModificationType, description: String) {
        val session = _currentSession.value ?: return
        val modification = UserModification(stepId, type, description)
        session.userModifications.add(modification)
        
        _currentSession.value = session
        saveCurrentSession()
    }
    
    fun addUserNote(stepId: String, note: String) {
        val session = _currentSession.value ?: return
        session.notes.add("Step ${session.recipe.steps.find { it.id == stepId }?.stepNumber}: $note")
        
        _currentSession.value = session
        saveCurrentSession()
    }
    
    // User Profile Management
    fun updateUserProfile(profile: UserCookingProfile) {
        _userProfile.value = profile
        saveUserProfile()
    }
    
    fun initializeUserProfile(userId: String, name: String) {
        val profile = UserCookingProfile(userId = userId, name = name)
        _userProfile.value = profile
        saveUserProfile()
    }
    
    // Personalized Suggestions
    fun getPersonalizedSuggestions(stepId: String): List<SmartSuggestion> {
        val profile = _userProfile.value ?: return emptyList()
        val session = _currentSession.value ?: return emptyList()
        val step = session.recipe.steps.find { it.id == stepId } ?: return emptyList()
        
        val suggestions = mutableListOf<SmartSuggestion>()
        
        // Based on cooking history
        val similarRecipes = profile.cookingHistory.filter { 
            it.difficulty == session.recipe.difficulty 
        }
        
        if (similarRecipes.any { it.modifications.any { mod -> mod.type == ModificationType.TIME_ADJUSTMENT } }) {
            suggestions.add(
                SmartSuggestion(
                    type = SuggestionType.TIME_ADJUSTMENT,
                    title = "Consider Timer Adjustment",
                    description = "Based on your cooking style, you might want to adjust the timing",
                    confidence = 0.7f
                )
            )
        }
        
        // Based on dietary restrictions
        profile.allergies.forEach { allergy ->
            session.recipe.ingredients.forEach { ingredient ->
                if (ingredient.name.contains(allergy, ignoreCase = true)) {
                    suggestions.add(
                        SmartSuggestion(
                            type = SuggestionType.INGREDIENT_SUBSTITUTION,
                            title = "Allergy Alert",
                            description = "Consider substituting ${ingredient.name} due to your allergy to $allergy",
                            confidence = 1.0f
                        )
                    )
                }
            }
        }
        
        // Safety reminders for critical steps
        if (step.criticalStep) {
            suggestions.add(
                SmartSuggestion(
                    type = SuggestionType.SAFETY_REMINDER,
                    title = "Critical Step",
                    description = "This step is crucial for the recipe's success. Take your time!",
                    confidence = 1.0f
                )
            )
        }
        
        return suggestions
    }
    
    // Persistence
    private fun saveUserProfile() {
        _userProfile.value?.let { profile ->
            prefs.edit()
                .putString(PREF_USER_PROFILE, json.encodeToString(profile))
                .apply()
        }
    }
    
    private fun loadUserProfile() {
        prefs.getString(PREF_USER_PROFILE, null)?.let { profileJson ->
            try {
                _userProfile.value = json.decodeFromString<UserCookingProfile>(profileJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user profile", e)
            }
        }
    }
    
    private fun saveCurrentSession() {
        _currentSession.value?.let { session ->
            prefs.edit()
                .putString(PREF_CURRENT_SESSION, json.encodeToString(session))
                .apply()
        }
    }
    
    private fun loadCurrentSession() {
        prefs.getString(PREF_CURRENT_SESSION, null)?.let { sessionJson ->
            try {
                _currentSession.value = json.decodeFromString<CookingSession>(sessionJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current session", e)
            }
        }
    }
    
    private fun clearCurrentSession() {
        prefs.edit().remove(PREF_CURRENT_SESSION).apply()
        _currentSession.value = null
    }
    
    fun cleanup() {
        timerJobs.values.forEach { it.cancel() }
        timerJobs.clear()
        coroutineScope.cancel()
    }
}