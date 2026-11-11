package com.aarogyasanjeevani.doctor.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.RequestsRepository
import com.aarogyasanjeevani.doctor.domain.model.CareRequest
import com.aarogyasanjeevani.doctor.domain.model.RequestStatusEvent
import com.aarogyasanjeevani.doctor.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestsViewModel @Inject constructor(
    private val requestsRepository: RequestsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<CareRequest>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CareRequest>>> = _uiState.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actioningId = MutableStateFlow<String?>(null)
    val actioningId: StateFlow<String?> = _actioningId.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            requestsRepository.fetchMyRequests()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load your requests.") }
        }
    }

    fun accept(requestId: String) = runAction(requestId) { requestsRepository.respond(requestId, accept = true, reason = null) }

    fun reject(requestId: String, reason: String) =
        runAction(requestId) { requestsRepository.respond(requestId, accept = false, reason = reason) }

    fun confirm(requestId: String) = runAction(requestId) { requestsRepository.confirm(requestId) }

    fun complete(requestId: String) = runAction(requestId) { requestsRepository.complete(requestId) }

    fun cancel(requestId: String, reason: String?) = runAction(requestId) { requestsRepository.cancel(requestId, reason) }

    private fun runAction(requestId: String, block: suspend () -> Result<Unit>) {
        if (_actioningId.value != null) return
        viewModelScope.launch {
            _actioningId.value = requestId
            _actionError.value = null
            block()
                .onSuccess {
                    _actioningId.value = null
                    load()
                }
                .onFailure {
                    _actioningId.value = null
                    _actionError.value = it.message ?: "Something went wrong."
                }
        }
    }
}

@HiltViewModel
class RequestHistoryViewModel @Inject constructor(
    private val requestsRepository: RequestsRepository,
) : ViewModel() {

    private val _history = MutableStateFlow<List<RequestStatusEvent>>(emptyList())
    val history: StateFlow<List<RequestStatusEvent>> = _history.asStateFlow()

    fun load(requestId: String) {
        viewModelScope.launch {
            requestsRepository.fetchStatusHistory(requestId).onSuccess { _history.value = it }
        }
    }
}
