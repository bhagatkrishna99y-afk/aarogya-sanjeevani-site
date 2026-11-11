package com.aarogyasanjeevani.patient.ui.booking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogyasanjeevani.patient.ui.common.LoadingButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDetailScreen(
    uiState: BookingUiState,
    onChooseDate: () -> Unit,
    onBack: () -> Unit,
) {
    val doctor = uiState.doctor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Doctor details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (doctor == null) return@Scaffold

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text(doctor.fullName, style = MaterialTheme.typography.headlineMedium)
            Text(
                doctor.specialization,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "${doctor.yearsExperience} years experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            doctor.bio?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
            }

            uiState.service?.let { service ->
                Text(
                    "Booking: ${service.name} (${service.durationMinutes} min)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            LoadingButton(
                text = "Choose a date",
                isLoading = false,
                onClick = onChooseDate,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
