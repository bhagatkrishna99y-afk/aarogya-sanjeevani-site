package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/** One selectable time slot in a booking screen's slot grid. An
 *  [available] = false slot is rendered disabled with a struck-through
 *  label and its own "(unavailable)" announcement -- never just a
 *  dimmed color, since that distinction has to survive both low vision
 *  and a plain look at the screen. */
@Composable
fun SlotChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    available: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = available,
        label = {
            Text(
                text = label,
                textDecoration = if (available) TextDecoration.None else TextDecoration.LineThrough,
            )
        },
        modifier =
            modifier.semantics {
                contentDescription = if (available) label else "$label, unavailable"
            },
        colors = FilterChipDefaults.filterChipColors(),
    )
}

@Preview(name = "Slot chip - light")
@Preview(name = "Slot chip - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SlotChipPreview() {
    AarogyaTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                SlotChip(label = "10:00 AM", selected = true, onClick = {})
                SlotChip(
                    label = "10:30 AM",
                    selected = false,
                    onClick = {},
                    modifier = Modifier.padding(start = 8.dp),
                )
                SlotChip(
                    label = "11:00 AM",
                    selected = false,
                    onClick = {},
                    available = false,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
