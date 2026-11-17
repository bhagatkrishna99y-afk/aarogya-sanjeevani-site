package com.aarogya.sanjeevani.patient.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.doctors.DoctorCard

private const val CARD_SHIMMER_COUNT = 3

/** Reached only from [com.aarogya.sanjeevani.patient.services.ServiceDetailScreen]'s
 *  "Continue" -- see `PatientRoutes.kt`'s doc comment on
 *  [com.aarogya.sanjeevani.patient.navigation.DoctorListForService] for
 *  why [serviceName] is a nav arg rather than re-fetched here. */
@Composable
fun DoctorListForServiceRoute(
    serviceId: String,
    serviceName: String,
    onBack: () -> Unit,
    onBackToServices: () -> Unit,
    onDoctorClick: (doctorId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorListForServiceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(serviceId) {
        viewModel.onAction(DoctorListForServiceAction.Load(serviceId = serviceId, serviceName = serviceName))
    }

    DoctorListForServiceScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onBackToServices = onBackToServices,
        onDoctorClick = onDoctorClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this module. */
@Composable
internal fun DoctorListForServiceScreen(
    uiState: DoctorListForServiceUiState,
    onAction: (DoctorListForServiceAction) -> Unit,
    onBack: () -> Unit,
    onBackToServices: () -> Unit,
    onDoctorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Doctors", onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ServiceHeader(serviceName = uiState.serviceName)
            when (val section = uiState.section) {
                is DoctorListForServiceSectionState.Loading ->
                    Column(
                        modifier =
                            Modifier
                                .padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm)
                                .loadingGroupSemantics(),
                        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                    ) {
                        repeat(CARD_SHIMMER_COUNT) {
                            SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(140.dp))
                        }
                    }
                is DoctorListForServiceSectionState.Error ->
                    ErrorContent(error = section.error, onRetry = { onAction(DoctorListForServiceAction.Retry) })
                is DoctorListForServiceSectionState.Content ->
                    if (section.doctors.isEmpty()) {
                        EmptyContent(
                            message = "No doctors currently offer ${uiState.serviceName}.",
                            action = { AsButton(text = "Back to Services", onClick = onBackToServices) },
                        )
                    } else {
                        DoctorsForServiceList(
                            section = section,
                            serviceName = uiState.serviceName,
                            onDoctorClick = onDoctorClick,
                        )
                    }
            }
        }
    }
}

@Composable
private fun ServiceHeader(serviceName: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = "Doctors for $serviceName",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(AsTheme.spacing.lg),
        )
    }
}

@Composable
private fun DoctorsForServiceList(
    section: DoctorListForServiceSectionState.Content,
    serviceName: String,
    onDoctorClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        items(section.doctors, key = { it.doctorId }) { doctor ->
            DoctorCard(
                doctor = doctor,
                avatarUrl = section.avatarUrlsByDoctorId[doctor.doctorId],
                offeredServiceNames = listOf(serviceName),
                onClick = { onDoctorClick(doctor.doctorId) },
            )
        }
    }
}
