package com.aarogyasanjeevani.patient.ui.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.patient.ui.common.ErrorState
import com.aarogyasanjeevani.patient.ui.common.LoadingButton
import com.aarogyasanjeevani.patient.ui.common.LoadingState
import com.aarogyasanjeevani.patient.ui.common.UiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: RequestDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(requestId) { viewModel.load(requestId) }

    val requestState by viewModel.request.collectAsState()
    val history by viewModel.history.collectAsState()
    val isCancelling by viewModel.isCancelling.collectAsState()
    val cancelError by viewModel.cancelError.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = requestState) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(message = current.message, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                val request = current.data
                val local = request.scheduledStart.toLocalDateTime(TimeZone.currentSystemDefault())

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
                ) {
                    Text(request.serviceName, style = MaterialTheme.typography.headlineMedium)
                    request.doctorName?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusBadge(status = request.status, modifier = Modifier.padding(top = 8.dp))

                    Text(
                        "${local.dayOfMonth}/${local.monthNumber}/${local.year} at " +
                            "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )

                    request.patientNotes?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "Your notes: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    request.doctorResponseNote?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "Doctor's note: $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (history.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                        Text("Status history", style = MaterialTheme.typography.titleMedium)
                        history.forEach { event ->
                            val changedLocal = event.changedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            Text(
                                text = buildString {
                                    append(event.oldStatus?.label()?.plus(" → ") ?: "Created as ")
                                    append(event.newStatus.label())
                                    append(" (${changedLocal.dayOfMonth}/${changedLocal.monthNumber} ")
                                    append("${changedLocal.hour.toString().padStart(2, '0')}:${changedLocal.minute.toString().padStart(2, '0')})")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    cancelError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    if (request.status.isCancellable) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        ) {
                            Text("Cancel request")
                        }
                    }
                }

                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Cancel this request?") },
                        text = {
                            Column {
                                Text("Let the doctor know why (optional).")
                                OutlinedTextField(
                                    value = cancelReason,
                                    onValueChange = { cancelReason = it },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                )
                            }
                        },
                        confirmButton = {
                            LoadingButton(
                                text = "Confirm cancel",
                                isLoading = isCancelling,
                                onClick = {
                                    viewModel.cancel(request.id, cancelReason.takeIf { it.isNotBlank() }) {
                                        showCancelDialog = false
                                    }
                                },
                            )
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) { Text("Keep request") }
                        },
                    )
                }
            }
        }
    }
}
