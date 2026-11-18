package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.PillShape

/** A selectable filter chip -- a service category, a specialty, a day
 *  of the week. [selected] drives both its look and its a11y state
 *  (Material3's `FilterChip` exposes selection to TalkBack on its own,
 *  nothing extra needed here); [leadingIcon] is optional decoration,
 *  never the only way a choice is conveyed. */
@Composable
fun AsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        shape = PillShape,
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.primary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            ),
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
    )
}

@Preview(name = "Chip - light")
@Preview(name = "Chip - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsChipPreview() {
    AarogyaTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                AsChip(label = "Cardiology", selected = true, onClick = {})
                AsChip(
                    label = "Dermatology",
                    selected = false,
                    onClick = {},
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
