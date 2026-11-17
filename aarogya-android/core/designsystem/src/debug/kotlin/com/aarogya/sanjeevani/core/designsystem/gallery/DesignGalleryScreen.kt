package com.aarogya.sanjeevani.core.designsystem.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogya.sanjeevani.core.designsystem.component.AsAvatar
import com.aarogya.sanjeevani.core.designsystem.component.AsBadge
import com.aarogya.sanjeevani.core.designsystem.component.AsBottomBar
import com.aarogya.sanjeevani.core.designsystem.component.AsBottomBarItem
import com.aarogya.sanjeevani.core.designsystem.component.AsButton
import com.aarogya.sanjeevani.core.designsystem.component.AsButtonVariant
import com.aarogya.sanjeevani.core.designsystem.component.AsCard
import com.aarogya.sanjeevani.core.designsystem.component.AsChip
import com.aarogya.sanjeevani.core.designsystem.component.AsDialog
import com.aarogya.sanjeevani.core.designsystem.component.AsDivider
import com.aarogya.sanjeevani.core.designsystem.component.AsDoctorStatus
import com.aarogya.sanjeevani.core.designsystem.component.AsListItem
import com.aarogya.sanjeevani.core.designsystem.component.AsRequestStatus
import com.aarogya.sanjeevani.core.designsystem.component.AsSearchField
import com.aarogya.sanjeevani.core.designsystem.component.AsSnackbarHost
import com.aarogya.sanjeevani.core.designsystem.component.AsTextField
import com.aarogya.sanjeevani.core.designsystem.component.AsTopBar
import com.aarogya.sanjeevani.core.designsystem.component.DateStrip
import com.aarogya.sanjeevani.core.designsystem.component.KeyValueRow
import com.aarogya.sanjeevani.core.designsystem.component.RatingPlaceholder
import com.aarogya.sanjeevani.core.designsystem.component.SectionHeader
import com.aarogya.sanjeevani.core.designsystem.component.SkeletonShimmer
import com.aarogya.sanjeevani.core.designsystem.component.SlotChip
import com.aarogya.sanjeevani.core.designsystem.component.StatusChip
import com.aarogya.sanjeevani.core.designsystem.component.TimelineItem
import com.aarogya.sanjeevani.core.designsystem.theme.AsTheme
import com.aarogya.sanjeevani.core.designsystem.theme.ThemeMode
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Every component this module exports, grouped the same way the task
 *  that created this module grouped them -- scroll top to bottom and
 *  you've seen the whole design system in both themes (toggle at top). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun DesignGalleryScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var dialogVisible by remember { mutableStateOf(false) }
    var selectedBottomIndex by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var searchQuery by remember { mutableStateOf("") }
    var nameFieldValue by remember { mutableStateOf("Jane Doe") }
    var chipSelected by remember { mutableStateOf(true) }
    var selectedSlot by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            AsTopBar(
                title = "Design Gallery",
                actions = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                },
            )
        },
        bottomBar = {
            AsBottomBar(
                items =
                    listOf(
                        AsBottomBarItem(label = "Home", icon = Icons.Filled.Home),
                        AsBottomBarItem(label = "Bookings", icon = Icons.Filled.CalendarToday),
                        AsBottomBarItem(label = "Profile", icon = Icons.Filled.Person),
                    ),
                selectedIndex = selectedBottomIndex,
                onItemSelected = { selectedBottomIndex = it },
            )
        },
        snackbarHost = { AsSnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth().padding(horizontal = AsTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(top = AsTheme.spacing.lg)) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        ) {
                            Text(mode.name)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Buttons") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsButton(text = "Book appointment", onClick = {})
                    AsButton(text = "View details", onClick = {}, variant = AsButtonVariant.Secondary)
                    AsButton(text = "Cancel", onClick = {}, variant = AsButtonVariant.Text)
                    AsButton(text = "Booking...", onClick = {}, loading = true)
                    AsButton(text = "Unavailable", onClick = {}, enabled = false)
                }
            }

            item { SectionHeader(title = "Text fields") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsTextField(
                        value = nameFieldValue,
                        onValueChange = { nameFieldValue = it },
                        label = "Full name",
                        helperText = "As it appears on your ID",
                    )
                    AsTextField(value = "", onValueChange = {}, label = "Email address", errorText = "Please enter your email address.")
                    AsTextField(value = "secret123", onValueChange = {}, label = "Password", isPassword = true)
                    AsSearchField(query = searchQuery, onQueryChange = { searchQuery = it }, placeholder = "Search doctors or services")
                }
            }

            item { SectionHeader(title = "Cards & list items") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsCard { Text("A static card with some content inside it.") }
                    AsListItem(
                        headline = "Dr. Asha Rao",
                        supportingText = "General Medicine · 4.8 ★",
                        leadingContent = { AsAvatar(name = "Asha Rao") },
                        onClick = {},
                    )
                }
            }

            item { SectionHeader(title = "Chips & badges") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsChip(label = "Cardiology", selected = chipSelected, onClick = { chipSelected = !chipSelected })
                    AsBadge(text = "3")
                    AsBadge(
                        text = "New",
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }

            item { SectionHeader(title = "Status chips -- request") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsRequestStatus.entries.forEach { StatusChip(status = it) }
                }
            }

            item { SectionHeader(title = "Status chips -- doctor") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsDoctorStatus.entries.forEach { StatusChip(status = it) }
                }
            }

            item { SectionHeader(title = "Avatars") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsAvatar(name = "Asha Rao")
                    AsAvatar(name = "Jane Doe", size = 56.dp)
                    AsAvatar(name = "X")
                }
            }

            item { SectionHeader(title = "Dialog & snackbar") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    AsButton(text = "Show dialog", onClick = { dialogVisible = true }, variant = AsButtonVariant.Secondary)
                    AsButton(
                        text = "Show snackbar",
                        variant = AsButtonVariant.Secondary,
                        onClick = { scope.launch { snackbarHostState.showSnackbar("Request submitted") } },
                    )
                }
            }

            item { SectionHeader(title = "Divider") }
            item { AsDivider() }

            item { SectionHeader(title = "Key/value row") }
            item {
                Column {
                    KeyValueRow(key = "Service", value = "General consultation")
                    KeyValueRow(key = "Date", value = "12 Nov, 10:00 AM")
                }
            }

            item { SectionHeader(title = "Timeline") }
            item {
                Column {
                    TimelineItem(title = "Requested", timestamp = "12 Nov, 9:00 AM", isCompleted = true)
                    TimelineItem(title = "Accepted", timestamp = "12 Nov, 9:15 AM", isCompleted = true)
                    TimelineItem(title = "Confirmed", isCompleted = false, isLast = true)
                }
            }

            item { SectionHeader(title = "Slot chips") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    listOf("10:00 AM", "10:30 AM", "11:00 AM").forEachIndexed { index, label ->
                        SlotChip(
                            label = label,
                            selected = selectedSlot == index,
                            onClick = { selectedSlot = index },
                            available = index != 2,
                        )
                    }
                }
            }

            item { SectionHeader(title = "Date strip") }
            item {
                DateStrip(
                    dates = (0 until 10).map { LocalDate.now().plusDays(it.toLong()) },
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                )
            }

            item { SectionHeader(title = "Rating placeholder") }
            item { RatingPlaceholder() }

            item { SectionHeader(title = "Skeleton shimmer") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AsTheme.spacing.sm)) {
                    SkeletonShimmer(modifier = Modifier.fillMaxWidth().height(20.dp))
                    SkeletonShimmer(modifier = Modifier.width(160.dp).height(16.dp))
                }
            }

            item { Spacer(Modifier.height(AsTheme.spacing.xxl)) }
        }
    }

    if (dialogVisible) {
        AsDialog(
            title = "Cancel this request?",
            text = "The doctor will be notified. This can't be undone.",
            confirmLabel = "Cancel request",
            onConfirm = { dialogVisible = false },
            onDismiss = { dialogVisible = false },
        )
    }
}
