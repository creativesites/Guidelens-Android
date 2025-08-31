package com.craftflowtechnologies.guidelens.clerk

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        _uiState.value = SignInUiState.Loading
        viewModelScope.launch {
            Log.d("SignInViewModel", "Attempting sign in for: $email")
            SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
                .onSuccess { signIn ->
                    Log.d("SignInViewModel", "SignIn created successfully. Status: ${signIn.status}")
                    if (signIn.status == SignIn.Status.COMPLETE) {
                        Log.d("SignInViewModel", "Sign-In complete")
                        _uiState.value = SignInUiState.Success
                    } else {
                        Log.w("SignInViewModel", "Sign-In incomplete. Status: ${signIn.status}")
                        _uiState.value = SignInUiState.Error("Sign-In incomplete. Status: ${signIn.status}")
                    }
                }
                .onFailure { error ->
                    Log.e("SignInViewModel", "Sign-In failed: ${error.longErrorMessageOrNull}", error.throwable)
                    val errorMessage = when {
                        error.longErrorMessageOrNull?.contains("not found", ignoreCase = true) == true -> 
                            "Account not found. Please check your email or sign up first."
                        error.longErrorMessageOrNull?.contains("password", ignoreCase = true) == true -> 
                            "Incorrect password. Please try again."
                        error.longErrorMessageOrNull?.contains("identifier", ignoreCase = true) == true -> 
                            "Invalid email address. Please check and try again."
                        else -> error.longErrorMessageOrNull ?: "Sign in failed. Please try again."
                    }
                    _uiState.value = SignInUiState.Error(errorMessage)
                }
        }
    }

    fun clearError() {
        if (_uiState.value is SignInUiState.Error) {
            _uiState.value = SignInUiState.Idle
        }
    }

    sealed interface SignInUiState {
        data object Idle : SignInUiState
        data object Loading : SignInUiState
        data class Error(val message: String) : SignInUiState
        data object Success : SignInUiState
    }
}