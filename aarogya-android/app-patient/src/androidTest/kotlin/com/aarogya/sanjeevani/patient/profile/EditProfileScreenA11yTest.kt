package com.aarogya.sanjeevani.patient.profile

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility-focused checks for [EditProfileScreen]'s loading state --
 * the audit finding this covers: the screen used to render the actual
 * (empty) form fields plus a bare spinner while loading, which both
 * looked like blank input and left TalkBack silent. [EditProfileScreen]
 * now shows a dedicated skeleton instead, and that skeleton group must
 * announce "Loading" once (see [com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics]).
 */
@RunWith(AndroidJUnit4::class)
class EditProfileScreenA11yTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whileLoading_announcesLoadingOnce_andNeverShowsTheEmptyForm() {
        composeTestRule.setContent {
            AarogyaTheme {
                EditProfileScreen(
                    uiState = EditProfileUiState(isLoading = true),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Full name").assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Loading")).assertExists()
    }

    @Test
    fun theScreenTitle_isMarkedAsAHeading() {
        composeTestRule.setContent {
            AarogyaTheme {
                EditProfileScreen(
                    uiState = EditProfileUiState(isLoading = false, fullName = "Jane Doe"),
                    onAction = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Edit profile")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }
}
