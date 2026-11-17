package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic

/** [Content] with an empty [Content.doctors] is a legitimate, expected
 *  outcome here -- not every service has a doctor signed up for it yet
 *  -- so it's deliberately not folded into [Error]; the screen renders
 *  it as its own "no doctors currently offer this service" state with
 *  a way back to Services, not a retry. */
sealed interface DoctorListForServiceSectionState {
    data object Loading : DoctorListForServiceSectionState

    data class Error(val error: AppError) : DoctorListForServiceSectionState

    data class Content(
        val doctors: List<DoctorPublic>,
        val avatarUrlsByDoctorId: Map<String, String> = emptyMap(),
    ) : DoctorListForServiceSectionState
}

/** [serviceName] is set the moment [DoctorListForServiceAction.Load]
 *  fires (from the nav arg, before any network read completes) so the
 *  pinned header has something to show through every [section] state,
 *  loading and error included. */
data class DoctorListForServiceUiState(
    val serviceName: String = "",
    val section: DoctorListForServiceSectionState = DoctorListForServiceSectionState.Loading,
)

sealed interface DoctorListForServiceAction {
    data class Load(val serviceId: String, val serviceName: String) : DoctorListForServiceAction

    data object Retry : DoctorListForServiceAction
}
