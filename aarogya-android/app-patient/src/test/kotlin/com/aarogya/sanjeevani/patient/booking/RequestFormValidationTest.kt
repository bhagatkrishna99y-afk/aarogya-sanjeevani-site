package com.aarogya.sanjeevani.patient.booking

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestFormValidationTest {
    @Test
    fun `a plausible phone number with a country code passes`() {
        assertTrue(RequestFormValidation.isPhoneValid("+91 98765 43210"))
    }

    @Test
    fun `a bare 10-digit phone number passes`() {
        assertTrue(RequestFormValidation.isPhoneValid("9876543210"))
    }

    @Test
    fun `too few digits fails`() {
        assertFalse(RequestFormValidation.isPhoneValid("12345"))
    }

    @Test
    fun `letters fail`() {
        assertFalse(RequestFormValidation.isPhoneValid("98765abcde"))
    }

    @Test
    fun `blank fails`() {
        assertFalse(RequestFormValidation.isPhoneValid(""))
    }

    @Test
    fun `home visit without an address is flagged, every other mode is not`() {
        val withHomeVisit = RequestFormValidation.validate(ServiceMode.HOME_VISIT, "9876543210", "", "")
        val withInPerson = RequestFormValidation.validate(ServiceMode.IN_PERSON, "9876543210", "", "")

        assertEquals(RequestFormFieldError.ADDRESS_REQUIRED, withHomeVisit["address"])
        assertFalse(withInPerson.containsKey("address"))
    }

    @Test
    fun `a home visit with an address is not flagged`() {
        val errors = RequestFormValidation.validate(ServiceMode.HOME_VISIT, "9876543210", "221B Baker Street", "")
        assertFalse(errors.containsKey("address"))
    }

    @Test
    fun `notes right at the limit are fine, one over is flagged`() {
        val atLimit = "a".repeat(RequestFormValidation.NOTES_MAX_LENGTH)
        val overLimit = "a".repeat(RequestFormValidation.NOTES_MAX_LENGTH + 1)

        assertFalse(RequestFormValidation.validate(null, "9876543210", "", atLimit).containsKey("notes"))
        assertEquals(
            RequestFormFieldError.NOTES_TOO_LONG,
            RequestFormValidation.validate(null, "9876543210", "", overLimit)["notes"],
        )
    }

    @Test
    fun `an invalid phone is flagged regardless of mode`() {
        val errors = RequestFormValidation.validate(ServiceMode.ONLINE, "abc", "", "")
        assertEquals(RequestFormFieldError.CONTACT_PHONE_INVALID, errors["contactPhone"])
    }

    @Test
    fun `everything valid yields no errors at all`() {
        val errors = RequestFormValidation.validate(ServiceMode.IN_PERSON, "9876543210", "", "Please call ahead.")
        assertTrue(errors.isEmpty())
    }
}
