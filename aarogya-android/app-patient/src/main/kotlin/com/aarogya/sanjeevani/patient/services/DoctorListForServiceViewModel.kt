package com.aarogya.sanjeevani.patient.services

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import com.aarogya.sanjeevani.patient.doctors.avatarUrlsByDoctorId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Doctors-for-service list. Two reads chained, not run in
 * parallel: [DoctorsRepository.getDoctorsForService] first decides
 * *whether there's anything to show at all* (and, when there is, which
 * doctor ids) -- running [DoctorsRepository.getDoctorsByIds] alongside
 * it would mean sending a second request this method already knows is
 * pointless whenever the first comes back empty.
 */
@HiltViewModel
class DoctorListForServiceViewModel
    @Inject
    constructor(
        private val doctorsRepository: DoctorsRepository,
    ) : BaseViewModel<DoctorListForServiceUiState, Nothing, DoctorListForServiceAction>(DoctorListForServiceUiState()) {
        private var serviceId: String? = null

        override fun onAction(action: DoctorListForServiceAction) {
            when (action) {
                is DoctorListForServiceAction.Load -> {
                    serviceId = action.serviceId
                    setState { copy(serviceName = action.serviceName) }
                    load()
                }
                DoctorListForServiceAction.Retry -> load()
            }
        }

        private fun load() {
            val id = serviceId ?: return
            setState { copy(section = DoctorListForServiceSectionState.Loading) }
            viewModelScope.launch {
                when (val pairings = doctorsRepository.getDoctorsForService(id)) {
                    is AppResult.Failure ->
                        setState { copy(section = DoctorListForServiceSectionState.Error(pairings.error)) }
                    is AppResult.Success -> {
                        if (pairings.data.isEmpty()) {
                            setState { copy(section = DoctorListForServiceSectionState.Content(doctors = emptyList())) }
                        } else {
                            loadDoctorProfiles(pairings.data.map { it.doctorId })
                        }
                    }
                }
            }
        }

        private suspend fun loadDoctorProfiles(doctorIds: List<String>) {
            when (val doctors = doctorsRepository.getDoctorsByIds(doctorIds)) {
                is AppResult.Failure ->
                    setState { copy(section = DoctorListForServiceSectionState.Error(doctors.error)) }
                is AppResult.Success -> {
                    val avatarUrls = resolveAvatarUrls(doctors.data)
                    setState {
                        copy(
                            section =
                                DoctorListForServiceSectionState.Content(
                                    doctors = doctors.data,
                                    avatarUrlsByDoctorId = avatarUrls,
                                ),
                        )
                    }
                }
            }
        }

        private suspend fun resolveAvatarUrls(doctors: List<DoctorPublic>): Map<String, String> {
            val urlsByPath = doctorsRepository.getAvatarUrls(doctors.mapNotNull { it.avatarPath })
            return avatarUrlsByDoctorId(doctors, urlsByPath)
        }
    }
