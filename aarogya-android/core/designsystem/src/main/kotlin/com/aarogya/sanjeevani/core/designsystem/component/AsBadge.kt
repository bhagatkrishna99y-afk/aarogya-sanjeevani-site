package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.PillShape

/** A small count/label pill -- an unread-notifications count, a "New"
 *  flag next to a list item. For a domain status (a request's or
 *  doctor's state) use [StatusChip] instead, which always pairs its
 *  color with an icon; this one is plain-text and meant for short,
 *  self-explanatory labels. */
@Composable
fun AsBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(modifier = Modifier.sizeIn(minHeight = 20.dp).padding(PaddingValues(horizontal = 6.dp, vertical = 2.dp))) {
            Text(text = text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview(name = "Badge - light")
@Preview(name = "Badge - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsBadgePreview() {
    AarogyaTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                AsBadge(text = "3")
                AsBadge(
                    text = "New",
                    modifier = Modifier.padding(start = 8.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
    }
}
