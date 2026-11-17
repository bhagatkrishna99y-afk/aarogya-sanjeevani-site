package com.aarogya.sanjeevani.core.data.auth

import com.aarogya.sanjeevani.core.data.model.UserRole

/**
 * The outcome of [AuthRepository.signInAsPatient] -- credentials were
 * valid (a real auth failure, e.g. wrong password, comes back as an
 * [AppResult.Failure] instead), but that's not the whole story for this
 * app: the Patient app only wants patient accounts signed in at all,
 * and a suspended account shouldn't get in even if it is one. Every
 * variant except [Success] has already been signed back out again by
 * the time this is returned -- see [AuthRepositoryImpl.signInAsPatient].
 */
sealed interface PatientSignInResult {
    data class Success(val userId: String) : PatientSignInResult

    /** A real account, just not a patient one -- a doctor or admin
     *  account signing in to the wrong app. */
    data class WrongRole(val role: UserRole) : PatientSignInResult

    data object AccountSuspended : PatientSignInResult
}
