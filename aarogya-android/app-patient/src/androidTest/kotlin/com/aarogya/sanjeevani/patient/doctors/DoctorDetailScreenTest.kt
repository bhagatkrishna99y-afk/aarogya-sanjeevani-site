package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the stateless [DoctorDetailScreen] directly, same pattern
 * as every other screen test in this module -- covering the one piece
 * of real interaction this screen owns itself: whether "View
 * availability" opens [ServicePickerBottomSheet] (no [serviceId] known
 * yet) or skips straight to [onViewAvailabilityClick] (one already
 * carried in).
 */
@RunWith(AndroidJUnit4::class)
class DoctorDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val doctor =
        DoctorPublic(
            doctorId = "doc-1",
            fullName = "Asha Rao",
            title = "Dr.",
            specialization = "General Medicine",
            qualifications = "MBBS",
            bio = "A caring general physician.",
            yearsOfExperience = 8,
            consultationModes = listOf(ServiceMode.IN_PERSON),
            acceptingRequests = true,
            avatarPath = null,
        )

    private val services =
        listOf(
            DoctorServicePublic(
                doctorId = "doc-1",
                fullName = "Asha Rao",
                serviceId = "svc-checkup",
                serviceName = "General Checkup",
                serviceSlug = "general-checkup",
            ),
            DoctorServicePublic(
                doctorId = "doc-1",
                fullName = "Asha Rao",
                serviceId = "svc-nursing",
                serviceName = "Home Nursing",
                serviceSlug = "home-nursing",
            ),
        )

    @Test
    fun noServiceIdYet_viewAvailabilityOpensThePickerSheet_andSelectingOneReportsItsId() {
        var clickedServiceId: String? = null
        composeTestRule.setContent {
            AarogyaTheme {
                DoctorDetailScreen(
                    uiState = DoctorDetailUiState.Content(doctor = doctor, avatarUrl = null, services = services),
                    serviceId = null,
                    serviceName = null,
                    onBack = {},
                    onRetry = {},
                    onViewAvailabilityClick = { serviceId, _, _ -> clickedServiceId = serviceId },
                )
            }
        }

        composeTestRule.onNode(hasText("View availability").and(hasClickAction())).performClick()

        // The sheet is open, listing both of this doctor's services.
        composeTestRule.onNodeWithText("Choose a service").assertExists()
        composeTestRule.onNodeWithText("General Checkup").assertExists()
        composeTestRule.onNodeWithText("Home Nursing").assertExists()
        assertNull(clickedServiceId)

        composeTestRule.onNodeWithText("Home Nursing").performClick()

        assertEquals("svc-nursing", clickedServiceId)
    }

    @Test
    fun aServiceIdAlreadyKnown_viewAvailabilitySkipsThePickerEntirely() {
        var clickedServiceId: String? = null
        composeTestRule.setContent {
            AarogyaTheme {
                DoctorDetailScreen(
                    uiState = DoctorDetailUiState.Content(doctor = doctor, avatarUrl = null, services = services),
                    serviceId = "svc-checkup",
                    serviceName = "General Checkup",
                    onBack = {},
                    onRetry = {},
                    onViewAvailabilityClick = { serviceId, _, _ -> clickedServiceId = serviceId },
                )
            }
        }

        composeTestRule.onNode(hasText("View availability").and(hasClickAction())).performClick()

        assertEquals("svc-checkup", clickedServiceId)
        composeTestRule.onNodeWithText("Choose a service").assertDoesNotExist()
    }

    @Test
    fun aDoctorNotAcceptingRequests_hasTheViewAvailabilityButtonDisabled() {
        val notAccepting = doctor.copy(acceptingRequests = false)
        composeTestRule.setContent {
            AarogyaTheme {
                DoctorDetailScreen(
                    uiState = DoctorDetailUiState.Content(doctor = notAccepting, avatarUrl = null, services = services),
                    serviceId = "svc-checkup",
                    serviceName = "General Checkup",
                    onBack = {},
                    onRetry = {},
                    onViewAvailabilityClick = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNode(hasText("View availability").and(hasClickAction())).assertIsNotEnabled()
        composeTestRule.onNodeWithText("This doctor isn't accepting new requests right now. Check back later.").assertExists()
    }
}
