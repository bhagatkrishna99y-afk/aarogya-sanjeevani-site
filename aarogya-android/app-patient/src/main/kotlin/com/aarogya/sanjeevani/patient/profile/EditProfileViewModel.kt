package com.aarogya.sanjeevani.patient.profile

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives Edit Profile. Only [EditProfileUiState.fullName]/[EditProfileUiState.phone]
 *  are ever sent to [ProfileRepository.updateProfile] -- the same two,
 *  and only those two, columns `profiles_update_own` actually permits
 *  (client-contract.md §2); email has no field here to edit at all. */
@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
    ) : BaseViewModel<EditProfileUiState, EditProfileUiEvent, EditProfileAction>(EditProfileUiState()) {
        override fun onAction(action: EditProfileAction) {
            when (action) {
                EditProfileAction.Load -> load()
                is EditProfileAction.FullNameChanged -> setState { copy(fullName = action.value) }
                EditProfileAction.FullNameFocusLost -> setState { copy(fullNameTouched = true) }
                is EditProfileAction.PhoneChanged -> setState { copy(phone = action.value) }
                EditProfileAction.PhoneFocusLost -> setState { copy(phoneTouched = true) }
                EditProfileAction.SaveClicked -> save()
            }
        }

        private fun load() {
            setState { copy(isLoading = true, loadError = null) }
            viewModelScope.launch {
                val userId = authRepository.currentUserId()
                if (userId == null) {
                    setState { copy(isLoading = false, loadError = AppError.Unauthorized()) }
                    return@launch
                }
                when (val result = profileRepository.getProfile(userId)) {
                    is AppResult.Success -> {
                        val name = result.data.fullName.orEmpty()
                        val phone = result.data.phone.orEmpty()
                        setState {
                            copy(
                                isLoading = false,
                                fullName = name,
                                phone = phone,
                                originalFullName = name,
                                originalPhone = phone,
                            )
                        }
                    }
                    is AppResult.Failure -> setState { copy(isLoading = false, loadError = result.error) }
                }
            }
        }

        private fun save() {
            if (!currentState.isSaveEnabled) {
                setState { copy(submitAttempted = true, fullNameTouched = true, phoneTouched = true) }
                return
            }
            val userId = authRepository.currentUserId() ?: return
            val state = currentState

            setState { copy(isSaving = true) }
            viewModelScope.launch {
                val trimmedPhone = state.phone.trim().ifBlank { null }
                when (val result = profileRepository.updateProfile(userId, state.fullName.trim(), trimmedPhone)) {
                    is AppResult.Success -> {
                        setState {
                            copy(
                                isSaving = false,
                                fullName = result.data.fullName.orEmpty(),
                                phone = result.data.phone.orEmpty(),
                                originalFullName = result.data.fullName.orEmpty(),
                                originalPhone = result.data.phone.orEmpty(),
                            )
                        }
                        sendEvent(EditProfileUiEvent.SaveSucceeded)
                    }
                    is AppResult.Failure -> {
                        setState { copy(isSaving = false) }
                        sendEvent(EditProfileUiEvent.SaveFailed(result.error))
                    }
                }
            }
        }
    }
