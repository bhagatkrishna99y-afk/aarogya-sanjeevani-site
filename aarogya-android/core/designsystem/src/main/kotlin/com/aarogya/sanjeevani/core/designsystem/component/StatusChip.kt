package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsExtendedColors
import com.aarogya.sanjeevani.core.designsystem.theme.AsStatusColor
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.designsystem.theme.PillShape

// Only shown in the preview, below -- the full 6-entry set wouldn't fit one row.
private const val PREVIEW_REQUEST_STATUS_COUNT = 3

/**
 * This design system's own presentation-layer status vocabulary --
 * **not** the same type as `core.data.requests.RequestStatus` (the
 * `@Serializable` DTO enum that mirrors the `requests` table). Kept
 * deliberately separate: this module has no dependency on `core:data`
 * and shouldn't gain one just to let a chip render, so a feature screen
 * maps its real domain status to one of these with a small `when` at
 * the call site (or a `core:ui`-level extension function, once one is
 * needed) rather than this module importing the data layer's type.
 */
enum class AsRequestStatus(val label: String, val icon: ImageVector) {
    Requested(label = "Requested", icon = Icons.Outlined.Schedule),
    Accepted(label = "Accepted", icon = Icons.Outlined.ThumbUp),
    Confirmed(label = "Confirmed", icon = Icons.Outlined.EventAvailable),
    Completed(label = "Completed", icon = Icons.Outlined.CheckCircle),
    Rejected(label = "Rejected", icon = Icons.Outlined.Cancel),
    Cancelled(label = "Cancelled", icon = Icons.Outlined.Block),
}

/** Same note as [AsRequestStatus] -- a presentation-layer vocabulary,
 *  not tied to wherever the doctor-verification domain model ends up. */
enum class AsDoctorStatus(val label: String, val icon: ImageVector) {
    Pending(label = "Pending", icon = Icons.Outlined.HourglassEmpty),
    Approved(label = "Approved", icon = Icons.Outlined.Verified),
    Rejected(label = "Rejected", icon = Icons.Outlined.Cancel),
    Suspended(label = "Suspended", icon = Icons.Outlined.PauseCircle),
}

private fun AsRequestStatus.colorOf(colors: AsExtendedColors): AsStatusColor =
    when (this) {
        AsRequestStatus.Requested -> colors.requestRequested
        AsRequestStatus.Accepted -> colors.requestAccepted
        AsRequestStatus.Confirmed -> colors.requestConfirmed
        AsRequestStatus.Completed -> colors.requestCompleted
        AsRequestStatus.Rejected -> colors.requestRejected
        AsRequestStatus.Cancelled -> colors.requestCancelled
    }

private fun AsDoctorStatus.colorOf(colors: AsExtendedColors): AsStatusColor =
    when (this) {
        AsDoctorStatus.Pending -> colors.doctorPending
        AsDoctorStatus.Approved -> colors.doctorApproved
        AsDoctorStatus.Rejected -> colors.doctorRejected
        AsDoctorStatus.Suspended -> colors.doctorSuspended
    }

/** A request's status: icon + label together, never color alone, so the
 *  distinction still reads under TalkBack or color-blindness. */
@Composable
fun StatusChip(
    status: AsRequestStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.colorOf(AsTheme.extendedColors)
    StatusChip(label = status.label, icon = status.icon, color = color, modifier = modifier)
}

/** A doctor's verification status -- see [StatusChip] for [AsRequestStatus]. */
@Composable
fun StatusChip(
    status: AsDoctorStatus,
    modifier: Modifier = Modifier,
) {
    val color = status.colorOf(AsTheme.extendedColors)
    StatusChip(label = status.label, icon = status.icon, color = color, modifier = modifier)
}

@Composable
private fun StatusChip(
    label: String,
    icon: ImageVector,
    color: AsStatusColor,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = "Status: $label" },
        shape = PillShape,
        color = color.container,
        contentColor = color.onContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .heightIn(min = 32.dp)
                    .padding(horizontal = AsTheme.spacing.sm, vertical = AsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(AsTheme.spacing.xs))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview(name = "Request statuses - light")
@Preview(name = "Request statuses - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequestStatusChipsPreview() {
    AarogyaTheme {
        Surface {
            Row(
                modifier = Modifier.padding(AsTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                AsRequestStatus.entries.take(PREVIEW_REQUEST_STATUS_COUNT).forEach { StatusChip(status = it) }
            }
        }
    }
}

@Preview(name = "Doctor statuses - light")
@Preview(name = "Doctor statuses - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DoctorStatusChipsPreview() {
    AarogyaTheme {
        Surface {
            Row(
                modifier = Modifier.padding(AsTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
            ) {
                AsDoctorStatus.entries.forEach { StatusChip(status = it) }
            }
        }
    }
}
