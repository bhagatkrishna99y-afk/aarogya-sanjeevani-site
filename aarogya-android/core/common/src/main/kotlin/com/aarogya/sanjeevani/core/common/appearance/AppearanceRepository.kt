package com.aarogya.sanjeevani.core.common.appearance

import kotlinx.coroutines.flow.Flow

/** The user's theme preference -- deliberately *not* the same type as
 *  `core.designsystem.theme.ThemeMode` (this module has no dependency
 *  on `core:designsystem` and shouldn't gain one just to persist three
 *  labels); the app layer maps one to the other with a small `when` at
 *  the call site, same split as `core.designsystem`'s own
 *  `AsRequestStatus`/`RequestStatus`. [System] is the default. */
enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Device-local only -- never synced to the backend, never tied to a
 *  particular signed-in user (a shared/kiosk device keeping its own
 *  appearance choice across accounts is the right default here, unlike
 *  the notification badge, which genuinely is per-account state). */
interface AppearanceRepository {
    /** Emits the current [AppearanceMode] immediately, then again on
     *  every change. */
    fun observeAppearanceMode(): Flow<AppearanceMode>

    suspend fun setAppearanceMode(mode: AppearanceMode)
}
