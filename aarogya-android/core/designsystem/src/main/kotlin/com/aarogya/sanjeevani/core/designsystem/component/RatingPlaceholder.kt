package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

private const val STAR_COUNT = 5

/**
 * **Not used yet** -- a visual stand-in for a future doctor-rating
 * display, included now so the design language (dimmed stars, "Ratings
 * coming soon") exists ahead of the feature actually shipping. Marked
 * [invisibleToUser] so it doesn't show up as a dead-end stop for
 * TalkBack users today; remove that once real ratings land and this
 * becomes an interactive/informative component.
 */
@Composable
fun RatingPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics { invisibleToUser() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STAR_COUNT) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        Text(
            text = "Ratings coming soon",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = AsTheme.spacing.xs),
        )
    }
}

@Preview(name = "Rating placeholder - light")
@Preview(name = "Rating placeholder - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RatingPlaceholderPreview() {
    AarogyaTheme {
        Surface { RatingPlaceholder(modifier = Modifier.padding(16.dp)) }
    }
}
