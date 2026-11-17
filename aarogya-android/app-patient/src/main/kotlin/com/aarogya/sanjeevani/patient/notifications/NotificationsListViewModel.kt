package com.aarogya.sanjeevani.patient.notifications

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.data.notifications.NotificationsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 20

/**
 * Drives the Notifications tab. [allNotifications] is this screen's own
 * full snapshot of `getNotifications()` (already newest-first per
 * client-contract.md §11, so never re-sorted here); [visibleCount]
 * slices it into the currently-revealed local page, same pattern as
 * [com.aarogya.sanjeevani.patient.requests.RequestsListViewModel].
 */
@HiltViewModel
class NotificationsListViewModel
    @Inject
    constructor(
        private val notificationsRepository: NotificationsRepository,
    ) : BaseViewModel<NotificationsListUiState, NotificationsListUiEvent, NotificationsListAction>(
            NotificationsListUiState(),
        ) {
        private var allNotifications: List<Notification> = emptyList()
        private var visibleCount = PAGE_SIZE

        override fun onAction(action: NotificationsListAction) {
            when (action) {
                NotificationsListAction.Resumed -> load(isUserInitiated = false)
                NotificationsListAction.Refresh -> load(isUserInitiated = true)
                NotificationsListAction.Retry -> {
                    setState { copy(section = NotificationsListSectionState.Loading) }
                    load(isUserInitiated = false)
                }
                NotificationsListAction.LoadMore -> loadMore()
                is NotificationsListAction.NotificationClicked -> onNotificationClicked(action.notificationId)
                NotificationsListAction.MarkAllReadClicked -> markAllRead()
            }
        }

        private fun load(isUserInitiated: Boolean) {
            if (isUserInitiated) setState { copy(isRefreshing = true) }
            viewModelScope.launch {
                when (val result = notificationsRepository.getNotifications()) {
                    is AppResult.Success -> {
                        allNotifications = result.data
                        visibleCount = PAGE_SIZE
                        setState { copy(section = buildContent(), isRefreshing = false) }
                    }
                    is AppResult.Failure -> {
                        val staysOnContent = currentState.section is NotificationsListSectionState.Content
                        setState {
                            copy(
                                section =
                                    if (staysOnContent) section else NotificationsListSectionState.Error(result.error),
                                isRefreshing = false,
                            )
                        }
                        if (isUserInitiated && staysOnContent) {
                            sendEvent(NotificationsListUiEvent.ShowRefreshFailedMessage)
                        }
                    }
                }
            }
        }

        private fun loadMore() {
            val content = currentState.section as? NotificationsListSectionState.Content ?: return
            if (!content.hasMore) return
            visibleCount += PAGE_SIZE
            setState { copy(section = buildContent()) }
        }

        private fun buildContent(): NotificationsListSectionState.Content =
            NotificationsListSectionState.Content(
                notifications = allNotifications.take(visibleCount),
                hasMore = allNotifications.size > visibleCount,
            )

        private fun onNotificationClicked(notificationId: String) {
            val notification = allNotifications.firstOrNull { it.id == notificationId } ?: return
            if (!notification.isRead) markOneRead(notificationId)
            notification.relatedRequestId?.let { requestId ->
                sendEvent(NotificationsListUiEvent.NavigateToRequestDetail(requestId))
            }
        }

        private fun markOneRead(notificationId: String) {
            val previousAll = allNotifications
            replaceNotification(notificationId) { it.copy(isRead = true) }
            viewModelScope.launch {
                when (notificationsRepository.markRead(notificationId)) {
                    is AppResult.Success -> sendEvent(NotificationsListUiEvent.UnreadCountMayHaveChanged)
                    is AppResult.Failure -> {
                        allNotifications = previousAll
                        setState { copy(section = buildContent()) }
                        sendEvent(NotificationsListUiEvent.ShowMarkReadFailedMessage)
                    }
                }
            }
        }

        private fun markAllRead() {
            if (currentState.isMarkingAllRead || currentState.section !is NotificationsListSectionState.Content) {
                return
            }
            val previousAll = allNotifications
            allNotifications = allNotifications.map { it.copy(isRead = true) }
            setState { copy(section = buildContent(), isMarkingAllRead = true) }
            viewModelScope.launch {
                when (notificationsRepository.markAllRead()) {
                    is AppResult.Success -> {
                        setState { copy(isMarkingAllRead = false) }
                        sendEvent(NotificationsListUiEvent.UnreadCountMayHaveChanged)
                    }
                    is AppResult.Failure -> {
                        allNotifications = previousAll
                        setState { copy(section = buildContent(), isMarkingAllRead = false) }
                        sendEvent(NotificationsListUiEvent.ShowMarkReadFailedMessage)
                    }
                }
            }
        }

        private fun replaceNotification(
            id: String,
            transform: (Notification) -> Notification,
        ) {
            allNotifications = allNotifications.map { if (it.id == id) transform(it) else it }
            setState { copy(section = buildContent()) }
        }
    }
