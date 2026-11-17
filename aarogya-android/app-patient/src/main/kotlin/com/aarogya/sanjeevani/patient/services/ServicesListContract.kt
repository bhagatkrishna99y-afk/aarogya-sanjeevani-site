package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory

/** The catalog load as a whole -- categories and services are fetched
 *  together and treated as one section (unlike Home's screen, nothing
 *  here needs independent per-section failure/retry), since the list
 *  can't render meaningfully with only one of the two. */
sealed interface ServicesListSectionState {
    data object Loading : ServicesListSectionState

    data class Error(val error: AppError) : ServicesListSectionState

    data class Success(val categories: List<ServiceCategory>, val services: List<Service>) : ServicesListSectionState
}

/**
 * [groups] is the screen's actual render target once [section] is
 * [ServicesListSectionState.Success] -- [ServicesListViewModel] keeps it
 * precomputed (via [groupServicesForDisplay]) rather than having the
 * screen re-derive it, so the screen stays a pure function of state
 * exactly like every other screen in this app. [searchQuery] is the
 * immediate, un-debounced text as typed (what the search field itself
 * shows); the debounced value that actually drove [groups] isn't
 * exposed separately since nothing renders it on its own.
 */
data class ServicesListUiState(
    val section: ServicesListSectionState = ServicesListSectionState.Loading,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val groups: List<ServiceGroup> = emptyList(),
)

/** Everything the user (or the screen's own first-composition effect)
 *  can trigger here, handled in [ServicesListViewModel.onAction]. */
sealed interface ServicesListAction {
    /** Fired once, on first composition -- [categoryId] is the nav arg
     *  Home's category row (or the bottom bar's own tab switch, with
     *  `null`) arrived with. */
    data class Initialize(val categoryId: String?) : ServicesListAction

    data object Retry : ServicesListAction

    data class SearchQueryChanged(val query: String) : ServicesListAction

    /** `null` (the "All" chip) clears the filter. Applied immediately,
     *  unlike [SearchQueryChanged] -- a discrete tap needs no debounce. */
    data class CategorySelected(val categoryId: String?) : ServicesListAction

    /** The "no results" empty state's own action -- resets both filters
     *  back to "everything, unfiltered" in one step. */
    data object ClearFilters : ServicesListAction
}
