package com.craftflowtechnologies.guidelens.ui

import kotlinx.serialization.Serializable

/**
 * Data class representing user preferences for the GuideLens app.
 * @param favoriteAgent The user's preferred AI agent (e.g., "cooking").
 * @param voiceEnabled Whether voice interaction is enabled.
 * @param notifications Whether notifications are enabled.
 * @param theme The app theme preference (e.g., "dark", "light").
 */
@Serializable
data class UserPreferences(
    val favoriteAgent: String = "cooking",
    val voiceEnabled: Boolean = true,
    val notifications: Boolean = true,
    val theme: String = "dark"
)