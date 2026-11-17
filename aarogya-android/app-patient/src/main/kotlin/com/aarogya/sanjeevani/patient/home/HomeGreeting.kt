package com.aarogya.sanjeevani.patient.home

private const val MORNING_START_HOUR = 5
private const val AFTERNOON_START_HOUR = 12
private const val EVENING_START_HOUR = 17
private const val NIGHT_START_HOUR = 21

/** "Good morning"/afternoon/evening/night by [hour] (0-23, local time) --
 *  pure so it's testable without touching a real clock. */
fun greetingPrefixFor(hour: Int): String =
    when (hour) {
        in MORNING_START_HOUR until AFTERNOON_START_HOUR -> "Good morning"
        in AFTERNOON_START_HOUR until EVENING_START_HOUR -> "Good afternoon"
        in EVENING_START_HOUR until NIGHT_START_HOUR -> "Good evening"
        else -> "Good night"
    }

/** The first token of [fullName], or `null` for a blank/empty name --
 *  the Home greeting falls back to a name-less "Good morning" rather
 *  than showing an empty or whitespace-only name. */
fun firstNameOf(fullName: String): String? =
    fullName
        .trim()
        .split(Regex("\\s+"))
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
