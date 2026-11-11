package com.aarogyasanjeevani.patient.data.repository

import com.aarogyasanjeevani.patient.data.common.safeCall
import com.aarogyasanjeevani.patient.data.remote.dto.ServiceCategoryWithServicesDto
import com.aarogyasanjeevani.patient.data.remote.dto.ServiceDto
import com.aarogyasanjeevani.patient.domain.model.Service
import com.aarogyasanjeevani.patient.domain.model.ServiceCategory
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val postgrest: Postgrest,
) {
    /** Public, active service catalog grouped by category. Readable while signed out. */
    suspend fun fetchCatalog(): Result<List<ServiceCategory>> = safeCall {
        postgrest.from("service_categories")
            .select(Columns.raw("*, services(*)")) {
                filter { eq("is_active", true) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<ServiceCategoryWithServicesDto>()
            .map { category ->
                ServiceCategory(
                    id = category.id,
                    name = category.name,
                    description = category.description,
                    services = category.services
                        .filter { it.isActive }
                        .sortedBy { it.sortOrder }
                        .map { service ->
                            Service(
                                id = service.id,
                                categoryId = service.categoryId,
                                name = service.name,
                                description = service.description,
                                requiresDoctor = service.requiresDoctor,
                                durationMinutes = service.durationMinutes,
                            )
                        },
                )
            }
    }

    suspend fun fetchService(serviceId: String): Result<Service> = safeCall {
        val dto = postgrest.from("services")
            .select { filter { eq("id", serviceId) } }
            .decodeSingle<ServiceDto>()
        Service(
            id = dto.id,
            categoryId = dto.categoryId,
            name = dto.name,
            description = dto.description,
            requiresDoctor = dto.requiresDoctor,
            durationMinutes = dto.durationMinutes,
        )
    }
}
