package com.aarogya.sanjeevani.patient.requests

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.requests.RequestStatusHistoryEntry
import com.aarogya.sanjeevani.core.data.requests.RequestUpdatesSource
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import com.aarogya.sanjeevani.patient.home.toAsRequestStatus
import com.aarogya.sanjeevani.patient.ui.common.formatRelativeTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AS016_INVALID_TRANSITION = "AS016_INVALID_TRANSITION"

/**
 * Drives the Request Detail screen. There's no single-row "get me this
 * one request" RPC for a patient (client-contract.md §9 composes the
 * same `patient_requests_view` the Requests tab's own full list already
 * reads) -- so [load] re-reads that whole list and finds [requestId] in
 * it client-side, exactly like [RequestsListViewModel] does for its own
 * two tabs. [requestId] itself arrives via [RequestDetailAction.Resumed]
 * rather than a `SavedStateHandle.toRoute()` read in this class's own
 * constructor -- see that action's doc comment for why.
 */
@HiltViewModel
class RequestDetailViewModel
    @Inject
    constructor(
        private val requestsRepository: RequestsRepository,
        private val authRepository: AuthRepository,
        private val requestUpdatesSource: RequestUpdatesSource,
    ) : BaseViewModel<RequestDetailUiState, RequestDetailUiEvent, RequestDetailAction>(RequestDetailUiState()) {
        private var requestId: String? = null
        private var pollingStarted = false

        override fun onAction(action: RequestDetailAction) {
            when (action) {
                is RequestDetailAction.Resumed -> {
                    requestId = action.requestId
                    load()
                    if (!pollingStarted) startPolling(action.requestId)
                }
                RequestDetailAction.Retry -> {
                    setState { copy(section = RequestDetailSectionState.Loading) }
                    load()
                }
                RequestDetailAction.CancelClicked -> setState { copy(showCancelDialog = true, cancelReason = "") }
                RequestDetailAction.CancelDismissed -> setState { copy(showCancelDialog = false) }
                is RequestDetailAction.CancelReasonChanged -> setState { copy(cancelReason = action.value) }
                RequestDetailAction.CancelConfirmed -> cancelRequest()
            }
        }

        /** Started once, the first time this screen resumes -- the
         *  [kotlinx.coroutines.flow.Flow] itself lives in
         *  [viewModelScope], so it's torn down automatically the moment
         *  this screen (and this ViewModel) goes away; there's no
         *  explicit "stop polling" action needed for "while the screen
         *  is open." */
        private fun startPolling(id: String) {
            pollingStarted = true
            viewModelScope.launch {
                requestUpdatesSource.observeRequestUpdates(id).collect { load() }
            }
        }

        private fun load() {
            val id = requestId ?: return
            viewModelScope.launch {
                coroutineScope {
                    val detailDeferred = async { requestsRepository.getMyRequestsAsPatient() }
                    val historyDeferred = async { requestsRepository.getStatusHistory(id) }
                    applyLoad(id, detailDeferred.await(), historyDeferred.await())
                }
            }
        }

        private fun applyLoad(
            id: String,
            detailResult: AppResult<List<PatientRequest>>,
            historyResult: AppResult<List<RequestStatusHistoryEntry>>,
        ) {
            when (detailResult) {
                is AppResult.Failure -> {
                    if (currentState.section !is RequestDetailSectionState.Content) {
                        setState { copy(section = RequestDetailSectionState.Error(detailResult.error)) }
                    }
                }
                is AppResult.Success -> {
                    val request = detailResult.data.firstOrNull { it.id == id }
                    if (request == null) {
                        setState { copy(section = RequestDetailSectionState.Error(AppError.NotFound())) }
                        return
                    }
                    val myUserId = authRepository.currentUserId()
                    val timeline =
                        when (historyResult) {
                            is AppResult.Success -> historyResult.data.map { it.toTimelineEntry(myUserId) }
                            is AppResult.Failure -> currentTimelineOrEmpty()
                        }
                    setState { copy(section = RequestDetailSectionState.Content(request, timeline)) }
                }
            }
        }

        private fun currentTimelineOrEmpty(): List<RequestTimelineEntry> =
            (currentState.section as? RequestDetailSectionState.Content)?.timeline ?: emptyList()

        private fun RequestStatusHistoryEntry.toTimelineEntry(myUserId: String?): RequestTimelineEntry {
            val actor =
                when {
                    changedBy == null -> "System"
                    changedBy == myUserId -> "You"
                    else -> "Doctor"
                }
            return RequestTimelineEntry(
                label = newStatus.toAsRequestStatus().label,
                actor = actor,
                relativeTime = formatRelativeTime(changedAt),
                absoluteTime = formatAbsoluteTimestamp(changedAt),
            )
        }

        private fun cancelRequest() {
            if (currentState.isCancelling) return
            val id = requestId ?: return

            setState { copy(isCancelling = true) }
            viewModelScope.launch {
                val reason = currentState.cancelReason.trim().ifBlank { null }
                when (val result = requestsRepository.transitionStatus(id, RequestStatus.CANCELLED, reason)) {
                    is AppResult.Success -> {
                        setState { copy(isCancelling = false, showCancelDialog = false, cancelReason = "") }
                        load()
                        sendEvent(RequestDetailUiEvent.CancelSucceeded)
                    }
                    is AppResult.Failure -> onCancelFailure(result.error)
                }
            }
        }

        private fun onCancelFailure(error: AppError) {
            val code = (error as? AppError.Business)?.code
            if (code == AS016_INVALID_TRANSITION) {
                setState { copy(isCancelling = false, showCancelDialog = false) }
                load()
                sendEvent(RequestDetailUiEvent.CancelConflict(error))
            } else {
                setState { copy(isCancelling = false) }
                sendEvent(RequestDetailUiEvent.CancelFailed(error))
            }
        }
    }
