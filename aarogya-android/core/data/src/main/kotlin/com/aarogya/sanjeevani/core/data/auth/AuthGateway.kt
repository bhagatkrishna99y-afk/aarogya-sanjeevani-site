package com.aarogya.sanjeevani.core.data.auth

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Everything [AuthRepositoryImpl] needs from supabase-kt's `Auth` plugin
 * -- narrowed to exactly this handful of operations so a test can fake
 * this interface directly (see
 * [com.aarogya.sanjeevani.core.testing.fake.FakeAuthGateway]) instead of
 * needing a real/mocked `SupabaseClient`, which would mean constructing
 * a real Ktor `HttpResponse` for every scenario. [RealAuthGateway] is
 * the only production implementation.
 */
interface AuthGateway {
    /** Mirrors `SupabaseClient.auth.sessionStatus` directly -- see
     *  [AuthState] for how [AuthRepositoryImpl] turns this into the
     *  app's own vocabulary. */
    val sessionStatus: Flow<SessionStatus>

    /** `auth.signUpWith(Email) { email = ...; password = ...; data =
     *  metadata }`. Returns the created user's info regardless of
     *  whether a session was also established -- call [currentSessionOrNull]
     *  afterward to tell "signed up and signed in" apart from "signed up,
     *  pending email confirmation" (client-contract.md §1). */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        metadata: Map<String, String>,
    ): UserInfo?

    /** `auth.signInWith(Email) { ... }`. */
    suspend fun signInWithEmail(
        email: String,
        password: String,
    )

    /** `auth.signOut()` -- revokes the current session's token server-side
     *  (see [RealAuthGateway] for why the default `SignOutScope.LOCAL` is
     *  enough to satisfy "invalidate the session on the server"). */
    suspend fun signOut()

    suspend fun resetPasswordForEmail(email: String)

    /** `auth.currentSessionOrNull()` -- a live/local read, not a network
     *  call; used right after [signUpWithEmail] to check whether a
     *  session actually got established. */
    fun currentSessionOrNull(): UserSession?
}
