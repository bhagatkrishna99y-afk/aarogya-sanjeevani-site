package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `submit_doctor_profile(...)` params (client-contract.md §12). */
@Serializable
data class SubmitDoctorProfileParams(
    @SerialName("p_title") val title: String?,
    @SerialName("p_specialization") val specialization: String,
    @SerialName("p_qualifications") val qualifications: String?,
    @SerialName("p_bio") val bio: String?,
    @SerialName("p_years") val years: Int,
    @SerialName("p_license_number") val licenseNumber: String,
    @SerialName("p_modes") val modes: List<ServiceMode>,
    @SerialName("p_slot_duration") val slotDuration: Int,
)

/** One entry of `set_doctor_availability`'s `p_windows` jsonb array
 *  (client-contract.md §12) -- encode a `List` of these with
 *  `Json.encodeToJsonElement` and pass the result as `p_windows`
 *  directly, not wrapped in another object. */
@Serializable
data class SetDoctorAvailabilityWindowDto(
    @SerialName("day_of_week") val dayOfWeek: Int,
    // "HH:MM", sent as plain text inside the jsonb array
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
)
