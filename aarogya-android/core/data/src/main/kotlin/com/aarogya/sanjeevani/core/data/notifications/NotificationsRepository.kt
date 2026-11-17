package com.aarogya.sanjeevani.core.data.notifications

import com.aarogya.sanjeevani.core.common.result.AppResult

interface NotificationsRepository {
    /** `select * from notifications where user_id = auth.uid() order by
     *  created_at desc` (client-contract.md §11). */
    suspend fun getNotifications(): AppResult<List<Notification>>

    /** `get_unread_notification_count()` -- for a badge. */
    suspend fun getUnreadCount(): AppResult<Int>

    /** `mark_notification_read(p_notification_id)` -- returns `true` if
     *  a row was actually updated (`0` rows -- didn't exist, or wasn't
     *  the caller's -- isn't an error either way per the docs, just a
     *  `false` here). */
    suspend fun markRead(notificationId: String): AppResult<Boolean>

    /** `mark_all_notifications_read()` -- the count of rows updated. */
    suspend fun markAllRead(): AppResult<Int>
}
