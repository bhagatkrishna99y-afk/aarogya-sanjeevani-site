package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/** The title above a grouped section of content (a list, a form, a
 *  summary block) -- marked as a heading in the semantics tree so
 *  TalkBack users can jump between sections the way sighted users scan
 *  by eye. [action], when passed (e.g. "See all"), sits trailing. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = AsTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        action?.invoke()
    }
}

@Preview(name = "Section header - light")
@Preview(name = "Section header - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SectionHeaderPreview() {
    AarogyaTheme {
        Surface {
            SectionHeader(
                title = "Upcoming appointments",
                modifier = Modifier.padding(16.dp),
                action = { Text("See all", style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
