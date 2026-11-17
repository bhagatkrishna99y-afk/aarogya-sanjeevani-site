package com.aarogya.sanjeevani.core.testing.fake

import com.aarogya.sanjeevani.core.common.error.AppError
import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.asError
import com.aarogya.sanjeevani.core.common.result.asSuccess
import com.aarogya.sanjeevani.core.data.model.UserRole
import com.aarogya.sanjeevani.core.data.profile.Profile
import com.aarogya.sanjeevani.core.data.profile.ProfileRepository
import kotlinx.datetime.Instant

/** In-memory [ProfileRepository] seeded with [profiles] by user id. */
class FakeProfileRepository : ProfileRepository {
    val profiles =
        mutableMapOf(
            FakeAuthRepository.FAKE_USER_ID to
                Profile(
                    id = FakeAuthRepository.FAKE_USER_ID,
                    role = UserRole.USER,
                    fullName = "Fake Patient",
                    phone = null,
                    email = "patient@example.com",
                    avatarUrl = null,
                    isSuspended = false,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                ),
        )

    var errorToReturn: AppError? = null

    /** Keyed by the path [uploadAvatar] wrote -- seed this so
     *  [getAvatarSignedUrl] has something to resolve. */
    val signedUrlsByPath = mutableMapOf<String, String>()
    var uploadAvatarCallCount = 0
    var updateProfileCallCount = 0

    override suspend fun getProfile(userId: String): AppResult<Profile> {
        errorToReturn?.let { return it.asError() }
        return profiles[userId]?.asSuccess() ?: AppError.NotFound().asError()
    }

    override suspend fun updateProfile(
        userId: String,
        fullName: String,
        phone: String?,
    ): AppResult<Profile> {
        updateProfileCallCount++
        errorToReturn?.let { return it.asError() }
        val existing = profiles[userId]
        return if (existing == null) {
            AppError.NotFound().asError()
        } else {
            existing.copy(fullName = fullName, phone = phone).also { profiles[userId] = it }.asSuccess()
        }
    }

    override suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray,
        contentType: String,
    ): AppResult<Profile> {
        uploadAvatarCallCount++
        val error = errorToReturn ?: if (profiles[userId] == null) AppError.NotFound() else null
        if (error != null) return error.asError()

        val path = "$userId/avatar.jpg"
        return profiles.getValue(userId).copy(avatarUrl = path).also { profiles[userId] = it }.asSuccess()
    }

    override suspend fun getAvatarSignedUrl(path: String): AppResult<String> {
        errorToReturn?.let { return it.asError() }
        return signedUrlsByPath[path]?.asSuccess() ?: AppError.NotFound().asError()
    }
}
