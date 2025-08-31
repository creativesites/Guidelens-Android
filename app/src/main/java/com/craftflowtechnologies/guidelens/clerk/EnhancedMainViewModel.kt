package com.craftflowtechnologies.guidelens.clerk

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EnhancedMainViewModel(context: Context): ViewModel() {
    companion object {
        private const val TAG = "EnhancedMainViewModel"
    }
    
    private val _uiState = MutableStateFlow<EnhancedAuthUiState>(EnhancedAuthUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            _uiState.value = when {
                !isInitialized -> EnhancedAuthUiState.Loading
                user != null -> EnhancedAuthUiState.SignedIn
                else -> EnhancedAuthUiState.SignedOut
            }
        }
        .launchIn(viewModelScope)
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                _uiState.value = EnhancedAuthUiState.Loading
                Log.d(TAG, "Starting Google OAuth flow")
                
                // Use Clerk's OAuth redirect flow for Google Sign-In
                SignIn.authenticateWithRedirect(
                    SignIn.AuthenticateWithRedirectParams.OAuth(OAuthProvider.GOOGLE)
                ).onSuccess { result ->
                    Log.d(TAG, "OAuth result: ${result.resultType}")
                    when(result.resultType) {
                        ResultType.SIGN_IN -> {
                            val signIn = result.signIn
                            Log.d(TAG, "Sign-In flow - Status: ${signIn?.status}")
                            if (signIn?.status == SignIn.Status.COMPLETE) {
                                Log.d(TAG, "Google Sign-In successful - User: ${signIn.id}")
                                // Force refresh the user state
                                refreshUserState()
                            } else {
                                Log.w(TAG, "Sign-In not complete, status: ${signIn?.status}")
                                _uiState.value = EnhancedAuthUiState.Error("Sign-In incomplete. Status: ${signIn?.status}")
                            }
                        }
                        ResultType.SIGN_UP -> {
                            val signUp = result.signUp
                            Log.d(TAG, "Sign-Up flow - Status: ${signUp?.status}")
                            if (signUp?.status == SignUp.Status.COMPLETE) {
                                Log.d(TAG, "Google Sign-Up successful - User: ${signUp.id}")
                                // Force refresh the user state
                                refreshUserState()
                            } else {
                                Log.w(TAG, "Sign-Up not complete, status: ${signUp?.status}")
                                _uiState.value = EnhancedAuthUiState.Error("Sign-Up incomplete. Status: ${signUp?.status}")
                            }
                        }
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Clerk Google OAuth failed: ${error.longErrorMessageOrNull}", error.throwable)
                    _uiState.value = EnhancedAuthUiState.Error(
                        error.longErrorMessageOrNull ?: "Google Sign-In failed. Please try again."
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                _uiState.value = EnhancedAuthUiState.Error(e.message ?: "Sign-In failed. Please try again.")
            }
        }
    }
    
    private fun refreshUserState() {
        // Force check the current user state
        viewModelScope.launch {
            try {
                val currentUser = Clerk.user
                if (currentUser != null) {
                    Log.d(TAG, "User authenticated: ${currentUser.id}")
                    _uiState.value = EnhancedAuthUiState.SignedIn
                } else {
                    Log.w(TAG, "No user found after authentication")
                    _uiState.value = EnhancedAuthUiState.Error("Authentication completed but user not found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing user state", e)
                _uiState.value = EnhancedAuthUiState.Error("Error verifying authentication")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Clerk.signOut()
                .onSuccess { 
                    Log.d(TAG, "Sign out successful")
                    _uiState.value = EnhancedAuthUiState.SignedOut 
                }
                .onFailure {
                    Log.e(TAG, it.longErrorMessageOrNull, it.throwable)
                    _uiState.value = EnhancedAuthUiState.Error("Sign out failed")
                }
        }
    }
    
    fun clearError() {
        if (_uiState.value is EnhancedAuthUiState.Error) {
            _uiState.value = EnhancedAuthUiState.SignedOut
        }
    }
}

sealed interface EnhancedAuthUiState {
    data object Loading : EnhancedAuthUiState
    data object SignedIn : EnhancedAuthUiState
    data object SignedOut : EnhancedAuthUiState
    data class Error(val message: String) : EnhancedAuthUiState
}