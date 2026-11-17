package com.aarogya.sanjeevani.patient.requests

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 20
private val ACTIVE_STATUSES = setOf(RequestStatus.REQUESTED, RequestStatus.ACCEPTED, RequestStatus.CONFIRMED)
private val PAST_STATUSES = setOf(RequestStatus.COMPLETED, RequestStatus.REJECTED, RequestStatus.CANCELLED)

/**
 * Drives the Requests tab. [allRequests] is this screen's own full,
 * unsliced snapshot of `getMyRequestsAsPatient()` -- [buildContent]
 * derives both tabs' sorted, paginated views from it on every change,
 * so [RequestsListAction.TabSelected]/[RequestsListAction.LoadMore]
 * never need to re-fetch anything, only re-slice what's already here.
 */
@HiltViewModel
class RequestsListViewModel
    @Inject
    constructor(
        private val requestsRepository: RequestsRepository,
    ) : BaseViewModel<RequestsListUiState, RequestsListUiEvent, RequestsListAction>(RequestsListUiState()) {
        private var allRequests: List<PatientRequest> = emptyList()
        private var visibleActiveCount = PAGE_SIZE
        private var visiblePastCount = PAGE_SIZE

        override fun onAction(action: RequestsListAction) {
            when (action) {
                RequestsListAction.Resumed -> load(isUserInitiated = false)
                RequestsListAction.Refresh -> load(isUserInitiated = true)
                RequestsListAction.Retry -> {
                    setState { copy(section = RequestsListSectionState.Loading) }
                    load(isUserInitiated = false)
                }
                is RequestsListAction.TabSelected -> setState { copy(selectedTab = action.tab) }
                RequestsListAction.LoadMore -> loadMore()
            }
        }

        private fun load(isUserInitiated: Boolean) {
            if (isUserInitiated) setState { copy(isRefreshing = true) }
            viewModelScope.launch {
                when (val result = requestsRepository.getMyRequestsAsPatient()) {
                    is AppResult.Success -> {
                        allRequests = result.data
                        visibleActiveCount = PAGE_SIZE
                        visiblePastCount = PAGE_SIZE
                        setState { copy(section = buildContent(), isRefreshing = false) }
                    }
                    is AppResult.Failure -> {
                        val staysOnContent = currentState.section is RequestsListSectionState.Content
                        setState {
                            copy(
                                section =
                                    if (staysOnContent) section else RequestsListSectionState.Error(result.error),
                                isRefreshing = false,
                            )
                        }
                        if (isUserInitiated && staysOnContent) sendEvent(RequestsListUiEvent.ShowRefreshFailedMessage)
                    }
                }
            }
        }

        private fun loadMore() {
            val content = currentState.section as? RequestsListSectionState.Content ?: return
            when (currentState.selectedTab) {
                RequestsTab.ACTIVE -> if (content.hasMoreActive) visibleActiveCount += PAGE_SIZE
                RequestsTab.PAST -> if (content.hasMorePast) visiblePastCount += PAGE_SIZE
            }
            setState { copy(section = buildContent()) }
        }

        private fun buildContent(): RequestsListSectionState.Content {
            val active = allRequests.filter { it.status in ACTIVE_STATUSES }.sortedBy { it.scheduledStart }
            val past = allRequests.filter { it.status in PAST_STATUSES }.sortedByDescending { it.scheduledStart }
            return RequestsListSectionState.Content(
                active = active.take(visibleActiveCount),
                past = past.take(visiblePastCount),
                hasMoreActive = active.size > visibleActiveCount,
                hasMorePast = past.size > visiblePastCount,
            )
        }
    }
