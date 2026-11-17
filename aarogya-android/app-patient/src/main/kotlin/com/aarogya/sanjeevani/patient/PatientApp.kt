package com.aarogya.sanjeevani.patient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.designsystem.component.AsSnackbarHost
import com.aarogya.sanjeevani.core.ui.connectivity.OfflineBanner
import com.aarogya.sanjeevani.patient.auth.AuthGateViewModel
import com.aarogya.sanjeevani.patient.connectivity.ConnectivityViewModel
import com.aarogya.sanjeevani.patient.navigation.AuthGateEffect
import com.aarogya.sanjeevani.patient.navigation.HomeGraph
import com.aarogya.sanjeevani.patient.navigation.PatientBottomBar
import com.aarogya.sanjeevani.patient.navigation.PatientNavHost
import com.aarogya.sanjeevani.patient.navigation.Splash
import com.aarogya.sanjeevani.patient.navigation.isInGraph
import com.aarogya.sanjeevani.patient.notifications.NotificationBadgeViewModel

/**
 * The entire patient app, minus anything Hilt -- [navController],
 * [authState] and [isOffline] are all plain parameters rather than
 * resolved internally via `hiltViewModel()`, which is what lets a test
 * drive this directly with a [androidx.navigation.testing.TestNavHostController]
 * and a hand-picked [AuthState]/[startDestination] with no DI setup at
 * all (see `AuthGateTest.kt`/`TabStateRestorationTest.kt`). [PatientAppRoot]
 * below is the thin, Hilt-aware wrapper [MainActivity] actually calls.
 *
 * Insets: this `Scaffold`'s own `contentWindowInsets` is explicitly
 * zeroed and [Modifier.consumeWindowInsets] marks [padding] as already
 * "spent" on the bottom bar/snackbar -- without both, a per-screen
 * `Scaffold`/`TopAppBar` further down (e.g. [com.aarogya.sanjeevani.patient.auth.login.LoginScreen])
 * would pad for the status bar *again*, on top of what this one already
 * reserved, double-counting it. [MainActivity] additionally calls
 * `enableEdgeToEdge()` so the system bars are actually transparent for
 * any of this to matter.
 *
 * [windowWidthSizeClass] is [WindowWidthSizeClass.Compact] by default --
 * every existing caller (previews, [com.aarogya.sanjeevani.patient.navigation.AuthGateTest],
 * [com.aarogya.sanjeevani.patient.navigation.TabStateRestorationTest]) keeps
 * today's edge-to-edge single-column layout without having to compute or
 * pass one. [PatientAppRoot] is the one real caller that does, from
 * [MainActivity]'s own `calculateWindowSizeClass`: on a tablet-width
 * ([WindowWidthSizeClass.Expanded]) screen, every destination's content
 * is capped at [MAX_CONTENT_WIDTH] and centered rather than stretching
 * full-bleed, the same adaptive treatment regardless of which screen is
 * showing -- applying it once here, rather than in every individual
 * screen, is what keeps that guarantee app-wide.
 */
@Composable
fun PatientApp(
    authState: AuthState,
    isOffline: Boolean,
    onSignOutClick: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Splash,
    unreadNotificationCount: Int = 0,
    onAppResumed: () -> Unit = {},
    onAppPaused: () -> Unit = {},
    onRefreshUnreadCount: () -> Unit = {},
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    AuthGateEffect(navController = navController, authState = authState)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onAppResumed() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { onAppPaused() }

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val showBottomBar = currentDestination.isInGraph<HomeGraph>()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { AsSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                PatientBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    unreadNotificationCount = unreadNotificationCount,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        ) {
            OfflineBanner(visible = isOffline)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val navHostModifier =
                    if (windowWidthSizeClass == WindowWidthSizeClass.Expanded) {
                        Modifier.fillMaxSize().widthIn(max = MAX_CONTENT_WIDTH)
                    } else {
                        Modifier.fillMaxSize()
                    }
                PatientNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    snackbarHostState = snackbarHostState,
                    onSignOutClick = onSignOutClick,
                    unreadNotificationCount = unreadNotificationCount,
                    onRefreshUnreadCount = onRefreshUnreadCount,
                    modifier = navHostModifier,
                )
            }
        }
    }
}

/** A tablet-width screen keeps every form/list readable rather than
 *  letting a single Column of text stretch edge-to-edge -- the same
 *  reasoning most single-pane adaptive layouts use for a "reading
 *  width" cap, short of this app's screens being worth a full
 *  multi-pane rebuild. */
private val MAX_CONTENT_WIDTH = 600.dp

/** What [MainActivity] actually calls -- resolves the two app-wide
 *  ViewModels [PatientApp] needs via Hilt and collects their state with
 *  [collectAsStateWithLifecycle] so both pause while the app is
 *  backgrounded. [windowWidthSizeClass] comes straight from
 *  [MainActivity]'s own `calculateWindowSizeClass` -- this function
 *  exists so that call stays the one and only place this app computes
 *  it, rather than every destination re-deriving it from
 *  [androidx.compose.ui.platform.LocalConfiguration] independently. */
@Composable
fun PatientAppRoot(windowWidthSizeClass: WindowWidthSizeClass) {
    val authGateViewModel: AuthGateViewModel = hiltViewModel()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val notificationBadgeViewModel: NotificationBadgeViewModel = hiltViewModel()
    val authState by authGateViewModel.authState.collectAsStateWithLifecycle()
    val isOffline by connectivityViewModel.isOffline.collectAsStateWithLifecycle()
    val unreadNotificationCount by notificationBadgeViewModel.unreadCount.collectAsStateWithLifecycle()

    PatientApp(
        authState = authState,
        isOffline = isOffline,
        onSignOutClick = authGateViewModel::signOut,
        unreadNotificationCount = unreadNotificationCount,
        onAppResumed = notificationBadgeViewModel::resume,
        onAppPaused = notificationBadgeViewModel::pause,
        onRefreshUnreadCount = notificationBadgeViewModel::refresh,
        windowWidthSizeClass = windowWidthSizeClass,
    )
}
