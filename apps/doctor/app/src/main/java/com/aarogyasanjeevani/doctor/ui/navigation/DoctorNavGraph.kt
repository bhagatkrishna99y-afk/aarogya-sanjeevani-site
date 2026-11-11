package com.aarogyasanjeevani.doctor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aarogyasanjeevani.doctor.ui.application.ApplicationGate
import com.aarogyasanjeevani.doctor.ui.auth.AuthViewModel
import com.aarogyasanjeevani.doctor.ui.auth.LoginScreen
import com.aarogyasanjeevani.doctor.ui.auth.SignUpScreen
import com.aarogyasanjeevani.doctor.ui.availability.AvailabilityScreen
import com.aarogyasanjeevani.doctor.ui.common.LoadingState
import com.aarogyasanjeevani.doctor.ui.notifications.NotificationsScreen
import com.aarogyasanjeevani.doctor.ui.profile.ProfileScreen
import com.aarogyasanjeevani.doctor.ui.requests.RequestsScreen
import com.aarogyasanjeevani.doctor.ui.services.ServicesScreen
import io.github.jan.supabase.auth.SessionStatus

private val bottomNavRoutes =
    setOf(Routes.REQUESTS, Routes.SERVICES, Routes.AVAILABILITY, Routes.NOTIFICATIONS, Routes.PROFILE)

@Composable
fun DoctorApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                DoctorBottomBar(navController)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SPLASH) { SplashRoute(navController) }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onSignedIn = { navController.navigate(Routes.APPLICATION_GATE) { popUpTo(0) } },
                    onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) },
                )
            }
            composable(Routes.SIGN_UP) {
                SignUpScreen(
                    onSignedIn = { navController.navigate(Routes.APPLICATION_GATE) { popUpTo(0) } },
                    onNavigateToLogin = { navController.popBackStack() },
                )
            }

            composable(Routes.APPLICATION_GATE) {
                ApplicationGate(
                    onApproved = {
                        navController.navigate(Routes.REQUESTS) { popUpTo(Routes.APPLICATION_GATE) { inclusive = true } }
                    },
                    onSignOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } },
                )
            }

            composable(Routes.REQUESTS) { RequestsScreen() }
            composable(Routes.SERVICES) { ServicesScreen() }
            composable(Routes.AVAILABILITY) { AvailabilityScreen() }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(onSignedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } })
            }
        }
    }
}

@Composable
private fun SplashRoute(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionStatus by authViewModel.sessionStatus.collectAsState()

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> navController.navigate(Routes.APPLICATION_GATE) { popUpTo(0) }
            is SessionStatus.NotAuthenticated -> navController.navigate(Routes.LOGIN) { popUpTo(0) }
            else -> Unit // still resolving the stored session -- keep showing the splash spinner
        }
    }

    LoadingState()
}

@Composable
private fun DoctorBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val items = listOf(
        Triple(Routes.REQUESTS, "Requests", Icons.AutoMirrored.Filled.List),
        Triple(Routes.SERVICES, "Services", Icons.Filled.MedicalServices),
        Triple(Routes.AVAILABILITY, "Availability", Icons.Filled.CalendarMonth),
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
