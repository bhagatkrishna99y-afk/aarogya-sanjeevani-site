package com.aarogya.sanjeevani.patient.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens whatever the device's default mail app is, prefilled --
 *  [Intent.ACTION_SENDTO] with a `mailto:` URI, rather than
 *  `ACTION_SEND`, so this only ever resolves to an actual mail app
 *  (never a share sheet offering unrelated apps that can "handle"
 *  plain text). Used for the Profile screen's "Contact support" and
 *  "Request account deletion" links -- neither is a Custom Tab, since
 *  there's no web page to show, just a mail compose screen. */
fun Context.openMailto(
    email: String,
    subject: String,
    body: String? = null,
) {
    val intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body != null) putExtra(Intent.EXTRA_TEXT, body)
        }
    startActivity(intent)
}
