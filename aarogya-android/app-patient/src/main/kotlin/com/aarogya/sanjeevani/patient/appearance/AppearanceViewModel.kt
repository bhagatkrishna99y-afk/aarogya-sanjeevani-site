package com.aarogya.sanjeevani.patient.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.common.appearance.AppearanceRepository
import com.aarogya.sanjeevani.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Backs [com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme]
 *  at the app root (`MainActivity.kt`) -- the one place [AppearanceMode]
 *  (this app's own persisted preference, `core:common`) is mapped to
 *  [ThemeMode] (`core:designsystem`'s own presentation vocabulary); see
 *  [AppearanceMode]'s own doc comment for why those two types are
 *  deliberately kept separate rather than one module depending on the
 *  other. Changing the preference on the Profile screen flows back here
 *  through the same [AppearanceRepository] instance -- nothing pushes a
 *  value "down" to this ViewModel directly. */
@HiltViewModel
class AppearanceViewModel
    @Inject
    constructor(
        appearanceRepository: AppearanceRepository,
    ) : ViewModel() {
        val themeMode: StateFlow<ThemeMode> =
            appearanceRepository.observeAppearanceMode()
                .map { it.toThemeMode() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ThemeMode.System)
    }

private fun AppearanceMode.toThemeMode(): ThemeMode =
    when (this) {
        AppearanceMode.SYSTEM -> ThemeMode.System
        AppearanceMode.LIGHT -> ThemeMode.Light
        AppearanceMode.DARK -> ThemeMode.Dark
    }
