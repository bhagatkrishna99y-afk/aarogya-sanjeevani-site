package com.aarogyasanjeevani.patient.data.repository

import com.aarogyasanjeevani.patient.data.common.safeCall
import com.aarogyasanjeevani.patient.data.remote.dto.CancelRequestParams
import com.aarogyasanjeevani.patient.data.remote.dto.CreateRequestParams
import com.aarogyasanjeevani.patient.data.remote.dto.RequestStatusHistoryDto
import com.aarogyasanjeevani.patient.data.remote.dto.RequestWithDetailsDto
import com.aarogyasanjeevani.patient.domain.model.BookingRequest
import com.aarogyasanjeevani.patient.domain.model.RequestStatus
import com.aarogyasanjeevani.patient.domain.model.RequestStatusEvent
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun createRequest(
        serviceId: String,
        doctorId: String?,
        scheduledStart: Instant,
        patientNotes: String?,
    ): Result<Unit> = safeCall {
        postgrest.rpc(
            "create_request",
            CreateRequestParams(
                serviceId = serviceId,
                doctorId = doctorId,
                scheduledStart = scheduledStart.toString(),
                patientNotes = patientNotes?.takeIf { it.isNotBlank() },
            ),
        )
        Unit
    }

    suspend fun cancelRequest(requestId: String, reason: String?): Result<Unit> = safeCall {
        postgrest.rpc("cancel_request", CancelRequestParams(requestId = requestId, reason = reason))
        Unit
    }

    /** All requests for the signed-in patient, newest first. RLS already restricts this to their own rows. */
    suspend fun fetchMyRequests(): Result<List<BookingRequest>> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw IllegalStateException("Not signed in")
        postgrest.from("requests")
            .select(
                Columns.raw(
                    "*, service:services(name), doctor:profiles!requests_doctor_id_fkey(full_name)",
                ),
            ) {
                filter { eq("patient_id", userId) }
                order("created_at", Order.DESCENDING)
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

    private fun RequestWithDetailsDto.toDomain() = BookingRequest(
        id = id,
        serviceId = serviceId,
        serviceName = service?.name ?: "Service",
        doctorId = doctorId,
        doctorName = doctor?.fullName,
        scheduledStart = Instant.parse(scheduledStart),
        scheduledEnd = Instant.parse(scheduledEnd),
        status = RequestStatus.fromWire(status),
        patientNotes = patientNotes,
        doctorResponseNote = doctorResponseNote,
    )
}
