package com.aarogya.sanjeevani.patient.requests

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.requests.PatientRequest

/** The three statuses a patient can still cancel from -- the same set
 *  client-contract.md §10's transition table allows a patient to call
 *  `transition_request_status(..., 'cancelled', ...)` from. */
private val CANCELLABLE_STATUSES = setOf(RequestStatus.REQUESTED, RequestStatus.ACCEPTED, RequestStatus.CONFIRMED)

val RequestStatus.isCancellable: Boolean
    get() = this in CANCELLABLE_STATUSES

/** The header's one-line, plain-language explanation of where a
 *  request currently stands -- never the raw status name alone (that's
 *  what [com.aarogya.sanjeevani.core.designsystem.component.StatusChip]
 *  is for), and never anything about *why* internally (no admin notes,
 *  no raw backend text) beyond what [RequestDetailSectionState.Content]'s
 *  own [RequestDetailSectionState.Content.request] already exposes
 *  ([PatientRequest.rejectionReason]/[PatientRequest.cancellationReason]). */
fun RequestStatus.explanation(): String =
    when (this) {
        RequestStatus.REQUESTED -> "Waiting for the doctor to respond."
        RequestStatus.ACCEPTED -> "The doctor has accepted your request -- waiting for final confirmation."
        RequestStatus.CONFIRMED -> "Your appointment is confirmed."
        RequestStatus.COMPLETED -> "This appointment has been completed."
        RequestStatus.REJECTED -> "The doctor wasn't able to accept this request."
        RequestStatus.CANCELLED -> "This request was cancelled."
    }

/** One row of [com.aarogya.sanjeevani.core.data.requests.RequestStatusHistoryEntry],
 *  already resolved to exactly what [com.aarogya.sanjeevani.core.designsystem.component.TimelineItem]
 *  needs -- [actor] is one of "You"/"Doctor"/"System" (never a raw user
 *  id -- see [RequestDetailViewModel.toTimelineEntry]'s own doc comment
 *  for why that mapping has to happen there, not here).
 */
data class RequestTimelineEntry(
    val label: String,
    val actor: String,
    val relativeTime: String,
    val absoluteTime: String,
)

sealed interface RequestDetailSectionState {
    data object Loading : RequestDetailSectionState

    data class Error(val error: AppError) : RequestDetailSectionState

    data class Content(
        val request: PatientRequest,
        val timeline: List<RequestTimelineEntry>,
    ) : RequestDetailSectionState
}

data class RequestDetailUiState(
    val section: RequestDetailSectionState = RequestDetailSectionState.Loading,
    val showCancelDialog: Boolean = false,
    val cancelReason: String = "",
    val isCancelling: Boolean = false,
)

/** [CancelSucceeded] is a plain literal message, resolved nowhere but
 *  `RequestDetailScreen.kt`'s own copy; [CancelConflict]/[CancelFailed]
 *  both carry the real [AppError] to resolve at the UI edge (never in
 *  the ViewModel) -- split into two only because [CancelConflict]
 *  (AS016, "someone/something already changed this") also means the
 *  dialog itself no longer makes sense and should close, where
 *  [CancelFailed] (anything else, e.g. a network blip) should leave it
 *  open so the patient can just tap Confirm again. */
sealed interface RequestDetailUiEvent {
    data object CancelSucceeded : RequestDetailUiEvent

    data class CancelConflict(val error: AppError) : RequestDetailUiEvent

    data class CancelFailed(val error: AppError) : RequestDetailUiEvent
}

sealed interface RequestDetailAction {
    /** Carries [requestId] rather than the ViewModel reading it off a
     *  `SavedStateHandle.toRoute()` itself -- same reasoning as
     *  `AvailabilityAction.Resumed`: [com.aarogya.sanjeevani.patient.navigation.RequestDetail]'s
     *  id is resolved once, on a real `NavBackStackEntry`, by
     *  `PatientNavHost.kt` itself and handed down as a plain
     *  [com.aarogya.sanjeevani.patient.requests.RequestDetailRoute]
     *  parameter -- which also means this ViewModel needs no Bundle at
     *  all to unit-test. */
    data class Resumed(val requestId: String) : RequestDetailAction

    data object Retry : RequestDetailAction

    data object CancelClicked : RequestDetailAction

    data object CancelDismissed : RequestDetailAction

    data class CancelReasonChanged(val value: String) : RequestDetailAction

    data object CancelConfirmed : RequestDetailAction
}
