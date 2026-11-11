package com.aarogyasanjeevani.doctor.ui.application

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.doctor.domain.model.DoctorStatus
import com.aarogyasanjeevani.doctor.ui.common.ErrorState
import com.aarogyasanjeevani.doctor.ui.common.LoadingState
import com.aarogyasanjeevani.doctor.ui.common.UiState

/**
 * Gates access to the doctor's main app. Only an `approved` application is
 * let through -- every other case shows the right next step and lets the
 * doctor pull to refresh once they expect the admin team has acted.
 */
@Composable
fun ApplicationGate(
    onApproved: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ApplicationViewModel = hiltViewModel(),
) {
    val statusState by viewModel.statusState.collectAsState()

    when (val current = statusState) {
        is UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(message = current.message, onRetry = viewModel::refreshStatus)
        is UiState.Success -> {
            val application = current.data
            when {
                application == null -> ApplicationFormScreen(isResubmission = false, viewModel = viewModel)
                application.status == DoctorStatus.APPROVED -> {
                    LaunchedEffect(Unit) { onApproved() }
                }
                application.status == DoctorStatus.PENDING -> StatusMessageScreen(
                    title = "Application under review",
                    message = "Our admin team is reviewing your application. This usually doesn't take long -- " +
                        "pull to refresh to check for an update.",
                    onRefresh = viewModel::refreshStatus,
                    onSignOut = onSignOut,
                )
                application.status == DoctorStatus.SUSPENDED -> StatusMessageScreen(
                    title = "Account suspended",
                    message = "Your account has been suspended. Please contact support for details.",
                    onRefresh = viewModel::refreshStatus,
                    onSignOut = onSignOut,
                )
                application.status == DoctorStatus.REJECTED -> RejectedScreen(
                    reason = application.rejectionReason,
                    viewModel = viewModel,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

@Composable
private fun StatusMessageScreen(
    title: String,
    message: String,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(onClick = onRefresh, modifier = Modifier.padding(top = 20.dp)) {
                Text("Check again")
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Composable
private fun RejectedScreen(
    reason: String?,
    viewModel: ApplicationViewModel,
    onSignOut: () -> Unit,
) {
    var showForm by remember { mutableStateOf(false) }

    if (showForm) {
        ApplicationFormScreen(isResubmission = true, viewModel = viewModel)
        return
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text("Application not approved", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            if (!reason.isNullOrBlank()) {
                Text(
                    "Reason: $reason",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlinedButton(onClick = { showForm = true }, modifier = Modifier.padding(top = 20.dp)) {
                Text("Update and resubmit")
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}
