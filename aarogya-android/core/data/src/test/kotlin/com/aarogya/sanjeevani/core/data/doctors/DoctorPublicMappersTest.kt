package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DoctorPublicDto.toDomain]/[DoctorServicePublicDto.toDomain] are
 * field-for-field mappers with one deliberate rename --
 * [DoctorPublicDto.avatarUrl] becomes [DoctorPublic.avatarPath] (it's a
 * private storage path, never a usable URL on its own -- see the DTO's
 * own doc comment) -- which is exactly the kind of thing worth a
 * regression test, since the compiler can't catch a future accidental
 * "fix" that makes it `avatarPath = avatarUrl` turn into a no-op rename
 * revert.
 */
class DoctorPublicMappersTest {
    @Test
    fun `DoctorPublicDto maps every field, renaming avatarUrl to avatarPath`() {
        val dto =
            DoctorPublicDto(
                doctorId = "doctor-1",
                fullName = "Dr. Asha Rao",
                title = "MBBS, MD",
                specialization = "General Medicine",
                qualifications = "MBBS, MD",
                bio = "15 years of practice.",
                yearsOfExperience = 15,
                consultationModes = listOf(ServiceMode.ONLINE, ServiceMode.IN_PERSON),
                acceptingRequests = true,
                avatarUrl = "doctor-1/avatar.jpg",
            )

        val domain = dto.toDomain()

        assertEquals("doctor-1", domain.doctorId)
        assertEquals("Dr. Asha Rao", domain.fullName)
        assertEquals("MBBS, MD", domain.title)
        assertEquals("General Medicine", domain.specialization)
        assertEquals("MBBS, MD", domain.qualifications)
        assertEquals("15 years of practice.", domain.bio)
        assertEquals(15, domain.yearsOfExperience)
        assertEquals(listOf(ServiceMode.ONLINE, ServiceMode.IN_PERSON), domain.consultationModes)
        assertEquals(true, domain.acceptingRequests)
        assertEquals("doctor-1/avatar.jpg", domain.avatarPath)
    }

    @Test
    fun `DoctorPublicDto with a null avatarUrl maps to a null avatarPath`() {
        val dto =
            DoctorPublicDto(
                doctorId = "doctor-1",
                fullName = "Dr. Asha Rao",
                title = null,
                specialization = "General Medicine",
                qualifications = null,
                bio = null,
                yearsOfExperience = 0,
                consultationModes = emptyList(),
                acceptingRequests = false,
                avatarUrl = null,
            )

        val domain = dto.toDomain()

        assertEquals(null, domain.avatarPath)
    }

    @Test
    fun `DoctorServicePublicDto maps every field across unchanged`() {
        val dto =
            DoctorServicePublicDto(
                doctorId = "doctor-1",
                fullName = "Dr. Asha Rao",
                serviceId = "svc-1",
                serviceName = "General Checkup",
                serviceSlug = "general-checkup",
            )

        val domain = dto.toDomain()

        assertEquals("doctor-1", domain.doctorId)
        assertEquals("Dr. Asha Rao", domain.fullName)
        assertEquals("svc-1", domain.serviceId)
        assertEquals("General Checkup", domain.serviceName)
        assertEquals("general-checkup", domain.serviceSlug)
    }
}
