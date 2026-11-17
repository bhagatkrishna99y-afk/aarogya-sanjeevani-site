package com.aarogya.sanjeevani.core.data.profile

import com.aarogya.sanjeevani.core.data.model.UserRole
import kotlinx.datetime.Instant

/** The domain-layer shape of a `profiles` row -- no `@Serializable`,
 *  nothing wire-specific. [ProfileRepository] returns this, never
 *  [ProfileDto]; nothing above :core:data should ever import that DTO. */
data class Profile(
    val id: String,
    val role: UserRole,
    val fullName: String?,
    val phone: String?,
    val email: String,
    val avatarUrl: String?,
    val isSuspended: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun ProfileDto.toDomain(): Profile =
    Profile(
        id = id,
        role = role,
        fullName = fullName,
        phone = phone,
        email = email,
        avatarUrl = avatarUrl,
        isSuspended = isSuspended,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
