package com.craftflowtechnologies.guidelens.clerk

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.prepareVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.SignedOut)
    val uiState = _uiState.asStateFlow()

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            SignUp.create(SignUp.CreateParams.Standard(emailAddress = email, password = password))
                .onSuccess {
                    if (it.status == SignUp.Status.COMPLETE) {
                        _uiState.value = SignUpUiState.Success
                    } else {
                        _uiState.value = SignUpUiState.NeedsVerification
                        it.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode())
                    }
                }
                .onFailure {
                    // See https://clerk.com/docs/custom-flows/error-handling
                    // for more info on error handling
                    Log.e("SignUpViewModel", it.longErrorMessageOrNull, it.throwable)
                    _uiState.value = SignUpUiState.Error(it.longErrorMessageOrNull ?: "Sign up failed")
                }
        }
    }

    fun verify(code: String) {
        val inProgressSignUp = Clerk.signUp ?: return
        viewModelScope.launch {
            Log.d("SignUpViewModel", "Attempting verification with code: ${code}")
            inProgressSignUp.attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
                .onSuccess { signUp ->
                    Log.d("SignUpViewModel", "Verification successful. Status: ${signUp.status}")
                    if (signUp.status == SignUp.Status.COMPLETE) {
                        _uiState.value = SignUpUiState.Success
                    } else {
                        Log.w("SignUpViewModel", "Verification not complete. Status: ${signUp.status}")
                        _uiState.value = SignUpUiState.Error("Verification incomplete. Status: ${signUp.status}")
                    }
                }
                .onFailure {
                    // See https://clerk.com/docs/custom-flows/error-handling
                    // for more info on error handling
                    Log.e("SignUpViewModel", "Verification failed: ${it.longErrorMessageOrNull}", it.throwable)
                    _uiState.value = SignUpUiState.Error(it.longErrorMessageOrNull ?: "Verification failed")
                }
        }
    }

    fun clearError() {
        if (_uiState.value is SignUpUiState.Error) {
            _uiState.value = SignUpUiState.SignedOut
        }
    }

    sealed interface SignUpUiState {
        data object SignedOut : SignUpUiState
        data object Success : SignUpUiState
        data object NeedsVerification : SignUpUiState
        data class Error(val message: String) : SignUpUiState
    }
}