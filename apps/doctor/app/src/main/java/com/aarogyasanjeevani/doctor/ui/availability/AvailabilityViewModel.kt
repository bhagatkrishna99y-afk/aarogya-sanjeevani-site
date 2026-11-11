package com.aarogyasanjeevani.doctor.ui.availability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.AvailabilityRepository
import com.aarogyasanjeevani.doctor.domain.model.DayAvailability
import com.aarogyasanjeevani.doctor.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject

data class DayEditState(
    val dayOfWeek: Int,
    val enabled: Boolean = false,
    val startTimeText: String = "09:00",
    val endTimeText: String = "17:00",
    val slotMinutesText: String = "30",
)

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val availabilityRepository: AvailabilityRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<DayEditState>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<DayEditState>>> = _uiState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            availabilityRepository.fetchMyWeeklyAvailability()
                .onSuccess { existing ->
                    val byDay = existing.associateBy { it.dayOfWeek }
                    val days = (0..6).map { day ->
                        val match = byDay[day]
                        if (match != null) {
                            DayEditState(
                                dayOfWeek = day,
                                enabled = true,
                                startTimeText = match.startTime.toText(),
                                endTimeText = match.endTime.toText(),
                                slotMinutesText = match.slotDurationMinutes.toString(),
                            )
                        } else {
                            DayEditState(dayOfWeek = day)
                        }
                    }
                    _uiState.value = UiState.Success(days)
                }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load your availability.") }
        }
    }

    private fun LocalTime.toText() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    fun updateDay(dayOfWeek: Int, transform: DayEditState.() -> DayEditState) {
        val current = _uiState.value
        if (current !is UiState.Success) return
        _uiState.value = UiState.Success(current.data.map { if (it.dayOfWeek == dayOfWeek) it.transform() else it })
    }

    fun save() {
        val current = _uiState.value
        if (current !is UiState.Success || _isSaving.value) return

        val enabledDays = current.data.filter { it.enabled }
        val parsed = mutableListOf<DayAvailability>()
        for (day in enabledDays) {
            val start = day.startTimeText.toTimeOrNull()
            val end = day.endTimeText.toTimeOrNull()
            val slotMinutes = day.slotMinutesText.toIntOrNull()
            if (start == null || end == null || slotMinutes == null || slotMinutes <= 0) {
                _message.value = "Please check the time format (HH:MM) and slot duration for every enabled day."
                return
            }
            if (end <= start) {
                _message.value = "End time must be after start time for every enabled day."
                return
            }
            parsed += DayAvailability(day.dayOfWeek, start, end, slotMinutes)
        }

        viewModelScope.launch {
            _isSaving.value = true
            _message.value = null
            availabilityRepository.updateMyWeeklyAvailability(parsed)
                .onSuccess {
                    _isSaving.value = false
                    _message.value = "Availability updated."
                }
                .onFailure {
                    _isSaving.value = false
                    _message.value = it.message ?: "Couldn't update your availability."
                }
        }
    }

    private fun String.toTimeOrNull(): LocalTime? {
        val parts = trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime(hour, minute)
    }

    fun dismissMessage() {
        _message.value = null
    }
}
