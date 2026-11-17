// AutofillType is still experimental in this project's pinned
// compose-ui version -- see AsTextField.kt's doc comment.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.auth.signup

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.data.auth.toUserMessage
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.patient.BuildConfig
import com.aarogya.sanjeevani.patient.ui.common.openInCustomTab

/**
 * The stateful half -- see `LoginScreen.kt`'s doc comment for the
 * general shape every screen in this module follows. The one thing
 * unique to this screen's effects: [SignUpUiEvent.NavigateToCheckInbox]
 * is real, screen-owned navigation (see that event's doc comment).
 */
@Composable
fun SignUpRoute(
    onBack: () -> Unit,
    onNavigateToCheckInbox: (email: String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SignUpUiEvent.NavigateToCheckInbox -> onNavigateToCheckInbox(event.email)
                is SignUpUiEvent.ShowError -> snackbarHostState.showSnackbar(event.error.toUserMessage(context))
            }
        }
    }

    SignUpScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Stateless half of this screen -- `internal`, not `private`, so a
 *  Compose UI test can call it directly with a hand-built [SignUpUiState]
 *  and no Hilt, same reasoning as `LoginScreen.kt`'s own doc comment. A
 *  full sign-up form (name/email/password/confirm, strength meter,
 *  terms checkbox, autofill) in one composable -- the most fields of
 *  any auth screen in this module, so unsurprisingly its longest. */
@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SignUpScreen(
    uiState: SignUpUiState,
    onAction: (SignUpAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Create account", onBack = onBack) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(AsTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
        ) {
            AsTextField(
                value = uiState.fullName,
                onValueChange = { onAction(SignUpAction.FullNameChanged(it)) },
                label = "Full name",
                errorText = uiState.fullNameError?.toUserMessage(context),
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.PersonFullName),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier =
                    Modifier.onFocusChanged { if (!it.isFocused) onAction(SignUpAction.FullNameFocusLost) },
            )
            AsTextField(
                value = uiState.email,
                onValueChange = { onAction(SignUpAction.EmailChanged(it)) },
                label = "Email",
                errorText = uiState.emailError?.toUserMessage(context),
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.EmailAddress),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier =
                    Modifier.onFocusChanged { if (!it.isFocused) onAction(SignUpAction.EmailFocusLost) },
            )
            AsTextField(
                value = uiState.password,
                onValueChange = { onAction(SignUpAction.PasswordChanged(it)) },
                label = "Password",
                isPassword = true,
                errorText = uiState.passwordError?.toUserMessage(context),
                helperText =
                    if (uiState.passwordError == null && uiState.password.isNotEmpty()) {
                        "Strength: ${uiState.passwordStrength.toUserMessage(context)}"
                    } else {
                        null
                    },
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.NewPassword),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                modifier =
                    Modifier.onFocusChanged { if (!it.isFocused) onAction(SignUpAction.PasswordFocusLost) },
            )
            AsTextField(
                value = uiState.confirmPassword,
                onValueChange = { onAction(SignUpAction.ConfirmPasswordChanged(it)) },
                label = "Confirm password",
                isPassword = true,
                errorText = uiState.confirmPasswordError?.toUserMessage(context),
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.NewPassword),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onAction(SignUpAction.SubmitClicked)
                        },
                    ),
                modifier =
                    Modifier.onFocusChanged { if (!it.isFocused) onAction(SignUpAction.ConfirmPasswordFocusLost) },
            )

            ConsentRow(
                checked = uiState.consentChecked,
                showError = uiState.showConsentError,
                enabled = !uiState.isSubmitting,
                onCheckedChange = { onAction(SignUpAction.ConsentChanged(it)) },
                onTermsClick = { context.openInCustomTab("${BuildConfig.WEBSITE_BASE_URL}/terms") },
                onPrivacyClick = { context.openInCustomTab("${BuildConfig.WEBSITE_BASE_URL}/privacy-policy") },
            )

            AsButton(
                text = "Create account",
                onClick = { onAction(SignUpAction.SubmitClicked) },
                loading = uiState.isSubmitting,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    showError: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                modifier =
                    Modifier.semantics {
                        if (showError) contentDescription = "I agree to the Terms and Privacy Policy. Required."
                    },
            )
            val linkColor = MaterialTheme.colorScheme.primary
            val text =
                buildAnnotatedString {
                    append("I agree to the ")
                    withLink(
                        LinkAnnotation.Url(
                            url = "terms",
                            styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                            linkInteractionListener = { onTermsClick() },
                        ),
                    ) {
                        append("Terms")
                    }
                    append(" and ")
                    withLink(
                        LinkAnnotation.Url(
                            url = "privacy-policy",
                            styles = TextLinkStyles(style = SpanStyle(color = linkColor)),
                            linkInteractionListener = { onPrivacyClick() },
                        ),
                    ) {
                        append("Privacy Policy")
                    }
                }
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
        if (showError) {
            Text(
                text = "You must accept the Terms and Privacy Policy to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(start = 48.dp)
                        .semantics { contentDescription = "You must accept the Terms and Privacy Policy to continue." },
            )
        }
    }
}

@Preview(name = "Sign up - light")
@Preview(name = "Sign up - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignUpScreenPreview() {
    AarogyaTheme {
        Surface {
            SignUpScreen(uiState = SignUpUiState(fullName = "Jane Doe"), onAction = {}, onBack = {})
        }
    }
}
