package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic
import com.aarogya.sanjeevani.core.designsystem.component.AsListItem
import com.aarogya.sanjeevani.core.designsystem.component.SectionHeader
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * Reached only from [DoctorDetailScreen]'s "View availability" when no
 * service id arrived with it (the plain-directory path -- see that
 * screen's own doc comment) -- lets the patient pick which of this
 * doctor's own [services] to continue booking with, rather than this
 * screen guessing. A doctor with zero [services] never gets here in
 * the first place (`DoctorDetailScreen.kt` only shows "View
 * availability" at all when there's at least one to offer -- see its
 * own `AsButton` wiring), so there is no "nothing to pick" case to
 * render here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicePickerBottomSheet(
    services: List<DoctorServicePublic>,
    onServiceSelected: (DoctorServicePublic) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        SectionHeader(
            title = "Choose a service",
            modifier = Modifier.padding(horizontal = AsTheme.spacing.lg),
        )
        services.forEach { service ->
            AsListItem(
                headline = service.serviceName,
                onClick = { onServiceSelected(service) },
                modifier = Modifier.padding(horizontal = AsTheme.spacing.sm),
            )
        }
        Spacer(modifier = Modifier.navigationBarsPadding().padding(bottom = AsTheme.spacing.sm))
    }
}
