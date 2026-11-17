package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.network.rpcParams
import com.aarogya.sanjeevani.core.network.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

class RequestsRepositoryImpl
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : RequestsRepository {
        override suspend fun getOpenSlots(
            doctorId: String,
            date: LocalDate,
        ): AppResult<List<OpenSlot>> =
            safeApiCall(retryable = true) {
                client.postgrest
                    .rpc("get_doctor_open_slots", GetDoctorOpenSlotsParams(doctorId = doctorId, date = date))
                    .decodeList<OpenSlotDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getBookingSettings(): AppResult<BookingSettings> =
            safeApiCall(retryable = true) {
                client.postgrest.rpc("booking_settings").decodeSingle<BookingSettingsDto>()
            }.map { it.toDomain() }

        override suspend fun createRequest(request: NewServiceRequest): AppResult<String> =
            // A write -- never retried automatically (see safeApiCall's doc):
            // retrying a timed-out create_service_request could double-book
            // the same slot if the first attempt actually succeeded server-side.
            safeApiCall {
                client.postgrest
                    .rpc(
                        "create_service_request",
                        rpcParams(CreateServiceRequestParams.serializer(), request.toParams()),
                    )
                    .decodeAs<String>()
            }

        override suspend fun transitionStatus(
            requestId: String,
            newStatus: RequestStatus,
            reason: String?,
        ): AppResult<Unit> =
            safeApiCall {
                client.postgrest.rpc(
                    "transition_request_status",
                    rpcParams(
                        TransitionRequestStatusParams.serializer(),
                        TransitionRequestStatusParams(requestId = requestId, newStatus = newStatus, reason = reason),
                    ),
                )
                Unit
            }

        override suspend fun getMyRequestsAsPatient(): AppResult<List<PatientRequest>> =
            safeApiCall(retryable = true) {
                client.postgrest.from("patient_requests_view")
                    .select(columns = Columns.ALL) { order("scheduled_start", Order.DESCENDING) }
                    .decodeList<PatientRequestViewDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getMyRequestsAsDoctor(): AppResult<List<DoctorRequest>> =
            safeApiCall(retryable = true) {
                client.postgrest.from("doctor_requests_view")
                    .select(columns = Columns.ALL) { order("scheduled_start", Order.DESCENDING) }
                    .decodeList<DoctorRequestViewDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getStatusHistory(requestId: String): AppResult<List<RequestStatusHistoryEntry>> =
            safeApiCall(retryable = true) {
                client.postgrest.from("request_status_history")
                    .select(columns = Columns.ALL) {
                        filter { eq("request_id", requestId) }
                        order("changed_at", Order.ASCENDING)
                    }
                    .decodeList<RequestStatusHistoryEntryDto>()
            }.map { list -> list.map { it.toDomain() } }
    }

@Serializable
private data class GetDoctorOpenSlotsParams(
    @SerialName("p_doctor_id") val doctorId: String,
    @SerialName("p_date") val date: LocalDate,
)
