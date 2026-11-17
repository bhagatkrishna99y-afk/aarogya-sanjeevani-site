package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** The one snackbar host either app's root scaffold should install --
 *  explicit brand colors (inverse surface) rather than Material's
 *  un-themed default, so a transient message still reads as part of
 *  this app rather than generic platform chrome. */
@Composable
fun AsSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
        )
    }
}

@Preview(name = "Snackbar - light")
@Preview(name = "Snackbar - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsSnackbarHostPreview() {
    AarogyaTheme {
        Surface {
            val hostState = remember { SnackbarHostState() }
            LaunchedEffect(Unit) { hostState.showSnackbar("Request submitted") }
            AsSnackbarHost(hostState = hostState)
        }
    }
}
