package com.aarogyasanjeevani.patient.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val isBusy by viewModel.isSavingOrSigningOut.collectAsState()
    val message by viewModel.message.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Profile") }) }) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                var fullName by remember(current.data.fullName) { mutableStateOf(current.data.fullName) }
                var phone by remember(current.data.phone) { mutableStateOf(current.data.phone ?: "") }

                LaunchedEffect(message) {
                    if (message != null) {
                        kotlinx.coroutines.delay(2500)
                        viewModel.dismissMessage()
                    }
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )

                    message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }

                    LoadingButton(
                        text = "Save changes",
                        isLoading = isBusy,
                        onClick = { viewModel.save(fullName.trim(), phone.trim().takeIf { it.isNotBlank() }) },
                        modifier = Modifier.padding(top = 20.dp),
                    )

                    OutlinedButton(
                        onClick = { viewModel.signOut(onSignedOut) },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Text("Sign out")
                    }
                }
            }
        }
    }
}
