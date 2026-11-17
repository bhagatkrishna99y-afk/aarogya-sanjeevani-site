package com.aarogya.sanjeevani.patient.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.patient.PatientApp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [AuthGateEffect] through the real [PatientApp] composable --
 * no Hilt, no real [com.aarogya.sanjeevani.core.data.auth.AuthRepository]:
 * [PatientApp] takes [AuthState] as a plain parameter specifically so
 * this can drive it directly (see [PatientApp]'s own doc comment).
 *
 * Instrumented (connected device/emulator) rather than local/Robolectric
 * -- [androidx.compose.ui.test.junit4.createAndroidComposeRule] needs
 * one, matching the other Compose UI test dependencies this module
 * already declares.
 */
@RunWith(AndroidJUnit4::class)
class AuthGateTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController
    private lateinit var authState: MutableState<AuthState>

    private fun setContent(initialState: AuthState) {
        composeTestRule.setContent {
            authState = remember { mutableStateOf(initialState) }
            val context = LocalContext.current
            navController =
                remember {
                    TestNavHostController(context).apply {
                        navigatorProvider.addNavigator(ComposeNavigator())
                    }
                }
            AarogyaTheme {
                PatientApp(authState = authState.value, isOffline = false, navController = navController)
            }
        }
    }

    @Test
    fun loadingStaysOnSplash() {
        setContent(AuthState.Loading)
        composeTestRule.waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<Splash>() == true)
    }

    @Test
    fun unauthenticatedRoutesToWelcome() {
        setContent(AuthState.Loading)
        composeTestRule.waitForIdle()

        authState.value = AuthState.Unauthenticated
        composeTestRule.waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<Welcome>() == true)
    }

    @Test
    fun authenticatedRoutesToHome() {
        setContent(AuthState.Loading)
        composeTestRule.waitForIdle()

        authState.value = AuthState.Authenticated(userId = "user-1")
        composeTestRule.waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<HomeRoute>() == true)
    }

    @Test
    fun signOutDeepInsideHomeResetsTheBackStack() {
        setContent(AuthState.Authenticated(userId = "user-1"))
        composeTestRule.waitForIdle()

        // Go two screens deep into the Home tab before signing out.
        composeTestRule.runOnUiThread { navController.navigate(DoctorsDirectory) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { navController.navigate(DoctorDetail(doctorId = "doctor-1")) }
        composeTestRule.waitForIdle()

        authState.value = AuthState.Unauthenticated
        composeTestRule.waitForIdle()

        assertTrue(navController.currentDestination?.hasRoute<Welcome>() == true)

        // The reset actually cleared the stack rather than merely
        // navigating to Welcome on top of it -- nothing from the
        // signed-in session's back stack should still be present.
        val remainingRoutes = navController.currentBackStack.value.map { it.destination }
        assertTrue(remainingRoutes.none { it.hasRoute<DoctorDetail>() })
        assertTrue(remainingRoutes.none { it.hasRoute<DoctorsDirectory>() })
        assertTrue(remainingRoutes.none { it.hasRoute<HomeGraph>() })
    }
}
