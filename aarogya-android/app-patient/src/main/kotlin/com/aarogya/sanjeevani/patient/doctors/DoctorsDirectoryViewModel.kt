package com.aarogya.sanjeevani.patient.doctors

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 20
private const val SEARCH_DEBOUNCE_MILLIS = 300L

/**
 * Drives the Doctors Directory: a debounced search (same 300ms pattern
 * as `ServicesListViewModel`), two filter chips applied immediately,
 * and 20-at-a-time infinite scroll. Every filter/search change restarts
 * pagination from page 1 -- the result set itself changed, so appending
 * a page fetched under the old filter would make no sense.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class DoctorsDirectoryViewModel
    @Inject
    constructor(
        private val doctorsRepository: DoctorsRepository,
    ) : BaseViewModel<DoctorsDirectoryUiState, Nothing, DoctorsDirectoryAction>(DoctorsDirectoryUiState()) {
        private val searchQueryFlow = MutableStateFlow("")
        private var debouncedQuery = ""
        private var offset = 0

        init {
            viewModelScope.launch {
                searchQueryFlow.debounce(SEARCH_DEBOUNCE_MILLIS).collect { query ->
                    debouncedQuery = query
                    loadFirstPage()
                }
            }
        }

        override fun onAction(action: DoctorsDirectoryAction) {
            when (action) {
                DoctorsDirectoryAction.Initialize -> loadFirstPage()
                DoctorsDirectoryAction.Retry -> loadFirstPage()
                is DoctorsDirectoryAction.SearchQueryChanged -> {
                    setState { copy(searchQuery = action.query) }
                    searchQueryFlow.value = action.query
                }
                is DoctorsDirectoryAction.SpecializationSelected -> {
                    setState { copy(selectedSpecialization = action.specialization) }
                    loadFirstPage()
                }
                is DoctorsDirectoryAction.ModeSelected -> {
                    setState { copy(selectedMode = action.mode) }
                    loadFirstPage()
                }
                DoctorsDirectoryAction.LoadMore -> loadMore()
                DoctorsDirectoryAction.RetryLoadMore -> loadMore()
            }
        }

        private fun loadFirstPage() {
            offset = 0
            setState { copy(section = DoctorsDirectorySectionState.Loading) }
            viewModelScope.launch {
                when (val result = fetchPage(offset)) {
                    is AppResult.Success -> applyFirstPage(result.data)
                    is AppResult.Failure ->
                        setState { copy(section = DoctorsDirectorySectionState.Error(result.error)) }
                }
            }
        }

        private suspend fun applyFirstPage(doctors: List<DoctorPublic>) {
            offset = doctors.size
            val (avatarUrls, serviceNames) = enrichPage(doctors)
            setState {
                copy(
                    section =
                        DoctorsDirectorySectionState.Content(
                            doctors = doctors,
                            avatarUrlsByDoctorId = avatarUrls,
                            serviceNamesByDoctorId = serviceNames,
                            hasMore = doctors.size == PAGE_SIZE,
                        ),
                    availableSpecializations = availableSpecializations + doctors.map { it.specialization },
                )
            }
        }

        private fun loadMore() {
            val content = currentState.section as? DoctorsDirectorySectionState.Content ?: return
            if (content.isLoadingMore || !content.hasMore) return

            setState { copy(section = content.copy(isLoadingMore = true, loadMoreError = null)) }
            viewModelScope.launch {
                when (val result = fetchPage(offset)) {
                    is AppResult.Success -> applyNextPage(result.data)
                    is AppResult.Failure -> {
                        val latest = currentState.section as? DoctorsDirectorySectionState.Content ?: return@launch
                        setState { copy(section = latest.copy(isLoadingMore = false, loadMoreError = result.error)) }
                    }
                }
            }
        }

        private suspend fun applyNextPage(newDoctors: List<DoctorPublic>) {
            offset += newDoctors.size
            val (avatarUrls, serviceNames) = enrichPage(newDoctors)
            val latest = currentState.section as? DoctorsDirectorySectionState.Content ?: return
            setState {
                copy(
                    section =
                        latest.copy(
                            doctors = latest.doctors + newDoctors,
                            avatarUrlsByDoctorId = latest.avatarUrlsByDoctorId + avatarUrls,
                            serviceNamesByDoctorId = latest.serviceNamesByDoctorId + serviceNames,
                            isLoadingMore = false,
                            hasMore = newDoctors.size == PAGE_SIZE,
                            loadMoreError = null,
                        ),
                    availableSpecializations = availableSpecializations + newDoctors.map { it.specialization },
                )
            }
        }

        private suspend fun fetchPage(pageOffset: Int): AppResult<List<DoctorPublic>> =
            doctorsRepository.getDoctorsPage(
                offset = pageOffset,
                limit = PAGE_SIZE,
                searchQuery = debouncedQuery,
                specialization = currentState.selectedSpecialization,
                mode = currentState.selectedMode,
            )

        /** One extra query each for avatars and offered-service names per
         *  page, not per doctor -- see `DoctorsRepository.kt`'s own doc
         *  comments on [DoctorsRepository.getAvatarUrls] and
         *  [DoctorsRepository.getServicesForDoctors] for why both are
         *  batched this way. */
        private suspend fun enrichPage(doctors: List<DoctorPublic>): PageEnrichment =
            coroutineScope {
                val avatarDeferred = async { doctorsRepository.getAvatarUrls(doctors.mapNotNull { it.avatarPath }) }
                val servicesDeferred = async { doctorsRepository.getServicesForDoctors(doctors.map { it.doctorId }) }
                val avatarUrls = avatarUrlsByDoctorId(doctors, avatarDeferred.await())
                val serviceNames =
                    when (val result = servicesDeferred.await()) {
                        is AppResult.Success ->
                            result.data
                                .groupBy { it.doctorId }
                                .mapValues { (_, pairings) -> pairings.map { it.serviceName } }
                        is AppResult.Failure -> emptyMap()
                    }
                PageEnrichment(avatarUrls, serviceNames)
            }
    }

private data class PageEnrichment(
    val avatarUrls: Map<String, String>,
    val serviceNames: Map<String, List<String>>,
)
