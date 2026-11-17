package com.aarogya.sanjeevani.core.data.auth

import android.content.Context
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.data.R

/** Resolves one [AuthValidation.validateSignUp] entry to text for that
 *  specific field, same split as
 *  [com.aarogya.sanjeevani.core.common.error.AppError.Validation.messageFor]:
 *  [SignUpFieldError] is the stable identifier, this is where it
 *  becomes display text. */
fun SignUpFieldError.toUserMessage(context: Context): String =
    when (this) {
        SignUpFieldError.FULL_NAME_TOO_SHORT -> context.getString(R.string.signup_error_full_name_too_short)
        SignUpFieldError.FULL_NAME_TOO_LONG -> context.getString(R.string.signup_error_full_name_too_long)
        SignUpFieldError.EMAIL_INVALID -> context.getString(R.string.signup_error_email_invalid)
        SignUpFieldError.PASSWORD_TOO_SHORT -> context.getString(R.string.signup_error_password_too_short)
        SignUpFieldError.PASSWORD_CONFIRMATION_MISMATCH ->
            context.getString(R.string.signup_error_password_confirmation_mismatch)
    }

fun PasswordStrength.toUserMessage(context: Context): String =
    when (this) {
        PasswordStrength.WEAK -> context.getString(R.string.password_strength_weak)
        PasswordStrength.MEDIUM -> context.getString(R.string.password_strength_medium)
        PasswordStrength.STRONG -> context.getString(R.string.password_strength_strong)
    }

fun SignUpOutcome.PendingEmailConfirmation.toUserMessage(context: Context): String =
    context.getString(R.string.signup_pending_email_confirmation)

/** `null` for [PatientSignInResult.Success] -- there's nothing to show;
 *  the caller should just proceed to signed-in content. */
fun PatientSignInResult.toUserMessage(context: Context): String? =
    when (this) {
        is PatientSignInResult.Success -> null
        is PatientSignInResult.WrongRole -> context.getString(R.string.signin_wrong_role)
        // Reuses :core:common's existing AS005_ACCOUNT_SUSPENDED text
        // (same message as a suspended account hitting that backend
        // code elsewhere) rather than duplicating the copy here.
        is PatientSignInResult.AccountSuspended ->
            AppError.Business(code = "AS005_ACCOUNT_SUSPENDED", message = "").toUserMessage(context)
    }
