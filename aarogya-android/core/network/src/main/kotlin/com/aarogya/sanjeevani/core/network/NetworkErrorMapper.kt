package com.aarogya.sanjeevani.core.network

import com.aarogya.sanjeevani.core.common.error.AppError
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import timber.log.Timber
import java.io.IOException

/**
 * Every custom backend error is raised as `RAISE EXCEPTION '<CODE>:
 * message' USING ERRCODE = 'P0001'` (backend/docs/error-codes.md) --
 * **never branch on SQLSTATE**, only the `<CODE>` prefix (everything
 * before the first `:`) is a stable contract. [RestException.error]
 * carries that raw text (its inherited `.message` is a multi-line
 * string with the URL/headers appended, `.error` is just the text
 * PostgREST returned).
 */
private val AS_CODE_PREFIX = Regex("""^(AS\d{3}_\w+):""")

/** AS000 is the one code that always means exactly "you're not signed
 *  in" regardless of which RPC raised it. */
private val UNAUTHORIZED_CODES = setOf("AS000_NOT_AUTHENTICATED")

/** A genuine "this no longer exists" -- as opposed to "you're not
 *  allowed to see it," which this backend's RLS makes indistinguishable
 *  from not existing at all (backend/docs/security-principles.md). */
private val NOT_FOUND_CODES =
    setOf(
        "AS015_REQUEST_NOT_FOUND",
        "AS028_DOCTOR_NOT_FOUND",
        "AS030_USER_NOT_FOUND",
        "AS033_CATALOG_ENTITY_NOT_FOUND",
    )

/** Someone/something else changed the state this call depended on --
 *  a slot just got booked, a duplicate booking already exists. */
private val CONFLICT_CODES =
    setOf(
        "AS001_SLOT_UNAVAILABLE",
        "AS019_SLOT_ALREADY_BOOKED",
        "AS010_DUPLICATE_REQUEST",
    )

/** The submitted data itself was malformed/incomplete -- mapped to the
 *  form field key a submission screen would show the error next to.
 *  (AS026/AS032 are about the submitted data's own internal consistency
 *  -- two of *your* availability windows overlapping, a catalog field
 *  missing -- not a collision with someone else's concurrent change,
 *  which is why they're here and not in [CONFLICT_CODES].) */
private val VALIDATION_FIELD_CODES =
    mapOf(
        "AS011_ADDRESS_REQUIRED" to "address",
        "AS012_INVALID_CONTACT_PHONE" to "contactPhone",
        "AS013_TEXT_TOO_LONG" to "notes",
        "AS023_INVALID_DOCTOR_PROFILE_FIELDS" to "profile",
        "AS025_INVALID_AVAILABILITY_WINDOW" to "availability",
        "AS026_AVAILABILITY_OVERLAP" to "availability",
        "AS032_INVALID_CATALOG_FIELDS" to "catalog",
    )

/**
 * The actual AS0xx-registry mapping, as a pure function of the raw
 * backend text -- no exception type needed, so every code in the
 * registry can be exercised directly in a unit test with a plain
 * string, rather than needing a real/mocked `RestException` (which
 * needs a real Ktor `HttpResponse` to construct) for every single one.
 * [toAppError] (below) is the thin, obviously-correct glue that gets a
 * real exception's text into this function.
 */
internal fun appErrorForBackendText(
    rawText: String,
    cause: Throwable? = null,
): AppError? {
    val code = AS_CODE_PREFIX.find(rawText)?.groupValues?.get(1) ?: return null
    return when {
        code in UNAUTHORIZED_CODES -> AppError.Unauthorized(cause = cause)
        code in NOT_FOUND_CODES -> AppError.NotFound(code = code, cause = cause)
        code in CONFLICT_CODES -> AppError.Conflict(code = code, cause = cause)
        VALIDATION_FIELD_CODES.containsKey(code) ->
            AppError.Validation(fieldErrors = mapOf(VALIDATION_FIELD_CODES[code]!! to code), cause = cause)
        else -> AppError.Business(code = code, message = rawText, cause = cause)
    }
}

/**
 * Maps any exception :core:network's calls can throw down to the closed
 * [AppError] hierarchy. The one place in the app that knows what a
 * `RestException`/`IOException`/etc. actually is -- see [safeApiCall],
 * which is what every repository actually calls.
 */
internal fun Throwable.toAppError(): AppError {
    if (this is RestException) {
        // No AS-code prefix: either a real 401 (an actually expired/invalid
        // JWT, not the AS000 business check) or a backend error this
        // registry doesn't know about yet (e.g. set_doctor_services's
        // documented not-an-AS-code case -- see backend/docs/
        // client-contract.md §12 -- which only one specific call site can
        // safely interpret; a generic mapper guessing at unprefixed text
        // would mis-map any *other* unprefixed error the same way).
        return appErrorForBackendText(error, cause = this)
            ?: if (this is UnauthorizedRestException) {
                AppError.Unauthorized(cause = this)
            } else {
                // Debug builds only (Timber.DebugTree vs. release's
                // CrashReportingTree, see AarogyaPatientApplication) --
                // `error` is always the short stable code (PGRST202,
                // email_address_invalid, an AS0xx text with no
                // interpolated user data), never `description`/`message`,
                // which for some Auth errors echoes the submitted email.
                Timber.e(this, "Unmapped backend error (status=%d): %s", statusCode, error)
                AppError.Unknown(cause = this)
            }
    }
    return when (this) {
        is SerializationException -> {
            // Safe to log in full: a decode/encode shape mismatch, never
            // user-entered data.
            Timber.e(this, "Serialization error")
            AppError.Unknown(cause = this)
        }
        // Ktor's own request/connect/socket timeouts -- plain IOException
        // subtypes (not CancellationException, verified against Ktor's
        // source), so they land here rather than in safeApiCall's
        // TimeoutCancellationException catch. Distinguished from a bare
        // connectivity failure so the UI can say "that took too long"
        // instead of "no connection".
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            AppError.Network(isTimeout = true, cause = this)
        is IOException -> AppError.Network(cause = this)
        else -> {
            Timber.e(this, "Unrecognized error mapped to AppError.Unknown")
            AppError.Unknown(cause = this)
        }
    }
}
