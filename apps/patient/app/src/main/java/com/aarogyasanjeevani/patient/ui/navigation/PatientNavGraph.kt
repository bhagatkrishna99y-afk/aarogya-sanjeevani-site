package com.aarogyasanjeevani.patient.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.aarogyasanjeevani.patient.ui.auth.AuthViewModel
import com.aarogyasanjeevani.patient.ui.auth.LoginScreen
import com.aarogyasanjeevani.patient.ui.auth.SignUpScreen
import com.aarogyasanjeevani.patient.ui.booking.BookSlotScreen
import com.aarogyasanjeevani.patient.ui.booking.BookingViewModel
import com.aarogyasanjeevani.patient.ui.booking.DoctorDetailScreen
import com.aarogyasanjeevani.patient.ui.booking.DoctorListScreen
import com.aarogyasanjeevani.patient.ui.booking.ReviewRequestScreen
import com.aarogyasanjeevani.patient.ui.common.LoadingState
import com.aarogyasanjeevani.patient.ui.home.HomeScreen
import com.aarogyasanjeevani.patient.ui.notifications.NotificationsScreen
import com.aarogyasanjeevani.patient.ui.profile.ProfileScreen
import com.aarogyasanjeevani.patient.ui.requests.MyRequestsScreen
import com.aarogyasanjeevani.patient.ui.requests.RequestDetailScreen
import io.github.jan.supabase.auth.SessionStatus

private val bottomNavRoutes = setOf(Routes.HOME, Routes.MY_REQUESTS, Routes.NOTIFICATIONS, Routes.PROFILE)

@Composable
fun PatientApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                PatientBottomBar(navController)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SPLASH) {
                SplashRoute(navController)
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onSignedIn = { navController.navigate(Routes.HOME) { popUpTo(0) } },
                    onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
                )
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(
                    onSignedIn = { navController.navigate(Routes.HOME) { popUpTo(0) } },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onServiceSelected = { service -> navController.navigate(Routes.bookingGraph(service.id)) },
                )
            }
            composable(Routes.MY_REQUESTS) {
                MyRequestsScreen(
                    onRequestSelected = { request -> navController.navigate(Routes.requestDetail(request.id)) },
                )
            }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } })
            }

            composable(
                route = Routes.REQUEST_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_REQUEST_ID) { type = NavType.StringType }),
            ) { entry ->
                val requestId = entry.arguments?.getString(Routes.ARG_REQUEST_ID).orEmpty()
                RequestDetailScreen(requestId = requestId, onBack = { navController.popBackStack() })
            }

            navigation(
                route = Routes.BOOKING_GRAPH,
                startDestination = Routes.DOCTOR_LIST,
                arguments = listOf(navArgument(Routes.ARG_SERVICE_ID) { type = NavType.StringType }),
            ) {
                composable(Routes.DOCTOR_LIST) { entry ->
                    val bookingViewModel = entry.sharedBookingViewModel(navController)
                    val state by bookingViewModel.uiState.collectAsState()
                    DoctorListScreen(
                        uiState = state,
                        onDoctorSelected = {
                            bookingViewModel.selectDoctor(it)
                            navController.navigate(Routes.DOCTOR_DETAIL)
                        },
                        onServiceDoesNotRequireDoctor = {
                            navController.navigate(Routes.BOOK_SLOT) {
                                popUpTo(Routes.DOCTOR_LIST) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                    )
                }
                composable(Routes.DOCTOR_DETAIL) { entry ->
                    val bookingViewModel = entry.sharedBookingViewModel(navController)
                    val state by bookingViewModel.uiState.collectAsState()
                    DoctorDetailScreen(
                        uiState = state,
                        onChooseDate = { navController.navigate(Routes.BOOK_SLOT) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.BOOK_SLOT) { entry ->
                    val bookingViewModel = entry.sharedBookingViewModel(navController)
                    val state by bookingViewModel.uiState.collectAsState()
                    BookSlotScreen(
                        uiState = state,
                        viewModel = bookingViewModel,
                        onSlotChosen = { navController.navigate(Routes.REVIEW_REQUEST) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.REVIEW_REQUEST) { entry ->
                    val bookingViewModel = entry.sharedBookingViewModel(navController)
                    val state by bookingViewModel.uiState.collectAsState()
                    ReviewRequestScreen(
                        uiState = state,
                        viewModel = bookingViewModel,
                        onSubmitted = {
                            navController.navigate(Routes.MY_REQUESTS) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/** Scopes [BookingViewModel] to the booking nested graph's own back stack entry, so every step shares one instance. */
@Composable
private fun androidx.navigation.NavBackStackEntry.sharedBookingViewModel(navController: NavHostController): BookingViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry(Routes.BOOKING_GRAPH) }
    return hiltViewModel(parentEntry)
}

@Composable
private fun SplashRoute(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> navController.navigate(Routes.HOME) { popUpTo(0) }
            is SessionStatus.NotAuthenticated -> navController.navigate(Routes.LOGIN) { popUpTo(0) }
            else -> Unit // still resolving the stored session -- keep showing the splash spinner
        }
    }

    LoadingState()
}

@Composable
private fun PatientBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val items = listOf(
        Triple(Routes.HOME, "Home", Icons.Filled.Home),
        Triple(Routes.MY_REQUESTS, "Requests", Icons.AutoMirrored.Filled.List),
        Triple(Routes.NOTIFICATIONS, "Alerts", Icons.Filled.Notifications),
        Triple(Routes.PROFILE, "Profile", Icons.Filled.Person),
    )

    NavigationBar {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
