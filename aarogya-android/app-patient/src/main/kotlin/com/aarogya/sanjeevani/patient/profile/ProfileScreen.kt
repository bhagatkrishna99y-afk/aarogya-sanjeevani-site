package com.aarogya.sanjeevani.patient.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aarogya.sanjeevani.core.common.appearance.AppearanceMode
import com.aarogya.sanjeevani.core.common.error.toUserMessage
import com.aarogya.sanjeevani.core.data.profile.Profile
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsDialog
import com.aarogya.sanjeevani.core.designsystem.component.AsListItem
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.loadingGroupSemantics
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.ui.state.ErrorContent
import com.aarogya.sanjeevani.patient.BuildConfig
import com.aarogya.sanjeevani.patient.FeatureFlags
import com.aarogya.sanjeevani.patient.ui.common.openInCustomTab
import com.aarogya.sanjeevani.patient.ui.common.openMailto
import kotlinx.coroutines.launch

/**
 * The Profile tab's root. Never computes avatar-URL resolution,
 * appearance persistence, or anything else itself -- all of that came
 * straight from [ProfileViewModel]; this file only renders whatever
 * state that produced, plus the one truly local, no-state-implications
 * bit (the logout/appearance dialogs' own open/closed flag).
 */
@Composable
fun ProfileRoute(
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(ProfileAction.Resumed)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProfileUiEvent.AvatarUploadFailed -> {
                    val message = event.error?.toUserMessage(context) ?: "Please choose a smaller photo."
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    val pickAvatar =
        rememberAvatarPickerLauncher { uri ->
            val compressed = compressAvatarImage(context.contentResolver, uri)
            if (compressed != null) {
                viewModel.onAction(ProfileAction.AvatarSelected(compressed.bytes, compressed.contentType))
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Please choose a smaller photo.") }
            }
        }

    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onEditProfileClick = onEditProfileClick,
        onSignOutClick = onSignOutClick,
        onDeleteAccountClick = onDeleteAccountClick,
        onAvatarClick = if (FeatureFlags.AVATAR_UPLOAD_ENABLED) pickAvatar else null,
        modifier = modifier,
    )
}

/** Stateless half -- `internal`, not `private`, same reasoning as every
 *  other screen in this app. */
@Composable
internal fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AsTopBar(title = "Profile") },
    ) { padding ->
        when (val section = uiState.section) {
            ProfileSectionState.Loading -> ShimmerProfile(Modifier.fillMaxSize().padding(padding))
            is ProfileSectionState.Error ->
                ErrorContent(
                    error = section.error,
                    onRetry = { onAction(ProfileAction.Retry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            is ProfileSectionState.Content ->
                ProfileContent(
                    content = section,
                    appearanceMode = uiState.appearanceMode,
                    isUploadingAvatar = uiState.isUploadingAvatar,
                    onAction = onAction,
                    onEditProfileClick = onEditProfileClick,
                    onSignOutClick = onSignOutClick,
                    onDeleteAccountClick = onDeleteAccountClick,
                    onAvatarClick = onAvatarClick,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
        }
    }
}

@Composable
private fun ShimmerProfile(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(AsTheme.spacing.lg).loadingGroupSemantics(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.md),
    ) {
        SkeletonShimmer(modifier = Modifier.size(96.dp), shape = CircleShape)
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(24.dp))
        SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(160.dp))
    }
}

@Composable
private fun ProfileContent(
    content: ProfileSectionState.Content,
    appearanceMode: AppearanceMode,
    isUploadingAvatar: Boolean,
    onAction: (ProfileAction) -> Unit,
    onEditProfileClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(AsTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.xl),
    ) {
        ProfileHeader(
            profile = content.profile,
            avatarUrl = content.avatarUrl,
            isUploadingAvatar = isUploadingAvatar,
            onAvatarClick = onAvatarClick,
        )
        AsButton(text = "Edit profile", onClick = onEditProfileClick, modifier = Modifier.fillMaxWidth())
        AccountSection(
            appearanceMode = appearanceMode,
            onAction = onAction,
            onSignOutClick = onSignOutClick,
            onDeleteAccountClick = onDeleteAccountClick,
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: Profile,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onAvatarClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
    ) {
        val avatarBoxModifier =
            if (onAvatarClick != null) {
                Modifier
                    .clickable(onClick = onAvatarClick, enabled = !isUploadingAvatar)
                    .semantics {
                        contentDescription = "Change photo"
                        if (isUploadingAvatar) stateDescription = "Uploading"
                    }
            } else {
                Modifier
            }
        Box(contentAlignment = Alignment.BottomEnd, modifier = avatarBoxModifier) {
            AsAvatar(name = profile.fullName ?: profile.email, imageUrl = avatarUrl, size = 96.dp)
            if (onAvatarClick != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(text = profile.fullName ?: "Add your name", style = MaterialTheme.typography.titleLarge)
        ProfileDetailRow(label = "Your sign-in email", value = profile.email)
        val phone = profile.phone
        if (!phone.isNullOrBlank()) {
            ProfileDetailRow(label = "Phone", value = phone)
        }
        ProfileDetailRow(label = "Member since", value = formatMemberSince(profile.createdAt))
    }
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountSection(
    appearanceMode: AppearanceMode,
    onAction: (ProfileAction) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    AccountListItems(
        appearanceMode = appearanceMode,
        onAppearanceClick = { showAppearanceDialog = true },
        onDeleteAccountClick = onDeleteAccountClick,
        onLogoutClick = { showLogoutDialog = true },
    )

    if (showAppearanceDialog) {
        AppearanceModeDialog(
            current = appearanceMode,
            onSelect = { mode ->
                onAction(ProfileAction.AppearanceModeSelected(mode))
                showAppearanceDialog = false
            },
            onDismiss = { showAppearanceDialog = false },
        )
    }

    if (showLogoutDialog) {
        AsDialog(
            title = "Log out?",
            text = "You'll need to sign in again to use the app.",
            confirmLabel = "Log out",
            onConfirm = {
                showLogoutDialog = false
                onSignOutClick()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }
}

@Composable
private fun AccountListItems(
    appearanceMode: AppearanceMode,
    onAppearanceClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val context = LocalContext.current

    Column {
        Text(
            text = "Account",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = AsTheme.spacing.sm),
        )
        AsListItem(
            headline = "Appearance",
            supportingText = appearanceMode.label(),
            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
            onClick = onAppearanceClick,
        )
        AsListItem(
            headline = "Notifications",
            supportingText = "In-app notifications are on",
            leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
        )
        AsListItem(
            headline = "Privacy Policy",
            leadingContent = { Icon(Icons.Filled.Description, contentDescription = null) },
            onClick = { context.openInCustomTab("${BuildConfig.WEBSITE_BASE_URL}/privacy-policy") },
        )
        AsListItem(
            headline = "Terms",
            leadingContent = { Icon(Icons.Filled.Description, contentDescription = null) },
            onClick = { context.openInCustomTab("${BuildConfig.WEBSITE_BASE_URL}/terms") },
        )
        AsListItem(
            headline = "App version",
            supportingText = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
        AsListItem(
            headline = "Contact support",
            leadingContent = { Icon(Icons.Filled.Email, contentDescription = null) },
            onClick = { context.openMailto(BuildConfig.CONTACT_EMAIL, subject = "Aarogya Sanjeevani support request") },
        )
        AsListItem(
            headline = "Request account deletion",
            leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
            onClick = onDeleteAccountClick,
        )
        AsListItem(
            headline = "Log out",
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            onClick = onLogoutClick,
        )
    }
}

@Composable
private fun AppearanceModeDialog(
    current: AppearanceMode,
    onSelect: (AppearanceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                AppearanceMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = mode == current,
                                    onClick = { onSelect(mode) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = AsTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = null)
                        Spacer(Modifier.width(AsTheme.spacing.sm))
                        Text(mode.label())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun AppearanceMode.label(): String =
    when (this) {
        AppearanceMode.SYSTEM -> "System"
        AppearanceMode.LIGHT -> "Light"
        AppearanceMode.DARK -> "Dark"
    }
