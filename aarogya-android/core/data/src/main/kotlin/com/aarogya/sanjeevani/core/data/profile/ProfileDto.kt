package com.aarogya.sanjeevani.core.data.profile

import com.aarogya.sanjeevani.core.data.model.UserRole
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the `profiles` table exactly (backend/docs/kotlin-models.md,
 *  backend/docs/client-contract.md §2). */
@Serializable
data class ProfileDto(
    val id: String,
    val role: UserRole,
    @SerialName("full_name") val fullName: String?,
    val phone: String?,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("is_suspended") val isSuspended: Boolean,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
