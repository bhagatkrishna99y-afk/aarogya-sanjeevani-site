package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `create_service_request(...)` params (client-contract.md §7) --
 *  returns the new request's bare uuid, not a row (decode the RPC
 *  response as a plain `String`, no wrapper class needed). */
@Serializable
data class CreateServiceRequestParams(
    @SerialName("p_service_id") val serviceId: String,
    @SerialName("p_doctor_id") val doctorId: String?,
    @SerialName("p_slot_start") val slotStart: Instant,
    @SerialName("p_mode") val mode: ServiceMode,
    @SerialName("p_notes") val notes: String?,
    @SerialName("p_contact_phone") val contactPhone: String?,
    @SerialName("p_address") val address: String?,
)

/** `transition_request_status(...)` params -- the single entry point
 *  for every status change, for every role (client-contract.md §10). */
@Serializable
data class TransitionRequestStatusParams(
    @SerialName("p_request_id") val requestId: String,
    @SerialName("p_new_status") val newStatus: RequestStatus,
    @SerialName("p_reason") val reason: String? = null,
)
