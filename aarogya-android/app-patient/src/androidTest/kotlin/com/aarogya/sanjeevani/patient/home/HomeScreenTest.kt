package com.aarogya.sanjeevani.patient.home

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val SOME_INSTANT = Instant.parse("2026-06-01T00:00:00Z")
private val FUTURE_INSTANT = Instant.parse("2026-06-15T10:00:00Z")

/**
 * Exercises the stateless [HomeScreen] directly with a hand-built
 * [HomeUiState] -- same pattern as `LoginScreenTest.kt` -- covering the
 * two states most likely to regress silently: every section empty, and
 * every section populated with real content and working per-item
 * callbacks.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsFriendlyMessagesAndBookFirstServiceButtonWorks() {
        var browseServicesClicked = false
        composeTestRule.setContent {
            AarogyaTheme {
                HomeScreen(
                    uiState =
                        HomeUiState(
                            greetingName = "Aanya",
                            categories = HomeSectionState.Success(emptyList()),
                            requests = HomeSectionState.Success(emptyList()),
                            doctors = HomeSectionState.Success(emptyList()),
                        ),
                    greetingPrefix = "Good morning",
                    onAction = {},
                    onBrowseServicesClick = { browseServicesClicked = true },
                    onFindDoctorClick = {},
                    onCategoryClick = {},
                    onViewAllRequestsClick = {},
                    onRequestClick = {},
                    onDoctorClick = {},
                    onNotificationsClick = {},
                    onProfileClick = {},
                    unreadNotificationCount = 0,
                )
            }
        }

        composeTestRule.onNodeWithText("Good morning, Aanya").assertExists()
        composeTestRule.onNodeWithText("No services are available right now.").assertExists()
        composeTestRule.onNodeWithText("You don't have any upcoming requests yet.").assertExists()
        composeTestRule.onNodeWithText("No doctors are available right now.").assertExists()

        composeTestRule.onNodeWithText("Book your first service").performClick()
        assertEquals(true, browseServicesClicked)
    }

    @Test
    fun populatedState_showsRealContentAndEachItemReportsItsOwnCallback() {
        var categoryClicked: String? = null
        var requestClicked: String? = null
        var doctorClicked: String? = null

        composeTestRule.setContent {
            AarogyaTheme {
                HomeScreen(
                    uiState =
                        HomeUiState(
                            greetingName = "Aanya",
                            categories = HomeSectionState.Success(listOf(aCategory())),
                            requests = HomeSectionState.Success(listOf(aPatientRequest())),
                            doctors = HomeSectionState.Success(listOf(aDoctor())),
                        ),
                    greetingPrefix = "Good morning",
                    onAction = {},
                    onBrowseServicesClick = {},
                    onFindDoctorClick = {},
                    onCategoryClick = { categoryClicked = it },
                    onViewAllRequestsClick = {},
                    onRequestClick = { requestClicked = it },
                    onDoctorClick = { doctorClicked = it },
                    onNotificationsClick = {},
                    onProfileClick = {},
                    unreadNotificationCount = 3,
                )
            }
        }

        composeTestRule.onNodeWithText("Good morning, Aanya").assertExists()
        composeTestRule.onNodeWithContentDescription("3 unread notifications").assertExists()

        composeTestRule.onNodeWithText("General").performClick()
        assertEquals("cat-1", categoryClicked)

        composeTestRule.onNode(hasText("Home Nursing").and(hasClickAction())).performClick()
        assertEquals("req-1", requestClicked)

        composeTestRule.onNode(hasText("Dr. A").and(hasClickAction())).performClick()
        assertEquals("doc-1", doctorClicked)
    }

    private fun aCategory() =
        ServiceCategory(
            id = "cat-1",
            name = "General",
            slug = "general",
            description = null,
            icon = "stethoscope",
            isActive = true,
            sortOrder = 0,
            createdAt = SOME_INSTANT,
            updatedAt = SOME_INSTANT,
        )

    private fun aPatientRequest() =
        PatientRequest(
            id = "req-1",
            patientId = "patient-1",
            doctorId = "doc-1",
            serviceId = "svc-1",
            serviceName = "Home Nursing",
            doctorFullName = "Dr. A",
            doctorSpecialization = "General Practice",
            scheduledStart = FUTURE_INSTANT,
            scheduledEnd = FUTURE_INSTANT,
            mode = ServiceMode.IN_PERSON,
            status = RequestStatus.REQUESTED,
            patientNotes = null,
            contactPhone = null,
            addressText = null,
            rejectionReason = null,
            cancellationReason = null,
            createdAt = SOME_INSTANT,
            updatedAt = SOME_INSTANT,
        )

    private fun aDoctor() =
        DoctorPublic(
            doctorId = "doc-1",
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
}
