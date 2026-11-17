package com.aarogya.sanjeevani.core.common.connectivity

import kotlinx.coroutines.flow.Flow

/** Whether the device currently has a validated internet-capable
 *  network -- the one thing an offline banner needs to know. Emits the
 *  current state immediately on collection, then again on every change. */
interface ConnectivityObserver {
    val isConnected: Flow<Boolean>
}
