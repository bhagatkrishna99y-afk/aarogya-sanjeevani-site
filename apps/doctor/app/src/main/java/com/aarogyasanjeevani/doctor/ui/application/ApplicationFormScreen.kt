package com.aarogyasanjeevani.doctor.ui.application

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aarogyasanjeevani.doctor.ui.common.LoadingButton

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ApplicationFormScreen(
    isResubmission: Boolean,
    viewModel: ApplicationViewModel,
) {
    val formState by viewModel.formState.collectAsState()
    val submitted by viewModel.submitted.collectAsState()

    LaunchedEffect(submitted) {
        if (submitted) viewModel.acknowledgeSubmitted()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isResubmission) "Update your application" else "Complete your profile") })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
        ) {
            Text(
                "This information is reviewed by our admin team before you can accept requests.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = formState.fullName,
                onValueChange = viewModel::updateFullName,
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            OutlinedTextField(
                value = formState.phone,
                onValueChange = viewModel::updatePhone,
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = formState.specialization,
                onValueChange = viewModel::updateSpecialization,
                label = { Text("Specialization") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = formState.licenseNumber,
                onValueChange = viewModel::updateLicenseNumber,
                label = { Text("License number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = formState.yearsExperience,
                onValueChange = viewModel::updateYearsExperience,
                label = { Text("Years of experience") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            OutlinedTextField(
                value = formState.bio,
                onValueChange = viewModel::updateBio,
                label = { Text("Bio (shown to patients)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            formState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            LoadingButton(
                text = if (isResubmission) "Resubmit application" else "Submit application",
                isLoading = formState.isSubmitting,
                onClick = viewModel::submit,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
