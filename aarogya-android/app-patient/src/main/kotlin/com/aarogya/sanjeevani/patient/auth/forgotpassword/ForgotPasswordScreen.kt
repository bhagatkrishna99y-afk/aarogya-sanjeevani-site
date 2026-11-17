// AutofillType is still experimental in this project's pinned
// compose-ui version -- see AsTextField.kt's doc comment.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.auth.forgotpassword

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ForgotPasswordUiEvent.ShowError -> snackbarHostState.showSnackbar(event.error.toUserMessage(context))
            }
        }
    }

    ForgotPasswordScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

// Two full states (link-sent confirmation vs. the form) in one
// composable, matching every other screen in this module -- splitting
// it would just move the same line count into a second function with
// state threaded back and forth between the two.
@Suppress("LongMethod")
@Composable
private fun ForgotPasswordScreen(
    uiState: ForgotPasswordUiState,
    onAction: (ForgotPasswordAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Reset password", onBack = onBack) },
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
            if (uiState.linkSent) {
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = "Check your email", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text =
                        "If an account exists for ${uiState.email.trim()}, " +
                            "we've sent a link to reset your password.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                AsButton(text = "Back to login", onClick = onBack)
            } else {
                Text(
                    text =
                        "Enter the email associated with your account and " +
                            "we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                AsTextField(
                    value = uiState.email,
                    onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
                    label = "Email",
                    errorText = uiState.emailError?.toUserMessage(context),
                    enabled = !uiState.isSubmitting,
                    autofillTypes = listOf(AutofillType.EmailAddress),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Email),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                onAction(ForgotPasswordAction.SubmitClicked)
                            },
                        ),
                    modifier =
                        Modifier.onFocusChanged { if (!it.isFocused) onAction(ForgotPasswordAction.EmailFocusLost) },
                )
                AsButton(
                    text = "Send reset link",
                    onClick = { onAction(ForgotPasswordAction.SubmitClicked) },
                    loading = uiState.isSubmitting,
                )
            }
        }
    }
}

@Preview(name = "Forgot password - light")
@Preview(name = "Forgot password - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForgotPasswordScreenPreview() {
    AarogyaTheme {
        ForgotPasswordScreen(uiState = ForgotPasswordUiState(), onAction = {}, onBack = {})
    }
}
