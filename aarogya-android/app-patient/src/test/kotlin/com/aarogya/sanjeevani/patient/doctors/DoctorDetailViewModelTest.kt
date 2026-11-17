package com.aarogya.sanjeevani.patient.doctors

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeDoctorsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val DOCTOR_ID = "doc-1"

class DoctorDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var viewModel: DoctorDetailViewModel

    @Before
    fun setUp() {
        doctorsRepository = FakeDoctorsRepository()
        viewModel = DoctorDetailViewModel(doctorsRepository)
    }

    @Test
    fun `loads the doctor and their offered services`() =
        runTest {
            doctorsRepository.doctors += aDoctor()
            doctorsRepository.doctorServices += aPairing()

            viewModel.onAction(DoctorDetailAction.Load(DOCTOR_ID))

            val content = viewModel.uiState.value as DoctorDetailUiState.Content
            assertEquals(DOCTOR_ID, content.doctor.doctorId)
            assertEquals(listOf("svc-1"), content.services.map { it.serviceId })
        }

    @Test
    fun `a doctor that's gone by the time this screen loads sends DoctorUnavailable, not an Error`() =
        runTest {
            // doctorsRepository.doctors left empty -- mirrors a stale
            // directory snapshot: the doctor the patient tapped is no
            // longer visible (suspended/no longer approved) by the time
            // this screen's own, independent load runs.
            viewModel.uiEvent.test {
                viewModel.onAction(DoctorDetailAction.Load(DOCTOR_ID))
                assertEquals(DoctorDetailUiEvent.DoctorUnavailable, awaitItem())
            }
            assertTrue(viewModel.uiState.value is DoctorDetailUiState.Loading)
        }

    @Test
    fun `any other load failure surfaces as a full-screen error, not DoctorUnavailable`() =
        runTest {
            doctorsRepository.errorToReturn = AppError.Network()

            viewModel.uiEvent.test {
                viewModel.onAction(DoctorDetailAction.Load(DOCTOR_ID))
                expectNoEvents()
            }
            assertTrue(viewModel.uiState.value is DoctorDetailUiState.Error)
        }

    @Test
    fun `a doctor who offers no services yet still loads, with an empty services list`() =
        runTest {
            doctorsRepository.doctors += aDoctor()

            viewModel.onAction(DoctorDetailAction.Load(DOCTOR_ID))

            val content = viewModel.uiState.value as DoctorDetailUiState.Content
            assertEquals(emptyList<DoctorServicePublic>(), content.services)
        }

    @Test
    fun `retry reuses the doctor id from the original load`() =
        runTest {
            doctorsRepository.errorToReturn = AppError.Network()
            viewModel.onAction(DoctorDetailAction.Load(DOCTOR_ID))
            doctorsRepository.doctors += aDoctor()

            doctorsRepository.errorToReturn = null
            viewModel.onAction(DoctorDetailAction.Retry)

            val content = viewModel.uiState.value as DoctorDetailUiState.Content
            assertEquals(DOCTOR_ID, content.doctor.doctorId)
        }

    private fun aDoctor() =
        DoctorPublic(
            doctorId = DOCTOR_ID,
            fullName = "Dr. Asha Rao",
            title = "Dr.",
            specialization = "General Medicine",
            qualifications = "MBBS",
            bio = "A caring general physician.",
            yearsOfExperience = 8,
            consultationModes = listOf(ServiceMode.IN_PERSON, ServiceMode.ONLINE),
            acceptingRequests = true,
            avatarPath = null,
        )

    private fun aPairing() =
        DoctorServicePublic(
            doctorId = DOCTOR_ID,
            fullName = "Dr. Asha Rao",
            serviceId = "svc-1",
            serviceName = "General Checkup",
            serviceSlug = "general-checkup",
        )
}
