package com.aarogya.sanjeevani.patient.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.PatientSignInResult
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The reference implementation of the [BaseViewModel] pattern this
 * module's screens follow -- a single [LoginUiState], one-off
 * [LoginUiEvent]s for navigation/snackbar effects, and every user
 * action funneled through [onAction]. [LoginScreen]'s own doc comment
 * has the UI-layer half of this.
 *
 * [savedStateHandle] persists [LoginUiState.email] (only) across process
 * death -- a plain [androidx.lifecycle.ViewModel] already survives
 * rotation on its own, but not the system killing the whole process
 * while backgrounded; [LoginUiState.password] is never written to it,
 * on purpose.
 */
@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : BaseViewModel<LoginUiState, LoginUiEvent, LoginAction>(
            LoginUiState(email = savedStateHandle[KEY_EMAIL] ?: ""),
        ) {
        init {
            viewModelScope.launch {
                uiState.map { it.email }.distinctUntilChanged().collect { savedStateHandle[KEY_EMAIL] = it }
            }
        }

        override fun onAction(action: LoginAction) {
            when (action) {
                is LoginAction.EmailChanged -> setState { copy(email = action.value) }
                is LoginAction.PasswordChanged -> setState { copy(password = action.value) }
                LoginAction.SignUpClicked -> sendEvent(LoginUiEvent.NavigateToSignUp)
                LoginAction.ForgotPasswordClicked -> sendEvent(LoginUiEvent.NavigateToForgotPassword)
                LoginAction.SubmitClicked -> submit()
            }
        }

        private fun submit() {
            if (currentState.isSubmitting) return
            val email = currentState.email
            val password = currentState.password

            viewModelScope.launch {
                setState { copy(isSubmitting = true) }
                when (val result = authRepository.signInAsPatient(email, password)) {
                    is AppResult.Failure -> {
                        setState { copy(isSubmitting = false) }
                        sendEvent(LoginUiEvent.ShowError(result.error))
                    }
                    is AppResult.Success -> {
                        setState { copy(isSubmitting = false) }
                        // Success: nothing to do here -- the global auth
                        // gate reacts to AuthState flipping to
                        // Authenticated and navigates on its own.
                        if (result.data !is PatientSignInResult.Success) {
                            sendEvent(LoginUiEvent.ShowRejection(result.data))
                        }
                    }
                }
            }
        }

        companion object {
            private const val KEY_EMAIL = "login_email"
        }
    }
