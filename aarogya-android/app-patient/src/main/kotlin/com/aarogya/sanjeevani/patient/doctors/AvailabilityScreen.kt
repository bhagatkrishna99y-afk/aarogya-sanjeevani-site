@file:OptIn(ExperimentalLayoutApi::class)

package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.EmptyContent
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SLOT_SHIMMER_COUNT = 9
private val SUGGESTED_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

/**
 * The Availability screen. Never computes availability or conflicts
 * itself -- every slot shown came straight from
 * [com.aarogya.sanjeevani.core.data.requests.RequestsRepository.getOpenSlots],
 * via [AvailabilityViewModel]; this file only renders whatever state
 * that produced.
 */
@Composable
fun AvailabilityRoute(
    doctorId: String,
    doctorName: String,
    serviceName: String,
    onBack: () -> Unit,
    onContinue: (slotStart: Instant, slotDurationMinutes: Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AvailabilityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(AvailabilityAction.Resumed(doctorId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                AvailabilityUiEvent.SlotTaken ->
                    snackbarHostState.showSnackbar("That time was just taken -- please pick another.")
                is AvailabilityUiEvent.NavigateToRequestForm ->
                    onContinue(event.slotStart, event.slotDurationMinutes)
            }
        }
    }

    AvailabilityScreen(
        uiState = uiState,
        doctorName = doctorName,
        serviceName = serviceName,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, so a Compose UI test
 *  can drive it directly with a hand-built [AvailabilityUiState] and no
 *  Hilt/ViewModel, same reasoning as every other screen in this module. */
@Composable
internal fun AvailabilityScreen(
    uiState: AvailabilityUiState,
    doctorName: String,
    serviceName: String,
    onAction: (AvailabilityAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Availability", onBack = onBack) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "$doctorName · $serviceName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg, vertical = AsTheme.spacing.sm),
            )
            DateStrip(
                today = uiState.today,
                selectedDate = uiState.selectedDate,
                maxHorizonDays = uiState.maxHorizonDays,
                onDateSelected = { date -> onAction(AvailabilityAction.DateSelected(date)) },
            )
            Column(modifier = Modifier.fillMaxSize().weight(1f).padding(top = AsTheme.spacing.md)) {
                SlotsContent(uiState = uiState, onAction = onAction)
            }
            ContinueButton(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun SlotsContent(
    uiState: AvailabilityUiState,
    onAction: (AvailabilityAction) -> Unit,
) {
    when (val section = uiState.slotsSection) {
        SlotsSectionState.Loading ->
            FlowRow(
                modifier = Modifier.padding(horizontal = AsTheme.spacing.lg).loadingGroupSemantics(),
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                repeat(SLOT_SHIMMER_COUNT) { SkeletonShimmer(modifier = Modifier.width(90.dp).height(36.dp)) }
            }
        is SlotsSectionState.Error ->
            ErrorContent(error = section.error, onRetry = { onAction(AvailabilityAction.Retry) })
        SlotsSectionState.DoctorUnavailable ->
            EmptyContent(message = "This doctor isn't accepting new requests right now.")
        is SlotsSectionState.Empty -> EmptySlotsContent(suggestedDate = section.suggestedDate, onAction = onAction)
        is SlotsSectionState.Content ->
            SlotsGrid(slots = section.slots, selectedSlot = uiState.selectedSlot, onAction = onAction)
    }
}

@Composable
private fun EmptySlotsContent(
    suggestedDate: LocalDate?,
    onAction: (AvailabilityAction) -> Unit,
) {
    EmptyContent(
        message = "No times are available on this date.",
        action =
            if (suggestedDate != null) {
                {
                    AsButton(
                        text = "Try ${SUGGESTED_DATE_FORMAT.format(suggestedDate.toJavaLocalDate())}",
                        onClick = { onAction(AvailabilityAction.DateSelected(suggestedDate)) },
                        variant = AsButtonVariant.Secondary,
                    )
                }
            } else {
                { Text("Try picking another date above.", style = MaterialTheme.typography.bodyMedium) }
            },
    )
}

@Composable
private fun SlotsGrid(
    slots: List<OpenSlot>,
    selectedSlot: OpenSlot?,
    onAction: (AvailabilityAction) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
    ) {
        slots.groupByPeriod(CLINIC_TIME_ZONE).forEach { (period, periodSlots) ->
            Text(text = period.label, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                periodSlots.forEach { slot ->
                    TimeSlotChip(
                        slot = slot,
                        selected = slot.slotStart == selectedSlot?.slotStart,
                        zone = CLINIC_TIME_ZONE,
                        onClick = { onAction(AvailabilityAction.SlotSelected(slot)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(
    uiState: AvailabilityUiState,
    onAction: (AvailabilityAction) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(AsTheme.spacing.lg), horizontalArrangement = Arrangement.Center) {
        AsButton(
            text = "Continue",
            onClick = { onAction(AvailabilityAction.ContinueClicked) },
            enabled = uiState.selectedSlot != null && !uiState.isContinuing,
            loading = uiState.isContinuing,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
