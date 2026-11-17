package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode

/**
 * [Content.isLoadingMore]/[Content.hasMore]/[Content.loadMoreError] are
 * the infinite-scroll page beyond the first -- kept on [Content] itself
 * (not a nested [DoctorsDirectorySectionState] of their own) since
 * there's no meaningful "loading" or "error" UI for page 2+ that isn't
 * anchored to the list already on screen (a full-screen spinner/error
 * would hide doctors the patient is already looking at). A failed
 * load-more never touches [Content.doctors] -- same "never regress
 * away from content already on screen" rule every other paginated/
 * silently-refreshed screen in this app follows.
 */
sealed interface DoctorsDirectorySectionState {
    data object Loading : DoctorsDirectorySectionState

    data class Error(val error: AppError) : DoctorsDirectorySectionState

    data class Content(
        val doctors: List<DoctorPublic>,
        val avatarUrlsByDoctorId: Map<String, String> = emptyMap(),
        val serviceNamesByDoctorId: Map<String, List<String>> = emptyMap(),
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val loadMoreError: AppError? = null,
    ) : DoctorsDirectorySectionState
}

/** [availableSpecializations] accumulates across every page loaded so
 *  far (union, never shrinks) -- there's no dedicated "distinct
 *  specializations" endpoint, so the filter chip row is necessarily
 *  best-effort from what's actually been seen, same spirit as
 *  `HomeGreeting.kt`'s own best-effort category icon mapping. */
data class DoctorsDirectoryUiState(
    val section: DoctorsDirectorySectionState = DoctorsDirectorySectionState.Loading,
    val searchQuery: String = "",
    val selectedSpecialization: String? = null,
    val selectedMode: ServiceMode? = null,
    val availableSpecializations: Set<String> = emptySet(),
)

/** Everything the user (or the list's own scroll position) can trigger
 *  here, handled in [DoctorsDirectoryViewModel.onAction]. */
sealed interface DoctorsDirectoryAction {
    data object Initialize : DoctorsDirectoryAction

    data object Retry : DoctorsDirectoryAction

    data class SearchQueryChanged(val query: String) : DoctorsDirectoryAction

    /** `null` (the "All" chip) clears the filter. */
    data class SpecializationSelected(val specialization: String?) : DoctorsDirectoryAction

    data class ModeSelected(val mode: ServiceMode?) : DoctorsDirectoryAction

    /** Fired once the list scrolls near its current end. A no-op unless
     *  [DoctorsDirectorySectionState.Content.hasMore] and nothing is
     *  already in flight -- see [DoctorsDirectoryViewModel]'s own guard,
     *  since a fast scroll can fire this several times before the first
     *  page even lands. */
    data object LoadMore : DoctorsDirectoryAction

    data object RetryLoadMore : DoctorsDirectoryAction
}
