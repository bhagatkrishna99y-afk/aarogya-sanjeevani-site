package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The horizontal date picker above a booking screen's slot grid --
 *  each day is its own 48dp+ selectable target, never a plain text
 *  label the user has to tap precisely. */
@Composable
fun DateStrip(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            DateStripDay(date = date, isSelected = date == selectedDate, onClick = { onDateSelected(date) })
        }
    }
}

@Composable
private fun DateStripDay(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val fullDescription =
        date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " +
            date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + date.dayOfMonth

    Surface(
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 64.dp)
                .clickable(onClick = onClick)
                .semantics {
                    role = Role.Tab
                    selected = isSelected
                    contentDescription = fullDescription
                },
        shape = RoundedCornerShape(16.dp),
        color =
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor =
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(vertical = AsTheme.spacing.sm, horizontal = AsTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = dayOfWeek, style = MaterialTheme.typography.labelSmall)
            Text(text = date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(name = "Date strip - light")
@Preview(name = "Date strip - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DateStripPreview() {
    AarogyaTheme {
        Surface {
            val today = LocalDate.now()
            DateStrip(
                dates = (0 until 7).map { today.plusDays(it.toLong()) },
                selectedDate = today,
                onDateSelected = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
