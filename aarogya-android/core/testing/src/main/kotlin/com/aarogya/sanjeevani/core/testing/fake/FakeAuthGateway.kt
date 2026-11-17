package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.data.auth.AuthGateway
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AuthGateway] -- this is the "fake Supabase layer" the
 * real sign-up/sign-in/session-restore/role-gate logic in
 * `AuthRepositoryImpl` gets tested against, with no real `SupabaseClient`
 * (and no real Ktor `HttpResponse`) involved anywhere.
 */
class FakeAuthGateway : AuthGateway {
    private val statusFlow = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(isSignOut = false))
    override val sessionStatus = statusFlow

    /** Thrown by the next [signUpWithEmail] call, then cleared. */
    var signUpError: Throwable? = null

    /** Thrown by the next [signInWithEmail] call, then cleared. */
    var signInError: Throwable? = null

    var resetPasswordError: Throwable? = null

    /** Whether a successful [signUpWithEmail] also establishes a
     *  session immediately -- `false` simulates this Supabase project
     *  requiring email confirmation before a session exists. */
    var autoConfirmSignUps: Boolean = true

    private var session: UserSession? = null

    fun setStatus(status: SessionStatus) {
        statusFlow.value = status
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        metadata: Map<String, String>,
    ): UserInfo? {
        signUpError?.let {
            signUpError = null
            throw it
        }
        val userInfo = fakeUserInfo(email)
        if (autoConfirmSignUps) {
            session = fakeSession(userInfo)
            statusFlow.value = SessionStatus.Authenticated(session!!)
        }
        return userInfo
    }

    override suspend fun signInWithEmail(
        email: String,
        password: String,
    ) {
        signInError?.let {
            signInError = null
            throw it
        }
        val userInfo = fakeUserInfo(email)
        session = fakeSession(userInfo)
        statusFlow.value = SessionStatus.Authenticated(session!!)
    }

    override suspend fun signOut() {
        session = null
        statusFlow.value = SessionStatus.NotAuthenticated(isSignOut = true)
    }

    override suspend fun resetPasswordForEmail(email: String) {
        resetPasswordError?.let {
            resetPasswordError = null
            throw it
        }
    }

    override fun currentSessionOrNull(): UserSession? = session

    companion object {
        fun fakeUserInfo(
            email: String,
            id: String = "fake-user-id",
        ): UserInfo = UserInfo(aud = "authenticated", id = id, email = email)

        fun fakeSession(user: UserInfo): UserSession =
            UserSession(
                accessToken = "fake-access-token",
                refreshToken = "fake-refresh-token",
                expiresIn = 3600,
                tokenType = "bearer",
                user = user,
            )
    }
}
