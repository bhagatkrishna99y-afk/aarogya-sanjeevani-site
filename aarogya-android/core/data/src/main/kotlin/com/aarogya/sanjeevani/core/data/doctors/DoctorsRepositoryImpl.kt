package com.aarogya.sanjeevani.core.data.doctors

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.data.model.ServiceMode
import com.aarogya.sanjeevani.core.network.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

private const val DOCTORS_PUBLIC = "doctors_public"
private const val DOCTOR_SERVICES_PUBLIC = "doctor_services_public"
private const val AVATAR_BUCKET = "doctor-avatars"
private val AVATAR_SIGNED_URL_TTL = 1.hours

/** The Postgres enum label each [ServiceMode] is stored/filtered as --
 *  see `core.data.model.Enums.kt`'s own `@SerialName`s; needed here too
 *  since a query *filter value* (unlike a decoded response) never goes
 *  through the serializer. */
private fun ServiceMode.wireValue(): String =
    when (this) {
        ServiceMode.IN_PERSON -> "in_person"
        ServiceMode.HOME_VISIT -> "home_visit"
        ServiceMode.ONLINE -> "online"
    }

/** Direct, publicly-readable view reads (client-contract.md §4-5) --
 *  all idempotent, so all retry on a transient connectivity failure. */
class DoctorsRepositoryImpl
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : DoctorsRepository {
        override suspend fun getAllDoctors(): AppResult<List<DoctorPublic>> =
            safeApiCall(retryable = true) {
                client.postgrest.from(DOCTORS_PUBLIC)
                    .select(columns = Columns.ALL)
                    .decodeList<DoctorPublicDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getDoctorsPage(
            offset: Int,
            limit: Int,
            searchQuery: String?,
            specialization: String?,
            mode: ServiceMode?,
        ): AppResult<List<DoctorPublic>> =
            safeApiCall(retryable = true) {
                client.postgrest.from(DOCTORS_PUBLIC)
                    .select(columns = Columns.ALL) {
                        filter {
                            if (!searchQuery.isNullOrBlank()) {
                                or {
                                    ilike("full_name", "%$searchQuery%")
                                    ilike("specialization", "%$searchQuery%")
                                }
                            }
                            specialization?.let { eq("specialization", it) }
                            mode?.let { contains("consultation_modes", listOf(it.wireValue())) }
                        }
                        order("full_name", Order.ASCENDING)
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                    .decodeList<DoctorPublicDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getDoctor(doctorId: String): AppResult<DoctorPublic> {
            val result =
                safeApiCall(retryable = true) {
                    client.postgrest.from(DOCTORS_PUBLIC)
                        .select(columns = Columns.ALL) { filter { eq("doctor_id", doctorId) } }
                        .decodeList<DoctorPublicDto>()
                }
            return when (result) {
                is AppResult.Success ->
                    result.data.firstOrNull()?.toDomain()?.asSuccess() ?: AppError.NotFound().asError()
                is AppResult.Failure -> result
            }
        }

        override suspend fun getDoctorsByIds(doctorIds: List<String>): AppResult<List<DoctorPublic>> {
            if (doctorIds.isEmpty()) return emptyList<DoctorPublic>().asSuccess()
            return safeApiCall(retryable = true) {
                client.postgrest.from(DOCTORS_PUBLIC)
                    .select(columns = Columns.ALL) { filter { isIn("doctor_id", doctorIds) } }
                    .decodeList<DoctorPublicDto>()
            }.map { list -> list.map { it.toDomain() } }
        }

        override suspend fun getDoctorsForService(serviceId: String): AppResult<List<DoctorServicePublic>> =
            safeApiCall(retryable = true) {
                client.postgrest.from(DOCTOR_SERVICES_PUBLIC)
                    .select(columns = Columns.ALL) { filter { eq("service_id", serviceId) } }
                    .decodeList<DoctorServicePublicDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getServicesForDoctor(doctorId: String): AppResult<List<DoctorServicePublic>> =
            safeApiCall(retryable = true) {
                client.postgrest.from(DOCTOR_SERVICES_PUBLIC)
                    .select(columns = Columns.ALL) { filter { eq("doctor_id", doctorId) } }
                    .decodeList<DoctorServicePublicDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getServicesForDoctors(doctorIds: List<String>): AppResult<List<DoctorServicePublic>> {
            if (doctorIds.isEmpty()) return emptyList<DoctorServicePublic>().asSuccess()
            return safeApiCall(retryable = true) {
                client.postgrest.from(DOCTOR_SERVICES_PUBLIC)
                    .select(columns = Columns.ALL) { filter { isIn("doctor_id", doctorIds) } }
                    .decodeList<DoctorServicePublicDto>()
            }.map { list -> list.map { it.toDomain() } }
        }

        override suspend fun getAvatarUrls(avatarPaths: List<String>): Map<String, String> {
            if (avatarPaths.isEmpty()) return emptyMap()
            val result =
                safeApiCall(retryable = true) {
                    client.storage.from(AVATAR_BUCKET).createSignedUrls(AVATAR_SIGNED_URL_TTL, avatarPaths)
                }
            return when (result) {
                is AppResult.Success ->
                    result.data.filter { it.signedURL.isNotBlank() }.associate { it.path to it.signedURL }
                is AppResult.Failure -> emptyMap()
            }
        }
    }
