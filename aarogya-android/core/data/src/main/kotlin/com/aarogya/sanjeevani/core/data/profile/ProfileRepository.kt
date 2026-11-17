package com.aarogya.sanjeevani.core.data.profile

import com.aarogya.sanjeevani.core.common.result.AppResult

interface ProfileRepository {
    suspend fun getProfile(userId: String): AppResult<Profile>

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        phone: String?,
    ): AppResult<Profile>

    /** Uploads [bytes] to the caller's own folder in the private
     *  `doctor-avatars` bucket (own-folder read/write/delete policies
     *  there are role-agnostic -- see that migration's own comment --
     *  so a patient uploading to their own path works the same way a
     *  doctor's does) at a fixed filename, `upsert`-ing so a repeat
     *  upload replaces the previous photo at the same path rather than
     *  accumulating orphaned objects. Then points `profiles.avatar_url`
     *  at that path (a permitted column -- client-contract.md §2) and
     *  returns the refreshed [Profile]. [contentType] must be one of
     *  the bucket's allowed mime types (`image/jpeg`, `image/png`,
     *  `image/webp`) or the Storage service itself rejects the upload. */
    suspend fun uploadAvatar(
        userId: String,
        bytes: ByteArray,
        contentType: String,
    ): AppResult<Profile>

    /** `avatar_url` is a private-bucket path, never a public URL --
     *  resolve it to a short-lived signed URL right before display,
     *  same reasoning as [com.aarogya.sanjeevani.core.data.doctors.DoctorsRepository.getAvatarUrls]. */
    suspend fun getAvatarSignedUrl(path: String): AppResult<String>
}
