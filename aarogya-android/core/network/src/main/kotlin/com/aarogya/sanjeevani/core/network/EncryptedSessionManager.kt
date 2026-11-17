package com.aarogya.sanjeevani.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.aarogya.sanjeevani.core.common.AppConstants
import com.aarogya.sanjeevani.core.common.di.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val SESSION_KEY = "supabase_user_session"

/**
 * Backs supabase-kt's [SessionManager] extension point (installed via
 * `NetworkModule`'s `install(Auth) { sessionManager = ... }`) with
 * [EncryptedSharedPreferences] instead of its own default (plain
 * SharedPreferences via `SettingsSessionManager`) -- the access/refresh
 * tokens in a [UserSession] must never sit on disk in plain text.
 *
 * With this wired in, the Auth plugin's own `autoLoadFromStorage`/
 * `autoSaveToStorage`/`alwaysAutoRefresh` (all on by default --
 * AuthConfig.kt) do the actual session-restore-on-start and
 * token-refresh work; this class is only the storage backend they read
 * and write through -- see `AuthRepository`/`AuthState` (:core:data)
 * for how the app reacts to the resulting [io.github.jan.supabase.auth.status.SessionStatus]
 * changes.
 *
 * Never logs a [UserSession] -- its own `toString()` already masks
 * every token, and this class never calls the explicit `unsafeToString()`
 * escape hatch supabase-kt provides for debugging.
 */
@Singleton
class EncryptedSessionManager
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val dispatcherProvider: DispatcherProvider,
    ) : SessionManager {
        private val json = Json { ignoreUnknownKeys = true }

        private val preferences: SharedPreferences by lazy {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                AppConstants.SESSION_PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        override suspend fun saveSession(session: UserSession) {
            withContext(dispatcherProvider.io) {
                preferences.edit().putString(SESSION_KEY, json.encodeToString(session)).apply()
            }
        }

        // Supabase's Auth plugin treats a `null` return as "nothing in
        // storage" and simply stays SessionStatus.NotAuthenticated -- it
        // never catches an exception thrown from here (verified against
        // AuthImpl.loadFromStorage), so returning null is the contract, not
        // a throw.
        override suspend fun loadSession(): UserSession? =
            withContext(dispatcherProvider.io) {
                preferences.getString(SESSION_KEY, null)?.let { json.decodeFromString<UserSession>(it) }
            }

        override suspend fun deleteSession() {
            withContext(dispatcherProvider.io) {
                preferences.edit().remove(SESSION_KEY).apply()
            }
        }
    }
