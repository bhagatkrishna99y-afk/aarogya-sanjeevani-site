package com.aarogya.sanjeevani.core.ui.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/** The one full-screen empty state every list screen uses when a load
 *  succeeds with zero items -- distinct from [ErrorContent] since
 *  there's nothing to retry here, just nothing to show yet. [action],
 *  when passed, is this screen's own useful next step (a "no search
 *  results" screen clearing its filters, e.g.) -- omit it when there
 *  genuinely isn't one. */
@Composable
fun EmptyContent(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

/** Same idea as [EmptyContent], sized for one section of a longer
 *  screen rather than the whole screen (see [CompactErrorContent]'s
 *  doc comment for why) -- [action], when passed, is this section's
 *  own call to action (Home's "book your first service" button for an
 *  empty requests section, e.g.), not a generic retry. */
@Composable
fun CompactEmptyContent(
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(AsTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}
