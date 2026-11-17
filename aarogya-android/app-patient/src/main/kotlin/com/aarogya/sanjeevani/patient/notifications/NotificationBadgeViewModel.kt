package com.aarogya.sanjeevani.patient.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.data.notifications.NotificationSource
import com.aarogya.sanjeevani.core.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MILLIS = 60_000L

/**
 * The single, app-wide source of the unread-notifications count --
 * resolved once in `PatientAppRoot` (Activity-scoped, so it survives
 * every tab switch), not per-destination like every other ViewModel in
 * this app. [com.aarogya.sanjeevani.patient.home.HomeScreen]'s bell and
 * [com.aarogya.sanjeevani.patient.navigation.PatientBottomBar]'s
 * Notifications tab both read [unreadCount] from this one instance --
 * neither polls its own, so the two can never show different numbers.
 *
 * [resume]/[pause] (wired to `PatientApp`'s own `LifecycleEventEffect`
 * for `ON_RESUME`/`ON_PAUSE`, since this ViewModel itself has no
 * Android lifecycle of its own to hook) start/stop the 60-second
 * foreground poll; [refresh] is the other two triggers from the task
 * spec -- an immediate one-off call made right after marking something
 * read, threaded down from the Notifications screen as a plain
 * callback (see `PatientNavHost.kt`'s `notificationsTabGraph`), and
 * whatever [NotificationSource] signals (nothing yet in this release --
 * see that interface's own doc comment).
 *
 * Resets to `0` and stops polling the moment auth flips to
 * [AuthState.Unauthenticated] -- the one piece of state in this app
 * that would otherwise survive a sign-out, since it's Activity-scoped
 * rather than tied to the back stack [com.aarogya.sanjeevani.patient.navigation.AuthGateEffect]
 * tears down.
 */
@HiltViewModel
class NotificationBadgeViewModel
    @Inject
    constructor(
        private val notificationsRepository: NotificationsRepository,
        authRepository: AuthRepository,
        notificationSource: NotificationSource,
    ) : ViewModel() {
        private val mutableUnreadCount = MutableStateFlow(0)
        val unreadCount: StateFlow<Int> = mutableUnreadCount.asStateFlow()

        private var pollingJob: Job? = null

        init {
            viewModelScope.launch {
                authRepository.observeAuthState().collect { state ->
                    if (state is AuthState.Unauthenticated) {
                        pollingJob?.cancel()
                        pollingJob = null
                        mutableUnreadCount.value = 0
                    }
                }
            }
            viewModelScope.launch {
                notificationSource.observeNewNotificationSignal().collect { refresh() }
            }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                when (val result = notificationsRepository.getUnreadCount()) {
                    is AppResult.Success -> mutableUnreadCount.value = result.data
                    // Keeps the last-known count -- not worth surfacing an error for a badge.
                    is AppResult.Failure -> Unit
                }
            }
        }

        fun resume() {
            if (pollingJob?.isActive == true) return
            pollingJob =
                viewModelScope.launch {
                    while (true) {
                        delay(POLL_INTERVAL_MILLIS)
                        refresh()
                    }
                }
            refresh()
        }

        fun pause() {
            pollingJob?.cancel()
            pollingJob = null
        }

        override fun onCleared() {
            pollingJob?.cancel()
        }
    }
