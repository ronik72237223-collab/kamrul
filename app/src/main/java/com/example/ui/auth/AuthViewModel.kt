package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AratRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isSignUpMode: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val aratName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val auth: FirebaseAuth = Firebase.auth,
    private val repository: AratRepository = AratRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null, infoMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun updateDisplayName(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun updateAratName(value: String) {
        _uiState.update { it.copy(aratName = value, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password

        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "সঠিক ইমেইল ঠিকানা লিখুন।") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।") }
            return
        }
        if (state.isSignUpMode) {
            if (state.displayName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "আপনার নাম লিখুন।") }
                return
            }
            if (password != state.confirmPassword) {
                _uiState.update { it.copy(errorMessage = "পাসওয়ার্ড দুটো মেলেনি।") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            try {
                if (state.isSignUpMode) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        val name = state.displayName.trim()
                        if (name.isNotBlank()) {
                            runCatching {
                                user.updateProfile(
                                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                                ).await()
                            }
                        }
                        repository.upsertUserProfile(
                            uid = user.uid,
                            email = email,
                            displayName = name,
                            aratName = state.aratName.trim()
                        )
                    }
                } else {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        repository.upsertUserProfile(
                            uid = user.uid,
                            email = user.email ?: email,
                            displayName = user.displayName ?: "",
                            aratName = ""
                        )
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = translateAuthError(e)
                    )
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "রিসেট লিংক পেতে আগে ইমেইল লিখুন।") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        infoMessage = "পাসওয়ার্ড রিসেট লিংক আপনার ইমেইলে পাঠানো হয়েছে।"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = translateAuthError(e)
                    )
                }
            }
        }
    }

    private fun translateAuthError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "পাসওয়ার্ড আরও শক্তিশালী দিন (কমপক্ষে ৬ অক্ষর)।"
        is FirebaseAuthInvalidCredentialsException -> "ইমেইল বা পাসওয়ার্ড সঠিক নয়।"
        is FirebaseAuthInvalidUserException -> "এই ইমেইলে কোনো অ্যাকাউন্ট পাওয়া যায়নি।"
        is FirebaseAuthUserCollisionException -> "এই ইমেইল দিয়ে আগেই অ্যাকাউন্ট খোলা হয়েছে।"
        else -> e.localizedMessage ?: "লগইন করতে সমস্যা হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
    }
}
