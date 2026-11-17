// AsButtonVariant lives here, next to the AsButton this file is really about.
@file:Suppress("MatchingDeclarationName")

package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** [AsButton]'s visual treatment -- maps 1:1 to the three Material3 button
 *  styles so "primary/secondary/text" (the brief's own vocabulary) never
 *  has to be re-explained to a caller already familiar with Material. */
enum class AsButtonVariant {
    Primary,
    Secondary,
    Text,
}

/**
 * The one button every primary/secondary/text action in either app
 * should use. Loading state keeps the label on screen and adds a
 * spinner alongside it -- never replaces it, so a screen reader doesn't
 * lose the action's name mid-load (same convention the original
 * scaffold's `AarogyaButton` already established).
 *
 * Always at least 48dp tall regardless of [variant] or content, Material's
 * own default (40dp for `Button`) being short of this design system's
 * touch target floor.
 */
@Composable
fun AsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AsButtonVariant = AsButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val isEnabled = enabled && !loading
    val buttonModifier =
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics {
                if (loading) stateDescription = "Loading"
                if (!enabled) disabled()
            }

    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }

    when (variant) {
        AsButtonVariant.Primary ->
            Button(onClick = onClick, modifier = buttonModifier, enabled = isEnabled, content = { content() })
        AsButtonVariant.Secondary ->
            OutlinedButton(onClick = onClick, modifier = buttonModifier, enabled = isEnabled, content = { content() })
        AsButtonVariant.Text ->
            TextButton(onClick = onClick, modifier = buttonModifier, enabled = isEnabled, content = { content() })
    }
}

@Preview(name = "Buttons - light")
@Preview(name = "Buttons - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsButtonPreview() {
    AarogyaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsButton(text = "Book appointment", onClick = {})
                AsButton(text = "View details", onClick = {}, variant = AsButtonVariant.Secondary)
                AsButton(text = "Cancel", onClick = {}, variant = AsButtonVariant.Text)
                AsButton(text = "Booking...", onClick = {}, loading = true)
                AsButton(text = "Unavailable", onClick = {}, enabled = false)
            }
        }
    }
}
