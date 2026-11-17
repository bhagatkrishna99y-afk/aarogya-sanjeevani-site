package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.data.model.ServiceMode

/** In-memory [DoctorsRepository] seeded with [doctors] and
 *  [doctorServices] (the `doctor_services_public` pairings).
 *  [getDoctorsPage] mirrors the real query's own sort/filter/pagination
 *  in plain Kotlin -- same ordering (by [DoctorPublic.fullName]), same
 *  filter semantics (name-or-specialization substring, exact
 *  specialization, mode containment), same `offset`/`limit` window. */
class FakeDoctorsRepository : DoctorsRepository {
    val doctors = mutableListOf<DoctorPublic>()
    val doctorServices = mutableListOf<DoctorServicePublic>()

    /** Seed with `path to signedUrl` entries for [getAvatarUrls] to
     *  return -- a path with no entry here simply resolves to nothing,
     *  exactly like the real bucket failing to sign it. */
    val avatarUrlsByPath = mutableMapOf<String, String>()

    var errorToReturn: AppError? = null

    override suspend fun getAllDoctors(): AppResult<List<DoctorPublic>> {
        errorToReturn?.let { return it.asError() }
        return doctors.toList().asSuccess()
    }

    override suspend fun getDoctorsPage(
        offset: Int,
        limit: Int,
        searchQuery: String?,
        specialization: String?,
        mode: ServiceMode?,
    ): AppResult<List<DoctorPublic>> {
        errorToReturn?.let { return it.asError() }
        val matching =
            doctors.filter { doctor ->
                (searchQuery.isNullOrBlank() || doctor.matchesSearch(searchQuery)) &&
                    (specialization == null || doctor.specialization == specialization) &&
                    (mode == null || mode in doctor.consultationModes)
            }
        return matching
            .sortedBy { it.fullName }
            .drop(offset)
            .take(limit)
            .asSuccess()
    }

    override suspend fun getDoctor(doctorId: String): AppResult<DoctorPublic> {
        errorToReturn?.let { return it.asError() }
        return doctors.firstOrNull { it.doctorId == doctorId }?.asSuccess()
            ?: AppError.NotFound().asError()
    }

    override suspend fun getDoctorsByIds(doctorIds: List<String>): AppResult<List<DoctorPublic>> {
        errorToReturn?.let { return it.asError() }
        return doctors.filter { it.doctorId in doctorIds }.asSuccess()
    }

    override suspend fun getDoctorsForService(serviceId: String): AppResult<List<DoctorServicePublic>> {
        errorToReturn?.let { return it.asError() }
        return doctorServices.filter { it.serviceId == serviceId }.asSuccess()
    }

    override suspend fun getServicesForDoctor(doctorId: String): AppResult<List<DoctorServicePublic>> {
        errorToReturn?.let { return it.asError() }
        return doctorServices.filter { it.doctorId == doctorId }.asSuccess()
    }

    override suspend fun getServicesForDoctors(doctorIds: List<String>): AppResult<List<DoctorServicePublic>> {
        errorToReturn?.let { return it.asError() }
        return doctorServices.filter { it.doctorId in doctorIds }.asSuccess()
    }

    override suspend fun getAvatarUrls(avatarPaths: List<String>): Map<String, String> =
        avatarPaths.mapNotNull { path -> avatarUrlsByPath[path]?.let { path to it } }.toMap()

    private fun DoctorPublic.matchesSearch(query: String): Boolean =
        fullName.contains(query, ignoreCase = true) || specialization.contains(query, ignoreCase = true)
}
