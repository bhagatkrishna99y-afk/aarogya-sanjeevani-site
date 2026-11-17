package com.aarogya.sanjeevani.patient.booking

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeServicesRepository
import com.aarogya.sanjeevani.patient.navigation.RequestForm
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val SERVICE_ID = "svc-1"
private const val DOCTOR_ID = "doc-1"
private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")
private val SLOT_START = Instant.parse("2026-01-05T03:30:00Z")

/**
 * The full happy path end to end -- the real [RequestFormViewModel]
 * (fakes throughout, no Hilt/Navigation) driving its own three
 * stateless screens directly, one swapped in for another exactly the
 * way `PatientNavHost.kt`'s `bookingFlow` reacts to the same events.
 * Each screen already has its own, narrower test elsewhere in this
 * module (selection/validation/error states); this one proves the
 * whole flow actually wires together: fill in Step 1, review it, submit,
 * land on Success with a "Requested" result.
 */
@RunWith(AndroidJUnit4::class)
class RequestFormFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var viewModel: RequestFormViewModel

    @Before
    fun setUp() {
        val servicesRepository = FakeServicesRepository().apply { services += aService() }
        val doctorsRepository = FakeDoctorsRepository().apply { doctors += aDoctor() }
        requestsRepository = FakeRequestsRepository()
        viewModel =
            RequestFormViewModel(
                servicesRepository = servicesRepository,
                doctorsRepository = doctorsRepository,
                authRepository = FakeAuthRepository(),
                profileRepository = FakeProfileRepository(),
                requestsRepository = requestsRepository,
                savedStateHandle = SavedStateHandle(),
                route =
                    RequestForm(
                        serviceId = SERVICE_ID,
                        serviceName = "General Checkup",
                        doctorId = DOCTOR_ID,
                        doctorName = "Dr. Asha Rao",
                        slotStart = SLOT_START.toString(),
                        slotDurationMinutes = 30,
                    ),
            )
    }

    @Test
    fun happyPath_fillingTheFormAndSubmitting_reachesSuccessWithARequestedResult() {
        composeTestRule.setContent {
            AarogyaTheme {
                var step by remember { mutableIntStateOf(0) }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            RequestFormUiEvent.NavigateToReview -> step = 1
                            RequestFormUiEvent.NavigateToSuccess -> step = 2
                            else -> Unit
                        }
                    }
                }

                when (step) {
                    0 -> RequestFormScreen(uiState = uiState, onAction = viewModel::onAction, onBack = {})
                    1 ->
                        ReviewScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            onBack = {},
                            onEditClick = { step = 0 },
                        )
                    else -> SuccessScreen(uiState = uiState, onDoneClick = {}, onViewRequestClick = {})
                }
            }
        }

        // Step 1: pick a mode, fill in the one required field.
        composeTestRule.onNodeWithText("In person").performClick()
        composeTestRule.onNodeWithText("Contact phone").performTextInput("9876543210")
        composeTestRule.onNode(hasText("Continue").and(hasClickAction())).performClick()

        // Step 2: everything just entered shows up for review.
        composeTestRule.onNodeWithText("General Checkup").assertExists()
        composeTestRule.onNodeWithText("Dr. Asha Rao").assertExists()
        composeTestRule.onNode(hasText("Submit request").and(hasClickAction())).performClick()

        // Success.
        composeTestRule.onNodeWithText("Request sent").assertExists()
        composeTestRule.onNodeWithText("View request").assertExists()
        assertEquals(1, requestsRepository.createRequestCallCount)
        assertEquals(ServiceMode.IN_PERSON, requestsRepository.patientRequests.single().mode)
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
