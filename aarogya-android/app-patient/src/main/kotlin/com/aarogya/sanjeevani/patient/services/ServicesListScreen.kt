package com.aarogya.sanjeevani.patient.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsChip
import com.aarogya.sanjeevani.core.designsystem.component.AsListItem
import com.aarogya.sanjeevani.core.designsystem.component.AsSearchField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SectionHeader
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent

private const val CHIP_SHIMMER_COUNT = 4
private const val ROW_SHIMMER_COUNT = 6

/** The Services tab's root. [categoryId], when set (arriving from Home's
 *  category row), pre-selects that category's chip. */
@Composable
fun ServicesListRoute(
    onServiceClick: (serviceId: String) -> Unit,
    modifier: Modifier = Modifier,
    categoryId: String? = null,
    viewModel: ServicesListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(ServicesListAction.Initialize(categoryId))
    }

    ServicesListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onServiceClick = onServiceClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, so a Compose UI test can
 *  drive it directly with a hand-built [ServicesListUiState] and no
 *  Hilt/ViewModel, same reasoning as `HomeScreen.kt`'s own doc comment. */
@Composable
internal fun ServicesListScreen(
    uiState: ServicesListUiState,
    onAction: (ServicesListAction) -> Unit,
    onServiceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Services") },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AsSearchField(
                query = uiState.searchQuery,
                onQueryChange = { onAction(ServicesListAction.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
                placeholder = "Search services",
            )
            CategoryChipRow(uiState = uiState, onAction = onAction)
            ServicesListContent(uiState = uiState, onAction = onAction, onServiceClick = onServiceClick)
        }
    }
}

@Composable
private fun CategoryChipRow(
    uiState: ServicesListUiState,
    onAction: (ServicesListAction) -> Unit,
) {
    val success = uiState.section as? ServicesListSectionState.Success
    when {
        uiState.section is ServicesListSectionState.Loading ->
            LazyRow(
                contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                items(CHIP_SHIMMER_COUNT) { SkeletonShimmer(modifier = Modifier.width(90.dp).height(32.dp)) }
            }
        success != null ->
            LazyRow(
                contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                item {
                    AsChip(
                        label = "All",
                        selected = uiState.selectedCategoryId == null,
                        onClick = { onAction(ServicesListAction.CategorySelected(null)) },
                    )
                }
                items(success.categories, key = { it.id }) { category ->
                    val isSelected = uiState.selectedCategoryId == category.id
                    AsChip(
                        label = category.name,
                        selected = isSelected,
                        onClick = {
                            onAction(ServicesListAction.CategorySelected(if (isSelected) null else category.id))
                        },
                    )
                }
            }
        else -> Unit
    }
}

@Composable
private fun ServicesListContent(
    uiState: ServicesListUiState,
    onAction: (ServicesListAction) -> Unit,
    onServiceClick: (String) -> Unit,
) {
    when (val section = uiState.section) {
        is ServicesListSectionState.Loading ->
            Column(
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg).loadingGroupSemantics(),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                repeat(ROW_SHIMMER_COUNT) { SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(64.dp)) }
            }
        is ServicesListSectionState.Error ->
            ErrorContent(error = section.error, onRetry = { onAction(ServicesListAction.Retry) })
        is ServicesListSectionState.Success ->
            if (section.services.isEmpty()) {
                EmptyContent(
                    message = "No services are available right now.",
                    action = {
                        AsButton(
                            text = "Try again",
                            onClick = { onAction(ServicesListAction.Retry) },
                            variant = AsButtonVariant.Text,
                        )
                    },
                )
            } else if (uiState.groups.isEmpty()) {
                EmptyContent(
                    message = "No services match your search.",
                    action = {
                        AsButton(
                            text = "Clear filters",
                            onClick = { onAction(ServicesListAction.ClearFilters) },
                            variant = AsButtonVariant.Text,
                        )
                    },
                )
            } else {
                GroupedServicesList(groups = uiState.groups, onServiceClick = onServiceClick)
            }
    }
}

@Composable
private fun GroupedServicesList(
    groups: List<ServiceGroup>,
    onServiceClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.category.id}") {
                SectionHeader(title = group.category.name, modifier = Modifier.padding(horizontal = AsTheme.spacing.lg))
            }
            items(group.services, key = { it.id }) { service ->
                ServiceRow(
                    service = service,
                    onClick = { onServiceClick(service.id) },
                    modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsListItem(
        headline = service.name,
        supportingText = service.shortDescription,
        // Stable test handle -- real service names/content come from
        // whatever the backend currently has, which a test can't know
        // in advance (see TabStateRestorationTest, the one place this is
        // used, for why it only needs *a* service row, not a specific
        // one).
        modifier = modifier.testTag("serviceRow"),
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
                    service.supportedModes.forEach { mode ->
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = mode.label(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(20.dp),
                        )
                    }
                }
                if (service.requiresDoctor) {
                    Text(
                        text = "Doctor required",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "This service requires choosing a doctor"
                            },
                    )
                }
            }
        },
        onClick = onClick,
    )
}
