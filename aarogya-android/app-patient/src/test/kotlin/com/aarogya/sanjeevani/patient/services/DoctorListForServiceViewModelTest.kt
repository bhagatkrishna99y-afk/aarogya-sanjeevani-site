package com.aarogya.sanjeevani.patient.services

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

private const val SERVICE_ID = "svc-1"
private const val SERVICE_NAME = "Home Nursing"

class DoctorListForServiceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var doctorsRepository: FakeDoctorsRepository
    private lateinit var viewModel: DoctorListForServiceViewModel

    @Before
    fun setUp() {
        doctorsRepository = FakeDoctorsRepository()
        viewModel = DoctorListForServiceViewModel(doctorsRepository)
    }

    @Test
    fun `the service name shows immediately, before any read completes`() =
        runTest {
            viewModel.onAction(DoctorListForServiceAction.Load(serviceId = SERVICE_ID, serviceName = SERVICE_NAME))

            assertEquals(SERVICE_NAME, viewModel.uiState.value.serviceName)
        }

    @Test
    fun `no doctors offering this service yields an empty Content, not an Error`() =
        runTest {
            viewModel.onAction(DoctorListForServiceAction.Load(serviceId = SERVICE_ID, serviceName = SERVICE_NAME))

            val content = viewModel.uiState.value.section as DoctorListForServiceSectionState.Content
            assertTrue(content.doctors.isEmpty())
        }

    @Test
    fun `doctors offering the service are resolved to their full profiles`() =
        runTest {
            doctorsRepository.doctorServices += aPairing(doctorId = "doc-1")
            doctorsRepository.doctors += aDoctor(id = "doc-1")

            viewModel.onAction(DoctorListForServiceAction.Load(serviceId = SERVICE_ID, serviceName = SERVICE_NAME))

            val content = viewModel.uiState.value.section as DoctorListForServiceSectionState.Content
            assertEquals(listOf("doc-1"), content.doctors.map { it.doctorId })
        }

    @Test
    fun `a failure reading the service's doctor pairings surfaces as Error`() =
        runTest {
            doctorsRepository.errorToReturn = AppError.Network()

            viewModel.onAction(DoctorListForServiceAction.Load(serviceId = SERVICE_ID, serviceName = SERVICE_NAME))

            assertTrue(viewModel.uiState.value.section is DoctorListForServiceSectionState.Error)
        }

    @Test
    fun `retry reuses the service id from the original load`() =
        runTest {
            doctorsRepository.errorToReturn = AppError.Network()
            viewModel.onAction(DoctorListForServiceAction.Load(serviceId = SERVICE_ID, serviceName = SERVICE_NAME))
            doctorsRepository.doctorServices += aPairing(doctorId = "doc-1")
            doctorsRepository.doctors += aDoctor(id = "doc-1")

            doctorsRepository.errorToReturn = null
            viewModel.onAction(DoctorListForServiceAction.Retry)

            val content = viewModel.uiState.value.section as DoctorListForServiceSectionState.Content
            assertEquals(listOf("doc-1"), content.doctors.map { it.doctorId })
        }

    private fun aPairing(doctorId: String) =
        DoctorServicePublic(
            doctorId = doctorId,
            fullName = "Doctor $doctorId",
            serviceId = SERVICE_ID,
            serviceName = SERVICE_NAME,
            serviceSlug = "home-nursing",
        )

    private fun aDoctor(id: String) =
        DoctorPublic(
            doctorId = id,
            fullName = "Doctor $id",
            title = "Dr.",
            specialization = "General",
            qualifications = null,
            bio = null,
            yearsOfExperience = 5,
            consultationModes = listOf(ServiceMode.IN_PERSON),
            acceptingRequests = true,
            avatarPath = null,
        )
}
