package com.aarogya.sanjeevani.core.data.requests

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val POLL_INTERVAL_MILLIS = 30_000L

/**
 * The seam between "the Request Detail screen refreshes itself" and
 * *how* it learns something changed. [PollingRequestUpdatesSource] --
 * this release's only implementation -- just emits every 30 seconds,
 * unconditionally, for as long as the screen collects it (its own
 * `viewModelScope` dies with the screen, which is what actually stops
 * this). A later realtime implementation (a Supabase Realtime channel
 * on this one request's row/status-history) would emit only when
 * something genuinely changed instead, with no change needed in
 * `RequestDetailViewModel` -- it only ever reacts to "something may
 * have changed, refresh," never to *how* it found out.
 */
interface RequestUpdatesSource {
    fun observeRequestUpdates(requestId: String): Flow<Unit>
}

@Singleton
class PollingRequestUpdatesSource
    @Inject
    constructor() : RequestUpdatesSource {
        override fun observeRequestUpdates(requestId: String): Flow<Unit> =
            flow {
                while (true) {
                    delay(POLL_INTERVAL_MILLIS)
                    emit(Unit)
                }
            }
    }
