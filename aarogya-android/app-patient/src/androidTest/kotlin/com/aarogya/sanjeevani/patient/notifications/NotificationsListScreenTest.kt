package com.aarogya.sanjeevani.patient.notifications

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.model.NotificationType
import com.aarogya.sanjeevani.core.data.notifications.Notification
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.testing.fake.FakeAuthRepository
import com.aarogya.sanjeevani.core.testing.fake.FakeNotificationsRepository
import kotlinx.datetime.Instant
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

/**
 * Exercises the real [NotificationsListViewModel] (fakes throughout, no
 * Hilt) driving its own stateless [NotificationsListScreen] -- tapping
 * an unread notification marks it read on screen, and "Mark all as
 * read" clears every unread dot/bold title at once. The underlying
 * optimistic-update/rollback logic is [NotificationsListViewModelTest]'s
 * job; this one proves tapping the real card/button on screen reaches
 * that logic at all.
 */
@RunWith(AndroidJUnit4::class)
class NotificationsListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var notificationsRepository: FakeNotificationsRepository
    private lateinit var viewModel: NotificationsListViewModel
    private var navigatedToRequestId: String? = null

    @Before
    fun setUp() {
        notificationsRepository = FakeNotificationsRepository()
        viewModel = NotificationsListViewModel(notificationsRepository)
        navigatedToRequestId = null
    }

    private fun setContent() {
        composeTestRule.setContent {
            AarogyaTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    viewModel.onAction(NotificationsListAction.Resumed)
                    viewModel.uiEvent.collect { event ->
                        if (event is NotificationsListUiEvent.NavigateToRequestDetail) {
                            navigatedToRequestId = event.requestId
                        }
                    }
                }
                NotificationsListScreen(uiState = uiState, onAction = viewModel::onAction)
            }
        }
    }

    @Test
    fun tappingAnUnreadNotification_marksItRead_andNavigatesToItsRequest() {
        notificationsRepository.notifications +=
            aNotification(id = "n1", title = "Request accepted", body = "Dr. Rao accepted your request.", relatedRequestId = "req-1")
        setContent()

        composeTestRule.onNode(hasContentDescription("Unread. Request accepted. Dr. Rao accepted your request.")).assertExists()

        composeTestRule.onNodeWithText("Request accepted").performClick()

        composeTestRule.onNode(hasContentDescription("Request accepted. Dr. Rao accepted your request.")).assertExists()
        assertTrue(notificationsRepository.notifications.single().isRead)
        assert(navigatedToRequestId == "req-1")
    }

    @Test
    fun markAllAsRead_clearsEveryUnreadNotificationAtOnce() {
        notificationsRepository.notifications += aNotification(id = "n1", title = "First", body = "First body")
        notificationsRepository.notifications += aNotification(id = "n2", title = "Second", body = "Second body")
        setContent()

        composeTestRule.onNodeWithText("Mark all as read").performClick()

        composeTestRule.onNode(hasContentDescription("First. First body")).assertExists()
        composeTestRule.onNode(hasContentDescription("Second. Second body")).assertExists()
        assertTrue(notificationsRepository.notifications.all { it.isRead })
    }

    @Test
    fun emptyNotifications_showsAnEmptyState() {
        setContent()

        composeTestRule.onNodeWithText("You're all caught up.").assertExists()
    }

    private fun aNotification(
        id: String,
        title: String,
        body: String,
        relatedRequestId: String? = null,
    ) = Notification(
        id = id,
        userId = FakeAuthRepository.FAKE_USER_ID,
        type = NotificationType.REQUEST_ACCEPTED,
        title = title,
        body = body,
        relatedRequestId = relatedRequestId,
        readAt = null,
        isRead = false,
        createdAt = SOME_INSTANT,
    )
}
