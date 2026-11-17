package com.aarogya.sanjeevani.patient.profile

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.appearance.AppearanceRepository
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.profile.Profile
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Profile screen. [ProfileAction.AvatarSelected] arrives
 * already compressed -- [compressAvatarImage] is a pure function the
 * route calls before dispatching this, not something this class does
 * itself, so a local-only "even the lowest quality didn't fit" failure
 * never needs to round-trip through an action at all (see
 * `ProfileScreen.kt`'s own picker callback).
 */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val appearanceRepository: AppearanceRepository,
    ) : BaseViewModel<ProfileUiState, ProfileUiEvent, ProfileAction>(ProfileUiState()) {
        init {
            viewModelScope.launch {
                appearanceRepository.observeAppearanceMode().collect { mode ->
                    setState { copy(appearanceMode = mode) }
                }
            }
        }

        override fun onAction(action: ProfileAction) {
            when (action) {
                ProfileAction.Resumed -> load()
                ProfileAction.Retry -> {
                    setState { copy(section = ProfileSectionState.Loading) }
                    load()
                }
                is ProfileAction.AppearanceModeSelected ->
                    viewModelScope.launch { appearanceRepository.setAppearanceMode(action.mode) }
                is ProfileAction.AvatarSelected -> uploadAvatar(action.bytes, action.contentType)
            }
        }

        private fun load() {
            viewModelScope.launch {
                val userId = authRepository.currentUserId() ?: return@launch
                when (val result = profileRepository.getProfile(userId)) {
                    is AppResult.Success -> applyProfile(result.data)
                    is AppResult.Failure -> {
                        if (currentState.section !is ProfileSectionState.Content) {
                            setState { copy(section = ProfileSectionState.Error(result.error)) }
                        }
                    }
                }
            }
        }

        private suspend fun applyProfile(profile: Profile) {
            val avatarPath = profile.avatarUrl
            val avatarUrl =
                if (avatarPath == null) {
                    null
                } else {
                    when (val result = profileRepository.getAvatarSignedUrl(avatarPath)) {
                        is AppResult.Success -> result.data
                        is AppResult.Failure -> null
                    }
                }
            setState { copy(section = ProfileSectionState.Content(profile, avatarUrl)) }
        }

        private fun uploadAvatar(
            bytes: ByteArray,
            contentType: String,
        ) {
            if (currentState.isUploadingAvatar) return
            val userId = authRepository.currentUserId() ?: return

            setState { copy(isUploadingAvatar = true) }
            viewModelScope.launch {
                when (val result = profileRepository.uploadAvatar(userId, bytes, contentType)) {
                    is AppResult.Success -> {
                        applyProfile(result.data)
                        setState { copy(isUploadingAvatar = false) }
                    }
                    is AppResult.Failure -> {
                        setState { copy(isUploadingAvatar = false) }
                        sendEvent(ProfileUiEvent.AvatarUploadFailed(result.error))
                    }
                }
            }
        }
    }
