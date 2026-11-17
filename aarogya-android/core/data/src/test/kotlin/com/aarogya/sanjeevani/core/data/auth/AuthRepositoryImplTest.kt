package com.aarogya.sanjeevani.core.data.auth

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AUTH_EMAIL_NOT_CONFIRMED_CODE
import com.aarogya.sanjeevani.core.common.error.AUTH_INVALID_CREDENTIALS_CODE
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.model.UserRole
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthGateway
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository.Companion.FAKE_USER_ID
import com.aarogya.sanjeevani.core.testing.fake.FakeProfileRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryImplTest {
    private lateinit var authGateway: FakeAuthGateway
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authGateway = FakeAuthGateway()
        profileRepository = FakeProfileRepository()
        repository = AuthRepositoryImpl(authGateway, profileRepository)
    }

    // ---- sign-up ----------------------------------------------------

    @Test
    fun `sign-up returns SignedIn when the project auto-confirms email`() =
        runTest {
            authGateway.autoConfirmSignUps = true

            val result = repository.signUpPatient("Jane Doe", "jane@example.com", "password123")

            assertTrue(result is AppResult.Success)
            val outcome = (result as AppResult.Success).data
            assertEquals(SignUpOutcome.SignedIn(FAKE_USER_ID), outcome)
        }

    @Test
    fun `sign-up returns PendingEmailConfirmation when the project requires confirmation`() =
        runTest {
            authGateway.autoConfirmSignUps = false

            val result = repository.signUpPatient("Jane Doe", "jane@example.com", "password123")

            assertTrue(result is AppResult.Success)
            val outcome = (result as AppResult.Success).data
            assertEquals(SignUpOutcome.PendingEmailConfirmation, outcome)
        }

    // ---- sign-in failure mapping --------------------------------------

    @Test
    fun `sign-in maps invalid credentials to a Business error with the auth code`() =
        runTest {
            authGateway.signInError = AuthGatewayException.InvalidCredentials()

            val result = repository.signIn("jane@example.com", "wrong-password")

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.Business)
            assertEquals(AUTH_INVALID_CREDENTIALS_CODE, (error as AppError.Business).code)
        }

    @Test
    fun `sign-in maps an unconfirmed email to a Business error with the auth code`() =
        runTest {
            authGateway.signInError = AuthGatewayException.EmailNotConfirmed()

            val result = repository.signIn("jane@example.com", "password123")

            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.Business)
            assertEquals(AUTH_EMAIL_NOT_CONFIRMED_CODE, (error as AppError.Business).code)
        }

    // ---- session restore ------------------------------------------------

    @Test
    fun `observeAuthState reflects session restore from Initializing to Authenticated`() =
        runTest {
            repository.observeAuthState().test {
                // The underlying StateFlow replays its current value
                // (Unauthenticated) to a new collector immediately --
                // consume that before asserting on the transitions this
                // test actually drives.
                assertEquals(AuthState.Unauthenticated, awaitItem())

                authGateway.setStatus(SessionStatus.Initializing)
                assertEquals(AuthState.Loading, awaitItem())

                val userInfo = FakeAuthGateway.fakeUserInfo("jane@example.com", id = FAKE_USER_ID)
                authGateway.setStatus(SessionStatus.Authenticated(FakeAuthGateway.fakeSession(userInfo)))
                assertEquals(AuthState.Authenticated(FAKE_USER_ID), awaitItem())
            }
        }

    // ---- expired / unrecoverable session ------------------------------

    @Test
    fun `observeAuthState emits Unauthenticated once the gateway gives up on an expired session`() =
        runTest {
            repository.observeAuthState().test {
                // Consume the StateFlow's initial Unauthenticated replay --
                // see the previous test for why.
                assertEquals(AuthState.Unauthenticated, awaitItem())

                val userInfo = FakeAuthGateway.fakeUserInfo("jane@example.com")
                authGateway.setStatus(SessionStatus.Authenticated(FakeAuthGateway.fakeSession(userInfo)))
                assertEquals(AuthState.Authenticated(FAKE_USER_ID), awaitItem())

                // The Auth plugin's own retry/refresh logic has given up and
                // settled back on NotAuthenticated -- see AuthState.kt's doc
                // comment on SessionStatus.RefreshFailure.
                authGateway.setStatus(SessionStatus.NotAuthenticated(isSignOut = false))
                assertEquals(AuthState.Unauthenticated, awaitItem())
            }
        }

    // ---- role gate ------------------------------------------------------

    @Test
    fun `signInAsPatient succeeds for a non-suspended patient account`() =
        runTest {
            val result = repository.signInAsPatient("patient@example.com", "password123")

            assertTrue(result is AppResult.Success)
            assertEquals(PatientSignInResult.Success(FAKE_USER_ID), (result as AppResult.Success).data)
            assertTrue(authGateway.currentSessionOrNull() != null)
        }

    @Test
    fun `signInAsPatient rejects and signs out a doctor account`() =
        runTest {
            profileRepository.profiles[FAKE_USER_ID] =
                profileRepository.profiles.getValue(FAKE_USER_ID).copy(role = UserRole.DOCTOR)

            val result = repository.signInAsPatient("patient@example.com", "password123")

            assertTrue(result is AppResult.Success)
            val outcome = (result as AppResult.Success).data
            assertEquals(PatientSignInResult.WrongRole(UserRole.DOCTOR), outcome)
            assertNull(authGateway.currentSessionOrNull())
        }

    @Test
    fun `signInAsPatient rejects and signs out a suspended account`() =
        runTest {
            profileRepository.profiles[FAKE_USER_ID] =
                profileRepository.profiles.getValue(FAKE_USER_ID).copy(isSuspended = true)

            val result = repository.signInAsPatient("patient@example.com", "password123")

            assertTrue(result is AppResult.Success)
            assertEquals(PatientSignInResult.AccountSuspended, (result as AppResult.Success).data)
            assertNull(authGateway.currentSessionOrNull())
        }
}
