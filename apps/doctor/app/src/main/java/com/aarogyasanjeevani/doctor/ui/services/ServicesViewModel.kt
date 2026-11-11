package com.aarogyasanjeevani.doctor.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.ServicesRepository
import com.aarogyasanjeevani.doctor.domain.model.Service
import com.aarogyasanjeevani.doctor.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServicesUiState(
    val services: List<Service> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val servicesRepository: ServicesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ServicesUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ServicesUiState>> = _uiState.asStateFlow()

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
            val servicesResult = servicesRepository.fetchBookableServices()
            val selectedResult = servicesRepository.fetchMySelectedServiceIds()

            if (servicesResult.isFailure) {
                _uiState.value = UiState.Error(servicesResult.exceptionOrNull()?.message ?: "Couldn't load services.")
                return@launch
            }
            if (selectedResult.isFailure) {
                _uiState.value = UiState.Error(selectedResult.exceptionOrNull()?.message ?: "Couldn't load your services.")
                return@launch
            }

            _uiState.value = UiState.Success(
                ServicesUiState(
                    services = servicesResult.getOrThrow(),
                    selectedIds = selectedResult.getOrThrow(),
                ),
            )
        }
    }

    fun toggle(serviceId: String) {
        val current = _uiState.value
        if (current !is UiState.Success) return
        val selected = current.data.selectedIds
        val updated = if (serviceId in selected) selected - serviceId else selected + serviceId
        _uiState.value = UiState.Success(current.data.copy(selectedIds = updated))
    }

    fun save() {
        val current = _uiState.value
        if (current !is UiState.Success || _isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            _message.value = null
            servicesRepository.updateMyServices(current.data.selectedIds)
                .onSuccess {
                    _isSaving.value = false
                    _message.value = "Services updated."
                }
                .onFailure {
                    _isSaving.value = false
                    _message.value = it.message ?: "Couldn't update your services."
                }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
