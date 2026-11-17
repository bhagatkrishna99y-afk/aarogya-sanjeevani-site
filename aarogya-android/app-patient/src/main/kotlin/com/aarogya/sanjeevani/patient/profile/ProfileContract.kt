package com.aarogya.sanjeevani.patient.profile

import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.profile.Profile

sealed interface ProfileSectionState {
    data object Loading : ProfileSectionState

    data class Error(val error: AppError) : ProfileSectionState

    /** [avatarUrl] is already resolved to a signed URL (or `null` when
     *  the profile has no avatar, or resolving one silently failed --
     *  either way [com.aarogya.sanjeevani.core.designsystem.component.AsAvatar]'s
     *  own initials fallback covers it, so this never needs its own
     *  error state). */
    data class Content(val profile: Profile, val avatarUrl: String?) : ProfileSectionState
}

data class ProfileUiState(
    val section: ProfileSectionState = ProfileSectionState.Loading,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val isUploadingAvatar: Boolean = false,
)

/** [AvatarUploadFailed] carries the real [AppError] for a backend
 *  failure, `null` for the one local-only failure
 *  ([com.aarogya.sanjeevani.patient.profile.compressAvatarImage]
 *  returning `null` because even the lowest-quality compression still
 *  didn't fit the bucket's own limit) -- resolved to text at the UI
 *  edge either way, same rule every screen in this app follows. */
sealed interface ProfileUiEvent {
    data class AvatarUploadFailed(val error: AppError?) : ProfileUiEvent
}

sealed interface ProfileAction {
    data object Resumed : ProfileAction

    data object Retry : ProfileAction

    data class AppearanceModeSelected(val mode: AppearanceMode) : ProfileAction

    data class AvatarSelected(val bytes: ByteArray, val contentType: String) : ProfileAction
}
