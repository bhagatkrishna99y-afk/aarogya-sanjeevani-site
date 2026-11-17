package com.aarogya.sanjeevani.core.network

/**
 * Deliberately not read from this module's own BuildConfig -- :core:network
 * has no product flavors of its own and shouldn't need any just to know a
 * URL. Each app module provides this (reading *its own* BuildConfig,
 * which does carry per-flavor SUPABASE_URL/SUPABASE_ANON_KEY -- see
 * AndroidApplicationConventionPlugin) via a small Hilt module of its own,
 * e.g.:
 *
 * ```
 * @Provides
 * @Singleton
 * fun provideSupabaseConfig(): SupabaseConfig =
 *     SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
 * ```
 */
data class SupabaseConfig(
    val url: String,
    val anonKey: String,
)
