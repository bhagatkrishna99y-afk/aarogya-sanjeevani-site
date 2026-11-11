package com.aarogyasanjeevani.doctor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.remote.dto.ProfileDto
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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val applicationRepository: DoctorApplicationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ProfileDto>>(UiState.Loading)
    val uiState: StateFlow<UiState<ProfileDto>> = _uiState.asStateFlow()

    private val _application = MutableStateFlow<DoctorApplication?>(null)
    val application: StateFlow<DoctorApplication?> = _application.asStateFlow()

    private val _isSavingOrSigningOut = MutableStateFlow(false)
    val isSavingOrSigningOut: StateFlow<Boolean> = _isSavingOrSigningOut.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            authRepository.fetchCurrentProfile()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load your profile.") }

            applicationRepository.fetchMyApplication().onSuccess { _application.value = it }
        }
    }

    fun save(fullName: String, phone: String?) {
        if (_isSavingOrSigningOut.value) return
        viewModelScope.launch {
            _isSavingOrSigningOut.value = true
            authRepository.updateProfile(fullName, phone)
                .onSuccess {
                    _isSavingOrSigningOut.value = false
                    _message.value = "Profile updated."
                    load()
                }
                .onFailure {
                    _isSavingOrSigningOut.value = false
                    _message.value = it.message ?: "Couldn't update your profile."
                }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        if (_isSavingOrSigningOut.value) return
        viewModelScope.launch {
            _isSavingOrSigningOut.value = true
            authRepository.signOut()
            _isSavingOrSigningOut.value = false
            onSignedOut()
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
