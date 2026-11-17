package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.common.result.AppResult

interface ServicesRepository {
    suspend fun getServiceCategories(): AppResult<List<ServiceCategory>>

    suspend fun getServices(categoryId: String? = null): AppResult<List<Service>>

    /** A single service by id, for the detail screen -- deliberately not
     *  filtered through [getServices]' category-scoped list, and never
     *  cached (unlike [getServiceCategories]): this is also how that
     *  screen detects a service going inactive while it's open -- RLS
     *  hides an inactive row from a non-admin caller exactly like a
     *  deleted one, so [AppError.NotFound] here means "no longer
     *  available," not just "never existed." */
    suspend fun getService(serviceId: String): AppResult<Service>
}
