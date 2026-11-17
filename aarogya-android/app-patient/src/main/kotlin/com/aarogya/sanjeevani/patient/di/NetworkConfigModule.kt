package com.aarogya.sanjeevani.patient.di

import com.aarogya.sanjeevani.core.network.SupabaseConfig
import com.aarogya.sanjeevani.patient.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The per-app half of :core:network's flavor-agnostic config -- see
 *  [SupabaseConfig]'s own doc comment for why this lives here instead
 *  of in :core:network itself. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkConfigModule {
    @Provides
    @Singleton
    fun provideSupabaseConfig(): SupabaseConfig {
        return SupabaseConfig(url = BuildConfig.SUPABASE_URL, anonKey = BuildConfig.SUPABASE_ANON_KEY)
    }
}
