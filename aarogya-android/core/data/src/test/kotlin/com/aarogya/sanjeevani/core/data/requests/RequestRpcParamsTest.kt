package com.aarogya.sanjeevani.core.data.requests

import com.aarogya.sanjeevani.core.data.model.RequestStatus
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.network.rpcParams
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Regression test for the #1 BLOCKER bug: the Supabase client's shared
// `explicitNulls = false` Json instance (correct for *decoding*
// responses, see NetworkModule's own doc comment) was also used to
// *encode* outgoing RPC bodies, silently dropping any null parameter.
// PostgREST resolves which function overload to call by the exact SET
// of parameter names present in the request, and none of this backend's
// RPCs declare an SQL-level default for any parameter, so a dropped key
// made PostgREST unable to find *any* matching overload (PGRST202) --
// reproduced for real against the dev project before this fix, for a
// request with no address (every mode except home_visit).
class RequestRpcParamsTest {
    private val slotStart = Instant.parse("2026-11-18T03:30:00Z")

    @Test
    fun `create_service_request params keep every key even when address, doctorId and notes are null`() {
        val params =
            CreateServiceRequestParams(
                serviceId = "service-1",
                doctorId = null,
                slotStart = slotStart,
                mode = ServiceMode.IN_PERSON,
                notes = null,
                contactPhone = "+919000000001",
                address = null,
            )

        val json = rpcParams(CreateServiceRequestParams.serializer(), params)

        assertEquals(
            setOf("p_service_id", "p_doctor_id", "p_slot_start", "p_mode", "p_notes", "p_contact_phone", "p_address"),
            json.keys,
        )
        assertTrue("p_doctor_id must be present as an explicit null", json["p_doctor_id"].toString() == "null")
        assertTrue("p_notes must be present as an explicit null", json["p_notes"].toString() == "null")
        assertTrue("p_address must be present as an explicit null", json["p_address"].toString() == "null")
    }

    @Test
    fun `transition_request_status params keep p_reason even when null`() {
        val params =
            TransitionRequestStatusParams(requestId = "request-1", newStatus = RequestStatus.CANCELLED, reason = null)

        val json = rpcParams(TransitionRequestStatusParams.serializer(), params)

        assertEquals(setOf("p_request_id", "p_new_status", "p_reason"), json.keys)
        assertTrue("p_reason must be present as an explicit null", json["p_reason"].toString() == "null")
    }
}
