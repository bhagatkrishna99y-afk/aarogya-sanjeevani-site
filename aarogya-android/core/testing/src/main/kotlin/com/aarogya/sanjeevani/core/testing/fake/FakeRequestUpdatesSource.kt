package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.data.requests.RequestUpdatesSource
import kotlinx.coroutines.flow.MutableSharedFlow

/** In-memory [RequestUpdatesSource] -- never emits on its own; a test
 *  calls [emitUpdate] to simulate a poll tick (or a future realtime
 *  push) for [requestId]. */
class FakeRequestUpdatesSource : RequestUpdatesSource {
    private val signal = MutableSharedFlow<Unit>()

    suspend fun emitUpdate() = signal.emit(Unit)

    override fun observeRequestUpdates(requestId: String) = signal
}
