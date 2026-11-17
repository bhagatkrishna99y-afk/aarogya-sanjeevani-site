package com.aarogya.sanjeevani.patient.services

import com.aarogya.sanjeevani.core.data.services.Service
import com.aarogya.sanjeevani.core.data.services.ServiceCategory

/** One category's section in the grouped list -- never constructed with
 *  an empty [services] list (see [groupServicesForDisplay]), so the
 *  screen never has to special-case rendering a header with nothing
 *  under it. */
data class ServiceGroup(
    val category: ServiceCategory,
    val services: List<Service>,
)

/**
 * The Services list's one piece of real logic, kept framework-free and
 * pure so it's testable with plain fixture data -- no ViewModel,
 * dispatcher, or Compose runtime needed. Applies, in order:
 * 1. [selectedCategoryId] (`null` = every category), then
 * 2. a case-insensitive substring match of [searchQuery] against each
 *    service's name (a blank query matches everything), then
 * 3. groups what's left by category, sorted by each category's own
 *    [ServiceCategory.sortOrder], each category's services sorted by
 *    their own [Service.sortOrder].
 *
 * A category with zero matching services after filtering is dropped
 * entirely -- its [SectionHeader][com.aarogya.sanjeevani.core.designsystem.component.SectionHeader]
 * would otherwise sit above an empty section, which is exactly the
 * "search matched nothing" case the caller already renders a dedicated
 * empty state for instead.
 */
fun groupServicesForDisplay(
    categories: List<ServiceCategory>,
    services: List<Service>,
    selectedCategoryId: String?,
    searchQuery: String,
): List<ServiceGroup> {
    val matching =
        services.filter { service ->
            (selectedCategoryId == null || service.categoryId == selectedCategoryId) &&
                (searchQuery.isBlank() || service.name.contains(searchQuery.trim(), ignoreCase = true))
        }
    val byCategoryId = matching.groupBy { it.categoryId }
    return categories
        .sortedBy { it.sortOrder }
        .mapNotNull { category ->
            val inCategory = byCategoryId[category.id]?.sortedBy { it.sortOrder }
            if (inCategory.isNullOrEmpty()) null else ServiceGroup(category, inCategory)
        }
}
