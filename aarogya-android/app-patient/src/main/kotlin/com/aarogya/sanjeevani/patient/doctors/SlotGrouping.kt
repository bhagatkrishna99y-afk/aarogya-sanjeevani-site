package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val AFTERNOON_START_HOUR = 12
private const val EVENING_START_HOUR = 17

/** The clinic's own day, per `get_doctor_open_slots`' doc comment --
 *  this feature's dates/times are always Asia/Kolkata, regardless of
 *  the device's own timezone. Shared by the ViewModel (date math) and
 *  the screen (slot time labels/grouping) so both agree on exactly the
 *  same zone. */
val CLINIC_TIME_ZONE: TimeZone = TimeZone.of("Asia/Kolkata")

/** The three buckets this screen's brief names explicitly -- no
 *  "night" bucket, since a bookable slot that late is both unlikely
 *  and not part of the asked-for grouping. */
enum class SlotPeriod(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
}

private fun periodFor(hour: Int): SlotPeriod =
    when {
        hour < AFTERNOON_START_HOUR -> SlotPeriod.MORNING
        hour < EVENING_START_HOUR -> SlotPeriod.AFTERNOON
        else -> SlotPeriod.EVENING
    }

/** Groups [this] by [SlotPeriod] in [zone] (the clinic's own day, not
 *  the device's) -- always in Morning/Afternoon/Evening order, and
 *  only the periods that actually have a slot in them, so the screen
 *  never renders an empty "Evening" header above nothing. */
fun List<OpenSlot>.groupByPeriod(zone: TimeZone): List<Pair<SlotPeriod, List<OpenSlot>>> {
    val byPeriod = groupBy { periodFor(it.slotStart.toLocalDateTime(zone).hour) }
    return SlotPeriod.entries.mapNotNull { period -> byPeriod[period]?.let { period to it } }
}
