package com.aarogya.sanjeevani.patient.home

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeConnectivityObserver
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeServicesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val FIXED_NOW = Instant.parse("2026-06-15T10:00:00Z")
private val FUTURE_SLOT = Instant.parse("2026-06-16T10:00:00Z")
private val fixedClock =
    object : Clock {
        override fun now(): Instant = FIXED_NOW
    }

/**
 * Covers [HomeViewModel]'s "load sections independently" contract --
 * one section failing never blocks or regresses another, a silent
 * (resume-triggered) refresh never surfaces an error event, and an
 * explicit per-section retry does. See [HomeViewModel]'s own doc
 * comment for the full rationale this exercises.
 */
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var servicesRepository: FakeServicesRepository
    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var connectivityObserver: FakeConnectivityObserver
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
        servicesRepository = FakeServicesRepository()
        requestsRepository = FakeRequestsRepository()
        doctorsRepository = FakeDoctorsRepository()
        connectivityObserver = FakeConnectivityObserver()
        viewModel =
            HomeViewModel(
                authRepository = authRepository,
                profileRepository = profileRepository,
                servicesRepository = servicesRepository,
                requestsRepository = requestsRepository,
                doctorsRepository = doctorsRepository,
                clock = fixedClock,
                connectivityObserver = connectivityObserver,
            )
    }

    @Test
    fun `resumed loads every section when all three succeed`() =
        runTest {
            servicesRepository.categories += aCategory()
            doctorsRepository.doctors += aDoctor()
            requestsRepository.patientRequests += aPatientRequest()

            viewModel.onAction(HomeAction.Resumed)

            val state = viewModel.uiState.value
            assertTrue(state.categories is HomeSectionState.Success)
            assertTrue(state.requests is HomeSectionState.Success)
            assertTrue(state.doctors is HomeSectionState.Success)
        }

    @Test
    fun `one section failing does not block or delay the other sections`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()
            doctorsRepository.doctors += aDoctor()
            requestsRepository.patientRequests += aPatientRequest()

            viewModel.onAction(HomeAction.Resumed)

            val state = viewModel.uiState.value
            assertTrue(state.categories is HomeSectionState.Error)
            assertTrue(state.requests is HomeSectionState.Success)
            assertTrue(state.doctors is HomeSectionState.Success)
        }

    @Test
    fun `all three sections can fail independently in the same refresh`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()
            requestsRepository.errorToReturn = AppError.Network()
            doctorsRepository.errorToReturn = AppError.Unknown()

            viewModel.onAction(HomeAction.Resumed)

            val state = viewModel.uiState.value
            assertTrue(state.categories is HomeSectionState.Error)
            assertTrue(state.requests is HomeSectionState.Error)
            assertTrue(state.doctors is HomeSectionState.Error)
        }

    @Test
    fun `greeting name loads from the profile on success`() =
        runTest {
            authRepository.signIn(email = "jane@example.com", password = "password123")

            viewModel.onAction(HomeAction.Resumed)

            val state = viewModel.uiState.value
            assertEquals("Fake", state.greetingName)
        }

    @Test
    fun `a failed profile load keeps the previous greeting instead of crashing`() =
        runTest {
            profileRepository.errorToReturn = AppError.Unknown()

            viewModel.onAction(HomeAction.Resumed)

            val state = viewModel.uiState.value
            assertEquals(null, state.greetingName)
        }

    @Test
    fun `a successful section is never regressed to error by a later silent refresh`() =
        runTest {
            doctorsRepository.doctors += aDoctor()
            viewModel.onAction(HomeAction.Resumed)
            assertTrue(viewModel.uiState.value.doctors is HomeSectionState.Success)

            doctorsRepository.errorToReturn = AppError.Unknown()
            viewModel.onAction(HomeAction.Resumed)

            assertTrue(viewModel.uiState.value.doctors is HomeSectionState.Success)
        }

    @Test
    fun `pull-to-refresh sends ShowRefreshFailedMessage when a section fails`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()

            viewModel.uiEvent.test {
                viewModel.onAction(HomeAction.Refresh)
                assertTrue(awaitItem() is HomeUiEvent.ShowRefreshFailedMessage)
            }
            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `silent resume refresh never sends the refresh-failed event even when a section fails`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()

            viewModel.uiEvent.test {
                viewModel.onAction(HomeAction.Resumed)
                expectNoEvents()
            }
        }

    @Test
    fun `pull-to-refresh sends no event when every section succeeds`() =
        runTest {
            viewModel.uiEvent.test {
                viewModel.onAction(HomeAction.Refresh)
                expectNoEvents()
            }
        }

    @Test
    fun `retrying a failed section moves it to Loading then Success once the error is fixed`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()
            viewModel.onAction(HomeAction.Resumed)
            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Error)

            servicesRepository.errorToReturn = null
            servicesRepository.categories += aCategory()
            viewModel.onAction(HomeAction.RetryCategories)

            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)
        }

    @Test
    fun `retrying a failed section that still fails reports a fresh Error, not the stale one`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Unknown()
            viewModel.onAction(HomeAction.Resumed)

            servicesRepository.errorToReturn = AppError.Network()
            viewModel.onAction(HomeAction.RetryCategories)

            val categories = viewModel.uiState.value.categories
            assertTrue(categories is HomeSectionState.Error)
            assertTrue((categories as HomeSectionState.Error).error is AppError.Network)
        }

    @Test
    fun `a failed section recovers automatically when connectivity is restored, with no explicit retry`() =
        runTest {
            connectivityObserver.setConnected(false)
            servicesRepository.errorToReturn = AppError.Network()
            viewModel.onAction(HomeAction.Resumed)
            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Error)

            servicesRepository.errorToReturn = null
            servicesRepository.categories += aCategory()
            connectivityObserver.setConnected(true)

            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)
        }

    @Test
    fun `connectivity merely flapping while already connected does not re-trigger a successful section`() =
        runTest {
            servicesRepository.categories += aCategory()
            viewModel.onAction(HomeAction.Resumed)
            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)

            // Already connected at startup -- this is not a false-to-true
            // edge, so it must not re-run anything that already succeeded.
            connectivityObserver.setConnected(true)

            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)
        }

    @Test
    fun `retrying an already-successful section is a no-op`() =
        runTest {
            servicesRepository.categories += aCategory()
            viewModel.onAction(HomeAction.Resumed)
            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)

            servicesRepository.errorToReturn = AppError.Unknown()
            viewModel.onAction(HomeAction.RetryCategories)

            assertTrue(viewModel.uiState.value.categories is HomeSectionState.Success)
        }

    @Test
    fun `requests section only surfaces up to 3 relevant upcoming requests`() =
        runTest {
            requestsRepository.patientRequests +=
                listOf(
                    aPatientRequest(id = "r1", status = RequestStatus.REQUESTED),
                    aPatientRequest(id = "r2", status = RequestStatus.ACCEPTED),
                    aPatientRequest(id = "r3", status = RequestStatus.CONFIRMED),
                    aPatientRequest(id = "r4", status = RequestStatus.REQUESTED),
                    aPatientRequest(id = "r5", status = RequestStatus.COMPLETED),
                    aPatientRequest(id = "r6", status = RequestStatus.REQUESTED, scheduledStart = FIXED_NOW),
                )

            viewModel.onAction(HomeAction.Resumed)

            val requests = viewModel.uiState.value.requests
            assertTrue(requests is HomeSectionState.Success)
            assertEquals(3, (requests as HomeSectionState.Success).data.size)
        }

    private fun aCategory(id: String = "cat-1") =
        ServiceCategory(
            id = id,
            name = "General",
            slug = "general",
            description = null,
            icon = "stethoscope",
            isActive = true,
            sortOrder = 0,
            createdAt = FIXED_NOW,
            updatedAt = FIXED_NOW,
        )

    private fun aDoctor(id: String = "doc-1") =
        DoctorPublic(
            doctorId = id,
            fullName = "Dr. A",
            title = null,
            specialization = "General Practice",
            qualifications = null,
            bio = null,
            yearsOfExperience = 5,
            consultationModes = listOf(ServiceMode.IN_PERSON),
            acceptingRequests = true,
            avatarPath = null,
        )

    private fun aPatientRequest(
        id: String = "req-1",
        status: RequestStatus = RequestStatus.REQUESTED,
        scheduledStart: Instant = FUTURE_SLOT,
    ) = PatientRequest(
        id = id,
        patientId = FakeAuthRepository.FAKE_USER_ID,
        doctorId = "doc-1",
        serviceId = "svc-1",
        serviceName = "Home Nursing",
        doctorFullName = "Dr. A",
        doctorSpecialization = "General Practice",
        scheduledStart = scheduledStart,
        scheduledEnd = scheduledStart,
        mode = ServiceMode.IN_PERSON,
        status = status,
        patientNotes = null,
        contactPhone = null,
        addressText = null,
        rejectionReason = null,
        cancellationReason = null,
        createdAt = FIXED_NOW,
        updatedAt = FIXED_NOW,
    )
}
