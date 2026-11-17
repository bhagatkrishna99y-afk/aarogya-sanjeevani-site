package com.aarogya.sanjeevani.patient.ui.common

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Opens [url] in a Custom Tab -- the right way to open an external web
 *  page from a link inside the app: it keeps this app's toolbar/back
 *  behavior (unlike handing off to a full browser app) while still
 *  being the user's actual, already-signed-in browser underneath
 *  (unlike a `WebView`, which would need its own separate cookie jar/
 *  session). Used for the Welcome screen's Privacy Policy link; nothing
 *  else in this app links out to the website yet. */
fun Context.openInCustomTab(url: String) {
    CustomTabsIntent.Builder().build().launchUrl(this, url.toUri())
}
