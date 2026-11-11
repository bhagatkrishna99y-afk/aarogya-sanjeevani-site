package com.aarogyasanjeevani.patient.ui.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.patient.data.repository.CatalogRepository
import com.aarogyasanjeevani.patient.data.repository.DoctorRepository
import com.aarogyasanjeevani.patient.data.repository.RequestRepository
import com.aarogyasanjeevani.patient.domain.model.AvailableSlot
import com.aarogyasanjeevani.patient.domain.model.Doctor
import com.aarogyasanjeevani.patient.domain.model.Service
import com.aarogyasanjeevani.patient.ui.common.UiState
import com.aarogyasanjeevani.patient.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject

data class BookingUiState(
    val service: Service? = null,
    val doctor: Doctor? = null,
    val doctorsState: UiState<List<Doctor>> = UiState.Loading,
    val selectedDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val slotsState: UiState<List<AvailableSlot>> = UiState.Success(emptyList()),
    val selectedSlot: AvailableSlot? = null,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSucceeded: Boolean = false,
)

@HiltViewModel
class BookingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val doctorRepository: DoctorRepository,
    private val requestRepository: RequestRepository,
) : ViewModel() {

    private val serviceId: String = checkNotNull(savedStateHandle[Routes.ARG_SERVICE_ID])

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        loadService()
        loadDoctorsIfNeeded()
    }

    private fun loadService() {
        viewModelScope.launch {
            catalogRepository.fetchService(serviceId).onSuccess { service ->
                _uiState.value = _uiState.value.copy(service = service)
            }
        }
    }

    private fun loadDoctorsIfNeeded() {
        viewModelScope.launch {
            doctorRepository.fetchDoctorsForService(serviceId)
                .onSuccess { _uiState.value = _uiState.value.copy(doctorsState = UiState.Success(it)) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        doctorsState = UiState.Error(it.message ?: "Couldn't load doctors."),
                    )
                }
        }
    }

    fun selectDoctor(doctor: Doctor) {
        _uiState.value = _uiState.value.copy(doctor = doctor)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date, selectedSlot = null)
    }

    /**
     * Doctor-bound services ask the backend for real availability. Services
     * that don't require a doctor have no doctor-scoped schedule to query at
     * all (the backend models availability per doctor), so the patient
     * instead picks from a fixed set of half-hourly slots during standard
     * hours -- still subject to the "must be in the future" check in
     * [RequestRepository.createRequest]; the double-booking exclusion
     * constraint only applies when a doctor is assigned, so there is no
     * conflict to check here.
     */
    fun loadSlots() {
        val state = _uiState.value
        val doctor = state.doctor
        val service = state.service ?: return

        if (doctor == null) {
            _uiState.value = state.copy(slotsState = UiState.Success(generateFixedHourSlots(state.selectedDate, service.durationMinutes)))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(slotsState = UiState.Loading)
            doctorRepository.fetchAvailableSlots(doctor.id, serviceId, state.selectedDate)
                .onSuccess { _uiState.value = _uiState.value.copy(slotsState = UiState.Success(it)) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        slotsState = UiState.Error(it.message ?: "Couldn't load available slots."),
                    )
                }
        }
    }

    private fun generateFixedHourSlots(date: LocalDate, durationMinutes: Int): List<AvailableSlot> {
        val now = Clock.System.now()
        val zone = TimeZone.currentSystemDefault()
        val slots = mutableListOf<AvailableSlot>()
        var minutesFromMidnight = 8 * 60 // 08:00
        val closingMinutes = 18 * 60 // 18:00
        while (minutesFromMidnight + durationMinutes <= closingMinutes) {
            val start = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, minutesFromMidnight / 60, minutesFromMidnight % 60)
                .toInstant(zone)
            if (start > now) {
                slots += AvailableSlot(start = start, end = start + durationMinutes.minutes)
            }
            minutesFromMidnight += 30
        }
        return slots
    }

    fun selectSlot(slot: AvailableSlot) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    fun updateNotes(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun submit() {
        val state = _uiState.value
        val slot = state.selectedSlot ?: return
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, submitError = null)
            requestRepository.createRequest(
                serviceId = serviceId,
                doctorId = state.doctor?.id,
                scheduledStart = slot.start,
                patientNotes = state.notes,
            )
                .onSuccess { _uiState.value = _uiState.value.copy(isSubmitting = false, submitSucceeded = true) }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        submitError = it.message ?: "Couldn't submit your request.",
                    )
                }
        }
    }
}
