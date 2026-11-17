package com.aarogya.sanjeevani.patient.booking

import com.aarogya.sanjeevani.core.data.model.ServiceMode

/** One request-form field's problem -- a stable, resolvable-to-text
 *  identifier, not raw display text itself, same split every other
 *  form in this app uses (see `AuthValidation.kt`'s own
 *  [com.aarogya.sanjeevani.core.data.auth.SignUpFieldError]). The
 *  backend is still the authoritative check (client-contract.md §7's
 *  own `AS011`/`AS012`/`AS013`) -- this is purely a client-side UX
 *  nicety, never trusted as a substitute. */
enum class RequestFormFieldError {
    CONTACT_PHONE_INVALID,
    ADDRESS_REQUIRED,
    NOTES_TOO_LONG,
}

/** Pure, Android-free validation for the request form -- every check
 *  is independent of the others, same reasoning as `AuthValidation`'s
 *  own doc comment. */
object RequestFormValidation {
    const val NOTES_MAX_LENGTH = 1000

    // Loose on purpose (a leading +, 7-15 digits, spaces/hyphens
    // allowed as separators) -- this is a fast, friendly client-side
    // sanity check; AS012_INVALID_CONTACT_PHONE is what actually
    // decides validity server-side.
    private val PHONE_DIGITS_REGEX = Regex("""^\+?[0-9\s-]{7,15}$""")
    private const val PHONE_MIN_DIGIT_COUNT = 7

    fun isPhoneValid(phone: String): Boolean {
        val trimmed = phone.trim()
        return PHONE_DIGITS_REGEX.matches(trimmed) && trimmed.count { it.isDigit() } >= PHONE_MIN_DIGIT_COUNT
    }

    /** [mode] is `null`-safe here (a field error for it is never
     *  raised) -- "no mode chosen yet" blocks [RequestFormUiState.isValid]
     *  on its own, via [RequestFormUiState.selectedMode] directly, not
     *  through this map (there's no text field to show a message
     *  next to; the mode cards' own "choose one" state covers it). */
    fun validate(
        mode: ServiceMode?,
        contactPhone: String,
        address: String,
        notes: String,
    ): Map<String, RequestFormFieldError> {
        val errors = mutableMapOf<String, RequestFormFieldError>()
        if (!isPhoneValid(contactPhone)) {
            errors["contactPhone"] = RequestFormFieldError.CONTACT_PHONE_INVALID
        }
        if (mode == ServiceMode.HOME_VISIT && address.isBlank()) {
            errors["address"] = RequestFormFieldError.ADDRESS_REQUIRED
        }
        if (notes.length > NOTES_MAX_LENGTH) {
            errors["notes"] = RequestFormFieldError.NOTES_TOO_LONG
        }
        return errors
    }
}
