package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

class ServicesGroupingTest {
    private val generalCategory = aCategory(id = "cat-general", name = "General", sortOrder = 1)
    private val labCategory = aCategory(id = "cat-lab", name = "Lab Tests", sortOrder = 0)

    private val checkup =
        aService(id = "svc-checkup", categoryId = "cat-general", name = "General Checkup", sortOrder = 1)
    private val nursing =
        aService(id = "svc-nursing", categoryId = "cat-general", name = "Home Nursing", sortOrder = 0)
    private val bloodTest = aService(id = "svc-blood", categoryId = "cat-lab", name = "Blood Test", sortOrder = 0)

    private val allCategories = listOf(generalCategory, labCategory)
    private val allServices = listOf(checkup, nursing, bloodTest)

    @Test
    fun `groups services under their category, categories sorted by sortOrder`() {
        val groups = groupServicesForDisplay(allCategories, allServices, selectedCategoryId = null, searchQuery = "")

        assertEquals(listOf(labCategory, generalCategory), groups.map { it.category })
    }

    @Test
    fun `each group's services are sorted by their own sortOrder`() {
        val groups = groupServicesForDisplay(allCategories, allServices, selectedCategoryId = null, searchQuery = "")

        val generalGroup = groups.first { it.category == generalCategory }
        assertEquals(listOf(nursing, checkup), generalGroup.services)
    }

    @Test
    fun `a blank query matches every service`() {
        val groups = groupServicesForDisplay(allCategories, allServices, selectedCategoryId = null, searchQuery = "   ")

        assertEquals(3, groups.sumOf { it.services.size })
    }

    @Test
    fun `search matches by name, case-insensitively and as a substring`() {
        val groups =
            groupServicesForDisplay(allCategories, allServices, selectedCategoryId = null, searchQuery = "blood")

        assertEquals(1, groups.size)
        assertEquals(listOf(bloodTest), groups.single().services)
    }

    @Test
    fun `a category with no matching services after search is dropped, not left empty`() {
        val groups =
            groupServicesForDisplay(allCategories, allServices, selectedCategoryId = null, searchQuery = "nursing")

        assertEquals(1, groups.size)
        assertEquals(generalCategory, groups.single().category)
        assertEquals(listOf(nursing), groups.single().services)
    }

    @Test
    fun `selecting a category restricts results to just that category, ignoring the others`() {
        val groups =
            groupServicesForDisplay(allCategories, allServices, selectedCategoryId = "cat-lab", searchQuery = "")

        assertEquals(1, groups.size)
        assertEquals(labCategory, groups.single().category)
    }

    @Test
    fun `category filter and search apply together`() {
        val groups =
            groupServicesForDisplay(
                allCategories,
                allServices,
                selectedCategoryId = "cat-general",
                searchQuery = "blood",
            )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `no services anywhere yields no groups`() {
        val groups = groupServicesForDisplay(allCategories, emptyList(), selectedCategoryId = null, searchQuery = "")

        assertTrue(groups.isEmpty())
    }

    private fun aCategory(
        id: String,
        name: String,
        sortOrder: Int,
    ) = ServiceCategory(
        id = id,
        name = name,
        slug = name.lowercase(),
        description = null,
        icon = null,
        isActive = true,
        sortOrder = sortOrder,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )

    private fun aService(
        id: String,
        categoryId: String,
        name: String,
        sortOrder: Int,
    ) = Service(
        id = id,
        categoryId = categoryId,
        name = name,
        slug = name.lowercase(),
        description = null,
        shortDescription = null,
        requiresDoctor = false,
        durationMinutes = 30,
        supportedModes = listOf(ServiceMode.IN_PERSON),
        metadata = null,
        isActive = true,
        sortOrder = sortOrder,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )
}
