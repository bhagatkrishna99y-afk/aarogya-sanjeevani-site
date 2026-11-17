package com.aarogya.sanjeevani.patient.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.aarogya.sanjeevani.patient.auth.CheckInboxScreen
import com.aarogya.sanjeevani.patient.auth.WelcomeScreen
import com.aarogya.sanjeevani.patient.auth.forgotpassword.ForgotPasswordRoute
import com.aarogya.sanjeevani.patient.auth.login.LoginRoute
import com.aarogya.sanjeevani.patient.auth.signup.SignUpRoute
import com.aarogya.sanjeevani.patient.booking.RequestFormRoute
import com.aarogya.sanjeevani.patient.booking.ReviewRoute
import com.aarogya.sanjeevani.patient.booking.SuccessRoute
import com.aarogya.sanjeevani.patient.doctors.AvailabilityRoute
import com.aarogya.sanjeevani.patient.doctors.DoctorDetailRoute
import com.aarogya.sanjeevani.patient.doctors.DoctorsDirectoryRoute
import com.aarogya.sanjeevani.patient.home.HomeRoute
import com.aarogya.sanjeevani.patient.notifications.NotificationsListRoute
import com.aarogya.sanjeevani.patient.profile.DeleteAccountInfoScreen
import com.aarogya.sanjeevani.patient.profile.EditProfileRoute
import com.aarogya.sanjeevani.patient.profile.ProfileRoute
import com.aarogya.sanjeevani.patient.requests.RequestDetailRoute
import com.aarogya.sanjeevani.patient.requests.RequestsListRoute
import com.aarogya.sanjeevani.patient.services.DoctorListForServiceRoute
import com.aarogya.sanjeevani.patient.services.ServiceDetailRoute
import com.aarogya.sanjeevani.patient.services.ServicesListRoute
import com.aarogya.sanjeevani.patient.splash.SplashScreen

/**
 * The entire patient app's navigation graph -- one function per nav
 * graph area below (see `PatientRoutes.kt` for the route shapes and why
 * some destinations ([com.aarogya.sanjeevani.patient.navigation.DoctorDetail]/[Availability])
 * are deliberately declared twice). [navController] and [startDestination]
 * come from [com.aarogya.sanjeevani.patient.PatientApp] rather than being
 * created here, so a test can supply a [androidx.navigation.testing.TestNavHostController]
 * and drive [startDestination] directly without needing Hilt or a real
 * [com.aarogya.sanjeevani.core.data.auth.AuthRepository] anywhere in this
 * composable tree -- every destination registered below takes its
 * dependencies as plain callbacks/[androidx.hilt.navigation.compose.hiltViewModel]
 * defaults, never a [NavHostController] itself.
 */
@Composable
fun PatientNavHost(
    navController: NavHostController,
    startDestination: Any,
    snackbarHostState: SnackbarHostState,
    onSignOutClick: () -> Unit,
    unreadNotificationCount: Int = 0,
    onRefreshUnreadCount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Splash> { SplashScreen() }
        authGraph(navController, snackbarHostState)
        homeGraph(navController, snackbarHostState, onSignOutClick, unreadNotificationCount, onRefreshUnreadCount)
    }
}

private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    navigation<AuthGraph>(startDestination = Welcome) {
        composable<Welcome> {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Login) },
                onSignUpClick = { navController.navigate(SignUp) },
            )
        }
        composable<Login> {
            LoginRoute(
                onNavigateToSignUp = { navController.navigate(SignUp) },
                onNavigateToForgotPassword = { navController.navigate(ForgotPassword) },
                snackbarHostState = snackbarHostState,
            )
        }
        composable<SignUp> {
            SignUpRoute(
                onBack = navController::popBackStack,
                onNavigateToCheckInbox = { email ->
                    navController.navigate(CheckInbox(email)) {
                        popUpTo<Welcome>()
                    }
                },
                snackbarHostState = snackbarHostState,
            )
        }
        composable<ForgotPassword> {
            ForgotPasswordRoute(onBack = navController::popBackStack, snackbarHostState = snackbarHostState)
        }
        composable<CheckInbox> { backStackEntry ->
            val route = backStackEntry.toRoute<CheckInbox>()
            CheckInboxScreen(
                email = route.email,
                onBackToLogin = {
                    navController.navigate(Login) {
                        popUpTo<Welcome>()
                    }
                },
            )
        }
    }
}

private fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onSignOutClick: () -> Unit,
    unreadNotificationCount: Int,
    onRefreshUnreadCount: () -> Unit,
) {
    navigation<HomeGraph>(startDestination = HomeTabGraph) {
        homeTabGraph(navController, snackbarHostState, unreadNotificationCount)
        servicesTabGraph(navController, snackbarHostState)
        requestsTabGraph(navController, snackbarHostState)
        notificationsTabGraph(navController, snackbarHostState, onRefreshUnreadCount)
        profileTabGraph(navController, snackbarHostState, onSignOutClick)
    }
}

private fun NavGraphBuilder.homeTabGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    unreadNotificationCount: Int,
) {
    navigation<HomeTabGraph>(startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeRoute(
                onBrowseServicesClick = { navController.navigateToTab(ServicesTabGraph) },
                onFindDoctorClick = { navController.navigate(DoctorsDirectory) },
                onCategoryClick = { categoryId ->
                    navController.navigateToTab(ServicesList(categoryId = categoryId))
                },
                onViewAllRequestsClick = { navController.navigateToTab(RequestsTabGraph) },
                onRequestClick = { requestId -> navController.navigate(RequestDetail(requestId)) },
                onDoctorClick = { doctorId -> navController.navigate(DoctorDetail(doctorId)) },
                onNotificationsClick = { navController.navigateToTab(NotificationsTabGraph) },
                onProfileClick = { navController.navigateToTab(ProfileTabGraph) },
                snackbarHostState = snackbarHostState,
                unreadNotificationCount = unreadNotificationCount,
            )
        }
        composable<DoctorsDirectory> {
            DoctorsDirectoryRoute(
                onBack = navController::popBackStack,
                onDoctorClick = { doctorId -> navController.navigate(DoctorDetail(doctorId)) },
            )
        }
        doctorDetailAndAvailability(navController, snackbarHostState)
        bookingFlow(
            navController = navController,
            snackbarHostState = snackbarHostState,
            onDoneClick = { navController.popBackStack<HomeRoute>(inclusive = false) },
            onDoctorUnavailable = { navController.popBackStack<DoctorsDirectory>(inclusive = false) },
        )
    }
}

/** [DoctorDetail] and [Availability] -- shared verbatim between
 *  [homeTabGraph] and [servicesTabGraph] (see `PatientRoutes.kt`'s doc
 *  comment on [DoctorDetail] for why they're still registered twice,
 *  once per tab, rather than this function itself being shared across
 *  a single registration). */
private fun NavGraphBuilder.doctorDetailAndAvailability(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    composable<DoctorDetail> { backStackEntry ->
        val route = backStackEntry.toRoute<DoctorDetail>()
        DoctorDetailRoute(
            doctorId = route.doctorId,
            serviceId = route.serviceId,
            serviceName = route.serviceName,
            onBack = navController::popBackStack,
            onViewAvailabilityClick = { serviceId, serviceName, doctorName ->
                navController.navigate(
                    Availability(
                        doctorId = route.doctorId,
                        doctorName = doctorName,
                        serviceId = serviceId,
                        serviceName = serviceName,
                    ),
                )
            },
            snackbarHostState = snackbarHostState,
        )
    }
    composable<Availability> { backStackEntry ->
        val route = backStackEntry.toRoute<Availability>()
        AvailabilityRoute(
            doctorId = route.doctorId,
            doctorName = route.doctorName,
            serviceName = route.serviceName,
            onBack = navController::popBackStack,
            onContinue = { slotStart, slotDurationMinutes ->
                navController.navigate(
                    RequestForm(
                        serviceId = route.serviceId,
                        serviceName = route.serviceName,
                        doctorId = route.doctorId,
                        doctorName = route.doctorName,
                        slotStart = slotStart.toString(),
                        slotDurationMinutes = slotDurationMinutes,
                    ),
                )
            },
            snackbarHostState = snackbarHostState,
        )
    }
}

private fun NavGraphBuilder.servicesTabGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    navigation<ServicesTabGraph>(startDestination = ServicesList()) {
        composable<ServicesList> { backStackEntry ->
            val route = backStackEntry.toRoute<ServicesList>()
            ServicesListRoute(
                categoryId = route.categoryId,
                onServiceClick = { serviceId -> navController.navigate(ServiceDetail(serviceId)) },
            )
        }
        composable<ServiceDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<ServiceDetail>()
            ServiceDetailRoute(
                serviceId = route.serviceId,
                onBack = navController::popBackStack,
                onFindDoctorsClick = { serviceName ->
                    navController.navigate(DoctorListForService(route.serviceId, serviceName))
                },
                onContinueWithoutDoctorClick = { serviceName ->
                    navController.navigate(RequestForm(serviceId = route.serviceId, serviceName = serviceName))
                },
            )
        }
        composable<DoctorListForService> { backStackEntry ->
            val route = backStackEntry.toRoute<DoctorListForService>()
            DoctorListForServiceRoute(
                serviceId = route.serviceId,
                serviceName = route.serviceName,
                onBack = navController::popBackStack,
                onBackToServices = { navController.popBackStack<ServicesList>(inclusive = false) },
                onDoctorClick = { doctorId ->
                    navController.navigate(DoctorDetail(doctorId, route.serviceId, route.serviceName))
                },
            )
        }
        doctorDetailAndAvailability(navController, snackbarHostState)
        bookingFlow(
            navController = navController,
            snackbarHostState = snackbarHostState,
            onDoneClick = {
                // Type-based (not `route = ServicesList(...)`): the actual
                // ServicesList entry on the back stack may carry a non-null
                // categoryId (reached via Home's category row) that this
                // call has no reason to know or match on.
                navController.popBackStack<ServicesList>(inclusive = false)
            },
            onDoctorUnavailable = { navController.popBackStack<DoctorListForService>(inclusive = false) },
        )
    }
}

/**
 * [onDoneClick] and [onDoctorUnavailable] are the two steps that
 * genuinely differ per tab graph (see the two call sites: "home" means
 * [HomeRoute]/[DoctorsDirectory], "services" means [ServicesList]/
 * [DoctorListForService]) -- everything else here is identical
 * regardless of which tab's back stack it's registered into, so only
 * those two are parameters rather than this whole function being
 * duplicated by hand. [Review] and [Success] both reuse [RequestForm]'s
 * own `RequestFormViewModel` instance via its backstack entry (see
 * `PatientRoutes.kt`'s doc comment on [RequestForm]) -- neither
 * registers its own ViewModel.
 */
private fun NavGraphBuilder.bookingFlow(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onDoneClick: () -> Unit,
    onDoctorUnavailable: () -> Unit,
) {
    composable<RequestForm> {
        RequestFormRoute(
            onBack = navController::popBackStack,
            onReviewClick = { navController.navigate(Review) },
        )
    }
    composable<Review> { backStackEntry ->
        val formEntry = remember(backStackEntry) { navController.getBackStackEntry<RequestForm>() }
        ReviewRoute(
            viewModel = hiltViewModel(viewModelStoreOwner = formEntry),
            onBack = navController::popBackStack,
            onEditClick = navController::popBackStack,
            onSlotUnavailable = { navController.popBackStack<Availability>(inclusive = false) },
            onDoctorUnavailable = onDoctorUnavailable,
            onSubmitted = { navController.navigate(Success) },
            snackbarHostState = snackbarHostState,
        )
    }
    composable<Success> { backStackEntry ->
        val formEntry = remember(backStackEntry) { navController.getBackStackEntry<RequestForm>() }
        SuccessRoute(
            viewModel = hiltViewModel(viewModelStoreOwner = formEntry),
            onDoneClick = onDoneClick,
            onViewRequestClick = { requestId ->
                onDoneClick()
                navController.navigate(RequestDetail(requestId))
            },
        )
    }
}

private fun NavGraphBuilder.requestsTabGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
) {
    navigation<RequestsTabGraph>(startDestination = RequestsList) {
        composable<RequestsList> {
            RequestsListRoute(
                onRequestClick = { requestId -> navController.navigate(RequestDetail(requestId)) },
                onBrowseServicesClick = { navController.navigateToTab(ServicesTabGraph) },
                snackbarHostState = snackbarHostState,
            )
        }
        composable<RequestDetail>(
            deepLinks = listOf(navDeepLink<RequestDetail>(basePath = "aarogya://request")),
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<RequestDetail>()
            RequestDetailRoute(
                requestId = route.requestId,
                onBack = navController::popBackStack,
                onBookAgainClick = { serviceId -> navController.navigate(ServiceDetail(serviceId)) },
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

private fun NavGraphBuilder.notificationsTabGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onRefreshUnreadCount: () -> Unit,
) {
    navigation<NotificationsTabGraph>(startDestination = NotificationsList) {
        composable<NotificationsList> {
            NotificationsListRoute(
                onNavigateToRequestDetail = { requestId -> navController.navigate(RequestDetail(requestId)) },
                onRefreshUnreadCount = onRefreshUnreadCount,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

private fun NavGraphBuilder.profileTabGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onSignOutClick: () -> Unit,
) {
    navigation<ProfileTabGraph>(startDestination = ProfileRoute) {
        composable<ProfileRoute> {
            ProfileRoute(
                onEditProfileClick = { navController.navigate(EditProfile) },
                onSignOutClick = onSignOutClick,
                onDeleteAccountClick = { navController.navigate(DeleteAccountInfo) },
                snackbarHostState = snackbarHostState,
            )
        }
        composable<EditProfile> {
            EditProfileRoute(onBack = navController::popBackStack, snackbarHostState = snackbarHostState)
        }
        composable<DeleteAccountInfo> {
            DeleteAccountInfoScreen(onBack = navController::popBackStack)
        }
    }
}
