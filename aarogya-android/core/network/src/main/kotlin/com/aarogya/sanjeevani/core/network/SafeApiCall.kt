package com.aarogya.sanjeevani.core.network

import com.aarogya.sanjeevani.core.common.AppConstants
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

private const val MAX_ATTEMPTS = 3
private const val RETRY_BASE_DELAY_MILLIS = 250L

/**
 * Runs [block], returning its value as [AppResult.Success] or mapping
 * whatever it throws down to the closed [AppError] hierarchy (see
 * [toAppError]) -- the one place above :core:network that still knows
 * what a `RestException`/`IOException` is lives right here, in this
 * module; repositories only ever see [AppResult].
 *
 * **Structured cancellation**: a real [CancellationException] (the
 * calling coroutine scope was cancelled -- a ViewModel's `viewModelScope`
 * cleared, a `select`/`race` lost) is always rethrown, never swallowed
 * into an [AppResult.Failure] -- doing otherwise would break the calling
 * scope's ability to actually cancel. [TimeoutCancellationException]
 * (thrown by [withTimeout] below) is deliberately the **one** exception
 * to that: it *is-a* [CancellationException] (that's how `withTimeout`
 * cancels the block's own coroutine), but from this function's caller's
 * perspective it's not a cancellation at all -- it's a timed-out network
 * call, reported as [AppError.Network] like any other connectivity
 * failure, not propagated as if the caller's own scope had been
 * cancelled. The `catch` order below (more specific before more general)
 * is what makes that distinction possible; swapping it would be a
 * compile error anyway (an unreachable catch block), but worth stating
 * why the order is load-bearing, not incidental.
 *
 * **Retry**: off by default -- a write should never be silently retried
 * (a `create_service_request` retried after a timeout could double-book
 * a slot). Pass [retryable] = true only for idempotent reads, where a
 * transient connectivity blip is worth a bounded, backed-off retry
 * ([MAX_ATTEMPTS] total attempts). Only [AppError.Network] is retried --
 * a business rejection (wrong input, not authorized, ...) will reject
 * the exact same way on attempt two, so retrying it would just add
 * latency for no benefit.
 */
@Suppress("TooGenericExceptionCaught") // deliberate: this is the one place that maps *any* throwable to AppError
suspend fun <T> safeApiCall(
    retryable: Boolean = false,
    timeoutMillis: Long = AppConstants.NETWORK_TIMEOUT_MILLIS,
    block: suspend () -> T,
): AppResult<T> {
    val maxAttempts = if (retryable) MAX_ATTEMPTS else 1

    for (attempt in 0 until maxAttempts) {
        val error =
            try {
                return AppResult.Success(withTimeout(timeoutMillis) { block() })
            } catch (e: TimeoutCancellationException) {
                AppError.Network(isTimeout = true, cause = e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.toAppError()
            }

        val isLastAttempt = attempt == maxAttempts - 1
        if (isLastAttempt || error !is AppError.Network) {
            return AppResult.Failure(error)
        }
        delay(RETRY_BASE_DELAY_MILLIS * (1L shl attempt))
    }

    error("unreachable: the loop above always returns on its last attempt")
}
