package com.aarogya.sanjeevani.patient.ui.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private const val DAYS_PER_WEEK = 7

/** "Just now", "5 min ago", "3 hours ago", "2 days ago" -- used wherever
 *  a timestamp needs a short, human-relative label (the notifications
 *  list, a request's status timeline) rather than a raw date. Falls
 *  back to weeks for anything a week or older rather than growing
 *  months/years deep -- every caller of this also shows the absolute
 *  timestamp alongside it, so there's no real need to be more precise
 *  than "a while ago" past that point. */
fun formatRelativeTime(
    instant: Instant,
    now: Instant = Clock.System.now(),
): String {
    val elapsed = (now - instant).coerceAtLeast(0.seconds)
    val totalSeconds = elapsed.inWholeSeconds
    val totalMinutes = totalSeconds / SECONDS_PER_MINUTE
    val totalHours = totalMinutes / MINUTES_PER_HOUR
    val totalDays = totalHours / HOURS_PER_DAY
    val totalWeeks = totalDays / DAYS_PER_WEEK

    return when {
        totalMinutes < 1 -> "Just now"
        totalMinutes < MINUTES_PER_HOUR -> "$totalMinutes min ago"
        totalHours < HOURS_PER_DAY -> if (totalHours == 1L) "1 hour ago" else "$totalHours hours ago"
        totalDays < DAYS_PER_WEEK -> if (totalDays == 1L) "1 day ago" else "$totalDays days ago"
        else -> if (totalWeeks == 1L) "1 week ago" else "$totalWeeks weeks ago"
    }
}
