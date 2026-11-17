package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.designsystem.component.AsChip
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SLOT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** "9:00 AM" -- a fixed 12-hour pattern rather than
 *  [java.time.format.FormatStyle.SHORT] so this screen's times read
 *  the same regardless of the device's own 12h/24h setting (still
 *  [Locale]-aware for AM/PM text and digit shaping). */
fun OpenSlot.timeLabel(zone: TimeZone): String {
    val zoneId = ZoneId.of(zone.id)
    return SLOT_TIME_FORMAT.format(slotStart.toJavaInstant().atZone(zoneId))
}

/**
 * One bookable time -- [selected] shows a check icon (via [AsChip]'s
 * own selected-state rendering), never color alone, so the choice
 * still reads under color-blindness or a grayscale screenshot.
 *
 * Deliberately named differently from
 * [com.aarogya.sanjeevani.core.designsystem.component.SlotChip] (that
 * one's own `available` struck-through/"unavailable" handling has
 * nothing to key off here -- [OpenSlot] is the backend's own list of
 * *already-bookable* slots, never a slot known to be taken) -- a name
 * collision between an unrelated design-system component and this
 * screen's own helper would only invite someone to assume this one
 * has that same capability.
 */
@Composable
fun TimeSlotChip(
    slot: OpenSlot,
    selected: Boolean,
    zone: TimeZone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsChip(label = slot.timeLabel(zone), selected = selected, onClick = onClick, modifier = modifier)
}
