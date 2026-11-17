package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** The one top bar every screen should use. [onBack] (back arrow) and
 *  [actions] (trailing icon buttons -- notifications, overflow) are both
 *  optional so a top-level destination can omit the back arrow without
 *  a separate component. [title] is marked as a heading in the semantics
 *  tree -- same reasoning as [SectionHeader] -- so TalkBack users can
 *  jump straight to a screen's own title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, modifier = Modifier.semantics { heading() }) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = { actions() },
    )
}

@Preview(name = "Top bar - light")
@Preview(name = "Top bar - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsTopBarPreview() {
    AarogyaTheme {
        Surface { AsTopBar(title = "Book appointment", onBack = {}) }
    }
}
