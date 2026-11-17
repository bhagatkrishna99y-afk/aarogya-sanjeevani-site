package com.aarogya.sanjeevani.patient.auth.forgotpassword

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : BaseViewModel<ForgotPasswordUiState, ForgotPasswordUiEvent, ForgotPasswordAction>(
            ForgotPasswordUiState(email = savedStateHandle[KEY_EMAIL] ?: ""),
        ) {
        init {
            viewModelScope.launch {
                uiState.map { it.email }.distinctUntilChanged().collect { savedStateHandle[KEY_EMAIL] = it }
            }
        }

        override fun onAction(action: ForgotPasswordAction) {
            when (action) {
                is ForgotPasswordAction.EmailChanged -> setState { copy(email = action.value, linkSent = false) }
                ForgotPasswordAction.EmailFocusLost -> setState { copy(emailTouched = true) }
                ForgotPasswordAction.SubmitClicked -> submit()
            }
        }

        private fun submit() {
            setState { copy(submitAttempted = true, emailTouched = true) }
            if (currentState.isSubmitting || currentState.emailError != null) return

            val email = currentState.email.trim()
            viewModelScope.launch {
                setState { copy(isSubmitting = true) }
                when (val result = authRepository.sendPasswordReset(email)) {
                    is AppResult.Success -> setState { copy(isSubmitting = false, linkSent = true) }
                    is AppResult.Failure -> {
                        setState { copy(isSubmitting = false) }
                        sendEvent(ForgotPasswordUiEvent.ShowError(result.error))
                    }
                }
            }
        }

        companion object {
            private const val KEY_EMAIL = "forgot_password_email"
        }
    }
