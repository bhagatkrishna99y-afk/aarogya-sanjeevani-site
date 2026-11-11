package com.aarogyasanjeevani.doctor.ui.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.doctor.domain.model.CareRequest
import com.aarogyasanjeevani.doctor.domain.model.RequestStatus
import com.aarogyasanjeevani.doctor.ui.common.EmptyState
import com.aarogyasanjeevani.doctor.ui.common.ErrorState
import com.aarogyasanjeevani.doctor.ui.common.LoadingState
import com.aarogyasanjeevani.doctor.ui.common.UiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val FILTERS = listOf(
    "Pending" to setOf(RequestStatus.REQUESTED),
    "Accepted" to setOf(RequestStatus.ACCEPTED),
    "Confirmed" to setOf(RequestStatus.CONFIRMED),
    "Completed" to setOf(RequestStatus.COMPLETED),
    "Past" to setOf(RequestStatus.REJECTED, RequestStatus.CANCELLED),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val actioningId by viewModel.actioningId.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    var selectedFilter by remember { mutableStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("My requests") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTERS.forEachIndexed { index, (label, _) ->
                    val selected = index == selectedFilter
                    Surface(
                        onClick = { selectedFilter = index },
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }

            actionError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            when (val current = state) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = current.message, onRetry = viewModel::load)
                is UiState.Success -> {
                    val filtered = current.data.filter { it.status in FILTERS[selectedFilter].second }
                    if (filtered.isEmpty()) {
                        EmptyState(title = "Nothing here right now")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp)) {
                            items(filtered, key = { it.id }) { request ->
                                RequestCard(
                                    request = request,
                                    busy = actioningId == request.id,
                                    onAccept = { viewModel.accept(request.id) },
                                    onReject = { reason -> viewModel.reject(request.id, reason) },
                                    onConfirm = { viewModel.confirm(request.id) },
                                    onComplete = { viewModel.complete(request.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: CareRequest,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: (String) -> Unit,
    onConfirm: () -> Unit,
    onComplete: () -> Unit,
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    val local = request.scheduledStart.toLocalDateTime(TimeZone.currentSystemDefault())

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.serviceName, style = MaterialTheme.typography.titleMedium)
            Text(
                request.patientName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            request.patientPhone?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${local.dayOfMonth.toString().padStart(2, '0')}/${local.monthNumber.toString().padStart(2, '0')}/${local.year} · " +
                    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            request.patientNotes?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Notes: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (request.status) {
                    RequestStatus.REQUESTED -> {
                        Button(onClick = onAccept, enabled = !busy) { Text("Accept") }
                        OutlinedButton(onClick = { showRejectDialog = true }, enabled = !busy) { Text("Reject") }
                    }
                    RequestStatus.ACCEPTED -> {
                        Button(onClick = onConfirm, enabled = !busy) { Text("Confirm") }
                    }
                    RequestStatus.CONFIRMED -> {
                        Button(onClick = onComplete, enabled = !busy) { Text("Mark completed") }
                    }
                    else -> Unit
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject this request?") },
            text = {
                Column {
                    Text("Let the patient know why.")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(rejectReason)
                        showRejectDialog = false
                    },
                    enabled = rejectReason.isNotBlank(),
                ) { Text("Confirm reject") }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
            },
        )
    }
}
