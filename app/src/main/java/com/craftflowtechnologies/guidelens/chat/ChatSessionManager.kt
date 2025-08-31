package com.craftflowtechnologies.guidelens.chat

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.craftflowtechnologies.guidelens.ui.ChatMessage
import com.craftflowtechnologies.guidelens.ui.ChatSession
import com.craftflowtechnologies.guidelens.ui.User
import com.craftflowtechnologies.guidelens.storage.UserDataManager
import com.craftflowtechnologies.guidelens.storage.UserProfile
import com.craftflowtechnologies.guidelens.storage.PersonalizedContext
import com.craftflowtechnologies.guidelens.storage.ArtifactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.util.*

class ChatSessionManager(
    private val context: Context,
    private val userDataManager: UserDataManager,
    private val artifactRepository: ArtifactRepository? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    companion object {
        private const val TAG = "ChatSessionManager"
        private const val PREFS_NAME = "chat_sessions"
        private const val PREF_SESSIONS_KEY = "sessions"
        private const val PREF_CURRENT_SESSION = "current_session_id"
        private const val PREF_CONTEXT_SUMMARIES = "context_summaries"
        private const val MAX_SESSIONS = 50 // Limit to prevent storage bloat
        private const val MAX_CONTEXT_TOKENS = 8000 // Token limit for personalization context
        private const val SUMMARY_TRIGGER_MESSAGES = 20 // Create summary after N messages
    }

    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    init {
        loadSessions()
        loadCurrentSession()
    }

    fun createNewSession(
        name: String,
        agentId: String,
        userId: String,
        isOffline: Boolean = false
    ): ChatSession {
        val session = ChatSession(
            name = name,
            messages = emptyList(),
            agentId = agentId,
            userId = userId,
            isOffline = isOffline
        )
        
        addSession(session)
        setCurrentSession(session)
        
        Log.d(TAG, "Created new session: ${session.name}")
        return session
    }

    fun addMessageToCurrentSession(message: ChatMessage) {
        val currentSession = _currentSession.value ?: return
        
        val updatedSession = currentSession.copy(
            messages = currentSession.messages + message,
            updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        
        updateSession(updatedSession)
        setCurrentSession(updatedSession)
        
        // Auto-extract recipes from assistant messages
        if (!message.isFromUser && artifactRepository != null) {
            scope.launch {
                try {
                    val extractionResult = artifactRepository.extractAndSaveRecipeFromMessage(
                        message = message.text,
                        userId = currentSession.userId,
                        agentType = currentSession.agentId
                    )
                    
                    if (extractionResult.isSuccess && extractionResult.getOrNull() != null) {
                        val recipe = extractionResult.getOrNull()!!
                        Log.i(TAG, "Auto-extracted recipe: ${recipe.title}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to auto-extract recipe from message", e)
                }
            }
        }
    }

    fun switchToSession(sessionId: String) {
        val session = _chatSessions.value.find { it.id == sessionId }
        if (session != null) {
            setCurrentSession(session)
            Log.d(TAG, "Switched to session: ${session.name}")
        }
    }

    fun deleteSession(sessionId: String) {
        val sessions = _chatSessions.value.toMutableList()
        sessions.removeAll { it.id == sessionId }
        
        _chatSessions.value = sessions
        saveSessions()

        // If we deleted the current session, switch to the most recent one or create new
        if (_currentSession.value?.id == sessionId) {
            val mostRecent = sessions.maxByOrNull { it.updatedAt }
            setCurrentSession(mostRecent)
        }
        
        Log.d(TAG, "Deleted session: $sessionId")
    }

    fun renameSession(sessionId: String, newName: String) {
        val sessions = _chatSessions.value.toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        
        if (sessionIndex != -1) {
            val updatedSession = sessions[sessionIndex].copy(
                name = newName,
                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            )
            
            sessions[sessionIndex] = updatedSession
            _chatSessions.value = sessions
            saveSessions()

            // Update current session if it's the one being renamed
            if (_currentSession.value?.id == sessionId) {
                _currentSession.value = updatedSession
            }
            
            Log.d(TAG, "Renamed session $sessionId to: $newName")
        }
    }

    fun getSessionsForUser(userId: String): List<ChatSession> {
        return _chatSessions.value.filter { it.userId == userId }
            .sortedByDescending { it.updatedAt }
    }

    fun getSessionsForAgent(agentId: String, userId: String): List<ChatSession> {
        return _chatSessions.value.filter { it.agentId == agentId && it.userId == userId }
            .sortedByDescending { it.updatedAt }
    }

    fun generateSessionName(firstMessage: String, agentId: String): String {
        // Generate a smart name based on the first message
        val words = firstMessage.trim().split(" ").take(4)
        val preview = words.joinToString(" ")
        
        return if (preview.length > 30) {
            "${preview.take(27)}..."
        } else if (preview.isBlank()) {
            "New ${getAgentName(agentId)} Chat"
        } else {
            preview
        }
    }

    private fun addSession(session: ChatSession) {
        val sessions = _chatSessions.value.toMutableList()
        sessions.add(0, session) // Add to beginning (most recent first)
        
        // Limit number of sessions to prevent storage bloat
        if (sessions.size > MAX_SESSIONS) {
            sessions.removeAt(sessions.lastIndex)
        }
        
        _chatSessions.value = sessions
        saveSessions()
    }

    private fun updateSession(session: ChatSession) {
        val sessions = _chatSessions.value.toMutableList()
        val index = sessions.indexOfFirst { it.id == session.id }
        
        if (index != -1) {
            sessions[index] = session
            // Move updated session to top
            sessions.removeAt(index)
            sessions.add(0, session)
            
            _chatSessions.value = sessions
            saveSessions()
        }
    }

    private fun setCurrentSession(session: ChatSession?) {
        _currentSession.value = session
        sharedPreferences.edit()
            .putString(PREF_CURRENT_SESSION, session?.id)
            .apply()
    }

    private fun loadSessions() {
        try {
            val sessionsJson = sharedPreferences.getString(PREF_SESSIONS_KEY, "[]") ?: "[]"
            val sessions = json.decodeFromString<List<ChatSession>>(sessionsJson)
            _chatSessions.value = sessions.sortedByDescending { it.updatedAt }
            Log.d(TAG, "Loaded ${sessions.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sessions", e)
            _chatSessions.value = emptyList()
        }
    }

    private fun saveSessions() {
        try {
            val sessionsJson = json.encodeToString(_chatSessions.value)
            sharedPreferences.edit()
                .putString(PREF_SESSIONS_KEY, sessionsJson)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving sessions", e)
        }
    }

    private fun loadCurrentSession() {
        val currentSessionId = sharedPreferences.getString(PREF_CURRENT_SESSION, null)
        if (currentSessionId != null) {
            val session = _chatSessions.value.find { it.id == currentSessionId }
            _currentSession.value = session
        }
    }

    private fun getAgentName(agentId: String): String {
        return when (agentId) {
            "cooking" -> "Cooking"
            "crafting" -> "Crafting"
            "diy" -> "DIY"
            "buddy" -> "Buddy"
            else -> "Assistant"
        }
    }

    fun clearAllSessions() {
        _chatSessions.value = emptyList()
        _currentSession.value = null
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "Cleared all sessions")
    }
    
    // Generate comprehensive context for AI messages
    suspend fun generateEnhancedAIContext(currentUser: User?): String {
        val personalContext = userDataManager.generateAIContext()
        val userContext = generateUserContext(currentUser)
        
        return buildString {
            if (userContext.isNotBlank()) {
                append(userContext)
                append(" ")
            }
            
            if (personalContext.isNotBlank()) {
                append(personalContext)
                append(" ")
            }
        }.trim().let { context ->
            // Apply token limit and compacting
            compactContext(context, MAX_CONTEXT_TOKENS)
        }
    }
    
    private fun generateUserContext(currentUser: User?): String {
        if (currentUser == null) return ""
        
        return buildString {
            append("Current user: ${currentUser.name}")
            if (currentUser.email.isNotEmpty()) append(" (${currentUser.email})")
            append(". ")
        }
    }
    
    private fun compactContext(context: String, maxTokens: Int): String {
        // Simple token estimation: ~4 characters per token
        val estimatedTokens = context.length / 4
        
        if (estimatedTokens <= maxTokens) return context
        
        // If context is too long, keep the most recent parts
        val maxChars = maxTokens * 4
        val sentences = context.split(". ")
        
        var compacted = ""
        for (sentence in sentences.reversed()) {
            val testString = sentence + ". " + compacted
            if (testString.length <= maxChars) {
                compacted = testString
            } else {
                break
            }
        }
        
        return compacted.ifBlank { context.take(maxChars) }
    }
    
    fun cleanup() {
        scope.cancel()
    }
}