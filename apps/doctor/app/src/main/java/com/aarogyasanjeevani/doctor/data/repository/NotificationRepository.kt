package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.MarkNotificationReadParams
import com.aarogyasanjeevani.doctor.data.remote.dto.NotificationDto
import com.aarogyasanjeevani.doctor.domain.model.AppNotification
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun fetchNotifications(): Result<List<AppNotification>> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")
        postgrest.from("notifications")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<NotificationDto>()
            .map {
                AppNotification(
                    id = it.id,
                    type = it.type,
                    title = it.title,
                    body = it.body,
                    relatedRequestId = it.relatedRequestId,
                    isRead = it.readAt != null,
                    createdAt = Instant.parse(it.createdAt),
                )
            }
    }

    suspend fun markRead(notificationId: String): Result<Unit> = safeCall {
        postgrest.rpc("mark_notification_read", MarkNotificationReadParams(notificationId = notificationId))
        Unit
    }
}
