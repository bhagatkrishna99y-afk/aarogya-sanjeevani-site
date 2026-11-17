package com.aarogya.sanjeevani.patient.designsystem

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.designsystem.component.AsChip
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Settles, by direct observation rather than reading Material3's own
 * source, whether [AsChip]'s doc comment claim -- that `FilterChip`
 * exposes `selected` to TalkBack on its own, nothing extra needed --
 * actually holds for this project's pinned composeBom. If it ever
 * regresses (a BOM bump changing `FilterChip`'s internals), this is
 * the test that would catch it, not a repeat of this investigation.
 */
@RunWith(AndroidJUnit4::class)
class AsChipA11yTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectedState_isExposedToTheAccessibilityTree() {
        composeTestRule.setContent {
            AarogyaTheme {
                AsChip(label = "Cardiology", selected = true, onClick = {})
                AsChip(label = "Dermatology", selected = false, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Cardiology").assertIsSelected()
        composeTestRule.onNodeWithText("Dermatology").assertIsNotSelected()
    }
}
