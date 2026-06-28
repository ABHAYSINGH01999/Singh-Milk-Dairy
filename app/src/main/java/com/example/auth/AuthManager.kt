package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithGoogle(autoSelect: Boolean = true): Boolean {
        try {
            val credentialManager = CredentialManager.create(context)
            
            // 1. Resolve Web Client ID dynamically:
            val webClientId = "78024425455-8qi9ds3vbs9qrhtclleupgqqbtk0un1b.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(autoSelect)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val success = handleSignInResult(result)
            if (success) {
                showToast("Logged in successfully!")
            }
            return success
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            e.printStackTrace()
            showToast("Authentication cancelled")
            return false
        } catch (e: androidx.credentials.exceptions.GetCredentialUnsupportedException) {
            e.printStackTrace()
            showToast("Google Sign-In is not supported on this device. Please update Google Play Services.")
            return false
        } catch (e: GetCredentialException) {
            e.printStackTrace()
            val msg = e.message ?: ""
            val errorType = e.type ?: "Unknown Type"
            if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                showToast("Internet connection required")
            } else if (msg.contains("developer error", ignoreCase = true) || errorType.contains("DeveloperError", ignoreCase = true) || msg.contains("10:", ignoreCase = true)) {
                showToast("OAuth Error: Check Firebase SHA-1 fingerprint & package name registration in Firebase Console.")
            } else {
                showToast("Google Sign-In failed (${e.type}): ${e.message}")
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.message ?: ""
            if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                showToast("Internet connection required")
            } else {
                showToast("Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}")
            }
            return false
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Boolean {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                if (idToken.isNullOrEmpty()) {
                    showToast("Google Sign-In failed: ID Token is null or empty.")
                    return false
                }
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                _currentUser.value = authResult.user
                return true
            } catch (e: com.google.firebase.auth.FirebaseAuthException) {
                e.printStackTrace()
                val code = e.errorCode
                val msg = e.message ?: ""
                if (code.contains("API_KEY_INVALID", ignoreCase = true)) {
                    showToast("Firebase Auth Error: Invalid API Key in firebase configuration.")
                } else if (msg.contains("disabled", ignoreCase = true)) {
                    showToast("Firebase Auth Error: Google Sign-In is disabled in your Firebase console.")
                } else {
                    showToast("Firebase Auth Error: [${code}] ${e.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Firebase Auth Error: ${e.localizedMessage ?: e.toString()}")
            }
        } else {
            showToast("Google Sign-In failed: Unexpected credential type")
        }
        return false
    }

    fun startPhoneAuth(phoneNumber: String, activity: android.app.Activity, callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhoneAuthCredential(credential: com.google.firebase.auth.PhoneAuthCredential): Boolean {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            _currentUser.value = authResult.user
            true
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Phone Auth Failed: ${e.message}")
            false
        }
    }

    suspend fun updateProfile(name: String, email: String) {
        try {
            val user = auth.currentUser ?: return
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val profile = hashMapOf(
                "name" to name,
                "email" to email,
                "phone" to (user.phoneNumber ?: "")
            )
            db.collection("users").document(user.uid).set(profile, com.google.firebase.firestore.SetOptions.merge()).await()
            
            // Also update Firebase Auth profile
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            if (email.isNotEmpty() && user.email.isNullOrEmpty()) {
                // user.updateEmail(email).await() // Might require re-auth, better to just store in firestore
            }
            _currentUser.value = auth.currentUser
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun hasProfile(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(user.uid).get().await()
            doc.contains("name")
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
