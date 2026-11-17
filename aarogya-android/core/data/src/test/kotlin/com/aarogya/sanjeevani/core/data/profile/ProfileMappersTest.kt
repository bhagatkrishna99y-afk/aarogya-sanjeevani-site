package com.aarogya.sanjeevani.core.data.profile

import com.aarogya.sanjeevani.core.data.model.UserRole
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val CREATED = Instant.parse("2026-01-01T00:00:00Z")
private val UPDATED = Instant.parse("2026-01-02T00:00:00Z")

/** [ProfileDto.toDomain] is a plain field-for-field mapper -- every
 *  field here is set to a distinct, recognizable value so a future
 *  dropped/renamed field fails this test instead of silently reaching
 *  production. */
class ProfileMappersTest {
    @Test
    fun `maps every field across unchanged for a fully-filled-in profile`() {
        val dto =
            ProfileDto(
                id = "user-1",
                role = UserRole.USER,
                fullName = "Jane Doe",
                phone = "9876543210",
                email = "jane@example.com",
                avatarUrl = "user-1/avatar.jpg",
                isSuspended = false,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals("user-1", domain.id)
        assertEquals(UserRole.USER, domain.role)
        assertEquals("Jane Doe", domain.fullName)
        assertEquals("9876543210", domain.phone)
        assertEquals("jane@example.com", domain.email)
        assertEquals("user-1/avatar.jpg", domain.avatarUrl)
        assertEquals(false, domain.isSuspended)
        assertEquals(CREATED, domain.createdAt)
        assertEquals(UPDATED, domain.updatedAt)
    }

    @Test
    fun `maps a profile with no name, phone or avatar yet to nulls`() {
        val dto =
            ProfileDto(
                id = "user-1",
                role = UserRole.USER,
                fullName = null,
                phone = null,
                email = "jane@example.com",
                avatarUrl = null,
                isSuspended = true,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertNull(domain.fullName)
        assertNull(domain.phone)
        assertNull(domain.avatarUrl)
        assertEquals(true, domain.isSuspended)
    }
}
