package com.aarogya.sanjeevani.core.data.doctors

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `doctor_weekly_availability` table (client-contract.md
 *  §12's `set_doctor_availability`). */
@Serializable
data class DoctorWeeklyAvailabilityDto(
    val id: String,
    @SerialName("doctor_id") val doctorId: String,
    // 0=Sunday .. 6=Saturday
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: LocalTime,
    @SerialName("end_time") val endTime: LocalTime,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

/** Mirrors the `doctor_services` join table -- which services a doctor
 *  has chosen to offer, set wholesale via `set_doctor_services`
 *  (client-contract.md §12). */
@Serializable
data class DoctorServiceLinkDto(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("service_id") val serviceId: String,
    @SerialName("created_at") val createdAt: Instant,
)
