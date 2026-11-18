package com.aarogya.sanjeevani.patient.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.ui.common.formatRelativeTime
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ROW_COUNT = 8

/** The Notifications tab's only destination. Never decides for itself
 *  whether a notification is unread or what it says -- all of that
 *  came straight from `notifications` via [NotificationsListViewModel]. */
@Composable
fun NotificationsListRoute(
    onNavigateToRequestDetail: (requestId: String) -> Unit,
    onRefreshUnreadCount: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: NotificationsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(NotificationsListAction.Resumed)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                NotificationsListUiEvent.ShowRefreshFailedMessage ->
                    snackbarHostState.showSnackbar("Couldn't refresh -- showing what we have.")
                NotificationsListUiEvent.ShowMarkReadFailedMessage ->
                    snackbarHostState.showSnackbar("Couldn't update that -- please try again.")
                NotificationsListUiEvent.UnreadCountMayHaveChanged -> onRefreshUnreadCount()
                is NotificationsListUiEvent.NavigateToRequestDetail -> onNavigateToRequestDetail(event.requestId)
            }
        }
    }

    NotificationsListScreen(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this app. */
@Composable
internal fun NotificationsListScreen(
    uiState: NotificationsListUiState,
    onAction: (NotificationsListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AsTopBar(
                title = "Notifications",
                actions = {
                    TextButton(
                        onClick = { onAction(NotificationsListAction.MarkAllReadClicked) },
                        enabled = !uiState.isMarkingAllRead,
                    ) {
                        Text("Mark all as read")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val section = uiState.section) {
                NotificationsListSectionState.Loading -> ShimmerList()
                is NotificationsListSectionState.Error ->
                    ErrorContent(error = section.error, onRetry = { onAction(NotificationsListAction.Retry) })
                is NotificationsListSectionState.Content ->
                    NotificationsContent(section = section, isRefreshing = uiState.isRefreshing, onAction = onAction)
            }
        }
    }
}

@Composable
private fun ShimmerList() {
    Column(
        modifier = Modifier.fillMaxSize().padding(AsTheme.spacing.lg).loadingGroupSemantics(),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        repeat(SHIMMER_ROW_COUNT) { SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    section: NotificationsListSectionState.Content,
    isRefreshing: Boolean,
    onAction: (NotificationsListAction) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onAction(NotificationsListAction.Refresh) },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (section.notifications.isEmpty()) {
            EmptyContent(message = "You're all caught up.")
        } else {
            NotificationsLazyList(section = section, onAction = onAction)
        }
    }
}

@Composable
private fun NotificationsLazyList(
    section: NotificationsListSectionState.Content,
    onAction: (NotificationsListAction) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState, section.notifications.size, section.hasMore, onAction)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        items(section.notifications, key = { it.id }) { notification ->
            NotificationCard(
                notification = notification,
                onClick = { onAction(NotificationsListAction.NotificationClicked(notification.id)) },
            )
        }
    }
}

@Composable
private fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    onAction: (NotificationsListAction) -> Unit,
) {
    LaunchedEffect(listState, itemCount, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (hasMore && lastVisibleIndex >= itemCount - LOAD_MORE_THRESHOLD) {
                    onAction(NotificationsListAction.LoadMore)
                }
            }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    val description =
        if (!notification.isRead) {
            "Unread. ${notification.title}. ${notification.body}"
        } else {
            "${notification.title}. ${notification.body}"
        }
    AsCard(
        onClick = onClick,
        // Unread notifications get a real tint, not just the dot --
        // the brief's violet "Unread" marker, carried through to the
        // whole card.
        containerColor =
            if (!notification.isRead) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            if (!notification.isRead) {
                Surface(
                    modifier = Modifier.size(8.dp).padding(top = 6.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                ) {}
            } else {
                Spacer(modifier = Modifier.size(8.dp))
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                )
                Text(text = notification.body, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = formatRelativeTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
