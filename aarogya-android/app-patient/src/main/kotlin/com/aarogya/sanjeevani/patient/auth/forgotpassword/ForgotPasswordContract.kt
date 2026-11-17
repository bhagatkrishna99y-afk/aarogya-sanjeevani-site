package com.aarogya.sanjeevani.patient.auth.forgotpassword

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.auth.AuthValidation
import com.aarogya.sanjeevani.core.data.auth.SignUpFieldError

/** [linkSent] is the *only* signal this screen ever shows for "did it
 *  work" -- it means exactly "the request completed", never "an account
 *  exists for this email" (see [com.aarogya.sanjeevani.core.data.auth.AuthRepository.sendPasswordReset]'s
 *  own doc comment: Supabase's reset endpoint is deliberately
 *  indistinguishable either way, by design, so there is nothing more
 *  specific this screen *could* truthfully show). */
data class ForgotPasswordUiState(
    val email: String = "",
    val emailTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val linkSent: Boolean = false,
) {
    /** Reuses [SignUpFieldError.EMAIL_INVALID] rather than a parallel
     *  single-purpose error type -- same identifier, same resolved
     *  text, one less thing for `AuthMessages.kt` to know about. */
    val emailError: SignUpFieldError?
        get() =
            SignUpFieldError.EMAIL_INVALID.takeIf {
                (emailTouched || submitAttempted) && !AuthValidation.isEmailValid(email)
            }
}

sealed interface ForgotPasswordUiEvent {
    data class ShowError(val error: AppError) : ForgotPasswordUiEvent
}

sealed interface ForgotPasswordAction {
    data class EmailChanged(val value: String) : ForgotPasswordAction

    data object EmailFocusLost : ForgotPasswordAction

    data object SubmitClicked : ForgotPasswordAction
}
