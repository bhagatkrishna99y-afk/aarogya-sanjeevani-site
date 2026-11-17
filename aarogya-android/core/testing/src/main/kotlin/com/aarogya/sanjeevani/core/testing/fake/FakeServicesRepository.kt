package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory
import com.aarogya.sanjeevani.core.data.services.ServicesRepository

/** In-memory [ServicesRepository] seeded with [categories] and [services]. */
class FakeServicesRepository : ServicesRepository {
    val categories = mutableListOf<ServiceCategory>()
    val services = mutableListOf<Service>()

    var errorToReturn: AppError? = null

    override suspend fun getServiceCategories(): AppResult<List<ServiceCategory>> {
        errorToReturn?.let { return it.asError() }
        return categories.filter { it.isActive }.sortedBy { it.sortOrder }.asSuccess()
    }

    override suspend fun getServices(categoryId: String?): AppResult<List<Service>> {
        errorToReturn?.let { return it.asError() }
        return services
            .filter { it.isActive && (categoryId == null || it.categoryId == categoryId) }
            .sortedBy { it.sortOrder }
            .asSuccess()
    }

    /** Mirrors the real backend's RLS exactly -- flip a seeded service's
     *  `isActive` to `false` (or remove it from [services] outright) to
     *  simulate it going unavailable mid-test, no separate error flag
     *  needed for that case. */
    override suspend fun getService(serviceId: String): AppResult<Service> {
        errorToReturn?.let { return it.asError() }
        return services.firstOrNull { it.id == serviceId && it.isActive }?.asSuccess()
            ?: AppError.NotFound().asError()
    }
}
