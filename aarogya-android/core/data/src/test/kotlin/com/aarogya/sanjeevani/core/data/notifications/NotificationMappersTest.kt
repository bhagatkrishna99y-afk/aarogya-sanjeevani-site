package com.aarogya.sanjeevani.core.data.notifications

import com.aarogya.sanjeevani.core.data.model.NotificationType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val CREATED = Instant.parse("2026-01-01T00:00:00Z")
private val READ_AT = Instant.parse("2026-01-01T01:00:00Z")

/** [NotificationDto.toDomain] is a plain field-for-field mapper -- every
 *  field here is set to a distinct, recognizable value so a future
 *  dropped/renamed field fails this test instead of silently reaching
 *  production. */
class NotificationMappersTest {
    @Test
    fun `maps every field across unchanged for an unread notification`() {
        val dto =
            NotificationDto(
                id = "notif-1",
                userId = "user-1",
                type = NotificationType.REQUEST_ACCEPTED,
                title = "Request accepted",
                body = "Dr. Rao accepted your request.",
                relatedRequestId = "req-1",
                readAt = null,
                isRead = false,
                createdAt = CREATED,
            )

        val domain = dto.toDomain()

        assertEquals("notif-1", domain.id)
        assertEquals("user-1", domain.userId)
        assertEquals(NotificationType.REQUEST_ACCEPTED, domain.type)
        assertEquals("Request accepted", domain.title)
        assertEquals("Dr. Rao accepted your request.", domain.body)
        assertEquals("req-1", domain.relatedRequestId)
        assertNull(domain.readAt)
        assertEquals(false, domain.isRead)
        assertEquals(CREATED, domain.createdAt)
    }

    @Test
    fun `maps a read notification's readAt through unchanged`() {
        val dto =
            NotificationDto(
                id = "notif-1",
                userId = "user-1",
                type = NotificationType.REQUEST_COMPLETED,
                title = "Request completed",
                body = "Your request is complete.",
                relatedRequestId = null,
                readAt = READ_AT,
                isRead = true,
                createdAt = CREATED,
            )

        val domain = dto.toDomain()

        assertEquals(READ_AT, domain.readAt)
        assertEquals(true, domain.isRead)
        assertNull(domain.relatedRequestId)
    }
}
