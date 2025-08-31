package com.craftflowtechnologies.guidelens.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Smart Session Recovery & Continuity System for GuideLens
 * Ensures users never lose cooking progress due to interruptions
 */
class GuideSessionPersistence private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuideSessionPersistence()
        
        private const val MAX_SESSIONS = 10
        const val SESSION_TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val AUTO_SAVE_INTERVAL_MS = 30_000L // 30 seconds
    }
    
    // Session state management
    private val _activeSessions = MutableStateFlow<Map<String, GuideSession>>(emptyMap())
    val activeSessions: StateFlow<Map<String, GuideSession>> = _activeSessions.asStateFlow()
    
    private val _currentSession = MutableStateFlow<GuideSession?>(null)
    val currentSession: StateFlow<GuideSession?> = _currentSession.asStateFlow()
    
    private val _recoveredSessions = MutableStateFlow<List<GuideSession>>(emptyList())
    val recoveredSessions: StateFlow<List<GuideSession>> = _recoveredSessions.asStateFlow()
    
    // Internal storage
    private val sessionStorage = ConcurrentHashMap<String, GuidePersistedSession>()
    private val autoSaveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Session lifecycle management
    private var autoSaveJob: Job? = null
    
    init {
        startAutoSave()
        loadPersistedSessions()
    }
    
    /**
     * Create a new cooking session
     */
    suspend fun startSession(
        type: GuideSessionType,
        recipeName: String? = null,
        agentId: String = "cooking",
        metadata: Map<String, Any> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        
        val sessionId = generateSessionId()
        val session = GuideSession(
            id = sessionId,
            type = type,
            recipeName = recipeName,
            agentId = agentId,
            startTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis(),
            status = GuideSessionStatus.ACTIVE,
            progress = GuideSessionProgress(),
            metadata = metadata.toMutableMap()
        )
        
        // Add to active sessions
        val currentSessions = _activeSessions.value.toMutableMap()
        currentSessions[sessionId] = session
        _activeSessions.value = currentSessions
        
        // Set as current session
        _currentSession.value = session
        
        // Persist immediately
        persistSession(session)
        
        sessionId
    }
    
    /**
     * Resume an existing session
     */
    suspend fun resumeSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = sessionStorage[sessionId]?.toGuideSession()
                ?: _activeSessions.value[sessionId]
            
            if (session != null && !session.isExpired()) {
                val updatedSession = session.copy(
                    status = GuideSessionStatus.ACTIVE,
                    lastActiveTime = System.currentTimeMillis(),
                    resumeCount = session.resumeCount + 1
                )
                
                updateSession(updatedSession)
                _currentSession.value = updatedSession
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.reportToGuide(
                context = "Session resume failed",
                category = GuideErrorCategory.DATA_SYNC
            )
            false
        }
    }
    
    /**
     * Update session progress
     */
    suspend fun updateProgress(
        sessionId: String,
        currentStep: Int? = null,
        completedSteps: Set<String>? = null,
        notes: String? = null,
        customData: Map<String, Any>? = null
    ) = withContext(Dispatchers.IO) {
        
        val session = _activeSessions.value[sessionId] ?: return@withContext
        
        val updatedProgress = session.progress.copy(
            currentStep = currentStep ?: session.progress.currentStep,
            completedSteps = completedSteps ?: session.progress.completedSteps,
            notes = notes ?: session.progress.notes,
            lastUpdateTime = System.currentTimeMillis(),
            customData = session.progress.customData.toMutableMap().apply {
                customData?.let { putAll(it) }
            }
        )
        
        val updatedSession = session.copy(
            progress = updatedProgress,
            lastActiveTime = System.currentTimeMillis()
        )
        
        updateSession(updatedSession)
    }
    
    /**
     * Pause current session
     */
    suspend fun pauseSession(sessionId: String, reason: String = "User paused") {
        withContext(Dispatchers.IO) {
            val session = _activeSessions.value[sessionId] ?: return@withContext
            
            val pausedSession = session.copy(
                status = GuideSessionStatus.PAUSED,
                lastActiveTime = System.currentTimeMillis(),
                metadata = session.metadata.toMutableMap().apply {
                    put("pause_reason", reason)
                    put("pause_time", System.currentTimeMillis())
                }
            )
            
            updateSession(pausedSession)
            persistSession(pausedSession)
        }
    }
    
    /**
     * Complete session
     */
    suspend fun completeSession(sessionId: String, rating: Int? = null) {
        withContext(Dispatchers.IO) {
            val session = _activeSessions.value[sessionId] ?: return@withContext
            
            val completedSession = session.copy(
                status = GuideSessionStatus.COMPLETED,
                endTime = System.currentTimeMillis(),
                lastActiveTime = System.currentTimeMillis(),
                metadata = session.metadata.toMutableMap().apply {
                    rating?.let { put("user_rating", it) }
                    put("completion_time", System.currentTimeMillis())
                }
            )
            
            updateSession(completedSession)
            persistSession(completedSession)
            
            // Remove from active sessions
            val currentSessions = _activeSessions.value.toMutableMap()
            currentSessions.remove(sessionId)
            _activeSessions.value = currentSessions
            
            // Clear current session if it was this one
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = null
            }
        }
    }
    
    /**
     * Handle app interruptions (calls, notifications, etc.)
     */
    suspend fun handleInterruption(
        interruptionType: GuideInterruptionType,
        context: String = ""
    ) {
        val currentSession = _currentSession.value ?: return
        
        withContext(Dispatchers.IO) {
            val interruptedSession = currentSession.copy(
                status = GuideSessionStatus.INTERRUPTED,
                metadata = currentSession.metadata.toMutableMap().apply {
                    put("interruption_type", interruptionType.name)
                    put("interruption_time", System.currentTimeMillis())
                    put("interruption_context", context)
                }
            )
            
            updateSession(interruptedSession)
            persistSession(interruptedSession)
        }
    }
    
    /**
     * Resume from interruption
     */
    suspend fun resumeFromInterruption(): Boolean {
        val currentSession = _currentSession.value ?: return false
        
        return if (currentSession.status == GuideSessionStatus.INTERRUPTED) {
            resumeSession(currentSession.id)
        } else {
            false
        }
    }
    
    /**
     * Get sessions available for recovery
     */
    fun getRecoverableSessions(): List<GuideSession> {
        val now = System.currentTimeMillis()
        return sessionStorage.values
            .mapNotNull { it.toGuideSession() }
            .filter { session ->
                session.status in listOf(
                    GuideSessionStatus.PAUSED,
                    GuideSessionStatus.INTERRUPTED,
                    GuideSessionStatus.ACTIVE
                ) && !session.isExpired()
            }
            .sortedByDescending { it.lastActiveTime }
    }
    
    /**
     * Delete old or unwanted sessions
     */
    suspend fun cleanupOldSessions() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val keysToRemove = mutableListOf<String>()
        
        sessionStorage.forEach { (key, session) ->
            val guideSession = session.toGuideSession()
            if (guideSession.isExpired() || 
                (guideSession.status == GuideSessionStatus.COMPLETED && 
                 now - (guideSession.endTime ?: 0) > SESSION_TIMEOUT_MS)) {
                keysToRemove.add(key)
            }
        }
        
        keysToRemove.forEach { sessionStorage.remove(it) }
        
        // Also cleanup active sessions
        val activeSessions = _activeSessions.value.toMutableMap()
        activeSessions.entries.removeIf { (_, session) -> session.isExpired() }
        _activeSessions.value = activeSessions
    }
    
    /**
     * Export session data for backup/sync
     */
    fun exportSessionData(): String {
        val exportData = GuideSessionExport(
            sessions = sessionStorage.values.toList(),
            exportTime = System.currentTimeMillis(),
            version = "1.0"
        )
        
        return Json.encodeToString(exportData)
    }
    
    /**
     * Import session data from backup/sync
     */
    suspend fun importSessionData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val importData = Json.decodeFromString<GuideSessionExport>(data)
            
            importData.sessions.forEach { persistedSession ->
                sessionStorage[persistedSession.id] = persistedSession
            }
            
            loadPersistedSessions()
            true
            
        } catch (e: Exception) {
            e.reportToGuide(
                context = "Session data import failed",
                category = GuideErrorCategory.DATA_SYNC
            )
            false
        }
    }
    
    // Internal helper functions
    private suspend fun updateSession(session: GuideSession) {
        val currentSessions = _activeSessions.value.toMutableMap()
        currentSessions[session.id] = session
        _activeSessions.value = currentSessions
        
        if (_currentSession.value?.id == session.id) {
            _currentSession.value = session
        }
    }
    
    private suspend fun persistSession(session: GuideSession) {
        val persistedSession = GuidePersistedSession.fromGuideSession(session)
        sessionStorage[session.id] = persistedSession
        
        // Keep only recent sessions to manage storage
        if (sessionStorage.size > MAX_SESSIONS) {
            val oldestKey = sessionStorage.entries
                .minByOrNull { it.value.lastActiveTime }?.key
            oldestKey?.let { sessionStorage.remove(it) }
        }
    }
    
    private fun loadPersistedSessions() {
        val recoverableSessions = getRecoverableSessions()
        _recoveredSessions.value = recoverableSessions
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    private fun startAutoSave() {
        autoSaveJob = autoSaveScope.launch {
            while (isActive) {
                delay(AUTO_SAVE_INTERVAL_MS)
                
                // Auto-save all active sessions
                _activeSessions.value.values.forEach { session ->
                    if (session.status == GuideSessionStatus.ACTIVE) {
                        persistSession(session)
                    }
                }
                
                // Cleanup old sessions periodically
                cleanupOldSessions()
            }
        }
    }
    
    fun destroy() {
        autoSaveJob?.cancel()
        autoSaveScope.cancel()
    }
}

// Data classes and enums
@Serializable
data class GuideSession(
    val id: String,
    val type: GuideSessionType,
    val recipeName: String? = null,
    val agentId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val lastActiveTime: Long,
    val status: GuideSessionStatus,
    val progress: GuideSessionProgress,
    val metadata: MutableMap<String, @Contextual Any> = mutableMapOf(),
    val resumeCount: Int = 0
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastActiveTime > GuideSessionPersistence.SESSION_TIMEOUT_MS
    }
    
    fun getDuration(): Long {
        return (endTime ?: System.currentTimeMillis()) - startTime
    }
    
    fun getFormattedDuration(): String {
        val duration = getDuration()
        val minutes = duration / (1000 * 60)
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}

@Serializable
data class GuideSessionProgress(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val completedSteps: Set<String> = emptySet(),
    val notes: String = "",
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val customData: MutableMap<String, @Contextual Any> = mutableMapOf()
) {
    fun getCompletionPercentage(): Float {
        return if (totalSteps > 0) {
            completedSteps.size.toFloat() / totalSteps.toFloat()
        } else 0f
    }
}

enum class GuideSessionType {
    COOKING,
    CRAFTING,
    DIY,
    LEARNING,
    GENERAL
}

enum class GuideSessionStatus {
    ACTIVE,
    PAUSED,
    INTERRUPTED,
    COMPLETED,
    CANCELLED,
    ERROR
}

enum class GuideInterruptionType {
    PHONE_CALL,
    NOTIFICATION,
    APP_BACKGROUNDED,
    SYSTEM_UPDATE,
    LOW_BATTERY,
    NETWORK_LOST,
    USER_SWITCH,
    OTHER
}

@Serializable
data class GuidePersistedSession(
    val id: String,
    val type: GuideSessionType,
    val recipeName: String? = null,
    val agentId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val lastActiveTime: Long,
    val status: GuideSessionStatus,
    val progress: GuideSessionProgress,
    val metadataJson: String = "{}",
    val resumeCount: Int = 0
) {
    fun toGuideSession(): GuideSession {
        val metadata: MutableMap<String, Any> = try {
            Json.decodeFromString<Map<String, String>>(metadataJson).toMutableMap().mapValues { it.value as Any }.toMutableMap()
        } catch (e: Exception) {
            mutableMapOf<String, Any>()
        }
        
        return GuideSession(
            id = id,
            type = type,
            recipeName = recipeName,
            agentId = agentId,
            startTime = startTime,
            endTime = endTime,
            lastActiveTime = lastActiveTime,
            status = status,
            progress = progress,
            metadata = metadata,
            resumeCount = resumeCount
        )
    }
    
    companion object {
        fun fromGuideSession(session: GuideSession): GuidePersistedSession {
            val metadataJson = try {
                Json.encodeToString(session.metadata.mapValues { it.value.toString() })
            } catch (e: Exception) {
                "{}"
            }
            
            return GuidePersistedSession(
                id = session.id,
                type = session.type,
                recipeName = session.recipeName,
                agentId = session.agentId,
                startTime = session.startTime,
                endTime = session.endTime,
                lastActiveTime = session.lastActiveTime,
                status = session.status,
                progress = session.progress,
                metadataJson = metadataJson,
                resumeCount = session.resumeCount
            )
        }
    }
}

@Serializable
data class GuideSessionExport(
    val sessions: List<GuidePersistedSession>,
    val exportTime: Long,
    val version: String
)

// Composable utilities
@Composable
fun rememberGuideCurrentSession(): State<GuideSession?> {
    return GuideSessionPersistence.instance.currentSession.collectAsState()
}

@Composable
fun rememberGuideActiveSessions(): State<Map<String, GuideSession>> {
    return GuideSessionPersistence.instance.activeSessions.collectAsState()
}

@Composable
fun rememberGuideRecoverableSessions(): State<List<GuideSession>> {
    return GuideSessionPersistence.instance.recoveredSessions.collectAsState()
}

// Extension functions
suspend fun GuideSession.pause(reason: String = "User paused") {
    GuideSessionPersistence.instance.pauseSession(this.id, reason)
}

suspend fun GuideSession.resume(): Boolean {
    return GuideSessionPersistence.instance.resumeSession(this.id)
}

suspend fun GuideSession.complete(rating: Int? = null) {
    GuideSessionPersistence.instance.completeSession(this.id, rating)
}

suspend fun GuideSession.updateProgress(
    currentStep: Int? = null,
    completedSteps: Set<String>? = null,
    notes: String? = null,
    customData: Map<String, Any>? = null
) {
    GuideSessionPersistence.instance.updateProgress(
        this.id, currentStep, completedSteps, notes, customData
    )
}