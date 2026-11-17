package com.aarogya.sanjeevani.core.common.result

import com.aarogya.sanjeevani.core.common.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun `asSuccess wraps the value`() {
        val result = "patient-42".asSuccess()

        assertTrue(result is AppResult.Success)
        assertEquals("patient-42", (result as AppResult.Success).data)
    }

    @Test
    fun `asError wraps the AppError as a Failure`() {
        val error = AppError.NotFound()
        val result = error.asError()

        assertTrue(result is AppResult.Failure)
        assertEquals(error, (result as AppResult.Failure).error)
    }

    @Test
    fun `map transforms a Success without touching Failure`() {
        val success: AppResult<Int> = 2.asSuccess()
        val failure: AppResult<Int> = AppError.NotFound().asError()

        assertEquals(4.asSuccess(), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `onSuccess and onError only fire for their own branch, for a Success`() {
        var successSeen = false
        var errorSeen = false

        1.asSuccess()
            .onSuccess { successSeen = true }
            .onError { errorSeen = true }

        assertTrue(successSeen)
        assertFalse(errorSeen)
    }

    @Test
    fun `onSuccess and onError only fire for their own branch, for a Failure`() {
        var successSeen = false
        var errorSeen: AppError? = null

        AppError.Network().asError()
            .onSuccess { successSeen = true }
            .onError { errorSeen = it }

        assertFalse(successSeen)
        assertEquals(AppError.Network(), errorSeen)
    }

    @Test
    fun `onSuccess and onError both return the original result for chaining`() {
        val success: AppResult<Int> = 5.asSuccess()

        val returned = success.onSuccess { }.onError { }

        assertEquals(success, returned)
    }
}
