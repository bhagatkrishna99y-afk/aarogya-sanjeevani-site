package com.aarogya.sanjeevani.patient.requests

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a", Locale.getDefault())
private val RELATIVE_FALLBACK_FORMAT = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault())
internal val CLINIC_ZONE_ID: ZoneId = ZoneId.of("Asia/Kolkata")

/** "Mon, Jan 5 · 9:00 AM", in the clinic's own timezone regardless of
 *  the device's -- same reasoning as the booking flow's own
 *  `formatSlotSummary`, just not shared code across packages. */
internal fun formatRequestDateTime(instant: Instant): String {
    val zoned = instant.toJavaInstant().atZone(CLINIC_ZONE_ID)
    return DATE_TIME_FORMAT.format(zoned)
}

/** "Jan 5, 9:00 AM" -- the timeline's absolute-time half, deliberately
 *  shorter than [formatRequestDateTime] since it always sits right next
 *  to a relative string ("2 hours ago") that already carries the day. */
internal fun formatAbsoluteTimestamp(instant: Instant): String =
    RELATIVE_FALLBACK_FORMAT.format(instant.toJavaInstant().atZone(CLINIC_ZONE_ID))
