package com.aarogya.sanjeevani.core.common.error

import android.content.Context
import com.aarogya.sanjeevani.core.common.R

/** Supabase Auth's own `invalid_credentials` code (`AuthErrorCode` in
 *  supabase-kt) -- not one of this backend's AS0xx codes, but common
 *  and friendly enough to recognize everywhere sign-in happens. Public
 *  so [com.aarogya.sanjeevani.core.data.auth.AuthRepositoryImpl] can
 *  produce an [AppError.Business] with this exact code rather than
 *  retyping the string. */
const val AUTH_INVALID_CREDENTIALS_CODE = "AUTH_INVALID_CREDENTIALS"

/** Supabase Auth's `email_not_confirmed` code -- same note as
 *  [AUTH_INVALID_CREDENTIALS_CODE]. */
const val AUTH_EMAIL_NOT_CONFIRMED_CODE = "AUTH_EMAIL_NOT_CONFIRMED"

/** Supabase Auth's `email_exists`/`user_already_exists` codes -- same
 *  note as [AUTH_INVALID_CREDENTIALS_CODE]. */
const val AUTH_EMAIL_ALREADY_IN_USE_CODE = "AUTH_EMAIL_ALREADY_IN_USE"

/**
 * Every backend business code this app knows how to put a friendly face
 * on, straight from backend/docs/error-codes.md, plus the two Supabase
 * Auth codes above -- the map's keys are the stable part of that
 * contract, the string resources are not (free to reword/localize
 * without touching this file). A code with no entry here is new/
 * unregistered on the client side and falls back to a generic message
 * in [toUserMessage]/[messageFor] -- never the raw backend text.
 */
@Suppress("ktlint:standard:argument-list-wrapping")
private val AS_CODE_STRINGS: Map<String, Int> =
    mapOf(
        "AS000_NOT_AUTHENTICATED" to R.string.error_as000_not_authenticated,
        "AS001_SLOT_UNAVAILABLE" to R.string.error_as001_slot_unavailable,
        "AS002_DOCTOR_NOT_AVAILABLE" to R.string.error_as002_doctor_not_available,
        "AS003_INVALID_DATE" to R.string.error_as003_invalid_date,
        "AS004_ROLE_NOT_PATIENT" to R.string.error_as004_role_not_patient,
        "AS005_ACCOUNT_SUSPENDED" to R.string.error_as005_account_suspended,
        "AS006_SERVICE_UNAVAILABLE" to R.string.error_as006_service_unavailable,
        "AS007_DOCTOR_REQUIRED" to R.string.error_as007_doctor_required,
        "AS008_DOCTOR_NOT_OFFERED" to R.string.error_as008_doctor_not_offered,
        "AS009_MODE_UNSUPPORTED" to R.string.error_as009_mode_unsupported,
        "AS010_DUPLICATE_REQUEST" to R.string.error_as010_duplicate_request,
        "AS011_ADDRESS_REQUIRED" to R.string.error_as011_address_required,
        "AS012_INVALID_CONTACT_PHONE" to R.string.error_as012_invalid_contact_phone,
        "AS013_TEXT_TOO_LONG" to R.string.error_as013_text_too_long,
        "AS014_DOCTOR_NOT_ALLOWED" to R.string.error_as014_doctor_not_allowed,
        "AS015_REQUEST_NOT_FOUND" to R.string.error_as015_request_not_found,
        "AS016_INVALID_TRANSITION" to R.string.error_as016_invalid_transition,
        "AS017_NOT_AUTHORIZED_FOR_TRANSITION" to R.string.error_as017_not_authorized_for_transition,
        "AS018_DOCTOR_NOT_APPROVED" to R.string.error_as018_doctor_not_approved,
        "AS019_SLOT_ALREADY_BOOKED" to R.string.error_as019_slot_already_booked,
        "AS020_TOO_EARLY_TO_COMPLETE" to R.string.error_as020_too_early_to_complete,
        "AS021_CANCELLATION_NOTICE_WINDOW" to R.string.error_as021_cancellation_notice_window,
        "AS022_NOT_A_DOCTOR_ACCOUNT" to R.string.error_as022_not_a_doctor_account,
        "AS023_INVALID_DOCTOR_PROFILE_FIELDS" to R.string.error_as023_invalid_doctor_profile_fields,
        "AS024_ONLY_APPROVED_DOCTORS" to R.string.error_as024_only_approved_doctors,
        "AS025_INVALID_AVAILABILITY_WINDOW" to R.string.error_as025_invalid_availability_window,
        "AS026_AVAILABILITY_OVERLAP" to R.string.error_as026_availability_overlap,
        "AS027_NOT_AUTHORIZED" to R.string.error_as027_not_authorized,
        "AS028_DOCTOR_NOT_FOUND" to R.string.error_as028_doctor_not_found,
        "AS029_INVALID_DOCTOR_STATUS_TRANSITION" to R.string.error_as029_invalid_doctor_status_transition,
        "AS030_USER_NOT_FOUND" to R.string.error_as030_user_not_found,
        "AS031_CANNOT_DEMOTE_LAST_ADMIN" to R.string.error_as031_cannot_demote_last_admin,
        "AS032_INVALID_CATALOG_FIELDS" to R.string.error_as032_invalid_catalog_fields,
        "AS033_CATALOG_ENTITY_NOT_FOUND" to R.string.error_as033_catalog_entity_not_found,
        "AS034_TOO_MANY_ACTIVE_REQUESTS" to R.string.error_as034_too_many_active_requests,
        "AS035_RATE_LIMITED" to R.string.error_as035_rate_limited,
        AUTH_INVALID_CREDENTIALS_CODE to R.string.error_auth_invalid_credentials,
        AUTH_EMAIL_NOT_CONFIRMED_CODE to R.string.error_auth_email_not_confirmed,
        AUTH_EMAIL_ALREADY_IN_USE_CODE to R.string.error_auth_email_already_in_use,
    )

internal fun resourceForCode(code: String): Int? = AS_CODE_STRINGS[code]

/**
 * The one sanctioned way to turn an [AppError] into text a user sees.
 * Always resolves through a curated string resource -- a [Business]/
 * [NotFound]/[Conflict] with a code this client doesn't recognize (an
 * older build talking to a newer backend, say) falls back to a generic
 * category message, and the backend's own raw text is never substituted
 * in its place.
 */
fun AppError.toUserMessage(context: Context): String =
    when (this) {
        is AppError.Network ->
            context.getString(if (isTimeout) R.string.error_network_timeout else R.string.error_network)
        is AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
        is AppError.Validation ->
            fieldErrors.values.firstNotNullOfOrNull(::resourceForCode)
                ?.let(context::getString)
                ?: context.getString(R.string.error_validation_generic)
        is AppError.Business ->
            resourceForCode(code)?.let(context::getString) ?: context.getString(R.string.error_unknown)
        is AppError.NotFound ->
            code?.let(::resourceForCode)?.let(context::getString) ?: context.getString(R.string.error_not_found_generic)
        is AppError.Conflict ->
            code?.let(::resourceForCode)?.let(context::getString) ?: context.getString(R.string.error_conflict_generic)
        is AppError.Unknown -> context.getString(R.string.error_unknown)
    }

/** Resolves one [AppError.Validation.fieldErrors] entry to display next
 *  to that specific field, or `null` if [field] has no error (or its
 *  code isn't one this client recognizes -- callers should treat a
 *  `null` here the same as "no error for this field" rather than
 *  showing a generic fallback inline). */
fun AppError.Validation.messageFor(
    context: Context,
    field: String,
): String? = fieldErrors[field]?.let(::resourceForCode)?.let(context::getString)
