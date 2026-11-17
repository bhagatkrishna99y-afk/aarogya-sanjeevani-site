package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val PAGE_SIZE = 20

class DoctorsDirectoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var viewModel: DoctorsDirectoryViewModel

    @Before
    fun setUp() {
        doctorsRepository = FakeDoctorsRepository()
        viewModel = DoctorsDirectoryViewModel(doctorsRepository)
    }

    @Test
    fun `initialize loads the first page, capped at the page size`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 25)

            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(PAGE_SIZE, content.doctors.size)
            assertTrue(content.hasMore)
            assertFalse(content.isLoadingMore)
        }

    @Test
    fun `load more appends the next page to the list already on screen`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 25)
            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            viewModel.onAction(DoctorsDirectoryAction.LoadMore)

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(25, content.doctors.size)
            assertFalse(content.hasMore)
            assertFalse(content.isLoadingMore)
        }

    @Test
    fun `a page shorter than the page size means there's nothing more to load`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 7)

            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(7, content.doctors.size)
            assertFalse(content.hasMore)
        }

    @Test
    fun `load more is a no-op once there's nothing left to load`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 7)
            viewModel.onAction(DoctorsDirectoryAction.Initialize)
            val before = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content

            viewModel.onAction(DoctorsDirectoryAction.LoadMore)

            val after = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(before.doctors, after.doctors)
        }

    @Test
    fun `a load-more failure keeps the doctors already on screen and reports a load-more error`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 25)
            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            doctorsRepository.errorToReturn = AppError.Network()
            viewModel.onAction(DoctorsDirectoryAction.LoadMore)

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(PAGE_SIZE, content.doctors.size)
            assertTrue(content.hasMore)
            assertFalse(content.isLoadingMore)
            assertTrue(content.loadMoreError is AppError.Network)
        }

    @Test
    fun `retrying a failed load-more clears the error and appends the next page`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 25)
            viewModel.onAction(DoctorsDirectoryAction.Initialize)
            doctorsRepository.errorToReturn = AppError.Network()
            viewModel.onAction(DoctorsDirectoryAction.LoadMore)

            doctorsRepository.errorToReturn = null
            viewModel.onAction(DoctorsDirectoryAction.RetryLoadMore)

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(25, content.doctors.size)
            assertEquals(null, content.loadMoreError)
        }

    @Test
    fun `an initial load failure surfaces as a full-screen error`() =
        runTest {
            doctorsRepository.errorToReturn = AppError.Unknown()

            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            assertTrue(viewModel.uiState.value.section is DoctorsDirectorySectionState.Error)
        }

    @Test
    fun `selecting a specialization filter restarts pagination from page 1 with that filter applied`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 25, specialization = "General")
            doctorsRepository.doctors += aDoctor(id = "cardio-1", name = "Dr. Cardio", specialization = "Cardiology")
            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            viewModel.onAction(DoctorsDirectoryAction.SpecializationSelected("Cardiology"))

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(1, content.doctors.size)
            assertEquals("cardio-1", content.doctors.single().doctorId)
            assertFalse(content.hasMore)
        }

    @Test
    fun `selecting a mode filter restarts pagination from page 1 with that filter applied`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 5, mode = ServiceMode.IN_PERSON)
            doctorsRepository.doctors += aDoctor(id = "online-1", name = "Dr. Online", mode = ServiceMode.ONLINE)
            viewModel.onAction(DoctorsDirectoryAction.Initialize)

            viewModel.onAction(DoctorsDirectoryAction.ModeSelected(ServiceMode.ONLINE))

            val content = viewModel.uiState.value.section as DoctorsDirectorySectionState.Content
            assertEquals(listOf("online-1"), content.doctors.map { it.doctorId })
        }

    @Test
    fun `specializations seen across loaded pages accumulate in the filter chip set`() =
        runTest {
            doctorsRepository.doctors += seedDoctors(count = 20, specialization = "General")
            doctorsRepository.doctors += aDoctor(id = "derma-1", name = "Dr. Derma", specialization = "Dermatology")
            viewModel.onAction(DoctorsDirectoryAction.Initialize)
            assertEquals(setOf("General"), viewModel.uiState.value.availableSpecializations)

            viewModel.onAction(DoctorsDirectoryAction.LoadMore)

            assertEquals(setOf("General", "Dermatology"), viewModel.uiState.value.availableSpecializations)
        }

    private fun seedDoctors(
        count: Int,
        specialization: String = "General",
        mode: ServiceMode = ServiceMode.IN_PERSON,
    ): List<DoctorPublic> =
        (1..count).map { index ->
            aDoctor(id = "doc-$index", name = "Doctor $index", specialization = specialization, mode = mode)
        }

    private fun aDoctor(
        id: String,
        name: String,
        specialization: String = "General",
        mode: ServiceMode = ServiceMode.IN_PERSON,
    ) = DoctorPublic(
        doctorId = id,
        fullName = name,
        title = "Dr.",
        specialization = specialization,
        qualifications = null,
        bio = null,
        yearsOfExperience = 5,
        consultationModes = listOf(mode),
        acceptingRequests = true,
        avatarPath = null,
    )
}
