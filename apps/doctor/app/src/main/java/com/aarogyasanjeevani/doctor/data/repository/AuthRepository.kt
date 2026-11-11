package com.aarogyasanjeevani.doctor.data.repository

import com.aarogyasanjeevani.doctor.data.common.safeCall
import com.aarogyasanjeevani.doctor.data.remote.dto.ProfileDto
import com.aarogyasanjeevani.doctor.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) {
    val sessionStatus: StateFlow<SessionStatus> get() = auth.sessionStatus

    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    suspend fun signUp(fullName: String, phone: String?, email: String, password: String): Result<Unit> =
        safeCall {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", JsonPrimitive(fullName))
                    phone?.let { put("phone", JsonPrimitive(it)) }
                }
            }
        }

    suspend fun signIn(email: String, password: String): Result<Unit> = safeCall {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut(): Result<Unit> = safeCall { auth.signOut() }

    suspend fun fetchCurrentProfile(): Result<ProfileDto> = safeCall {
        val userId = currentUserId() ?: throw IllegalStateException("Not signed in")
        postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>()
    }

    suspend fun updateProfile(fullName: String, phone: String?): Result<Unit> = safeCall {
        val userId = currentUserId() ?: throw IllegalStateException("Not signed in")
        postgrest.from("profiles").update(ProfileUpdateDto(fullName = fullName, phone = phone)) {
            filter { eq("id", userId) }
        }
    }
}
