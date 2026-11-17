package com.aarogya.sanjeevani.patient.doctors

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.doctors.DoctorPublic
import com.aarogya.sanjeevani.core.data.doctors.DoctorServicePublic

/** No `NotFound`/"unavailable" state here -- see [DoctorDetailUiEvent.DoctorUnavailable]:
 *  a doctor who's gone by the time this screen's *first* load runs
 *  (reached from a directory snapshot that's since gone stale) is
 *  handled as a one-off event instead, since there's nothing useful to
 *  keep showing in its place -- the friendly message and the navigate-
 *  back both happen once, not a screen state the patient sits on. */
sealed interface DoctorDetailUiState {
    data object Loading : DoctorDetailUiState

    data class Error(val error: AppError) : DoctorDetailUiState

    /** [services] backs both "Services offered" on the page itself and
     *  the service-picker bottom sheet (see `DoctorDetailScreen.kt`) --
     *  a failure fetching it is never allowed to block the rest of this
     *  doctor's profile from showing, same "secondary read, non-fatal"
     *  rule `ServiceDetailViewModel`'s category-name lookup follows --
     *  it just falls back to an empty list. */
    data class Content(
        val doctor: DoctorPublic,
        val avatarUrl: String?,
        val services: List<DoctorServicePublic>,
    ) : DoctorDetailUiState
}

/** [DoctorUnavailable] is the one event this screen ever sends -- see
 *  [DoctorDetailUiState]'s own doc comment for why this is an event,
 *  not a state. */
sealed interface DoctorDetailUiEvent {
    data object DoctorUnavailable : DoctorDetailUiEvent
}

sealed interface DoctorDetailAction {
    data class Load(val doctorId: String) : DoctorDetailAction

    data object Retry : DoctorDetailAction
}
