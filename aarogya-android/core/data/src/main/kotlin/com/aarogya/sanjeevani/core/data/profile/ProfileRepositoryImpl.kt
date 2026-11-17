package com.aarogya.sanjeevani.core.data.profile

import com.aarogya.sanjeevani.core.common.result.AppResult
import com.aarogya.sanjeevani.core.common.result.map
import com.aarogya.sanjeevani.core.network.safeApiCall
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

private const val TABLE = "profiles"
private const val AVATAR_BUCKET = "doctor-avatars"
private const val AVATAR_FILENAME = "avatar.jpg"
private val AVATAR_SIGNED_URL_TTL = 1.hours

/** Direct table read/update, RLS-scoped to the caller's own row
 *  (client-contract.md §2) -- no RPC involved, unlike most of the rest
 *  of this API surface. */
class ProfileRepositoryImpl
    @Inject
    constructor(
        private val client: SupabaseClient,
    ) : ProfileRepository {
        override suspend fun getProfile(userId: String): AppResult<Profile> =
            safeApiCall(retryable = true) {
                client.postgrest.from(TABLE)
                    .select(columns = Columns.ALL) { filter { eq("id", userId) } }
                    .decodeSingle<ProfileDto>()
            }.map { it.toDomain() }

        override suspend fun updateProfile(
            userId: String,
            fullName: String,
            phone: String?,
        ): AppResult<Profile> =
            safeApiCall {
                client.postgrest.from(TABLE)
                    .update(ProfileUpdate(fullName = fullName, phone = phone)) {
                        filter { eq("id", userId) }
                        select()
                    }
                    .decodeSingle<ProfileDto>()
            }.map { it.toDomain() }

        override suspend fun uploadAvatar(
            userId: String,
            bytes: ByteArray,
            contentType: String,
        ): AppResult<Profile> {
            val path = "$userId/$AVATAR_FILENAME"
            val uploadResult =
                safeApiCall {
                    client.storage.from(AVATAR_BUCKET).upload(path, bytes) {
                        upsert = true
                        this.contentType = ContentType.parse(contentType)
                    }
                }
            if (uploadResult is AppResult.Failure) return uploadResult

            return safeApiCall {
                client.postgrest.from(TABLE)
                    .update(ProfileAvatarUpdate(avatarUrl = path)) {
                        filter { eq("id", userId) }
                        select()
                    }
                    .decodeSingle<ProfileDto>()
            }.map { it.toDomain() }
        }

        override suspend fun getAvatarSignedUrl(path: String): AppResult<String> =
            safeApiCall(retryable = true) {
                client.storage.from(AVATAR_BUCKET).createSignedUrl(path, AVATAR_SIGNED_URL_TTL)
            }
    }

@Serializable
private data class ProfileUpdate(
    @SerialName("full_name") val fullName: String,
    val phone: String?,
)

@Serializable
private data class ProfileAvatarUpdate(
    @SerialName("avatar_url") val avatarUrl: String,
)
