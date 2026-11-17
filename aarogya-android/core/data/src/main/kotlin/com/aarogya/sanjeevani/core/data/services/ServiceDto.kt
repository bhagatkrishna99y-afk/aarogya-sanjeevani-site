package com.aarogya.sanjeevani.core.data.services

import com.aarogya.sanjeevani.core.data.model.ServiceMode
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Mirrors the `service_categories` table (backend/docs/kotlin-models.md,
 *  client-contract.md §3). */
@Serializable
data class ServiceCategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val icon: String?,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

/** Mirrors the `services` table (client-contract.md §3). [metadata] is a
 *  free-form `jsonb` column with no fixed shape across services -- kept
 *  as raw [JsonElement] rather than guessing at a shape, per kotlin-
 *  models.md's own note on this column. */
@Serializable
data class ServiceDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    val name: String,
    val slug: String,
    val description: String?,
    @SerialName("short_description") val shortDescription: String?,
    @SerialName("requires_doctor") val requiresDoctor: Boolean,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("supported_modes") val supportedModes: List<ServiceMode>,
    val metadata: JsonElement? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
