package com.aarogya.sanjeevani.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    private fun validate(
        fullName: String = "Jane Doe",
        email: String = "jane@example.com",
        password: String = "password123",
        confirmPassword: String = "password123",
    ) = AuthValidation.validateSignUp(fullName, email, password, confirmPassword)

    @Test
    fun `fully valid input has no field errors`() {
        assertTrue(validate().isEmpty())
    }

    @Test
    fun `full name shorter than 2 characters is rejected`() {
        val errors = validate(fullName = "J")
        assertEquals(SignUpFieldError.FULL_NAME_TOO_SHORT, errors["fullName"])
    }

    @Test
    fun `full name of exactly 2 characters is accepted`() {
        assertTrue(validate(fullName = "Jo").isEmpty())
    }

    @Test
    fun `blank full name is rejected`() {
        val errors = validate(fullName = "   ")
        assertEquals(SignUpFieldError.FULL_NAME_TOO_SHORT, errors["fullName"])
    }

    @Test
    fun `full name longer than 100 characters is rejected`() {
        val errors = validate(fullName = "A".repeat(101))
        assertEquals(SignUpFieldError.FULL_NAME_TOO_LONG, errors["fullName"])
    }

    @Test
    fun `full name of exactly 100 characters is accepted`() {
        assertTrue(validate(fullName = "A".repeat(100)).isEmpty())
    }

    @Test
    fun `email without an at sign is rejected`() {
        val errors = validate(email = "not-an-email")
        assertEquals(SignUpFieldError.EMAIL_INVALID, errors["email"])
    }

    @Test
    fun `email without a domain dot is rejected`() {
        val errors = validate(email = "jane@example")
        assertEquals(SignUpFieldError.EMAIL_INVALID, errors["email"])
    }

    @Test
    fun `email with whitespace is rejected`() {
        val errors = validate(email = "ja ne@example.com")
        assertEquals(SignUpFieldError.EMAIL_INVALID, errors["email"])
    }

    @Test
    fun `password shorter than 8 characters is rejected`() {
        val errors = validate(password = "short1", confirmPassword = "short1")
        assertEquals(SignUpFieldError.PASSWORD_TOO_SHORT, errors["password"])
    }

    @Test
    fun `password of exactly 8 characters is accepted`() {
        assertTrue(validate(password = "exactly8", confirmPassword = "exactly8").isEmpty())
    }

    @Test
    fun `mismatched confirm password is rejected`() {
        val errors = validate(password = "password123", confirmPassword = "password124")
        assertEquals(SignUpFieldError.PASSWORD_CONFIRMATION_MISMATCH, errors["confirmPassword"])
    }

    @Test
    fun `multiple independent problems are all reported together`() {
        val errors = validate(fullName = "J", email = "bad", password = "short", confirmPassword = "different")
        assertEquals(4, errors.size)
        assertEquals(SignUpFieldError.FULL_NAME_TOO_SHORT, errors["fullName"])
        assertEquals(SignUpFieldError.EMAIL_INVALID, errors["email"])
        assertEquals(SignUpFieldError.PASSWORD_TOO_SHORT, errors["password"])
        assertEquals(SignUpFieldError.PASSWORD_CONFIRMATION_MISMATCH, errors["confirmPassword"])
    }

    @Test
    fun `password strength is weak below the minimum length`() {
        assertEquals(PasswordStrength.WEAK, AuthValidation.passwordStrength("short1"))
    }

    @Test
    fun `password strength is weak for a long but single-character-class password`() {
        assertEquals(PasswordStrength.WEAK, AuthValidation.passwordStrength("alllowercase"))
    }

    @Test
    fun `password strength is medium with two character classes`() {
        assertEquals(PasswordStrength.MEDIUM, AuthValidation.passwordStrength("password123"))
    }

    @Test
    fun `password strength is strong with three classes and at least 12 characters`() {
        assertEquals(PasswordStrength.STRONG, AuthValidation.passwordStrength("Password123!"))
    }
}
