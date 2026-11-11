package com.aarogyasanjeevani.patient.data.common

import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

private const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again in a moment."
private const val NETWORK_ERROR_MESSAGE = "Couldn't connect. Please check your internet connection and try again."

/**
 * Runs a Supabase call and converts failures into a friendly, user-safe
 * message -- our own RPCs already raise friendly text (see backend), which
 * [RestException.message] surfaces as-is; anything else (network, unexpected
 * server errors) is never shown to the user verbatim (rule: never leak
 * internal database errors).
 */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: RestException) {
    Result.failure(UserFacingException(e.message?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR_MESSAGE))
} catch (e: IOException) {
    Result.failure(UserFacingException(NETWORK_ERROR_MESSAGE))
} catch (e: Exception) {
    Result.failure(UserFacingException(GENERIC_ERROR_MESSAGE))
}

/** An exception whose [message] is always safe to show directly to the user. */
class UserFacingException(message: String) : Exception(message)
