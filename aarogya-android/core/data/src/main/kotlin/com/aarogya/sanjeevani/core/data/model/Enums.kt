package com.aarogya.sanjeevani.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every Postgres enum label this app's current API surface can return,
 * transcribed verbatim from backend/docs/kotlin-models.md (itself hand
 * cross-checked against the live schema's `types/database.ts`) --
 * shared across packages since the same enum shows up in several
 * tables/views (e.g. [ServiceMode] in `services`, `doctor_profiles` and
 * `requests` alike).
 */
@Serializable
enum class UserRole {
    @SerialName("user")
    USER,

    @SerialName("doctor")
    DOCTOR,

    @SerialName("admin")
    ADMIN,
}

@Serializable
enum class DoctorStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("approved")
    APPROVED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("suspended")
    SUSPENDED,
}

@Serializable
enum class RequestStatus {
    @SerialName("requested")
    REQUESTED,

    @SerialName("accepted")
    ACCEPTED,

    @SerialName("confirmed")
    CONFIRMED,

    @SerialName("completed")
    COMPLETED,

    @SerialName("rejected")
    REJECTED,

    @SerialName("cancelled")
    CANCELLED,
}

@Serializable
enum class ServiceMode {
    @SerialName("in_person")
    IN_PERSON,

    @SerialName("home_visit")
    HOME_VISIT,

    @SerialName("online")
    ONLINE,
}

@Serializable
enum class NotificationType {
    @SerialName("request_created")
    REQUEST_CREATED,

    @SerialName("request_accepted")
    REQUEST_ACCEPTED,

    @SerialName("request_rejected")
    REQUEST_REJECTED,

    @SerialName("request_confirmed")
    REQUEST_CONFIRMED,

    @SerialName("request_cancelled")
    REQUEST_CANCELLED,

    @SerialName("request_completed")
    REQUEST_COMPLETED,

    @SerialName("doctor_approved")
    DOCTOR_APPROVED,

    @SerialName("doctor_rejected")
    DOCTOR_REJECTED,

    @SerialName("doctor_suspended")
    DOCTOR_SUSPENDED,

    @SerialName("system")
    SYSTEM,
}
