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

    /** GoTrue's `email_address_invalid` -- not deliverable/well-formed
     *  enough for this project's Auth settings (e.g. a non-resolvable
     *  domain). Not in supabase-kt 3.0.3's own `AuthErrorCode` enum, so
     *  [RealAuthGateway] matches the raw `error` string, not the enum. */
    class InvalidEmailAddress(cause: Throwable? = null) :
        AuthGatewayException("That email address isn't valid", cause)

    /** GoTrue's `over_email_send_rate_limit` -- the account itself would
     *  be fine, but this project's email provider has a send-rate cap
     *  and it's been hit (confirmation/reset emails share one budget). */
    class EmailSendRateLimited(cause: Throwable? = null) :
        AuthGatewayException("Too many emails sent recently", cause)
}
