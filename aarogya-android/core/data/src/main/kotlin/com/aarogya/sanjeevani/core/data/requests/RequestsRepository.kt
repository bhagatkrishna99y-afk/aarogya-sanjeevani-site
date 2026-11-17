package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import kotlinx.datetime.LocalDate

/**
 * The current (non-legacy) request-lifecycle surface
 * (client-contract.md §6-10) -- `get_doctor_open_slots`,
 * `create_service_request`, and the single `transition_request_status`
 * entry point for every status change, for every role. **Not**
 * `get_available_slots`/`create_request`/`cancel_request`/
 * `doctor_respond_request`/`doctor_confirm_request`/
 * `doctor_complete_request`, which are explicitly superseded legacy
 * functions (see the "Legacy functions" section at the bottom of that
 * doc) -- mixing them in would bypass the rate limits and the unified
 * notification/audit side effects the current functions provide.
 */
interface RequestsRepository {
    /** `get_doctor_open_slots(p_doctor_id, p_date)` -- an empty list
     *  means "nothing open that day," not an error. */
    suspend fun getOpenSlots(
        doctorId: String,
        date: LocalDate,
    ): AppResult<List<OpenSlot>>

    /** `booking_settings()` -- always exactly one row; the Availability
     *  screen's "Pick another date" range comes from this, never a
     *  hardcoded horizon. */
    suspend fun getBookingSettings(): AppResult<BookingSettings>

    /** `create_service_request(...)` -- returns the new request's id. */
    suspend fun createRequest(request: NewServiceRequest): AppResult<String>

    /** `transition_request_status(p_request_id, p_new_status, p_reason)`
     *  -- the one entry point for every status change; who may call
     *  which transition is enforced server-side (client-contract.md
     *  §10's table), not re-implemented here. */
    suspend fun transitionStatus(
        requestId: String,
        newStatus: RequestStatus,
        reason: String? = null,
    ): AppResult<Unit>

    /** `select * from patient_requests_view order by scheduled_start
     *  desc` -- no parameters; RLS already scopes this to the caller's
     *  own requests. */
    suspend fun getMyRequestsAsPatient(): AppResult<List<PatientRequest>>

    /** `select * from doctor_requests_view order by scheduled_start
     *  desc` -- no parameters; RLS already scopes this to the caller's
     *  own requests. */
    suspend fun getMyRequestsAsDoctor(): AppResult<List<DoctorRequest>>

    /** `select * from request_status_history where request_id = ? order
     *  by changed_at` -- the status timeline for one request. */
    suspend fun getStatusHistory(requestId: String): AppResult<List<RequestStatusHistoryEntry>>
}
