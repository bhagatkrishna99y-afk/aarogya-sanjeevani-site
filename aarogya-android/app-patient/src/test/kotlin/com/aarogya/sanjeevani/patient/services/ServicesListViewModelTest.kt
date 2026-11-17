package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeServicesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

class ServicesListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var servicesRepository: FakeServicesRepository
    private lateinit var viewModel: ServicesListViewModel

    private val generalCategory = aCategory(id = "cat-general", name = "General", sortOrder = 0)
    private val labCategory = aCategory(id = "cat-lab", name = "Lab Tests", sortOrder = 1)
    private val checkup = aService(id = "svc-checkup", categoryId = "cat-general", name = "General Checkup")
    private val bloodTest = aService(id = "svc-blood", categoryId = "cat-lab", name = "Blood Test")

    @Before
    fun setUp() {
        servicesRepository = FakeServicesRepository()
        servicesRepository.categories += listOf(generalCategory, labCategory)
        servicesRepository.services += listOf(checkup, bloodTest)
        viewModel = ServicesListViewModel(servicesRepository)
    }

    @Test
    fun `initialize loads categories and services and groups everything unfiltered`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onAction(ServicesListAction.Initialize(categoryId = null))

            val state = viewModel.uiState.value
            assertTrue(state.section is ServicesListSectionState.Success)
            assertEquals(2, state.groups.size)
            assertEquals(2, state.groups.sumOf { it.services.size })
        }

    @Test
    fun `initialize with a categoryId pre-selects that category's filter`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onAction(ServicesListAction.Initialize(categoryId = "cat-lab"))

            val state = viewModel.uiState.value
            assertEquals("cat-lab", state.selectedCategoryId)
            assertEquals(listOf(labCategory), state.groups.map { it.category })
        }

    @Test
    fun `a load failure surfaces as Error`() =
        runTest(mainDispatcherRule.testDispatcher) {
            servicesRepository.errorToReturn = AppError.Unknown()

            viewModel.onAction(ServicesListAction.Initialize(categoryId = null))

            assertTrue(viewModel.uiState.value.section is ServicesListSectionState.Error)
        }

    @Test
    fun `retry after the error is fixed succeeds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            servicesRepository.errorToReturn = AppError.Unknown()
            viewModel.onAction(ServicesListAction.Initialize(categoryId = null))
            assertTrue(viewModel.uiState.value.section is ServicesListSectionState.Error)

            servicesRepository.errorToReturn = null
            viewModel.onAction(ServicesListAction.Retry)

            assertTrue(viewModel.uiState.value.section is ServicesListSectionState.Success)
        }

    @Test
    fun `selecting a category filters groups immediately, with no debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onAction(ServicesListAction.Initialize(categoryId = null))

            viewModel.onAction(ServicesListAction.CategorySelected("cat-general"))

            assertEquals(listOf(generalCategory), viewModel.uiState.value.groups.map { it.category })
        }

    @Test
    fun `typing a search query updates the field immediately but only filters groups after the debounce`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onAction(ServicesListAction.Initialize(categoryId = null))

            viewModel.onAction(ServicesListAction.SearchQueryChanged("blood"))

            // Immediate: the field's own displayed value.
            assertEquals("blood", viewModel.uiState.value.searchQuery)
            // Not yet: the debounce window hasn't elapsed.
            assertEquals(2, viewModel.uiState.value.groups.sumOf { it.services.size })

            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(bloodTest), viewModel.uiState.value.groups.single().services)
        }

    @Test
    fun `clearing filters resets both search and category and shows everything again`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.onAction(ServicesListAction.Initialize(categoryId = "cat-lab"))
            viewModel.onAction(ServicesListAction.SearchQueryChanged("blood"))
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAction(ServicesListAction.ClearFilters)

            val state = viewModel.uiState.value
            assertEquals("", state.searchQuery)
            assertEquals(null, state.selectedCategoryId)
            assertEquals(2, state.groups.sumOf { it.services.size })
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
    ) = Service(
        id = id,
        categoryId = categoryId,
        name = name,
        slug = name.lowercase(),
        description = null,
        shortDescription = "Short description",
        requiresDoctor = false,
        durationMinutes = 30,
        supportedModes = listOf(ServiceMode.IN_PERSON),
        metadata = null,
        isActive = true,
        sortOrder = 0,
        createdAt = SOME_INSTANT,
        updatedAt = SOME_INSTANT,
    )
}
