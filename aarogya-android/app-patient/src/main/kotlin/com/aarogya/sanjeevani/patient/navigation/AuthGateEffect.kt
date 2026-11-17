package com.aarogya.sanjeevani.patient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.aarogya.sanjeevani.core.data.auth.AuthState

/**
 * The entire auth gate, as one reactive rule: whenever [authState]
 * settles on [AuthState.Authenticated] and the user isn't already
 * somewhere inside [HomeGraph], clear the back stack and land on
 * [HomeGraph]; whenever it settles on [AuthState.Unauthenticated] and
 * the user isn't already inside [AuthGraph], clear the back stack and
 * land on [Welcome]. [AuthState.Loading] does nothing -- [Splash] (the
 * app's start destination) is simply left showing.
 *
 * This one rule covers every case the task it was built under called
 * out by name:
 * - Cold start, `Loading` -> `Authenticated`/`Unauthenticated`: routes
 *   away from [Splash] for the first and only time.
 * - A successful sign-in/sign-up from deep in [AuthGraph]: once
 *   [AuthState] flips, this routes to [HomeGraph] -- see
 *   `LoginContract.kt`'s doc comment on why the login screen itself
 *   doesn't also try to navigate.
 * - "Respond to sign-out anywhere": a sign-out from
 *   [com.aarogya.sanjeevani.patient.profile.ProfileScreen], or the Auth
 *   plugin itself giving up on an unrecoverable token refresh, both
 *   just flip [AuthState] to `Unauthenticated` -- this rule reacts the
 *   same way regardless of *why* that happened or *where* in [HomeGraph]
 *   the user was.
 *
 * `popUpTo(0) { inclusive = true }` -- rather than popping up to a
 * named destination -- is what "resetting the back stack" actually
 * means here: there is no destination left standing to pop up *to*,
 * the entire previous graph (every [HomeGraph] tab's saved state
 * included) is gone.
 */
@Composable
fun AuthGateEffect(
    navController: NavHostController,
    authState: AuthState,
) {
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Loading -> Unit
            is AuthState.Authenticated -> {
                if (!navController.currentDestination.isInGraph<HomeGraph>()) {
                    navController.navigate(HomeGraph) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                if (!navController.currentDestination.isInGraph<AuthGraph>()) {
                    navController.navigate(AuthGraph) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
