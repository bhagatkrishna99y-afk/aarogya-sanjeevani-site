package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/** A label/value pair -- "Service: General consultation", "Date: 12
 *  Nov, 10:00 AM" -- used throughout request/doctor detail screens.
 *  Merged into one semantics node so TalkBack reads "Service, General
 *  consultation" as a single unit rather than two separate stops. */
@Composable
fun KeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = AsTheme.spacing.xs)
                .semantics(mergeDescendants = true) { contentDescription = "$key, $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

@Preview(name = "Key/value row - light")
@Preview(name = "Key/value row - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun KeyValueRowPreview() {
    AarogyaTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                KeyValueRow(key = "Service", value = "General consultation")
                KeyValueRow(key = "Date", value = "12 Nov, 10:00 AM")
            }
        }
    }
}
