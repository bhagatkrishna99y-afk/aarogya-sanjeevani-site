package com.aarogya.sanjeevani.patient.requests

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.days

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")
private const val PAGE_SIZE = 20

class RequestsListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var viewModel: RequestsListViewModel

    @Before
    fun setUp() {
        requestsRepository = FakeRequestsRepository()
        viewModel = RequestsListViewModel(requestsRepository)
    }

    @Test
    fun `active and past requests are split into the right tab`() =
        runTest {
            requestsRepository.patientRequests += aRequest(id = "r1", status = RequestStatus.REQUESTED)
            requestsRepository.patientRequests += aRequest(id = "r2", status = RequestStatus.COMPLETED)

            viewModel.onAction(RequestsListAction.Resumed)

            val content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(listOf("r1"), content.active.map { it.id })
            assertEquals(listOf("r2"), content.past.map { it.id })
        }

    @Test
    fun `active requests sort soonest-first, past requests sort most-recent-first`() =
        runTest {
            requestsRepository.patientRequests +=
                aRequest(id = "later", status = RequestStatus.REQUESTED, scheduledStart = SOME_INSTANT.plusDays(5))
            requestsRepository.patientRequests +=
                aRequest(id = "sooner", status = RequestStatus.ACCEPTED, scheduledStart = SOME_INSTANT.plusDays(1))
            requestsRepository.patientRequests +=
                aRequest(id = "older", status = RequestStatus.CANCELLED, scheduledStart = SOME_INSTANT.plusDays(1))
            requestsRepository.patientRequests +=
                aRequest(id = "newer", status = RequestStatus.REJECTED, scheduledStart = SOME_INSTANT.plusDays(5))

            viewModel.onAction(RequestsListAction.Resumed)

            val content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(listOf("sooner", "later"), content.active.map { it.id })
            assertEquals(listOf("newer", "older"), content.past.map { it.id })
        }

    @Test
    fun `load more reveals another page of only the currently-selected tab`() =
        runTest {
            repeat(PAGE_SIZE + 5) { index ->
                requestsRepository.patientRequests +=
                    aRequest(
                        id = "active-$index",
                        status = RequestStatus.REQUESTED,
                        scheduledStart = SOME_INSTANT.plusDays(index.toLong()),
                    )
            }
            viewModel.onAction(RequestsListAction.Resumed)

            var content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(PAGE_SIZE, content.active.size)
            assertTrue(content.hasMoreActive)

            viewModel.onAction(RequestsListAction.LoadMore)

            content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(PAGE_SIZE + 5, content.active.size)
            assertFalse(content.hasMoreActive)
        }

    @Test
    fun `load more on the past tab leaves the active tab's page untouched`() =
        runTest {
            repeat(PAGE_SIZE + 5) { index ->
                requestsRepository.patientRequests +=
                    aRequest(
                        id = "past-$index",
                        status = RequestStatus.COMPLETED,
                        scheduledStart = SOME_INSTANT.plusDays(index.toLong()),
                    )
            }
            repeat(PAGE_SIZE + 5) { index ->
                requestsRepository.patientRequests +=
                    aRequest(
                        id = "active-$index",
                        status = RequestStatus.REQUESTED,
                        scheduledStart = SOME_INSTANT.plusDays(index.toLong()),
                    )
            }
            viewModel.onAction(RequestsListAction.Resumed)
            viewModel.onAction(RequestsListAction.TabSelected(RequestsTab.PAST))

            viewModel.onAction(RequestsListAction.LoadMore)

            val content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(PAGE_SIZE + 5, content.past.size)
            assertEquals(PAGE_SIZE, content.active.size)
        }

    @Test
    fun `a refresh failure that lands on existing content keeps that content and sends an event`() =
        runTest {
            requestsRepository.patientRequests += aRequest(id = "r1", status = RequestStatus.REQUESTED)
            viewModel.onAction(RequestsListAction.Resumed)
            requestsRepository.errorToReturn = AppError.Network()

            viewModel.onAction(RequestsListAction.Refresh)

            val content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(listOf("r1"), content.active.map { it.id })
            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `a failure with nothing loaded yet shows the error section`() =
        runTest {
            requestsRepository.errorToReturn = AppError.Network()

            viewModel.onAction(RequestsListAction.Resumed)

            assertTrue(viewModel.uiState.value.section is RequestsListSectionState.Error)
        }

    @Test
    fun `retry resets to Loading before re-fetching`() =
        runTest {
            requestsRepository.errorToReturn = AppError.Network()
            viewModel.onAction(RequestsListAction.Resumed)
            requestsRepository.errorToReturn = null
            requestsRepository.patientRequests += aRequest(id = "r1", status = RequestStatus.REQUESTED)

            viewModel.onAction(RequestsListAction.Retry)

            val content = viewModel.uiState.value.section as RequestsListSectionState.Content
            assertEquals(listOf("r1"), content.active.map { it.id })
        }

    private fun aRequest(
        id: String,
        status: RequestStatus,
        scheduledStart: Instant = SOME_INSTANT,
    ) = PatientRequest(
        id = id,
        patientId = FakeAuthRepository.FAKE_USER_ID,
        doctorId = "doctor-1",
        serviceId = "svc-1",
        serviceName = "General Checkup",
        doctorFullName = "Dr. Asha Rao",
        doctorSpecialization = "General Medicine",
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledStart,
        mode = ServiceMode.ONLINE,
        status = status,
        patientNotes = null,
        contactPhone = "9876543210",
        addressText = null,
        rejectionReason = null,
        cancellationReason = null,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )

    private fun Instant.plusDays(count: Long): Instant = this + count.days
}
