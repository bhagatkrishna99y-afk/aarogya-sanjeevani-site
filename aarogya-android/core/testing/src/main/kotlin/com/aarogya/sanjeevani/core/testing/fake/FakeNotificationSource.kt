package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.data.notifications.NotificationSource
import kotlinx.coroutines.flow.MutableSharedFlow

/** In-memory [NotificationSource] -- a test calls [emitSignal] to
 *  simulate a future push source telling observers to refresh. */
class FakeNotificationSource : NotificationSource {
    val signal = MutableSharedFlow<Unit>()

    suspend fun emitSignal() = signal.emit(Unit)

    override fun observeNewNotificationSignal() = signal
}
