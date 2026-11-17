package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.requests.BookingSettings
import com.aarogya.sanjeevani.core.data.requests.DoctorRequest
import com.aarogya.sanjeevani.core.data.requests.NewServiceRequest
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.requests.RequestStatusHistoryEntry
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** In-memory [RequestsRepository]. Seed [openSlotsByDate] for
 *  [getOpenSlots] (per date, so a test can simulate a slot vanishing
 *  between two calls for the *same* date), and [patientRequests]/
 *  [doctorRequests] for the two "my requests" reads --
 *  [createRequest]/[transitionStatus] just append to/mutate those two
 *  lists directly rather than re-implementing the backend's own status
 *  state machine or keeping a single unified `requests` table the way
 *  the real schema does. */
class FakeRequestsRepository : RequestsRepository {
    val openSlotsByDate = mutableMapOf<LocalDate, List<OpenSlot>>()
    val patientRequests = mutableListOf<PatientRequest>()
    val doctorRequests = mutableListOf<DoctorRequest>()
    val statusHistory = mutableMapOf<String, MutableList<RequestStatusHistoryEntry>>()
    var nextRequestId = 1

    var errorToReturn: AppError? = null

    /** A test sets this > 0 to keep [getOpenSlots] genuinely suspended
     *  on the shared `MainDispatcherRule` scheduler long enough to
     *  prove a *later* call's result wins over this one -- see
     *  `AvailabilityViewModelTest`'s rapid-date-change cases. */
    var getOpenSlotsDelayMillis: Long = 0
    var getOpenSlotsCallCount = 0

    var bookingSettings = BookingSettings(maxHorizonDays = 60, minLeadMinutes = 60, cancellationNoticeMinutes = 0)

    var createRequestCallCount = 0

    /** Same role as [getOpenSlotsDelayMillis], for [createRequest] --
     *  a test sets this > 0 to keep a submit genuinely in flight on the
     *  shared `MainDispatcherRule` scheduler long enough for a second,
     *  rapid tap to land *while* it's still running and prove the
     *  double-submit guard actually blocks it (with no delay at all,
     *  the first call would already be done before the second one
     *  starts, and the guard would never be exercised). */
    var createRequestDelayMillis: Long = 0

    /** Independent of [errorToReturn] -- a test that wants `transitionStatus`
     *  to fail (an AS016 conflict, e.g.) while the detail screen's own
     *  follow-up re-read still succeeds needs the two decoupled; the
     *  real backend would never fail a plain read with a transition-
     *  specific business code the way a single shared flag would force
     *  here. */
    var transitionStatusErrorToReturn: AppError? = null

    override suspend fun getOpenSlots(
        doctorId: String,
        date: LocalDate,
    ): AppResult<List<OpenSlot>> {
        getOpenSlotsCallCount++
        errorToReturn?.let { return it.asError() }
        if (getOpenSlotsDelayMillis > 0) delay(getOpenSlotsDelayMillis)
        return (openSlotsByDate[date] ?: emptyList()).asSuccess()
    }

    override suspend fun getBookingSettings(): AppResult<BookingSettings> {
        errorToReturn?.let { return it.asError() }
        return bookingSettings.asSuccess()
    }

    override suspend fun createRequest(request: NewServiceRequest): AppResult<String> {
        createRequestCallCount++
        if (createRequestDelayMillis > 0) delay(createRequestDelayMillis)
        errorToReturn?.let { return it.asError() }
        val id = "fake-request-${nextRequestId++}"
        patientRequests +=
            PatientRequest(
                id = id,
                patientId = FakeAuthRepository.FAKE_USER_ID,
                doctorId = request.doctorId,
                serviceId = request.serviceId,
                serviceName = "Fake service",
                doctorFullName = null,
                doctorSpecialization = null,
                scheduledStart = request.slotStart,
                scheduledEnd = request.slotStart,
                mode = request.mode,
                status = RequestStatus.REQUESTED,
                patientNotes = request.notes,
                contactPhone = request.contactPhone,
                addressText = request.address,
                rejectionReason = null,
                cancellationReason = null,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
            )
        return id.asSuccess()
    }

    override suspend fun transitionStatus(
        requestId: String,
        newStatus: RequestStatus,
        reason: String?,
    ): AppResult<Unit> {
        (errorToReturn ?: transitionStatusErrorToReturn)?.let { return it.asError() }
        val patientIndex = patientRequests.indexOfFirst { it.id == requestId }
        if (patientIndex != -1) {
            patientRequests[patientIndex] = patientRequests[patientIndex].copy(status = newStatus)
        }
        val doctorIndex = doctorRequests.indexOfFirst { it.id == requestId }
        if (doctorIndex != -1) {
            doctorRequests[doctorIndex] = doctorRequests[doctorIndex].copy(status = newStatus)
        }
        val found = patientIndex != -1 || doctorIndex != -1
        return if (found) Unit.asSuccess() else AppError.NotFound(code = "AS015_REQUEST_NOT_FOUND").asError()
    }

    override suspend fun getMyRequestsAsPatient(): AppResult<List<PatientRequest>> {
        errorToReturn?.let { return it.asError() }
        return patientRequests.toList().asSuccess()
    }

    override suspend fun getMyRequestsAsDoctor(): AppResult<List<DoctorRequest>> {
        errorToReturn?.let { return it.asError() }
        return doctorRequests.toList().asSuccess()
    }

    override suspend fun getStatusHistory(requestId: String): AppResult<List<RequestStatusHistoryEntry>> {
        errorToReturn?.let { return it.asError() }
        return (statusHistory[requestId] ?: emptyList()).asSuccess()
    }
}
