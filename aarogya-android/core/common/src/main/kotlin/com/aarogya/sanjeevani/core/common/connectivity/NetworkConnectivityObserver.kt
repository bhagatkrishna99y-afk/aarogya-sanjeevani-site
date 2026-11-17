package com.aarogya.sanjeevani.core.common.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * "Connected" means a network that is both declared internet-capable
 * and has actually passed Android's own internet validation (a captive
 * Wi-Fi portal with no real uplink is [NetworkCapabilities] internet-
 * capable but not validated) -- being associated with an access point
 * isn't the same thing as having a path to this app's backend, and an
 * offline banner driven by the weaker signal would be wrong exactly
 * when it matters most (a hotel/airport captive portal).
 */
class NetworkConnectivityObserver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConnectivityObserver {
        override val isConnected: Flow<Boolean> =
            callbackFlow {
                val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
                if (connectivityManager == null) {
                    trySend(false)
                    close()
                    return@callbackFlow
                }

                val validatedNetworks = mutableSetOf<Network>()

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onLost(network: Network) {
                            validatedNetworks -= network
                            trySend(validatedNetworks.isNotEmpty())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                                validatedNetworks += network
                            } else {
                                validatedNetworks -= network
                            }
                            trySend(validatedNetworks.isNotEmpty())
                        }
                    }

                val request =
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                connectivityManager.registerNetworkCallback(request, callback)

                trySend(isCurrentlyConnected(connectivityManager))

                awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
            }.distinctUntilChanged()

        private fun isCurrentlyConnected(manager: ConnectivityManager): Boolean {
            val network = manager.activeNetwork
            val capabilities = network?.let(manager::getNetworkCapabilities) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }
