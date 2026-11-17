package com.aarogya.sanjeevani.core.ui.connectivity

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme

/**
 * The one offline indicator either app's root scaffold should install,
 * directly below the top bar -- stateless by design, so it reacts to
 * whatever already resolves [com.aarogya.sanjeevani.core.common.connectivity.ConnectivityObserver]
 * into a plain [Boolean] (a ViewModel at the app root) rather than
 * reading connectivity itself. [androidx.compose.ui.semantics.liveRegion]
 * makes TalkBack announce it the moment it appears, without the user
 * having to find and focus it first.
 */
@Composable
fun OfflineBanner(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(color = AsTheme.extendedColors.warningContainer) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "You're offline. Some features may not be available."
                            liveRegion = LiveRegionMode.Polite
                        },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = AsTheme.extendedColors.onWarningContainer,
                )
                Text(
                    text = "You're offline",
                    modifier = Modifier.padding(start = 8.dp),
                    color = AsTheme.extendedColors.onWarningContainer,
                )
            }
        }
    }
}

@Preview(name = "Offline banner - light")
@Preview(name = "Offline banner - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfflineBannerPreview() {
    AarogyaTheme {
        OfflineBanner(visible = true)
    }
}
