package com.aarogya.sanjeevani.core.data.auth

import com.aarogya.sanjeevani.core.common.result.AppResult
import kotlinx.coroutines.flow.Flow

/** The signed-in user's id, or null while signed out. */
typealias UserId = String

/*
 * FLAG_SECURE decision (no screen exists yet to attach this to, so it's
 * recorded here for whoever builds the sign-up/sign-in screens): **don't**
 * set it on the password-entry screen(s). FLAG_SECURE blocks screenshots
 * *and* makes the window appear black in the system Recents thumbnail and
 * to screen readers/accessibility services -- appropriate for a payment
 * card entry screen, too heavy-handed for a login form. The actual
 * sensitive material here (the session's access/refresh tokens) is
 * already protected independently and more precisely by
 * [com.aarogya.sanjeevani.core.network.EncryptedSessionManager] (Keystore-
 * backed encryption at rest) -- FLAG_SECURE would be defending against a
 * much narrower threat (someone screenshotting a password while it's
 * being typed) at the cost of breaking Recents previews and TalkBack for
 * that screen. Revisit this if a future requirement specifically demands
 * it (e.g. an enterprise/MDM policy), but don't set it by default.
 */

interface AuthRepository {
    /** Emits the current [AuthState] immediately, then again on every
     *  change -- session restore at app start, sign-in, sign-out, and
     *  an unrecoverable token-refresh failure all surface here. */
    fun observeAuthState(): Flow<AuthState>

    /** A live/local read (not a network call) -- `null` while signed out. */
    fun currentUserId(): UserId?

    /** `auth.signUpWith(Email)` with `{ full_name, signup_role: "user" }`
     *  metadata (client-contract.md §1) -- always a patient account,
     *  never a doctor one. */
    suspend fun signUpPatient(
        fullName: String,
        email: String,
        password: String,
    ): AppResult<SignUpOutcome>

    /** The raw sign-in operation, with no role gate -- prefer
     *  [signInAsPatient] for this app's actual sign-in screen; this one
     *  exists for anything that genuinely just needs "is this email/
     *  password combination valid" with no opinion on what role the
     *  account should have. */
    suspend fun signIn(
        email: String,
        password: String,
    ): AppResult<Unit>

    /**
     * [signIn], then verifies the now-signed-in account is a patient
     * (`profiles.role == 'user'`) and not suspended -- the entry point
     * this app's sign-in screen should actually call. A wrong-role or
     * suspended account is signed back out again before this returns;
     * either case comes back as a successful [PatientSignInResult]
     * (not an [AppResult.Failure]) since the credentials themselves
     * were valid -- see [PatientSignInResult].
     */
    suspend fun signInAsPatient(
        email: String,
        password: String,
    ): AppResult<PatientSignInResult>

    /**
     * Revokes the current session's token server-side and clears the
     * locally stored session (both handled by signing out through
     * supabase-kt's `Auth` plugin -- see
     * [com.aarogya.sanjeevani.core.network.EncryptedSessionManager]).
     * Nothing in this module caches anything else in memory today, so
     * there's nothing further to clear here; if a future repository
     * adds an in-memory cache, clearing it on sign-out belongs here.
     * Navigating to a signed-out destination is the caller's job --
     * react to [observeAuthState] emitting [AuthState.Unauthenticated]
     * rather than assuming this call itself triggers navigation.
     */
    suspend fun signOut(): AppResult<Unit>

    /**
     * `auth.resetPasswordForEmail(email)` -- Supabase's own endpoint for
     * this is deliberately indistinguishable whether or not an account
     * exists for [email] (an anti-enumeration measure, not a gap in this
     * wrapper), so an [AppResult.Success] here means "the request was
     * accepted", never "an account exists" -- the UI should show the
     * same message either way. An [AppResult.Failure] still means a
     * genuine problem (network, rate limit), worth surfacing as such.
     */
    suspend fun sendPasswordReset(email: String): AppResult<Unit>
}
