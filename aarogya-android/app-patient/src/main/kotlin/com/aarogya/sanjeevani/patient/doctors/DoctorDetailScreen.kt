package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsBadge
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.services.icon
import com.aarogya.sanjeevani.patient.services.label

/**
 * Reached two ways -- the plain Doctors Directory (no [serviceId]) and
 * Doctors-for-service (always carries one) -- see `PatientRoutes.kt`'s
 * doc comment on [com.aarogya.sanjeevani.patient.navigation.DoctorDetail].
 * "View availability" either continues straight on with [serviceId]/
 * [serviceName] when they arrived, or opens [ServicePickerBottomSheet]
 * to let the patient choose one of this doctor's own
 * [DoctorDetailUiState.Content.services] first -- [onViewAvailabilityClick]'s
 * own `serviceName` always comes from whichever of those two actually
 * supplied the id, and `doctorName` from this screen's own loaded
 * [DoctorDetailUiState.Content.doctor], since [Availability]'s route
 * needs both without re-fetching either.
 */
@Composable
fun DoctorDetailRoute(
    doctorId: String,
    serviceId: String?,
    serviceName: String?,
    onBack: () -> Unit,
    onViewAvailabilityClick: (serviceId: String, serviceName: String, doctorName: String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: DoctorDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(doctorId) {
        viewModel.onAction(DoctorDetailAction.Load(doctorId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                DoctorDetailUiEvent.DoctorUnavailable -> {
                    snackbarHostState.showSnackbar("This doctor is no longer available.")
                    onBack()
                }
            }
        }
    }

    DoctorDetailScreen(
        uiState = uiState,
        serviceId = serviceId,
        serviceName = serviceName,
        onBack = onBack,
        onRetry = { viewModel.onAction(DoctorDetailAction.Retry) },
        onViewAvailabilityClick = onViewAvailabilityClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this module. */
@Composable
internal fun DoctorDetailScreen(
    uiState: DoctorDetailUiState,
    serviceId: String?,
    serviceName: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onViewAvailabilityClick: (serviceId: String, serviceName: String, doctorName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showServicePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Doctor", onBack = onBack) },
    ) { padding ->
        when (uiState) {
            DoctorDetailUiState.Loading ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(AsTheme.spacing.lg)
                            .loadingGroupSemantics(),
                    verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
                ) {
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(80.dp))
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            is DoctorDetailUiState.Error ->
                ErrorContent(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            is DoctorDetailUiState.Content ->
                DoctorDetailContent(
                    uiState = uiState,
                    onViewAvailabilityClick = {
                        if (serviceId != null && serviceName != null) {
                            onViewAvailabilityClick(serviceId, serviceName, uiState.doctor.fullName)
                        } else {
                            showServicePicker = true
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }

    if (showServicePicker && uiState is DoctorDetailUiState.Content) {
        ServicePickerBottomSheet(
            services = uiState.services,
            onServiceSelected = { picked ->
                showServicePicker = false
                onViewAvailabilityClick(picked.serviceId, picked.serviceName, uiState.doctor.fullName)
            },
            onDismiss = { showServicePicker = false },
        )
    }
}

@Composable
private fun DoctorDetailContent(
    uiState: DoctorDetailUiState.Content,
    onViewAvailabilityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doctor = uiState.doctor
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
    ) {
        DoctorHeader(doctor = doctor, avatarUrl = uiState.avatarUrl)
        Text(
            text = "${doctor.yearsOfExperience} years of experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val bio = doctor.bio
        if (!bio.isNullOrBlank()) {
            Text(text = bio, style = MaterialTheme.typography.bodyLarge)
        }
        if (uiState.services.isNotEmpty()) {
            ServicesOfferedSection(services = uiState.services)
        }
        ModesRow(doctor = doctor)
        AcceptingRequestsBanner(doctor = doctor, onViewAvailabilityClick = onViewAvailabilityClick)
    }
}

@Composable
private fun DoctorHeader(
    doctor: DoctorPublic,
    avatarUrl: String?,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsAvatar(name = doctor.fullName, imageUrl = avatarUrl, size = 72.dp)
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
            Text(
                text = doctor.title?.let { "$it ${doctor.fullName}" } ?: doctor.fullName,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = doctor.specialization,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val qualifications = doctor.qualifications
            if (!qualifications.isNullOrBlank()) {
                Text(
                    text = qualifications,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ServicesOfferedSection(services: List<DoctorServicePublic>) {
    Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
        Text(text = "Services offered", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
            services.forEach { service ->
                AsBadge(
                    text = service.serviceName,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModesRow(doctor: DoctorPublic) {
    Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg)) {
        doctor.consultationModes.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = mode.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = mode.label(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Always visible, never just implied by the button's own enabled
 *  state -- "clearly stating" (per this screen's own brief) means a
 *  patient scanning quickly shouldn't have to notice a disabled button
 *  to learn a doctor isn't taking new requests right now. */
@Composable
private fun AcceptingRequestsBanner(
    doctor: DoctorPublic,
    onViewAvailabilityClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
        Surface(
            color =
                if (doctor.acceptingRequests) {
                    AsTheme.extendedColors.successContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
        ) {
            Text(
                text =
                    if (doctor.acceptingRequests) {
                        "This doctor is currently accepting new requests."
                    } else {
                        "This doctor isn't accepting new requests right now. Check back later."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (doctor.acceptingRequests) {
                        AsTheme.extendedColors.onSuccessContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.fillMaxWidth().padding(AsTheme.spacing.md),
            )
        }
        AsButton(
            text = "View availability",
            onClick = onViewAvailabilityClick,
            enabled = doctor.acceptingRequests,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
