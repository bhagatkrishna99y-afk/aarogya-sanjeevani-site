package com.aarogya.sanjeevani.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Encodes RPC call parameters with every key always present, even when
 * its value is `null` -- the opposite of [NetworkModule]'s `supabaseJson`
 * (`explicitNulls = false`, which is correct for *decoding* responses,
 * see its own doc comment). Postgres RPC parameter resolution over
 * PostgREST matches a call against a function overload by the exact SET
 * of parameter names present in the request body; every RPC this app
 * calls declares its parameters with no SQL-level `default`, so dropping
 * a key whose Kotlin value happens to be null (e.g. `p_address` when the
 * mode isn't `home_visit`) makes PostgREST unable to find *any* matching
 * overload at all (`PGRST202`), not merely pass `null` through. Encoding
 * with a [KSerializer] into a [JsonObject] first, then handing that
 * already-built [JsonObject] to `rpc(...)`, sidesteps the client's
 * shared, `explicitNulls = false` default serializer entirely -- once a
 * value is a [kotlinx.serialization.json.JsonElement] tree rather than a
 * plain Kotlin object, re-serializing it is a direct, lossless dump of
 * that tree (`explicitNulls` only governs the object-to-element step,
 * never element-to-string).
 *
 * `encodeDefaults = true` for the same reason, one level up: a parameter
 * declared with a Kotlin default (e.g. `TransitionRequestStatusParams
 * .reason: String? = null`) is omitted whenever its value *equals* that
 * default -- regardless of `explicitNulls` -- unless this is also set.
 * Caught by this file's own regression test, not by inspection: without
 * it, a call passing `reason = null` (accepting/completing/cancelling
 * with no reason given) would still drop `p_reason` and hit the exact
 * same `PGRST202` this file exists to prevent.
 */
private val rpcParamsJson =
    Json {
        explicitNulls = true
        encodeDefaults = true
    }

fun <T> rpcParams(
    serializer: KSerializer<T>,
    value: T,
): JsonObject = rpcParamsJson.encodeToJsonElement(serializer, value) as JsonObject
