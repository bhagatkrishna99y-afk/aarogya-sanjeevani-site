package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.network.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

/** Direct, publicly-readable table reads (client-contract.md §3) --
 *  both idempotent, so both retry on a transient connectivity failure. */
class ServicesRepositoryImpl
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : ServicesRepository {
        override suspend fun getServiceCategories(): AppResult<List<ServiceCategory>> =
            safeApiCall(retryable = true) {
                client.postgrest.from("service_categories")
                    .select(columns = Columns.ALL) {
                        filter { eq("is_active", true) }
                        order("sort_order", Order.ASCENDING)
                    }
                    .decodeList<ServiceCategoryDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getServices(categoryId: String?): AppResult<List<Service>> =
            safeApiCall(retryable = true) {
                client.postgrest.from("services")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("is_active", true)
                            categoryId?.let { eq("category_id", it) }
                        }
                        order("sort_order", Order.ASCENDING)
                    }
                    .decodeList<ServiceDto>()
            }.map { list -> list.map { it.toDomain() } }

        override suspend fun getService(serviceId: String): AppResult<Service> {
            val result =
                safeApiCall(retryable = true) {
                    client.postgrest.from("services")
                        .select(columns = Columns.ALL) { filter { eq("id", serviceId) } }
                        .decodeList<ServiceDto>()
                }
            return when (result) {
                is AppResult.Success ->
                    result.data.firstOrNull()?.toDomain()?.asSuccess() ?: AppError.NotFound().asError()
                is AppResult.Failure -> result
            }
        }
    }
