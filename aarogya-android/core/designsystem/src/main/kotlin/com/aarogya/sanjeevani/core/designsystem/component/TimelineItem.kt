package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * One step in a request's status timeline (Requested -> Accepted ->
 * Confirmed -> Completed, or a Rejected/Cancelled branch). [isCompleted]
 * drives the dot/line color (primary once reached, outline while still
 * pending); [isLast] hides the connecting line below the dot, since the
 * final step in the list has nothing to connect to. [actor] is who made
 * this change (e.g. "You", "Doctor", "System") -- optional since not
 * every caller has one to show.
 */
@Composable
fun TimelineItem(
    title: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null,
    actor: String? = null,
    isCompleted: Boolean = false,
    isLast: Boolean = false,
) {
    val dotColor = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val description =
        listOfNotNull(title, timestamp, actor?.let { "by $it" }).joinToString(", ")

    Row(modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(16.dp), shape = CircleShape, color = dotColor) {}
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(Modifier.width(AsTheme.spacing.sm))
        Column(modifier = Modifier.padding(bottom = AsTheme.spacing.lg)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (timestamp != null) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actor != null) {
                Text(
                    text = actor,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Timeline - light")
@Preview(name = "Timeline - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TimelineItemPreview() {
    AarogyaTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                TimelineItem(title = "Requested", timestamp = "12 Nov, 9:00 AM", isCompleted = true)
                TimelineItem(title = "Accepted", timestamp = "12 Nov, 9:15 AM", isCompleted = true)
                TimelineItem(title = "Confirmed", isCompleted = false, isLast = true)
            }
        }
    }
}
