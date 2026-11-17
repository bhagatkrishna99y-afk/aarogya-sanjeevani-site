package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.data.notifications.NotificationsRepository
import kotlinx.datetime.Instant

/** In-memory [NotificationsRepository] seeded with [notifications]. RLS
 *  scoping (the real backend's `user_id = auth.uid()`) isn't modeled --
 *  seed only what a given test's signed-in user should see. */
class FakeNotificationsRepository : NotificationsRepository {
    val notifications = mutableListOf<Notification>()

    var errorToReturn: AppError? = null

    override suspend fun getNotifications(): AppResult<List<Notification>> {
        errorToReturn?.let { return it.asError() }
        return notifications.toList().asSuccess()
    }

    override suspend fun getUnreadCount(): AppResult<Int> {
        errorToReturn?.let { return it.asError() }
        return notifications.count { !it.isRead }.asSuccess()
    }

    override suspend fun markRead(notificationId: String): AppResult<Boolean> {
        errorToReturn?.let { return it.asError() }
        val index = notifications.indexOfFirst { it.id == notificationId }
        val found = index != -1
        if (found) {
            notifications[index] =
                notifications[index].copy(isRead = true, readAt = Instant.parse("2026-01-01T00:00:00Z"))
        }
        return found.asSuccess()
    }

    override suspend fun markAllRead(): AppResult<Int> {
        errorToReturn?.let { return it.asError() }
        var updated = 0
        notifications.forEachIndexed { index, notification ->
            if (!notification.isRead) {
                notifications[index] = notification.copy(isRead = true, readAt = Instant.parse("2026-01-01T00:00:00Z"))
                updated++
            }
        }
        return updated.asSuccess()
    }
}
