package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.data.auth.PatientSignInResult
import com.aarogya.sanjeevani.core.data.auth.SignUpOutcome
import com.aarogya.sanjeevani.core.data.auth.UserId
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [AuthRepository] -- set [errorToReturn] before calling a
 *  method to make that call fail instead; otherwise sign-up/sign-in
 *  succeed and push [authStateFlow] to [AuthState.Authenticated] with
 *  [FAKE_USER_ID]. */
class FakeAuthRepository : AuthRepository {
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    var errorToReturn: AppError? = null

    /** The outcome [signInAsPatient] returns on success -- override to
     *  simulate a wrong-role/suspended account. */
    var patientSignInResult: PatientSignInResult = PatientSignInResult.Success(FAKE_USER_ID)

    /** The outcome [signUpPatient] returns on success -- override to
     *  [SignUpOutcome.PendingEmailConfirmation] to simulate a project
     *  that requires email confirmation (no session established). */
    var signUpOutcome: SignUpOutcome = SignUpOutcome.SignedIn(FAKE_USER_ID)

    override fun observeAuthState() = authStateFlow

    override fun currentUserId(): UserId? = (authStateFlow.value as? AuthState.Authenticated)?.userId

    override suspend fun signUpPatient(
        fullName: String,
        email: String,
        password: String,
    ): AppResult<SignUpOutcome> {
        errorToReturn?.let { return it.asError() }
        if (signUpOutcome is SignUpOutcome.SignedIn) {
            authStateFlow.value = AuthState.Authenticated(FAKE_USER_ID)
        }
        return signUpOutcome.asSuccess()
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): AppResult<Unit> {
        errorToReturn?.let { return it.asError() }
        authStateFlow.value = AuthState.Authenticated(FAKE_USER_ID)
        return Unit.asSuccess()
    }

    override suspend fun signInAsPatient(
        email: String,
        password: String,
    ): AppResult<PatientSignInResult> {
        errorToReturn?.let { return it.asError() }
        if (patientSignInResult is PatientSignInResult.Success) {
            authStateFlow.value = AuthState.Authenticated(FAKE_USER_ID)
        }
        return patientSignInResult.asSuccess()
    }

    override suspend fun signOut(): AppResult<Unit> {
        errorToReturn?.let { return it.asError() }
        authStateFlow.value = AuthState.Unauthenticated
        return Unit.asSuccess()
    }

    override suspend fun sendPasswordReset(email: String): AppResult<Unit> {
        errorToReturn?.let { return it.asError() }
        return Unit.asSuccess()
    }

    companion object {
        const val FAKE_USER_ID = "fake-user-id"
    }
}
