package com.aarogya.sanjeevani.patient.booking

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.patient.doctors.CLINIC_TIME_ZONE
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant

/**
 * A patient's preferred part of the day for a service that has no
 * doctor (and so no real-availability slot grid) to choose a precise
 * time from -- `backend/docs/client-contract.md`'s `create_service_request`
 * still needs one concrete `p_slot_start` either way, so each window
 * maps to one representative hour in [CLINIC_TIME_ZONE], the same
 * timezone every other slot-time calculation in this app already uses.
 * Each hour below *is* the named constant detekt wants -- it's the
 * window's own representative hour.
 */
@Suppress("MagicNumber")
enum class TimeWindow(val label: String, val representativeHour: Int) {
    MORNING("Morning", 9),
    AFTERNOON("Afternoon", 13),
    EVENING("Evening", 17),
}

/** The one-time context a service/doctor pairing needs before the form
 *  can render meaningfully -- which [ServiceMode]s both actually
 *  support together, so the mode cards never offer a choice the
 *  backend would just reject with `AS009_MODE_UNSUPPORTED`. */
sealed interface RequestFormContextState {
    data object Loading : RequestFormContextState

    data class Error(val error: AppError) : RequestFormContextState

    data class Loaded(val availableModes: List<ServiceMode>) : RequestFormContextState
}

/**
 * [serviceId]/[serviceName]/[doctorId]/[doctorName]/[slotStart]/
 * [slotDurationMinutes] are carried from [com.aarogya.sanjeevani.patient.navigation.RequestForm]'s
 * own nav args, read once at construction -- never edited, so never
 * persisted again beyond what Navigation's own `SavedStateHandle`
 * already restores for free. Every field below *is* user-editable and
 * is persisted (see `RequestFormViewModel.kt`'s `persist`) so rotation/
 * process death never loses what the patient already typed.
 * [submittedRequestId] is set once [RequestFormAction.SubmitClicked]
 * actually succeeds -- [com.aarogya.sanjeevani.patient.booking.SuccessRoute]
 * reads it from this same shared state rather than a nav arg of its
 * own (see `PatientRoutes.kt`'s doc comment on [com.aarogya.sanjeevani.patient.navigation.RequestForm]).
 */
data class RequestFormUiState(
    val serviceId: String,
    val serviceName: String,
    val doctorId: String?,
    val doctorName: String?,
    val slotStart: Instant?,
    val slotDurationMinutes: Int?,
    val today: LocalDate,
    val contextState: RequestFormContextState = RequestFormContextState.Loading,
    val selectedMode: ServiceMode? = null,
    val contactPhone: String = "",
    val contactPhoneTouched: Boolean = false,
    val address: String = "",
    val addressTouched: Boolean = false,
    val notes: String = "",
    val submitAttempted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submittedRequestId: String? = null,
    val cancellationNoticeMinutes: Int? = null,
    val maxHorizonDays: Int? = null,
    val selectedDate: LocalDate? = null,
    val selectedTimeWindow: TimeWindow? = null,
) {
    val fieldErrors: Map<String, RequestFormFieldError>
        get() = RequestFormValidation.validate(selectedMode, contactPhone, address, notes)

    /** True for a service with no doctor (so no availability-grid slot
     *  ever arrives as a nav arg) -- the only case where this screen
     *  itself has to ask for a preferred date and time window. */
    val needsSelfScheduledSlot: Boolean
        get() = doctorId == null && slotStart == null

    /** The doctor-flow's pre-assigned [slotStart] if there is one,
     *  otherwise [selectedDate] + [selectedTimeWindow] combined into one
     *  concrete instant for a self-scheduled, no-doctor request -- the
     *  one value [RequestFormViewModel.submit] actually sends as
     *  `p_slot_start`. Null until both parts of a self-scheduled pick
     *  are in. */
    val effectiveSlotStart: Instant?
        get() =
            slotStart ?: selectedDate?.let { date ->
                selectedTimeWindow?.let { window ->
                    LocalDateTime(date, LocalTime(hour = window.representativeHour, minute = 0))
                        .toInstant(CLINIC_TIME_ZONE)
                }
            }

    val isValid: Boolean
        get() = selectedMode != null && fieldErrors.isEmpty() && effectiveSlotStart != null
}

/**
 * [NavigateToReview] fires once [RequestFormAction.ContinueClicked]'s
 * own validation passes. The submit-outcome events are each named for
 * the recovery action the screen takes, not the raw backend code --
 * see `RequestFormViewModel.kt`'s own mapping of `error-codes.md`'s
 * AS0xx registry onto these four. All four (bar [NavigateToSuccess])
 * carry the [AppError] itself, never pre-resolved text -- resolving to
 * a message only happens at the UI edge, same rule every screen in
 * this app follows.
 */
sealed interface RequestFormUiEvent {
    data object NavigateToReview : RequestFormUiEvent

    data class SlotUnavailable(val error: AppError) : RequestFormUiEvent

    data class DoctorUnavailable(val error: AppError) : RequestFormUiEvent

    data class LimitExceeded(val error: AppError) : RequestFormUiEvent

    data class RetryableError(val error: AppError) : RequestFormUiEvent

    data object NavigateToSuccess : RequestFormUiEvent
}

sealed interface RequestFormAction {
    data object Retry : RequestFormAction

    data class ModeSelected(val mode: ServiceMode) : RequestFormAction

    /** Only meaningful when [RequestFormUiState.needsSelfScheduledSlot]. */
    data class DateSelected(val date: LocalDate) : RequestFormAction

    /** Only meaningful when [RequestFormUiState.needsSelfScheduledSlot]. */
    data class TimeWindowSelected(val window: TimeWindow) : RequestFormAction

    data class ContactPhoneChanged(val value: String) : RequestFormAction

    data object ContactPhoneFocusLost : RequestFormAction

    data class AddressChanged(val value: String) : RequestFormAction

    data object AddressFocusLost : RequestFormAction

    data class NotesChanged(val value: String) : RequestFormAction

    /** Validates and, only if [RequestFormUiState.isValid], sends
     *  [RequestFormUiEvent.NavigateToReview] -- otherwise just marks
     *  every field touched so their errors show, same pattern as
     *  `SignUpViewModel`'s own submit guard. */
    data object ContinueClicked : RequestFormAction

    /** The real [com.aarogya.sanjeevani.core.data.requests.RequestsRepository.createRequest]
     *  call -- fired from [com.aarogya.sanjeevani.patient.booking.ReviewRoute]'s
     *  own Submit button, guarded by [RequestFormUiState.isSubmitting]
     *  so a double tap can never submit twice. */
    data object SubmitClicked : RequestFormAction
}
