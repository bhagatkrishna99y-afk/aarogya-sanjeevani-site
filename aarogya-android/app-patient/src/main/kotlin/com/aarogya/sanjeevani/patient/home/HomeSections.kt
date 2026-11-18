package com.aarogya.sanjeevani.patient.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsIconTile
import com.aarogya.sanjeevani.core.designsystem.component.AsIconTileColor
import com.aarogya.sanjeevani.core.designsystem.component.SectionHeader
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.StatusChip
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.CompactEmptyContent
import com.aarogya.sanjeevani.core.ui.state.CompactErrorContent

private const val CATEGORY_SHIMMER_COUNT = 4
private const val DOCTOR_SHIMMER_COUNT = 3
private val DOCTOR_CARD_WIDTH = 160.dp

@Composable
internal fun CategoriesSection(
    state: HomeSectionState<List<ServiceCategory>>,
    onCategoryClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "Browse by category", modifier = Modifier.padding(horizontal = AsTheme.spacing.lg))
        when (state) {
            is HomeSectionState.Loading ->
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    modifier = Modifier.loadingGroupSemantics(),
                ) {
                    items(CATEGORY_SHIMMER_COUNT) {
                        SkeletonShimmer(
                            modifier = Modifier.width(CATEGORY_TILE_WIDTH).height(60.dp),
                            shape = MaterialTheme.shapes.medium,
                        )
                    }
                }
            is HomeSectionState.Error ->
                CompactErrorContent(
                    error = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
                )
            is HomeSectionState.Success ->
                if (state.data.isEmpty()) {
                    CompactEmptyContent(
                        message = "No services are available right now.",
                        modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    ) {
                        items(state.data, key = { it.id }) { category ->
                            val index = state.data.indexOf(category)
                            CategoryTile(
                                name = category.name,
                                icon = iconForCategory(category.icon),
                                color = AsIconTileColor.entries[index % AsIconTileColor.entries.size],
                                onClick = { onCategoryClick(category.id) },
                            )
                        }
                    }
                }
        }
    }
}

private val CATEGORY_TILE_WIDTH = 84.dp

@Composable
private fun CategoryTile(
    name: String,
    icon: ImageVector,
    color: AsIconTileColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(CATEGORY_TILE_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
    ) {
        AsIconTile(icon = icon, color = color, onClick = onClick, size = 60.dp)
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
internal fun RequestsSection(
    state: HomeSectionState<List<PatientRequest>>,
    onViewAllClick: () -> Unit,
    onRequestClick: (String) -> Unit,
    onBrowseServicesClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = "Upcoming & active requests",
            action = { AsButton(text = "View all", onClick = onViewAllClick, variant = AsButtonVariant.Text) },
        )
        when (state) {
            is HomeSectionState.Loading ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    modifier = Modifier.loadingGroupSemantics(),
                ) {
                    repeat(2) { SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(88.dp)) }
                }
            is HomeSectionState.Error -> CompactErrorContent(error = state.error, onRetry = onRetry)
            is HomeSectionState.Success ->
                if (state.data.isEmpty()) {
                    CompactEmptyContent(
                        message = "You don't have any upcoming requests yet.",
                        action = { AsButton(text = "Book your first service", onClick = onBrowseServicesClick) },
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                        state.data.forEach { request ->
                            RequestCard(request = request, onClick = { onRequestClick(request.id) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun RequestCard(
    request: PatientRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = request.serviceName, style = MaterialTheme.typography.titleSmall)
                StatusChip(status = request.status.toAsRequestStatus())
            }
            Text(
                text =
                    if (request.doctorFullName != null) {
                        "${request.doctorFullName} · ${formatRequestDateTime(request.scheduledStart)}"
                    } else {
                        formatRequestDateTime(request.scheduledStart)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DoctorsSection(
    state: HomeSectionState<List<DoctorPublic>>,
    onDoctorClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(title = "Featured doctors", modifier = Modifier.padding(horizontal = AsTheme.spacing.lg))
        when (state) {
            is HomeSectionState.Loading ->
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    modifier = Modifier.loadingGroupSemantics(),
                ) {
                    items(DOCTOR_SHIMMER_COUNT) {
                        SkeletonShimmer(modifier = Modifier.width(DOCTOR_CARD_WIDTH).height(120.dp))
                    }
                }
            is HomeSectionState.Error ->
                CompactErrorContent(
                    error = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
                )
            is HomeSectionState.Success ->
                if (state.data.isEmpty()) {
                    CompactEmptyContent(
                        message = "No doctors are available right now.",
                        modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    ) {
                        items(state.data, key = { it.doctorId }) { doctor ->
                            DoctorCard(doctor = doctor, onClick = { onDoctorClick(doctor.doctorId) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun DoctorCard(
    doctor: DoctorPublic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsCard(onClick = onClick, modifier = modifier.width(DOCTOR_CARD_WIDTH)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
        ) {
            AsAvatar(name = doctor.fullName, size = 48.dp)
            Text(text = doctor.fullName, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                text = doctor.specialization,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            val successColor = AsTheme.extendedColors.success
            val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = if (doctor.acceptingRequests) "Accepting requests" else "Not accepting requests",
                style = MaterialTheme.typography.labelSmall,
                color = if (doctor.acceptingRequests) successColor else mutedColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
