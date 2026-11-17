package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `requests` table. Prefer [PatientRequestViewDto]/
 *  [DoctorRequestViewDto] for list/detail screens (client-contract.md
 *  §8-9) -- this bare-table shape has no joined doctor/patient name. */
@Serializable
data class RequestDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String?,
    @SerialName("service_id") val serviceId: String,
    @SerialName("scheduled_start") val scheduledStart: Instant,
    @SerialName("scheduled_end") val scheduledEnd: Instant,
    @SerialName("requested_date") val requestedDate: LocalDate,
    val mode: ServiceMode,
    val status: RequestStatus,
    @SerialName("patient_notes") val patientNotes: String?,
    // legacy, expect null
    @SerialName("doctor_response_note") val doctorResponseNote: String?,
    @SerialName("contact_phone") val contactPhone: String?,
    @SerialName("address_text") val addressText: String?,
    @SerialName("rejection_reason") val rejectionReason: String?,
    @SerialName("cancellation_reason") val cancellationReason: String?,
    @SerialName("accepted_at") val acceptedAt: Instant?,
    @SerialName("confirmed_at") val confirmedAt: Instant?,
    @SerialName("completed_at") val completedAt: Instant?,
    @SerialName("cancelled_at") val cancelledAt: Instant?,
    @SerialName("rejected_at") val rejectedAt: Instant?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

/** `select * from request_status_history where request_id = ? order by
 *  changed_at` -- the status timeline (client-contract.md §9).
 *  [oldStatus] is `null` only for the very first ("created") entry. */
@Serializable
data class RequestStatusHistoryEntryDto(
    val id: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("old_status") val oldStatus: RequestStatus?,
    @SerialName("new_status") val newStatus: RequestStatus,
    @SerialName("changed_by") val changedBy: String?,
    val note: String?,
    @SerialName("changed_at") val changedAt: Instant,
)
