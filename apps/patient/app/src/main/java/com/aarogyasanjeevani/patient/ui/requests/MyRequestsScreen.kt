package com.aarogyasanjeevani.patient.ui.requests

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.patient.domain.model.BookingRequest
import com.aarogyasanjeevani.patient.domain.model.RequestStatus
import com.aarogyasanjeevani.patient.ui.common.EmptyState
import com.aarogyasanjeevani.patient.ui.common.ErrorState
import com.aarogyasanjeevani.patient.ui.common.LoadingState
import com.aarogyasanjeevani.patient.ui.common.UiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    onRequestSelected: (BookingRequest) -> Unit,
    viewModel: RequestsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("My requests") }) }) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                if (current.data.isEmpty()) {
                    EmptyState(
                        title = "No requests yet",
                        description = "Browse services from the Home tab to make your first request.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = padding.calculateTopPadding() + 16.dp, bottom = 16.dp,
                        ),
                    ) {
                        items(current.data, key = { it.id }) { request ->
                            RequestRow(request = request, onClick = { onRequestSelected(request) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestRow(request: BookingRequest, onClick: () -> Unit) {
    val local = request.scheduledStart.toLocalDateTime(TimeZone.currentSystemDefault())
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.serviceName, style = MaterialTheme.typography.titleMedium)
            request.doctorName?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${local.dayOfMonth}/${local.monthNumber}/${local.year} · " +
                    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            StatusBadge(status = request.status, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun StatusBadge(status: RequestStatus, modifier: Modifier = Modifier) {
    val (container, content) = statusColors(status)
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun statusColors(status: RequestStatus): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val scheme = MaterialTheme.colorScheme
    return when (status) {
        RequestStatus.REQUESTED -> scheme.surfaceVariant to scheme.onSurfaceVariant
        RequestStatus.ACCEPTED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RequestStatus.CONFIRMED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RequestStatus.COMPLETED -> com.aarogyasanjeevani.patient.ui.theme.Emerald100 to com.aarogyasanjeevani.patient.ui.theme.Emerald700
        RequestStatus.REJECTED -> scheme.errorContainer to scheme.error
        RequestStatus.CANCELLED -> com.aarogyasanjeevani.patient.ui.theme.Amber100 to com.aarogyasanjeevani.patient.ui.theme.Amber800
    }
}
