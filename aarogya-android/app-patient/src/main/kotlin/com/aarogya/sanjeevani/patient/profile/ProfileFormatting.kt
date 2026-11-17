package com.aarogya.sanjeevani.patient.profile

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MEMBER_SINCE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val CLINIC_ZONE_ID = ZoneId.of("Asia/Kolkata")

/** "Jan 5, 2026" -- the clinic's own timezone regardless of the
 *  device's, same reasoning as every other date shown in this app. */
internal fun formatMemberSince(createdAt: Instant): String {
    val zoned = createdAt.toJavaInstant().atZone(CLINIC_ZONE_ID)
    return MEMBER_SINCE_FORMAT.format(zoned)
}
