package com.aarogya.sanjeevani.core.data.notifications

import com.aarogya.sanjeevani.core.data.model.NotificationType
import kotlinx.datetime.Instant

data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val relatedRequestId: String?,
    val readAt: Instant?,
    val isRead: Boolean,
    val createdAt: Instant,
)

fun NotificationDto.toDomain(): Notification =
    Notification(
        id = id,
        userId = userId,
        type = type,
        title = title,
        body = body,
        relatedRequestId = relatedRequestId,
        readAt = readAt,
        isRead = isRead,
        createdAt = createdAt,
    )
