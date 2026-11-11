package com.aarogyasanjeevani.patient.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogyasanjeevani.patient.ui.common.LoadingButton
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRequestScreen(
    uiState: BookingUiState,
    viewModel: BookingViewModel,
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(uiState.submitSucceeded) {
        if (uiState.submitSucceeded) onSubmitted()
    }

    val slot = uiState.selectedSlot
    val service = uiState.service

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review your request") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow(label = "Service", value = service?.name ?: "")
                    uiState.doctor?.let { SummaryRow(label = "Doctor", value = it.fullName) }
                    if (slot != null) {
                        val local = slot.start.toLocalDateTime(TimeZone.currentSystemDefault())
                        SummaryRow(
                            label = "Date & time",
                            value = "${local.dayOfMonth}/${local.monthNumber}/${local.year} at " +
                                "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
                        )
                    }
                    service?.let { SummaryRow(label = "Duration", value = "${it.durationMinutes} minutes") }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Anything the doctor should know?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                placeholder = { Text("Optional notes about your request") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            uiState.submitError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            LoadingButton(
                text = "Submit request",
                isLoading = uiState.isSubmitting,
                enabled = slot != null,
                onClick = viewModel::submit,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
