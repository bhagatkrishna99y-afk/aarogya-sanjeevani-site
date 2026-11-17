package com.aarogya.sanjeevani.patient.services

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.services.ServicesRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Service Detail screen. [Retry]'s plain full reload and
 * [com.aarogya.sanjeevani.patient.services.ServiceDetailAction.Resumed]'s
 * silent background re-check share one [refresh] -- the only difference
 * is whether [ServiceDetailUiState.Content] is already on screen (see
 * [refresh]'s own comment for exactly how each of [AppResult]'s two
 * outcomes is handled differently depending on that).
 */
@HiltViewModel
class ServiceDetailViewModel
    @Inject
    constructor(
        private val servicesRepository: ServicesRepository,
    ) : BaseViewModel<ServiceDetailUiState, Nothing, ServiceDetailAction>(ServiceDetailUiState.Loading) {
        private var serviceId: String? = null

        override fun onAction(action: ServiceDetailAction) {
            when (action) {
                is ServiceDetailAction.Resumed -> {
                    serviceId = action.serviceId
                    refresh()
                }
                ServiceDetailAction.Retry -> refresh()
            }
        }

        private fun refresh() {
            val id = serviceId ?: return
            val previous = currentState as? ServiceDetailUiState.Content
            if (previous == null) setState { ServiceDetailUiState.Loading }

            viewModelScope.launch {
                when (val result = servicesRepository.getService(id)) {
                    is AppResult.Success -> {
                        val categoryName = resolveCategoryName(result.data.categoryId)
                        setState { ServiceDetailUiState.Content(service = result.data, categoryName = categoryName) }
                    }
                    is AppResult.Failure ->
                        when {
                            // Was showing content, and it's now gone from the
                            // active set -- a real, meaningful change, not a
                            // transient failure: keep the last-known details
                            // on screen, just mark them unavailable.
                            previous != null && result.error is AppError.NotFound ->
                                setState { previous.copy(isUnavailable = true) }
                            // Was showing content, but this was some other
                            // (likely transient) failure -- never regress
                            // away from content the patient is already
                            // looking at over a background re-check.
                            previous != null -> Unit
                            else -> setState { ServiceDetailUiState.Error(result.error) }
                        }
                }
            }
        }

        private suspend fun resolveCategoryName(categoryId: String): String? =
            when (val result = servicesRepository.getServiceCategories()) {
                is AppResult.Success -> result.data.firstOrNull { it.id == categoryId }?.name
                is AppResult.Failure -> (currentState as? ServiceDetailUiState.Content)?.categoryName
            }
    }
