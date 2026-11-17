package com.aarogya.sanjeevani.patient.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import com.aarogya.sanjeevani.core.data.requests.NewServiceRequest
import com.aarogya.sanjeevani.core.data.requests.RequestsRepository
import com.aarogya.sanjeevani.core.data.services.ServicesRepository
import com.aarogya.sanjeevani.core.ui.mvi.BaseViewModel
import com.aarogya.sanjeevani.patient.navigation.RequestForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

private val DOCTOR_UNAVAILABLE_CODES = setOf("AS002_DOCTOR_NOT_AVAILABLE", "AS018_DOCTOR_NOT_APPROVED")
private val LIMIT_EXCEEDED_CODES = setOf("AS034_TOO_MANY_ACTIVE_REQUESTS", "AS035_RATE_LIMITED")
private const val SLOT_UNAVAILABLE_CODE = "AS001_SLOT_UNAVAILABLE"

/**
 * Drives all three booking-flow screens -- [RequestFormRoute]'s own
 * form, then [ReviewRoute] and [SuccessRoute] reuse this exact
 * instance (see `PatientNavHost.kt`'s `bookingFlow`), rather than each
 * re-threading the same fields through nav args a second and third
 * time. [savedStateHandle] persists every *editable* field (never the
 * identifying ones below, which Navigation's own `SavedStateHandle`
 * already restores for free as this screen's nav args) -- see
 * [persist]'s own doc comment for why that split matters here more
 * than it did for `SignUpViewModel`.
 *
 * The real, Hilt-injected path is the `@Inject` secondary constructor
 * below, which does the actual [androidx.navigation.toRoute] decode --
 * [internal] primary constructor takes the already-decoded [RequestForm]
 * directly instead, purely so `RequestFormViewModelTest` can hand it a
 * hand-built route and skip that decode: `toRoute()` round-trips every
 * field through a real `android.os.Bundle`, which this app's plain JVM
 * unit tests have no working one of (no Robolectric in this project),
 * so a `SavedStateHandle` built from a plain test map can't go through
 * it. Production behavior is identical either way -- the `@Inject`
 * constructor is still the only one Hilt ever calls.
 */
@Suppress("LongParameterList")
@HiltViewModel
class RequestFormViewModel
    internal constructor(
        private val servicesRepository: ServicesRepository,
        private val doctorsRepository: DoctorsRepository,
        private val authRepository: AuthRepository,
        private val profileRepository: ProfileRepository,
        private val requestsRepository: RequestsRepository,
        private val savedStateHandle: SavedStateHandle,
        route: RequestForm,
    ) : BaseViewModel<RequestFormUiState, RequestFormUiEvent, RequestFormAction>(
            RequestFormUiState(
                serviceId = route.serviceId,
                serviceName = route.serviceName,
                doctorId = route.doctorId,
                doctorName = route.doctorName,
                slotStart = route.slotStart?.let { Instant.parse(it) },
                slotDurationMinutes = route.slotDurationMinutes,
                selectedMode = savedStateHandle.get<String>(KEY_MODE)?.let { ServiceMode.valueOf(it) },
                contactPhone = savedStateHandle[KEY_CONTACT_PHONE] ?: "",
                address = savedStateHandle[KEY_ADDRESS] ?: "",
                notes = savedStateHandle[KEY_NOTES] ?: "",
            ),
        ) {
        @Inject
        constructor(
            servicesRepository: ServicesRepository,
            doctorsRepository: DoctorsRepository,
            authRepository: AuthRepository,
            profileRepository: ProfileRepository,
            requestsRepository: RequestsRepository,
            savedStateHandle: SavedStateHandle,
        ) : this(
            servicesRepository,
            doctorsRepository,
            authRepository,
            profileRepository,
            requestsRepository,
            savedStateHandle,
            savedStateHandle.toRoute<RequestForm>(),
        )

        init {
            persist(KEY_MODE) { it.selectedMode?.name }
            persist(KEY_CONTACT_PHONE) { it.contactPhone }
            persist(KEY_ADDRESS) { it.address }
            persist(KEY_NOTES) { it.notes }
            loadContext()
        }

        /** Mirrors one field of [uiState] into [savedStateHandle] under
         *  [key] on every change -- same mechanism as `SignUpViewModel`'s
         *  own `persist`, just covering every editable field here
         *  (nothing as sensitive as a password is ever collected on
         *  this form, so there's no field this deliberately skips). */
        private fun <T> persist(
            key: String,
            selector: (RequestFormUiState) -> T,
        ) {
            viewModelScope.launch {
                uiState.map(selector).distinctUntilChanged().collect { savedStateHandle[key] = it }
            }
        }

        override fun onAction(action: RequestFormAction) {
            when (action) {
                RequestFormAction.Retry -> loadContext()
                is RequestFormAction.ModeSelected -> setState { copy(selectedMode = action.mode) }
                is RequestFormAction.ContactPhoneChanged -> setState { copy(contactPhone = action.value) }
                RequestFormAction.ContactPhoneFocusLost -> setState { copy(contactPhoneTouched = true) }
                is RequestFormAction.AddressChanged -> setState { copy(address = action.value) }
                RequestFormAction.AddressFocusLost -> setState { copy(addressTouched = true) }
                is RequestFormAction.NotesChanged -> setState { copy(notes = action.value) }
                RequestFormAction.ContinueClicked -> continueClicked()
                RequestFormAction.SubmitClicked -> submit()
            }
        }

        private fun loadContext() {
            setState { copy(contextState = RequestFormContextState.Loading) }
            viewModelScope.launch {
                val wasPhoneBlank = currentState.contactPhone.isBlank()
                coroutineScope {
                    val modesJob = launch { loadAvailableModes() }
                    val phoneJob = launch { if (wasPhoneBlank) prefillContactPhone() }
                    val policyJob = launch { loadCancellationPolicy() }
                    modesJob.join()
                    phoneJob.join()
                    policyJob.join()
                }
            }
        }

        private suspend fun loadAvailableModes() {
            val state = currentState
            when (val serviceResult = servicesRepository.getService(state.serviceId)) {
                is AppResult.Failure ->
                    setState { copy(contextState = RequestFormContextState.Error(serviceResult.error)) }
                is AppResult.Success -> {
                    val serviceModes = serviceResult.data.supportedModes
                    val doctorId = state.doctorId
                    val availableModes =
                        if (doctorId == null) {
                            serviceModes
                        } else {
                            when (val doctorResult = doctorsRepository.getDoctor(doctorId)) {
                                is AppResult.Failure -> {
                                    setState { copy(contextState = RequestFormContextState.Error(doctorResult.error)) }
                                    return
                                }
                                is AppResult.Success ->
                                    serviceModes.filter { it in doctorResult.data.consultationModes }
                            }
                        }
                    setState { copy(contextState = RequestFormContextState.Loaded(availableModes)) }
                }
            }
        }

        private suspend fun prefillContactPhone() {
            val userId = authRepository.currentUserId() ?: return
            when (val result = profileRepository.getProfile(userId)) {
                is AppResult.Success -> {
                    val phone = result.data.phone
                    if (!phone.isNullOrBlank() && currentState.contactPhone.isBlank()) {
                        setState { copy(contactPhone = phone) }
                    }
                }
                is AppResult.Failure -> Unit // decorative convenience only -- the field just starts empty.
            }
        }

        /** The Review screen's cancellation-policy line reads straight
         *  off [com.aarogya.sanjeevani.core.data.requests.BookingSettings.cancellationNoticeMinutes]
         *  -- never a hardcoded string -- so a silent failure here just
         *  means that line falls back to not showing a number, same
         *  decorative-convenience reasoning as [prefillContactPhone]. */
        private suspend fun loadCancellationPolicy() {
            when (val result = requestsRepository.getBookingSettings()) {
                is AppResult.Success ->
                    setState { copy(cancellationNoticeMinutes = result.data.cancellationNoticeMinutes) }
                is AppResult.Failure -> Unit
            }
        }

        private fun continueClicked() {
            setState { copy(submitAttempted = true, contactPhoneTouched = true, addressTouched = true) }
            if (currentState.isValid) sendEvent(RequestFormUiEvent.NavigateToReview)
        }

        private fun submit() {
            val state = currentState
            val mode = state.selectedMode
            val slotStart = state.slotStart
            if (state.isSubmitting || mode == null || slotStart == null) return

            setState { copy(isSubmitting = true) }
            viewModelScope.launch {
                val request =
                    NewServiceRequest(
                        serviceId = state.serviceId,
                        doctorId = state.doctorId,
                        slotStart = slotStart,
                        mode = mode,
                        notes = state.notes.trim().ifBlank { null },
                        contactPhone = state.contactPhone.trim(),
                        address = state.address.trim().ifBlank { null },
                    )
                when (val result = requestsRepository.createRequest(request)) {
                    is AppResult.Success -> {
                        setState { copy(isSubmitting = false, submittedRequestId = result.data) }
                        sendEvent(RequestFormUiEvent.NavigateToSuccess)
                    }
                    is AppResult.Failure -> {
                        setState { copy(isSubmitting = false) }
                        sendEvent(submitErrorEvent(result.error))
                    }
                }
            }
        }

        private fun submitErrorEvent(error: AppError): RequestFormUiEvent {
            val code = (error as? AppError.Conflict)?.code ?: (error as? AppError.Business)?.code
            return when {
                error is AppError.Conflict && code == SLOT_UNAVAILABLE_CODE -> RequestFormUiEvent.SlotUnavailable(error)
                code in DOCTOR_UNAVAILABLE_CODES -> RequestFormUiEvent.DoctorUnavailable(error)
                code in LIMIT_EXCEEDED_CODES -> RequestFormUiEvent.LimitExceeded(error)
                else -> RequestFormUiEvent.RetryableError(error)
            }
        }

        companion object {
            private const val KEY_MODE = "request_form_mode"
            private const val KEY_CONTACT_PHONE = "request_form_contact_phone"
            private const val KEY_ADDRESS = "request_form_address"
            private const val KEY_NOTES = "request_form_notes"
        }
    }
