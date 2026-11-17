package com.aarogya.sanjeevani.core.data.auth

/**
 * The handful of Supabase Auth failures [AuthRepositoryImpl] gives a
 * specific, friendly treatment to -- [RealAuthGateway] is the one place
 * that translates supabase-kt's own `AuthRestException`/`AuthErrorCode`
 * into these. Deliberately a plain, no-arg-constructible exception
 * hierarchy (unlike `AuthRestException`, which needs a real Ktor
 * `HttpResponse` to construct) so [AuthGateway]'s contract -- and every
 * test exercising it, via
 * [com.aarogya.sanjeevani.core.testing.fake.FakeAuthGateway] -- never
 * needs to know supabase-kt's exception types exist at all.
 */
sealed class AuthGatewayException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidCredentials(cause: Throwable? = null) : AuthGatewayException("Invalid email or password", cause)

    class EmailNotConfirmed(cause: Throwable? = null) : AuthGatewayException("Email not confirmed", cause)

    class EmailAlreadyInUse(cause: Throwable? = null) :
        AuthGatewayException("An account with this email already exists", cause)
}
