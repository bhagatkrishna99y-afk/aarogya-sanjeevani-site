package com.aarogya.sanjeevani.patient.auth.signup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.auth.SignUpOutcome
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: SignUpViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = SignUpViewModel(authRepository, SavedStateHandle())
    }

    private fun fillValidForm() {
        viewModel.onAction(SignUpAction.FullNameChanged("Jane Doe"))
        viewModel.onAction(SignUpAction.EmailChanged("jane@example.com"))
        viewModel.onAction(SignUpAction.PasswordChanged("password123"))
        viewModel.onAction(SignUpAction.ConfirmPasswordChanged("password123"))
        viewModel.onAction(SignUpAction.ConsentChanged(true))
    }

    @Test
    fun `submitting an empty form shows every field error and does not call the repository`() =
        runTest {
            viewModel.onAction(SignUpAction.SubmitClicked)

            val state = viewModel.uiState.value
            assertTrue(state.fullNameError != null)
            assertTrue(state.emailError != null)
            assertTrue(state.passwordError != null)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun `an unchecked consent box blocks submit and shows the consent error`() =
        runTest {
            viewModel.onAction(SignUpAction.FullNameChanged("Jane Doe"))
            viewModel.onAction(SignUpAction.EmailChanged("jane@example.com"))
            viewModel.onAction(SignUpAction.PasswordChanged("password123"))
            viewModel.onAction(SignUpAction.ConfirmPasswordChanged("password123"))

            viewModel.onAction(SignUpAction.SubmitClicked)

            assertTrue(viewModel.uiState.value.showConsentError)
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `a field is only touched, and its error visible, after it loses focus`() {
        viewModel.onAction(SignUpAction.EmailChanged("not-an-email"))
        assertFalse(viewModel.uiState.value.emailTouched)

        viewModel.onAction(SignUpAction.EmailFocusLost)

        assertTrue(viewModel.uiState.value.emailTouched)
        assertTrue(viewModel.uiState.value.emailError != null)
    }

    @Test
    fun `signing in immediately as SignedIn sends no navigation event`() =
        runTest {
            fillValidForm()
            authRepository.signUpOutcome = SignUpOutcome.SignedIn(FakeAuthRepository.FAKE_USER_ID)

            viewModel.onAction(SignUpAction.SubmitClicked)

            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `pending email confirmation navigates to check-inbox with the submitted email`() =
        runTest {
            fillValidForm()
            authRepository.signUpOutcome = SignUpOutcome.PendingEmailConfirmation

            viewModel.uiEvent.test {
                viewModel.onAction(SignUpAction.SubmitClicked)
                val event = awaitItem()
                assertTrue(event is SignUpUiEvent.NavigateToCheckInbox)
                assertEquals("jane@example.com", (event as SignUpUiEvent.NavigateToCheckInbox).email)
            }
        }

    @Test
    fun `a duplicate email surfaces a ShowError event with the duplicate-email code`() =
        runTest {
            fillValidForm()
            authRepository.errorToReturn = AppError.Business(code = "AUTH_EMAIL_ALREADY_IN_USE", message = "")

            viewModel.uiEvent.test {
                viewModel.onAction(SignUpAction.SubmitClicked)
                val event = awaitItem()
                assertTrue(event is SignUpUiEvent.ShowError)
                val error = (event as SignUpUiEvent.ShowError).error
                assertEquals("AUTH_EMAIL_ALREADY_IN_USE", (error as AppError.Business).code)
            }
            assertFalse(viewModel.uiState.value.isSubmitting)
        }
}
