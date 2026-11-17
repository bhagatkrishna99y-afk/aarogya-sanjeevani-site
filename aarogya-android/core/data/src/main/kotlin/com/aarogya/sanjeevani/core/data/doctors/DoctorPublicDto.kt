package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `select * from doctors_public` -- every approved, non-suspended
 *  doctor, patient-safe fields only (client-contract.md §4). Never
 *  includes `license_number`, `admin_notes`, email, or phone -- use
 *  [DoctorProfileDto] for the doctor's own full profile. */
@Serializable
data class DoctorPublicDto(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("full_name") val fullName: String,
    val title: String?,
    val specialization: String,
    val qualifications: String?,
    val bio: String?,
    @SerialName("years_of_experience") val yearsOfExperience: Int,
    @SerialName("consultation_modes") val consultationModes: List<ServiceMode>,
    @SerialName("accepting_requests") val acceptingRequests: Boolean,
    // Storage path inside the private `doctor-avatars` bucket, not a
    // public URL -- request a signed URL via the storage client
    // (client-contract.md §4), never construct a URL from this directly.
    @SerialName("avatar_url") val avatarUrl: String?,
)

/** `select * from doctor_services_public where service_id = ?` --
 *  which doctors offer a given service (client-contract.md §4). */
@Serializable
data class DoctorServicePublicDto(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("service_slug") val serviceSlug: String,
)
