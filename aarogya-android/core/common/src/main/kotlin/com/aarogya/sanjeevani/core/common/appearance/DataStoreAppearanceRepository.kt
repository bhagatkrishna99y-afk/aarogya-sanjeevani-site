package com.aarogya.sanjeevani.core.common.appearance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The delegate itself is the singleton -- one `DataStore<Preferences>`
 *  per process regardless of how many [DataStoreAppearanceRepository]
 *  instances exist (there's only ever one, `@Singleton`-bound, but this
 *  is also just how `preferencesDataStore` is meant to be used). */
private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(name = "appearance")

private val APPEARANCE_MODE_KEY = stringPreferencesKey("appearance_mode")

@Singleton
class DataStoreAppearanceRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppearanceRepository {
        override fun observeAppearanceMode(): Flow<AppearanceMode> =
            context.appearanceDataStore.data.map { prefs ->
                prefs[APPEARANCE_MODE_KEY]
                    ?.let { stored -> runCatching { AppearanceMode.valueOf(stored) }.getOrNull() }
                    ?: AppearanceMode.SYSTEM
            }

        override suspend fun setAppearanceMode(mode: AppearanceMode) {
            context.appearanceDataStore.edit { it[APPEARANCE_MODE_KEY] = mode.name }
        }
    }
