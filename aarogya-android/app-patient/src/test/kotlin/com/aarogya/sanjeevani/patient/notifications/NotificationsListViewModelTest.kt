package com.aarogya.sanjeevani.patient.notifications

import app.cash.turbine.test
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.model.NotificationType
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeNotificationsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

class NotificationsListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notificationsRepository: FakeNotificationsRepository
    private lateinit var viewModel: NotificationsListViewModel

    @Before
    fun setUp() {
        notificationsRepository = FakeNotificationsRepository()
        viewModel = NotificationsListViewModel(notificationsRepository)
    }

    // -- Badge capping (pure function) -----------------------------------

    @Test
    fun `badge count is null for zero, the plain number up to 99, and 99+ beyond that`() {
        assertNull(formatUnreadBadgeCount(0))
        assertEquals("1", formatUnreadBadgeCount(1))
        assertEquals("99", formatUnreadBadgeCount(99))
        assertEquals("99+", formatUnreadBadgeCount(100))
        assertEquals("99+", formatUnreadBadgeCount(250))
    }

    // -- Optimistic mark-read + rollback -----------------------------------

    @Test
    fun `tapping an unread notification marks it read immediately, before the network call returns`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            viewModel.onAction(NotificationsListAction.Resumed)

            viewModel.onAction(NotificationsListAction.NotificationClicked("n1"))

            val content = viewModel.uiState.value.section as NotificationsListSectionState.Content
            assertTrue(content.notifications.single().isRead)
        }

    @Test
    fun `a failed mark-read rolls the optimistic update back and reports it`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            viewModel.onAction(NotificationsListAction.Resumed)
            notificationsRepository.errorToReturn = AppError.Network()

            viewModel.uiEvent.test {
                viewModel.onAction(NotificationsListAction.NotificationClicked("n1"))
                assertEquals(NotificationsListUiEvent.ShowMarkReadFailedMessage, awaitItem())
            }

            val content = viewModel.uiState.value.section as NotificationsListSectionState.Content
            assertFalse(content.notifications.single().isRead)
        }

    @Test
    fun `a successful mark-read reports that the unread count may have changed`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            viewModel.onAction(NotificationsListAction.Resumed)

            viewModel.uiEvent.test {
                viewModel.onAction(NotificationsListAction.NotificationClicked("n1"))
                assertEquals(NotificationsListUiEvent.UnreadCountMayHaveChanged, awaitItem())
            }
        }

    @Test
    fun `mark all read optimistically clears every unread flag and rolls back together on failure`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            notificationsRepository.notifications += aNotification(id = "n2", isRead = false)
            viewModel.onAction(NotificationsListAction.Resumed)
            notificationsRepository.errorToReturn = AppError.Network()

            viewModel.onAction(NotificationsListAction.MarkAllReadClicked)

            val content = viewModel.uiState.value.section as NotificationsListSectionState.Content
            assertTrue(content.notifications.all { !it.isRead })
            assertFalse(viewModel.uiState.value.isMarkingAllRead)
        }

    @Test
    fun `a second mark-all-read tap while one is already in flight is a no-op`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            viewModel.onAction(NotificationsListAction.Resumed)

            viewModel.onAction(NotificationsListAction.MarkAllReadClicked)
            viewModel.onAction(NotificationsListAction.MarkAllReadClicked)

            assertFalse(viewModel.uiState.value.isMarkingAllRead)
            val content = viewModel.uiState.value.section as NotificationsListSectionState.Content
            assertTrue(content.notifications.all { it.isRead })
        }

    // -- Navigation on tap --------------------------------------------------

    @Test
    fun `tapping a notification with a related request navigates there`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = true, relatedRequestId = "req-1")
            viewModel.onAction(NotificationsListAction.Resumed)

            viewModel.uiEvent.test {
                viewModel.onAction(NotificationsListAction.NotificationClicked("n1"))
                val event = awaitItem() as NotificationsListUiEvent.NavigateToRequestDetail
                assertEquals("req-1", event.requestId)
            }
        }

    @Test
    fun `tapping a notification with no related request sends no navigation event`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = true, relatedRequestId = null)
            viewModel.onAction(NotificationsListAction.Resumed)

            viewModel.uiEvent.test {
                viewModel.onAction(NotificationsListAction.NotificationClicked("n1"))
                expectNoEvents()
            }
        }

    private fun aNotification(
        id: String,
        isRead: Boolean,
        relatedRequestId: String? = null,
    ) = Notification(
        id = id,
        userId = FakeAuthRepository.FAKE_USER_ID,
        type = NotificationType.SYSTEM,
        title = "Title $id",
        body = "Body $id",
        relatedRequestId = relatedRequestId,
        readAt = null,
        isRead = isRead,
        createdAt = SOME_INSTANT,
    )
}
