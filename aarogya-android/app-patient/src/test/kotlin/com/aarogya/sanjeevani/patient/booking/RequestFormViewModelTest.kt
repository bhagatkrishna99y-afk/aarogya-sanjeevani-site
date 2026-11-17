package com.aarogya.sanjeevani.patient.booking

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeServicesRepository
import com.aarogya.sanjeevani.patient.navigation.RequestForm
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val SERVICE_ID = "svc-1"
private const val DOCTOR_ID = "doc-1"
private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")
private val SLOT_START = Instant.parse("2026-01-05T03:30:00Z")
private val FIXED_NOW = Instant.parse("2026-01-05T00:00:00Z")
private val FIXED_CLOCK =
    object : Clock {
        override fun now() = FIXED_NOW
    }

/** Drives [RequestFormViewModel] directly via its `internal` route-based
 *  constructor (see that class's own doc comment for why: the real
 *  `@Inject` constructor's [androidx.navigation.toRoute] decode needs a
 *  working `android.os.Bundle`, which a plain JVM unit test's
 *  `SavedStateHandle` doesn't have). [aSavedStateHandle] is still
 *  real -- just empty, standing in for a fresh screen with nothing
 *  persisted yet; a couple of tests seed it to prove persistence. */
class RequestFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var servicesRepository: FakeServicesRepository
    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var requestsRepository: FakeRequestsRepository

    @Before
    fun setUp() {
        servicesRepository = FakeServicesRepository()
        doctorsRepository = FakeDoctorsRepository()
        authRepository = FakeAuthRepository()
        profileRepository = FakeProfileRepository()
        requestsRepository = FakeRequestsRepository()

        servicesRepository.services += aService()
        doctorsRepository.doctors += aDoctor()
    }

    private fun viewModel(
        doctorId: String? = DOCTOR_ID,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        preAssignedSlot: Boolean = true,
    ) = RequestFormViewModel(
        servicesRepository = servicesRepository,
        doctorsRepository = doctorsRepository,
        authRepository = authRepository,
        profileRepository = profileRepository,
        requestsRepository = requestsRepository,
        savedStateHandle = savedStateHandle,
        clock = FIXED_CLOCK,
        route = aRoute(doctorId, preAssignedSlot),
    )

    private fun aRoute(
        doctorId: String?,
        preAssignedSlot: Boolean = true,
    ) = RequestForm(
        serviceId = SERVICE_ID,
        serviceName = "General Checkup",
        doctorId = doctorId,
        doctorName = if (doctorId != null) "Dr. Asha Rao" else null,
        slotStart = if (preAssignedSlot) SLOT_START.toString() else null,
        slotDurationMinutes = if (preAssignedSlot) 30 else null,
    )

    @Test
    fun `only modes both the service and the doctor support are offered`() =
        runTest {
            val viewModel = viewModel()

            val context = viewModel.uiState.value.contextState as RequestFormContextState.Loaded
            assertEquals(listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE), context.availableModes)
        }

    @Test
    fun `with no doctor assigned, every mode the service supports is offered`() =
        runTest {
            val viewModel = viewModel(doctorId = null)

            val context = viewModel.uiState.value.contextState as RequestFormContextState.Loaded
            assertEquals(
                listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE, ServiceMode.HOME_VISIT),
                context.availableModes,
            )
        }

    // ---- self-scheduled slot (no doctor, no pre-assigned slot) --------

    @Test
    fun `a no-doctor service with no pre-assigned slot needs a self-scheduled one`() =
        runTest {
            val viewModel = viewModel(doctorId = null, preAssignedSlot = false)

            assertTrue(viewModel.uiState.value.needsSelfScheduledSlot)
            assertNull(viewModel.uiState.value.effectiveSlotStart)
        }

    @Test
    fun `Continue stays disabled until both a date and a time window are chosen`() =
        runTest {
            val viewModel = viewModel(doctorId = null, preAssignedSlot = false)
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            assertFalse(viewModel.uiState.value.isValid)

            viewModel.onAction(RequestFormAction.DateSelected(viewModel.uiState.value.today))
            assertFalse(viewModel.uiState.value.isValid)

            viewModel.onAction(RequestFormAction.TimeWindowSelected(TimeWindow.MORNING))
            assertTrue(viewModel.uiState.value.isValid)
        }

    @Test
    fun `a date plus a time window combine into the window's representative hour in Asia-Kolkata`() =
        runTest {
            val viewModel = viewModel(doctorId = null, preAssignedSlot = false)
            val date = LocalDate(2026, 1, 10)

            viewModel.onAction(RequestFormAction.DateSelected(date))
            viewModel.onAction(RequestFormAction.TimeWindowSelected(TimeWindow.AFTERNOON))

            // 1:00 pm IST == 07:30 UTC.
            assertEquals(Instant.parse("2026-01-10T07:30:00Z"), viewModel.uiState.value.effectiveSlotStart)
        }

    @Test
    fun `submitting a self-scheduled request sends the computed slot as p_slot_start`() =
        runTest {
            val viewModel = viewModel(doctorId = null, preAssignedSlot = false)
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))
            viewModel.onAction(RequestFormAction.DateSelected(LocalDate(2026, 1, 10)))
            viewModel.onAction(RequestFormAction.TimeWindowSelected(TimeWindow.MORNING))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertEquals(RequestFormUiEvent.NavigateToSuccess, awaitItem())
            }
            // 9:00 am IST == 03:30 UTC.
            val created = requestsRepository.patientRequests.last()
            assertEquals(Instant.parse("2026-01-10T03:30:00Z"), created.scheduledStart)
            assertNull(created.doctorId)
        }

    @Test
    fun `selected date and time window survive a rotation via the shared SavedStateHandle`() =
        runTest {
            val handle = SavedStateHandle()
            val firstInstance = viewModel(doctorId = null, preAssignedSlot = false, savedStateHandle = handle)
            firstInstance.onAction(RequestFormAction.DateSelected(LocalDate(2026, 1, 10)))
            firstInstance.onAction(RequestFormAction.TimeWindowSelected(TimeWindow.EVENING))

            val recreatedInstance = viewModel(doctorId = null, preAssignedSlot = false, savedStateHandle = handle)

            assertEquals(LocalDate(2026, 1, 10), recreatedInstance.uiState.value.selectedDate)
            assertEquals(TimeWindow.EVENING, recreatedInstance.uiState.value.selectedTimeWindow)
        }

    @Test
    fun `mode, phone, address and notes survive a rotation via the shared SavedStateHandle`() =
        runTest {
            val handle = SavedStateHandle()
            val firstInstance = viewModel(savedStateHandle = handle)
            firstInstance.onAction(RequestFormAction.ModeSelected(ServiceMode.HOME_VISIT))
            firstInstance.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))
            firstInstance.onAction(RequestFormAction.AddressChanged("221B Baker Street"))
            firstInstance.onAction(RequestFormAction.NotesChanged("Ring the bell twice"))

            // Same SavedStateHandle instance, new ViewModel -- exactly
            // what rotation/process death hands back in production.
            val recreatedInstance = viewModel(savedStateHandle = handle)

            assertEquals(ServiceMode.HOME_VISIT, recreatedInstance.uiState.value.selectedMode)
            assertEquals("9876543210", recreatedInstance.uiState.value.contactPhone)
            assertEquals("221B Baker Street", recreatedInstance.uiState.value.address)
            assertEquals("Ring the bell twice", recreatedInstance.uiState.value.notes)
        }

    @Test
    fun `the contact phone is prefilled from the profile when it's blank`() =
        runTest {
            authRepository.signIn("patient@example.com", "pw")
            profileRepository.profiles[FakeAuthRepository.FAKE_USER_ID] =
                profileRepository.profiles.getValue(FakeAuthRepository.FAKE_USER_ID).copy(phone = "9876543210")

            val viewModel = viewModel()

            assertEquals("9876543210", viewModel.uiState.value.contactPhone)
        }

    @Test
    fun `continuing with an invalid phone marks every field touched and does not navigate`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("123"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.ContinueClicked)
                expectNoEvents()
            }
            assertTrue(viewModel.uiState.value.contactPhoneTouched)
            assertTrue(viewModel.uiState.value.submitAttempted)
        }

    @Test
    fun `continuing with valid data navigates to review`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.ContinueClicked)
                assertEquals(RequestFormUiEvent.NavigateToReview, awaitItem())
            }
        }

    @Test
    fun `a double tap on Submit only creates one request`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            requestsRepository.createRequestDelayMillis = 1_000
            viewModel.onAction(RequestFormAction.SubmitClicked)
            viewModel.onAction(RequestFormAction.SubmitClicked)
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, requestsRepository.createRequestCallCount)
        }

    @Test
    fun `a successful submit stores the request id and navigates to Success`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertEquals(RequestFormUiEvent.NavigateToSuccess, awaitItem())
            }
            assertEquals("fake-request-1", viewModel.uiState.value.submittedRequestId)
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `AS001_SLOT_UNAVAILABLE maps to SlotUnavailable`() =
        runTest {
            requestsRepository.errorToReturn = AppError.Conflict(code = "AS001_SLOT_UNAVAILABLE")
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertTrue(awaitItem() is RequestFormUiEvent.SlotUnavailable)
            }
        }

    @Test
    fun `a doctor-not-available business error maps to DoctorUnavailable`() =
        runTest {
            requestsRepository.errorToReturn =
                AppError.Business(code = "AS002_DOCTOR_NOT_AVAILABLE", message = "unavailable")
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertTrue(awaitItem() is RequestFormUiEvent.DoctorUnavailable)
            }
        }

    @Test
    fun `a doctor-not-approved business error also maps to DoctorUnavailable`() =
        runTest {
            requestsRepository.errorToReturn =
                AppError.Business(code = "AS018_DOCTOR_NOT_APPROVED", message = "not approved")
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertTrue(awaitItem() is RequestFormUiEvent.DoctorUnavailable)
            }
        }

    @Test
    fun `a too-many-active-requests business error maps to LimitExceeded`() =
        runTest {
            requestsRepository.errorToReturn =
                AppError.Business(code = "AS034_TOO_MANY_ACTIVE_REQUESTS", message = "limit")
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertTrue(awaitItem() is RequestFormUiEvent.LimitExceeded)
            }
        }

    @Test
    fun `a network failure maps to RetryableError and keeps the entered data`() =
        runTest {
            requestsRepository.errorToReturn = AppError.Network()
            val viewModel = viewModel()
            viewModel.onAction(RequestFormAction.ModeSelected(ServiceMode.IN_PERSON))
            viewModel.onAction(RequestFormAction.ContactPhoneChanged("9876543210"))
            viewModel.onAction(RequestFormAction.NotesChanged("Please call ahead"))

            viewModel.uiEvent.test {
                viewModel.onAction(RequestFormAction.SubmitClicked)
                assertTrue(awaitItem() is RequestFormUiEvent.RetryableError)
            }
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertEquals("9876543210", viewModel.uiState.value.contactPhone)
            assertEquals("Please call ahead", viewModel.uiState.value.notes)
        }

    private fun aService() =
        Service(
            id = SERVICE_ID,
            categoryId = "cat-1",
            name = "General Checkup",
            slug = "general-checkup",
            description = null,
            shortDescription = "Short description",
            requiresDoctor = true,
            durationMinutes = 30,
            supportedModes = listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE, ServiceMode.HOME_VISIT),
            metadata = null,
            isActive = true,
            sortOrder = 0,
            createdAt = SOME_INSTANT,
            updatedAt = SOME_INSTANT,
        )

    private fun aDoctor() =
        DoctorPublic(
            doctorId = DOCTOR_ID,
            fullName = "Dr. Asha Rao",
            title = "Dr.",
            specialization = "General Medicine",
            qualifications = "MBBS",
            bio = "A caring general physician.",
            yearsOfExperience = 8,
            consultationModes = listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE),
            acceptingRequests = true,
            avatarPath = null,
        )
}
