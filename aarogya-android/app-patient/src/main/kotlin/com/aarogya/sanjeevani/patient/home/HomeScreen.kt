package com.aarogya.sanjeevani.patient.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsBadge
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.designsystem.theme.PillShape
import com.aarogya.sanjeevani.patient.notifications.formatUnreadBadgeCount

/**
 * The stateful half -- see `LoginScreen.kt`'s doc comment for the
 * general shape every screen in this module follows. The one thing
 * unique to this screen: [HomeAction.Resumed] fires from
 * [LifecycleEventEffect] rather than any user interaction, covering
 * both "first time this screen appears" and "the app was backgrounded
 * and is back" with the one mechanism (see [HomeViewModel]'s doc
 * comment for why that's deliberate).
 */
@Suppress("LongParameterList")
@Composable
fun HomeRoute(
    onBrowseServicesClick: () -> Unit,
    onFindDoctorClick: () -> Unit,
    onCategoryClick: (categoryId: String) -> Unit,
    onViewAllRequestsClick: () -> Unit,
    onRequestClick: (requestId: String) -> Unit,
    onDoctorClick: (doctorId: String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(HomeAction.Resumed)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                HomeUiEvent.ShowRefreshFailedMessage ->
                    snackbarHostState.showSnackbar("Couldn't refresh everything -- showing what we have.")
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        greetingPrefix = viewModel.greetingPrefix,
        onAction = viewModel::onAction,
        onBrowseServicesClick = onBrowseServicesClick,
        onFindDoctorClick = onFindDoctorClick,
        onCategoryClick = onCategoryClick,
        onViewAllRequestsClick = onViewAllRequestsClick,
        onRequestClick = onRequestClick,
        onDoctorClick = onDoctorClick,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        unreadNotificationCount = unreadNotificationCount,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, so a Compose UI test
 *  can call it directly with a hand-built [HomeUiState] and no Hilt,
 *  same reasoning as `LoginScreen.kt`'s own doc comment. As many
 *  parameters as this screen has distinct navigation targets -- same
 *  tolerance config/detekt/detekt.yml already documents for a
 *  richly-configurable Compose entry point, just past the raised
 *  `functionThreshold` since this is also the screen's one call site. */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    greetingPrefix: String,
    onAction: (HomeAction) -> Unit,
    onBrowseServicesClick: () -> Unit,
    onFindDoctorClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllRequestsClick: () -> Unit,
    onRequestClick: (String) -> Unit,
    onDoctorClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { onAction(HomeAction.Refresh) },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize(),
    ) {
        HomeContent(
            uiState = uiState,
            greetingPrefix = greetingPrefix,
            onAction = onAction,
            onBrowseServicesClick = onBrowseServicesClick,
            onFindDoctorClick = onFindDoctorClick,
            onCategoryClick = onCategoryClick,
            onViewAllRequestsClick = onViewAllRequestsClick,
            onRequestClick = onRequestClick,
            onDoctorClick = onDoctorClick,
            onNotificationsClick = onNotificationsClick,
            onProfileClick = onProfileClick,
            unreadNotificationCount = unreadNotificationCount,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    greetingPrefix: String,
    onAction: (HomeAction) -> Unit,
    onBrowseServicesClick: () -> Unit,
    onFindDoctorClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onViewAllRequestsClick: () -> Unit,
    onRequestClick: (String) -> Unit,
    onDoctorClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    unreadNotificationCount: Int,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xl),
    ) {
        item {
            HomeHeader(
                greetingPrefix = greetingPrefix,
                greetingName = uiState.greetingName,
                unreadNotificationCount = unreadNotificationCount,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
            )
        }
        item {
            HomeSearchEntry(
                onClick = onBrowseServicesClick,
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
            )
        }
        item {
            QuickActionsRow(
                onBrowseServicesClick = onBrowseServicesClick,
                onFindDoctorClick = onFindDoctorClick,
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
            )
        }
        item {
            CategoriesSection(
                state = uiState.categories,
                onCategoryClick = onCategoryClick,
                onRetry = { onAction(HomeAction.RetryCategories) },
            )
        }
        item {
            RequestsSection(
                state = uiState.requests,
                onViewAllClick = onViewAllRequestsClick,
                onRequestClick = onRequestClick,
                onBrowseServicesClick = onBrowseServicesClick,
                onRetry = { onAction(HomeAction.RetryRequests) },
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
            )
        }
        item {
            DoctorsSection(
                state = uiState.doctors,
                onDoctorClick = onDoctorClick,
                onRetry = { onAction(HomeAction.RetryDoctors) },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    greetingPrefix: String,
    greetingName: String?,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (greetingName != null) "$greetingPrefix, $greetingName" else greetingPrefix,
            style = MaterialTheme.typography.headlineSmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = onNotificationsClick) {
                    Icon(imageVector = Icons.Filled.Notifications, contentDescription = "Notifications")
                }
                formatUnreadBadgeCount(unreadNotificationCount)?.let { badgeText ->
                    AsBadge(
                        text = badgeText,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .semantics { contentDescription = "$unreadNotificationCount unread notifications" },
                    )
                }
            }
            IconButton(onClick = onProfileClick) {
                AsAvatar(name = greetingName ?: "?", size = 32.dp)
            }
        }
    }
}

/** Looks like a search field, but -- unlike [com.aarogya.sanjeevani.core.designsystem.component.AsSearchField]
 *  -- isn't one: tapping it always just navigates to Services rather
 *  than typing in place, so it's built from a plain [Surface] instead
 *  of repurposing a real text field as a disguised button (which would
 *  pop the keyboard open for a frame before navigating away). */
@Composable
private fun HomeSearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Search services or doctors",
                modifier = Modifier.padding(start = AsTheme.spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    onBrowseServicesClick: () -> Unit,
    onFindDoctorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md)) {
        AsCard(onClick = onBrowseServicesClick, modifier = Modifier.weight(1f)) {
            Text(text = "Browse Services", style = MaterialTheme.typography.titleSmall)
        }
        AsCard(onClick = onFindDoctorClick, modifier = Modifier.weight(1f)) {
            Text(text = "Find a Doctor", style = MaterialTheme.typography.titleSmall)
        }
    }
}
