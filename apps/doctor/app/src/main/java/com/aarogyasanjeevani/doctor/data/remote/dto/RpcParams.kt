package com.aarogyasanjeevani.doctor.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Parameter payloads for Postgres RPCs. Keys must match the SQL function's argument names exactly. */

@Serializable
data class SubmitDoctorApplicationParams(
    @SerialName("p_full_name") val fullName: String,
    @SerialName("p_phone") val phone: String?,
    @SerialName("p_specialization") val specialization: String,
    @SerialName("p_license_number") val licenseNumber: String,
    @SerialName("p_bio") val bio: String?,
    @SerialName("p_years_experience") val yearsExperience: Int,
)

@Serializable
data class SetDoctorServicesParams(@SerialName("p_service_ids") val serviceIds: List<String>)

@Serializable
data class SetWeeklyAvailabilityParams(@SerialName("p_schedule") val schedule: JsonElement)

@Serializable
data class DoctorRespondRequestParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_accept") val accept: Boolean,
    @SerialName("p_reason") val reason: String? = null,
)

@Serializable
data class RequestIdParams(@SerialName("p_request_id") val requestId: String)

@Serializable
data class CancelRequestParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_reason") val reason: String? = null,
)

@Serializable
data class MarkNotificationReadParams(@SerialName("p_notification_id") val notificationId: String)
