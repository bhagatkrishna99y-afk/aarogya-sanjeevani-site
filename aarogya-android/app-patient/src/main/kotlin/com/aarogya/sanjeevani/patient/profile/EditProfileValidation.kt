package com.aarogya.sanjeevani.patient.profile

import com.aarogya.sanjeevani.core.data.auth.AuthValidation
import com.aarogya.sanjeevani.patient.booking.RequestFormValidation

/** Same stable-identifier-not-display-text split as `AuthValidation`'s
 *  own [com.aarogya.sanjeevani.core.data.auth.SignUpFieldError]. */
enum class EditProfileFieldError {
    FULL_NAME_TOO_SHORT,
    FULL_NAME_TOO_LONG,
    PHONE_INVALID,
}

/** Pure, Android-free validation for Edit Profile -- full name reuses
 *  [AuthValidation]'s own length bounds (the same name a patient typed
 *  at sign-up, just editable now), phone reuses [RequestFormValidation.isPhoneValid]
 *  (the exact same loose client-side check the booking form uses) --
 *  but unlike there, phone is optional here: blank is valid, only a
 *  non-blank value has to actually look like a phone number. */
object EditProfileValidation {
    fun validate(
        fullName: String,
        phone: String,
    ): Map<String, EditProfileFieldError> {
        val errors = mutableMapOf<String, EditProfileFieldError>()
        val trimmedName = fullName.trim()
        when {
            trimmedName.length < AuthValidation.FULL_NAME_MIN_LENGTH ->
                errors["fullName"] = EditProfileFieldError.FULL_NAME_TOO_SHORT
            trimmedName.length > AuthValidation.FULL_NAME_MAX_LENGTH ->
                errors["fullName"] = EditProfileFieldError.FULL_NAME_TOO_LONG
        }
        val trimmedPhone = phone.trim()
        if (trimmedPhone.isNotEmpty() && !RequestFormValidation.isPhoneValid(trimmedPhone)) {
            errors["phone"] = EditProfileFieldError.PHONE_INVALID
        }
        return errors
    }
}
