package com.aarogya.sanjeevani.patient.profile

import com.aarogya.sanjeevani.core.common.error.AppError

/**
 * [originalFullName]/[originalPhone] are what [EditProfileAction.Load]
 * fetched -- [isDirty] compares the live fields against those, never
 * against some separately-tracked "has the user typed anything" flag,
 * so pasting back the exact original value correctly makes the form
 * clean again rather than staying stuck dirty.
 */
data class EditProfileUiState(
    val isLoading: Boolean = true,
    val loadError: AppError? = null,
    val fullName: String = "",
    val phone: String = "",
    val originalFullName: String = "",
    val originalPhone: String = "",
    val fullNameTouched: Boolean = false,
    val phoneTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSaving: Boolean = false,
) {
    val fieldErrors: Map<String, EditProfileFieldError>
        get() = EditProfileValidation.validate(fullName, phone)

    val isDirty: Boolean
        get() = fullName.trim() != originalFullName.trim() || phone.trim() != originalPhone.trim()

    val isSaveEnabled: Boolean
        get() = isDirty && fieldErrors.isEmpty() && !isSaving && !isLoading
}

sealed interface EditProfileUiEvent {
    data object SaveSucceeded : EditProfileUiEvent

    data class SaveFailed(val error: AppError) : EditProfileUiEvent
}

sealed interface EditProfileAction {
    /** Fired once, from the route's own `LaunchedEffect(Unit)` -- never
     *  from a lifecycle resume the way most other screens in this app
     *  load, since re-fetching every resume would silently stomp
     *  whatever the patient is mid-typing if they, say, briefly switch
     *  apps and come back. */
    data object Load : EditProfileAction

    data class FullNameChanged(val value: String) : EditProfileAction

    data object FullNameFocusLost : EditProfileAction

    data class PhoneChanged(val value: String) : EditProfileAction

    data object PhoneFocusLost : EditProfileAction

    data object SaveClicked : EditProfileAction
}
