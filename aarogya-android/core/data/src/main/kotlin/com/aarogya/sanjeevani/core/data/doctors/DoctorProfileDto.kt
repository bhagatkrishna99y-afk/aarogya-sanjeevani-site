package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.data.model.DoctorStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `doctor_profiles` table -- a doctor's own application/
 *  profile, read via `select * from doctor_profiles where user_id =
 *  auth.uid()` (client-contract.md §12). Includes admin-only fields
 *  ([adminNotes], [licenseNumber]) that are never present in the public
 *  [DoctorPublicDto]/[DoctorServicePublicDto] views -- this shape is
 *  only ever the signed-in doctor's own row (or an admin's), never
 *  shown for another doctor. */
@Serializable
data class DoctorProfileDto(
    @SerialName("user_id") val userId: String,
    val title: String?,
    val specialization: String,
    val qualifications: String?,
    val bio: String?,
    @SerialName("years_experience") val yearsExperience: Int,
    @SerialName("license_number") val licenseNumber: String,
    @SerialName("consultation_modes") val consultationModes: List<ServiceMode>,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    val status: DoctorStatus,
    @SerialName("rejection_reason") val rejectionReason: String?,
    @SerialName("admin_notes") val adminNotes: String?,
    @SerialName("reviewed_by") val reviewedBy: String?,
    @SerialName("reviewed_at") val reviewedAt: Instant?,
    @SerialName("accepting_requests") val acceptingRequests: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
