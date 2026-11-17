// AutofillType is still experimental in this project's pinned
// compose-ui version -- see AsTextField.kt's doc comment.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.auth.login

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme

/**
 * The stateful half of the [com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel]
 * example: collects [LoginViewModel.uiState] with
 * [collectAsStateWithLifecycle] (never a plain `collectAsState`, so
 * collection pauses while the screen is stopped), collects
 * [LoginViewModel.uiEvent] once in a `LaunchedEffect(Unit)`, and is the
 * one place that resolves an [LoginUiEvent.ShowError]/[LoginUiEvent.ShowRejection]
 * to display text -- see those variants' doc comments for why that
 * resolution happens here and not in the ViewModel.
 */
@Composable
fun LoginRoute(
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                LoginUiEvent.NavigateToSignUp -> onNavigateToSignUp()
                LoginUiEvent.NavigateToForgotPassword -> onNavigateToForgotPassword()
                is LoginUiEvent.ShowError -> snackbarHostState.showSnackbar(event.error.toUserMessage(context))
                is LoginUiEvent.ShowRejection ->
                    event.outcome.toUserMessage(context)?.let { snackbarHostState.showSnackbar(it) }
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** The stateless half -- every piece of UI follows from [uiState], every
 *  interaction is reported as a [LoginAction] rather than mutating
 *  anything itself, which is what makes this previewable with no
 *  ViewModel at all (see [LoginScreenPreview]), and directly testable
 *  from a Compose UI test with a hand-built [uiState] and no Hilt --
 *  `internal`, not `private`, for exactly that reason. A full login
 *  form (two fields, two links, validation, autofill) in one
 *  composable, same shape as every other auth screen in this module. */
@Suppress("LongMethod")
@Composable
internal fun LoginScreen(
    uiState: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Log in") },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Welcome back", style = MaterialTheme.typography.headlineSmall)
            AsTextField(
                value = uiState.email,
                onValueChange = { onAction(LoginAction.EmailChanged(it)) },
                label = "Email",
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.EmailAddress),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )
            AsTextField(
                value = uiState.password,
                onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                label = "Password",
                isPassword = true,
                enabled = !uiState.isSubmitting,
                autofillTypes = listOf(AutofillType.Password),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            onAction(LoginAction.SubmitClicked)
                        },
                    ),
            )
            AsButton(
                text = "Log in",
                onClick = { onAction(LoginAction.SubmitClicked) },
                loading = uiState.isSubmitting,
            )
            AsButton(
                text = "Forgot password?",
                onClick = { onAction(LoginAction.ForgotPasswordClicked) },
                variant = AsButtonVariant.Text,
                enabled = !uiState.isSubmitting,
            )
            AsButton(
                text = "Create an account",
                onClick = { onAction(LoginAction.SignUpClicked) },
                variant = AsButtonVariant.Secondary,
                enabled = !uiState.isSubmitting,
            )
        }
    }
}

@Preview(name = "Login - light")
@Preview(name = "Login - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    AarogyaTheme {
        Surface {
            LoginScreen(uiState = LoginUiState(email = "jane@example.com"), onAction = {})
        }
    }
}
