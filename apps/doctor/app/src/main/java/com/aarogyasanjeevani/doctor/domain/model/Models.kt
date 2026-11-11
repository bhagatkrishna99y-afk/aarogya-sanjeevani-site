package com.aarogyasanjeevani.doctor.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

enum class DoctorStatus {
    PENDING, APPROVED, REJECTED, SUSPENDED;

    companion object {
        fun fromWire(value: String): DoctorStatus = when (value) {
            "pending" -> PENDING
            "approved" -> APPROVED
            "rejected" -> REJECTED
            "suspended" -> SUSPENDED
            else -> error("Unknown doctor status: $value")
        }
    }
}

enum class RequestStatus {
    REQUESTED, ACCEPTED, CONFIRMED, COMPLETED, REJECTED, CANCELLED;

    companion object {
        fun fromWire(value: String): RequestStatus = when (value) {
            "requested" -> REQUESTED
            "accepted" -> ACCEPTED
            "confirmed" -> CONFIRMED
            "completed" -> COMPLETED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            else -> error("Unknown request status: $value")
        }
    }

    fun label(): String = when (this) {
        REQUESTED -> "Requested"
        ACCEPTED -> "Accepted"
        CONFIRMED -> "Confirmed"
        COMPLETED -> "Completed"
        REJECTED -> "Rejected"
        CANCELLED -> "Cancelled"
    }
}

data class DoctorApplication(
    val specialization: String,
    val bio: String?,
    val licenseNumber: String,
    val yearsExperience: Int,
    val status: DoctorStatus,
    val rejectionReason: String?,
)

data class Service(
    val id: String,
    val name: String,
    val description: String?,
    val durationMinutes: Int,
)

data class DayAvailability(
    val dayOfWeek: Int, // 0 = Sunday .. 6 = Saturday, matching Postgres EXTRACT(DOW)
    val startTime: LocalTime,
    val endTime: LocalTime,
    val slotDurationMinutes: Int,
)

data class CareRequest(
    val id: String,
    val serviceName: String,
    val patientName: String,
    val patientPhone: String?,
    val scheduledStart: Instant,
    val scheduledEnd: Instant,
    val status: RequestStatus,
    val patientNotes: String?,
    val doctorResponseNote: String?,
)

data class RequestStatusEvent(
    val oldStatus: RequestStatus?,
    val newStatus: RequestStatus,
    val note: String?,
    val changedAt: Instant,
)

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedRequestId: String?,
    val isRead: Boolean,
    val createdAt: Instant,
)

val DAY_NAMES = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
