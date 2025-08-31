// Replace the existing SupabaseAuthManager implementation with this fixed version:

package com.craftflowtechnologies.guidelens.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.craftflowtechnologies.guidelens.ui.AuthState
import com.craftflowtechnologies.guidelens.ui.User
import com.craftflowtechnologies.guidelens.ui.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit

class SupabaseAuthManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val googleSignInManager = GoogleSignInManager(context)
    companion object {
        private const val TAG = "SupabaseAuthManager"
        private const val PREFS_NAME = "guide_lens_auth"
        private const val PREF_USER_ID = "user_id"
        private const val PREF_USER_NAME = "user_name"
        private const val PREF_USER_EMAIL = "user_email"
        private const val PREF_ACCESS_TOKEN = "access_token"
        private const val PREF_REFRESH_TOKEN = "refresh_token"
        private const val PREF_TOKEN_EXPIRY = "token_expiry"
        private const val PREF_IS_LOGGED_IN = "is_logged_in"
        private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val PREF_PREFERRED_AGENT_ID = "preferred_agent_id"
        private const val PREF_LAST_LOGIN_TIME = "last_login_time"
        private const val PREF_LOGIN_COUNT = "login_count"
        private const val SESSION_DURATION_DAYS = 30L

        private const val SUPABASE_URL = "https://aeqbdzcazkernanmvyyg.supabase.co"
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFlcWJkemNhemtlcm5hbm12eXlnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTIzMzMwNDMsImV4cCI6MjA2NzkwOTA0M30.psmkPYNcT5q77-h4itrLfhVbReAx0dbXWCMlPzXgCSQ"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    private val _authState = MutableStateFlow(
        AuthState(
            isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false),
            user = loadUserFromPrefs()
        )
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize with saved state immediately
        val savedUser = loadUserFromPrefs()
        val isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)

        if (savedUser != null && isLoggedIn) {
            _authState.value = AuthState(
                isLoggedIn = true,
                user = savedUser,
                error = null
            )
            Log.d(TAG, "Restored session for user: ${savedUser.email}")
        }

        // Then validate the session asynchronously
        coroutineScope.launch {
            checkAuthSession()
        }
    }

    private fun loadUserFromPrefs(): User? {
        val isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
        val userId = sharedPreferences.getString(PREF_USER_ID, "")
        val userName = sharedPreferences.getString(PREF_USER_NAME, "")
        val userEmail = sharedPreferences.getString(PREF_USER_EMAIL, "")
        val onboardingCompleted = sharedPreferences.getBoolean(PREF_ONBOARDING_COMPLETED, false)
        val preferredAgentId = sharedPreferences.getString(PREF_PREFERRED_AGENT_ID, null)

        return if (isLoggedIn && !userId.isNullOrBlank() && !userEmail.isNullOrBlank()) {
            User(
                id = userId,
                name = userName ?: "User",
                email = userEmail,
                preferences = UserPreferences(),
                profilePicture = "",
                avatarUrl = "",
                createdAt = "",
                onboardingCompleted = onboardingCompleted,
                preferredAgentId = preferredAgentId
            )
        } else {
            null
        }
    }

    // PROPERLY IMPLEMENTED updateUserProfile method
    suspend fun updateUserProfile(
        onboardingCompleted: Boolean? = null,
        preferredAgentId: String? = null
    ) {
        try {
            val currentUser = _authState.value.user ?: return
            val accessToken = sharedPreferences.getString(PREF_ACCESS_TOKEN, null) ?: return

            // Prepare update data
            val updates = mutableMapOf<String, Any>()
            onboardingCompleted?.let { updates["onboarding_completed"] = it }
            preferredAgentId?.let { updates["preferred_agent_id"] = it }

            if (updates.isNotEmpty()) {
                // Update in Supabase database
                val responseResult = withContext(Dispatchers.IO) {
                    val requestBody = JSONObject(updates as Map<String, Any>)

                    val request = Request.Builder()
                        .url("$SUPABASE_URL/rest/v1/users?id=eq.${currentUser.id}")
                        .patch(requestBody.toString().toRequestBody(jsonMediaType))
                        .header("Authorization", "Bearer $accessToken")
                        .header("apikey", SUPABASE_ANON_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    Pair(response, response.body?.string() ?: "")
                }

                val (response, responseBody) = responseResult

                if (response.isSuccessful) {
                    // Update local preferences
                    val editor = sharedPreferences.edit()
                    onboardingCompleted?.let { editor.putBoolean(PREF_ONBOARDING_COMPLETED, it) }
                    preferredAgentId?.let { editor.putString(PREF_PREFERRED_AGENT_ID, it) }
                    editor.apply()

                    // Update local state
                    val updatedUser = currentUser.copy(
                        onboardingCompleted = onboardingCompleted ?: currentUser.onboardingCompleted,
                        preferredAgentId = preferredAgentId ?: currentUser.preferredAgentId
                    )

                    _authState.value = _authState.value.copy(user = updatedUser)
                    Log.d(TAG, "User profile updated successfully")
                } else {
                    Log.w(TAG, "Failed to update user profile in Supabase: HTTP ${response.code}")
                    // Update local preferences anyway for offline functionality
                    val editor = sharedPreferences.edit()
                    onboardingCompleted?.let { editor.putBoolean(PREF_ONBOARDING_COMPLETED, it) }
                    preferredAgentId?.let { editor.putString(PREF_PREFERRED_AGENT_ID, it) }
                    editor.apply()

                    val updatedUser = currentUser.copy(
                        onboardingCompleted = onboardingCompleted ?: currentUser.onboardingCompleted,
                        preferredAgentId = preferredAgentId ?: currentUser.preferredAgentId
                    )

                    _authState.value = _authState.value.copy(user = updatedUser)
                    Log.d(TAG, "User profile updated locally (Supabase update failed)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile: ${e.message}", e)

            // Try to update locally if network fails
            try {
                val currentUser = _authState.value.user ?: return
                val editor = sharedPreferences.edit()
                onboardingCompleted?.let { editor.putBoolean(PREF_ONBOARDING_COMPLETED, it) }
                preferredAgentId?.let { editor.putString(PREF_PREFERRED_AGENT_ID, it) }
                editor.apply()

                val updatedUser = currentUser.copy(
                    onboardingCompleted = onboardingCompleted ?: currentUser.onboardingCompleted,
                    preferredAgentId = preferredAgentId ?: currentUser.preferredAgentId
                )

                _authState.value = _authState.value.copy(user = updatedUser)
                Log.d(TAG, "User profile updated locally only due to network error")
            } catch (localError: Exception) {
                Log.e(TAG, "Failed to update user profile locally: ${localError.message}", localError)
                _authState.value = _authState.value.copy(
                    error = "Failed to update profile: ${e.message}"
                )
            }
        }
    }

    private suspend fun checkAuthSession() {
        val isLoggedIn = sharedPreferences.getBoolean(PREF_IS_LOGGED_IN, false)
        val accessToken = sharedPreferences.getString(PREF_ACCESS_TOKEN, null)
        val tokenExpiry = sharedPreferences.getLong(PREF_TOKEN_EXPIRY, 0)
        val currentTime = System.currentTimeMillis() / 1000

        if (!isLoggedIn || accessToken.isNullOrBlank()) {
            clearUserFromPrefs()
            _authState.value = _authState.value.copy(
                isLoggedIn = false,
                user = null,
                error = null
            )
            return
        }

        // Check if token is expired (with 5-minute buffer)
        if (tokenExpiry <= currentTime + 300) {
            Log.d(TAG, "Access token expired or near expiry, attempting refresh...")
            refreshToken()
            return
        }

        try {
            val response = withContext(Dispatchers.IO) {
                validateSession(accessToken)
            }
            if (response.isSuccessful) {
                val user = loadUserFromPrefs()
                if (user != null) {
                    _authState.value = _authState.value.copy(
                        isLoggedIn = true,
                        user = user,
                        error = null
                    )
                    Log.d(TAG, "Session validated successfully for user: ${user.email}")
                } else {
                    Log.w(TAG, "Session valid but user data missing, signing out")
                    clearUserFromPrefs()
                    _authState.value = _authState.value.copy(
                        isLoggedIn = false,
                        user = null,
                        error = null
                    )
                }
            } else {
                Log.w(TAG, "Session validation failed with HTTP ${response.code}, attempting refresh")
                refreshToken()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session validation error: ${e.message}", e)
            // Try to refresh token on network errors
            refreshToken()
        }
    }

    private suspend fun validateSession(accessToken: String): Response {
        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/user")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .header("apikey", SUPABASE_ANON_KEY)
            .build()

        return httpClient.newCall(request).execute()
    }

    private suspend fun refreshToken() {
        val refreshToken = sharedPreferences.getString(PREF_REFRESH_TOKEN, null)
        if (refreshToken.isNullOrBlank()) {
            Log.w(TAG, "No refresh token available, signing out")
            clearUserFromPrefs()
            _authState.value = _authState.value.copy(
                isLoggedIn = false,
                user = null,
                error = "Session expired. Please sign in again."
            )
            return
        }

        try {
            Log.d(TAG, "Attempting to refresh token...")
            val responseResult = withContext(Dispatchers.IO) {
                val requestBody = JSONObject().apply {
                    put("refresh_token", refreshToken)
                    put("grant_type", "refresh_token")
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    .build()

                val response = httpClient.newCall(request).execute()
                Pair(response, response.body?.string() ?: "")
            }

            val (response, responseBody) = responseResult
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val newAccessToken = jsonResponse.optString("access_token")
                val newRefreshToken = jsonResponse.optString("refresh_token")
                val expiresIn = jsonResponse.optLong("expires_in")

                if (newAccessToken.isNotBlank()) {
                    saveTokens(
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken.ifBlank { refreshToken },
                        expiresIn = expiresIn // Will be overridden to 30 days in saveTokens()
                    )

                    val user = loadUserFromPrefs()
                    if (user != null) {
                        _authState.value = _authState.value.copy(
                            isLoggedIn = true,
                            user = user,
                            error = null
                        )
                        Log.d(TAG, "Token refresh successful for user: ${user.email}")
                    } else {
                        Log.w(TAG, "Token refreshed but user data missing")
                        clearUserFromPrefs()
                        _authState.value = _authState.value.copy(
                            isLoggedIn = false,
                            user = null,
                            error = "Session data corrupted. Please sign in again."
                        )
                    }
                } else {
                    Log.w(TAG, "Token refresh response missing access token")
                    clearUserFromPrefs()
                    _authState.value = _authState.value.copy(
                        isLoggedIn = false,
                        user = null,
                        error = "Token refresh failed. Please sign in again."
                    )
                }
            } else {
                Log.w(TAG, "Token refresh failed with HTTP ${response.code}: $responseBody")
                clearUserFromPrefs()
                _authState.value = _authState.value.copy(
                    isLoggedIn = false,
                    user = null,
                    error = "Session expired. Please sign in again."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error: ${e.message}", e)
            _authState.value = _authState.value.copy(
                error = "Network error. Please check your connection."
            )
        }
    }

    suspend fun login(email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        try {
            val responseResult = withContext(Dispatchers.IO) {
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                Pair(response, response.body?.string() ?: "")
            }

            val (response, responseBody) = responseResult
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val user = jsonResponse.optJSONObject("user")
                val userMetadata = user?.optJSONObject("user_metadata")

                val lastLoginTime = sharedPreferences.getLong(PREF_LAST_LOGIN_TIME, 0)
                val shouldShowOnboardingForUser = shouldShowOnboarding(lastLoginTime)

                val guideLensUser = User(
                    id = user?.optString("id") ?: UUID.randomUUID().toString(),
                    name = userMetadata?.optString("full_name") ?: "User",
                    email = user?.optString("email") ?: email,
                    preferences = UserPreferences(),
                    profilePicture = "",
                    avatarUrl = "",
                    createdAt = user?.optString("created_at") ?: "",
                    onboardingCompleted = !shouldShowOnboardingForUser,
                    preferredAgentId = sharedPreferences.getString(PREF_PREFERRED_AGENT_ID, null)
                )

                // Save tokens with our 30-day expiration
                saveTokens(
                    accessToken = jsonResponse.optString("access_token"),
                    refreshToken = jsonResponse.optString("refresh_token"),
                    expiresIn = jsonResponse.optLong("expires_in") // This gets overridden to 30 days
                )

                saveUserToPrefs(guideLensUser, true)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = guideLensUser,
                    error = null,
                    showOnboarding = shouldShowOnboardingForUser
                )
                Log.d(TAG, "Login successful for user: $email")
            } else {
                throw Exception(JSONObject(responseBody).optString("error_description", "Login failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = parseAuthError(e.message)
            )
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        try {
            val responseResult = withContext(Dispatchers.IO) {
                val requestBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("full_name", name)
                    })
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/signup")
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                Pair(response, response.body?.string() ?: "")
            }

            val (response, responseBody) = responseResult
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val user = jsonResponse.optJSONObject("user")
                val userMetadata = user?.optJSONObject("user_metadata")

                val guideLensUser = User(
                    id = user?.optString("id") ?: UUID.randomUUID().toString(),
                    name = userMetadata?.optString("full_name") ?: name,
                    email = user?.optString("email") ?: email,
                    preferences = UserPreferences(),
                    profilePicture = "",
                    avatarUrl = "",
                    createdAt = user?.optString("created_at") ?: "",
                    onboardingCompleted = false, // New users need onboarding
                    preferredAgentId = null
                )

                saveTokens(
                    accessToken = jsonResponse.optString("access_token"),
                    refreshToken = jsonResponse.optString("refresh_token"),
                    expiresIn = jsonResponse.optLong("expires_in") // Will be overridden to 30 days in saveTokens()
                )
                saveUserToPrefs(guideLensUser, true)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = guideLensUser,
                    error = null,
                    successMessage = "Account created successfully!"
                )
                Log.d(TAG, "Registration successful for user: $email")
            } else {
                throw Exception(JSONObject(responseBody).optString("error_description", "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = parseAuthError(e.message)
            )
        }
    }

    suspend fun resetPassword(email: String) {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        try {
            val responseResult = withContext(Dispatchers.IO) {
                val requestBody = JSONObject().apply {
                    put("email", email)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/recover")
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                Pair(response, response.body?.string() ?: "")
            }

            val (response, responseBody) = responseResult
            if (response.isSuccessful) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = null,
                    successMessage = "Password reset link sent to $email"
                )
                Log.d(TAG, "Password reset email sent to: $email")
            } else {
                throw Exception(JSONObject(responseBody).optString("error_description", "Password reset failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = parseAuthError(e.message)
            )
        }
    }

    suspend fun signOut() {
        try {
            val accessToken = sharedPreferences.getString(PREF_ACCESS_TOKEN, null)
            if (accessToken != null) {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/logout")
                    .post(RequestBody.create(null, ""))
                    .header("Authorization", "Bearer $accessToken")
                    .header("apikey", SUPABASE_ANON_KEY)
                    .build()

                httpClient.newCall(request).execute()
            }
            
            // Clear Google Sign-In state
            googleSignInManager.clearSignInState()
            
            clearUserFromPrefs()
            _authState.value = AuthState(
                isLoggedIn = false,
                user = null,
                error = null
            )
            Log.d(TAG, "Sign out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            clearUserFromPrefs()
            _authState.value = AuthState(
                isLoggedIn = false,
                user = null,
                error = "Sign out failed. Please try again."
            )
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null, successMessage = null)
    }

    private fun saveUserToPrefs(user: User, isLoggedIn: Boolean) {
        sharedPreferences.edit()
            .putString(PREF_USER_ID, user.id)
            .putString(PREF_USER_NAME, user.name)
            .putString(PREF_USER_EMAIL, user.email)
            .putBoolean(PREF_ONBOARDING_COMPLETED, user.onboardingCompleted)
            .putString(PREF_PREFERRED_AGENT_ID, user.preferredAgentId)
            .putBoolean(PREF_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }

    private fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        // Set session to last for 30 days (one month) instead of default 1 hour
        val oneMonthInSeconds = SESSION_DURATION_DAYS * 24L * 60L * 60L // 30 days in seconds
        val expiryTime = (System.currentTimeMillis() / 1000) + oneMonthInSeconds

        val currentTime = System.currentTimeMillis()
        val loginCount = sharedPreferences.getInt(PREF_LOGIN_COUNT, 0) + 1

        sharedPreferences.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putLong(PREF_TOKEN_EXPIRY, expiryTime) // Use our 30-day expiry instead of Supabase's
            .putLong(PREF_LAST_LOGIN_TIME, currentTime)
            .putInt(PREF_LOGIN_COUNT, loginCount)
            .apply()

        Log.d(TAG, "Session extended to $SESSION_DURATION_DAYS days from now (login #$loginCount)")
    }

    private fun clearUserFromPrefs() {
        Log.d(TAG, "Clearing user session data")
        sharedPreferences.edit()
            .remove(PREF_USER_ID)
            .remove(PREF_USER_NAME)
            .remove(PREF_USER_EMAIL)
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .remove(PREF_TOKEN_EXPIRY)
            .remove(PREF_ONBOARDING_COMPLETED)
            .remove(PREF_PREFERRED_AGENT_ID)
            // Keep login history for analytics
            // .remove(PREF_LAST_LOGIN_TIME)
            // .remove(PREF_LOGIN_COUNT)
            .putBoolean(PREF_IS_LOGGED_IN, false)
            .apply()
    }

    // Google OAuth integration with Supabase
    suspend fun signInWithGoogle() {
        _authState.value = _authState.value.copy(isLoading = true, error = null)

        try {
            val googleResult = googleSignInManager.signInWithGoogle()
            
            if (googleResult.error != null) {
                throw Exception(googleResult.error)
            }

            if (googleResult.idToken.isNullOrBlank()) {
                throw Exception("Google sign-in failed to provide valid credentials")
            }

            // Use Google ID token with Supabase
            val responseResult = withContext(Dispatchers.IO) {
                val requestBody = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", googleResult.idToken)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=id_token")
                    .post(requestBody.toString().toRequestBody(jsonMediaType))
                    .header("apikey", SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                Pair(response, response.body?.string() ?: "")
            }

            val (response, responseBody) = responseResult
            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val user = jsonResponse.optJSONObject("user")
                val userMetadata = user?.optJSONObject("user_metadata")
                
                val lastLoginTime = sharedPreferences.getLong(PREF_LAST_LOGIN_TIME, 0)
                val shouldShowOnboarding = shouldShowOnboarding(lastLoginTime)

                val guideLensUser = User(
                    id = user?.optString("id") ?: UUID.randomUUID().toString(),
                    name = googleResult.displayName ?: userMetadata?.optString("full_name") ?: "User",
                    email = user?.optString("email") ?: googleResult.email ?: "",
                    preferences = UserPreferences(),
                    profilePicture = googleResult.profilePicture ?: "",
                    avatarUrl = googleResult.profilePicture ?: "",
                    createdAt = user?.optString("created_at") ?: "",
                    onboardingCompleted = !shouldShowOnboarding,
                    preferredAgentId = sharedPreferences.getString(PREF_PREFERRED_AGENT_ID, null)
                )

                saveTokens(
                    accessToken = jsonResponse.optString("access_token"),
                    refreshToken = jsonResponse.optString("refresh_token"),
                    expiresIn = jsonResponse.optLong("expires_in")
                )
                saveUserToPrefs(guideLensUser, true)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    user = guideLensUser,
                    error = null,
                    showOnboarding = shouldShowOnboarding
                )
                Log.d(TAG, "Google sign-in successful for user: ${guideLensUser.email}")
            } else {
                throw Exception("Failed to authenticate with Supabase using Google credentials")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = parseAuthError(e.message)
            )
        }
    }

    // Check if user should see onboarding (monthly logins or new users)
    private fun shouldShowOnboarding(lastLoginTime: Long): Boolean {
        if (lastLoginTime == 0L) {
            return true // New user or first time login
        }

        val currentTime = System.currentTimeMillis()
        val daysSinceLastLogin = (currentTime - lastLoginTime) / (1000 * 60 * 60 * 24)
        
        // Show onboarding if more than 25 days since last login (monthly-ish)
        return daysSinceLastLogin >= 25
    }

    // Check if session needs renewal (25+ days old)
    fun isSessionNearExpiry(): Boolean {
        val lastLoginTime = sharedPreferences.getLong(PREF_LAST_LOGIN_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val daysSinceLogin = (currentTime - lastLoginTime) / (1000 * 60 * 60 * 24)
        return daysSinceLogin >= 25
    }

    // Get session info for UI display
    fun getSessionInfo(): Pair<Int, Long> {
        val loginCount = sharedPreferences.getInt(PREF_LOGIN_COUNT, 0)
        val lastLoginTime = sharedPreferences.getLong(PREF_LAST_LOGIN_TIME, 0)
        return Pair(loginCount, lastLoginTime)
    }

    private fun parseAuthError(errorMessage: String?): String {
        return when {
            errorMessage?.contains("Invalid login credentials") == true ->
                "Incorrect email or password"
            errorMessage?.contains("Email not confirmed") == true ->
                "Please verify your email to continue"
            errorMessage?.contains("User already registered") == true ->
                "This email is already registered"
            errorMessage?.contains("Password should be at least") == true ->
                "Password must be at least 6 characters"
            errorMessage?.contains("Unable to validate email address") == true ->
                "Please enter a valid email address"
            errorMessage?.contains("Network") == true ->
                "Network error. Please check your connection"
            errorMessage?.contains("Invalid id_token") == true ->
                "Google Sign-In failed. Try again."
            errorMessage?.contains("Google sign-in failed") == true ->
                "Google Sign-In was cancelled or failed"
            else -> errorMessage ?: "An unexpected error occurred"
        }
    }
}