@file:OptIn(ExperimentalCoroutinesApi::class)

package com.aarogya.sanjeevani.patient.notifications

import com.aarogya.sanjeevani.core.data.model.NotificationType
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.testing.MainDispatcherRule
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeNotificationSource
import com.aarogya.sanjeevani.core.testing.fake.FakeNotificationsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")
private const val POLL_INTERVAL_MILLIS = 60_000L

/**
 * [NotificationBadgeViewModel] is the one app-wide source of the unread
 * count -- these tests cover its three real behaviors: an immediate
 * refresh on construction, a 60-second foreground poll gated by
 * [NotificationBadgeViewModel.resume]/[NotificationBadgeViewModel.pause],
 * and resetting to zero (and stopping the poll) the moment auth flips to
 * signed-out.
 */
class NotificationBadgeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var notificationsRepository: FakeNotificationsRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var notificationSource: FakeNotificationSource

    @Before
    fun setUp() {
        notificationsRepository = FakeNotificationsRepository()
        authRepository = FakeAuthRepository()
        notificationSource = FakeNotificationSource()
    }

    private fun viewModel() = NotificationBadgeViewModel(notificationsRepository, authRepository, notificationSource)

    @Test
    fun `refreshes the unread count once on construction`() =
        runTest {
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            notificationsRepository.notifications += aNotification(id = "n2", isRead = false)

            val viewModel = viewModel()

            assertEquals(2, viewModel.unreadCount.value)
        }

    // These three drive mainDispatcherRule's own TestDispatcher, not runTest's
    // default scheduler -- resume()'s delay runs on Dispatchers.Main (this
    // rule's dispatcher), a different scheduler instance than plain runTest{}
    // would advance (see MainDispatcherRule's own doc comment).

    @Test
    fun `resume starts a 60-second poll that refresh alone does not`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)

            viewModel.resume()
            assertEquals(1, viewModel.unreadCount.value)

            notificationsRepository.notifications += aNotification(id = "n2", isRead = false)
            advanceTimeBy(POLL_INTERVAL_MILLIS + 1_000L)

            assertEquals(2, viewModel.unreadCount.value)
            // resume()'s poll loop never completes on its own (by design --
            // see its own doc comment) -- leaving it running past this point
            // means runTest's own wind-down keeps re-draining this shared
            // scheduler forever, since there's always another iteration
            // queued. pause() is the only way this test ends.
            viewModel.pause()
        }

    @Test
    fun `pause stops the poll from picking up a later change`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.resume()
            viewModel.pause()

            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            advanceTimeBy(POLL_INTERVAL_MILLIS + 1_000L)

            assertEquals(0, viewModel.unreadCount.value)
        }

    @Test
    fun `signing out resets the count to zero and stops the poll`() =
        runTest(mainDispatcherRule.testDispatcher) {
            authRepository.signIn("patient@example.com", "password123")
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)
            val viewModel = viewModel()
            viewModel.resume()
            assertEquals(1, viewModel.unreadCount.value)

            authRepository.signOut()

            assertEquals(0, viewModel.unreadCount.value)

            notificationsRepository.notifications += aNotification(id = "n2", isRead = false)
            advanceTimeBy(POLL_INTERVAL_MILLIS + 1_000L)
            assertEquals(0, viewModel.unreadCount.value)
            // Belt-and-suspenders alongside the auth-state-driven cancel
            // this ViewModel already does on its own -- see the previous
            // test's own comment on why a live poll loop must never
            // outlive this test body.
            viewModel.pause()
        }

    @Test
    fun `a push-source signal triggers an immediate refresh`() =
        runTest {
            val viewModel = viewModel()
            notificationsRepository.notifications += aNotification(id = "n1", isRead = false)

            notificationSource.emitSignal()

            assertEquals(1, viewModel.unreadCount.value)
        }

    private fun aNotification(
        id: String,
        isRead: Boolean,
    ) = Notification(
        id = id,
        userId = FakeAuthRepository.FAKE_USER_ID,
        type = NotificationType.REQUEST_ACCEPTED,
        title = "Status update",
        body = "Your request status changed.",
        relatedRequestId = null,
        readAt = if (isRead) SOME_INSTANT else null,
        isRead = isRead,
        createdAt = SOME_INSTANT,
    )
}
