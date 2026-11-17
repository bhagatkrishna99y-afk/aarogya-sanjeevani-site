package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

private const val DEFAULT_MAX_HORIZON_DAYS = 14

/**
 * The slots grid's own state, independent of the date strip around it
 * -- switching [AvailabilityUiState.selectedDate] always goes through
 * [Loading] again (there's no "silent" variant here the way a resume
 * refresh has: a date change is a deliberate user action, not a
 * background re-check), while [Content]/[DoctorUnavailable]/[Empty]
 * are the three ways a *successful* `get_doctor_open_slots` call can
 * resolve -- see that RPC's own doc comment on why an empty result is
 * never, by itself, [Error].
 */
sealed interface SlotsSectionState {
    data object Loading : SlotsSectionState

    data class Error(val error: AppError) : SlotsSectionState

    /** The doctor stopped accepting requests (or lost approval) by the
     *  time this load ran -- distinguished from [Empty] by a *separate*
     *  [com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository.getDoctor]
     *  read, since `get_doctor_open_slots` itself returns the same
     *  empty list either way (see that RPC's doc comment). */
    data object DoctorUnavailable : SlotsSectionState

    /** Zero slots for [AvailabilityUiState.selectedDate], doctor still
     *  accepting -- [suggestedDate] is filled in lazily, one day at a
     *  time, by a probe that starts once this state is reached (see
     *  [AvailabilityViewModel]); `null` while still probing *or* once
     *  the probe has given up within its own short window -- either
     *  way the screen's fallback action is just "pick another date"
     *  via the date strip/picker. */
    data class Empty(val suggestedDate: LocalDate? = null) : SlotsSectionState

    data class Content(val slots: List<OpenSlot>) : SlotsSectionState
}

data class AvailabilityUiState(
    val today: LocalDate,
    val selectedDate: LocalDate = today,
    val maxHorizonDays: Int = DEFAULT_MAX_HORIZON_DAYS,
    val slotsSection: SlotsSectionState = SlotsSectionState.Loading,
    val selectedSlot: OpenSlot? = null,
    val isContinuing: Boolean = false,
)

/** [SlotTaken] fires both when a silent resume refresh *and* when the
 *  pre-continue re-check finds the previously selected slot gone --
 *  same message either way, since from the patient's point of view
 *  it's the same fact ("someone else took it first"). */
sealed interface AvailabilityUiEvent {
    data object SlotTaken : AvailabilityUiEvent

    data class NavigateToRequestForm(val slotStart: Instant, val slotDurationMinutes: Int) : AvailabilityUiEvent
}

sealed interface AvailabilityAction {
    /** Fires from `LifecycleEventEffect(ON_RESUME)` -- both this
     *  screen's first appearance and every later resume, same
     *  unification [com.aarogya.sanjeevani.patient.services.ServiceDetailViewModel]
     *  uses and for the same reason: re-checking the doctor's own
     *  status on resume is exactly how [SlotsSectionState.DoctorUnavailable]
     *  gets detected if it changes while this screen is already open. */
    data class Resumed(val doctorId: String) : AvailabilityAction

    data class DateSelected(val date: LocalDate) : AvailabilityAction

    data object Retry : AvailabilityAction

    data class SlotSelected(val slot: OpenSlot) : AvailabilityAction

    data object ContinueClicked : AvailabilityAction
}
