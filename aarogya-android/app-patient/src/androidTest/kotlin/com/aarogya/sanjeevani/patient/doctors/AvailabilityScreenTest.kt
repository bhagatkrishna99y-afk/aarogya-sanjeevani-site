package com.aarogya.sanjeevani.patient.doctors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.requests.OpenSlot
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the stateless [AvailabilityScreen] directly -- the one
 * piece of real interaction this screen owns itself: tapping a
 * [TimeSlotChip] highlights it (a check icon, never color alone -- see
 * [TimeSlotChip]'s own doc comment) and enables Continue; picking a
 * different slot moves the highlight rather than adding to it.
 */
@RunWith(AndroidJUnit4::class)
class AvailabilityScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate(2026, 1, 5)
    private val slot1 =
        OpenSlot(slotStart = Instant.parse("2026-01-05T03:30:00Z"), slotEnd = Instant.parse("2026-01-05T04:00:00Z"))
    private val slot2 =
        OpenSlot(slotStart = Instant.parse("2026-01-05T09:30:00Z"), slotEnd = Instant.parse("2026-01-05T10:00:00Z"))

    @Test
    fun selectingASlot_showsAChecked_chip_andEnablesContinue_movingTheSelectionOnASecondTap() {
        composeTestRule.setContent {
            AarogyaTheme {
                var state by remember {
                    mutableStateOf(
                        AvailabilityUiState(
                            today = today,
                            slotsSection = SlotsSectionState.Content(listOf(slot1, slot2)),
                        ),
                    )
                }
                AvailabilityScreen(
                    uiState = state,
                    doctorName = "Dr. Asha Rao",
                    serviceName = "General Checkup",
                    onAction = { action ->
                        when (action) {
                            is AvailabilityAction.SlotSelected -> state = state.copy(selectedSlot = action.slot)
                            else -> Unit
                        }
                    },
                    onBack = {},
                )
            }
        }

        val slot1Label = slot1.timeLabel(CLINIC_TIME_ZONE)
        val slot2Label = slot2.timeLabel(CLINIC_TIME_ZONE)

        composeTestRule.onNode(hasText("Continue").and(hasClickAction())).assertIsNotEnabled()
        composeTestRule.onNode(hasText(slot1Label).and(hasClickAction())).assertIsNotSelected()
        composeTestRule.onNode(hasText(slot2Label).and(hasClickAction())).assertIsNotSelected()

        composeTestRule.onNode(hasText(slot1Label).and(hasClickAction())).performClick()

        composeTestRule.onNode(hasText(slot1Label).and(hasClickAction())).assertIsSelected()
        composeTestRule.onNode(hasText(slot2Label).and(hasClickAction())).assertIsNotSelected()
        composeTestRule.onNode(hasText("Continue").and(hasClickAction())).assertIsEnabled()

        composeTestRule.onNode(hasText(slot2Label).and(hasClickAction())).performClick()

        composeTestRule.onNode(hasText(slot1Label).and(hasClickAction())).assertIsNotSelected()
        composeTestRule.onNode(hasText(slot2Label).and(hasClickAction())).assertIsSelected()
        composeTestRule.onNode(hasText("Continue").and(hasClickAction())).assertIsEnabled()
    }

    @Test
    fun theScreenTitle_isMarkedAsAHeading() {
        composeTestRule.setContent {
            AarogyaTheme {
                AvailabilityScreen(
                    uiState = AvailabilityUiState(today = today),
                    doctorName = "Dr. Asha Rao",
                    serviceName = "General Checkup",
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Availability").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
