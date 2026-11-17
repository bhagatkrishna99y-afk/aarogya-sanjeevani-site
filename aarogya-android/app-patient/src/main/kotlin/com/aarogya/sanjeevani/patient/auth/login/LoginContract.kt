package com.aarogya.sanjeevani.patient.auth.login

import com.aarogya.sanjeevani.core.common.error.AppError

/** Everything [LoginScreen] needs to render -- a single immutable
 *  snapshot, per [com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel]. */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
)

/**
 * One-off effects [LoginRoute] reacts to exactly once. There's no
 * "navigate home on success" event here -- a successful sign-in is just
 * one more [com.aarogya.sanjeevani.core.data.auth.AuthState] transition,
 * and the global auth gate (`AuthGateEffect.kt`) is the *only* place
 * that reacts to those, so this screen staying signed-in-but-not-yet-
 * navigated for one frame while that effect catches up is expected, not
 * a bug to work around here. A wrong-role/suspended rejection is *not*
 * one of these events, for the same reason in reverse: that same auth
 * gate's reaction tears this screen down before an event sent from here
 * could ever be collected -- see `PatientApp.kt`'s doc comment on
 * [com.aarogya.sanjeevani.core.data.auth.AuthRepository.signInRejections],
 * the channel that message actually comes from. [ShowError] deliberately
 * carries the domain type ([AppError]), not pre-resolved text: resolving
 * it to a string needs an [android.content.Context], which only the
 * composable has -- see [AppError.toUserMessage]'s own doc comment for
 * why that split exists.
 */
sealed interface LoginUiEvent {
    data object NavigateToSignUp : LoginUiEvent

    data object NavigateToForgotPassword : LoginUiEvent

    data class ShowError(val error: AppError) : LoginUiEvent
}

/** Everything the user (or the screen itself) can do here, handled in
 *  [LoginViewModel.onAction]. */
sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction

    data class PasswordChanged(val value: String) : LoginAction

    data object SubmitClicked : LoginAction

    data object SignUpClicked : LoginAction

    data object ForgotPasswordClicked : LoginAction
}
