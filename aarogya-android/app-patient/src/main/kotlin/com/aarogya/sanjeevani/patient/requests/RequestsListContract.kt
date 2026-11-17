package com.aarogya.sanjeevani.patient.requests

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.requests.PatientRequest

/** The two tabs this screen offers -- never a raw [com.aarogya.sanjeevani.core.data.model.RequestStatus]
 *  set shown directly, since "which three statuses count as 'Active'"
 *  is this screen's own grouping choice, not a backend concept. */
enum class RequestsTab {
    ACTIVE,
    PAST,
}

/**
 * [Content.active]/[Content.past] are already sorted and already
 * sliced to however much of each tab's own local page is currently
 * "visible" ([RequestsListViewModel]'s own [com.aarogya.sanjeevani.patient.requests.RequestsListViewModel.PAGE_SIZE]
 * window) -- there's no backend pagination for `patient_requests_view`
 * (client-contract.md §8: "no parameters"), so this is the honest
 * client-side equivalent: fetch everything once, reveal it in pages as
 * the list scrolls, same infinite-scroll *feel* as `DoctorsDirectoryScreen.kt`'s
 * real, network-paginated one. [hasMoreActive]/[hasMorePast] are each
 * tracked separately since switching tabs doesn't re-fetch anything.
 */
sealed interface RequestsListSectionState {
    data object Loading : RequestsListSectionState

    data class Error(val error: AppError) : RequestsListSectionState

    data class Content(
        val active: List<PatientRequest>,
        val past: List<PatientRequest>,
        val hasMoreActive: Boolean,
        val hasMorePast: Boolean,
    ) : RequestsListSectionState
}

data class RequestsListUiState(
    val section: RequestsListSectionState = RequestsListSectionState.Loading,
    val selectedTab: RequestsTab = RequestsTab.ACTIVE,
    val isRefreshing: Boolean = false,
)

/** Sent only for [RequestsListAction.Refresh] landing on stale data --
 *  same "a silent/resume refresh never surfaces an error" rule
 *  `HomeViewModel`'s own doc comment explains, reused here verbatim. */
sealed interface RequestsListUiEvent {
    data object ShowRefreshFailedMessage : RequestsListUiEvent
}

sealed interface RequestsListAction {
    /** First appearance and every later resume -- see `RequestsListScreen.kt`'s
     *  `LifecycleEventEffect`. */
    data object Resumed : RequestsListAction

    data object Refresh : RequestsListAction

    data object Retry : RequestsListAction

    data class TabSelected(val tab: RequestsTab) : RequestsListAction

    /** Fired once the currently-selected tab's list scrolls near its
     *  own end -- a no-op once that tab has nothing further to reveal. */
    data object LoadMore : RequestsListAction
}
