package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.CancelRequestParams
import com.aarogyasanjeevani.doctor.data.remote.dto.DoctorRespondRequestParams
import com.aarogyasanjeevani.doctor.data.remote.dto.RequestIdParams
import com.aarogyasanjeevani.doctor.data.remote.dto.RequestStatusHistoryDto
import com.aarogyasanjeevani.doctor.data.remote.dto.RequestWithDetailsDto
import com.aarogyasanjeevani.doctor.domain.model.CareRequest
import com.aarogyasanjeevani.doctor.domain.model.RequestStatus
import com.aarogyasanjeevani.doctor.domain.model.RequestStatusEvent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestsRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    /** All requests assigned to the signed-in doctor, newest first. RLS already restricts this to their own rows. */
    suspend fun fetchMyRequests(): Result<List<CareRequest>> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")
        postgrest.from("requests")
            .select(Columns.raw("*, service:services(name), patient:profiles!requests_patient_id_fkey(full_name, phone)")) {
                filter { eq("doctor_id", userId) }
                order("scheduled_start", Order.ASCENDING)
            }
            .decodeList<RequestWithDetailsDto>()
            .map { it.toDomain() }
    }

    suspend fun fetchStatusHistory(requestId: String): Result<List<RequestStatusEvent>> = safeCall {
        postgrest.from("request_status_history")
            .select {
                filter { eq("request_id", requestId) }
                order("changed_at", Order.ASCENDING)
            }
            .decodeList<RequestStatusHistoryDto>()
            .map {
                RequestStatusEvent(
                    oldStatus = it.oldStatus?.let(RequestStatus::fromWire),
                    newStatus = RequestStatus.fromWire(it.newStatus),
                    note = it.note,
                    changedAt = Instant.parse(it.changedAt),
                )
            }
    }

    suspend fun respond(requestId: String, accept: Boolean, reason: String?): Result<Unit> = safeCall {
        postgrest.rpc(
            "doctor_respond_request",
            DoctorRespondRequestParams(requestId = requestId, accept = accept, reason = reason),
        )
        Unit
    }

    suspend fun confirm(requestId: String): Result<Unit> = safeCall {
        postgrest.rpc("doctor_confirm_request", RequestIdParams(requestId = requestId))
        Unit
    }

    suspend fun complete(requestId: String): Result<Unit> = safeCall {
        postgrest.rpc("doctor_complete_request", RequestIdParams(requestId = requestId))
        Unit
    }

    suspend fun cancel(requestId: String, reason: String?): Result<Unit> = safeCall {
        postgrest.rpc("cancel_request", CancelRequestParams(requestId = requestId, reason = reason))
        Unit
    }

    private fun RequestWithDetailsDto.toDomain() = CareRequest(
        id = id,
        serviceName = service?.name ?: "Service",
        patientName = patient?.fullName ?: "Patient",
        patientPhone = patient?.phone,
        scheduledStart = Instant.parse(scheduledStart),
        scheduledEnd = Instant.parse(scheduledEnd),
        status = RequestStatus.fromWire(status),
        patientNotes = patientNotes,
        doctorResponseNote = doctorResponseNote,
    )
}
