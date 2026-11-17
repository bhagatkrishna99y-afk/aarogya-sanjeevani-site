package com.aarogya.sanjeevani.patient.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.services.label

/**
 * Step 2 of the booking flow. Reuses [RequestFormRoute]'s own
 * [RequestFormViewModel] instance (see `PatientNavHost.kt`'s
 * `bookingFlow`) rather than re-reading any nav args of its own --
 * every field shown below came from the patient's own step-1 input.
 * [snackbarHostState] is the *app-level* one (survives this
 * destination popping, unlike a locally-`remember`-ed one would), so a
 * message shown here is still visible after [onSlotUnavailable]/
 * [onDoctorUnavailable] navigate away.
 */
@Composable
fun ReviewRoute(
    viewModel: RequestFormViewModel,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onSlotUnavailable: () -> Unit,
    onDoctorUnavailable: () -> Unit,
    onSubmitted: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is RequestFormUiEvent.SlotUnavailable -> {
                    snackbarHostState.showSnackbar(event.error.toUserMessage(context))
                    onSlotUnavailable()
                }
                is RequestFormUiEvent.DoctorUnavailable -> {
                    snackbarHostState.showSnackbar(event.error.toUserMessage(context))
                    onDoctorUnavailable()
                }
                is RequestFormUiEvent.LimitExceeded ->
                    snackbarHostState.showSnackbar(event.error.toUserMessage(context))
                is RequestFormUiEvent.RetryableError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = event.error.toUserMessage(context),
                            actionLabel = "Retry",
                            duration = SnackbarDuration.Long,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onAction(RequestFormAction.SubmitClicked)
                    }
                }
                RequestFormUiEvent.NavigateToSuccess -> onSubmitted()
                RequestFormUiEvent.NavigateToReview -> Unit
            }
        }
    }

    ReviewScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onEditClick = onEditClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this module. */
@Composable
internal fun ReviewScreen(
    uiState: RequestFormUiState,
    onAction: (RequestFormAction) -> Unit,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Review request", onBack = onBack) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(AsTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
        ) {
            ReviewSummaryCard(uiState = uiState)
            InfoNote(
                text =
                    "The doctor must accept this request before it's confirmed -- " +
                        "your slot is only reserved once they accept.",
            )
            uiState.cancellationNoticeMinutes?.let { minutes ->
                InfoNote(text = formatCancellationPolicy(minutes))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AsButton(
                    text = "Edit",
                    onClick = onEditClick,
                    variant = AsButtonVariant.Secondary,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
                AsButton(
                    text = "Submit request",
                    onClick = { onAction(RequestFormAction.SubmitClicked) },
                    enabled = !uiState.isSubmitting,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ReviewSummaryCard(uiState: RequestFormUiState) {
    AsCard {
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
            Text(text = uiState.serviceName, style = MaterialTheme.typography.titleMedium)
            if (uiState.doctorName != null) {
                ReviewRow(label = "Doctor", value = uiState.doctorName)
            }
            val slotStart = uiState.slotStart
            if (slotStart != null) {
                ReviewRow(label = "Date & time", value = formatSlotSummary(slotStart, uiState.slotDurationMinutes))
            }
            uiState.selectedMode?.let { mode -> ReviewRow(label = "Mode", value = mode.label()) }
            ReviewRow(label = "Contact phone", value = uiState.contactPhone)
            if (uiState.selectedMode == ServiceMode.HOME_VISIT) {
                ReviewRow(label = "Address", value = uiState.address)
            }
            if (uiState.notes.isNotBlank()) {
                ReviewRow(label = "Notes for the doctor", value = uiState.notes)
            }
        }
    }
}

@Composable
private fun ReviewRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
