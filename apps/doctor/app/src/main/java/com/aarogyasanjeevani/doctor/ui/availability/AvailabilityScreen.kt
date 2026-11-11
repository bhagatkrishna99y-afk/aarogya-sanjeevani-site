package com.aarogyasanjeevani.doctor.ui.availability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.doctor.domain.model.DAY_NAMES
import com.aarogyasanjeevani.doctor.ui.common.ErrorState
import com.aarogyasanjeevani.doctor.ui.common.LoadingButton
import com.aarogyasanjeevani.doctor.ui.common.LoadingState
import com.aarogyasanjeevani.doctor.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(viewModel: AvailabilityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissMessage()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Weekly availability") }) }) { padding ->
        when (val current = state) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(
                message = current.message,
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            is UiState.Success -> {
                Column(modifier = Modifier.padding(padding)) {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                        items(current.data, key = { it.dayOfWeek }) { day ->
                            DayCard(day = day, onChange = { transform -> viewModel.updateDay(day.dayOfWeek, transform) })
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
                        text = "Save availability",
                        isLoading = isSaving,
                        onClick = viewModel::save,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: DayEditState, onChange: (DayEditState.() -> DayEditState) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(DAY_NAMES[day.dayOfWeek], style = MaterialTheme.typography.titleMedium)
                Switch(checked = day.enabled, onCheckedChange = { checked -> onChange { copy(enabled = checked) } })
            }

            if (day.enabled) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = day.startTimeText,
                        onValueChange = { value -> onChange { copy(startTimeText = value) } },
                        label = { Text("Start (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = day.endTimeText,
                        onValueChange = { value -> onChange { copy(endTimeText = value) } },
                        label = { Text("End (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = day.slotMinutesText,
                    onValueChange = { value -> onChange { copy(slotMinutesText = value.filter { it.isDigit() }) } },
                    label = { Text("Slot length (minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
