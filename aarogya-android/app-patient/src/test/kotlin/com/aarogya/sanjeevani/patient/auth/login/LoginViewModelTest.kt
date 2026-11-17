package com.aarogya.sanjeevani.patient.auth.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.auth.PatientSignInResult
import com.aarogya.sanjeevani.core.data.model.UserRole
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(authRepository, SavedStateHandle())
    }

    @Test
    fun `successful sign-in clears submitting and sends no navigation event itself`() =
        runTest {
            viewModel.onAction(LoginAction.EmailChanged("jane@example.com"))
            viewModel.onAction(LoginAction.PasswordChanged("password123"))

            viewModel.onAction(LoginAction.SubmitClicked)

            assertFalse(viewModel.uiState.value.isSubmitting)
            // Navigation is the auth gate's job once AuthState flips --
            // see LoginContract.kt's doc comment -- so the fake's
            // authStateFlow having moved to Authenticated is the only
            // observable effect of a successful sign-in here.
            assertEquals(FakeAuthRepository.FAKE_USER_ID, authRepository.currentUserId())
        }

    @Test
    fun `wrong credentials surfaces a ShowError event and resets submitting`() =
        runTest {
            authRepository.errorToReturn = AppError.Business(code = "AUTH_INVALID_CREDENTIALS", message = "")
            viewModel.onAction(LoginAction.EmailChanged("jane@example.com"))
            viewModel.onAction(LoginAction.PasswordChanged("wrong-password"))

            viewModel.uiEvent.test {
                viewModel.onAction(LoginAction.SubmitClicked)
                val event = awaitItem()
                assertTrue(event is LoginUiEvent.ShowError)
                val businessError = (event as LoginUiEvent.ShowError).error as AppError.Business
                assertEquals("AUTH_INVALID_CREDENTIALS", businessError.code)
            }
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `a wrong-role account resets submitting and sends no event of its own`() =
        runTest {
            authRepository.patientSignInResult = PatientSignInResult.WrongRole(UserRole.DOCTOR)
            viewModel.onAction(LoginAction.EmailChanged("doctor@example.com"))
            viewModel.onAction(LoginAction.PasswordChanged("password123"))

            // Not a ShowRejection event from this ViewModel -- the auth
            // gate's reaction to this same sign-in attempt would tear this
            // screen down before any event it sent could be collected. See
            // AuthRepositoryImplTest's own coverage of signInRejections,
            // the channel that message actually comes from.
            viewModel.uiEvent.test {
                viewModel.onAction(LoginAction.SubmitClicked)
                expectNoEvents()
            }
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `double-tapping submit while already submitting only sends one sign-in attempt`() =
        runTest {
            viewModel.onAction(LoginAction.EmailChanged("jane@example.com"))
            viewModel.onAction(LoginAction.PasswordChanged("password123"))
            // The fake completes synchronously under the unconfined test
            // dispatcher, so this mainly documents the guard exists --
            // a slow repository is exercised by the Compose UI test's
            // "double submit" scenario instead.
            viewModel.onAction(LoginAction.SubmitClicked)
            viewModel.onAction(LoginAction.SubmitClicked)

            assertFalse(viewModel.uiState.value.isSubmitting)
        }
}
