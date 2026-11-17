package com.aarogya.sanjeevani.patient.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.BuildConfig
import com.aarogya.sanjeevani.patient.auth.welcome.BrandLogo
import com.aarogya.sanjeevani.patient.auth.welcome.WelcomeIllustration
import com.aarogya.sanjeevani.patient.ui.common.openInCustomTab

/** The very first thing a signed-out user sees -- the root of
 *  [com.aarogya.sanjeevani.patient.navigation.AuthGraph], never reached
 *  with a back action available (there's nothing behind it once the
 *  auth gate has routed here). */
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AsTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg, Alignment.Bottom),
        ) {
            BrandLogo()
            WelcomeIllustration()
            Text(
                text = "Aarogya Sanjeevani",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Book trusted doctors for home, clinic, or video consultations.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AsButton(text = "Create an account", onClick = onSignUpClick, modifier = Modifier.fillMaxWidth())
            AsButton(
                text = "Log in",
                onClick = onLoginClick,
                variant = AsButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            PrivacyNote(
                privacyPolicyUrl = "${BuildConfig.WEBSITE_BASE_URL}/privacy-policy",
                onPrivacyPolicyClick = { url -> context.openInCustomTab(url) },
            )
        }
    }
}

/** [onPrivacyPolicyClick] always wins over the link's own default
 *  behavior (handing the URL to whatever the system would otherwise
 *  open it with) -- that's what makes this a Custom Tab rather than a
 *  full browser hand-off. */
@Composable
private fun PrivacyNote(
    privacyPolicyUrl: String,
    onPrivacyPolicyClick: (String) -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val text =
        buildAnnotatedString {
            append("By continuing, you agree to our treatment of your data as described in our ")
            withLink(
                LinkAnnotation.Url(
                    url = privacyPolicyUrl,
                    styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                    linkInteractionListener = { onPrivacyPolicyClick(privacyPolicyUrl) },
                ),
            ) {
                append("Privacy Policy")
            }
            append(".")
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
