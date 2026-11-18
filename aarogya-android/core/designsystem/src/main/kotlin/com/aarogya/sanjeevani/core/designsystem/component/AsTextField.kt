// The classic AutofillNode/Autofill/LocalAutofillTree API (see
// AsTextField's own doc comment on why this is what's used, not the
// newer `contentType` semantics) is marked experimental in this
// compose-ui version -- still the stable, documented way to do Compose
// autofill prior to that newer API, just not yet annotation-free.
@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.theme.AarogyaTheme
import com.aarogya.sanjeevani.core.designsystem.theme.InputShape

/**
 * The one text field every form field in either app should use.
 * [helperText] and [errorText] are mutually exclusive by construction --
 * an [errorText] always wins when both are passed, since a field can't
 * be simultaneously "here's a hint" and "here's what's wrong".
 * Material3's own `isError` wiring gives TalkBack the error *state*
 * automatically; [errorText] is additionally attached via
 * [androidx.compose.ui.semantics.error] so TalkBack announces *what*
 * the problem is, not just that there is one.
 *
 * [autofillTypes] (e.g. [AutofillType.EmailAddress]/[AutofillType.Password])
 * is what lets a password manager recognize and offer to fill/save this
 * field -- pass it on every credential field, nothing works
 * automatically without it. This is the older [AutofillNode]/
 * [LocalAutofillTree] API, not the newer `contentType` semantics
 * property: that one is still `internal` in the compose-ui version this
 * project is pinned to (verified by trying it -- see the task this was
 * built under), so this is the stable mechanism actually available
 * here. Functionally equivalent from the password manager's point of
 * view.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
fun AsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    autofillTypes: List<AutofillType> = emptyList(),
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isError = errorText != null

    val autofill = LocalAutofill.current
    val autofillNode =
        if (autofillTypes.isNotEmpty()) {
            AutofillNode(autofillTypes = autofillTypes, onFill = onValueChange)
        } else {
            null
        }
    if (autofillNode != null) {
        LocalAutofillTree.current += autofillNode
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { if (errorText != null) error(errorText) }
                .let { base ->
                    if (autofillNode == null) {
                        base
                    } else {
                        base
                            .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    autofill?.requestAutofillForNode(autofillNode)
                                } else {
                                    autofill?.cancelAutofillForNode(autofillNode)
                                }
                            }
                    }
                },
        enabled = enabled,
        readOnly = readOnly,
        shape = InputShape,
        colors =
            if (readOnly) {
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { icon -> { Icon(imageVector = icon, contentDescription = null) } },
        isError = isError,
        singleLine = singleLine,
        visualTransformation =
            if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions =
            if (isPassword) keyboardOptions.copy(keyboardType = KeyboardType.Password) else keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon =
            if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                }
            } else {
                null
            },
        supportingText =
            if (errorText != null) {
                { Text(errorText) }
            } else if (helperText != null) {
                { Text(helperText) }
            } else {
                null
            },
    )
}

@Preview(name = "Text field - light")
@Preview(name = "Text field - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AsTextFieldPreview() {
    AarogyaTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                AsTextField(
                    value = "Jane Doe",
                    onValueChange = {},
                    label = "Full name",
                    helperText = "As it appears on your ID",
                )
                Spacer(Modifier.height(12.dp))
                AsTextField(
                    value = "",
                    onValueChange = {},
                    label = "Email address",
                    errorText = "Please enter your email address.",
                )
                Spacer(Modifier.height(12.dp))
                AsTextField(value = "secret123", onValueChange = {}, label = "Password", isPassword = true)
            }
        }
    }
}
