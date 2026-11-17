package com.aarogya.sanjeevani.core.data.auth

import io.github.jan.supabase.auth.status.SessionStatus

/** The app's own, small vocabulary for auth state -- everything above
 *  :core:data reacts to this, never to supabase-kt's own [SessionStatus]
 *  directly. */
sealed interface AuthState {
    /** The Auth plugin is still restoring the session at app start, or
     *  is mid-retry trying to refresh an expired one -- not a final
     *  answer either way, so a UI watching this should keep showing a
     *  splash/loading state rather than routing to sign-in. */
    data object Loading : AuthState

    data class Authenticated(val userId: String) : AuthState

    /**
     * No session, or a session that could not be restored/refreshed.
     * The Auth plugin's own `alwaysAutoRefresh`/retry logic
     * (AuthConfig.kt) is what decides when a [SessionStatus.RefreshFailure]
     * is actually unrecoverable; once it gives up, it settles on
     * [SessionStatus.NotAuthenticated] on its own and also clears the
     * stored session via [com.aarogya.sanjeevani.core.network.EncryptedSessionManager.deleteSession] --
     * this state is simply where the app reacts to that, there's no
     * separate "give up" decision made here.
     */
    data object Unauthenticated : AuthState
}

internal fun SessionStatus.toAuthState(): AuthState =
    when (this) {
        is SessionStatus.Initializing -> AuthState.Loading
        is SessionStatus.RefreshFailure -> AuthState.Loading
        is SessionStatus.Authenticated -> session.user?.id?.let(AuthState::Authenticated) ?: AuthState.Unauthenticated
        is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
    }
