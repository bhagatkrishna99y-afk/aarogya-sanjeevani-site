package com.aarogya.sanjeevani.patient.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.BuildConfig
import com.aarogya.sanjeevani.patient.ui.common.openMailto

private const val DELETE_REQUEST_SUBJECT = "Account deletion request"
private const val DELETE_REQUEST_BODY =
    "Please delete my Aarogya Sanjeevani account associated with this email address."

/**
 * No delete button here -- self-service deletion isn't a capability
 * this release has, and faking one (an in-app button that just emails
 * someone, dressed up as "Delete") would be actively misleading about
 * what's actually going to happen and when. This screen's only job is
 * to explain that plainly and hand the patient a prefilled email to
 * actually start the real process.
 */
@Composable
fun DeleteAccountInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Delete my account", onBack = onBack) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(AsTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
        ) {
            Text(
                text = "We don't yet support deleting your account directly in the app.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    "To request deletion, email us from the address on this account and " +
                        "we'll take care of it from there -- we'll confirm once it's done.",
                style = MaterialTheme.typography.bodyMedium,
            )
            AsButton(
                text = "Email to request deletion",
                onClick = {
                    context.openMailto(
                        email = BuildConfig.CONTACT_EMAIL,
                        subject = DELETE_REQUEST_SUBJECT,
                        body = DELETE_REQUEST_BODY,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
