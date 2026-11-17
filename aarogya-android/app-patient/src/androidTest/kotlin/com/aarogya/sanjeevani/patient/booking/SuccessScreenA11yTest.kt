package com.aarogya.sanjeevani.patient.booking

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [StatusChip] (used here for "Requested") always carries an explicit
 * "Status: ..." content description alongside its icon+label -- this
 * confirms that non-color-only signal actually reaches [SuccessScreen],
 * not just the shared component in isolation.
 */
@RunWith(AndroidJUnit4::class)
class SuccessScreenA11yTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theStatusChip_announcesStatusAsText_notJustColor() {
        composeTestRule.setContent {
            AarogyaTheme {
                SuccessScreen(
                    uiState =
                        RequestFormUiState(
                            serviceId = "svc-1",
                            serviceName = "General Checkup",
                            doctorId = "doctor-1",
                            doctorName = "Dr. Asha Rao",
                            slotStart = null,
                            slotDurationMinutes = null,
                            today = LocalDate(2026, 1, 1),
                            submittedRequestId = "req-1",
                        ),
                    onDoneClick = {},
                    onViewRequestClick = {},
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Status: Requested")).assertExists()
    }
}
