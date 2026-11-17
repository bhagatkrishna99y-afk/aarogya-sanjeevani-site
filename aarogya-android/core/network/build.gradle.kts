plugins {
    alias(libs.plugins.aarogya.android.library)
    alias(libs.plugins.aarogya.android.hilt)
    alias(libs.plugins.aarogya.android.lint)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.aarogya.sanjeevani.core.network"
}

dependencies {
    api(project(":core:common"))

    // Versions for every supabase-kt artifact below come from this BOM --
    // see gradle/libs.versions.toml's `supabase` version, not an
    // individual version on auth-kt/postgrest-kt themselves. `api`, not
    // `implementation`: auth-kt/postgrest-kt are `api` below, so a
    // consumer's *own* configurations (app-patient's androidTest, e.g.)
    // resolve them transitively too -- without the BOM also being `api`,
    // those configurations have no version constraint to resolve
    // auth-kt/postgrest-kt against at all (both are version-less in
    // gradle/libs.versions.toml on purpose, the BOM is the only source
    // of their version).
    api(platform(libs.supabase.bom))
    api(libs.supabase.auth.kt)
    api(libs.supabase.postgrest.kt)
    api(libs.supabase.storage.kt)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // EncryptedSessionManager -- the Supabase session (access/refresh
    // tokens) must never sit on disk in plain text.
    implementation(libs.androidx.security.crypto)

    implementation(libs.timber)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
