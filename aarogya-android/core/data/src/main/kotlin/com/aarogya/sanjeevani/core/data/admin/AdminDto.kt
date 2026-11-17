package com.aarogya.sanjeevani.core.data.admin

import com.aarogya.sanjeevani.core.data.model.DoctorStatus
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.RequestStatusHistoryEntryDto
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `admin_list_doctors(p_status?, p_limit, p_offset)` result row
 *  (client-contract.md §13). */
@Serializable
data class AdminDoctorListItemDto(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val phone: String?,
    val specialization: String,
    @SerialName("license_number") val licenseNumber: String,
    val status: DoctorStatus,
    @SerialName("years_experience") val yearsExperience: Int,
    @SerialName("is_suspended") val isSuspended: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("reviewed_at") val reviewedAt: Instant?,
)

/** `admin_list_requests(...)` result row (client-contract.md §13). */
@Serializable
data class AdminRequestListItemDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_full_name") val patientFullName: String,
    @SerialName("doctor_id") val doctorId: String?,
    @SerialName("doctor_full_name") val doctorFullName: String?,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("scheduled_start") val scheduledStart: Instant,
    @SerialName("scheduled_end") val scheduledEnd: Instant,
    val mode: ServiceMode,
    val status: RequestStatus,
    @SerialName("created_at") val createdAt: Instant,
)

/** `admin_get_request_detail(p_request_id)` result -- one row, every
 *  field in [AdminRequestListItemDto] plus the rest, with the full
 *  status history pre-joined server-side (client-contract.md §13). */
@Serializable
data class AdminRequestDetailDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_full_name") val patientFullName: String,
    @SerialName("patient_phone") val patientPhone: String?,
    @SerialName("doctor_id") val doctorId: String?,
    @SerialName("doctor_full_name") val doctorFullName: String?,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
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
    @SerialName("status_log") val statusLog: List<RequestStatusHistoryEntryDto>,
)
