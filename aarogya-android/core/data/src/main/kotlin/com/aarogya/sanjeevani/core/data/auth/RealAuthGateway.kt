package com.aarogya.sanjeevani.core.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class RealAuthGateway
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : AuthGateway {
        override val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

        override suspend fun signUpWithEmail(
            email: String,
            password: String,
            metadata: Map<String, String>,
        ): UserInfo? {
            try {
                return client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    this.data = buildJsonObject { metadata.forEach { (key, value) -> put(key, value) } }
                }
            } catch (e: AuthRestException) {
                throw when {
                    // GoTrue has used both codes for "this email is already
                    // registered" across versions -- map either.
                    e.errorCode == AuthErrorCode.EmailExists || e.errorCode == AuthErrorCode.UserAlreadyExists ->
                        AuthGatewayException.EmailAlreadyInUse(e)
                    // Neither of these has an AuthErrorCode entry in
                    // supabase-kt 3.0.3 -- match GoTrue's raw `error` code
                    // string directly (confirmed via a real signup call;
                    // never log `e.description`/`e.message` for these,
                    // GoTrue echoes the submitted email address in them).
                    e.error == "email_address_invalid" -> AuthGatewayException.InvalidEmailAddress(e)
                    e.error == "over_email_send_rate_limit" -> AuthGatewayException.EmailSendRateLimited(e)
                    else -> e
                }
            }
        }

        override suspend fun signInWithEmail(
            email: String,
            password: String,
        ) {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: AuthRestException) {
                throw when (e.errorCode) {
                    AuthErrorCode.InvalidCredentials -> AuthGatewayException.InvalidCredentials(e)
                    AuthErrorCode.EmailNotConfirmed -> AuthGatewayException.EmailNotConfirmed(e)
                    else -> e
                }
            }
        }

        // SignOutScope.LOCAL (the default) still revokes this session's access
        // token server-side -- "invalidate the session on the server" per the
        // task -- it just doesn't also sign out every *other* device/session
        // for this account, which nothing here asks for.
        override suspend fun signOut() {
            client.auth.signOut()
        }

        override suspend fun resetPasswordForEmail(email: String) {
            client.auth.resetPasswordForEmail(email)
        }

        override fun currentSessionOrNull(): UserSession? = client.auth.currentSessionOrNull()
    }
