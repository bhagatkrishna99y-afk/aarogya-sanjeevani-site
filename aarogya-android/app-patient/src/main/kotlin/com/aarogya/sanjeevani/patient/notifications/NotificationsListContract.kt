package com.aarogya.sanjeevani.patient.notifications

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.notifications.Notification

/** [Content.notifications] is already sliced to [NotificationsListViewModel]'s
 *  own currently-visible local page -- same honest client-side
 *  pagination as [com.aarogya.sanjeevani.patient.requests.RequestsListViewModel],
 *  since `select * from notifications ...` (client-contract.md §11) has
 *  no backend offset/limit either. */
sealed interface NotificationsListSectionState {
    data object Loading : NotificationsListSectionState

    data class Error(val error: AppError) : NotificationsListSectionState

    data class Content(
        val notifications: List<Notification>,
        val hasMore: Boolean,
    ) : NotificationsListSectionState
}

data class NotificationsListUiState(
    val section: NotificationsListSectionState = NotificationsListSectionState.Loading,
    val isRefreshing: Boolean = false,
    val isMarkingAllRead: Boolean = false,
)

/** [UnreadCountMayHaveChanged] fires after every successful mark-read/
 *  mark-all-read call -- `NotificationsListRoute` maps it straight to
 *  the one, app-wide `NotificationBadgeViewModel`'s own refresh (see
 *  `PatientNavHost.kt`'s `notificationsTabGraph`), so the badge never
 *  waits for its own next 60-second poll to catch up with something
 *  the patient just did on screen. */
sealed interface NotificationsListUiEvent {
    data object ShowRefreshFailedMessage : NotificationsListUiEvent

    data object ShowMarkReadFailedMessage : NotificationsListUiEvent

    data object UnreadCountMayHaveChanged : NotificationsListUiEvent

    /** [com.aarogya.sanjeevani.patient.requests.RequestDetailRoute]
     *  already shows a friendly "doesn't exist" state on its own if
     *  [requestId] turns out to be gone -- this screen doesn't need to
     *  check first, just navigate. */
    data class NavigateToRequestDetail(val requestId: String) : NotificationsListUiEvent
}

sealed interface NotificationsListAction {
    data object Resumed : NotificationsListAction

    data object Refresh : NotificationsListAction

    data object Retry : NotificationsListAction

    data object LoadMore : NotificationsListAction

    data class NotificationClicked(val notificationId: String) : NotificationsListAction

    data object MarkAllReadClicked : NotificationsListAction
}
