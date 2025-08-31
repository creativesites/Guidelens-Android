package com.craftflowtechnologies.guidelens.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleSignInResult(
    val idToken: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val profilePicture: String? = null,
    val error: String? = null
)

class GoogleSignInManager(private val context: Context) {
    companion object {
        private const val TAG = "GoogleSignInManager"
        // Your OAuth client ID from the Google Cloud Console
        private const val WEB_CLIENT_ID = "169904244919-h72o96c00kl3r713rdbn1t66mvgh1dil.apps.googleusercontent.com"
    }

    private val credentialManager = CredentialManager.create(context)
    
    private val _signInState = MutableStateFlow<GoogleSignInResult?>(null)
    val signInState: StateFlow<GoogleSignInResult?> = _signInState.asStateFlow()

    suspend fun signInWithGoogle(): GoogleSignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            
            val signInResult = GoogleSignInResult(
                idToken = googleIdTokenCredential.idToken,
                email = googleIdTokenCredential.id,
                displayName = googleIdTokenCredential.displayName,
                profilePicture = googleIdTokenCredential.profilePictureUri?.toString()
            )
            
            _signInState.value = signInResult
            Log.d(TAG, "Google Sign-In successful for ${signInResult.email}")
            signInResult
            
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed", e)
            val errorResult = GoogleSignInResult(error = e.message ?: "Sign-In failed")
            _signInState.value = errorResult
            errorResult
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Google ID token parsing failed", e)
            val errorResult = GoogleSignInResult(error = "Invalid Google credentials")
            _signInState.value = errorResult
            errorResult
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            val errorResult = GoogleSignInResult(error = "Unexpected error occurred")
            _signInState.value = errorResult
            errorResult
        }
    }

    fun clearSignInState() {
        _signInState.value = null
    }
}