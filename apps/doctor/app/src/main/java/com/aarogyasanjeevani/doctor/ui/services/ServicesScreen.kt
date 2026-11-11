package com.aarogyasanjeevani.doctor.ui.services

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.doctor.ui.common.EmptyState
import com.aarogyasanjeevani.doctor.ui.common.ErrorState
import com.aarogyasanjeevani.doctor.ui.common.LoadingButton
import com.aarogyasanjeevani.doctor.ui.common.LoadingState
import com.aarogyasanjeevani.doctor.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: ServicesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissMessage()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("My services") }) }) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                if (current.data.services.isEmpty()) {
                    EmptyState(
                        title = "No bookable services configured yet",
                        description = "Check back once the admin team adds services.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    Column(modifier = Modifier.padding(padding)) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(current.data.services, key = { it.id }) { service ->
                                val checked = service.id in current.data.selectedIds
                                ListItem(
                                    headlineContent = { Text(service.name) },
                                    supportingContent = service.description?.let { { Text(it) } },
                                    trailingContent = {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { viewModel.toggle(service.id) },
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }

                        message?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        LoadingButton(
                            text = "Save",
                            isLoading = isSaving,
                            onClick = viewModel::save,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
