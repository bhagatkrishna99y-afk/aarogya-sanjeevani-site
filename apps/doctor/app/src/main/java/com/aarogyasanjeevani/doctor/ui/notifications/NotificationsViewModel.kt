package com.aarogyasanjeevani.doctor.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogyasanjeevani.doctor.data.repository.NotificationRepository
import com.aarogyasanjeevani.doctor.domain.model.AppNotification
import com.aarogyasanjeevani.doctor.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<AppNotification>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<AppNotification>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            notificationRepository.fetchNotifications()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Couldn't load notifications.") }
        }
    }

    fun markRead(notification: AppNotification) {
        if (notification.isRead) return
        viewModelScope.launch {
            notificationRepository.markRead(notification.id).onSuccess { load() }
        }
    }
}
