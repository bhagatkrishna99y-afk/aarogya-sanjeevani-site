package com.aarogya.sanjeevani.patient.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.StatusChip
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.home.toAsRequestStatus
import com.aarogya.sanjeevani.patient.services.icon

/** One row in the Requests tab -- identical shape for Active and Past,
 *  the only difference between the two tabs is which requests and
 *  which sort order [RequestsListViewModel] hands this. */
@Composable
fun RequestCard(
    request: PatientRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(text = request.serviceName, style = MaterialTheme.typography.titleMedium)
                StatusChip(status = request.status.toAsRequestStatus())
            }
            val doctorFullName = request.doctorFullName
            if (doctorFullName != null) {
                Text(
                    text = doctorFullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = request.mode.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = formatRequestDateTime(request.scheduledStart),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
