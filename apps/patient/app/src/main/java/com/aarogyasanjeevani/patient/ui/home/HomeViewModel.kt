package com.aarogyasanjeevani.patient.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.patient.data.repository.CatalogRepository
import com.aarogyasanjeevani.patient.domain.model.ServiceCategory
import com.aarogyasanjeevani.patient.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ServiceCategory>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ServiceCategory>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            catalogRepository.fetchCatalog()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load services.") }
        }
    }
}
