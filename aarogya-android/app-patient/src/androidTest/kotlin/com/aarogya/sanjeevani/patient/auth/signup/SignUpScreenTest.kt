package com.aarogya.sanjeevani.patient.auth.signup

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [SignUpScreen] directly, same approach as `LoginScreenTest.kt`
 * -- a hand-built [SignUpUiState] and an [onAction] spy, no Hilt. The
 * submit button's label ("Create account") collides with the top bar's
 * title, same disambiguation as that file.
 */
@RunWith(AndroidJUnit4::class)
class SignUpScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun submitButton() = composeTestRule.onNode(hasText("Create account").and(hasClickAction()))

    @Test
    fun happyPath_fillingEveryFieldAndAcceptingConsent_reportsSubmit() {
        val actions = mutableListOf<SignUpAction>()
        composeTestRule.setContent {
            AarogyaTheme { SignUpScreen(uiState = SignUpUiState(), onAction = { actions.add(it) }, onBack = {}) }
        }

        composeTestRule.onNodeWithText("Full name").performTextInput("Jane Doe")
        composeTestRule.onNodeWithText("Email").performTextInput("jane@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm password").performTextInput("password123")
        // The consent checkbox has no text of its own -- toggled via its
        // toggleable role instead.
        composeTestRule.onNode(isToggleable()).performClick()
        submitButton().performClick()

        assertTrue(actions.any { it is SignUpAction.FullNameChanged && it.value == "Jane Doe" })
        assertTrue(actions.any { it is SignUpAction.EmailChanged && it.value == "jane@example.com" })
        assertTrue(actions.any { it is SignUpAction.ConsentChanged && it.checked })
        assertEquals(SignUpAction.SubmitClicked, actions.last())
    }

    @Test
    fun movingFocusPastTheEmailField_reportsFocusLost() {
        val actions = mutableListOf<SignUpAction>()
        composeTestRule.setContent {
            AarogyaTheme { SignUpScreen(uiState = SignUpUiState(), onAction = { actions.add(it) }, onBack = {}) }
        }

        composeTestRule.onNodeWithText("Email").performTextInput("not-an-email")
        composeTestRule.onNodeWithText("Email").performImeAction()

        assertTrue(actions.contains(SignUpAction.EmailFocusLost))
    }

    @Test
    fun touchedInvalidFields_showInlineErrorsForEveryField() {
        composeTestRule.setContent {
            AarogyaTheme {
                SignUpScreen(
                    uiState =
                        SignUpUiState(
                            fullName = "J",
                            email = "not-an-email",
                            password = "short",
                            confirmPassword = "different",
                            fullNameTouched = true,
                            emailTouched = true,
                            passwordTouched = true,
                            confirmPasswordTouched = true,
                        ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Please enter your full name.").assertExists()
        composeTestRule.onNodeWithText("Please enter a valid email address.").assertExists()
        composeTestRule.onNodeWithText("Password must be at least 8 characters.").assertExists()
        composeTestRule.onNodeWithText("Passwords don't match.").assertExists()
    }

    @Test
    fun submittingWithoutAcceptingConsent_showsTheConsentErrorAndDoesNotEnableResubmit() {
        composeTestRule.setContent {
            AarogyaTheme {
                SignUpScreen(
                    uiState =
                        SignUpUiState(
                            fullName = "Jane Doe",
                            email = "jane@example.com",
                            password = "password123",
                            confirmPassword = "password123",
                            submitAttempted = true,
                            consentChecked = false,
                        ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("You must accept the Terms and Privacy Policy to continue.")
            .assertExists()
    }

    @Test
    fun submittingState_disablesTheButton() {
        composeTestRule.setContent {
            AarogyaTheme {
                SignUpScreen(uiState = SignUpUiState(isSubmitting = true), onAction = {}, onBack = {})
            }
        }

        submitButton().assertIsNotEnabled()
    }
}
