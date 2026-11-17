package com.aarogya.sanjeevani.core.data.notifications

import com.aarogya.sanjeevani.core.data.model.NotificationType
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `notifications` table (client-contract.md §11). */
@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    @SerialName("related_request_id") val relatedRequestId: String?,
    @SerialName("read_at") val readAt: Instant?,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: Instant,
)
