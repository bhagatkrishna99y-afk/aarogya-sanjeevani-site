package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.data.services.Service

/**
 * [Error] only ever happens on a first load that fails outright (a bad
 * id, no connectivity) -- there's nothing to show instead. Once
 * [Content] is reached, a later background re-check (see
 * [ServiceDetailViewModel]'s doc comment) that comes back
 * [AppError.NotFound] flips [Content.isUnavailable] rather than
 * regressing to [Error]: the patient is already looking at the
 * service's details, and "this went away" is a different, more useful
 * message than a generic failure would be. Any *other* re-check failure
 * (a network blip) changes nothing -- the last-known-good [Content]
 * just stays on screen.
 */
sealed interface ServiceDetailUiState {
    data object Loading : ServiceDetailUiState

    data class Error(val error: AppError) : ServiceDetailUiState

    data class Content(
        val service: Service,
        val categoryName: String?,
        val isUnavailable: Boolean = false,
    ) : ServiceDetailUiState
}

/** [Resumed] fires from `LifecycleEventEffect(ON_RESUME)` -- both the
 *  screen's first appearance and every time it's resumed, same
 *  unification [com.aarogya.sanjeevani.patient.home.HomeViewModel] uses
 *  and for the same reason (see this file's own doc comment on why a
 *  resume here specifically needs to re-check, not just refresh). */
sealed interface ServiceDetailAction {
    data class Resumed(val serviceId: String) : ServiceDetailAction

    data object Retry : ServiceDetailAction
}
