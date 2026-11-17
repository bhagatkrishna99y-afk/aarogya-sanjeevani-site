package com.aarogya.sanjeevani.patient.services

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.data.services.ServicesRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MILLIS = 300L

/**
 * Drives the Services tab's root list -- one combined load (categories
 * + services, see [ServicesListSectionState]'s doc comment), a category
 * filter applied immediately, and a search query applied after
 * [SEARCH_DEBOUNCE_MILLIS] of no further typing. Both filters (and a
 * fresh [ServicesListSectionState.Success]) recompute [ServicesListUiState.groups]
 * through the same pure [groupServicesForDisplay] -- this class only
 * ever decides *when* to call it, never how.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ServicesListViewModel
    @Inject
    constructor(
        private val servicesRepository: ServicesRepository,
    ) : BaseViewModel<ServicesListUiState, Nothing, ServicesListAction>(ServicesListUiState()) {
        private val searchQueryFlow = MutableStateFlow("")
        private var debouncedQuery = ""

        init {
            viewModelScope.launch {
                searchQueryFlow.debounce(SEARCH_DEBOUNCE_MILLIS).collect { query ->
                    debouncedQuery = query
                    recomputeGroups()
                }
            }
        }

        override fun onAction(action: ServicesListAction) {
            when (action) {
                is ServicesListAction.Initialize -> {
                    setState { copy(selectedCategoryId = action.categoryId) }
                    load()
                }
                ServicesListAction.Retry -> load()
                is ServicesListAction.SearchQueryChanged -> {
                    setState { copy(searchQuery = action.query) }
                    searchQueryFlow.value = action.query
                }
                is ServicesListAction.CategorySelected -> {
                    setState { copy(selectedCategoryId = action.categoryId) }
                    recomputeGroups()
                }
                ServicesListAction.ClearFilters -> {
                    searchQueryFlow.value = ""
                    debouncedQuery = ""
                    setState { copy(searchQuery = "", selectedCategoryId = null) }
                    recomputeGroups()
                }
            }
        }

        private fun load() {
            setState { copy(section = ServicesListSectionState.Loading) }
            viewModelScope.launch {
                var categories: List<ServiceCategory>? = null
                var services: List<Service>? = null
                var firstError: AppError? = null
                coroutineScope {
                    launch {
                        when (val result = servicesRepository.getServiceCategories()) {
                            is AppResult.Success -> categories = result.data
                            is AppResult.Failure -> firstError = result.error
                        }
                    }
                    launch {
                        when (val result = servicesRepository.getServices()) {
                            is AppResult.Success -> services = result.data
                            is AppResult.Failure -> firstError = firstError ?: result.error
                        }
                    }
                }
                val loadedCategories = categories
                val loadedServices = services
                if (loadedCategories != null && loadedServices != null) {
                    setState { copy(section = ServicesListSectionState.Success(loadedCategories, loadedServices)) }
                    recomputeGroups()
                } else {
                    setState { copy(section = ServicesListSectionState.Error(firstError ?: AppError.Unknown())) }
                }
            }
        }

        private fun recomputeGroups() {
            val success = currentState.section as? ServicesListSectionState.Success ?: return
            val groups =
                groupServicesForDisplay(
                    categories = success.categories,
                    services = success.services,
                    selectedCategoryId = currentState.selectedCategoryId,
                    searchQuery = debouncedQuery,
                )
            setState { copy(groups = groups) }
        }
    }
