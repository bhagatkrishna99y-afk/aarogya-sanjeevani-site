package com.aarogyasanjeevani.doctor.ui.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.AuthRepository
import com.aarogyasanjeevani.doctor.data.repository.DoctorApplicationRepository
import com.aarogyasanjeevani.doctor.domain.model.DoctorApplication
import com.aarogyasanjeevani.doctor.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApplicationFormState(
    val fullName: String = "",
    val phone: String = "",
    val specialization: String = "",
    val licenseNumber: String = "",
    val bio: String = "",
    val yearsExperience: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val applicationRepository: DoctorApplicationRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _statusState = MutableStateFlow<UiState<DoctorApplication?>>(UiState.Loading)
    val statusState: StateFlow<UiState<DoctorApplication?>> = _statusState.asStateFlow()

    private val _formState = MutableStateFlow(ApplicationFormState())
    val formState: StateFlow<ApplicationFormState> = _formState.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()

    init {
        refreshStatus()
        prefillFromProfile()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _statusState.value = UiState.Loading
            applicationRepository.fetchMyApplication()
                .onSuccess { app ->
                    _statusState.value = UiState.Success(app)
                    if (app != null) {
                        _formState.value = _formState.value.copy(
                            specialization = app.specialization,
                            licenseNumber = app.licenseNumber,
                            bio = app.bio ?: "",
                            yearsExperience = app.yearsExperience.toString(),
                        )
                    }
                }
                .onFailure { _statusState.value = UiState.Error(it.message ?: "Couldn't check your application status.") }
        }
    }

    private fun prefillFromProfile() {
        viewModelScope.launch {
            authRepository.fetchCurrentProfile().onSuccess { profile ->
                _formState.value = _formState.value.copy(
                    fullName = profile.fullName,
                    phone = profile.phone ?: "",
                )
            }
        }
    }

    fun updateFullName(value: String) = updateForm { copy(fullName = value) }
    fun updatePhone(value: String) = updateForm { copy(phone = value) }
    fun updateSpecialization(value: String) = updateForm { copy(specialization = value) }
    fun updateLicenseNumber(value: String) = updateForm { copy(licenseNumber = value) }
    fun updateBio(value: String) = updateForm { copy(bio = value) }
    fun updateYearsExperience(value: String) = updateForm { copy(yearsExperience = value.filter { it.isDigit() }) }

    private inline fun updateForm(crossinline block: ApplicationFormState.() -> ApplicationFormState) {
        _formState.value = _formState.value.block()
    }

    fun submit() {
        val state = _formState.value
        if (state.isSubmitting) return
        if (state.fullName.isBlank() || state.specialization.isBlank() || state.licenseNumber.isBlank()) {
            updateForm { copy(errorMessage = "Please fill in your name, specialization and license number.") }
            return
        }

        viewModelScope.launch {
            updateForm { copy(isSubmitting = true, errorMessage = null) }
            applicationRepository.submitApplication(
                fullName = state.fullName.trim(),
                phone = state.phone.trim().takeIf { it.isNotBlank() },
                specialization = state.specialization.trim(),
                licenseNumber = state.licenseNumber.trim(),
                bio = state.bio.trim().takeIf { it.isNotBlank() },
                yearsExperience = state.yearsExperience.toIntOrNull() ?: 0,
            )
                .onSuccess {
                    updateForm { copy(isSubmitting = false) }
                    _submitted.value = true
                    refreshStatus()
                }
                .onFailure { updateForm { copy(isSubmitting = false, errorMessage = it.message) } }
        }
    }

    fun acknowledgeSubmitted() {
        _submitted.value = false
    }
}
