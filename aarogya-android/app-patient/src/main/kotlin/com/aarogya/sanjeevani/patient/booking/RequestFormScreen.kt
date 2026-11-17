@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.services.icon
import com.aarogya.sanjeevani.patient.services.label

/**
 * Step 1 of the booking flow. [serviceId]/[doctorId]/[slotStart] etc.
 * all arrive as [com.aarogya.sanjeevani.patient.navigation.RequestForm]'s
 * own nav args, read once by [RequestFormViewModel] -- this route
 * itself only reacts to [RequestFormUiEvent.NavigateToReview].
 */
@Composable
fun RequestFormRoute(
    onBack: () -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RequestFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is RequestFormUiEvent.NavigateToReview) onReviewClick()
        }
    }

    RequestFormScreen(uiState = uiState, onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}

/** Stateless half -- `internal`, not `private`, same reasoning as
 *  every other screen in this module. */
@Composable
internal fun RequestFormScreen(
    uiState: RequestFormUiState,
    onAction: (RequestFormAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Request details", onBack = onBack) },
    ) { padding ->
        when (val context = uiState.contextState) {
            RequestFormContextState.Loading ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(AsTheme.spacing.lg)
                            .loadingGroupSemantics(),
                    verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
                ) {
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(100.dp))
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(56.dp))
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(56.dp))
                }
            is RequestFormContextState.Error ->
                ErrorContent(
                    error = context.error,
                    onRetry = { onAction(RequestFormAction.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            is RequestFormContextState.Loaded ->
                RequestFormContent(
                    uiState = uiState,
                    availableModes = context.availableModes,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }
}

@Composable
private fun RequestFormContent(
    uiState: RequestFormUiState,
    availableModes: List<ServiceMode>,
    onAction: (RequestFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
    ) {
        SummaryCard(uiState = uiState)

        if (uiState.slotStart == null) {
            Text(
                text = "Choosing a date and time for this service isn't available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
            Text(text = "Service mode", style = MaterialTheme.typography.titleSmall)
            availableModes.forEach { mode ->
                ModeCard(
                    mode = mode,
                    selected = uiState.selectedMode == mode,
                    onClick = { onAction(RequestFormAction.ModeSelected(mode)) },
                )
            }
        }

        ContactPhoneField(uiState = uiState, onAction = onAction)

        if (uiState.selectedMode == ServiceMode.HOME_VISIT) {
            AddressField(uiState = uiState, onAction = onAction)
        }

        NotesField(notes = uiState.notes, onAction = onAction)

        AsButton(
            text = "Continue",
            onClick = { onAction(RequestFormAction.ContinueClicked) },
            enabled = uiState.slotStart != null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryCard(uiState: RequestFormUiState) {
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
}

@Composable
private fun ModeCard(
    mode: ServiceMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor =
            if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AsTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = mode.icon(), contentDescription = null)
            Text(text = mode.label(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ContactPhoneField(
    uiState: RequestFormUiState,
    onAction: (RequestFormAction) -> Unit,
) {
    AsTextField(
        value = uiState.contactPhone,
        onValueChange = { onAction(RequestFormAction.ContactPhoneChanged(it)) },
        label = "Contact phone",
        modifier = Modifier.onFocusChanged { if (!it.isFocused) onAction(RequestFormAction.ContactPhoneFocusLost) },
        errorText =
            if (uiState.contactPhoneTouched) {
                uiState.fieldErrors["contactPhone"]?.let { "Please enter a valid contact phone number." }
            } else {
                null
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )
}

@Composable
private fun AddressField(
    uiState: RequestFormUiState,
    onAction: (RequestFormAction) -> Unit,
) {
    AsTextField(
        value = uiState.address,
        onValueChange = { onAction(RequestFormAction.AddressChanged(it)) },
        label = "Address",
        modifier = Modifier.onFocusChanged { if (!it.isFocused) onAction(RequestFormAction.AddressFocusLost) },
        errorText =
            if (uiState.addressTouched) {
                uiState.fieldErrors["address"]?.let { "Please provide an address for a home visit." }
            } else {
                null
            },
        singleLine = false,
    )
}

@Composable
private fun NotesField(
    notes: String,
    onAction: (RequestFormAction) -> Unit,
) {
    AsTextField(
        value = notes,
        onValueChange = {
            if (it.length <= RequestFormValidation.NOTES_MAX_LENGTH) onAction(RequestFormAction.NotesChanged(it))
        },
        label = "Notes for the doctor (optional)",
        helperText =
            "Do not include emergency information or sensitive medical records. " +
                "${notes.length}/${RequestFormValidation.NOTES_MAX_LENGTH}",
        singleLine = false,
    )
}
