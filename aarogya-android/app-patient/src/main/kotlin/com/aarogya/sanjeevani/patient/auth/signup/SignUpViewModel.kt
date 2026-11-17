package com.aarogya.sanjeevani.patient.auth.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.SignUpOutcome
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : BaseViewModel<SignUpUiState, SignUpUiEvent, SignUpAction>(
            SignUpUiState(
                fullName = savedStateHandle[KEY_FULL_NAME] ?: "",
                email = savedStateHandle[KEY_EMAIL] ?: "",
                consentChecked = savedStateHandle[KEY_CONSENT] ?: false,
            ),
        ) {
        init {
            persist(KEY_FULL_NAME) { it.fullName }
            persist(KEY_EMAIL) { it.email }
            persist(KEY_CONSENT) { it.consentChecked }
        }

        /** Mirrors one field of [uiState] into [savedStateHandle] under
         *  [key] on every change -- never called with a selector that
         *  reads [SignUpUiState.password]/[SignUpUiState.confirmPassword],
         *  see this class's own doc comment on why. */
        private fun <T> persist(
            key: String,
            selector: (SignUpUiState) -> T,
        ) {
            viewModelScope.launch {
                uiState.map(selector).distinctUntilChanged().collect { savedStateHandle[key] = it }
            }
        }

        override fun onAction(action: SignUpAction) {
            when (action) {
                is SignUpAction.FullNameChanged -> setState { copy(fullName = action.value) }
                is SignUpAction.EmailChanged -> setState { copy(email = action.value) }
                is SignUpAction.PasswordChanged -> setState { copy(password = action.value) }
                is SignUpAction.ConfirmPasswordChanged -> setState { copy(confirmPassword = action.value) }
                SignUpAction.FullNameFocusLost -> setState { copy(fullNameTouched = true) }
                SignUpAction.EmailFocusLost -> setState { copy(emailTouched = true) }
                SignUpAction.PasswordFocusLost -> setState { copy(passwordTouched = true) }
                SignUpAction.ConfirmPasswordFocusLost -> setState { copy(confirmPasswordTouched = true) }
                is SignUpAction.ConsentChanged -> setState { copy(consentChecked = action.checked) }
                SignUpAction.SubmitClicked -> submit()
            }
        }

        private fun submit() {
            setState {
                copy(
                    submitAttempted = true,
                    fullNameTouched = true,
                    emailTouched = true,
                    passwordTouched = true,
                    confirmPasswordTouched = true,
                )
            }
            if (currentState.isSubmitting || !currentState.isValid) return

            val fullName = currentState.fullName.trim()
            val email = currentState.email.trim()
            val password = currentState.password

            viewModelScope.launch {
                setState { copy(isSubmitting = true) }
                when (val result = authRepository.signUpPatient(fullName, email, password)) {
                    is AppResult.Failure -> {
                        setState { copy(isSubmitting = false) }
                        sendEvent(SignUpUiEvent.ShowError(result.error))
                    }
                    is AppResult.Success ->
                        when (result.data) {
                            // Signed in: nothing to do, the auth gate
                            // reacts to AuthState on its own.
                            is SignUpOutcome.SignedIn -> setState { copy(isSubmitting = false) }
                            SignUpOutcome.PendingEmailConfirmation -> {
                                setState { copy(isSubmitting = false) }
                                sendEvent(SignUpUiEvent.NavigateToCheckInbox(email))
                            }
                        }
                }
            }
        }

        companion object {
            private const val KEY_FULL_NAME = "signup_full_name"
            private const val KEY_EMAIL = "signup_email"
            private const val KEY_CONSENT = "signup_consent"
        }
    }
