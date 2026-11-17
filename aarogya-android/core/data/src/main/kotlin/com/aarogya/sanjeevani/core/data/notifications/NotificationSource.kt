package com.aarogya.sanjeevani.core.data.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The seam that lets a future push-notification source (FCM) be added
 * without any screen or ViewModel that reacts to "a notification just
 * arrived" needing to change. [InAppNotificationSource] -- this
 * release's only implementation -- never actually emits: every refresh
 * ([com.aarogya.sanjeevani.core.data.notifications.NotificationsRepository]
 * is still pull-only) already happens on its own cadence (app resume, a
 * 60-second foreground poll, right after marking something read), not
 * because anything pushed. A later `FcmNotificationSource` would emit
 * here whenever a push arrives, and every observer (the unread badge,
 * the notifications list) would simply start refreshing sooner, with
 * no code of theirs touched.
 */
interface NotificationSource {
    fun observeNewNotificationSignal(): Flow<Unit>
}

@Singleton
class InAppNotificationSource
    @Inject
    constructor() : NotificationSource {
        override fun observeNewNotificationSignal(): Flow<Unit> = emptyFlow()
    }
