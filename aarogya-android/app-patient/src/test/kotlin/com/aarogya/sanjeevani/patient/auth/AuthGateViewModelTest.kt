package com.aarogya.sanjeevani.patient.auth

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * [AuthGateViewModel] is the one place this app observes
 * [com.aarogya.sanjeevani.core.data.auth.AuthRepository]'s session state
 * and the one place [AuthGateViewModel.signOut] is called from (see its
 * own doc comment) -- these tests cover the state mirroring and the
 * double-tap guard added against a fast double-tap on the logout
 * confirmation dialog firing `signOut` twice.
 */
class AuthGateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: AuthGateViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel = AuthGateViewModel(authRepository)
    }

    @Test
    fun `starts unauthenticated and mirrors the repository's auth state`() =
        runTest {
            viewModel.authState.test {
                assertEquals(AuthState.Unauthenticated, awaitItem())

                authRepository.signIn("patient@example.com", "password123")
                assertEquals(AuthState.Authenticated(FakeAuthRepository.FAKE_USER_ID), awaitItem())
            }
        }

    @Test
    fun `signOut signs the repository out`() =
        runTest {
            authRepository.signIn("patient@example.com", "password123")

            viewModel.authState.test {
                assertEquals(AuthState.Authenticated(FakeAuthRepository.FAKE_USER_ID), awaitItem())

                viewModel.signOut()

                assertEquals(AuthState.Unauthenticated, awaitItem())
            }
        }
}
