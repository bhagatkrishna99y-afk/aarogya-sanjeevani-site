package com.aarogya.sanjeevani.patient.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direct tests of the pure [EditProfileValidation.validate] function --
 * [EditProfileViewModelTest] already covers it indirectly through the
 * ViewModel, but a dedicated test here is cheaper to run and exercises
 * the boundary cases exhaustively without any coroutine/ViewModel setup.
 */
class EditProfileValidationTest {
    @Test
    fun `a name right at the minimum length is valid`() {
        val errors = EditProfileValidation.validate(fullName = "Jo", phone = "")

        assertNull(errors["fullName"])
    }

    @Test
    fun `a name one character under the minimum is flagged too short`() {
        val errors = EditProfileValidation.validate(fullName = "J", phone = "")

        assertEquals(EditProfileFieldError.FULL_NAME_TOO_SHORT, errors["fullName"])
    }

    @Test
    fun `a name right at the maximum length is valid`() {
        val errors = EditProfileValidation.validate(fullName = "A".repeat(100), phone = "")

        assertNull(errors["fullName"])
    }

    @Test
    fun `a name one character over the maximum is flagged too long`() {
        val errors = EditProfileValidation.validate(fullName = "A".repeat(101), phone = "")

        assertEquals(EditProfileFieldError.FULL_NAME_TOO_LONG, errors["fullName"])
    }

    @Test
    fun `leading and trailing whitespace is trimmed before checking length`() {
        val errors = EditProfileValidation.validate(fullName = "  Jo  ", phone = "")

        assertNull(errors["fullName"])
    }

    @Test
    fun `a blank phone is valid -- phone is optional here, unlike at sign-up`() {
        val errors = EditProfileValidation.validate(fullName = "Jane Doe", phone = "")

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `whitespace-only phone counts as blank`() {
        val errors = EditProfileValidation.validate(fullName = "Jane Doe", phone = "   ")

        assertNull(errors["phone"])
    }

    @Test
    fun `a non-blank but malformed phone is flagged`() {
        val errors = EditProfileValidation.validate(fullName = "Jane Doe", phone = "abc")

        assertEquals(EditProfileFieldError.PHONE_INVALID, errors["phone"])
    }

    @Test
    fun `a well-formed phone is valid`() {
        val errors = EditProfileValidation.validate(fullName = "Jane Doe", phone = "9876543210")

        assertNull(errors["phone"])
    }

    @Test
    fun `a fully valid name and phone produce no errors at all`() {
        val errors = EditProfileValidation.validate(fullName = "Jane Doe", phone = "9876543210")

        assertTrue(errors.isEmpty())
    }
}
