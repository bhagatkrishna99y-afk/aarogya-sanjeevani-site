package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.connectivity.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow

/** Defaults to connected -- flip [isConnected] in a test to exercise the
 *  offline-banner path. */
class FakeConnectivityObserver : ConnectivityObserver {
    private val state = MutableStateFlow(true)

    override val isConnected = state

    fun setConnected(connected: Boolean) {
        state.value = connected
    }
}
