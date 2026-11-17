package com.aarogya.sanjeevani.patient.auth.signup

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.auth.AuthValidation
import com.aarogya.sanjeevani.core.data.auth.PasswordStrength
import com.aarogya.sanjeevani.core.data.auth.SignUpFieldError

/**
 * Everything [SignUpScreen] needs to render. The four `*Touched` flags
 * (plus [submitAttempted]) are what makes validation show up "after the
 * first blur" rather than on every keystroke from an empty field --
 * [AuthValidation.validateSignUp] itself has no notion of touched, it's
 * always run against the current values; this state decides which of
 * its results are currently worth *showing*.
 *
 * Deliberately excluded from this state surviving process death (see
 * [SignUpViewModel]'s `SavedStateHandle` wiring): [password] and
 * [confirmPassword] -- passwords are never persisted, even to the
 * process-local, app-private `SavedStateHandle` bundle.
 */
data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameTouched: Boolean = false,
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmPasswordTouched: Boolean = false,
    val consentChecked: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
) {
    private val fieldErrors: Map<String, SignUpFieldError>
        get() = AuthValidation.validateSignUp(fullName, email, password, confirmPassword)

    val passwordStrength: PasswordStrength
        get() = AuthValidation.passwordStrength(password)

    val fullNameError: SignUpFieldError?
        get() = fieldErrors["fullName"]?.takeIf { fullNameTouched || submitAttempted }

    val emailError: SignUpFieldError?
        get() = fieldErrors["email"]?.takeIf { emailTouched || submitAttempted }

    val passwordError: SignUpFieldError?
        get() = fieldErrors["password"]?.takeIf { passwordTouched || submitAttempted }

    val confirmPasswordError: SignUpFieldError?
        get() = fieldErrors["confirmPassword"]?.takeIf { confirmPasswordTouched || submitAttempted }

    val showConsentError: Boolean
        get() = submitAttempted && !consentChecked

    val isValid: Boolean
        get() = fieldErrors.isEmpty() && consentChecked
}

/** One-off effects [SignUpRoute] reacts to exactly once. There's no
 *  "navigate home" event -- a [com.aarogya.sanjeevani.core.data.auth.SignUpOutcome.SignedIn]
 *  result is just an [com.aarogya.sanjeevani.core.data.auth.AuthState]
 *  transition the global auth gate already reacts to on its own, same
 *  reasoning as `LoginContract.kt`'s own doc comment. [NavigateToCheckInbox]
 *  is the one case that genuinely needs local navigation: pending email
 *  confirmation leaves the user signed out, so nothing else would ever
 *  navigate away from this screen. */
sealed interface SignUpUiEvent {
    data class NavigateToCheckInbox(val email: String) : SignUpUiEvent

    data class ShowError(val error: AppError) : SignUpUiEvent
}

/** Everything the user (or the screen itself, on blur) can do here,
 *  handled in [SignUpViewModel.onAction]. */
sealed interface SignUpAction {
    data class FullNameChanged(val value: String) : SignUpAction

    data class EmailChanged(val value: String) : SignUpAction

    data class PasswordChanged(val value: String) : SignUpAction

    data class ConfirmPasswordChanged(val value: String) : SignUpAction

    data object FullNameFocusLost : SignUpAction

    data object EmailFocusLost : SignUpAction

    data object PasswordFocusLost : SignUpAction

    data object ConfirmPasswordFocusLost : SignUpAction

    data class ConsentChanged(val checked: Boolean) : SignUpAction

    data object SubmitClicked : SignUpAction
}
