package com.aarogya.sanjeevani.patient.booking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsRequestStatus
import com.aarogya.sanjeevani.core.designsystem.component.StatusChip
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * Step 3 -- the confirmation shown right after
 * [com.aarogya.sanjeevani.core.data.requests.RequestsRepository.createRequest]
 * succeeds. Reuses the same shared [RequestFormViewModel] instance as
 * [RequestFormRoute]/[ReviewRoute] purely to read back
 * [RequestFormUiState.submittedRequestId] and the summary fields --
 * nothing here mutates it further. [onDoneClick] (wired in
 * `PatientNavHost.kt`'s `bookingFlow`) pops the whole booking flow off
 * the back stack back to the Home tab; [BackHandler] routes the
 * hardware/gesture back action through the exact same callback so a
 * patient can never navigate back into the now-stale form.
 */
@Composable
fun SuccessRoute(
    viewModel: RequestFormViewModel,
    onDoneClick: () -> Unit,
    onViewRequestClick: (requestId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onDoneClick)

    SuccessScreen(
        uiState = uiState,
        onDoneClick = onDoneClick,
        onViewRequestClick = onViewRequestClick,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this module. */
@Composable
internal fun SuccessScreen(
    uiState: RequestFormUiState,
    onDoneClick: () -> Unit,
    onViewRequestClick: (requestId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AsTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Request sent", style = MaterialTheme.typography.headlineSmall)
            StatusChip(status = AsRequestStatus.Requested)
            AsCard {
                Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
                    Text(text = uiState.serviceName, style = MaterialTheme.typography.titleMedium)
                    if (uiState.doctorName != null) {
                        Text(
                            text = uiState.doctorName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val slotStart = uiState.slotStart
                    if (slotStart != null) {
                        Text(
                            text = formatSlotSummary(slotStart, uiState.slotDurationMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = "We'll notify you as soon as the doctor accepts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val requestId = uiState.submittedRequestId
            if (requestId != null) {
                AsButton(
                    text = "View request",
                    onClick = { onViewRequestClick(requestId) },
                    variant = AsButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AsButton(
                text = "Back to Home",
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
