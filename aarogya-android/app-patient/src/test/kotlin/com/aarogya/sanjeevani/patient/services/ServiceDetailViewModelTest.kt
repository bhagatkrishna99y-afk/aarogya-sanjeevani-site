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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")
private const val SERVICE_ID = "svc-checkup"

class ServiceDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var servicesRepository: FakeServicesRepository
    private lateinit var viewModel: ServiceDetailViewModel

    private val category = aCategory(id = "cat-general", name = "General")
    private val service = aService(id = SERVICE_ID, categoryId = "cat-general", name = "General Checkup")

    @Before
    fun setUp() {
        servicesRepository = FakeServicesRepository()
        servicesRepository.categories += category
        servicesRepository.services += service
        viewModel = ServiceDetailViewModel(servicesRepository)
    }

    @Test
    fun `resumed loads the service and resolves its category name`() =
        runTest {
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))

            val state = viewModel.uiState.value
            assertTrue(state is ServiceDetailUiState.Content)
            assertEquals(service, (state as ServiceDetailUiState.Content).service)
            assertEquals("General", state.categoryName)
            assertFalse(state.isUnavailable)
        }

    @Test
    fun `a first load for a service that isn't visible surfaces as Error, not Unavailable`() =
        runTest {
            viewModel.onAction(ServiceDetailAction.Resumed("no-such-service"))

            val state = viewModel.uiState.value
            assertTrue(state is ServiceDetailUiState.Error)
            assertTrue((state as ServiceDetailUiState.Error).error is AppError.NotFound)
        }

    @Test
    fun `a first load that fails outright surfaces as Error`() =
        runTest {
            servicesRepository.errorToReturn = AppError.Network()

            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))

            assertTrue(viewModel.uiState.value is ServiceDetailUiState.Error)
        }

    @Test
    fun `a service that disappears after being shown stays visible but flips isUnavailable`() =
        runTest {
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))
            assertFalse((viewModel.uiState.value as ServiceDetailUiState.Content).isUnavailable)

            servicesRepository.services.clear()
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))

            val state = viewModel.uiState.value as ServiceDetailUiState.Content
            assertTrue(state.isUnavailable)
            assertEquals(service, state.service)
        }

    @Test
    fun `a transient failure during a silent resume refresh never regresses away from Content`() =
        runTest {
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))

            servicesRepository.errorToReturn = AppError.Network()
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))

            val state = viewModel.uiState.value as ServiceDetailUiState.Content
            assertFalse(state.isUnavailable)
            assertEquals(service, state.service)
        }

    @Test
    fun `a service that becomes available again clears isUnavailable`() =
        runTest {
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))
            servicesRepository.services.clear()
            viewModel.onAction(ServiceDetailAction.Resumed(SERVICE_ID))
            assertTrue((viewModel.uiState.value as ServiceDetailUiState.Content).isUnavailable)

            servicesRepository.services += service
            viewModel.onAction(ServiceDetailAction.Retry)

            assertFalse((viewModel.uiState.value as ServiceDetailUiState.Content).isUnavailable)
        }

    private fun aCategory(
        id: String,
        name: String,
    ) = ServiceCategory(
        id = id,
        name = name,
        slug = name.lowercase(),
        description = null,
        icon = null,
        isActive = true,
        sortOrder = 0,
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
        description = "Full description",
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
