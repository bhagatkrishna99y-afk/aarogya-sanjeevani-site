package com.aarogya.sanjeevani.patient.home

import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.common.connectivity.ConnectivityObserver
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.data.requests.PatientRequest
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.data.services.ServicesRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val MAX_UPCOMING_REQUESTS = 3
private const val MAX_FEATURED_DOCTORS = 5
private val RELEVANT_REQUEST_STATUSES = setOf(RequestStatus.REQUESTED, RequestStatus.ACCEPTED, RequestStatus.CONFIRMED)

/**
 * Drives the Home screen's 6 content areas, each loaded independently
 * (one failing/slow section never blocks or delays another -- see
 * [refreshAll] launching one coroutine per section rather than
 * fetching sequentially).
 *
 * **Silent vs. visible refresh.** [onResume][HomeAction.Resumed] (the
 * screen becoming visible, including the very first time) and
 * [HomeAction.Refresh] (pull-to-refresh) both call [refreshAll], but a
 * section already showing [HomeSectionState.Success] never regresses
 * to [HomeSectionState.Error] from either -- a background refresh
 * failing silently keeps the last good data on screen instead of
 * replacing it with an error (no flicker, nothing alarming for a
 * failure the user didn't explicitly ask about). [HomeAction.Refresh]
 * additionally sends [HomeUiEvent.ShowRefreshFailedMessage] when that
 * happens, since that refresh *was* a deliberate user action worth a
 * quiet acknowledgment that it didn't fully succeed --
 * [HomeAction.Resumed] never does, since nothing about it was
 * user-visible to begin with. An explicit per-section retry
 * ([HomeAction.RetryCategories] etc.), by contrast, *does* show
 * [HomeSectionState.Loading] again and *can* land on
 * [HomeSectionState.Error] -- the user asked for this one specifically
 * and is already looking at it. This screen genuinely aggregates 6
 * repositories for its 6 independent content areas, each with its own
 * load+retry pair -- not accidental complexity to refactor away (see
 * config/detekt/detekt.yml's own rationale for similarly tolerating a
 * Compose entry point's size).
 */
@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val servicesRepository: ServicesRepository,
        private val requestsRepository: RequestsRepository,
        private val doctorsRepository: DoctorsRepository,
        private val clock: Clock,
        connectivityObserver: ConnectivityObserver,
    ) : BaseViewModel<HomeUiState, HomeUiEvent, HomeAction>(HomeUiState()) {
        /** "Good morning, Aanya" -- `null` when nothing's loaded yet
         *  (falls back to a name-less greeting in `HomeScreen.kt`). */
        val greetingPrefix: String
            get() = greetingPrefixFor(clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour)

        init {
            // A manual "Try again" tap right as connectivity returns can lose
            // the race against the OS/network stack not being fully ready yet
            // -- this gives every currently-failed section a second, unasked-for
            // shot the moment connectivity is confirmed back, rather than
            // leaving the user stuck until they navigate away and back (which
            // only works because `Resumed` happens to fire then too).
            viewModelScope.launch {
                var wasConnected: Boolean? = null
                connectivityObserver.isConnected.collect { isConnected ->
                    if (wasConnected == false && isConnected) {
                        retryFailedSections()
                    }
                    wasConnected = isConnected
                }
            }
        }

        override fun onAction(action: HomeAction) {
            when (action) {
                HomeAction.Resumed -> refreshAll(isUserInitiated = false)
                HomeAction.Refresh -> refreshAll(isUserInitiated = true)
                HomeAction.RetryCategories -> retryCategories()
                HomeAction.RetryRequests -> retryRequests()
                HomeAction.RetryDoctors -> retryDoctors()
            }
        }

        private fun refreshAll(isUserInitiated: Boolean) {
            viewModelScope.launch {
                if (isUserInitiated) setState { copy(isRefreshing = true) }

                val greetingJob = launch { loadGreeting() }
                var categoriesFailed = false
                var requestsFailed = false
                var doctorsFailed = false
                coroutineScope {
                    launch { categoriesFailed = loadCategories() }
                    launch { requestsFailed = loadRequests() }
                    launch { doctorsFailed = loadDoctors() }
                }
                greetingJob.join()

                if (isUserInitiated) {
                    setState { copy(isRefreshing = false) }
                    if (categoriesFailed || requestsFailed || doctorsFailed) {
                        sendEvent(HomeUiEvent.ShowRefreshFailedMessage)
                    }
                }
            }
        }

        private fun retryCategories() {
            if (currentState.categories is HomeSectionState.Success) return
            viewModelScope.launch {
                setState { copy(categories = HomeSectionState.Loading) }
                loadCategories()
            }
        }

        private fun retryRequests() {
            if (currentState.requests is HomeSectionState.Success) return
            viewModelScope.launch {
                setState { copy(requests = HomeSectionState.Loading) }
                loadRequests()
            }
        }

        private fun retryDoctors() {
            if (currentState.doctors is HomeSectionState.Success) return
            viewModelScope.launch {
                setState { copy(doctors = HomeSectionState.Loading) }
                loadDoctors()
            }
        }

        private fun retryFailedSections() {
            if (currentState.categories is HomeSectionState.Error) retryCategories()
            if (currentState.requests is HomeSectionState.Error) retryRequests()
            if (currentState.doctors is HomeSectionState.Error) retryDoctors()
        }

        private suspend fun loadGreeting() {
            val userId = authRepository.currentUserId()
            val name =
                if (userId == null) {
                    currentState.greetingName
                } else {
                    when (val result = profileRepository.getProfile(userId)) {
                        is AppResult.Success -> result.data.fullName?.let(::firstNameOf)
                        is AppResult.Failure -> currentState.greetingName
                    }
                }
            setState { copy(greetingName = name) }
        }

        /** @return `true` if the fetch failed (regardless of whether the
         *  old, still-displayed data was kept -- see [loadSection]). */
        private suspend fun loadCategories(): Boolean =
            loadSection(
                current = currentState.categories,
                fetch = servicesRepository::getServiceCategories,
                update = { setState { copy(categories = it) } },
            )

        private suspend fun loadRequests(): Boolean =
            loadSection(
                current = currentState.requests,
                fetch = { requestsRepository.getMyRequestsAsPatient().map { it.upcomingAndActive() } },
                update = { setState { copy(requests = it) } },
            )

        private suspend fun loadDoctors(): Boolean =
            loadSection(
                current = currentState.doctors,
                fetch = { doctorsRepository.getAllDoctors().map { it.featured() } },
                update = { setState { copy(doctors = it) } },
            )

        /** The one place that decides "did this section's fetch fail,
         *  and if so, should that actually replace what's on screen" --
         *  see this class's own doc comment for why a [HomeSectionState.Success]
         *  is never downgraded to [HomeSectionState.Error]. */
        private suspend fun <T> loadSection(
            current: HomeSectionState<T>,
            fetch: suspend () -> AppResult<T>,
            update: (HomeSectionState<T>) -> Unit,
        ): Boolean =
            when (val result = fetch()) {
                is AppResult.Success -> {
                    update(HomeSectionState.Success(result.data))
                    false
                }
                is AppResult.Failure -> {
                    if (current !is HomeSectionState.Success) {
                        update(HomeSectionState.Error(result.error))
                    }
                    true
                }
            }

        private fun List<PatientRequest>.upcomingAndActive(): List<PatientRequest> {
            val now = clock.now()
            return filter { it.status in RELEVANT_REQUEST_STATUSES && it.scheduledStart > now }
                .sortedBy { it.scheduledStart }
                .take(MAX_UPCOMING_REQUESTS)
        }

        private fun List<DoctorPublic>.featured(): List<DoctorPublic> =
            sortedWith(compareByDescending<DoctorPublic> { it.acceptingRequests }.thenBy { it.fullName })
                .take(MAX_FEATURED_DOCTORS)
    }
