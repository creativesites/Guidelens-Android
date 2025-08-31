package com.craftflowtechnologies.guidelens.ui

import kotlinx.serialization.Serializable

/**
 * Data class representing a user in the GuideLens app.
 * @param id Unique identifier for the user.
 * @param name Full name of the user.
 * @param email Email address of the user.
 * @param profilePicture Optional URL or path to the user's profile picture.
 * @param preferences User-specific preferences for app settings.
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val profilePicture: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val avatarUrl: String? = null,
    val createdAt: String,
    val onboardingCompleted: Boolean = false,
    val preferredAgentId: String? = null
){
    fun copy(
        id: String = this.id,
        email: String = this.email,
        name: String = this.name,
        avatarUrl: String? = this.avatarUrl,
        createdAt: String = this.createdAt,
        onboardingCompleted: Boolean = this.onboardingCompleted,
        preferredAgentId: String? = this.preferredAgentId
    ) = User(id, name, email, profilePicture, preferences, avatarUrl, createdAt, onboardingCompleted, preferredAgentId)
}