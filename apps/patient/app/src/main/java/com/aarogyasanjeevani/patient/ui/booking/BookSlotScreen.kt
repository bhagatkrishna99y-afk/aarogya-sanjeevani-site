package com.aarogyasanjeevani.patient.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aarogyasanjeevani.patient.domain.model.AvailableSlot
import com.aarogyasanjeevani.patient.ui.common.EmptyState
import com.aarogyasanjeevani.patient.ui.common.ErrorState
import com.aarogyasanjeevani.patient.ui.common.LoadingState
import com.aarogyasanjeevani.patient.ui.common.UiState
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSlotScreen(
    uiState: BookingUiState,
    viewModel: BookingViewModel,
    onSlotChosen: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(uiState.service, uiState.doctor, uiState.selectedDate) {
        if (uiState.service != null) viewModel.loadSlots()
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dates = remember(today) { (0 until 14).map { today.plus(it, DateTimeUnit.DAY) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a date & time") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(dates) { date ->
                    FilterChip(
                        selected = date == uiState.selectedDate,
                        onClick = { viewModel.selectDate(date) },
                        label = { Text(formatDateShort(date)) },
                    )
                }
            }

            when (val slots = uiState.slotsState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = slots.message, onRetry = viewModel::loadSlots)
                is UiState.Success -> {
                    if (slots.data.isEmpty()) {
                        EmptyState(
                            title = "No open slots on this day",
                            description = "Try another date.",
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(slots.data) { slot ->
                                SlotChip(
                                    slot = slot,
                                    selected = slot == uiState.selectedSlot,
                                    onClick = {
                                        viewModel.selectSlot(slot)
                                        onSlotChosen()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotChip(slot: AvailableSlot, selected: Boolean, onClick: () -> Unit) {
    val localDateTime = slot.start.toLocalDateTime(TimeZone.currentSystemDefault())
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = formatTime(localDateTime.hour, localDateTime.minute),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

private fun formatDateShort(date: LocalDate): String =
    "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}"

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
