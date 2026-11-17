package com.aarogya.sanjeevani.core.common.error

/**
 * The closed set of failure categories every feature needs to react to,
 * independent of which library actually threw (Ktor, supabase-kt,
 * kotlinx.serialization, ...) -- :core:network's error mapper
 * (`NetworkErrorMapper.kt`) is the one place that knows how to turn a
 * real exception into one of these; nothing above :core:network should
 * ever see a raw `RestException`/`IOException`/etc.
 *
 * Every variant carries its original [cause] for logging only -- never
 * for display. [toUserMessage] (below) is the one sanctioned way to turn
 * any of these into text a user sees, and it never falls back to a raw
 * exception/backend message.
 */
sealed class AppError {
    /** No network reached the server at all, or the request timed out
     *  waiting for one. */
    data class Network(
        val isTimeout: Boolean = false,
        val cause: Throwable? = null,
    ) : AppError()

    /** No session, or the session expired mid-request. */
    data class Unauthorized(val cause: Throwable? = null) : AppError()

    /**
     * The input itself was rejected. [fieldErrors] maps a form field key
     * (e.g. `"address"`, `"contactPhone"`) to the **backend AS-code**
     * responsible for that field being wrong -- not pre-resolved text.
     * Codes are the stable, localization-independent contract
     * (backend/docs/error-codes.md); resolving a code to a string only
     * happens at the UI edge, in [toUserMessage]/[messageFor], where a
     * [android.content.Context] is actually available. This backend
     * raises one error at a time rather than a structured batch of field
     * errors, so [fieldErrors] is realistically a single entry most of
     * the time -- still a map, so a future multi-field response (or a
     * client-side pre-submit check covering several fields at once)
     * fits without changing this shape.
     */
    data class Validation(
        val fieldErrors: Map<String, String>,
        val cause: Throwable? = null,
    ) : AppError()

    /** A documented backend business-rule code
     *  (backend/docs/error-codes.md) that isn't one of the more specific
     *  categories below. [message] is the raw backend text -- logging
     *  only, see the class doc; [toUserMessage] resolves [code] to a
     *  curated string instead of ever returning this directly. */
    data class Business(
        val code: String,
        val message: String,
        val cause: Throwable? = null,
    ) : AppError()

    /** The thing being requested doesn't exist (or isn't visible to this
     *  caller, which this backend's RLS makes indistinguishable from not
     *  existing at all -- see backend/docs/security-principles.md). */
    data class NotFound(
        val code: String? = null,
        val cause: Throwable? = null,
    ) : AppError()

    /** Someone else changed the thing first -- a slot just got booked, a
     *  duplicate booking, an overlapping availability window. */
    data class Conflict(
        val code: String? = null,
        val cause: Throwable? = null,
    ) : AppError()

    /** Anything that doesn't cleanly map to the above -- keep this one
     *  rare; a growing pile of `Unknown` means a real category is
     *  missing from this hierarchy or from the mapper. */
    data class Unknown(val cause: Throwable? = null) : AppError()
}
