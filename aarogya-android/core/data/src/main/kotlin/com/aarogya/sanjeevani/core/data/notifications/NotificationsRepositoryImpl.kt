package com.aarogya.sanjeevani.core.data.notifications

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.network.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val TABLE = "notifications"

class NotificationsRepositoryImpl
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : NotificationsRepository {
        override suspend fun getNotifications(): AppResult<List<Notification>> =
            safeApiCall(retryable = true) {
                client.postgrest.from(TABLE)
                    .select(columns = Columns.ALL) { order("created_at", Order.DESCENDING) }
                    .decodeList<NotificationDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getUnreadCount(): AppResult<Int> =
            safeApiCall(retryable = true) {
                client.postgrest.rpc("get_unread_notification_count").decodeAs<Int>()
            }

        override suspend fun markRead(notificationId: String): AppResult<Boolean> =
            safeApiCall {
                client.postgrest
                    .rpc("mark_notification_read", MarkReadParams(notificationId))
                    .decodeAs<Int>()
            }.map { rowsUpdated -> rowsUpdated > 0 }

        override suspend fun markAllRead(): AppResult<Int> =
            safeApiCall {
                client.postgrest.rpc("mark_all_notifications_read").decodeAs<Int>()
            }
    }

@Serializable
private data class MarkReadParams(
    @SerialName("p_notification_id") val notificationId: String,
)
