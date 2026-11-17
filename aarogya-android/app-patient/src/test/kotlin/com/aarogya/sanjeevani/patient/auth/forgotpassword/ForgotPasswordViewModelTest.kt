package com.aarogya.sanjeevani.patient.auth.forgotpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = ForgotPasswordViewModel(authRepository, SavedStateHandle())
    }

    @Test
    fun `an invalid email blocks submit and shows a field error instead of calling the repository`() =
        runTest {
            viewModel.onAction(ForgotPasswordAction.EmailChanged("not-an-email"))

            viewModel.onAction(ForgotPasswordAction.SubmitClicked)

            assertTrue(viewModel.uiState.value.emailError != null)
            assertFalse(viewModel.uiState.value.linkSent)
        }

    @Test
    fun `a valid email always shows the same generic success state`() =
        runTest {
            viewModel.onAction(ForgotPasswordAction.EmailChanged("jane@example.com"))

            viewModel.onAction(ForgotPasswordAction.SubmitClicked)

            assertTrue(viewModel.uiState.value.linkSent)
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `changing the email after a successful send clears the success state`() =
        runTest {
            viewModel.onAction(ForgotPasswordAction.EmailChanged("jane@example.com"))
            viewModel.onAction(ForgotPasswordAction.SubmitClicked)
            assertTrue(viewModel.uiState.value.linkSent)

            viewModel.onAction(ForgotPasswordAction.EmailChanged("jane2@example.com"))

            assertFalse(viewModel.uiState.value.linkSent)
        }

    @Test
    fun `a genuine failure, such as a network error, still surfaces as an error, not a fake success`() =
        runTest {
            authRepository.errorToReturn = AppError.Network()
            viewModel.onAction(ForgotPasswordAction.EmailChanged("jane@example.com"))

            viewModel.uiEvent.test {
                viewModel.onAction(ForgotPasswordAction.SubmitClicked)
                val event = awaitItem()
                assertTrue(event is ForgotPasswordUiEvent.ShowError)
            }
            assertFalse(viewModel.uiState.value.linkSent)
        }
}
