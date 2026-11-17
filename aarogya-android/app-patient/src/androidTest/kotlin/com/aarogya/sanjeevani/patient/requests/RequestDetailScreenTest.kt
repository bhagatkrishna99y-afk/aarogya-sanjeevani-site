package com.aarogya.sanjeevani.patient.requests

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestUpdatesSource
import com.aarogya.sanjeevani.core.testing.fake.FakeRequestsRepository
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val REQUEST_ID = "req-1"
private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

/**
 * Exercises the real [RequestDetailViewModel] (fakes throughout, no
 * Hilt) driving its own stateless [RequestDetailScreen] -- the one
 * interaction this screen owns itself: tapping "Cancel request" opens a
 * confirmation dialog first, and only confirming it actually calls
 * through to [com.aarogya.sanjeevani.core.data.requests.RequestsRepository.transitionStatus].
 * [RequestDetailViewModelTest] already covers this ViewModel's state
 * transitions directly; this one proves the dialog itself is wired up
 * on screen the way a patient would actually tap it.
 */
@RunWith(AndroidJUnit4::class)
class RequestDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var requestsRepository: FakeRequestsRepository
    private lateinit var viewModel: RequestDetailViewModel

    @Before
    fun setUp() {
        requestsRepository = FakeRequestsRepository()
        requestsRepository.patientRequests += aRequest(status = RequestStatus.REQUESTED)
        viewModel = RequestDetailViewModel(requestsRepository, FakeAuthRepository(), FakeRequestUpdatesSource())
    }

    private fun setContent() {
        composeTestRule.setContent {
            AarogyaTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { viewModel.onAction(RequestDetailAction.Resumed(REQUEST_ID)) }
                RequestDetailScreen(uiState = uiState, onAction = viewModel::onAction, onBack = {}, onBookAgainClick = {})
            }
        }
    }

    @Test
    fun tappingCancelRequest_opensAConfirmationDialog_andDoesNotCancelUntilConfirmed() {
        setContent()

        composeTestRule.onNode(hasText("Cancel request").and(hasClickAction())).performClick()

        composeTestRule.onNodeWithText("Cancel this request?").assertExists()
        assertEquals(RequestStatus.REQUESTED, requestsRepository.patientRequests.single().status)
    }

    @Test
    fun dismissingTheDialog_keepsTheRequestUntouched() {
        setContent()

        composeTestRule.onNode(hasText("Cancel request").and(hasClickAction())).performClick()
        composeTestRule.onNode(hasText("Keep request").and(hasClickAction())).performClick()

        composeTestRule.onNodeWithText("Cancel this request?").assertDoesNotExist()
        assertEquals(RequestStatus.REQUESTED, requestsRepository.patientRequests.single().status)
    }

    @Test
    fun confirmingTheDialog_cancelsTheRequest_andClosesTheDialog() {
        setContent()

        composeTestRule.onNode(hasText("Cancel request").and(hasClickAction())).performClick()
        composeTestRule
            .onNode(hasText("Cancel request").and(hasClickAction()).and(hasAnyAncestor(isDialog())))
            .performClick()

        composeTestRule.onNodeWithText("Cancel this request?").assertDoesNotExist()
        assertEquals(RequestStatus.CANCELLED, requestsRepository.patientRequests.single().status)
    }

    private fun aRequest(status: RequestStatus) =
        PatientRequest(
            id = REQUEST_ID,
            patientId = FakeAuthRepository.FAKE_USER_ID,
            doctorId = "doctor-1",
            serviceId = "svc-1",
            serviceName = "General Checkup",
            doctorFullName = "Dr. Asha Rao",
            doctorSpecialization = "General Medicine",
            scheduledStart = SOME_INSTANT,
            scheduledEnd = SOME_INSTANT,
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
}
