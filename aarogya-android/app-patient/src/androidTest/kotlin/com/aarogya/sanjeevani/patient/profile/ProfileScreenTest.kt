package com.aarogya.sanjeevani.patient.profile

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.data.model.UserRole
import com.aarogya.sanjeevani.core.data.profile.Profile
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val SOME_INSTANT = Instant.parse("2026-01-01T00:00:00Z")

/**
 * Exercises the stateless [ProfileScreen] directly -- the one piece of
 * real interaction this screen owns itself outside of its ViewModel:
 * the logout confirmation dialog (an accidental tap on "Log out" must
 * never sign anyone out without a confirm step in between).
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val profile =
        Profile(
            id = "user-1",
            role = UserRole.USER,
            fullName = "Jane Doe",
            phone = "9876543210",
            email = "jane@example.com",
            avatarUrl = null,
            isSuspended = false,
            createdAt = SOME_INSTANT,
            updatedAt = SOME_INSTANT,
        )

    private fun setContent(onSignOutClick: () -> Unit) {
        composeTestRule.setContent {
            AarogyaTheme {
                ProfileScreen(
                    uiState =
                        ProfileUiState(
                            section = ProfileSectionState.Content(profile = profile, avatarUrl = null),
                            appearanceMode = AppearanceMode.SYSTEM,
                        ),
                    onAction = {},
                    onEditProfileClick = {},
                    onSignOutClick = onSignOutClick,
                    onDeleteAccountClick = {},
                    onAvatarClick = null,
                )
            }
        }
    }

    @Test
    fun tappingLogOut_showsAConfirmationDialog_andDoesNotSignOutUntilConfirmed() {
        var signedOut = false
        setContent(onSignOutClick = { signedOut = true })

        composeTestRule.onNodeWithText("Log out").performClick()

        composeTestRule.onNodeWithText("Log out?").assertExists()
        assertEquals(false, signedOut)
    }

    @Test
    fun dismissingTheLogOutDialog_doesNotSignOut() {
        var signedOut = false
        setContent(onSignOutClick = { signedOut = true })

        composeTestRule.onNodeWithText("Log out").performClick()
        composeTestRule.onNode(hasText("Cancel").and(hasAnyAncestor(isDialog()))).performClick()

        composeTestRule.onNodeWithText("Log out?").assertDoesNotExist()
        assertEquals(false, signedOut)
    }

    @Test
    fun confirmingTheLogOutDialog_callsOnSignOutClick() {
        var signedOut = false
        setContent(onSignOutClick = { signedOut = true })

        composeTestRule.onNodeWithText("Log out").performClick()
        composeTestRule
            .onNode(hasText("Log out").and(hasClickAction()).and(hasAnyAncestor(isDialog())))
            .performClick()

        assertEquals(true, signedOut)
        composeTestRule.onNodeWithText("Log out?").assertDoesNotExist()
    }
}
