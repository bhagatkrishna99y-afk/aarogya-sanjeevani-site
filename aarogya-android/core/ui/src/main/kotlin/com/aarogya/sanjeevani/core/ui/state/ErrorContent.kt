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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/** The one full-screen error state every list/detail screen uses when
 *  its load fails -- always offers a retry, never just a dead-end
 *  message, since [AppError] is exactly the set of things worth
 *  retrying or clearly explaining (see that type for why). */
@Composable
fun ErrorContent(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = error.toUserMessage(LocalContext.current),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        AsButton(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Same idea as [ErrorContent], sized for one section of a longer
 *  screen (Home's requests/doctors/categories, e.g.) rather than the
 *  whole screen -- [ErrorContent]'s own `fillMaxSize()` would otherwise
 *  make that one section swallow every other one below it. */
@Composable
fun CompactErrorContent(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
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
            text = error.toUserMessage(LocalContext.current),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        AsButton(text = "Try again", onClick = onRetry, variant = AsButtonVariant.Text)
    }
}
