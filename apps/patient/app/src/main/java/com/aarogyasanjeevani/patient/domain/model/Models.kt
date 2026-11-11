package com.aarogyasanjeevani.patient.domain.model

import kotlinx.datetime.Instant

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

    /** Patients may cancel only while the request hasn't reached a terminal outcome. */
    val isCancellable: Boolean
        get() = this == REQUESTED || this == ACCEPTED || this == CONFIRMED
}

data class ServiceCategory(
    val id: String,
    val name: String,
    val description: String?,
    val services: List<Service>,
)

data class Service(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String?,
    val requiresDoctor: Boolean,
    val durationMinutes: Int,
)

data class Doctor(
    val id: String,
    val fullName: String,
    val specialization: String,
    val bio: String?,
    val yearsExperience: Int,
)

data class AvailableSlot(
    val start: Instant,
    val end: Instant,
)

data class BookingRequest(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val doctorId: String?,
    val doctorName: String?,
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
