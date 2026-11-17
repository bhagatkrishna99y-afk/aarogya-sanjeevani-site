package com.aarogya.sanjeevani.patient.services

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent

private const val MINUTES_PER_HOUR = 60

/**
 * The Service Detail screen. [onFindDoctorsClick] and
 * [onContinueWithoutDoctorClick] are both wired by the caller regardless
 * of [Service.requiresDoctor] -- this screen itself decides which one
 * "Continue" actually calls once the service has loaded (see
 * [ServiceDetailScreen]'s own `AsButton`), so the navigation graph never
 * has to know that distinction either.
 */
@Composable
fun ServiceDetailRoute(
    serviceId: String,
    onBack: () -> Unit,
    onFindDoctorsClick: (serviceName: String) -> Unit,
    onContinueWithoutDoctorClick: (serviceName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(ServiceDetailAction.Resumed(serviceId))
    }

    ServiceDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = { viewModel.onAction(ServiceDetailAction.Retry) },
        onFindDoctorsClick = onFindDoctorsClick,
        onContinueWithoutDoctorClick = onContinueWithoutDoctorClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, for the same reason as
 *  every other screen in this module (see `HomeScreen.kt`'s doc comment). */
@Composable
internal fun ServiceDetailScreen(
    uiState: ServiceDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFindDoctorsClick: (serviceName: String) -> Unit,
    onContinueWithoutDoctorClick: (serviceName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Service", onBack = onBack) },
    ) { padding ->
        when (uiState) {
            is ServiceDetailUiState.Loading ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(AsTheme.spacing.lg)
                            .loadingGroupSemantics(),
                    verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
                ) {
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(28.dp))
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(100.dp))
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(48.dp))
                }
            is ServiceDetailUiState.Error ->
                ErrorContent(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            is ServiceDetailUiState.Content ->
                ServiceDetailContent(
                    uiState = uiState,
                    onFindDoctorsClick = onFindDoctorsClick,
                    onContinueWithoutDoctorClick = onContinueWithoutDoctorClick,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }
}

@Composable
private fun ServiceDetailContent(
    uiState: ServiceDetailUiState.Content,
    onFindDoctorsClick: (serviceName: String) -> Unit,
    onContinueWithoutDoctorClick: (serviceName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val service = uiState.service
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
    ) {
        if (uiState.categoryName != null) {
            Text(
                text = uiState.categoryName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = service.name, style = MaterialTheme.typography.headlineSmall)
        if (uiState.isUnavailable) {
            Text(
                text = "This service is no longer available.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        val description = service.description
        if (description != null) {
            Text(text = description, style = MaterialTheme.typography.bodyLarge)
        }
        SupportedModesRow(service = service)
        Text(
            text = "Duration: ${formatDuration(service.durationMinutes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                if (service.requiresDoctor) {
                    "You'll choose a doctor for this service."
                } else {
                    "No doctor selection is needed for this service."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AsButton(
            text = "Continue",
            onClick = {
                if (service.requiresDoctor) {
                    onFindDoctorsClick(service.name)
                } else {
                    onContinueWithoutDoctorClick(service.name)
                }
            },
            enabled = !uiState.isUnavailable,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SupportedModesRow(service: Service) {
    Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg)) {
        service.supportedModes.forEach { mode ->
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

private fun formatDuration(minutes: Int): String =
    if (minutes % MINUTES_PER_HOUR == 0) {
        val hours = minutes / MINUTES_PER_HOUR
        if (hours == 1) "1 hour" else "$hours hours"
    } else {
        "$minutes minutes"
    }
