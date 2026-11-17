package com.aarogya.sanjeevani.patient.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination

/** `true` if [this] (or any of its parent graphs) is [T] -- the one
 *  check both [PatientBottomBar] (which tab is selected) and
 *  [AuthGateEffect] (are we already inside this top-level area) need,
 *  checked against the *whole* hierarchy rather than just the top entry
 *  so being several screens deep still counts. */
internal inline fun <reified T : Any> NavDestination?.isInGraph(): Boolean {
    return this?.hierarchy?.any { it.hasRoute<T>() } == true
}

/**
 * The standard "switch bottom-nav tab" navigation call: pops back to
 * this [androidx.navigation.NavGraph]'s start destination *saving* the
 * popped tab's state, then navigates to [route] *restoring* its own --
 * this is what makes switching tabs preserve each tab's own back stack
 * and scroll position instead of resetting it every time. Not just for
 * [PatientBottomBar]'s own taps -- any destination that offers a
 * shortcut *into* a tab (Home's "Browse Services" card, e.g.) should
 * use this too, rather than a plain `navController.navigate(route)`,
 * for the exact same reason.
 */
internal inline fun <reified T : Any> NavController.navigateToTab(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
