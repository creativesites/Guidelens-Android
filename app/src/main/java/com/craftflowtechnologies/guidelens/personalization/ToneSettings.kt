package com.craftflowtechnologies.guidelens.personalization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentTone(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val systemPromptModifier: String
) {
    PROFESSIONAL(
        id = "professional",
        displayName = "Professional",
        description = "Formal, precise, and methodical",
        icon = "💼",
        systemPromptModifier = "Be professional, precise, and methodical in your responses. Use formal language and provide detailed, structured guidance."
    ),
    
    CHATTY_WOMAN(
        id = "chatty_woman",
        displayName = "Chatty Friend",
        description = "Fun, gossipy, and encouraging like your best friend",
        icon = "💃",
        systemPromptModifier = "Be like a fun, chatty best friend! Use casual language, be encouraging and enthusiastic. Share tips like you're gossiping with a close friend. Use expressions like 'Girl!', 'Honey', 'Oh my goodness!'"
    ),
    
    COOL_GUY(
        id = "cool_guy", 
        displayName = "Cool Buddy",
        description = "Relaxed, friendly, and laid-back",
        icon = "😎",
        systemPromptModifier = "Be like a cool, relaxed buddy. Use casual, laid-back language. Be encouraging but chill. Use expressions like 'Dude', 'Bro', 'That's awesome', 'No worries'. Keep it friendly and easy-going."
    ),
    
    ENTHUSIASTIC(
        id = "enthusiastic",
        displayName = "Enthusiastic",
        description = "Energetic, motivating, and upbeat",
        icon = "🎉",
        systemPromptModifier = "Be super enthusiastic and energetic! Use lots of exclamation points, be motivating and upbeat. Celebrate every small win and keep the energy high!"
    ),
    
    GENTLE(
        id = "gentle",
        displayName = "Gentle Guide",
        description = "Calm, patient, and nurturing",
        icon = "🤗",
        systemPromptModifier = "Be very gentle, patient, and nurturing. Use calming language, be understanding if things don't go perfectly. Offer reassurance and gentle encouragement."
    ),
    
    WITTY(
        id = "witty",
        displayName = "Witty Helper",
        description = "Clever, humorous, and engaging",
        icon = "🧠",
        systemPromptModifier = "Be clever and witty in your responses. Use appropriate humor, make clever observations, and keep things engaging. Balance wit with helpful guidance."
    )
}

data class TonePreferences(
    val selectedTone: AgentTone = AgentTone.PROFESSIONAL,
    val customModifiers: Map<String, String> = emptyMap()
)

class ToneManager {
    private val _tonePreferences = MutableStateFlow(TonePreferences())
    val tonePreferences: StateFlow<TonePreferences> = _tonePreferences.asStateFlow()
    
    fun setTone(tone: AgentTone) {
        _tonePreferences.value = _tonePreferences.value.copy(selectedTone = tone)
    }
    
    fun getCurrentTone(): AgentTone = _tonePreferences.value.selectedTone
    
    fun getSystemPromptModifier(): String = getCurrentTone().systemPromptModifier
    
    fun addCustomModifier(key: String, modifier: String) {
        val currentModifiers = _tonePreferences.value.customModifiers.toMutableMap()
        currentModifiers[key] = modifier
        _tonePreferences.value = _tonePreferences.value.copy(customModifiers = currentModifiers)
    }
}

@Composable
fun rememberToneManager(): ToneManager {
    return remember { ToneManager() }
}