package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.designsystem.component.AsChip
import com.aarogya.sanjeevani.core.designsystem.component.AsSearchField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.CompactErrorContent
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.services.label
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

private const val CARD_SHIMMER_COUNT = 4
private const val CHIP_SHIMMER_COUNT = 4
private const val LOAD_MORE_THRESHOLD = 5

/** The Doctors Directory, reached from Home. */
@Composable
fun DoctorsDirectoryRoute(
    onBack: () -> Unit,
    onDoctorClick: (doctorId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorsDirectoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(DoctorsDirectoryAction.Initialize)
    }

    DoctorsDirectoryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onDoctorClick = onDoctorClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, so a Compose UI test can
 *  drive it directly with a hand-built [DoctorsDirectoryUiState] and no
 *  Hilt/ViewModel, same reasoning as every other screen in this module. */
@Composable
internal fun DoctorsDirectoryScreen(
    uiState: DoctorsDirectoryUiState,
    onAction: (DoctorsDirectoryAction) -> Unit,
    onBack: () -> Unit,
    onDoctorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Doctors", onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AsSearchField(
                query = uiState.searchQuery,
                onQueryChange = { onAction(DoctorsDirectoryAction.SearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
                placeholder = "Search by name or specialization",
            )
            FilterChipsRows(uiState = uiState, onAction = onAction)
            DirectoryContent(uiState = uiState, onAction = onAction, onDoctorClick = onDoctorClick)
        }
    }
}

@Composable
private fun FilterChipsRows(
    uiState: DoctorsDirectoryUiState,
    onAction: (DoctorsDirectoryAction) -> Unit,
) {
    if (uiState.section is DoctorsDirectorySectionState.Loading) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
        ) {
            items(CHIP_SHIMMER_COUNT) { SkeletonShimmer(modifier = Modifier.width(100.dp).height(32.dp)) }
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        item {
            AsChip(
                label = "All specializations",
                selected = uiState.selectedSpecialization == null,
                onClick = { onAction(DoctorsDirectoryAction.SpecializationSelected(null)) },
            )
        }
        items(uiState.availableSpecializations.toList(), key = { it }) { specialization ->
            val isSelected = uiState.selectedSpecialization == specialization
            AsChip(
                label = specialization,
                selected = isSelected,
                onClick = {
                    val next = if (isSelected) null else specialization
                    onAction(DoctorsDirectoryAction.SpecializationSelected(next))
                },
            )
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        item {
            AsChip(
                label = "All modes",
                selected = uiState.selectedMode == null,
                onClick = { onAction(DoctorsDirectoryAction.ModeSelected(null)) },
            )
        }
        items(ServiceMode.entries.toList(), key = { it.name }) { mode ->
            val isSelected = uiState.selectedMode == mode
            AsChip(
                label = mode.label(),
                selected = isSelected,
                onClick = { onAction(DoctorsDirectoryAction.ModeSelected(if (isSelected) null else mode)) },
            )
        }
    }
}

@Composable
private fun DirectoryContent(
    uiState: DoctorsDirectoryUiState,
    onAction: (DoctorsDirectoryAction) -> Unit,
    onDoctorClick: (String) -> Unit,
) {
    when (val section = uiState.section) {
        is DoctorsDirectorySectionState.Loading ->
            Column(
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg).loadingGroupSemantics(),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                repeat(CARD_SHIMMER_COUNT) { SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(140.dp)) }
            }
        is DoctorsDirectorySectionState.Error ->
            ErrorContent(error = section.error, onRetry = { onAction(DoctorsDirectoryAction.Retry) })
        is DoctorsDirectorySectionState.Content ->
            if (section.doctors.isEmpty()) {
                EmptyContent(message = "No doctors match your search or filters.")
            } else {
                DoctorsList(section = section, onAction = onAction, onDoctorClick = onDoctorClick)
            }
    }
}

@Composable
private fun DoctorsList(
    section: DoctorsDirectorySectionState.Content,
    onAction: (DoctorsDirectoryAction) -> Unit,
    onDoctorClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreEffect(listState = listState, itemCount = section.doctors.size, section = section, onAction = onAction)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        items(section.doctors, key = { it.doctorId }) { doctor ->
            DoctorCard(
                doctor = doctor,
                avatarUrl = section.avatarUrlsByDoctorId[doctor.doctorId],
                offeredServiceNames = section.serviceNamesByDoctorId[doctor.doctorId].orEmpty(),
                onClick = { onDoctorClick(doctor.doctorId) },
            )
        }
        item {
            LoadMoreFooter(section = section, onAction = onAction)
        }
    }
}

@Composable
private fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    section: DoctorsDirectorySectionState.Content,
    onAction: (DoctorsDirectoryAction) -> Unit,
) {
    LaunchedEffect(listState, itemCount, section.hasMore, section.isLoadingMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                val nearEnd = lastVisibleIndex >= itemCount - LOAD_MORE_THRESHOLD
                if (nearEnd && section.hasMore && !section.isLoadingMore) {
                    onAction(DoctorsDirectoryAction.LoadMore)
                }
            }
    }
}

@Composable
private fun LoadMoreFooter(
    section: DoctorsDirectorySectionState.Content,
    onAction: (DoctorsDirectoryAction) -> Unit,
) {
    when {
        section.isLoadingMore ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(AsTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        section.loadMoreError != null ->
            CompactErrorContent(
                error = section.loadMoreError,
                onRetry = { onAction(DoctorsDirectoryAction.RetryLoadMore) },
            )
        else -> Unit
    }
}
