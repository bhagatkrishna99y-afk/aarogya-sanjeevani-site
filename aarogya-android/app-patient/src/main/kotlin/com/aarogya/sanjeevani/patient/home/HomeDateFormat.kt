package com.aarogya.sanjeevani.patient.home

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val REQUEST_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d '·' h:mm a")

/** "Mon, Jan 5 · 10:00 AM", in the device's local time zone -- `java.time`
 *  directly, no desugaring needed, since this app's `minSdk` (26)
 *  already meets its native requirement. */
fun formatRequestDateTime(instant: Instant): String =
    REQUEST_DATE_TIME_FORMAT.format(instant.toJavaInstant().atZone(ZoneId.systemDefault()))
