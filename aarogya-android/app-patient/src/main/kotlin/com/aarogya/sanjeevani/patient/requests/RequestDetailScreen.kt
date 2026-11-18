@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.StatusChip
import com.aarogya.sanjeevani.core.designsystem.component.TimelineItem
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.home.toAsRequestStatus
import com.aarogya.sanjeevani.patient.services.label

/**
 * Request Detail. Never computes a request's status or history itself
 * -- everything shown came straight from `patient_requests_view`/
 * `request_status_history` via [RequestDetailViewModel].
 */
@Composable
fun RequestDetailRoute(
    requestId: String,
    onBack: () -> Unit,
    onBookAgainClick: (serviceId: String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: RequestDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(RequestDetailAction.Resumed(requestId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                RequestDetailUiEvent.CancelSucceeded -> snackbarHostState.showSnackbar("Request cancelled.")
                is RequestDetailUiEvent.CancelConflict ->
                    snackbarHostState.showSnackbar(event.error.toUserMessage(context))
                is RequestDetailUiEvent.CancelFailed ->
                    snackbarHostState.showSnackbar(event.error.toUserMessage(context))
            }
        }
    }

    RequestDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onBookAgainClick = onBookAgainClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this module. */
@Composable
internal fun RequestDetailScreen(
    uiState: RequestDetailUiState,
    onAction: (RequestDetailAction) -> Unit,
    onBack: () -> Unit,
    onBookAgainClick: (serviceId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Request details", onBack = onBack) },
    ) { padding ->
        when (val section = uiState.section) {
            RequestDetailSectionState.Loading -> ShimmerDetail(Modifier.fillMaxSize().padding(padding))
            is RequestDetailSectionState.Error ->
                ErrorContent(
                    error = section.error,
                    onRetry = { onAction(RequestDetailAction.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            is RequestDetailSectionState.Content ->
                RequestDetailContent(
                    section = section,
                    onAction = onAction,
                    onBookAgainClick = onBookAgainClick,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }

    if (uiState.showCancelDialog) {
        CancelRequestDialog(
            reason = uiState.cancelReason,
            isCancelling = uiState.isCancelling,
            onReasonChanged = { onAction(RequestDetailAction.CancelReasonChanged(it)) },
            onConfirm = { onAction(RequestDetailAction.CancelConfirmed) },
            onDismiss = { onAction(RequestDetailAction.CancelDismissed) },
        )
    }
}

@Composable
private fun ShimmerDetail(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AsTheme.spacing.lg).loadingGroupSemantics(),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
    ) {
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(32.dp))
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(160.dp))
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(200.dp))
    }
}

@Composable
private fun RequestDetailContent(
    section: RequestDetailSectionState.Content,
    onAction: (RequestDetailAction) -> Unit,
    onBookAgainClick: (serviceId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val request = section.request
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
    ) {
        RequestHeader(request = request)
        RequestDetailsCard(request = request)
        if (request.status == RequestStatus.REJECTED) {
            RejectedCard(request = request, onBookAgainClick = onBookAgainClick)
        }
        RequestTimeline(timeline = section.timeline)
        if (request.status.isCancellable) {
            AsButton(
                text = "Cancel request",
                onClick = { onAction(RequestDetailAction.CancelClicked) },
                variant = AsButtonVariant.Danger,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RequestHeader(request: PatientRequest) {
    Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
        StatusChip(status = request.status.toAsRequestStatus())
        Text(text = request.status.explanation(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RequestDetailsCard(request: PatientRequest) {
    AsCard {
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
            Text(text = request.serviceName, style = MaterialTheme.typography.titleMedium)
            val doctorFullName = request.doctorFullName
            val doctorSpecialization = request.doctorSpecialization
            if (doctorFullName != null) {
                val doctorValue =
                    if (doctorSpecialization != null) "$doctorFullName -- $doctorSpecialization" else doctorFullName
                DetailRow(label = "Doctor", value = doctorValue)
            }
            DetailRow(label = "Date & time", value = formatRequestDateTime(request.scheduledStart))
            DetailRow(label = "Mode", value = request.mode.label())
            val addressText = request.addressText
            if (!addressText.isNullOrBlank()) {
                DetailRow(label = "Address", value = addressText)
            }
            val patientNotes = request.patientNotes
            if (!patientNotes.isNullOrBlank()) {
                DetailRow(label = "Your notes", value = patientNotes)
            }
            val contactPhone = request.contactPhone
            if (!contactPhone.isNullOrBlank()) {
                DetailRow(label = "Contact phone", value = contactPhone)
            }
            DetailRow(label = "Requested on", value = formatRequestDateTime(request.createdAt))
        }
    }
}

@Composable
private fun RejectedCard(
    request: PatientRequest,
    onBookAgainClick: (serviceId: String) -> Unit,
) {
    AsCard {
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
            val rejectionReason = request.rejectionReason
            if (!rejectionReason.isNullOrBlank()) {
                DetailRow(label = "Doctor's reason", value = rejectionReason)
            }
            AsButton(
                text = "Book again",
                onClick = { onBookAgainClick(request.serviceId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RequestTimeline(timeline: List<RequestTimelineEntry>) {
    if (timeline.isEmpty()) return
    Column {
        Text(text = "History", style = MaterialTheme.typography.titleSmall)
        Column(modifier = Modifier.padding(top = AsTheme.spacing.sm)) {
            timeline.forEachIndexed { index, entry ->
                TimelineItem(
                    title = entry.label,
                    timestamp = "${entry.relativeTime} -- ${entry.absoluteTime}",
                    actor = entry.actor,
                    isCompleted = true,
                    isLast = index == timeline.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
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

/** A custom [AlertDialog] rather than the shared [com.aarogya.sanjeevani.core.designsystem.component.AsDialog]
 *  -- that one's `text` slot is a plain string, and this dialog needs a
 *  composable body (the optional reason field) inside it. */
@Composable
private fun CancelRequestDialog(
    reason: String,
    isCancelling: Boolean,
    onReasonChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this request?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                Text("The doctor will be notified. This can't be undone.")
                AsTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    label = "Reason (optional)",
                    singleLine = false,
                )
            }
        },
        confirmButton = {
            AsButton(text = "Cancel request", onClick = onConfirm, loading = isCancelling, enabled = !isCancelling)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCancelling) { Text("Keep request") }
        },
    )
}
