package com.aarogya.sanjeevani.patient.requests

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.requests.RequestStatusHistoryEntry
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestUpdatesSource
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val REQUEST_ID = "req-1"
private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

class RequestDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var requestUpdatesSource: FakeRequestUpdatesSource
    private lateinit var viewModel: RequestDetailViewModel

    @Before
    fun setUp() {
        requestsRepository = FakeRequestsRepository()
        authRepository = FakeAuthRepository()
        requestUpdatesSource = FakeRequestUpdatesSource()
        viewModel = RequestDetailViewModel(requestsRepository, authRepository, requestUpdatesSource)
    }

    // -- Cancel-button visibility, for every status --------------------

    @Test
    fun `requested, accepted and confirmed are cancellable`() {
        assertTrue(RequestStatus.REQUESTED.isCancellable)
        assertTrue(RequestStatus.ACCEPTED.isCancellable)
        assertTrue(RequestStatus.CONFIRMED.isCancellable)
    }

    @Test
    fun `completed, rejected and cancelled are not cancellable`() {
        assertFalse(RequestStatus.COMPLETED.isCancellable)
        assertFalse(RequestStatus.REJECTED.isCancellable)
        assertFalse(RequestStatus.CANCELLED.isCancellable)
    }

    // -- Timeline mapping -----------------------------------------------

    @Test
    fun `timeline actors map null to System, the signed-in patient to You, and anyone else to Doctor`() =
        runTest {
            authRepository.signIn("patient@example.com", "pw")
            requestsRepository.patientRequests += aRequest()
            requestsRepository.statusHistory[REQUEST_ID] =
                mutableListOf(
                    aHistoryEntry(oldStatus = null, newStatus = RequestStatus.REQUESTED, changedBy = null),
                    aHistoryEntry(
                        oldStatus = RequestStatus.REQUESTED,
                        newStatus = RequestStatus.ACCEPTED,
                        changedBy = "doctor-99",
                    ),
                    aHistoryEntry(
                        oldStatus = RequestStatus.ACCEPTED,
                        newStatus = RequestStatus.CONFIRMED,
                        changedBy = FakeAuthRepository.FAKE_USER_ID,
                    ),
                )

            viewModel.onAction(RequestDetailAction.Resumed(REQUEST_ID))

            val content = viewModel.uiState.value.section as RequestDetailSectionState.Content
            assertEquals(listOf("System", "Doctor", "You"), content.timeline.map { it.actor })
            assertEquals(listOf("Requested", "Accepted", "Confirmed"), content.timeline.map { it.label })
        }

    // -- Cancel flow ------------------------------------------------------

    @Test
    fun `confirming cancel calls transitionStatus, refreshes the detail and sends CancelSucceeded`() =
        runTest {
            requestsRepository.patientRequests += aRequest(status = RequestStatus.REQUESTED)
            viewModel.onAction(RequestDetailAction.Resumed(REQUEST_ID))
            viewModel.onAction(RequestDetailAction.CancelClicked)

            viewModel.uiEvent.test {
                viewModel.onAction(RequestDetailAction.CancelConfirmed)
                assertEquals(RequestDetailUiEvent.CancelSucceeded, awaitItem())
            }

            val content = viewModel.uiState.value.section as RequestDetailSectionState.Content
            assertEquals(RequestStatus.CANCELLED, content.request.status)
            assertFalse(viewModel.uiState.value.isCancelling)
            assertFalse(viewModel.uiState.value.showCancelDialog)
        }

    @Test
    fun `an AS016 conflict closes the dialog, refreshes the detail and sends CancelConflict`() =
        runTest {
            requestsRepository.patientRequests += aRequest(status = RequestStatus.REQUESTED)
            viewModel.onAction(RequestDetailAction.Resumed(REQUEST_ID))
            viewModel.onAction(RequestDetailAction.CancelClicked)

            requestsRepository.transitionStatusErrorToReturn =
                AppError.Business(code = "AS016_INVALID_TRANSITION", message = "stale")
            // The request itself is already accepted by the time the patient confirms --
            // the re-read right after the conflict should reflect that, not the stale "requested".
            requestsRepository.patientRequests[0] =
                requestsRepository.patientRequests[0].copy(status = RequestStatus.ACCEPTED)

            viewModel.uiEvent.test {
                viewModel.onAction(RequestDetailAction.CancelConfirmed)
                assertTrue(awaitItem() is RequestDetailUiEvent.CancelConflict)
            }

            assertFalse(viewModel.uiState.value.showCancelDialog)
            assertFalse(viewModel.uiState.value.isCancelling)
            val content = viewModel.uiState.value.section as RequestDetailSectionState.Content
            assertEquals(RequestStatus.ACCEPTED, content.request.status)
        }

    @Test
    fun `a network failure on cancel keeps the dialog open and sends CancelFailed`() =
        runTest {
            requestsRepository.patientRequests += aRequest(status = RequestStatus.REQUESTED)
            viewModel.onAction(RequestDetailAction.Resumed(REQUEST_ID))
            viewModel.onAction(RequestDetailAction.CancelClicked)
            requestsRepository.transitionStatusErrorToReturn = AppError.Network()

            viewModel.uiEvent.test {
                viewModel.onAction(RequestDetailAction.CancelConfirmed)
                assertTrue(awaitItem() is RequestDetailUiEvent.CancelFailed)
            }

            assertTrue(viewModel.uiState.value.showCancelDialog)
            assertFalse(viewModel.uiState.value.isCancelling)
        }

    private fun aRequest(status: RequestStatus = RequestStatus.REQUESTED) =
        PatientRequest(
            id = REQUEST_ID,
            patientId = FakeAuthRepository.FAKE_USER_ID,
            doctorId = "doctor-99",
            serviceId = "svc-1",
            serviceName = "General Checkup",
            doctorFullName = "Dr. Asha Rao",
            doctorSpecialization = "General Medicine",
            scheduledStart = SOME_INSTANT,
            scheduledEnd = SOME_INSTANT,
            mode = ServiceMode.IN_PERSON,
            status = status,
            patientNotes = null,
            contactPhone = "9876543210",
            addressText = null,
            rejectionReason = null,
            cancellationReason = null,
            createdAt = SOME_INSTANT,
            updatedAt = SOME_INSTANT,
        )

    private fun aHistoryEntry(
        oldStatus: RequestStatus?,
        newStatus: RequestStatus,
        changedBy: String?,
    ) = RequestStatusHistoryEntry(
        id = "hist-$newStatus",
        requestId = REQUEST_ID,
        oldStatus = oldStatus,
        newStatus = newStatus,
        changedBy = changedBy,
        note = null,
        changedAt = SOME_INSTANT,
    )
}
