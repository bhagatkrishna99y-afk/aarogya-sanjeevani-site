@file:OptIn(ExperimentalComposeUiApi::class)

package com.aarogya.sanjeevani.patient.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsDialog
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent

/**
 * Edit Profile. [EditProfileRoute] owns the one piece of state that
 * isn't [EditProfileViewModel]'s to track -- whether the discard-changes
 * dialog is open -- since it's pure navigation-intent UI, not form data.
 */
@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onAction(EditProfileAction.Load)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                EditProfileUiEvent.SaveSucceeded -> onBack()
                is EditProfileUiEvent.SaveFailed -> snackbarHostState.showSnackbar(event.error.toUserMessage(context))
            }
        }
    }

    val attemptBack = { if (uiState.isDirty) showDiscardDialog = true else onBack() }
    BackHandler(onBack = attemptBack)

    EditProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = attemptBack,
        modifier = modifier,
    )

    if (showDiscardDialog) {
        AsDialog(
            title = "Discard changes?",
            text = "Your edits haven't been saved.",
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this app. */
@Composable
internal fun EditProfileScreen(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Edit profile", onBack = onBackClick) },
    ) { padding ->
        when {
            uiState.loadError != null ->
                ErrorContent(
                    error = uiState.loadError,
                    onRetry = { onAction(EditProfileAction.Load) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            uiState.isLoading -> EditProfileSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
            else ->
                EditProfileForm(
                    uiState = uiState,
                    onAction = onAction,
                    onCancelClick = onBackClick,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }
}

@Composable
private fun EditProfileSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AsTheme.spacing.lg).loadingGroupSemantics(),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
    ) {
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(56.dp))
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(56.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md), modifier = Modifier.fillMaxWidth()) {
            SkeletonShimmer(modifier = Modifier.weight(1f).height(48.dp))
            SkeletonShimmer(modifier = Modifier.weight(1f).height(48.dp))
        }
    }
}

@Composable
private fun FullNameField(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
) {
    AsTextField(
        value = uiState.fullName,
        onValueChange = { onAction(EditProfileAction.FullNameChanged(it)) },
        label = "Full name",
        modifier = Modifier.onFocusChanged { if (!it.isFocused) onAction(EditProfileAction.FullNameFocusLost) },
        errorText =
            if (uiState.fullNameTouched || uiState.submitAttempted) {
                when (uiState.fieldErrors["fullName"]) {
                    EditProfileFieldError.FULL_NAME_TOO_SHORT -> "Please enter your full name."
                    EditProfileFieldError.FULL_NAME_TOO_LONG -> "That name is too long."
                    else -> null
                }
            } else {
                null
            },
    )
}

@Composable
private fun EditProfileForm(
    uiState: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.lg),
    ) {
        FullNameField(uiState = uiState, onAction = onAction)
        AsTextField(
            value = uiState.phone,
            onValueChange = { onAction(EditProfileAction.PhoneChanged(it)) },
            label = "Phone (optional)",
            modifier = Modifier.onFocusChanged { if (!it.isFocused) onAction(EditProfileAction.PhoneFocusLost) },
            errorText =
                if ((uiState.phoneTouched || uiState.submitAttempted) &&
                    uiState.fieldErrors["phone"] == EditProfileFieldError.PHONE_INVALID
                ) {
                    "Please enter a valid phone number."
                } else {
                    null
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.md), modifier = Modifier.fillMaxWidth()) {
            AsButton(
                text = "Cancel",
                onClick = onCancelClick,
                variant = AsButtonVariant.Secondary,
                enabled = !uiState.isSaving,
                modifier = Modifier.weight(1f),
            )
            AsButton(
                text = "Save",
                onClick = { onAction(EditProfileAction.SaveClicked) },
                enabled = uiState.isSaveEnabled,
                loading = uiState.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
