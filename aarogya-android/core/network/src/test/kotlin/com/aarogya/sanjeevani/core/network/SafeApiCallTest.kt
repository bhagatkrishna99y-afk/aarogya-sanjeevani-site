package com.aarogya.sanjeevani.core.network

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SafeApiCallTest {
    @Test
    fun `a successful block returns Success`() =
        runTest {
            val result = safeApiCall { "patient-42" }

            assertEquals(AppResult.Success("patient-42"), result)
        }

    @Test
    fun `IOException maps to a Network failure`() =
        runTest {
            val result = safeApiCall { throw IOException("no route to host") }

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
        }

    @Test
    fun `a write (not retryable) is never retried, even on a transient failure`() =
        runTest {
            var calls = 0
            val result =
                safeApiCall(retryable = false) {
                    calls++
                    throw IOException("transient")
                }

            assertEquals(1, calls)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
        }

    @Test
    fun `a retryable read retries on Network failure and eventually succeeds`() =
        runTest {
            var calls = 0
            val result =
                safeApiCall(retryable = true) {
                    calls++
                    if (calls < 2) throw IOException("transient") else "ok"
                }

            assertEquals(AppResult.Success("ok"), result)
            assertEquals(2, calls)
        }

    @Test
    fun `a retryable read gives up after its bounded number of attempts`() =
        runTest {
            var calls = 0
            val result =
                safeApiCall(retryable = true) {
                    calls++
                    throw IOException("still down")
                }

            assertTrue((result as AppResult.Failure).error is AppError.Network)
            assertEquals(3, calls) // MAX_ATTEMPTS in SafeApiCall.kt
        }

    @Test
    fun `a retryable read does not retry a non-Network error`() =
        runTest {
            var calls = 0
            val result =
                safeApiCall(retryable = true) {
                    calls++
                    error("not a connectivity problem")
                }

            assertEquals(1, calls)
            assertTrue((result as AppResult.Failure).error is AppError.Unknown)
        }

    @Suppress("SwallowedException") // deliberately not inspected -- only propagation itself is under test
    @Test
    fun `a real CancellationException is rethrown, never swallowed into a Failure`() =
        runTest {
            var threw = false
            try {
                safeApiCall { throw CancellationException("scope cancelled") }
            } catch (e: CancellationException) {
                threw = true
            }

            assertTrue("CancellationException must propagate, not become an AppResult.Failure", threw)
        }

    @Suppress("SwallowedException") // deliberately not inspected -- the test asserts it must NOT reach here at all
    @Test
    fun `a timeout is reported as AppError Network, not rethrown as a cancellation`() =
        runTest {
            var caughtCancellation = false
            val result =
                try {
                    safeApiCall(timeoutMillis = 10L) {
                        delay(10_000L)
                        "too slow"
                    }
                } catch (e: CancellationException) {
                    caughtCancellation = true
                    fail("the timeout must be reported as AppResult.Failure, not rethrown")
                    return@runTest
                }

            assertTrue(!caughtCancellation)
            assertTrue(result is AppResult.Failure)
            val error = (result as AppResult.Failure).error
            assertTrue(error is AppError.Network)
            assertTrue((error as AppError.Network).isTimeout)
        }
}
