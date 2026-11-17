package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** The one confirmation/alert dialog either app should use -- a
 *  destructive or hard-to-undo action (cancelling a request, rejecting
 *  a doctor) always goes through this rather than acting immediately on
 *  a single tap. [onDismiss] fires for both the system back gesture and
 *  tapping outside, same as [AlertDialog]'s own `onDismissRequest`. */
@Composable
fun AsDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = "Cancel",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton =
            dismissLabel?.let {
                { TextButton(onClick = onDismiss) { Text(it) } }
            },
    )
}

@Preview(name = "Dialog - light")
@Preview(name = "Dialog - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsDialogPreview() {
    AarogyaTheme {
        Surface {
            AsDialog(
                title = "Cancel this request?",
                text = "The doctor will be notified. This can't be undone.",
                confirmLabel = "Cancel request",
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
