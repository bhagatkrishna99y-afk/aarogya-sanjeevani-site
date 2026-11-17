package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `select * from patient_requests_view order by scheduled_start desc`
 *  -- a patient's own requests, joined with service/doctor names
 *  (client-contract.md §8). */
@Serializable
data class PatientRequestViewDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String?,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("doctor_full_name") val doctorFullName: String?,
    @SerialName("doctor_specialization") val doctorSpecialization: String?,
    @SerialName("scheduled_start") val scheduledStart: Instant,
    @SerialName("scheduled_end") val scheduledEnd: Instant,
    val mode: ServiceMode,
    val status: RequestStatus,
    @SerialName("patient_notes") val patientNotes: String?,
    // legacy, expect null
    @SerialName("doctor_response_note") val doctorResponseNote: String?,
    @SerialName("contact_phone") val contactPhone: String?,
    @SerialName("address_text") val addressText: String?,
    @SerialName("rejection_reason") val rejectionReason: String?,
    @SerialName("cancellation_reason") val cancellationReason: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
