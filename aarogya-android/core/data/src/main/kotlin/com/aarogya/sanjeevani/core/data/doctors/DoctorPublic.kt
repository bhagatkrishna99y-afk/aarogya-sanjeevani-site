package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.data.model.ServiceMode

data class DoctorPublic(
    val doctorId: String,
    val fullName: String,
    val title: String?,
    val specialization: String,
    val qualifications: String?,
    val bio: String?,
    val yearsOfExperience: Int,
    val consultationModes: List<ServiceMode>,
    val acceptingRequests: Boolean,
    /** Storage path inside the private `doctor-avatars` bucket -- resolve
     *  to a signed URL via the storage client before displaying, never
     *  construct a URL from this directly (client-contract.md §4). */
    val avatarPath: String?,
)

data class DoctorServicePublic(
    val doctorId: String,
    val fullName: String,
    val serviceId: String,
    val serviceName: String,
    val serviceSlug: String,
)

fun DoctorPublicDto.toDomain(): DoctorPublic =
    DoctorPublic(
        doctorId = doctorId,
        fullName = fullName,
        title = title,
        specialization = specialization,
        qualifications = qualifications,
        bio = bio,
        yearsOfExperience = yearsOfExperience,
        consultationModes = consultationModes,
        acceptingRequests = acceptingRequests,
        avatarPath = avatarUrl,
    )

fun DoctorServicePublicDto.toDomain(): DoctorServicePublic =
    DoctorServicePublic(
        doctorId = doctorId,
        fullName = fullName,
        serviceId = serviceId,
        serviceName = serviceName,
        serviceSlug = serviceSlug,
    )
