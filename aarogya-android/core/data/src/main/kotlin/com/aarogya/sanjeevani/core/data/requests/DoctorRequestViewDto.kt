package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `select * from doctor_requests_view order by scheduled_start desc`
 *  -- a doctor's own requests, joined with patient/service names
 *  (client-contract.md §8). [contactPhone] is masked to `null` by the
 *  view itself unless [status] is `accepted`/`confirmed` -- a doctor
 *  shouldn't see a patient's phone number before accepting them. */
@Serializable
data class DoctorRequestViewDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_full_name") val patientFullName: String,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("scheduled_start") val scheduledStart: Instant,
    @SerialName("scheduled_end") val scheduledEnd: Instant,
    val mode: ServiceMode,
    val status: RequestStatus,
    @SerialName("patient_notes") val patientNotes: String?,
    // legacy, expect null
    @SerialName("doctor_response_note") val doctorResponseNote: String?,
    @SerialName("contact_phone") val contactPhone: String?,
    @SerialName("address_text") val addressText: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
