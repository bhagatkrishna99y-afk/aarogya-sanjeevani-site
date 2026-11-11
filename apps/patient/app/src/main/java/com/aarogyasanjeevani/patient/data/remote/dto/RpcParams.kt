package com.aarogyasanjeevani.patient.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Parameter payloads for Postgres RPCs. Keys must match the SQL function's argument names exactly. */

@Serializable
data class ListApprovedDoctorsParams(@SerialName("p_service_id") val serviceId: String? = null)

@Serializable
data class GetAvailableSlotsParams(
    @SerialName("p_doctor_id") val doctorId: String,
    @SerialName("p_service_id") val serviceId: String,
    @SerialName("p_date") val date: String,
)

@Serializable
data class CreateRequestParams(
    @SerialName("p_service_id") val serviceId: String,
    @SerialName("p_doctor_id") val doctorId: String? = null,
    @SerialName("p_scheduled_start") val scheduledStart: String,
    @SerialName("p_patient_notes") val patientNotes: String? = null,
)

@Serializable
data class CancelRequestParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_reason") val reason: String? = null,
)

@Serializable
data class MarkNotificationReadParams(@SerialName("p_notification_id") val notificationId: String)
