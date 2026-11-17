package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** The one hairline divider every screen should use between list
 *  sections/rows -- `outlineVariant`, never a raw gray, so it tracks
 *  light/dark and any future palette change automatically. */
@Composable
fun AsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}

@Preview(name = "Divider - light")
@Preview(name = "Divider - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsDividerPreview() {
    AarogyaTheme {
        Surface { AsDivider() }
    }
}
