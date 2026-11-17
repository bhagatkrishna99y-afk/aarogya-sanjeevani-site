package com.aarogya.sanjeevani.core.data.requests

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `get_doctor_open_slots(p_doctor_id, p_date)` result row
 *  (client-contract.md §6) -- already filtered to bookable slots; an
 *  empty list means "nothing open that day," not an error. */
@Serializable
data class OpenSlotDto(
    @SerialName("slot_start") val slotStart: Instant,
    @SerialName("slot_end") val slotEnd: Instant,
)

/** `booking_settings()` result -- always exactly one row. */
@Serializable
data class BookingSettingsDto(
    @SerialName("max_horizon_days") val maxHorizonDays: Int,
    @SerialName("min_lead_minutes") val minLeadMinutes: Int,
    @SerialName("cancellation_notice_minutes") val cancellationNoticeMinutes: Int,
)
