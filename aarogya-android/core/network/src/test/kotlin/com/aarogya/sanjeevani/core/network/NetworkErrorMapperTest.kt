package com.aarogya.sanjeevani.core.network

import com.aarogya.sanjeevani.core.common.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One assertion per code in backend/docs/error-codes.md's AS000-AS035
 * registry, grouped by the [AppError] category each maps to -- every
 * single code is exercised, just not as 36 separate `@Test` methods.
 * [appErrorForBackendText] takes the raw backend text directly (no
 * `RestException`/`HttpResponse` to construct), matching exactly what
 * `RAISE EXCEPTION '<CODE>: message'` actually produces.
 */
class NetworkErrorMapperTest {
    private fun textFor(code: String) = "$code: some backend-provided English text"

    @Test
    fun `AS000 maps to Unauthorized`() {
        val error = appErrorForBackendText(textFor("AS000_NOT_AUTHENTICATED"))
        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `every NotFound code maps to NotFound with its own code preserved`() {
        val codes =
            listOf(
                "AS015_REQUEST_NOT_FOUND",
                "AS028_DOCTOR_NOT_FOUND",
                "AS030_USER_NOT_FOUND",
                "AS033_CATALOG_ENTITY_NOT_FOUND",
            )
        for (code in codes) {
            val error = appErrorForBackendText(textFor(code))
            assertEquals("code=$code", AppError.NotFound(code = code), error)
        }
    }

    @Test
    fun `every Conflict code maps to Conflict with its own code preserved`() {
        val codes =
            listOf(
                "AS001_SLOT_UNAVAILABLE",
                "AS019_SLOT_ALREADY_BOOKED",
                "AS010_DUPLICATE_REQUEST",
            )
        for (code in codes) {
            val error = appErrorForBackendText(textFor(code))
            assertEquals("code=$code", AppError.Conflict(code = code), error)
        }
    }

    @Test
    fun `every Validation code maps to its documented form field`() {
        val codeToField =
            mapOf(
                "AS011_ADDRESS_REQUIRED" to "address",
                "AS012_INVALID_CONTACT_PHONE" to "contactPhone",
                "AS013_TEXT_TOO_LONG" to "notes",
                "AS023_INVALID_DOCTOR_PROFILE_FIELDS" to "profile",
                "AS025_INVALID_AVAILABILITY_WINDOW" to "availability",
                "AS026_AVAILABILITY_OVERLAP" to "availability",
                "AS032_INVALID_CATALOG_FIELDS" to "catalog",
            )
        for ((code, field) in codeToField) {
            val error = appErrorForBackendText(textFor(code))
            assertEquals("code=$code", AppError.Validation(fieldErrors = mapOf(field to code)), error)
        }
    }

    @Test
    fun `every other documented code maps to Business with its code and raw text preserved`() {
        val codes =
            listOf(
                "AS002_DOCTOR_NOT_AVAILABLE",
                "AS003_INVALID_DATE",
                "AS004_ROLE_NOT_PATIENT",
                "AS005_ACCOUNT_SUSPENDED",
                "AS006_SERVICE_UNAVAILABLE",
                "AS007_DOCTOR_REQUIRED",
                "AS008_DOCTOR_NOT_OFFERED",
                "AS009_MODE_UNSUPPORTED",
                "AS014_DOCTOR_NOT_ALLOWED",
                "AS016_INVALID_TRANSITION",
                "AS017_NOT_AUTHORIZED_FOR_TRANSITION",
                "AS018_DOCTOR_NOT_APPROVED",
                "AS020_TOO_EARLY_TO_COMPLETE",
                "AS021_CANCELLATION_NOTICE_WINDOW",
                "AS022_NOT_A_DOCTOR_ACCOUNT",
                "AS024_ONLY_APPROVED_DOCTORS",
                "AS027_NOT_AUTHORIZED",
                "AS029_INVALID_DOCTOR_STATUS_TRANSITION",
                "AS031_CANNOT_DEMOTE_LAST_ADMIN",
                "AS034_TOO_MANY_ACTIVE_REQUESTS",
                "AS035_RATE_LIMITED",
            )
        for (code in codes) {
            val text = textFor(code)
            val error = appErrorForBackendText(text)
            assertEquals("code=$code", AppError.Business(code = code, message = text), error)
        }
    }

    @Test
    fun `text with no AS-code prefix is not matched by the generic mapper`() {
        // backend/docs/client-contract.md §12's set_doctor_services case --
        // deliberately NOT guessed at generically here, see the comment on
        // Throwable#toAppError; only that one call site may interpret an
        // unprefixed error.
        assertNull(appErrorForBackendText("Only approved doctors can configure services"))
    }

    @Test
    fun `malformed or unknown-shaped text is not matched`() {
        assertNull(appErrorForBackendText("not a code at all"))
        assertNull(appErrorForBackendText("AS1_TOO_SHORT: bad format"))
    }
}
