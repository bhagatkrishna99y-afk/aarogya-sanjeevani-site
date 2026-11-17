package com.aarogya.sanjeevani.core.data.auth

/** One signup field's problem -- a stable, resolvable-to-text identifier
 *  (see `AuthMessages.kt`), not raw display text itself, same split as
 *  [com.aarogya.sanjeevani.core.common.error.AppError.Validation]. */
enum class SignUpFieldError {
    FULL_NAME_TOO_SHORT,
    FULL_NAME_TOO_LONG,
    EMAIL_INVALID,
    PASSWORD_TOO_SHORT,
    PASSWORD_CONFIRMATION_MISMATCH,
}

/** A non-blocking hint, not a validation failure -- [AuthValidation.PASSWORD_MIN_LENGTH]
 *  is the only hard requirement; this is just what the UI shows alongside
 *  the password field while typing. */
enum class PasswordStrength {
    WEAK,
    MEDIUM,
    STRONG,
}

/**
 * Pure, Android-free validation for the patient sign-up form -- every
 * check is independent of the others (a name that's too short and a
 * mismatched confirm-password both get reported together, rather than
 * stopping at the first problem found), since a form screen wants to
 * show every field's error at once, not one at a time.
 */
object AuthValidation {
    const val FULL_NAME_MIN_LENGTH = 2
    const val FULL_NAME_MAX_LENGTH = 100
    const val PASSWORD_MIN_LENGTH = 8
    private const val STRONG_PASSWORD_MIN_LENGTH = 12
    private const val STRONG_PASSWORD_MIN_CHARACTER_CLASSES = 3
    private const val MEDIUM_PASSWORD_MIN_CHARACTER_CLASSES = 2

    // Deliberately loose (requires an @, a ., and no whitespace) rather
    // than a strict RFC 5322 pattern -- the backend/Supabase itself is
    // the real authority on whether an address is deliverable; this is
    // just a fast, friendly "did you forget the @" client-side check.
    private val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    /** The same check [validateSignUp] applies to its own email field --
     *  public so a single-field screen (Login, Forgot password) can
     *  reuse it without going through the full sign-up validation. */
    fun isEmailValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun validateSignUp(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Map<String, SignUpFieldError> {
        val errors = mutableMapOf<String, SignUpFieldError>()
        val trimmedName = fullName.trim()

        when {
            trimmedName.length < FULL_NAME_MIN_LENGTH -> errors["fullName"] = SignUpFieldError.FULL_NAME_TOO_SHORT
            trimmedName.length > FULL_NAME_MAX_LENGTH -> errors["fullName"] = SignUpFieldError.FULL_NAME_TOO_LONG
        }
        if (!isEmailValid(email)) {
            errors["email"] = SignUpFieldError.EMAIL_INVALID
        }
        if (password.length < PASSWORD_MIN_LENGTH) {
            errors["password"] = SignUpFieldError.PASSWORD_TOO_SHORT
        }
        if (password != confirmPassword) {
            errors["confirmPassword"] = SignUpFieldError.PASSWORD_CONFIRMATION_MISMATCH
        }
        return errors
    }

    /** Character-class variety + length -- a rough, good-enough-for-a-hint
     *  heuristic, not a real entropy estimate. */
    fun passwordStrength(password: String): PasswordStrength {
        if (password.length < PASSWORD_MIN_LENGTH) return PasswordStrength.WEAK

        var characterClasses = 0
        if (password.any { it.isDigit() }) characterClasses++
        if (password.any { it.isUpperCase() }) characterClasses++
        if (password.any { it.isLowerCase() }) characterClasses++
        if (password.any { !it.isLetterOrDigit() }) characterClasses++

        val isLongEnoughToBeStrong = password.length >= STRONG_PASSWORD_MIN_LENGTH
        val hasEnoughClassesToBeStrong = characterClasses >= STRONG_PASSWORD_MIN_CHARACTER_CLASSES
        val isStrong = isLongEnoughToBeStrong && hasEnoughClassesToBeStrong
        return when {
            isStrong -> PasswordStrength.STRONG
            characterClasses >= MEDIUM_PASSWORD_MIN_CHARACTER_CLASSES -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
