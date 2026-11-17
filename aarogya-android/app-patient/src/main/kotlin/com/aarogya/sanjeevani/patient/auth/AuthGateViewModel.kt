package com.aarogya.sanjeevani.patient.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aarogya.sanjeevani.core.data.auth.AuthRepository
import com.aarogya.sanjeevani.core.data.auth.AuthState
import com.aarogya.sanjeevani.core.data.auth.PatientSignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * The single source of truth [com.aarogya.sanjeevani.patient.PatientApp]'s
 * auth gate (`AuthGateEffect.kt`) reacts to -- every screen-level
 * ViewModel that changes auth state (sign-in, sign-up, sign-out) goes
 * through [AuthRepository] directly and lets *this* observe the result,
 * rather than any of them navigating "to home"/"to signed-out" on their
 * own. [signOut] exists here (rather than a separate ViewModel) purely
 * because this is the one place in the Compose tree that already holds
 * an [AuthRepository] reference outside of a specific destination --
 * see `ProfileScreen.kt`'s own doc comment for why it doesn't own this
 * call itself.
 */
@HiltViewModel
class AuthGateViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        val authState: StateFlow<AuthState> =
            authRepository
                .observeAuthState()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AuthState.Loading)

        /** See [AuthRepository.signInRejections]'s own doc comment --
         *  exposed here, not from `LoginViewModel`, for exactly that
         *  reason: this ViewModel is Activity-scoped and survives the
         *  back-stack reset that would otherwise take the message with
         *  it. */
        val signInRejections: Flow<PatientSignInResult> = authRepository.signInRejections

        /** Guards against a double-tap on the confirm button of the
         *  logout dialog firing [AuthRepository.signOut] twice before the
         *  dialog has a chance to dismiss. */
        private var isSigningOut = false

        fun signOut() {
            if (isSigningOut) return
            isSigningOut = true
            viewModelScope.launch {
                try {
                    authRepository.signOut()
                } finally {
                    isSigningOut = false
                }
            }
        }
    }
