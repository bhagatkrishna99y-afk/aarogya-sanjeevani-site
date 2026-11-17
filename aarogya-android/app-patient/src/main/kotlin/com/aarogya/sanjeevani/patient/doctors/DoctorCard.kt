package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsBadge
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.services.icon
import com.aarogya.sanjeevani.patient.services.label

private const val MAX_SERVICE_CHIPS = 3

/**
 * The one doctor summary row both Doctors Directory and Doctors-for-
 * service use, so a doctor looks identical regardless of which list led
 * there. [avatarUrl] is already resolved (a signed URL, never the raw
 * [DoctorPublic.avatarPath] -- see [avatarUrlsByDoctorId]'s doc comment)
 * and optional: [AsAvatar] itself falls back to initials for `null` or
 * a failed load. [offeredServiceNames] is capped to [MAX_SERVICE_CHIPS]
 * here, with a "+N" tag for the rest, rather than letting a doctor who
 * offers a dozen services blow out every card's height.
 */
@Composable
fun DoctorCard(
    doctor: DoctorPublic,
    avatarUrl: String?,
    offeredServiceNames: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md)) {
            AsAvatar(name = doctor.fullName, imageUrl = avatarUrl, size = 56.dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs),
            ) {
                Text(
                    text = doctorDisplayName(doctor.fullName, doctor.title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = doctor.specialization,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val bio = doctor.bio
                if (!bio.isNullOrBlank()) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (offeredServiceNames.isNotEmpty()) {
                    ServiceTagsRow(offeredServiceNames)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
                    doctor.consultationModes.forEach { mode ->
                        Icon(
                            imageVector = mode.icon(),
                            contentDescription = mode.label(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.height(16.dp),
                        )
                    }
                }
                AcceptingRequestsIndicator(acceptingRequests = doctor.acceptingRequests)
            }
        }
    }
}

@Composable
private fun ServiceTagsRow(names: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.xs)) {
        names.take(MAX_SERVICE_CHIPS).forEach { name ->
            AsBadge(
                text = name,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val remaining = names.size - MAX_SERVICE_CHIPS
        if (remaining > 0) {
            AsBadge(
                text = "+$remaining",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AcceptingRequestsIndicator(acceptingRequests: Boolean) {
    val color = if (acceptingRequests) AsTheme.extendedColors.success else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = if (acceptingRequests) "Accepting requests" else "Not accepting requests",
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}
