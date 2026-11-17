package com.aarogya.sanjeevani.patient.notifications

const val UNREAD_BADGE_MAX = 99

/** "3", "99+" -- shared by [com.aarogya.sanjeevani.patient.home.HomeScreen]'s
 *  bell and [com.aarogya.sanjeevani.patient.navigation.PatientBottomBar]'s
 *  Notifications tab, so both ever cap at the same number. `null` for
 *  zero -- the caller decides whether that means "draw nothing." */
fun formatUnreadBadgeCount(count: Int): String? =
    when {
        count <= 0 -> null
        count > UNREAD_BADGE_MAX -> "$UNREAD_BADGE_MAX+"
        else -> "$count"
    }
