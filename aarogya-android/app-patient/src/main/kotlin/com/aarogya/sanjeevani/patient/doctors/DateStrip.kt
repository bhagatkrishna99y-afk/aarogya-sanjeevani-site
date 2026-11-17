package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale

private const val STRIP_DAYS = 14

/**
 * The next [STRIP_DAYS] days, [today] first, plus a "Pick another
 * date" action for anything further out (up to [maxHorizonDays], the
 * backend's own real horizon -- never hardcoded here). Dates before
 * [today] are never rendered at all, which is this screen's own way
 * of "disabling" the past -- there's nothing to disable in a strip
 * that never scrolls backward past today.
 */
@Composable
fun DateStrip(
    today: LocalDate,
    selectedDate: LocalDate,
    maxHorizonDays: Int,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val stripDates = remember(today) { (0 until STRIP_DAYS).map { today.plus(it, DateTimeUnit.DAY) } }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = AsTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        items(stripDates, key = { it.toString() }) { date ->
            DateStripItem(date = date, selected = date == selectedDate, onClick = { onDateSelected(date) })
        }
        item {
            AsButton(
                text = "Pick another date",
                onClick = { showDatePicker = true },
                variant = AsButtonVariant.Text,
                modifier = Modifier.height(72.dp),
            )
        }
    }

    if (showDatePicker) {
        AvailabilityDatePickerDialog(
            today = today,
            initialDate = selectedDate,
            maxHorizonDays = maxHorizonDays,
            onDateSelected = { date ->
                showDatePicker = false
                onDateSelected(date)
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DateStripItem(
    date: LocalDate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val weekdayLabel = date.toJavaLocalDate().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val isSelected = selected

    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .width(56.dp)
                .height(72.dp)
                .semantics { this.selected = isSelected },
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor =
            if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(AsTheme.spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = weekdayLabel, style = MaterialTheme.typography.labelMedium)
            Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailabilityDatePickerDialog(
    today: LocalDate,
    initialDate: LocalDate,
    maxHorizonDays: Int,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val maxDate = remember(today, maxHorizonDays) { today.plus(maxHorizonDays, DateTimeUnit.DAY) }
    val selectableDates =
        remember(today, maxDate) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.UTC).date
                    return date in today..maxDate
                }
            }
        }
    val initialMillis =
        remember(initialDate) {
            initialDate.atStartOfDayUtcMillis()
        }
    val datePickerState =
        rememberDatePickerState(initialSelectedDateMillis = initialMillis, selectableDates = selectableDates)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(date)
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("Select") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun LocalDate.atStartOfDayUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
