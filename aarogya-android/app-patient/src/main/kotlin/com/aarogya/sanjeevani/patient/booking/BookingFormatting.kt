package com.aarogya.sanjeevani.patient.booking

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SUMMARY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val SUMMARY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val CLINIC_ZONE_ID = ZoneId.of("Asia/Kolkata")
private const val MINUTES_PER_HOUR = 60

/** Shared between [RequestFormScreen] and [ReviewScreen] -- both render
 *  the exact same "service · date · time" summary line. */
internal fun formatSlotSummary(
    slotStart: Instant,
    slotDurationMinutes: Int?,
): String {
    val start = slotStart.toJavaInstant().atZone(CLINIC_ZONE_ID)
    val datePart = SUMMARY_DATE_FORMAT.format(start)
    val startTime = SUMMARY_TIME_FORMAT.format(start)
    return if (slotDurationMinutes != null) {
        val end = start.plus(slotDurationMinutes.toLong(), ChronoUnit.MINUTES)
        "$datePart · $startTime - ${SUMMARY_TIME_FORMAT.format(end)}"
    } else {
        "$datePart · $startTime"
    }
}

/** Renders [com.aarogya.sanjeevani.core.data.requests.BookingSettings.cancellationNoticeMinutes]
 *  as a short policy line -- never a hardcoded policy string, since the
 *  backend (`booking_settings()`) is the authoritative source, same
 *  reasoning as `AvailabilityViewModel`'s own `maxHorizonDays`. */
internal fun formatCancellationPolicy(cancellationNoticeMinutes: Int): String =
    if (cancellationNoticeMinutes <= 0) {
        "You can cancel this request free of charge at any time before it's completed."
    } else {
        val hours = cancellationNoticeMinutes / MINUTES_PER_HOUR
        val minutes = cancellationNoticeMinutes % MINUTES_PER_HOUR
        val notice =
            when {
                hours > 0 && minutes > 0 -> "$hours h $minutes min"
                hours > 0 -> "$hours h"
                else -> "$minutes min"
            }
        "Free cancellation up to $notice before your scheduled time."
    }
