package com.aarogya.sanjeevani.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * The one place a [SupabaseClient] gets built -- every repository in
 * :core:data injects the same singleton rather than any of them
 * constructing their own.
 *
 * [Json] is configured explicitly rather than left at supabase-kt's own
 * default (which only sets `ignoreUnknownKeys`):
 * - `ignoreUnknownKeys = true` -- a column/view this DTO hasn't been
 *   taught about yet (a migration landed, docs/kotlin-models.md hasn't
 *   been hand-updated for it yet) must never crash decoding.
 * - `explicitNulls = false` -- a response field this DTO's Kotlin type
 *   makes nullable-with-a-default can simply be absent from the JSON
 *   (PostgREST never omits a column, but an RPC's jsonb result or a
 *   future partial-select can) rather than requiring a literal `null`.
 * - `coerceInputValues = true` -- an enum value this build doesn't know
 *   about yet (the backend added a new `request_status`/`service_mode`
 *   label before this app shipped support for it) falls back to that
 *   property's default instead of failing the whole decode.
 */
private val supabaseJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(
        config: SupabaseConfig,
        sessionManager: EncryptedSessionManager,
    ): SupabaseClient =
        createSupabaseClient(
            supabaseUrl = config.url,
            supabaseKey = config.anonKey,
        ) {
            defaultSerializer = KotlinXSerializer(supabaseJson)
            install(Auth) {
                // Session restore-on-start and token refresh both read/write
                // through this at Auth-plugin-init time (the moment
                // `install(Auth)` runs, right here) -- setting it any later
                // than this would miss the initial auto-load-from-storage
                // step, so it can't be wired in post-construction from
                // :core:data (which is also why this lives in :core:network
                // rather than :core:data, despite "session handling" being
                // scoped to :core:data elsewhere in this app -- this is the
                // one piece that has to live where the client is built).
                this.sessionManager = sessionManager
            }
            install(Postgrest)
            install(Storage)
        }
}
