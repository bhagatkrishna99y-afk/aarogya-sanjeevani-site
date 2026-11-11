package com.aarogyasanjeevani.patient.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogyasanjeevani.patient.domain.model.Doctor
import com.aarogyasanjeevani.patient.ui.common.EmptyState
import com.aarogyasanjeevani.patient.ui.common.ErrorState
import com.aarogyasanjeevani.patient.ui.common.LoadingState
import com.aarogyasanjeevani.patient.ui.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorListScreen(
    uiState: BookingUiState,
    onDoctorSelected: (Doctor) -> Unit,
    onServiceDoesNotRequireDoctor: () -> Unit,
    onBack: () -> Unit,
) {
    // The booking graph always starts here; services that don't need a
    // doctor redirect straight to slot picking as soon as we know that.
    LaunchedEffect(uiState.service) {
        if (uiState.service?.requiresDoctor == false) onServiceDoesNotRequireDoctor()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.service?.name ?: "Choose a doctor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val doctors = uiState.doctorsState) {
            is UiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            is UiState.Error -> ErrorState(message = doctors.message, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                if (doctors.data.isEmpty()) {
                    EmptyState(
                        title = "No doctors available for this service yet",
                        description = "Please check back soon.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = padding.calculateTopPadding() + 16.dp, bottom = 16.dp,
                        ),
                    ) {
                        items(doctors.data, key = { it.id }) { doctor ->
                            Card(
                                onClick = { onDoctorSelected(doctor) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(doctor.fullName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        doctor.specialization,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "${doctor.yearsExperience} years experience",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
