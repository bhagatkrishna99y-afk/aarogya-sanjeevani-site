package com.aarogyasanjeevani.patient.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.patient.data.repository.RequestRepository
import com.aarogyasanjeevani.patient.domain.model.BookingRequest
import com.aarogyasanjeevani.patient.domain.model.RequestStatus
import com.aarogyasanjeevani.patient.domain.model.RequestStatusEvent
import com.aarogyasanjeevani.patient.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<BookingRequest>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<BookingRequest>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            requestRepository.fetchMyRequests()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load your requests.") }
        }
    }
}

@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
) : ViewModel() {

    private val _request = MutableStateFlow<UiState<BookingRequest>>(UiState.Loading)
    val request: StateFlow<UiState<BookingRequest>> = _request.asStateFlow()

    private val _history = MutableStateFlow<List<RequestStatusEvent>>(emptyList())
    val history: StateFlow<List<RequestStatusEvent>> = _history.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError: StateFlow<String?> = _cancelError.asStateFlow()

    fun load(requestId: String) {
        viewModelScope.launch {
            _request.value = UiState.Loading
            requestRepository.fetchMyRequests()
                .onSuccess { requests ->
                    val match = requests.find { it.id == requestId }
                    _request.value = if (match != null) {
                        UiState.Success(match)
                    } else {
                        UiState.Error("This request could not be found.")
                    }
                }
                .onFailure { _request.value = UiState.Error(it.message ?: "Couldn't load this request.") }

            requestRepository.fetchStatusHistory(requestId).onSuccess { _history.value = it }
        }
    }

    fun cancel(requestId: String, reason: String?, onCancelled: () -> Unit) {
        if (_isCancelling.value) return
        viewModelScope.launch {
            _isCancelling.value = true
            _cancelError.value = null
            requestRepository.cancelRequest(requestId, reason)
                .onSuccess {
                    _isCancelling.value = false
                    onCancelled()
                }
                .onFailure {
                    _isCancelling.value = false
                    _cancelError.value = it.message ?: "Couldn't cancel this request."
                }
        }
    }
}

fun RequestStatus.label(): String = when (this) {
    RequestStatus.REQUESTED -> "Requested"
    RequestStatus.ACCEPTED -> "Accepted"
    RequestStatus.CONFIRMED -> "Confirmed"
    RequestStatus.COMPLETED -> "Completed"
    RequestStatus.REJECTED -> "Rejected"
    RequestStatus.CANCELLED -> "Cancelled"
}
