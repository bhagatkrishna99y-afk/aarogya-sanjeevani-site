package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement

data class ServiceCategory(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val icon: String?,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Service(
    val id: String,
    val categoryId: String,
    val name: String,
    val slug: String,
    val description: String?,
    val shortDescription: String?,
    val requiresDoctor: Boolean,
    val durationMinutes: Int,
    val supportedModes: List<ServiceMode>,
    val metadata: JsonElement?,
    val isActive: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun ServiceCategoryDto.toDomain(): ServiceCategory =
    ServiceCategory(
        id = id,
        name = name,
        slug = slug,
        description = description,
        icon = icon,
        isActive = isActive,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ServiceDto.toDomain(): Service =
    Service(
        id = id,
        categoryId = categoryId,
        name = name,
        slug = slug,
        description = description,
        shortDescription = shortDescription,
        requiresDoctor = requiresDoctor,
        durationMinutes = durationMinutes,
        supportedModes = supportedModes,
        metadata = metadata,
        isActive = isActive,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
