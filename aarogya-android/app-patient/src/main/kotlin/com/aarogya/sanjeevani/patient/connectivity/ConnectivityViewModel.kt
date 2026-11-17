package com.aarogya.sanjeevani.patient.connectivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.connectivity.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Backs [com.aarogya.sanjeevani.core.ui.connectivity.OfflineBanner] at
 *  the app root -- the one place [ConnectivityObserver] gets turned
 *  into UI state. */
@HiltViewModel
class ConnectivityViewModel
    @Inject
    constructor(
        connectivityObserver: ConnectivityObserver,
    ) : ViewModel() {
        val isOffline: StateFlow<Boolean> =
            connectivityObserver.isConnected
                .map { isConnected -> !isConnected }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)
    }
