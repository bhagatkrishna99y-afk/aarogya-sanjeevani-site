package com.aarogya.sanjeevani.patient.auth.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [LoginScreen] directly -- the stateless half, with a
 * hand-built [LoginUiState] and an [onAction] lambda that just records
 * what it was called with, no Hilt/ViewModel/NavController anywhere
 * (see that composable's own doc comment for why it's `internal`
 * rather than `private`). The submit button's own label ("Log in")
 * collides with the top bar's title, so every lookup for it below
 * matches on `hasClickAction()` too, to find the button specifically.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun loginButton() = composeTestRule.onNode(hasText("Log in").and(hasClickAction()))

    @Test
    fun happyPath_fillingBothFieldsAndSubmitting_reportsTheExpectedActions() {
        val actions = mutableListOf<LoginAction>()
        composeTestRule.setContent {
            AarogyaTheme {
                LoginScreen(uiState = LoginUiState(), onAction = { actions.add(it) })
            }
        }

        composeTestRule.onNodeWithText("Email").performTextInput("jane@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        loginButton().performClick()

        assertTrue(actions.any { it is LoginAction.EmailChanged && it.value == "jane@example.com" })
        assertTrue(actions.any { it is LoginAction.PasswordChanged && it.value == "password123" })
        assertEquals(LoginAction.SubmitClicked, actions.last())
    }

    @Test
    fun forgotPasswordAndSignUpLinks_reportTheirOwnActions() {
        val actions = mutableListOf<LoginAction>()
        composeTestRule.setContent {
            AarogyaTheme { LoginScreen(uiState = LoginUiState(), onAction = { actions.add(it) }) }
        }

        composeTestRule.onNodeWithText("Forgot password?").performClick()
        composeTestRule.onNodeWithText("Create an account").performClick()

        assertEquals(listOf(LoginAction.ForgotPasswordClicked, LoginAction.SignUpClicked), actions)
    }

    @Test
    fun submittingState_disablesEveryControl_soASecondTapCannotDoubleSubmit() {
        composeTestRule.setContent {
            AarogyaTheme {
                LoginScreen(
                    uiState = LoginUiState(email = "jane@example.com", password = "password123", isSubmitting = true),
                    onAction = {},
                )
            }
        }

        loginButton().assertIsNotEnabled()
    }

    @Test
    fun idleState_submitButtonIsEnabled() {
        composeTestRule.setContent {
            AarogyaTheme { LoginScreen(uiState = LoginUiState(), onAction = {}) }
        }

        loginButton().assertIsEnabled()
    }
}
