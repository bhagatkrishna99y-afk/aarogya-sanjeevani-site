package com.aarogya.sanjeevani.patient.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.patient.HiltTestActivity
import com.aarogya.sanjeevani.patient.PatientApp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the bottom bar's `navigateToTab` (see `PatientBottomBar.kt`)
 * through the real [PatientApp] composable, starting straight at
 * [HomeGraph] -- an already-authenticated state the auth gate has no
 * reason to navigate away from, so it never interferes with the clicks
 * this test drives. Instrumented, same reasoning as [AuthGateTest]
 * (including needing `@HiltAndroidTest`/[HiltTestActivity] rather than a
 * plain `ComponentActivity`, since Home is a real `hiltViewModel()`
 * screen).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TabStateRestorationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            val context = LocalContext.current
            navController =
                remember {
                    TestNavHostController(context).apply {
                        navigatorProvider.addNavigator(ComposeNavigator())
                    }
                }
            AarogyaTheme {
                PatientApp(
                    authState = AuthState.Authenticated(userId = "user-1"),
                    isOffline = false,
                    navController = navController,
                    startDestination = HomeGraph,
                )
            }
        }
    }

    @Test
    fun pushingIntoOneTabThenSwitchingAwayAndBackRestoresIt() {
        // Home tab is selected initially.
        composeTestRule.onNodeWithText("Services").performClick()
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination?.hasRoute<ServicesList>() == true)

        // Push one destination deep into the Services tab -- whichever
        // real service the backend currently has first, the test only
        // cares that *a* service was entered, not which one. The list
        // loads from the real backend asynchronously, so waitForIdle()
        // alone (Compose recomposition idle, not network completion)
        // isn't enough -- wait for a row to actually exist first.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("serviceRow").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithTag("serviceRow").onFirst().performClick()
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination?.hasRoute<ServiceDetail>() == true)

        // Switch to a different tab...
        composeTestRule.onNodeWithText("Requests").performClick()
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination?.hasRoute<RequestsList>() == true)

        // ...and back. Restoring state (rather than resetting it) means
        // Services should still be on ServiceDetail, not back at its own
        // start destination.
        composeTestRule.onNodeWithText("Services").performClick()
        composeTestRule.waitForIdle()
        assertTrue(navController.currentDestination?.hasRoute<ServiceDetail>() == true)
    }

    @Test
    fun switchingTabsNeverResetsTheHomeTabItDidNotVisit() {
        composeTestRule.onNodeWithText("Services").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.waitForIdle()

        // Home was never pushed anywhere -- still its own start
        // destination, same as if it had never left.
        assertTrue(navController.currentDestination?.hasRoute<HomeRoute>() == true)
    }
}
