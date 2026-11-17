package com.aarogya.sanjeevani.patient.home

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.services.ServiceCategory

/**
 * One independently-loadable section of the Home screen -- categories,
 * requests and doctors each carry their own [HomeSectionState], exactly
 * so one section's [Error] never has to imply (or wait on) the other
 * two. [Error] always carries the *new* failure; on a silent refresh
 * (pull-to-refresh, screen resume) that would otherwise clobber data
 * already on screen, [HomeViewModel] deliberately never transitions a
 * section from [Success] back to [Error] -- see its own doc comment.
 */
sealed interface HomeSectionState<out T> {
    data object Loading : HomeSectionState<Nothing>

    data class Success<T>(val data: T) : HomeSectionState<T>

    data class Error(val error: AppError) : HomeSectionState<Nothing>
}

/**
 * Everything [HomeScreen] needs to render. [greetingName] is a
 * deliberately plain field, not a [HomeSectionState] of its own: it has
 * no dedicated error/retry UI (a failed load just quietly keeps showing
 * the last good value, or a generic fallback on the very first load --
 * see [HomeViewModel]), so the extra ceremony of a full section state
 * would have nowhere to actually show up on screen. The unread
 * notification badge shown on this screen's bell is *not* part of this
 * state -- it's the one, app-wide
 * [com.aarogya.sanjeevani.patient.notifications.NotificationBadgeViewModel]'s
 * own count, passed into [HomeRoute] as a plain parameter, so the bell
 * and the bottom bar's Notifications tab can never disagree.
 */
data class HomeUiState(
    val greetingName: String? = null,
    val categories: HomeSectionState<List<ServiceCategory>> = HomeSectionState.Loading,
    val requests: HomeSectionState<List<PatientRequest>> = HomeSectionState.Loading,
    val doctors: HomeSectionState<List<DoctorPublic>> = HomeSectionState.Loading,
    val isRefreshing: Boolean = false,
)

/** One-off effects [HomeRoute] reacts to exactly once. */
sealed interface HomeUiEvent {
    /** Sent once per pull-to-refresh that left at least one section
     *  showing stale data because its own refresh failed -- never sent
     *  for the silent, resume-triggered refresh (see [HomeViewModel]),
     *  since that one was never a user-visible request to begin with. */
    data object ShowRefreshFailedMessage : HomeUiEvent
}

/** Everything the user (or the screen's own lifecycle) can trigger
 *  here, handled in [HomeViewModel.onAction]. */
sealed interface HomeAction {
    /** The screen becoming visible for the first time, or again after
     *  being backgrounded -- see `HomeScreen.kt`'s `LifecycleEventEffect`. */
    data object Resumed : HomeAction

    /** The pull-to-refresh gesture. */
    data object Refresh : HomeAction

    data object RetryCategories : HomeAction

    data object RetryRequests : HomeAction

    data object RetryDoctors : HomeAction
}
