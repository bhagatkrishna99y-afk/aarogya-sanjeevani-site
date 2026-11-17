package com.aarogya.sanjeevani.patient.doctors

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Doctor Detail screen. A single load, not a resume-aware
 * one like `ServiceDetailViewModel` -- this screen's "doctor went away"
 * case is "stale by the time you tapped in from a list," not "changed
 * while you were already looking at it," so one check on arrival is
 * enough (see [DoctorDetailUiState]'s doc comment for how that check's
 * result is handled).
 */
@HiltViewModel
class DoctorDetailViewModel
    @Inject
    constructor(
        private val doctorsRepository: DoctorsRepository,
    ) : BaseViewModel<DoctorDetailUiState, DoctorDetailUiEvent, DoctorDetailAction>(DoctorDetailUiState.Loading) {
        private var doctorId: String? = null

        override fun onAction(action: DoctorDetailAction) {
            when (action) {
                is DoctorDetailAction.Load -> {
                    doctorId = action.doctorId
                    load()
                }
                DoctorDetailAction.Retry -> load()
            }
        }

        private fun load() {
            val id = doctorId ?: return
            setState { DoctorDetailUiState.Loading }
            viewModelScope.launch {
                when (val result = doctorsRepository.getDoctor(id)) {
                    is AppResult.Success -> loadServicesAndAvatar(result.data)
                    is AppResult.Failure ->
                        if (result.error is AppError.NotFound) {
                            sendEvent(DoctorDetailUiEvent.DoctorUnavailable)
                        } else {
                            setState { DoctorDetailUiState.Error(result.error) }
                        }
                }
            }
        }

        private suspend fun loadServicesAndAvatar(doctor: DoctorPublic) =
            coroutineScope {
                val avatarDeferred =
                    async {
                        doctor.avatarPath?.let { path -> doctorsRepository.getAvatarUrls(listOf(path))[path] }
                    }
                val servicesDeferred = async { doctorsRepository.getServicesForDoctor(doctor.doctorId) }
                val avatarUrl = avatarDeferred.await()
                val services =
                    when (val result = servicesDeferred.await()) {
                        is AppResult.Success -> result.data
                        is AppResult.Failure -> emptyList()
                    }
                setState { DoctorDetailUiState.Content(doctor = doctor, avatarUrl = avatarUrl, services = services) }
            }
    }
