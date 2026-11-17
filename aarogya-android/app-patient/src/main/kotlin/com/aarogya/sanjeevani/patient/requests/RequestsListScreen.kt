package com.aarogya.sanjeevani.patient.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private const val LOAD_MORE_THRESHOLD = 5
private const val SHIMMER_ROW_COUNT = 6

/** Requests tab root. Never computes which requests are "active"/"past"
 *  from a real-time clock inside the UI layer -- [RequestsListViewModel]
 *  already decided that when it built [RequestsListUiState.section]. */
@Composable
fun RequestsListRoute(
    onRequestClick: (requestId: String) -> Unit,
    onBrowseServicesClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: RequestsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(RequestsListAction.Resumed)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RequestsListUiEvent.ShowRefreshFailedMessage ->
                    snackbarHostState.showSnackbar("Couldn't refresh -- showing what we have.")
            }
        }
    }

    RequestsListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRequestClick = onRequestClick,
        onBrowseServicesClick = onBrowseServicesClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RequestsListScreen(
    uiState: RequestsListUiState,
    onAction: (RequestsListAction) -> Unit,
    onRequestClick: (String) -> Unit,
    onBrowseServicesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "My requests") },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                Tab(
                    selected = uiState.selectedTab == RequestsTab.ACTIVE,
                    onClick = { onAction(RequestsListAction.TabSelected(RequestsTab.ACTIVE)) },
                    text = { Text("Active") },
                )
                Tab(
                    selected = uiState.selectedTab == RequestsTab.PAST,
                    onClick = { onAction(RequestsListAction.TabSelected(RequestsTab.PAST)) },
                    text = { Text("Past") },
                )
            }
            RequestsListBody(
                uiState = uiState,
                onAction = onAction,
                onRequestClick = onRequestClick,
                onBrowseServicesClick = onBrowseServicesClick,
            )
        }
    }
}

@Composable
private fun RequestsListBody(
    uiState: RequestsListUiState,
    onAction: (RequestsListAction) -> Unit,
    onRequestClick: (String) -> Unit,
    onBrowseServicesClick: () -> Unit,
) {
    when (val section = uiState.section) {
        RequestsListSectionState.Loading -> ShimmerList()
        is RequestsListSectionState.Error ->
            ErrorContent(error = section.error, onRetry = { onAction(RequestsListAction.Retry) })
        is RequestsListSectionState.Content ->
            RequestsListContent(
                section = section,
                selectedTab = uiState.selectedTab,
                isRefreshing = uiState.isRefreshing,
                onAction = onAction,
                onRequestClick = onRequestClick,
                onBrowseServicesClick = onBrowseServicesClick,
            )
    }
}

@Composable
private fun ShimmerList() {
    Column(
        modifier = Modifier.fillMaxSize().padding(AsTheme.spacing.lg).loadingGroupSemantics(),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        repeat(SHIMMER_ROW_COUNT) { SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(88.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestsListContent(
    section: RequestsListSectionState.Content,
    selectedTab: RequestsTab,
    isRefreshing: Boolean,
    onAction: (RequestsListAction) -> Unit,
    onRequestClick: (String) -> Unit,
    onBrowseServicesClick: () -> Unit,
) {
    val requests = if (selectedTab == RequestsTab.ACTIVE) section.active else section.past
    val hasMore = if (selectedTab == RequestsTab.ACTIVE) section.hasMoreActive else section.hasMorePast
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onAction(RequestsListAction.Refresh) },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (requests.isEmpty()) {
            EmptyRequestsContent(selectedTab = selectedTab, onBrowseServicesClick = onBrowseServicesClick)
        } else {
            RequestsLazyList(requests, hasMore, onAction, onRequestClick)
        }
    }
}

@Composable
private fun RequestsLazyList(
    requests: List<PatientRequest>,
    hasMore: Boolean,
    onAction: (RequestsListAction) -> Unit,
    onRequestClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, itemCount = requests.size, hasMore = hasMore, onAction = onAction)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        items(requests, key = { it.id }) { request ->
            RequestCard(request = request, onClick = { onRequestClick(request.id) })
        }
    }
}

@Composable
private fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    onAction: (RequestsListAction) -> Unit,
) {
    LaunchedEffect(listState, itemCount, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (hasMore && lastVisibleIndex >= itemCount - LOAD_MORE_THRESHOLD) {
                    onAction(RequestsListAction.LoadMore)
                }
            }
    }
}

@Composable
private fun EmptyRequestsContent(
    selectedTab: RequestsTab,
    onBrowseServicesClick: () -> Unit,
) {
    val message =
        when (selectedTab) {
            RequestsTab.ACTIVE -> "You don't have any upcoming requests yet."
            RequestsTab.PAST -> "You don't have any past requests yet."
        }
    EmptyContent(
        message = message,
        action = { AsButton(text = "Browse services", onClick = onBrowseServicesClick) },
    )
}
