package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/**
 * The one list row every list screen (requests, doctors, notifications)
 * should use -- headline + optional supporting text, optional leading
 * (avatar/icon) and trailing (chevron/status) content. [onClick], when
 * passed, makes the whole row clickable with role [Role.Button] for
 * TalkBack and keeps the row at least 48dp tall regardless of content.
 */
@Composable
fun AsListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        modifier
            .sizeIn(minHeight = 48.dp)
            .let { base ->
                if (onClick != null) {
                    base.clickable(onClick = onClick).semantics { role = Role.Button }
                } else {
                    base
                }
            }

    ListItem(
        headlineContent = { Text(headline) },
        modifier = rowModifier,
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
    )
}

@Preview(name = "List item - light")
@Preview(name = "List item - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsListItemPreview() {
    AarogyaTheme {
        Surface {
            AsListItem(
                headline = "Dr. Asha Rao",
                supportingText = "General Medicine · 4.8 ★",
                leadingContent = { AsAvatar(name = "Asha Rao") },
                onClick = {},
            )
        }
    }
}
