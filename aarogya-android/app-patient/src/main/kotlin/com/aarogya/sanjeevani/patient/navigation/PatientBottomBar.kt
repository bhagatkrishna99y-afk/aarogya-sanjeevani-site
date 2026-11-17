package com.aarogya.sanjeevani.patient.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.aarogya.sanjeevani.core.designsystem.component.AsBottomBar
import com.aarogya.sanjeevani.core.designsystem.component.AsBottomBarItem
import com.aarogya.sanjeevani.patient.notifications.formatUnreadBadgeCount

/**
 * One entry in [AsBottomBar] -- bundles a tab's icons with the two
 * things that make tab state restoration work: [isSelected] (checked
 * against the *whole* back-stack hierarchy, not just the top entry, so
 * being three screens deep in Services still highlights the Services
 * tab) and [navigate] (always [navigateToTab], never a plain
 * `navController.navigate(route)`). [badgeText], given the unread
 * count, is `null` for every tab but Notifications -- a function
 * rather than a plain field so this whole list can stay a `val`
 * computed once, with the one genuinely dynamic bit (the count) read
 * fresh on every [PatientBottomBar] recomposition.
 */
private data class PatientBottomBarTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val isSelected: (NavDestination?) -> Boolean,
    val navigate: (NavController) -> Unit,
    val badgeText: (unreadNotificationCount: Int) -> String? = { null },
)

private val patientBottomBarTabs =
    listOf(
        PatientBottomBarTab(
            label = "Home",
            icon = Icons.Outlined.Home,
            selectedIcon = Icons.Filled.Home,
            isSelected = { it.isInGraph<HomeTabGraph>() },
            navigate = { it.navigateToTab(HomeTabGraph) },
        ),
        PatientBottomBarTab(
            label = "Services",
            icon = Icons.Outlined.MedicalServices,
            selectedIcon = Icons.Filled.MedicalServices,
            isSelected = { it.isInGraph<ServicesTabGraph>() },
            navigate = { it.navigateToTab(ServicesTabGraph) },
        ),
        PatientBottomBarTab(
            label = "Requests",
            icon = Icons.Outlined.CalendarMonth,
            selectedIcon = Icons.Filled.CalendarMonth,
            isSelected = { it.isInGraph<RequestsTabGraph>() },
            navigate = { it.navigateToTab(RequestsTabGraph) },
        ),
        PatientBottomBarTab(
            label = "Notifications",
            icon = Icons.Outlined.Notifications,
            selectedIcon = Icons.Filled.Notifications,
            isSelected = { it.isInGraph<NotificationsTabGraph>() },
            navigate = { it.navigateToTab(NotificationsTabGraph) },
            badgeText = { count -> formatUnreadBadgeCount(count) },
        ),
        PatientBottomBarTab(
            label = "Profile",
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person,
            isSelected = { it.isInGraph<ProfileTabGraph>() },
            navigate = { it.navigateToTab(ProfileTabGraph) },
        ),
    )

@Composable
fun PatientBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    unreadNotificationCount: Int,
    modifier: Modifier = Modifier,
) {
    AsBottomBar(
        modifier = modifier,
        items =
            patientBottomBarTabs.map {
                AsBottomBarItem(it.label, it.icon, it.selectedIcon, it.badgeText(unreadNotificationCount))
            },
        selectedIndex = patientBottomBarTabs.indexOfFirst { it.isSelected(currentDestination) }.coerceAtLeast(0),
        onItemSelected = { index -> patientBottomBarTabs[index].navigate(navController) },
    )
}
