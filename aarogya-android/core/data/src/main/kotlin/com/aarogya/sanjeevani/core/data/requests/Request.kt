package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant

/** A patient's own view of one of their requests (`patient_requests_view`). */
data class PatientRequest(
    val id: String,
    val patientId: String,
    val doctorId: String?,
    val serviceId: String,
    val serviceName: String,
    val doctorFullName: String?,
    val doctorSpecialization: String?,
    val scheduledStart: Instant,
    val scheduledEnd: Instant,
    val mode: ServiceMode,
    val status: RequestStatus,
    val patientNotes: String?,
    val contactPhone: String?,
    val addressText: String?,
    val rejectionReason: String?,
    val cancellationReason: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A doctor's own view of one of their requests (`doctor_requests_view`).
 *  [contactPhone] is `null` until the view itself reveals it (status
 *  `accepted`/`confirmed`) -- not something this layer re-derives. */
data class DoctorRequest(
    val id: String,
    val patientId: String,
    val patientFullName: String,
    val serviceId: String,
    val serviceName: String,
    val doctorId: String,
    val scheduledStart: Instant,
    val scheduledEnd: Instant,
    val mode: ServiceMode,
    val status: RequestStatus,
    val patientNotes: String?,
    val contactPhone: String?,
    val addressText: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class OpenSlot(
    val slotStart: Instant,
    val slotEnd: Instant,
)

/** `booking_settings()` -- the only authoritative source for the
 *  booking horizon/lead time; the client never hardcodes or re-derives
 *  these (client-contract.md's own framing: the backend may raise
 *  [maxHorizonDays] at any time without an app update). */
data class BookingSettings(
    val maxHorizonDays: Int,
    val minLeadMinutes: Int,
    val cancellationNoticeMinutes: Int,
)

data class RequestStatusHistoryEntry(
    val id: String,
    val requestId: String,
    val oldStatus: RequestStatus?,
    val newStatus: RequestStatus,
    val changedBy: String?,
    val note: String?,
    val changedAt: Instant,
)

/** What the caller actually chooses on the booking screen --
 *  [NewServiceRequest.toParams] is where this becomes the RPC's
 *  `p_`-prefixed wire shape ([CreateServiceRequestParams]). */
data class NewServiceRequest(
    val serviceId: String,
    val doctorId: String?,
    val slotStart: Instant,
    val mode: ServiceMode,
    val notes: String?,
    val contactPhone: String?,
    val address: String?,
)

fun PatientRequestViewDto.toDomain(): PatientRequest =
    PatientRequest(
        id = id,
        patientId = patientId,
        doctorId = doctorId,
        serviceId = serviceId,
        serviceName = serviceName,
        doctorFullName = doctorFullName,
        doctorSpecialization = doctorSpecialization,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        mode = mode,
        status = status,
        patientNotes = patientNotes,
        contactPhone = contactPhone,
        addressText = addressText,
        rejectionReason = rejectionReason,
        cancellationReason = cancellationReason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun DoctorRequestViewDto.toDomain(): DoctorRequest =
    DoctorRequest(
        id = id,
        patientId = patientId,
        patientFullName = patientFullName,
        serviceId = serviceId,
        serviceName = serviceName,
        doctorId = doctorId,
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledEnd,
        mode = mode,
        status = status,
        patientNotes = patientNotes,
        contactPhone = contactPhone,
        addressText = addressText,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun OpenSlotDto.toDomain(): OpenSlot = OpenSlot(slotStart = slotStart, slotEnd = slotEnd)

fun BookingSettingsDto.toDomain(): BookingSettings =
    BookingSettings(
        maxHorizonDays = maxHorizonDays,
        minLeadMinutes = minLeadMinutes,
        cancellationNoticeMinutes = cancellationNoticeMinutes,
    )

fun RequestStatusHistoryEntryDto.toDomain(): RequestStatusHistoryEntry =
    RequestStatusHistoryEntry(
        id = id,
        requestId = requestId,
        oldStatus = oldStatus,
        newStatus = newStatus,
        changedBy = changedBy,
        note = note,
        changedAt = changedAt,
    )

fun NewServiceRequest.toParams(): CreateServiceRequestParams =
    CreateServiceRequestParams(
        serviceId = serviceId,
        doctorId = doctorId,
        slotStart = slotStart,
        mode = mode,
        notes = notes,
        contactPhone = contactPhone,
        address = address,
    )
