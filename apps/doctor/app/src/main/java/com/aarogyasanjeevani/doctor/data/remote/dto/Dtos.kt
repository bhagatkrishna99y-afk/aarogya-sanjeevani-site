package com.aarogyasanjeevani.doctor.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire-format DTOs. Field names mirror the Postgres column names exactly so
 * PostgREST's JSON (snake_case) decodes without custom property mappers.
 * Timestamps are kept as raw ISO-8601 strings here and parsed to
 * [kotlinx.datetime.Instant] in the repository/domain layer.
 */

@Serializable
data class ProfileDto(
    val id: String,
    val role: String,
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Partial update payload for `profiles` -- only full_name/phone are client-writable (see backend RLS). */
@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
)

/** Result of `get_my_doctor_application` -- deliberately excludes admin_notes (admin-only, see backend). */
@Serializable
data class MyDoctorApplicationDto(
    @SerialName("user_id") val userId: String,
    val specialization: String,
    val bio: String? = null,
    @SerialName("license_number") val licenseNumber: String,
    @SerialName("years_experience") val yearsExperience: Int,
    val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class ServiceDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val description: String? = null,
    @SerialName("requires_doctor") val requiresDoctor: Boolean,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class DoctorServiceDto(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("service_id") val serviceId: String,
)

@Serializable
data class WeeklyAvailabilityDto(
    val id: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    @SerialName("is_active") val isActive: Boolean,
)

@Serializable
data class RequestDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("service_id") val serviceId: String,
    @SerialName("scheduled_start") val scheduledStart: String,
    @SerialName("scheduled_end") val scheduledEnd: String,
    val status: String,
    @SerialName("patient_notes") val patientNotes: String? = null,
    @SerialName("doctor_response_note") val doctorResponseNote: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** [RequestDto] with the patient's name and the service name embedded via a PostgREST resource join. */
@Serializable
data class RequestWithDetailsDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String? = null,
    @SerialName("service_id") val serviceId: String,
    @SerialName("scheduled_start") val scheduledStart: String,
    @SerialName("scheduled_end") val scheduledEnd: String,
    val status: String,
    @SerialName("patient_notes") val patientNotes: String? = null,
    @SerialName("doctor_response_note") val doctorResponseNote: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val service: ServiceNameDto? = null,
    val patient: PatientInfoDto? = null,
)

@Serializable
data class ServiceNameDto(val name: String)

@Serializable
data class PatientInfoDto(
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
)

@Serializable
data class RequestStatusHistoryDto(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("old_status") val oldStatus: String? = null,
    @SerialName("new_status") val newStatus: String,
    val note: String? = null,
    @SerialName("changed_at") val changedAt: String,
)

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String,
    @SerialName("related_request_id") val relatedRequestId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
)
