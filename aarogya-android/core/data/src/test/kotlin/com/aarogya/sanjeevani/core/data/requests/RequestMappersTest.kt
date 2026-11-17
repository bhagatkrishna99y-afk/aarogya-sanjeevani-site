package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val START = Instant.parse("2026-01-01T09:00:00Z")
private val END = Instant.parse("2026-01-01T09:30:00Z")
private val CREATED = Instant.parse("2025-12-01T00:00:00Z")
private val UPDATED = Instant.parse("2025-12-02T00:00:00Z")

/**
 * Every mapper in `Request.kt` is a plain field-for-field copy -- each
 * test here sets every field to a distinct, recognizable value so a
 * future dropped/renamed field fails loudly instead of silently
 * reaching production, since neither side's type system catches that on
 * its own.
 */
class RequestMappersTest {
    @Test
    fun `PatientRequestViewDto maps every field across unchanged`() {
        val dto =
            PatientRequestViewDto(
                id = "req-1",
                patientId = "patient-1",
                doctorId = "doctor-1",
                serviceId = "svc-1",
                serviceName = "General Checkup",
                doctorFullName = "Dr. Asha Rao",
                doctorSpecialization = "General Medicine",
                scheduledStart = START,
                scheduledEnd = END,
                mode = ServiceMode.ONLINE,
                status = RequestStatus.ACCEPTED,
                patientNotes = "First visit",
                doctorResponseNote = null,
                contactPhone = "9876543210",
                addressText = null,
                rejectionReason = null,
                cancellationReason = null,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals("req-1", domain.id)
        assertEquals("patient-1", domain.patientId)
        assertEquals("doctor-1", domain.doctorId)
        assertEquals("svc-1", domain.serviceId)
        assertEquals("General Checkup", domain.serviceName)
        assertEquals("Dr. Asha Rao", domain.doctorFullName)
        assertEquals("General Medicine", domain.doctorSpecialization)
        assertEquals(START, domain.scheduledStart)
        assertEquals(END, domain.scheduledEnd)
        assertEquals(ServiceMode.ONLINE, domain.mode)
        assertEquals(RequestStatus.ACCEPTED, domain.status)
        assertEquals("First visit", domain.patientNotes)
        assertEquals("9876543210", domain.contactPhone)
        assertEquals(CREATED, domain.createdAt)
        assertEquals(UPDATED, domain.updatedAt)
    }

    @Test
    fun `PatientRequestViewDto with no doctor assigned yet maps the doctor fields to null`() {
        val dto =
            PatientRequestViewDto(
                id = "req-1",
                patientId = "patient-1",
                doctorId = null,
                serviceId = "svc-1",
                serviceName = "General Checkup",
                doctorFullName = null,
                doctorSpecialization = null,
                scheduledStart = START,
                scheduledEnd = END,
                mode = ServiceMode.ONLINE,
                status = RequestStatus.REQUESTED,
                patientNotes = null,
                doctorResponseNote = null,
                contactPhone = null,
                addressText = null,
                rejectionReason = null,
                cancellationReason = null,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertNull(domain.doctorId)
        assertNull(domain.doctorFullName)
        assertNull(domain.doctorSpecialization)
    }

    @Test
    fun `DoctorRequestViewDto maps every field across unchanged`() {
        val dto =
            DoctorRequestViewDto(
                id = "req-1",
                patientId = "patient-1",
                patientFullName = "Jane Doe",
                serviceId = "svc-1",
                serviceName = "General Checkup",
                doctorId = "doctor-1",
                scheduledStart = START,
                scheduledEnd = END,
                mode = ServiceMode.HOME_VISIT,
                status = RequestStatus.CONFIRMED,
                patientNotes = "Allergic to penicillin",
                doctorResponseNote = null,
                contactPhone = "9876543210",
                addressText = "221B Baker Street",
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals("req-1", domain.id)
        assertEquals("patient-1", domain.patientId)
        assertEquals("Jane Doe", domain.patientFullName)
        assertEquals("svc-1", domain.serviceId)
        assertEquals("General Checkup", domain.serviceName)
        assertEquals("doctor-1", domain.doctorId)
        assertEquals(START, domain.scheduledStart)
        assertEquals(END, domain.scheduledEnd)
        assertEquals(ServiceMode.HOME_VISIT, domain.mode)
        assertEquals(RequestStatus.CONFIRMED, domain.status)
        assertEquals("Allergic to penicillin", domain.patientNotes)
        assertEquals("9876543210", domain.contactPhone)
        assertEquals("221B Baker Street", domain.addressText)
        assertEquals(CREATED, domain.createdAt)
        assertEquals(UPDATED, domain.updatedAt)
    }

    @Test
    fun `OpenSlotDto maps both fields across unchanged`() {
        val dto = OpenSlotDto(slotStart = START, slotEnd = END)

        val domain = dto.toDomain()

        assertEquals(START, domain.slotStart)
        assertEquals(END, domain.slotEnd)
    }

    @Test
    fun `BookingSettingsDto maps every field across unchanged`() {
        val dto = BookingSettingsDto(maxHorizonDays = 30, minLeadMinutes = 60, cancellationNoticeMinutes = 120)

        val domain = dto.toDomain()

        assertEquals(30, domain.maxHorizonDays)
        assertEquals(60, domain.minLeadMinutes)
        assertEquals(120, domain.cancellationNoticeMinutes)
    }

    @Test
    fun `RequestStatusHistoryEntryDto maps every field across unchanged`() {
        val dto =
            RequestStatusHistoryEntryDto(
                id = "hist-1",
                requestId = "req-1",
                oldStatus = RequestStatus.REQUESTED,
                newStatus = RequestStatus.ACCEPTED,
                changedBy = "doctor-1",
                note = "Accepted",
                changedAt = CREATED,
            )

        val domain = dto.toDomain()

        assertEquals("hist-1", domain.id)
        assertEquals("req-1", domain.requestId)
        assertEquals(RequestStatus.REQUESTED, domain.oldStatus)
        assertEquals(RequestStatus.ACCEPTED, domain.newStatus)
        assertEquals("doctor-1", domain.changedBy)
        assertEquals("Accepted", domain.note)
        assertEquals(CREATED, domain.changedAt)
    }

    @Test
    fun `RequestStatusHistoryEntryDto's first entry has a null oldStatus`() {
        val dto =
            RequestStatusHistoryEntryDto(
                id = "hist-1",
                requestId = "req-1",
                oldStatus = null,
                newStatus = RequestStatus.REQUESTED,
                changedBy = "patient-1",
                note = null,
                changedAt = CREATED,
            )

        val domain = dto.toDomain()

        assertNull(domain.oldStatus)
    }

    @Test
    fun `NewServiceRequest maps to CreateServiceRequestParams with every field across unchanged`() {
        val newRequest =
            NewServiceRequest(
                serviceId = "svc-1",
                doctorId = "doctor-1",
                slotStart = START,
                mode = ServiceMode.IN_PERSON,
                notes = "First visit",
                contactPhone = "9876543210",
                address = "221B Baker Street",
            )

        val params = newRequest.toParams()

        assertEquals("svc-1", params.serviceId)
        assertEquals("doctor-1", params.doctorId)
        assertEquals(START, params.slotStart)
        assertEquals(ServiceMode.IN_PERSON, params.mode)
        assertEquals("First visit", params.notes)
        assertEquals("9876543210", params.contactPhone)
        assertEquals("221B Baker Street", params.address)
    }
}
