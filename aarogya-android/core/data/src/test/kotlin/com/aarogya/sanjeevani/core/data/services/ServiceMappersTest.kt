package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

private val CREATED = Instant.parse("2026-01-01T00:00:00Z")
private val UPDATED = Instant.parse("2026-01-02T00:00:00Z")

/**
 * [ServiceCategoryDto.toDomain]/[ServiceDto.toDomain] are plain field-
 * for-field mappers -- every field here is set to a distinct,
 * recognizable value so a future rename/drop/mismatch (not caught by
 * the compiler, since both sides are just `String`/`Int`/etc.) fails
 * loudly here instead of silently in production.
 */
class ServiceMappersTest {
    @Test
    fun `ServiceCategoryDto maps every field across unchanged`() {
        val dto =
            ServiceCategoryDto(
                id = "cat-1",
                name = "Dermatology",
                slug = "dermatology",
                description = "Skin care",
                icon = "skin_icon",
                isActive = true,
                sortOrder = 3,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals("cat-1", domain.id)
        assertEquals("Dermatology", domain.name)
        assertEquals("dermatology", domain.slug)
        assertEquals("Skin care", domain.description)
        assertEquals("skin_icon", domain.icon)
        assertEquals(true, domain.isActive)
        assertEquals(3, domain.sortOrder)
        assertEquals(CREATED, domain.createdAt)
        assertEquals(UPDATED, domain.updatedAt)
    }

    @Test
    fun `ServiceCategoryDto with null optional fields maps them through as null`() {
        val dto =
            ServiceCategoryDto(
                id = "cat-1",
                name = "Dermatology",
                slug = "dermatology",
                description = null,
                icon = null,
                isActive = false,
                sortOrder = 0,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals(null, domain.description)
        assertEquals(null, domain.icon)
        assertEquals(false, domain.isActive)
    }

    @Test
    fun `ServiceDto maps every field across unchanged, including supportedModes`() {
        val dto =
            ServiceDto(
                id = "svc-1",
                categoryId = "cat-1",
                name = "General Checkup",
                slug = "general-checkup",
                description = "A full checkup",
                shortDescription = "Checkup",
                requiresDoctor = true,
                durationMinutes = 30,
                supportedModes = listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE),
                metadata = JsonPrimitive("extra"),
                isActive = true,
                sortOrder = 5,
                createdAt = CREATED,
                updatedAt = UPDATED,
            )

        val domain = dto.toDomain()

        assertEquals("svc-1", domain.id)
        assertEquals("cat-1", domain.categoryId)
        assertEquals("General Checkup", domain.name)
        assertEquals("general-checkup", domain.slug)
        assertEquals("A full checkup", domain.description)
        assertEquals("Checkup", domain.shortDescription)
        assertEquals(true, domain.requiresDoctor)
        assertEquals(30, domain.durationMinutes)
        assertEquals(listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE), domain.supportedModes)
        assertEquals(JsonPrimitive("extra"), domain.metadata)
        assertEquals(true, domain.isActive)
        assertEquals(5, domain.sortOrder)
        assertEquals(CREATED, domain.createdAt)
        assertEquals(UPDATED, domain.updatedAt)
    }
}
