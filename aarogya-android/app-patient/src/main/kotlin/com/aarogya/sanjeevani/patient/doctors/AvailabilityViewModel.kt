package com.aarogya.sanjeevani.patient.doctors

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val PROBE_WINDOW_DAYS = 7

/** One [RequestsRepository.getOpenSlots] query, keyed so [Resumed]/
 *  [Retry] can force a re-fetch of the *same* date (a plain
 *  [LocalDate] alone wouldn't change value, and a [kotlinx.coroutines.flow.MutableStateFlow]
 *  never re-emits an unchanged value). */
private data class SlotsQuery(val date: LocalDate, val nonce: Int)

/**
 * Drives the Availability screen. Never computes availability or
 * conflicts itself -- every slot shown came straight from
 * `get_doctor_open_slots`; this class only decides *when* to call it
 * and how to fold the result into [AvailabilityUiState].
 *
 * [slotsQueryFlow] -> [kotlinx.coroutines.flow.flatMapLatest] is what
 * [AvailabilityAction.DateSelected] rides on: picking a new date before
 * the previous one's call has returned cancels that call outright
 * (flatMapLatest's own contract), so a slow response for a date the
 * patient has already moved on from can never land after a newer one
 * and clobber it.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AvailabilityViewModel
    @Inject
    constructor(
        private val requestsRepository: RequestsRepository,
        private val doctorsRepository: DoctorsRepository,
        clock: Clock,
    ) : BaseViewModel<AvailabilityUiState, AvailabilityUiEvent, AvailabilityAction>(
            AvailabilityUiState(today = clock.now().toLocalDateTime(CLINIC_TIME_ZONE).date),
        ) {
        private var doctorId: String? = null
        private var doctorAcceptingRequests = true
        private var nonce = 0
        private var probeJob: Job? = null
        private val slotsQueryFlow = MutableStateFlow(SlotsQuery(currentState.selectedDate, nonce))

        init {
            viewModelScope.launch {
                slotsQueryFlow
                    .flatMapLatest { query -> fetchSlotsFlow(query) }
                    .collectLatest { result -> applySlotsResult(result) }
            }
            viewModelScope.launch {
                when (val result = requestsRepository.getBookingSettings()) {
                    is AppResult.Success -> setState { copy(maxHorizonDays = result.data.maxHorizonDays) }
                    is AppResult.Failure -> Unit // non-critical -- keeps the 14-day default.
                }
            }
        }

        private fun fetchSlotsFlow(query: SlotsQuery) =
            flow {
                val id = doctorId
                if (id == null) return@flow
                emit(requestsRepository.getOpenSlots(id, query.date))
            }

        override fun onAction(action: AvailabilityAction) {
            when (action) {
                is AvailabilityAction.Resumed -> {
                    doctorId = action.doctorId
                    refreshDoctorStatus(action.doctorId)
                    requestSlots(currentState.selectedDate)
                }
                is AvailabilityAction.DateSelected -> {
                    setState { copy(selectedDate = action.date, selectedSlot = null) }
                    requestSlots(action.date)
                }
                AvailabilityAction.Retry -> requestSlots(currentState.selectedDate)
                is AvailabilityAction.SlotSelected -> setState { copy(selectedSlot = action.slot) }
                AvailabilityAction.ContinueClicked -> continueClicked()
            }
        }

        private fun requestSlots(date: LocalDate) {
            nonce++
            setState { copy(slotsSection = SlotsSectionState.Loading) }
            slotsQueryFlow.value = SlotsQuery(date, nonce)
        }

        private fun refreshDoctorStatus(doctorId: String) {
            viewModelScope.launch {
                when (val result = doctorsRepository.getDoctor(doctorId)) {
                    is AppResult.Success -> doctorAcceptingRequests = result.data.acceptingRequests
                    is AppResult.Failure -> Unit // keeps the last-known value; not this screen's load to fail over.
                }
            }
        }

        private fun applySlotsResult(result: AppResult<List<OpenSlot>>) {
            probeJob?.cancel()
            when (result) {
                is AppResult.Failure -> setState { copy(slotsSection = SlotsSectionState.Error(result.error)) }
                is AppResult.Success -> applySlots(result.data)
            }
        }

        private fun applySlots(slots: List<OpenSlot>) {
            val previousSelection = currentState.selectedSlot
            val selectionStillThere =
                previousSelection == null || slots.any { it.slotStart == previousSelection.slotStart }

            val newSection =
                when {
                    slots.isNotEmpty() -> SlotsSectionState.Content(slots)
                    !doctorAcceptingRequests -> SlotsSectionState.DoctorUnavailable
                    else -> SlotsSectionState.Empty()
                }
            setState {
                copy(slotsSection = newSection, selectedSlot = if (selectionStillThere) previousSelection else null)
            }
            if (!selectionStillThere) sendEvent(AvailabilityUiEvent.SlotTaken)
            if (newSection is SlotsSectionState.Empty) startProbe(from = currentState.selectedDate)
        }

        private fun startProbe(from: LocalDate) {
            val doctorIdSnapshot = doctorId ?: return
            probeJob =
                viewModelScope.launch {
                    val horizonLimit = currentState.today.plus(currentState.maxHorizonDays, DateTimeUnit.DAY)
                    val probeLimit = minOf(from.plus(PROBE_WINDOW_DAYS, DateTimeUnit.DAY), horizonLimit)
                    var probeDate = from.plus(1, DateTimeUnit.DAY)
                    while (probeDate <= probeLimit) {
                        val result = requestsRepository.getOpenSlots(doctorIdSnapshot, probeDate)
                        if (result is AppResult.Success && result.data.isNotEmpty()) {
                            val latest = currentState.slotsSection as? SlotsSectionState.Empty ?: return@launch
                            setState { copy(slotsSection = latest.copy(suggestedDate = probeDate)) }
                            return@launch
                        }
                        probeDate = probeDate.plus(1, DateTimeUnit.DAY)
                    }
                }
        }

        private fun continueClicked() {
            val id = doctorId
            val selected = currentState.selectedSlot
            if (id == null || selected == null || currentState.isContinuing) return

            setState { copy(isContinuing = true) }
            viewModelScope.launch {
                when (val result = requestsRepository.getOpenSlots(id, currentState.selectedDate)) {
                    is AppResult.Failure -> setState { copy(isContinuing = false) }
                    is AppResult.Success -> {
                        val freshSlot = result.data.firstOrNull { it.slotStart == selected.slotStart }
                        if (freshSlot == null) {
                            applySlots(result.data)
                            setState { copy(isContinuing = false) }
                        } else {
                            setState { copy(isContinuing = false) }
                            val durationMinutes = (freshSlot.slotEnd - freshSlot.slotStart).inWholeMinutes.toInt()
                            sendEvent(AvailabilityUiEvent.NavigateToRequestForm(freshSlot.slotStart, durationMinutes))
                        }
                    }
                }
            }
        }
    }
