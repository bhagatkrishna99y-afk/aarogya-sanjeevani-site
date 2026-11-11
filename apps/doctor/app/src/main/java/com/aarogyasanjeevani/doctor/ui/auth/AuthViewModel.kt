package com.aarogyasanjeevani.doctor.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthFormState(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val sessionStatus: StateFlow<SessionStatus> get() = authRepository.sessionStatus

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    fun updateFullName(value: String) = updateForm { copy(fullName = value) }
    fun updatePhone(value: String) = updateForm { copy(phone = value) }
    fun updateEmail(value: String) = updateForm { copy(email = value) }
    fun updatePassword(value: String) = updateForm { copy(password = value) }

    private inline fun updateForm(crossinline block: AuthFormState.() -> AuthFormState) {
        _formState.value = _formState.value.block()
    }

    fun signIn(onSuccess: () -> Unit) {
        val state = _formState.value
        if (state.isSubmitting) return
        if (state.email.isBlank() || state.password.isBlank()) {
            updateForm { copy(errorMessage = "Please enter your email and password.") }
            return
        }

        viewModelScope.launch {
            updateForm { copy(isSubmitting = true, errorMessage = null) }
            authRepository.signIn(state.email.trim(), state.password)
                .onSuccess {
                    updateForm { copy(isSubmitting = false) }
                    onSuccess()
                }
                .onFailure { updateForm { copy(isSubmitting = false, errorMessage = it.message) } }
        }
    }

    /**
     * [onSignedIn] fires only if sign-up left us with an active session.
     * Projects with email confirmation required (as this one's hosted
     * instance is configured) create the user but no session until the
     * confirmation link is followed -- in that case [AuthFormState.infoMessage]
     * is set instead of pretending the user is signed in, and the screen
     * shows it in place rather than navigating.
     */
    fun signUp(onSignedIn: () -> Unit) {
        val state = _formState.value
        if (state.isSubmitting) return
        if (state.fullName.isBlank() || state.email.isBlank() || state.password.isBlank()) {
            updateForm { copy(errorMessage = "Please fill in your name, email and password.") }
            return
        }
        if (state.password.length < 8) {
            updateForm { copy(errorMessage = "Password must be at least 8 characters.") }
            return
        }

        viewModelScope.launch {
            updateForm { copy(isSubmitting = true, errorMessage = null) }
            authRepository.signUp(
                fullName = state.fullName.trim(),
                phone = state.phone.trim().takeIf { it.isNotBlank() },
                email = state.email.trim(),
                password = state.password,
            )
                .onSuccess {
                    updateForm { copy(isSubmitting = false) }
                    if (authRepository.currentUserId() != null) {
                        onSignedIn()
                    } else {
                        updateForm {
                            copy(infoMessage = "Check your email to confirm your account, then sign in.")
                        }
                    }
                }
                .onFailure { updateForm { copy(isSubmitting = false, errorMessage = it.message) } }
        }
    }

    fun dismissError() = updateForm { copy(errorMessage = null) }
}
