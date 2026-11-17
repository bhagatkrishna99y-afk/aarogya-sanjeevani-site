package com.aarogya.sanjeevani.core.common

/** App-wide constants with no natural home in a specific feature. Keep
 *  this small -- a per-feature constant belongs next to that feature,
 *  not here. */
object AppConstants {
    const val DEFAULT_PAGE_SIZE = 20
    const val NETWORK_TIMEOUT_MILLIS = 15_000L
    const val SESSION_PREFS_NAME = "aarogya_session_prefs"
}
